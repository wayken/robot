package cloud.apposs.robot.harness;

import cloud.apposs.robot.harness.message.AIImageContent;
import cloud.apposs.robot.harness.message.AIMessage;
import cloud.apposs.robot.harness.message.AIMessageListener;
import cloud.apposs.robot.harness.message.kind.AIUserMessage;
import cloud.apposs.robot.harness.mind.IMind;
import cloud.apposs.robot.harness.struct.MessageStruct;

import java.util.ArrayList;
import java.util.List;

public class HarnessMessagePersistence implements AIMessageListener {
    private final HarnessWorker worker;

    private final MessageStruct message;

    public HarnessMessagePersistence(HarnessWorker worker, MessageStruct message) {
        this.worker = worker;
        this.message = message;
    }

    @Override
    public void onMessageAppend(AIMessage message) throws Exception {
        String wid = this.message.getWid();
        String sid = this.message.getSid();
        String rid = this.message.getRid();
        worker.getMind().addSessionMessage(wid, sid, rid, handleMessagesFormat(message));
    }

    private AIMessage handleMessagesFormat(AIMessage message) {
        long timestamp = message.getTimestamp() > 0 ? message.getTimestamp() : System.currentTimeMillis();
        if (message instanceof AIUserMessage) {
            // 对用户进行格式化，基于副本数据操作
            String content = message.getContent();
            List<AIImageContent> images = message.hasImages() ? new ArrayList<>(message.getImages()) : null;
            // 剥离运行时上下文前缀
            if (content != null && content.startsWith(IMind.RUNTIME_CONTEXT_PREFIX)) {
                int separatorIndex = content.indexOf(IMind.RUNTIME_CONTEXT_SEPARATOR);
                if (separatorIndex >= 0) {
                    content = content.substring(separatorIndex + IMind.RUNTIME_CONTEXT_SEPARATOR.length());
                }
            }
            // 多模态消息：将 Base64 图片替换为 [image] 占位符
            if (images != null && !images.isEmpty()) {
                List<AIImageContent> newImages = new ArrayList<>();
                StringBuilder placeholders = new StringBuilder();
                for (AIImageContent image : images) {
                    String url = image.getUrl();
                    if (url != null && url.startsWith("data:image/")) {
                        placeholders.append("[image]");
                    } else {
                        newImages.add(image);
                    }
                }
                if (placeholders.length() > 0) {
                    content = (content == null || content.isEmpty()) ? placeholders.toString() : content + "\n" + placeholders;
                    images = newImages.isEmpty() ? null : newImages;
                }
            }
            // 构建持久化用的副本消息
            AIUserMessage persisted = new AIUserMessage(content, images);
            persisted.setReasoning(message.getReasoning());
            persisted.setTimestamp(timestamp);
            return persisted;
        }

        // 非 UserMessage 类型，只需设置时间戳（直接在原始对象上设置，因为timestamp不影响AI Provider发送）
        if (message.getTimestamp() <= 0) {
            message.setTimestamp(timestamp);
        }
        return message;
    }
}
