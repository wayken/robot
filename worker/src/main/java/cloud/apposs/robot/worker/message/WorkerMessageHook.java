package cloud.apposs.robot.worker.message;

import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.robot.harness.bus.ToolApprovalRequest;
import cloud.apposs.robot.harness.message.kind.AIAssistantMessage;
import cloud.apposs.robot.harness.provider.AIResponse;
import cloud.apposs.robot.worker.WorkerMessageApi;
import cloud.apposs.util.Param;
import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.namespace.Namespace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WorkerMessageHook implements IMessageHook {
    private static final Map<String, PendingApproval> APPROVAL_REQUESTS = new ConcurrentHashMap<>();

    private final Namespace namespace;

    public WorkerMessageHook(Namespace namespace) {
        this.namespace = namespace;
    }

    @Override
    public void onProcessing(String sid, String rid, AIResponse response) throws Exception {
        if (response.getContent().isEmpty() && !response.hasReasoningContent() && !response.hasToolCall()) {
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
    @Override
    public void onApprovalRequired(String sid, String rid, ToolApprovalRequest request) throws Exception {
        if (WorkerSecurityPolicy.isFullAccess()) {
            request.approve(true);
            return;
        }
        String id = UUID.randomUUID().toString();
        PendingApproval approval = new PendingApproval(id, sid, rid, request);
        APPROVAL_REQUESTS.put(id, approval);
        handleMessageBroadcast(approval.toMessage());
    }

    public static boolean approve(String id, boolean approved) {
        PendingApproval approval = APPROVAL_REQUESTS.remove(id);
        if (approval == null) {
            return false;
        }
        approval.request.approve(approved);
        return true;
    }

    public static List<Param> onApprovePending(boolean approved) {
        List<Param> results = new ArrayList<>();
        List<String> ids = new ArrayList<>(APPROVAL_REQUESTS.keySet());
        for (String id : ids) {
            PendingApproval approval = APPROVAL_REQUESTS.remove(id);
            if (approval == null) {
                continue;
            }
            approval.request.approve(approved);
            results.add(approval.toStatusMessage(approved));
        }
        return results;
    }

    public static List<Param> pending(String sid) {
        List<Param> approvals = new ArrayList<>();
        for (PendingApproval approval : APPROVAL_REQUESTS.values()) {
            if (sid == null || sid.equals(approval.sid)) {
                approvals.add(approval.toMessage());
            }
        }
        return approvals;
    }

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

    private static final class PendingApproval {
        private final String id;
        private final String sid;
        private final String rid;
        private final ToolApprovalRequest request;

        private PendingApproval(String id, String sid, String rid, ToolApprovalRequest request) {
            this.id = id;
            this.sid = sid;
            this.rid = rid;
            this.request = request;
        }

        private Param toMessage() {
            Param approval = Param.builder("id", id)
                    .setString("sid", sid)
                    .setString("rid", rid)
                    .setString("command", request.getCommand())
                    .setString("reason", request.getReason())
                    .setString("status", "pending");
            return Param.builder("id", id)
                    .setString("sid", sid)
                    .setString("rid", rid)
                    .setBoolean("finished", false)
                    .setBoolean("approval", true)
                    .setParam("message", Param.builder("role", "assistant")
                            .setString("content", "")
                            .setParam("approval", approval));
        }

        private Param toStatusMessage(boolean approved) {
            Param approval = Param.builder("id", id)
                    .setString("sid", sid)
                    .setString("rid", rid)
                    .setString("command", request.getCommand())
                    .setString("reason", request.getReason())
                    .setString("status", approved ? "approved" : "rejected")
                    .setBoolean("approved", approved);
            return Param.builder("id", id)
                    .setString("sid", sid)
                    .setString("rid", rid)
                    .setBoolean("finished", false)
                    .setBoolean("approval", true)
                    .setParam("message", Param.builder("role", "assistant")
                            .setString("content", "")
                            .setParam("approval", approval));
        }
    }
}
