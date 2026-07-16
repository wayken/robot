package cloud.apposs.robot.harness.message.kind;

import cloud.apposs.robot.harness.message.AIBaseMessage;
import cloud.apposs.util.Param;

/**
 * 工具调用结果消息，表示当前消息内容是工具调用的结果内容，消息示例：
 * <pre>
 *     {
 *         "role": "tool",
 *         "name": "read_file",
 *         "tool_call_id": "L2cVSvDt1",
 *         "content": "Tool execute result"
 *     }
 * </pre>
 */
public class AIToolMessage extends AIBaseMessage {
    public static String ROLE = "tool";

    private String id;

    private String name;

    public AIToolMessage(String content) {
        super(ROLE, content);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Param deserialize() {
        Param infomation = super.deserialize();
        return infomation.setString("id", id).setString("name", name);
    }
}
