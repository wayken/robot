package cloud.apposs.robot.harness.message;

import java.util.EventListener;

public interface AIMessageListener extends EventListener {
    void onMessageAppend(AIMessage message) throws Exception;
}
