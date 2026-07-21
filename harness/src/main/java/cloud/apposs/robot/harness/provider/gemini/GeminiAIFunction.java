package cloud.apposs.robot.harness.provider.gemini;

import cloud.apposs.okhttp.OkResponse;
import cloud.apposs.react.IoFunction;
import cloud.apposs.robot.harness.provider.AIResponse;
import cloud.apposs.robot.harness.provider.AITool;
import cloud.apposs.util.HttpStatus;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

import java.io.IOException;

/**
 * Gemini AI模型调用响应解析函数，负责将Gemini API的HTTP响应解析成{@link AIResponse}对象，
 * 支持流式(SSE)和非流式两种格式：
 * <pre>
 *   非流式响应：
 *   {
 *     "candidates": [{
 *       "content": {
 *         "parts": [{"text": "Hello!"}],
 *         "role": "model"
 *       },
 *       "finishReason": "STOP"
 *     }],
 *     "usageMetadata": {
 *       "promptTokenCount": 10,
 *       "candidatesTokenCount": 20,
 *       "totalTokenCount": 30
 *     }
 *   }
 *
 *   流式响应（每个SSE chunk均为完整JSON，非delta）：
 *   data: {"candidates":[{"content":{"parts":[{"text":"Hello"}],"role":"model"}}]}
 *   data: {"candidates":[{"content":{"parts":[{"text":"!"}],"role":"model"},"finishReason":"STOP"}]}
 * </pre>
 */
public class GeminiAIFunction implements IoFunction<OkResponse, AIResponse> {
    // 流式模式下持续累积的响应对象
    private AIResponse accumulated = null;
    // 跨网络分片累积未以换行符结尾的半行原始数据
    private final StringBuilder pendingLine = new StringBuilder();
    // 跨chunk累积当前未完成的SSE事件data行
    private final StringBuilder pendingEventData = new StringBuilder();

    @Override
    public AIResponse call(OkResponse response) throws Exception {
        // 检查HTTP状态码，非200则表示大模型响应错误，直接返回错误信息
        if (response.getStatus() != HttpStatus.HTTP_STATUS_200.getCode()) {
            return AIResponse.of(null, response.getContent(), response.getStatus(), true);
        }
        // 流式响应：解析每个SSE chunk，累积所有数据直到流结束
        if (response.isSseResponse()) {
            return handleSseChunkParse(response);
        }
        // 非流式：直接解析完整响应体
        return handleFullChunkParse(response.getContent());
    }

    /**
     * 处理SSE流式chunk，Gemini每个chunk都是完整JSON（非delta），需要累积文本内容
     */
    private AIResponse handleSseChunkParse(OkResponse response) throws IOException {
        String chunk = response.getContent();
        AIResponse lastResponse = accumulated != null ? accumulated : AIResponse.EMPTY;
        // 先追加网络分片，只处理以换行符结尾的完整行，末尾未结束的半行保留到下个网络分片再拼接，避免续接字节因不以"data:"开头而被误丢弃导致JSON不完整。
        if (chunk != null && !chunk.isEmpty()) {
            pendingLine.append(chunk);
            int start = 0;
            int newlineIndex;
            while ((newlineIndex = pendingLine.indexOf("\n", start)) >= 0) {
                int lineEnd = newlineIndex;
                if (lineEnd > start && pendingLine.charAt(lineEnd - 1) == '\r') {
                    lineEnd--;
                }
                String rawLine = pendingLine.substring(start, lineEnd);
                start = newlineIndex + 1;
                AIResponse eventResponse = handleSseLine(rawLine);
                if (eventResponse != null) {
                    lastResponse = eventResponse;
                }
            }
            pendingLine.delete(0, start);
        }
        // 流结束时冲刷缓冲区中残留的最后一行（无换行符结尾）及尚未派发的事件数据
        if (response.isCompleted()) {
            if (pendingLine.length() > 0) {
                AIResponse eventResponse = handleSseLine(pendingLine.toString());
                pendingLine.setLength(0);
                if (eventResponse != null) {
                    lastResponse = eventResponse;
                }
            }
            if (pendingEventData.length() > 0) {
                AIResponse eventResponse = handleSseEvent(pendingEventData);
                if (eventResponse != null) {
                    lastResponse = eventResponse;
                }
            }
        }
        return lastResponse;
    }

    /**
     * 处理一条完整的SSE行：空行触发当前事件派发，data:行则追加到当前事件的data缓冲
     */
    private AIResponse handleSseLine(String rawLine) throws IOException {
        String dataLine = rawLine.trim();
        if (dataLine.isEmpty()) {
            return handleSseEvent(pendingEventData);
        }
        if (!dataLine.startsWith("data:")) {
            return null;
        }
        if (pendingEventData.length() > 0) {
            pendingEventData.append('\n');
        }
        pendingEventData.append(dataLine.substring(5).trim());
        return null;
    }

    private AIResponse handleSseEvent(StringBuilder eventData) throws IOException {
        if (eventData.length() == 0) {
            return null;
        }
        String data = eventData.toString().trim();
        eventData.setLength(0);
        return handleStreamChunkParse(data);
    }

    /**
     * 解析单个流式chunk（Gemini流式每个chunk是完整JSON，将文本追加到accumulated）
     */
    private AIResponse handleStreamChunkParse(String chunk) throws IOException {
        Param body = JsonUtil.parseJsonParam(chunk);
        if (body == null) {
            return accumulated != null ? accumulated : AIResponse.EMPTY;
        }
        if (accumulated == null) {
            accumulated = AIResponse.of(null, "", false);
        }
        Table<Param> candidates = body.getTable("candidates");
        if (candidates != null && !candidates.isEmpty()) {
            Param candidate = candidates.get(0);
            Param content = candidate.getParam("content");
            if (content != null) {
                Table<Param> parts = content.getTable("parts");
                if (parts != null) {
                    for (Param part : parts) {
                        String text = part.getString("text");
                        if (text != null && !text.isEmpty()) {
                            accumulated.appendContent(text);
                        }
                        // 解析 functionCall part（工具调用）
                        Param functionCall = part.getParam("functionCall");
                        if (functionCall != null) {
                            handleFunctionCallParse(functionCall, accumulated);
                        }
                    }
                }
            }
            // 检查是否结束
            String finishReason = candidate.getString("finishReason");
            if (finishReason != null && !finishReason.isEmpty() && !"null".equals(finishReason)) {
                accumulated.setFinished(true);
            }
        }
        // 解析 token 消耗信息
        handleUsageParse(body, accumulated);
        return accumulated;
    }

    /**
     * 解析非流式完整响应
     */
    private AIResponse handleFullChunkParse(String json) throws IOException {
        Param body = JsonUtil.parseJsonParam(json);
        if (body == null) {
            return AIResponse.EMPTY;
        }
        Table<Param> candidates = body.getTable("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return AIResponse.of(null, "", true);
        }
        Param candidate = candidates.get(0);
        Param content = candidate.getParam("content");
        if (content == null) {
            return AIResponse.of(null, "", true);
        }
        StringBuilder textBuilder = new StringBuilder();
        Table<AITool> tools = Table.builder();
        Table<Param> parts = content.getTable("parts");
        if (parts != null) {
            for (Param part : parts) {
                String text = part.getString("text");
                if (text != null && !text.isEmpty()) {
                    textBuilder.append(text);
                }
                // 解析 functionCall part（工具调用）
                Param functionCall = part.getParam("functionCall");
                if (functionCall != null) {
                    AITool tool = parseFunctionCall(functionCall);
                    if (tool != null) {
                        tools.add(tool);
                    }
                }
            }
        }
        AIResponse completion = AIResponse.of(null, textBuilder.toString(), true);
        if (!tools.isEmpty()) {
            completion.setTools(tools);
        }
        // 解析 token 消耗信息
        handleUsageParse(body, completion);
        return completion;
    }

    /**
     * 解析 functionCall part 并追加到 AIResponse 的 tools 列表
     * <pre>
     *   {"functionCall": {"name": "get_weather", "args": {"location": "Beijing"}}}
     * </pre>
     */
    private void handleFunctionCallParse(Param functionCall, AIResponse response) {
        AITool tool = parseFunctionCall(functionCall);
        if (tool == null) {
            return;
        }
        Table<AITool> tools = response.getTools();
        if (tools == null) {
            tools = Table.builder();
            response.setTools(tools);
        }
        tools.add(tool);
    }

    private AITool parseFunctionCall(Param functionCall) {
        String name = functionCall.getString("name");
        if (name == null || name.isEmpty()) {
            return null;
        }
        // Gemini 的 args 是 Param 对象，需要序列化为 JSON 字符串
        Param args = functionCall.getParam("args");
        String arguments = "{}";
        if (args != null) {
            try {
                arguments = JsonUtil.toJson(args);
            } catch (Exception ignored) {
                arguments = "{}";
            }
        }
        // Gemini 不返回 tool call id，使用 name 作为 id
        return AITool.of(name, name, arguments);
    }

    /**
     * 解析响应体中的 usageMetadata 字段，填充 AIResponse 的 token 消耗信息
     * <pre>
     *   "usageMetadata": {
     *     "promptTokenCount": 10,
     *     "candidatesTokenCount": 20,
     *     "totalTokenCount": 30
     *   }
     * </pre>
     */
    private void handleUsageParse(Param body, AIResponse response) {
        Param usage = body.getParam("usageMetadata");
        if (usage == null) {
            return;
        }
        response.setPromptTokens(usage.getInt("promptTokenCount", 0));
        response.setCompletionTokens(usage.getInt("candidatesTokenCount", 0));
        response.setTotalTokens(usage.getInt("totalTokenCount", 0));
    }
}
