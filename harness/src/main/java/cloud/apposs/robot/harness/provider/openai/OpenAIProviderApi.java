package cloud.apposs.robot.harness.provider.openai;

import cloud.apposs.balance.Peer;
import cloud.apposs.discovery.IDiscovery;
import cloud.apposs.discovery.MemoryDiscovery;
import cloud.apposs.okhttp.FormEntity;
import cloud.apposs.okhttp.OkHttp;
import cloud.apposs.okhttp.OkRequest;
import cloud.apposs.react.React;
import cloud.apposs.robot.harness.provider.AIProviderApi;
import cloud.apposs.robot.harness.provider.AIProviderType;
import cloud.apposs.robot.harness.provider.AIRequest;
import cloud.apposs.robot.harness.provider.AIResponse;
import cloud.apposs.robot.harness.setting.AIModelSetting;
import cloud.apposs.robot.harness.setting.AIProviderSetting;
import cloud.apposs.robot.harness.message.AIMessage;
import cloud.apposs.robot.harness.message.AIMessages;
import cloud.apposs.robot.harness.provider.AITool;
import cloud.apposs.robot.harness.message.kind.AIAssistantMessage;
import cloud.apposs.robot.harness.message.kind.AIToolMessage;
import cloud.apposs.robot.harness.setting.AIProxySetting;
import cloud.apposs.robot.harness.tool.ITool;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

import java.util.*;

public class OpenAIProviderApi implements AIProviderApi {
    public static final String NAME = "openai";

    @Override
    public React<AIResponse> completions(OkHttp httpClient, AIProviderSetting provider, AIRequest request) throws Exception {
        OkRequest okRequest = handleRequestBuild(provider, request);
        IDiscovery discovery = handleDiscoveryBuild(provider);
        return httpClient.execute(okRequest, discovery).map(new OpenAIFunction());
    }

    /**
     * 构建API请求，请求示例：
     * <pre>
     * curl https://api.openai.com/v1/chat/completions \
     *   -H "Content-Type: application/json" \
     *   -H "Authorization: Bearer $OPENAI_API_KEY" \
     *   -d '{
     *     "model": "gpt-3.5-turbo",
     *     "messages": [
     *       {
     *         "role": "system",
     *         "content": "You are a helpful assistant."
     *       },
     *       {
     *         "role": "user",
     *         "content": "Hello, who are you?"
     *       }
     *     ],
     *     "temperature": 0.7,
     *     "max_tokens": 100
     *   }'
     * </pre>
     *
     * @param  provider AI提供商设置
     * @param  request  AI请求参数封装，包括消息列表、工具列表等
     * @return 构建好的OkRequest对象，包含请求URL、请求体和必要的请求头
     */
    private OkRequest handleRequestBuild(AIProviderSetting provider, AIRequest request) throws Exception {
        AIMessages messages = request.getMessages();
        Table<Object> formatedMessages = Table.builder();
        for (AIMessage message : messages.getMessages()) {
            formatedMessages.add(handleMessageFormat(message));
        }
        String providerLink = provider.getLink();
        if (providerLink == null || providerLink.isEmpty()) {
            providerLink = AIProviderType.fromName(provider.getName()).getLink();
        }
        AIModelSetting model = provider.getModel();
        FormEntity formEntity = FormEntity.builder(FormEntity.FORM_ENCTYPE_JSON)
                .add("stream", provider.isStream())
                .add("model", model.getName())
                .add("messages", formatedMessages)
                .add("max_tokens", provider.getMaxTokens())
                .add("temperature", provider.getTemperature());
        if (!model.isBedrockModel()) {
            formEntity.add("top_p", provider.getTopP());
        }
        if (provider.isStream()) {
            // 开启流式时携带 stream_options，让服务端在最后一个chunk中返回usage信息
            formEntity.add("stream_options", Param.builder("include_usage", true));
        }
        // 将 ITool 列表转换为 OpenAI function calling 格式
        // 格式: [{"type":"function","function":{"name":"...","description":"...","parameters":{...}}}]
        if (model.isToolModel()) {
            Table<ITool> tools = request.getTools();
            if (tools != null && !tools.isEmpty()) {
                Table<Param> definition = Table.builder();
                for (ITool tool : tools) {
                    Param function = Param.builder("name", tool.name())
                            .setString("description", tool.description())
                            .setParam("parameters", tool.parameters());
                    definition.add(Param.builder("type", "function")
                            .setParam("function", function));
                }
                formEntity.add("tools", definition);
            }
        }
        OkRequest okRequest = OkRequest.builder().url(providerLink).sse(provider.isStream()).post(formEntity);
        String providerKey = handleProviderKeyPickup(provider.getKeys());
        if (Objects.nonNull(providerKey)) {
            okRequest.header("Authorization", "Bearer " + providerKey);
        }
        return okRequest;
    }

    private IDiscovery handleDiscoveryBuild(AIProviderSetting provider) {
        List<AIProxySetting> proxies = provider.getProxies();
        if (proxies == null || proxies.isEmpty()) {
            return null;
        }
        Map<String, List<Peer>> peers = new HashMap<String, List<Peer>>();
        for (AIProxySetting proxy : proxies) {
            Peer peer = new Peer(proxy.getHost(), proxy.getPort());
            peers.computeIfAbsent(NAME, k -> new ArrayList<>()).add(peer);
        }
        IDiscovery discovery = new MemoryDiscovery(false, peers);
        return discovery;
    }

    private Param handleMessageFormat(AIMessage message) {
        Param formattedMessage = Param.builder("role", message.getRole()).setString("content", message.getContent());
        if (message instanceof AIAssistantMessage) {
            AIAssistantMessage assistantMessage = (AIAssistantMessage) message;
            List<AITool> tools = assistantMessage.getTools();
            if (tools != null && !tools.isEmpty()) {
                Table<Param> toolCalls = Table.builder();
                for (AITool tool : tools) {
                    Param function = Param.builder("name", tool.getName()).setString("arguments", tool.getArguments());
                    Param toolCall = Param.builder("type", "function").setParam("function", function);
                    if (tool.getId() != null && !tool.getId().isEmpty()) {
                        toolCall.setString("id", tool.getId());
                    }
                    toolCalls.add(toolCall);
                }
                formattedMessage.setTable("tool_calls", toolCalls);
            }
        } else if (message instanceof AIToolMessage) {
            AIToolMessage toolMessage = (AIToolMessage) message;
            formattedMessage.setString("name", toolMessage.getName());
            formattedMessage.setString("content", toolMessage.getContent());
            formattedMessage.setString("tool_call_id", toolMessage.getId());
        }
        return formattedMessage;
    }

    private String handleProviderKeyPickup(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return null;
        }
        if (keys.size() == 1) {
            return keys.get(0);
        }
        // 随机获取一个key
        return keys.get(Math.abs(keys.hashCode()) % keys.size());
    }
}
