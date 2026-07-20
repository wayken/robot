package cloud.apposs.robot.worker.message;

import cloud.apposs.robot.harness.bus.ILifeCycleHook;
import cloud.apposs.robot.harness.provider.AIResponse;
import cloud.apposs.robot.worker.WorkerMessageApi;
import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.namespace.Namespace;

public class WorkerLifeCycleHook implements ILifeCycleHook {
    private final Namespace namespace;

    public WorkerLifeCycleHook(Namespace namespace) {
        this.namespace = namespace;
    }

    @Override
    public void onLifeCycle(String id, String sid, String rid, Phase phase, Object... arguments) throws Exception {
        if (!phase.equals(Phase.PHASE_POST_COMPLETION)) {
            return;
        }
        AIResponse response = (AIResponse) arguments[0];
        for (WSSession session : namespace.getSessions()) {
            session.sendCommand(WorkerMessageApi.COMMAND_MESSAGE_RESPONSE, response);
        }
    }
}
