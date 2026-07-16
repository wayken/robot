package cloud.apposs.robot.harness.platform.dingtalk;

import cloud.apposs.robot.harness.HarnessMessageBus;
import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.harness.struct.MessageStruct;
import cloud.apposs.robot.harness.util.Strings;
import com.dingtalk.open.app.api.callback.OpenDingTalkCallbackListener;
import com.dingtalk.open.app.api.chatbot.BotReplier;
import com.dingtalk.open.app.api.models.bot.ChatbotMessage;
import com.dingtalk.open.app.api.models.bot.MessageContent;

/**
 * 钉钉机器人消息监听器，实现 {@link OpenDingTalkCallbackListener} 接口，
 * 负责将钉钉 Stream 模式收到的机器人消息转发给智能体迭代推理，并将 AI 响应回复到原始钉钉会话中，消息处理流程：
 * <pre>
 *   1. 通过 Stream 长连接接收钉钉机器人消息（单聊或群聊 AT）
 *   2. 提取消息文本内容，封装为通用消息体驱动智能体推理
 *   3. 智能体推理完成后，将结果以 Markdown 格式回复到原始会话
 * </pre>
 */
public class DingTalkMessageListener implements OpenDingTalkCallbackListener<ChatbotMessage, Void> {
    private final DingTalkPlatform platform;

    private final DingTalkLifeCycleHook hook;

    public DingTalkMessageListener(DingTalkPlatform platform) {
        this.platform = platform;
        this.hook = new DingTalkLifeCycleHook(platform.getWorker().getId(), platform.getId());
    }

    @Override
    public Void execute(ChatbotMessage message) {
        if (message == null) {
            return null;
        }
        // 1. 提取消息文本
        String content = handleTextExtract(message);
        if (Strings.isBlank(content)) {
            return null;
        }
        // 2. 构建通用消息结构体
        HarnessWorker worker = platform.getWorker();
        MessageStruct messageStruct = new MessageStruct();
        messageStruct.setWid(worker.getId());
        messageStruct.setSid(platform.getId());
        messageStruct.setMessage(content.trim());
        // 3. 注册消息生命周期钩子并发布消息到智能体迭代推理
        HarnessMessageBus messageBus = worker.getFramework().getMessageBus();
        if (!messageBus.isLifeCycleHookRegistered(hook)) {
            hook.setReplier(BotReplier.fromWebhook(message.getSessionWebhook()));
            messageBus.registerLifeCycleHook(hook);
        }
        messageBus.publishInboundMessage(messageStruct);
        return null;
    }

    /**
     * 从 {@link ChatbotMessage} 中提取文本内容
     * <pre>
     *   1. 文本类型消息：{@code message.getText().getContent()}
     *   2. 其他消息类型兜底：{@code message.getContent().getContent()}
     * </pre>
     */
    private String handleTextExtract(ChatbotMessage message) {
        MessageContent textContent = message.getText();
        if (textContent != null && textContent.getContent() != null && !textContent.getContent().isEmpty()) {
            return textContent.getContent();
        }
        MessageContent content = message.getContent();
        if (content != null && content.getContent() != null && !content.getContent().isEmpty()) {
            return content.getContent();
        }
        return null;
    }
}
