package cloud.apposs.robot.worker.message;

import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.robot.harness.message.kind.AIAssistantMessage;
import cloud.apposs.robot.harness.provider.AIResponse;
import cloud.apposs.robot.worker.WorkerMessageApi;
import cloud.apposs.util.Param;
import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.namespace.Namespace;

public class WorkerMessageHook implements IMessageHook {
    private final Namespace namespace;

    public WorkerMessageHook(Namespace namespace) {
        this.namespace = namespace;
    }

    @Override
    public void onProcessing(String sid, String rid, AIResponse response) throws Exception {
        if (response.getContent().isEmpty() && !response.hasReasoningContent()) {
            return;
        }
        Param infomation = handleResponseFormat(sid, rid, response);
        handleMessageBroadcast(infomation);
    }

    @Override
    public void onCompletion(String sid, String rid, AIResponse response) throws Exception {
        if (response.getContent().isEmpty()) {
            return;
        }
        Param infomation = handleResponseFormat(sid, rid, response)
                .setBoolean("completion", true);
        handleMessageBroadcast(infomation);
    }

    // 广播消息到所有连接的WS客户端，支持多浏览器窗口同步接收LLM响应
    private void handleMessageBroadcast(Param infomation) throws Exception {
        for (WSSession session : namespace.getSessions()) {
            session.sendCommand(WorkerMessageApi.COMMAND_MESSAGE_RESPONSE, infomation);
        }
    }

    private Param handleResponseFormat(String sid, String rid, AIResponse response) {
        AIAssistantMessage assistant = new AIAssistantMessage(response.getContent());
        if (response.hasReasoningContent()) {
            assistant.setReasoning(response.getReasoningContent());
        }
        if (response.getTools() != null) {
            assistant.setTools(response.getTools());
        }
        Param infomation = Param.builder("id", response.getId())
                .setString("sid", sid)
                .setString("rid", rid)
                .setBoolean("finished", response.isFinished())
                .setParam("message", assistant.deserialize().setInt("status", response.getStatus()));
        return infomation;
    }
}
