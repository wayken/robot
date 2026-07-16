package cloud.apposs.robot.harness;

import cloud.apposs.logger.Logger;
import cloud.apposs.react.IoSubscriber;
import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.robot.harness.message.AIMessages;
import cloud.apposs.robot.harness.message.kind.AIAssistantMessage;
import cloud.apposs.robot.harness.provider.AIRequest;
import cloud.apposs.robot.harness.provider.AIResponse;

public class HarnessSubscriber extends IoSubscriber<AIResponse> {
    private final HarnessWorker worker;

    private final AIRequest request;

    private final IMessageHook messageHook;

    public HarnessSubscriber(HarnessWorker worker, AIRequest request, IMessageHook messageHook) {
        this.worker = worker;
        this.request = request;
        this.messageHook = messageHook;
    }

    @Override
    public void onNext(AIResponse response) throws Exception {
        if (response != null) {
            String wid = request.getWid();
            String sid = request.getSid();
            String rid = request.getRid();
            AIMessages messages = request.getMessages();
            String content = response.getContent();
            AIAssistantMessage assistant = new AIAssistantMessage(content);
            if (response.hasReasoningContent()) {
                assistant.setReasoning(response.getReasoningContent());
            }
            messages.append(assistant, true);
            worker.getMind().submitSession(wid, sid, false);
            messageHook.onCompletion(sid, rid, response);
            worker.getFramework().getLogger().print(request.getWid(), request.getSid(), request.getRid(), "Response To: " + content);
        }
    }

    @Override
    public void onError(Throwable cause) {
        try {
            messageHook.onError(cause);
        } catch (Exception e) {
            Logger.error("HarnessSubscriber onError callback failed", e);
        }
    }

    public void onUnsubscribe() throws Exception {
        String sid = request.getSid();
        String rid = request.getRid();
        messageHook.onCompletion(sid, rid, AIResponse.of(null, "", true));
    }
}
