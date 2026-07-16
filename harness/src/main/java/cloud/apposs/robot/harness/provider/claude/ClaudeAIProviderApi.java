package cloud.apposs.robot.harness.provider.claude;

import cloud.apposs.balance.Peer;
import cloud.apposs.discovery.IDiscovery;
import cloud.apposs.discovery.MemoryDiscovery;
import cloud.apposs.okhttp.FormEntity;
import cloud.apposs.okhttp.OkHttp;
import cloud.apposs.okhttp.OkRequest;
import cloud.apposs.react.React;
import cloud.apposs.robot.harness.message.AIImageContent;
import cloud.apposs.robot.harness.message.AIMessage;
import cloud.apposs.robot.harness.message.AIMessages;
import cloud.apposs.robot.harness.message.kind.AIAssistantMessage;
import cloud.apposs.robot.harness.message.kind.AISystemMessage;
import cloud.apposs.robot.harness.message.kind.AIToolMessage;
import cloud.apposs.robot.harness.provider.AIProviderApi;
import cloud.apposs.robot.harness.provider.AIProviderType;
import cloud.apposs.robot.harness.provider.AIRequest;
import cloud.apposs.robot.harness.provider.AIResponse;
import cloud.apposs.robot.harness.provider.AITool;
import cloud.apposs.robot.harness.setting.AIProviderSetting;
import cloud.apposs.robot.harness.setting.AIProxySetting;
import cloud.apposs.robot.harness.tool.ITool;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 基于Anthropic Claude的大模型调用API，参考
 * <pre>
 *     https://docs.anthropic.com/en/api/messages
 * </pre>
 */
public class ClaudeAIProviderApi implements AIProviderApi {
    public static final String NAME = "claude";

    /** Claude API 必须携带的版本头 */
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    @Override
    public React<AIResponse> completions(OkHttp httpClient, AIProviderSetting provider, AIRequest request) throws Exception {
        OkRequest okRequest = handleRequestBuild(provider, request);
        IDiscovery discovery = handleDiscoveryBuild(provider);
        return httpClient.execute(okRequest, discovery).map(new ClaudeAIFunction());
    }

    /**
     * 构建API请求，请求示例：
     * <pre>
     * curl https://api.anthropic.com/v1/messages \
     *   -H "x-api-key: $ANTHROPIC_API_KEY" \
     *   -H "anthropic-version: 2023-06-01" \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "model": "claude-opus-4-5",
     *     "max_tokens": 4096,
     *     "system": "You are a helpful assistant.",
     *     "messages": [
     *       {"role": "user", "content": "Hello, who are you?"}
     *     ]
     *   }'
     * </pre>
     *
     * @param  provider AI提供商设置
     * @param  request  AI请求参数封装，包括消息列表、工具列表等
     * @return 构建好的OkRequest对象，包含请求URL、请求体和必要的请求头
     */
    private OkRequest handleRequestBuild(AIProviderSetting provider, AIRequest request) throws Exception {
        AIMessages messages = request.getMessages();

        // Claude 将 system 消息作为顶层 system 字段（字符串），不放入 messages 数组
        StringBuilder systemBuilder = new StringBuilder();
        Table<Object> formattedMessages = Table.builder();
        for (AIMessage message : messages.getMessages()) {
            if (message instanceof AISystemMessage) {
                if (systemBuilder.length() > 0) {
                    systemBuilder.append("\n");
                }
                systemBuilder.append(message.getContent());
            } else {
                Object formatted = handleMessageFormat(message);
                if (formatted != null) {
                    formattedMessages.add(formatted);
                }
            }
        }

        String providerLink = provider.getLink();
        if (providerLink == null || providerLink.isEmpty()) {
            providerLink = AIProviderType.fromName(NAME).getLink();
        }

        FormEntity formEntity = FormEntity.builder(FormEntity.FORM_ENCTYPE_JSON)
                .add("model", provider.getModel().getName())
                .add("max_tokens", provider.getMaxTokens())
                .add("stream", provider.isStream())
                .add("temperature", provider.getTemperature())
                .add("top_p", provider.getTopP())
                .add("messages", formattedMessages);

        if (systemBuilder.length() > 0) {
            formEntity.add("system", systemBuilder.toString());
        }

        // 将 ITool 列表转换为 Claude function calling 格式
        // 格式: [{"name":"...","description":"...","input_schema":{...}}]
        Table<ITool> tools = request.getTools();
        if (tools != null && !tools.isEmpty()) {
            Table<Param> definition = Table.builder();
            for (ITool tool : tools) {
                definition.add(Param.builder("name", tool.name())
                        .setString("description", tool.description())
                        .setParam("input_schema", tool.parameters()));
            }
            formEntity.add("tools", definition);
        }

        OkRequest okRequest = OkRequest.builder().url(providerLink).sse(provider.isStream()).post(formEntity);
        // Claude 使用 x-api-key header 传递 API Key，并且必须携带 anthropic-version
        String providerKey = handleProviderKeyPickup(provider.getKeys());
        if (Objects.nonNull(providerKey)) {
            okRequest.header("x-api-key", providerKey);
        }
        okRequest.header("anthropic-version", ANTHROPIC_VERSION);
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
        return new MemoryDiscovery(false, peers);
    }

    /**
     * 将 AIMessage 转换为 Claude messages 格式：
     * <pre>
     *   纯文本：{"role": "user"|"assistant", "content": "..."}
     *   多模态：{"role": "user", "content": [{"type":"text","text":"..."},{"type":"image",...}]}
     *   工具调用（assistant）：{"role": "assistant", "content": [{"type":"tool_use","id":"...","name":"...","input":{...}}]}
     *   工具结果（user）：{"role": "user", "content": [{"type":"tool_result","tool_use_id":"...","content":"..."}]}
     * </pre>
     */
    private Object handleMessageFormat(AIMessage message) {
        if (message instanceof AIAssistantMessage) {
            AIAssistantMessage assistantMessage = (AIAssistantMessage) message;
            List<AITool> toolCalls = assistantMessage.getTools();
            if (toolCalls != null && !toolCalls.isEmpty()) {
                // 工具调用：转为 tool_use content block 数组
                Table<Param> contentBlocks = Table.builder();
                for (AITool tool : toolCalls) {
                    Param toolUse = Param.builder("type", "tool_use")
                            .setString("name", tool.getName());
                    if (tool.getId() != null && !tool.getId().isEmpty()) {
                        toolUse.setString("id", tool.getId());
                    }
                    // arguments 是 JSON 字符串，Claude 需要 Param 对象作为 input
                    if (tool.getArguments() != null && !tool.getArguments().isEmpty()) {
                        try {
                            Param input = cloud.apposs.util.JsonUtil.parseJsonParam(tool.getArguments());
                            if (input != null) {
                                toolUse.setParam("input", input);
                            }
                        } catch (Exception ignored) {
                            toolUse.setParam("input", Param.builder());
                        }
                    } else {
                        toolUse.setParam("input", Param.builder());
                    }
                    contentBlocks.add(toolUse);
                }
                return Param.builder("role", "assistant").setTable("content", contentBlocks);
            }
            // 普通 assistant 文本回复
            String content = message.getContent();
            return Param.builder("role", "assistant").setString("content", content != null ? content : "");

        } else if (message instanceof AIToolMessage) {
            // 工具调用结果：转为 tool_result content block，role 为 user
            AIToolMessage toolMessage = (AIToolMessage) message;
            Param toolResult = Param.builder("type", "tool_result")
                    .setString("tool_use_id", toolMessage.getId())
                    .setString("content", toolMessage.getContent());
            Table<Param> contentBlocks = Table.builder();
            contentBlocks.add(toolResult);
            return Param.builder("role", "user").setTable("content", contentBlocks);

        } else {
            // user 消息，支持多模态
            if (message.hasImages()) {
                // 多模态：content 为 block 数组
                Table<Param> contentBlocks = Table.builder();
                String text = message.getContent();
                if (text != null && !text.isEmpty()) {
                    contentBlocks.add(Param.builder("type", "text").setString("text", text));
                }
                for (AIImageContent image : message.getImages()) {
                    contentBlocks.add(handleImageBlockFormat(image));
                }
                return Param.builder("role", "user").setTable("content", contentBlocks);
            }
            // 纯文本
            String content = message.getContent();
            return Param.builder("role", "user").setString("content", content != null ? content : "");
        }
    }

    /**
     * 将 AIImageContent 转换为 Claude image content block 格式：
     * <pre>
     *   Base64: {"type":"image","source":{"type":"base64","media_type":"image/png","data":"..."}}
     *   URL:    {"type":"image","source":{"type":"url","url":"https://..."}}
     * </pre>
     */
    private Param handleImageBlockFormat(AIImageContent image) {
        String url = image.getUrl();
        if (url != null && url.startsWith("data:")) {
            // Base64 格式：data:{mimeType};base64,{data}
            int semicolon = url.indexOf(';');
            int comma = url.indexOf(',');
            if (semicolon > 5 && comma > semicolon) {
                String mediaType = url.substring(5, semicolon);
                String base64Data = url.substring(comma + 1);
                Param source = Param.builder("type", "base64")
                        .setString("media_type", mediaType)
                        .setString("data", base64Data);
                return Param.builder("type", "image").setParam("source", source);
            }
        }
        // URL 格式
        Param source = Param.builder("type", "url").setString("url", url);
        return Param.builder("type", "image").setParam("source", source);
    }

    private String handleProviderKeyPickup(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return null;
        }
        if (keys.size() == 1) {
            return keys.get(0);
        }
        // 随机获取一个 key
        return keys.get(Math.abs(keys.hashCode()) % keys.size());
    }
}
