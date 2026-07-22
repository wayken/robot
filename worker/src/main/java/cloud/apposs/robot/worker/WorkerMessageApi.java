package cloud.apposs.robot.worker;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.robot.harness.bus.ILifeCycleHook;
import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.robot.harness.struct.MessageStruct;
import cloud.apposs.robot.worker.message.WorkerLifeCycleHook;
import cloud.apposs.robot.worker.message.WorkerMessageHook;
import cloud.apposs.robot.worker.service.MessagesService;
import cloud.apposs.robot.worker.service.model.MessageModel;
import cloud.apposs.util.Param;
import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.annotation.OnCommand;
import cloud.apposs.websocket.annotation.OnConnect;
import cloud.apposs.websocket.annotation.OnDisconnect;
import cloud.apposs.websocket.annotation.ServerEndpoint;
import cloud.apposs.websocket.namespace.Namespace;
import cloud.apposs.websocket.protocol.Metadata;

/**
 * AI 核心引擎，负责接收处理外部消息，进行AI统一调度和管理，
 * 一个 Worker 下可以创建多个聊天会话，但在前端连接只会建立一个 WS 会话连接，不同会话通过会话中的 sid 进行区分
 */
@ServerEndpoint("/worker.io")
public class WorkerMessageApi {
    public static final String COMMAND_MESSAGE_COMMIT = "message.commit";
    public static final String COMMAND_MESSAGE_RESPONSE = "message.response";
    public static final String COMMAND_MESSAGE_APPROVAL = "message.approval";
    public static final String COMMAND_MESSAGE_INTERRUPT = "message.interrupt";
    public static final String COMMAND_MESSAGE_TRUNCATE = "message.truncate";
    public static final String COMMAND_BROADCAST_STATUS = "message.broadcast.status";
    public static final String COMMAND_BROADCAST_TRUNCATE = "message.broadcast.truncate";

    private Namespace namespace;

    @Autowired
    private WorkerFramework framework;

    @Autowired
    private MessagesService messagesService;

    /**
     * 处理外部消息输入，进行AI调度和管理，响应数据时前端处理逻辑如下：
     * <pre>
     *     1. 如果消息属性tools不为空，表示AI响应包含工具调用结果，前端可以根据工具调用结果进行特殊展示
     *     2. 如果消息属性finished为true，表示AI响应完成，前端展示最终结果
     * </pre>
     *
     * @param session WebSocket会话对象，用于发送响应消息
     * @param message 输入消息结构体，包含消息内容、消息类型等信息
     */
    @OnCommand(COMMAND_MESSAGE_COMMIT)
    public void message(WSSession session, MessageModel.Commit message) throws Exception {
        IMessageHook messageHook = new WorkerMessageHook(namespace);
        MessageStruct struct = new MessageStruct();
        struct.setWid(message.getWid());
        struct.setSid(message.getSid());
        struct.setRid(message.getRid());
        struct.setMessage(message.getMessage());
        framework.getHarness().harness(struct, messageHook);
    }

    @OnCommand(COMMAND_MESSAGE_TRUNCATE)
    public void truncate(WSSession session, Metadata metadata, MessageModel.Truncate message) throws Exception {
        boolean truncated = messagesService.truncateMessages(message);
        if (metadata != null) {
            session.sendResponse(metadata.getCommandId(), truncated);
        }
        if (truncated) {
            handleTruncateBroadcast(message);
        }
    }

    /**
     * 中断指定会话正在进行的AI迭代循环
     *
     * @param session WebSocket会话对象
     * @param message 中断请求，包含 wid 和 sid
     */
    @OnCommand(COMMAND_MESSAGE_INTERRUPT)
    public void interrupt(WSSession session, Metadata metadata, MessageModel.Interrupt message) throws Exception {
        boolean interrupted = framework.getHarness().interrupt(message.getWid(), message.getSid());
        if (metadata != null) {
            session.sendResponse(metadata.getCommandId(), interrupted);
        }
        handleStatusBroadcast(message.getWid(), message.getSid(), false);
    }

    @OnCommand(COMMAND_MESSAGE_APPROVAL)
    public void approval(WSSession session, Metadata metadata, MessageModel.Approval message) throws Exception {
        boolean approved = Boolean.TRUE.equals(message.getApproved());
        boolean accepted = WorkerMessageHook.approve(message.getId(), approved);
        if (metadata != null) {
            session.sendResponse(metadata.getCommandId(), accepted);
        }
        if (accepted) {
            handleApprovalBroadcast(message, approved);
        }
    }

    @OnConnect
    public void onConnect(WSSession session) {
        namespace = session.getNamespace();
        ILifeCycleHook hook = new WorkerLifeCycleHook(namespace);
        framework.getHarness().getMessageBus().registerLifeCycleHook(hook);
    }

    @OnDisconnect
    public void onDisconnect(WSSession session) {
        ILifeCycleHook hook = new WorkerLifeCycleHook(namespace);
        framework.getHarness().getMessageBus().unregisterLifeCycleHook(hook);
    }

    private void handleStatusBroadcast(String wid, String sid, boolean running) throws Exception {
        if (namespace == null) {
            return;
        }
        Param infomation = Param.builder("wid", wid)
                .setString("sid", sid)
                .setBoolean("running", running);
        for (WSSession session : namespace.getSessions()) {
            session.sendCommand(COMMAND_BROADCAST_STATUS, infomation);
        }
    }

    private void handleTruncateBroadcast(MessageModel.Truncate message) throws Exception {
        if (namespace == null) {
            return;
        }
        Param infomation = Param.builder("wid", message.getWid())
                .setString("sid", message.getSid())
                .setString("id", message.getId())
                .setString("rid", message.getRid())
                .setString("message", message.getMessage());
        for (WSSession session : namespace.getSessions()) {
            session.sendCommand(COMMAND_BROADCAST_TRUNCATE, infomation);
        }
    }

    private void handleApprovalBroadcast(MessageModel.Approval message, boolean approved) throws Exception {
        if (namespace == null) {
            return;
        }
        Param approval = Param.builder("id", message.getId())
                .setString("status", approved ? "approved" : "rejected")
                .setBoolean("approved", approved);
        Param infomation = Param.builder("id", message.getId())
                .setString("sid", message.getSid())
                .setString("rid", message.getRid())
                .setBoolean("finished", false)
                .setBoolean("approval", true)
                .setParam("message", Param.builder("role", "assistant")
                        .setString("content", "")
                        .setParam("approval", approval));
        for (WSSession wsSession : namespace.getSessions()) {
            wsSession.sendCommand(COMMAND_MESSAGE_RESPONSE, infomation);
        }
    }
}
