package cloud.apposs.robot.harness.message.kind;

import cloud.apposs.robot.harness.message.AIBaseMessage;
import cloud.apposs.robot.harness.provider.AITool;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

/**
 * 用户消息，表示当前消息内容是用户输入的文本内容，消息示例：
 * <pre>
 *     1. 普通消息回复：{
 *         "assistant": "user",
 *         "content": "Hello! 👋 I"m teambeit robot, your AI assistant. How can I help you today?"
 *     }
 *     2. 工具调用回复：{
 *         "assistant": "tool",
 *         "content": null,
 *         "tool_calls": [
 *             {
 *                 "id": "L2cVSvDt1",
 *                 "type": "function",
 *                 "function": {"name": "read_file", "arguments": "{\"path\": \"/path/to/file.txt\"}"}
 *             }
 *         ]
 *     }
 * </pre>
 */
public class AIAssistantMessage extends AIBaseMessage {
    public static String ROLE = "assistant";

    protected Table<AITool> tools;

    public AIAssistantMessage(String content) {
        super(ROLE, content);
    }

    public Table<AITool> getTools() {
        return tools;
    }

    public void setTools(Table<AITool> tools) {
        this.tools = tools;
    }

    @Override
    public Param deserialize() {
        Param infomation = super.deserialize();
        if (reasoning != null && !reasoning.isEmpty()) {
            infomation.setString("reasoning", reasoning);
        }
        if (tools != null && !tools.isEmpty()) {
            Table<Param> formatTools = Table.builder();
            for (AITool tool : tools) {
                formatTools.add(tool.deserialize());
            }
            infomation.setTable("tools", formatTools);
        }
        return infomation;
    }
}
