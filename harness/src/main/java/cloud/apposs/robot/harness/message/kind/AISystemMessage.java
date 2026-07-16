package cloud.apposs.robot.harness.message.kind;

import cloud.apposs.robot.harness.message.AIBaseMessage;

/**
 * 用户消息，表示当前消息内容是用户输入的文本内容，消息示例：
 * <pre>
 *     {"role": "system", "content": "You are a helpful assistant."}
 * </pre>
 */
public class AISystemMessage extends AIBaseMessage {
    public static String ROLE = "system";

    public AISystemMessage(String content) {
        super(ROLE, content);
    }
}
