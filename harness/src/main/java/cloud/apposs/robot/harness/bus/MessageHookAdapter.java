package cloud.apposs.robot.harness.bus;

import cloud.apposs.robot.harness.provider.AIResponse;

public class MessageHookAdapter implements IMessageHook {
    @Override
    public void onProcessing(String sid, String rid, AIResponse message) throws Exception {
    }

    @Override
    public void onCompletion(String sid, String rid, AIResponse message) throws Exception {
    }
}
