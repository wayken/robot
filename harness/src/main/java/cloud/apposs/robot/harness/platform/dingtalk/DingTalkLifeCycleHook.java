package cloud.apposs.robot.harness.platform.dingtalk;

import cloud.apposs.robot.harness.bus.ILifeCycleHook;
import cloud.apposs.robot.harness.provider.AIResponse;
import com.dingtalk.open.app.api.chatbot.BotReplier;

public class DingTalkLifeCycleHook implements ILifeCycleHook {
    private final String workerId;

    private final String platformId;

    private BotReplier replier;

    public DingTalkLifeCycleHook(String workerId, String platformId) {
        this.workerId = workerId;
        this.platformId = platformId;
    }

    public void setReplier(BotReplier replier) {
        this.replier = replier;
    }

    @Override
    public void onLifeCycle(String id, String sid, String rid, Phase phase, Object... arguments) throws Exception {
        if (!phase.equals(Phase.PHASE_POST_COMPLETION)) {
            return;
        }
        // 先匹配通道是否一致，如果不一致则直接返回，不进行后续的处理
        if (!workerId.equals(id) && !platformId.equals(sid)) {
            return;
        }
        AIResponse response = (AIResponse) arguments[0];
        replier.replyMarkdown("Teambeit Robot Reply", response.getContent());
    }
}
