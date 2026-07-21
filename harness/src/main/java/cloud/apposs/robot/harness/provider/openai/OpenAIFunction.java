package cloud.apposs.robot.harness.provider.openai;

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
 * AI模型调用响应解析函数，负责将OpenAI API的HTTP响应解析成{@link AIResponse}对象
 */
public class OpenAIFunction implements IoFunction<OkResponse, AIResponse> {
    // 流式模式下持续累积的响应对象，所有chunk都append到这里
    private AIResponse accumulated = null;
    // tool_calls 按 index 累积 name + arguments（流式下 arguments 分片传输）
    private List<ToolCallBuffer> toolCallBuffers = null;
    // 跨网络分片累积未以换行符结尾的半行原始数据（一行 data: 可能被TCP拆分到多个网络chunk中）
    private final StringBuilder pendingLine = new StringBuilder();
    // 跨chunk累积当前未完成的SSE事件data行（一个SSE事件可能被拆分到多个网络chunk中）
    private final StringBuilder pendingEventData = new StringBuilder();

    /**
     * 响应解析函数，支持流式(SSE)和非流式两种格式：
     * <pre>
     *   流式:   data: {"choices":[{"delta":{"content":"..."},"finish_reason":null}]}
     *   终止:   data: [DONE]
     *   非流式: {"choices":[{"message":{"role":"assistant","content":"..."}}]}
     * </pre>
     *
     * @param  response AI响应结构，包含响应体和流式标识
     * @return 解析后的数据结构，包含完整的响应内容和工具调用信息
     */
    @Override
    public AIResponse call(OkResponse response) throws Exception {
        // 检查HTTP状态码，非200则表示大模型响应错误，直接返回错误信息
        if (response.getStatus() != HttpStatus.HTTP_STATUS_200.getCode()) {
            return AIResponse.of(null, response.getContent(), response.getStatus(), true);
        }
        // 流式响应：解析每个chunk，积累所有数据内容直到收到 [DONE] 标记
        if (response.isSseResponse()) {
            return handleSseChunkParse(response);
        }
        // 非流式：直接解析完整响应体，走 message 字段
        return handleFullChunkParse(response.getContent());
    }

    /**
     * 处理单个SSE chunk，格式为 "data: {...}" 或 "data: [DONE]"
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
                // 去掉可能存在的\r（兼容\r\n换行）
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
            // 删除已处理的完整行，仅保留末尾未以换行符结尾的半行
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
        // [DONE] 标记流结束，将累积的tool_calls合并进accumulated后返回
        if ("[DONE]".equals(data)) {
            return buildFinishedResponse();
        }
        return handleStreamChunkParse(data);
    }

    /**
     * 解析流式chunk，返回当前chunk的delta数据，同时将内容追加到accumulated供[DONE]时汇总
     */
    private AIResponse handleStreamChunkParse(String chunk) throws IOException {
        Param body = JsonUtil.parseJsonParam(chunk);
        if (body == null) {
            return AIResponse.EMPTY;
        }
        Table<Param> choices = body.getTable("choices");
        if (choices == null || choices.isEmpty()) {
            return AIResponse.EMPTY;
        }
        Param choice = choices.get(0);
        Param delta = choice.getParam("delta");
        if (delta == null) {
            return AIResponse.EMPTY;
        }
        String id = body.getString("id");
        // 懒初始化累积对象
        if (accumulated == null) {
            accumulated = AIResponse.of(id, "", false);
        }
        // 当前chunk的delta内容
        String content = delta.getString("content");
        // 将delta追加到accumulated
        if (content != null && !content.isEmpty()) {
            accumulated.appendContent(content);
        }
        // 累积 reasoning_content（DeepSeek-R1等推理模型在content为空时通过此字段返回思考过程和工具调用）
        String reasoningContent = delta.getString("reasoning_content");
        if (reasoningContent != null && !reasoningContent.isEmpty()) {
            accumulated.appendReasoningContent(reasoningContent);
        }
        // 流式模式下部分服务商会在最后一个chunk携带usage（需请求时设置stream_options.include_usage=true）
        handleUsageParse(body, accumulated);
        // 累积 tool_calls 分片（arguments 是逐片追加的）
        Table<Param> toolCallChunks = delta.getTable("tool_calls");
        if (toolCallChunks != null && !toolCallChunks.isEmpty()) {
            if (toolCallBuffers == null) {
                toolCallBuffers = new ArrayList<>();
            }
            for (Param toolCallChunk : toolCallChunks) {
                int index = toolCallChunk.getInt("index", toolCallBuffers.size());
                while (toolCallBuffers.size() <= index) {
                    toolCallBuffers.add(new ToolCallBuffer());
                }
                ToolCallBuffer buffer = toolCallBuffers.get(index);
                String tid = toolCallChunk.getString("id");
                if (tid != null) {
                    buffer.id = tid;
                }
                Param function = toolCallChunk.getParam("function");
                if (function == null) {
                    continue;
                }
                String name = function.getString("name");
                if (name != null && !name.isEmpty()) {
                    buffer.name = name;
                }
                String argChunk = function.getString("arguments");
                if (argChunk != null) {
                    buffer.arguments.append(argChunk);
                }
            }
            handlePartialToolCallsUpdate();
        }
        // 返回累积的accumulated（全量内容），onProcessing通过sentContentLength截取delta
        return accumulated;
    }

    private void handlePartialToolCallsUpdate() {
        if (accumulated == null || toolCallBuffers == null || toolCallBuffers.isEmpty()) {
            return;
        }
        Table<AITool> tools = Table.builder();
        for (ToolCallBuffer buffer : toolCallBuffers) {
            if (buffer.name == null || buffer.name.isEmpty()) {
                continue;
            }
            AITool tool = AITool.of(buffer.id, buffer.name, buffer.arguments.toString());
            tool.setPartial(true);
            tools.add(tool);
        }
        if (!tools.isEmpty()) {
            accumulated.setTools(tools);
        }
    }

    /**
     * 解析非流式完整响应，走 choices[0].message 字段
     */
    private AIResponse handleFullChunkParse(String json) throws IOException {
        Param body = JsonUtil.parseJsonParam(json);
        if (body == null) {
            return AIResponse.EMPTY;
        }
        String id = body.getString("id");
        Table<Param> choices = body.getTable("choices");
        if (choices == null || choices.isEmpty()) {
            return AIResponse.of(id, "");
        }
        Param message = choices.get(0).getParam("message");
        if (message == null) {
            return AIResponse.of(id, "");
        }
        String content = message.getString("content");
        AIResponse completion = AIResponse.of(id, content != null ? content : "", true);
        // 解析 reasoning_content（DeepSeek-R1等推理模型通过此字段返回思考过程）
        String reasoningContent = message.getString("reasoning_content");
        if (reasoningContent != null && !reasoningContent.isEmpty()) {
            completion.appendReasoningContent(reasoningContent);
        }
        Table<Param> toolCalls = message.getTable("tool_calls");
        if (toolCalls != null && !toolCalls.isEmpty()) {
            Table<AITool> tools = Table.builder();
            for (Param toolCall : toolCalls) {
                Param function = toolCall.getParam("function");
                if (function == null) {
                    continue;
                }
                String tid = toolCall.getString("id");
                tools.add(AITool.of(tid, function.getString("name"), function.getString("arguments")));
            }
            completion.setTools(tools);
        }
        // 解析 usage token 消耗信息
        handleUsageParse(body, completion);
        // 若没有解析到tool_calls，但reasoning_content有内容，则尝试从中提取工具调用
        if (!completion.hasToolCall() && completion.hasReasoningContent()) {
            Table<AITool> tools = handleReasoningToolCallParse(completion.getReasoningContent());
            if (tools != null && !tools.isEmpty()) {
                completion.setTools(tools);
            }
        }
        return completion;
    }

    private AIResponse buildFinishedResponse() {
        if (accumulated == null) {
            accumulated = AIResponse.of(null, "", true);
        } else {
            accumulated.setFinished(true);
        }
        // 将累积的tool_calls buffer合并进accumulated
        if (toolCallBuffers != null && !toolCallBuffers.isEmpty()) {
            Table<AITool> tools = Table.builder();
            for (ToolCallBuffer buffer : toolCallBuffers) {
                if (buffer.name != null) {
                    tools.add(AITool.of(buffer.id, buffer.name, buffer.arguments.toString()));
                }
            }
            if (!tools.isEmpty()) {
                accumulated.setTools(tools);
            }
            toolCallBuffers = null;
        }
        // 若没有解析到tool_calls，但reasoning_content有内容，则尝试从中提取工具调用
        if (!accumulated.hasToolCall() && accumulated.hasReasoningContent()) {
            Table<AITool> tools = handleReasoningToolCallParse(accumulated.getReasoningContent());
            if (tools != null && !tools.isEmpty()) {
                accumulated.setTools(tools);
            }
        }
        AIResponse finished = accumulated;
        accumulated = null;
        return finished;
    }

    /**
     * 从reasoning_content中提取工具调用，格式为：
     * <pre>
     *   &lt;tool_call&gt;
     *       &lt;function=xxx&gt;
     *           &lt;parameter=yyy&gt;value&lt;/parameter&gt;
     *       &lt;/function&gt;
     *   &lt;/tool_call&gt;
     * </pre>
     */
    private Table<AITool> handleReasoningToolCallParse(String reasoningContent) {
        Table<AITool> tools = Table.builder();
        int searchFrom = 0;
        while (true) {
            int toolCallStart = reasoningContent.indexOf("<tool_call>", searchFrom);
            if (toolCallStart < 0) {
                break;
            }
            int toolCallEnd = reasoningContent.indexOf("</tool_call>", toolCallStart);
            if (toolCallEnd < 0) {
                break;
            }
            String toolCallBlock = reasoningContent.substring(toolCallStart + "<tool_call>".length(), toolCallEnd).trim();
            searchFrom = toolCallEnd + "</tool_call>".length();
            // 解析 <function=name> ... </function>
            int funcStart = toolCallBlock.indexOf("<function=");
            if (funcStart < 0) {
                continue;
            }
            int funcNameEnd = toolCallBlock.indexOf(">", funcStart);
            if (funcNameEnd < 0) {
                continue;
            }
            String funcName = toolCallBlock.substring(funcStart + "<function=".length(), funcNameEnd).trim();
            int funcEnd = toolCallBlock.indexOf("</function>", funcNameEnd);
            if (funcEnd < 0) {
                continue;
            }
            String funcBody = toolCallBlock.substring(funcNameEnd + 1, funcEnd).trim();
            // 将 <parameter=key>value</parameter> 解析为JSON arguments
            StringBuilder argsJson = new StringBuilder("{");
            boolean firstArg = true;
            int paramFrom = 0;
            while (true) {
                int paramStart = funcBody.indexOf("<parameter=", paramFrom);
                if (paramStart < 0) {
                    break;
                }
                int paramNameEnd = funcBody.indexOf(">", paramStart);
                if (paramNameEnd < 0) {
                    break;
                }
                String paramName = funcBody.substring(paramStart + "<parameter=".length(), paramNameEnd).trim();
                int paramEnd = funcBody.indexOf("</parameter>", paramNameEnd);
                if (paramEnd < 0) {
                    break;
                }
                String paramValue = funcBody.substring(paramNameEnd + 1, paramEnd);
                paramFrom = paramEnd + "</parameter>".length();
                if (!firstArg) {
                    argsJson.append(",");
                }
                firstArg = false;
                // 简单JSON转义
                argsJson.append("\"").append(paramName.replace("\"", "\\\"")).append("\":");
                argsJson.append("\"").append(paramValue.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")).append("\"");
            }
            argsJson.append("}");
            tools.add(AITool.of(null, funcName, argsJson.toString()));
        }
        return tools;
    }

    /**
     * 解析响应体中的 usage 字段，填充 AIResponse 的 token 消耗信息
     * <pre>
     *   "usage": {"prompt_tokens": 10, "completion_tokens": 20, "total_tokens": 30}
     * </pre>
     */
    private void handleUsageParse(Param body, AIResponse response) {
        Param usage = body.getParam("usage");
        if (usage == null) {
            return;
        }
        response.setPromptTokens(usage.getInt("prompt_tokens", 0));
        response.setCompletionTokens(usage.getInt("completion_tokens", 0));
        response.setTotalTokens(usage.getInt("total_tokens", 0));
    }

    /** 流式tool_call分片累积缓冲 */
    private static class ToolCallBuffer {
        String id;
        String name;
        StringBuilder arguments = new StringBuilder();
    }
}
