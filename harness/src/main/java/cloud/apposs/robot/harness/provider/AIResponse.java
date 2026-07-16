package cloud.apposs.robot.harness.provider;

import cloud.apposs.util.HttpStatus;
import cloud.apposs.util.Table;

/**
 * AI响应结果，包含AI生成的文本内容、AI调用工具的结果、输入输出消耗的TOKEN数等信息
 */
public class AIResponse {
    public static final AIResponse EMPTY = AIResponse.of(null, "");

    // AI响应的唯一标识，可以用于追踪和调试AI响应的来源和处理过程
    private final String id;

    // AI响应的状态码，表示AI响应的处理结果，正常为200
    private final int status;

    // AI生成的文本内容
    private final StringBuilder content;

    // AI推理内容（部分模型如DeepSeek-R1会在reasoning_content中返回思考过程）
    private final StringBuilder reasoningContent = new StringBuilder();

    // AI调用工具的结果，AI在对话过程中可以调用工具来获取信息或者执行操作，
    // 工具调用的结果会包含在AIResponse中返回给调用方，调用方可以根据工具调用的结果来决定下一步的操作
    private Table<AITool> tools;

    // 输入消耗的TOKEN数
    private int promptTokens;

    // 输出消耗的TOKEN数
    private int completionTokens;

    // 总消耗的TOKEN数
    private int totalTokens;

    // AI是否已经完成了对话响应，在开启STREAMING模式时，AI会分多次返回响应内容，直到最后一次返回时才会设置finished为true
    private boolean finished = false;

    public AIResponse(String id, int status, String content, boolean finished) {
        this.id = id;
        this.status = status;
        this.content = new StringBuilder(content);
        this.finished = finished;
    }

    public static AIResponse of(String id, String content) {
        return new AIResponse(id, HttpStatus.HTTP_STATUS_200.getCode(), content, false);
    }

    public static AIResponse of(String id, String content, boolean finished) {
        return new AIResponse(id, HttpStatus.HTTP_STATUS_200.getCode(), content, finished);
    }

    public static AIResponse of(String id, String content, int status, boolean finished) {
        return new AIResponse(id, HttpStatus.HTTP_STATUS_200.getCode(), content, finished);
    }

    public String getId() {
        return id;
    }

    public int getStatus() {
        return status;
    }

    public String getContent() {
        return content.toString();
    }

    public void appendContent(String content) {
        this.content.append(content);
    }

    public String getReasoningContent() {
        return reasoningContent.toString();
    }

    public void appendReasoningContent(String reasoning) {
        this.reasoningContent.append(reasoning);
    }

    public boolean hasReasoningContent() {
        return reasoningContent.length() > 0;
    }

    public Table<AITool> getTools() {
        return tools;
    }

    public void setTools(Table<AITool> tools) {
        this.tools = tools;
    }

    public boolean hasToolCall() {
        return tools != null && !tools.isEmpty();
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(int promptTokens) {
        this.promptTokens = promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(int completionTokens) {
        this.completionTokens = completionTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    @Override
    public String toString() {
        return "AIResponse{" +
                "id='" + id + '\'' +
                ", status=" + status +
                ", content='" + content + '\'' +
                ", reasoningContent='" + reasoningContent + '\'' +
                ", tools=" + tools +
                ", promptTokens=" + promptTokens +
                ", completionTokens=" + completionTokens +
                ", totalTokens=" + totalTokens +
                ", finished=" + finished +
                '}';
    }
}
