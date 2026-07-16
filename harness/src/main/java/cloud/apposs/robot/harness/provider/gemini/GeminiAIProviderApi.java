package cloud.apposs.robot.harness.provider.gemini;

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
import cloud.apposs.robot.harness.setting.AIModelSetting;
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
 * 基于Google Gemini的大模型调用API，参考
 * <pre>
 *     https://ai.google.dev/gemini-api/docs/text-generation?hl=zh-cn
 * </pre>
 */
public class GeminiAIProviderApi implements AIProviderApi {
    public static final String NAME = "gemini";

    @Override
    public React<AIResponse> completions(OkHttp httpClient, AIProviderSetting provider, AIRequest request) throws Exception {
        OkRequest okRequest = handleRequestBuild(provider, request);
        IDiscovery discovery = handleDiscoveryBuild(provider);
        return httpClient.execute(okRequest, discovery).map(new GeminiAIFunction());
    }

    /**
     * 构建API请求，请求示例：
     * <pre>
     *   curl "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent" \
     *   -H "x-goog-api-key: $GEMINI_API_KEY" \
     *   -H 'Content-Type: application/json' \
     *   -X POST \
     *   -d '{
     *     "system_instruction": {
     *       "parts": [{"text": "You are a helpful assistant."}]
     *     },
     *     "contents": [
     *       {
     *         "role": "user",
     *         "parts": [{"text": "Hello, who are you?"}]
     *       }
     *     ],
     *     "generationConfig": {
     *       "temperature": 0.7,
     *       "maxOutputTokens": 4096,
     *       "topP": 1.0
     *     }
     *   }'
     * </pre>
     *
     * @param  provider AI提供商设置
     * @param  request  AI请求参数封装，包括消息列表、工具列表等
     * @return 构建好的OkRequest对象，包含请求URL、请求体和必要的请求头
     */
    private OkRequest handleRequestBuild(AIProviderSetting provider, AIRequest request) throws Exception {
        AIMessages messages = request.getMessages();

        // Gemini 将 system 消息单独放在 system_instruction 字段，其余消息放在 contents
        Param systemInstruction = null;
        Table<Object> contents = Table.builder();
        for (AIMessage message : messages.getMessages()) {
            if (message instanceof AISystemMessage) {
                // 多个 system 消息合并为一个 system_instruction
                String text = message.getContent();
                if (text != null && !text.isEmpty()) {
                    if (systemInstruction == null) {
                        systemInstruction = Param.builder("parts",
                                Table.builder().add(Param.builder("text", text)));
                    } else {
                        // 追加到已有 parts
                        Table<Param> parts = systemInstruction.getTable("parts");
                        if (parts != null) {
                            parts.add(Param.builder("text", text));
                        }
                    }
                }
            } else {
                Param content = handleMessageFormat(message);
                if (content != null) {
                    contents.add(content);
                }
            }
        }

        // 构建 URL：{baseLink}/models/{model}:generateContent 或 :streamGenerateContent
        String providerLink = provider.getLink();
        if (providerLink == null || providerLink.isEmpty()) {
            providerLink = AIProviderType.fromName(NAME).getLink();
        }
        // 去掉末尾斜杠，拼接模型和方法
        if (providerLink.endsWith("/")) {
            providerLink = providerLink.substring(0, providerLink.length() - 1);
        }
        String method = provider.isStream() ? "streamGenerateContent" : "generateContent";
        String url = providerLink + "/models/" + provider.getModel().getName() + ":" + method;
        // 流式时需要追加 alt=sse 参数让服务端以 SSE 格式返回
        if (provider.isStream()) {
            url = url + "?alt=sse";
        }

        // 构建 generationConfig
        Param generationConfig = Param.builder("temperature", provider.getTemperature())
                .setInt("maxOutputTokens", provider.getMaxTokens())
                .setDouble("topP", provider.getTopP());

        FormEntity formEntity = FormEntity.builder(FormEntity.FORM_ENCTYPE_JSON)
                .add("contents", contents)
                .add("generationConfig", generationConfig);

        if (systemInstruction != null) {
            formEntity.add("system_instruction", systemInstruction);
        }

        // 将 ITool 列表转换为 Gemini function calling 格式
        // 格式: [{"functionDeclarations": [{"name":"...","description":"...","parameters":{...}}]}]
        Table<ITool> tools = request.getTools();
        if (tools != null && !tools.isEmpty()) {
            Table<Param> functionDeclarations = Table.builder();
            for (ITool tool : tools) {
                functionDeclarations.add(Param.builder("name", tool.name())
                        .setString("description", tool.description())
                        .setParam("parameters", tool.parameters()));
            }
            Table<Param> toolsDefinition = Table.builder();
            toolsDefinition.add(Param.builder("functionDeclarations", functionDeclarations));
            formEntity.add("tools", toolsDefinition);
        }

        OkRequest okRequest = OkRequest.builder().url(url).sse(provider.isStream()).post(formEntity);
        // Gemini 使用 x-goog-api-key header 传递 API Key
        String providerKey = handleProviderKeyPickup(provider.getKeys());
        if (Objects.nonNull(providerKey)) {
            okRequest.header("x-goog-api-key", providerKey);
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
        return new MemoryDiscovery(false, peers);
    }

    /**
     * 将 AIMessage 转换为 Gemini contents 格式：
     * <pre>
     *   {"role": "user"|"model", "parts": [...]}
     * </pre>
     * Gemini 的 role 只有 user 和 model（对应 OpenAI 的 assistant），
     * tool 调用结果用 functionResponse part 表示。
     */
    private Param handleMessageFormat(AIMessage message) {
        Table<Param> parts = Table.builder();

        if (message instanceof AIAssistantMessage) {
            AIAssistantMessage assistantMessage = (AIAssistantMessage) message;
            List<AITool> toolCalls = assistantMessage.getTools();
            if (toolCalls != null && !toolCalls.isEmpty()) {
                // 工具调用：每个 tool call 转为 functionCall part
                for (AITool tool : toolCalls) {
                    Param functionCall = Param.builder("name", tool.getName());
                    // arguments 是 JSON 字符串，Gemini 需要 Param 对象
                    if (tool.getArguments() != null && !tool.getArguments().isEmpty()) {
                        try {
                            Param args = cloud.apposs.util.JsonUtil.parseJsonParam(tool.getArguments());
                            if (args != null) {
                                functionCall.setParam("args", args);
                            }
                        } catch (Exception ignored) {
                            // 解析失败时跳过 args
                        }
                    }
                    parts.add(Param.builder("functionCall", functionCall));
                }
            } else {
                // 普通 assistant 文本回复
                String content = message.getContent();
                if (content != null && !content.isEmpty()) {
                    parts.add(Param.builder("text", content));
                }
            }
            return Param.builder("role", "model").setTable("parts", parts);

        } else if (message instanceof AIToolMessage) {
            // 工具调用结果：转为 functionResponse part
            AIToolMessage toolMessage = (AIToolMessage) message;
            Param response = Param.builder("name", toolMessage.getName())
                    .setParam("response", Param.builder("output", toolMessage.getContent()));
            parts.add(Param.builder("functionResponse", response));
            // Gemini 工具结果消息的 role 为 user
            return Param.builder("role", "user").setTable("parts", parts);

        } else {
            // user 消息，支持多模态
            String content = message.getContent();
            if (content != null && !content.isEmpty()) {
                parts.add(Param.builder("text", content));
            }
            // 处理多模态图片
            if (message.hasImages()) {
                for (AIImageContent image : message.getImages()) {
                    parts.add(handleImagePartFormat(image));
                }
            }
            return Param.builder("role", "user").setTable("parts", parts);
        }
    }

    /**
     * 将 AIImageContent 转换为 Gemini parts 格式：
     * <pre>
     *   Base64: {"inlineData": {"mimeType": "image/png", "data": "base64..."}}
     *   URL:    {"fileData":   {"mimeType": "image/jpeg", "fileUri": "https://..."}}
     * </pre>
     */
    private Param handleImagePartFormat(AIImageContent image) {
        String url = image.getUrl();
        if (url != null && url.startsWith("data:")) {
            // Base64 格式：data:{mimeType};base64,{data}
            int semicolon = url.indexOf(';');
            int comma = url.indexOf(',');
            if (semicolon > 5 && comma > semicolon) {
                String mimeType = url.substring(5, semicolon);
                String base64Data = url.substring(comma + 1);
                Param inlineData = Param.builder("mimeType", mimeType).setString("data", base64Data);
                return Param.builder("inlineData", inlineData);
            }
        }
        // URL 格式：推断 mimeType（Gemini 需要显式指定）
        String mimeType = inferMimeType(url);
        Param fileData = Param.builder("mimeType", mimeType).setString("fileUri", url);
        return Param.builder("fileData", fileData);
    }

    /**
     * 根据 URL 后缀推断图片 MIME 类型，默认返回 image/jpeg
     */
    private String inferMimeType(String url) {
        if (url == null) {
            return "image/jpeg";
        }
        String lower = url.toLowerCase();
        if (lower.contains(".png")) return "image/png";
        if (lower.contains(".gif")) return "image/gif";
        if (lower.contains(".webp")) return "image/webp";
        if (lower.contains(".bmp")) return "image/bmp";
        return "image/jpeg";
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
