package cloud.apposs.robot.worker;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.robot.harness.bus.ILifeCycleHook;
import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.robot.harness.message.kind.AIAssistantMessage;
import cloud.apposs.robot.harness.provider.AIResponse;
import cloud.apposs.robot.harness.struct.MessageStruct;
import cloud.apposs.robot.worker.service.model.MessageModel;
import cloud.apposs.util.Param;
import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.annotation.OnCommand;
import cloud.apposs.websocket.annotation.OnConnect;
import cloud.apposs.websocket.annotation.OnDisconnect;
import cloud.apposs.websocket.annotation.ServerEndpoint;
import cloud.apposs.websocket.namespace.Namespace;

/**
 * AI 核心引擎，负责接收处理外部消息，进行AI统一调度和管理，
 * 一个 Worker 下可以创建多个聊天会话，但在前端连接只会建立一个 WS 会话连接，不同会话通过会话中的 sid 进行区分
 */
@ServerEndpoint("/worker.io")
public class WorkerMessageApi {
    private static final String COMMAND_MESSAGE = "message.commit";
    private static final String COMMAND_INTERRUPT = "message.interrupt";
    private static final String COMMAND_RESPONSE = "message.response";

    private Namespace namespace;

    @Autowired
    private WorkerFramework framework;

    private final ILifeCycleHook hook;

    public WorkerMessageApi() {
        this.hook = new WorkerLifeCycleHook();
    }

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
    @OnCommand(COMMAND_MESSAGE)
    public void message(WSSession session, MessageModel.Commit message) throws Exception {
        IMessageHook messageHook = new WorkerMessageHook(namespace);
        MessageStruct struct = new MessageStruct();
        struct.setWid(message.getWid());
        struct.setSid(message.getSid());
        struct.setRid(message.getRid());
        struct.setMessage(message.getMessage());
        framework.getHarness().harness(struct, messageHook);
    }

    /**
     * 中断指定会话正在进行的AI迭代循环
     *
     * @param session WebSocket会话对象
     * @param message 中断请求，包含 wid 和 sid
     */
    @OnCommand(COMMAND_INTERRUPT)
    public void interrupt(WSSession session, MessageModel.Interrupt message) throws Exception {
        framework.getHarness().interrupt(message.getWid(), message.getSid());
    }

    @OnConnect
    public void onConnect(WSSession session) {
        namespace = session.getNamespace();
        framework.getHarness().getMessageBus().registerLifeCycleHook(hook);
    }

    @OnDisconnect
    public void onDisconnect(WSSession session) {
        framework.getHarness().getMessageBus().unregisterLifeCycleHook(hook);
    }

    private static final class WorkerMessageHook implements IMessageHook {
        private final Namespace namespace;

        private WorkerMessageHook(Namespace namespace) {
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
                session.sendCommand(COMMAND_RESPONSE, infomation);
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
                    .setParam("message", assistant.deserialize());
            return infomation;
        }
    }

    private class WorkerLifeCycleHook implements ILifeCycleHook {
        @Override
        public void onLifeCycle(String id, String sid, String rid, Phase phase, Object... arguments) throws Exception {
            if (!phase.equals(Phase.PHASE_POST_COMPLETION)) {
                return;
            }
            AIResponse response = (AIResponse) arguments[0];
            for (WSSession session : namespace.getSessions()) {
                session.sendCommand(COMMAND_RESPONSE, response);
            }
        }
    }
}
