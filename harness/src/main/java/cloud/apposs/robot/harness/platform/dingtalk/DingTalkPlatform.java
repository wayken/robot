package cloud.apposs.robot.harness.platform.dingtalk;

import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.harness.platform.AbstractPlatform;
import cloud.apposs.robot.harness.platform.dingtalk.DingTalkMessageListener;
import cloud.apposs.robot.harness.setting.AIPlatformSetting;
import cloud.apposs.util.Param;
import com.dingtalk.open.app.api.OpenDingTalkClient;
import com.dingtalk.open.app.api.OpenDingTalkStreamClientBuilder;
import com.dingtalk.open.app.api.callback.DingTalkStreamTopics;
import com.dingtalk.open.app.api.security.AuthClientCredential;

/**
 * 钉钉消息通道，基于 DingTalk Stream 模式监听机器人消息，并将用户消息驱动智能体迭代循环，最终将 AI 响应回复到钉钉会话中
 * 工作流程如下：
 * <pre>
 *   1. 使用 DingTalk Stream SDK 建立长连接，监听钉钉机器人消息
 *   2. 收到消息后构建通用消息结构体，驱动智能体进行 AI 推理迭代
 *   3. AI 推理完成后通过 {@link DingTalkMessageListener} 将结果回复到原始会话（单聊/群聊）
 * </pre>
 * 开发文档详见：https://opensource.dingtalk.com/developerpedia/docs/explore/tutorials/stream/overview/
 */
public class DingTalkPlatform extends AbstractPlatform {
    // 钉钉应用 Client ID（AppKey）
    private final String clientId;

    // 钉钉应用 Client Secret（AppSecret）
    private final String clientSecret;

    // DingTalk Stream 长连接客户端
    private volatile OpenDingTalkClient openStreamClient;

    private final HarnessWorker worker;

    public DingTalkPlatform(AIPlatformSetting setting, HarnessWorker worker) {
        super(setting);
        Param properties = getProperties();
        this.worker = worker;
        this.clientId = properties.getString("clientId");
        this.clientSecret = properties.getString("clientSecret");
    }

    public HarnessWorker getWorker() {
        return worker;
    }

    @Override
    public void start() throws Exception {
        DingTalkMessageListener messageListener = new DingTalkMessageListener(this);
        openStreamClient = OpenDingTalkStreamClientBuilder.custom()
                .credential(new AuthClientCredential(clientId, clientSecret))
                .registerCallbackListener(DingTalkStreamTopics.BOT_MESSAGE_TOPIC, messageListener).build();
        openStreamClient.start();
    }

    @Override
    public void shutdown() {
        if (openStreamClient != null) {
            try {
                openStreamClient.stop();
            } catch (Exception ignored) {
            } finally {
                openStreamClient = null;
            }
        }
    }
}
