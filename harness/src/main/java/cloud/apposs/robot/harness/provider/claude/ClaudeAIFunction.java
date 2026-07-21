package cloud.apposs.robot.harness.provider.claude;

import cloud.apposs.okhttp.OkResponse;
import cloud.apposs.react.IoFunction;
import cloud.apposs.robot.harness.provider.AIResponse;
import cloud.apposs.robot.harness.provider.AITool;
import cloud.apposs.util.HttpStatus;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Claude AI模型调用响应解析函数，负责将Anthropic Claude API的HTTP响应解析成{@link AIResponse}对象，
 * 支持流式(SSE)和非流式两种格式：
 * <pre>
 *   非流式响应：
 *   {
 *     "id": "msg_01XFDUDYJgAACzvnptvVoYEL",
 *     "type": "message",
 *     "role": "assistant",
 *     "content": [
 *       {"type": "text", "text": "Hello!"},
 *       {"type": "tool_use", "id": "toolu_01A09q90qw90lq917835lq9", "name": "get_weather", "input": {"location": "Beijing"}}
 *     ],
 *     "stop_reason": "end_turn",
 *     "usage": {"input_tokens": 10, "output_tokens": 20}
 *   }
 *
 *   流式SSE事件序列：
 *   event: message_start
 *   data: {"type":"message_start","message":{"id":"msg_...","usage":{"input_tokens":10}}}
 *
 *   event: content_block_start
 *   data: {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}
 *
 *   event: content_block_delta
 *   data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Hello"}}
 *
 *   event: content_block_delta
 *   data: {"type":"content_block_delta","index":1,"delta":{"type":"input_json_delta","partial_json":"{\"loc"}}
 *
 *   event: message_delta
 *   data: {"type":"message_delta","delta":{"stop_reason":"end_turn"},"usage":{"output_tokens":20}}
 *
 *   event: message_stop
 *   data: {"type":"message_stop"}
 * </pre>
 */
public class ClaudeAIFunction implements IoFunction<OkResponse, AIResponse> {
    // 流式模式下持续累积的响应对象
    private AIResponse accumulated = null;
    // 流式模式下按 content block index 累积 tool_use 的 id/name/input_json
    private List<ToolUseBuffer> toolUseBuffers = null;
    // 跨网络分片累积未以换行符结尾的半行原始数据
    private final StringBuilder pendingLine = new StringBuilder();
    // 跨chunk累积当前未完成的SSE事件（event行 + data行）
    private final StringBuilder pendingEventData = new StringBuilder();
    // 当前SSE事件类型（event: xxx 行）
    private String pendingEventType = null;

    @Override
    public AIResponse call(OkResponse response) throws Exception {
        // 检查HTTP状态码，非200则表示大模型响应错误，直接返回错误信息
        if (response.getStatus() != HttpStatus.HTTP_STATUS_200.getCode()) {
            return AIResponse.of(null, response.getContent(), response.getStatus(), true);
        }
        // 流式响应：解析每个SSE chunk
        if (response.isSseResponse()) {
            return handleSseChunkParse(response);
        }
        // 非流式：直接解析完整响应体
        return handleFullChunkParse(response.getContent());
    }

    /**
     * 处理SSE流式chunk，Claude的SSE格式包含 event: 和 data: 两行
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
                AIResponse eventResponse = handleSseEvent(pendingEventType, pendingEventData);
                pendingEventType = null;
                pendingEventData.setLength(0);
                if (eventResponse != null) {
                    lastResponse = eventResponse;
                }
            }
        }
        return lastResponse;
    }

    /**
     * 处理一条完整的SSE行：空行表示一个SSE事件结束并派发；event:/data: 行则累积到当前事件
     */
    private AIResponse handleSseLine(String rawLine) throws IOException {
        String line = rawLine.trim();
        if (line.isEmpty()) {
            // 空行表示一个SSE事件结束，处理已累积的事件
            AIResponse eventResponse = handleSseEvent(pendingEventType, pendingEventData);
            pendingEventType = null;
            pendingEventData.setLength(0);
            return eventResponse;
        }
        if (line.startsWith("event:")) {
            pendingEventType = line.substring(6).trim();
        } else if (line.startsWith("data:")) {
            if (pendingEventData.length() > 0) {
                pendingEventData.append('\n');
            }
            pendingEventData.append(line.substring(5).trim());
        }
        return null;
    }

    private AIResponse handleSseEvent(String eventType, StringBuilder eventData) throws IOException {
        if (eventData.length() == 0) {
            return null;
        }
        String data = eventData.toString().trim();
        if (data.isEmpty()) {
            return null;
        }
        Param body = JsonUtil.parseJsonParam(data);
        if (body == null) {
            return accumulated != null ? accumulated : AIResponse.EMPTY;
        }
        if (accumulated == null) {
            accumulated = AIResponse.of(null, "", false);
        }
        String type = eventType != null ? eventType : body.getString("type", "");
        switch (type) {
            case "message_start":
                // 提取 message id 和 input_tokens
                Param message = body.getParam("message");
                if (message != null) {
                    handleUsageParse(message, accumulated);
                }
                break;
            case "content_block_start":
                // 初始化 tool_use buffer（text block 不需要特殊处理）
                Param contentBlock = body.getParam("content_block");
                if (contentBlock != null && "tool_use".equals(contentBlock.getString("type"))) {
                    int index = body.getInt("index", 0);
                    if (toolUseBuffers == null) {
                        toolUseBuffers = new ArrayList<>();
                    }
                    while (toolUseBuffers.size() <= index) {
                        toolUseBuffers.add(new ToolUseBuffer());
                    }
                    ToolUseBuffer buffer = toolUseBuffers.get(index);
                    buffer.id = contentBlock.getString("id");
                    buffer.name = contentBlock.getString("name");
                    handlePartialToolCallsUpdate();
                }
                break;
            case "content_block_delta":
                // 累积文本或 tool input JSON 片段
                int index = body.getInt("index", 0);
                Param delta = body.getParam("delta");
                if (delta == null) {
                    break;
                }
                String deltaType = delta.getString("type", "");
                if ("text_delta".equals(deltaType)) {
                    String text = delta.getString("text");
                    if (text != null && !text.isEmpty()) {
                        accumulated.appendContent(text);
                    }
                } else if ("input_json_delta".equals(deltaType)) {
                    // tool_use 的 input 分片传输
                    String partialJson = delta.getString("partial_json");
                    if (partialJson != null && toolUseBuffers != null && index < toolUseBuffers.size()) {
                        toolUseBuffers.get(index).inputJson.append(partialJson);
                        handlePartialToolCallsUpdate();
                    }
                }
                break;
            case "message_delta":
                // 包含 stop_reason 和 output_tokens
                Param msgDelta = body.getParam("delta");
                if (msgDelta != null) {
                    String stopReason = msgDelta.getString("stop_reason");
                    if (stopReason != null && !stopReason.isEmpty()) {
                        accumulated.setFinished(true);
                    }
                }
                handleUsageParse(body, accumulated);
                break;
            case "message_stop":
                // 流结束，合并 tool_use buffers
                buildFinishedResponse();
                break;
            default:
                break;
        }
        return accumulated;
    }

    private void handlePartialToolCallsUpdate() {
        if (accumulated == null || toolUseBuffers == null || toolUseBuffers.isEmpty()) {
            return;
        }
        Table<AITool> tools = Table.builder();
        for (ToolUseBuffer buffer : toolUseBuffers) {
            if (buffer.name == null || buffer.name.isEmpty()) {
                continue;
            }
            AITool tool = AITool.of(buffer.id, buffer.name, buffer.inputJson.toString());
            tool.setPartial(true);
            tools.add(tool);
        }
        if (!tools.isEmpty()) {
            accumulated.setTools(tools);
        }
    }

    private void buildFinishedResponse() {
        if (accumulated == null) {
            accumulated = AIResponse.of(null, "", true);
        }
        accumulated.setFinished(true);
        // 将累积的 tool_use buffers 合并进 accumulated
        if (toolUseBuffers != null && !toolUseBuffers.isEmpty()) {
            Table<AITool> tools = Table.builder();
            for (ToolUseBuffer buffer : toolUseBuffers) {
                if (buffer.name != null) {
                    tools.add(AITool.of(buffer.id, buffer.name, buffer.inputJson.toString()));
                }
            }
            if (!tools.isEmpty()) {
                accumulated.setTools(tools);
            }
            toolUseBuffers = null;
        }
    }

    /**
     * 解析非流式完整响应
     */
    private AIResponse handleFullChunkParse(String json) throws IOException {
        Param body = JsonUtil.parseJsonParam(json);
        if (body == null) {
            return AIResponse.EMPTY;
        }
        String id = body.getString("id");
        Table<Param> contentBlocks = body.getTable("content");
        if (contentBlocks == null || contentBlocks.isEmpty()) {
            return AIResponse.of(id, "", true);
        }
        StringBuilder textBuilder = new StringBuilder();
        Table<AITool> tools = Table.builder();
        for (Param block : contentBlocks) {
            String blockType = block.getString("type", "");
            if ("text".equals(blockType)) {
                String text = block.getString("text");
                if (text != null && !text.isEmpty()) {
                    textBuilder.append(text);
                }
            } else if ("tool_use".equals(blockType)) {
                // 工具调用：input 是 Param 对象，需序列化为 JSON 字符串
                String toolId = block.getString("id");
                String toolName = block.getString("name");
                Param input = block.getParam("input");
                String arguments = "{}";
                if (input != null) {
                    try {
                        arguments = JsonUtil.toJson(input);
                    } catch (Exception ignored) {
                        arguments = "{}";
                    }
                }
                tools.add(AITool.of(toolId, toolName, arguments));
            }
        }
        AIResponse completion = AIResponse.of(id, textBuilder.toString(), true);
        if (!tools.isEmpty()) {
            completion.setTools(tools);
        }
        // 解析 token 消耗信息
        handleUsageParse(body, completion);
        return completion;
    }

    /**
     * 解析响应体中的 usage 字段，填充 AIResponse 的 token 消耗信息
     * <pre>
     *   非流式/message_start: "usage": {"input_tokens": 10, "output_tokens": 20}
     *   message_delta:        "usage": {"output_tokens": 20}
     * </pre>
     */
    private void handleUsageParse(Param body, AIResponse response) {
        Param usage = body.getParam("usage");
        if (usage == null) {
            return;
        }
        int inputTokens = usage.getInt("input_tokens", 0);
        int outputTokens = usage.getInt("output_tokens", 0);
        if (inputTokens > 0) {
            response.setPromptTokens(inputTokens);
        }
        if (outputTokens > 0) {
            response.setCompletionTokens(outputTokens);
            response.setTotalTokens(response.getPromptTokens() + outputTokens);
        }
    }

    /** 流式 tool_use 分片累积缓冲 */
    private static class ToolUseBuffer {
        String id;
        String name;
        StringBuilder inputJson = new StringBuilder();
    }
}
