package cloud.apposs.robot.harness.message;

import cloud.apposs.robot.harness.message.kind.AIAssistantMessage;
import cloud.apposs.robot.harness.message.kind.AISystemMessage;
import cloud.apposs.robot.harness.message.kind.AIToolMessage;
import cloud.apposs.robot.harness.message.kind.AIUserMessage;
import cloud.apposs.robot.harness.provider.AITool;
import cloud.apposs.robot.harness.util.Strings;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * AI消息对象，包含请求AI模型时的每条消息，消息内容可以是请求文本或者工具调用结果，
 * 主要用于在对话过程中传递AI模型生成的响应内容，以及AI调用工具的结果等信息，各消息类型详见{@link AIMessage}接口定义，
 * {@link cloud.apposs.robot.harness.provider.AIProviderApi}接口会根据不同AI模型服务商的要求来构建和解析AI消息对象，确保与AI模型接口的兼容性和正确性
 */
public class AIMessages {
    private final Table<AIMessage> messages = new Table<>();

    private final List<AIMessageListener> listeners = new ArrayList<>(1);

    public Table<AIMessage> getMessages() {
        return messages;
    }

    public static AIMessages builder() {
        return new AIMessages();
    }

    public AIMessages append(AIMessage message) throws Exception {
        return append(message, false);
    }

    /**
     * 添加消息到消息列表中，并触发消息追加事件
     *
     * @param  message   消息对象
     * @param  fireEvent 是否触发消息追加事件
     * @return 当前消息对象
     */
    public AIMessages append(AIMessage message, boolean fireEvent) throws Exception {
        messages.add(message);
        if (fireEvent) {
            for (AIMessageListener listener : listeners) {
                listener.onMessageAppend(message);
            }
        }
        return this;
    }

    /**
     * 添加消息列表到当前消息对象中
     *
     * @param  messages 消息列表
     * @return 当前消息对象
     */
    public AIMessages append(AIMessages messages) throws Exception {
        this.messages.addAll(messages.getMessages());
        return this;
    }

    public int size() {
        return messages.size();
    }

    public void addListener(AIMessageListener listener) {
        listeners.add(listener);
    }

    /**
     * 将消息文本数据序列化为{@link AIMessage}对象
     *
     * @param  infomation 消息文本数据，包含role和content等字段
     * @return {@link AIMessage}对象
     */
    public static AIMessage serialize(Param infomation) {
        String role = infomation.getString("role");
        if (Strings.isBlank(role)) {
            throw new IllegalArgumentException("role is required");
        }
        String content = infomation.getString("content");
        if (role.equals(AIToolMessage.ROLE)) {
            AIToolMessage message = new AIToolMessage(content);
            message.setId(infomation.getString("id"));
            message.setName(infomation.getString("name"));
            return message;
        } else if (role.equals(AIAssistantMessage.ROLE)) {
            Table<AITool> tools = Table.builder();
            Table<Param> rawTools = infomation.getTable("tools");
            if (rawTools != null) {
                for (Param rawTool : rawTools) {
                    tools.add(AITool.serialize(rawTool));
                }
            }
            AIAssistantMessage message = new AIAssistantMessage(content);
            message.setTools(tools);
            message.setContent(content);
            return message;
        } else if (role.equals(AIUserMessage.ROLE) || role.equals(AISystemMessage.ROLE)) {
            return new AIUserMessage(content);
        }
        throw new IllegalArgumentException("role is not supported: " + role);
    }
}
