package cloud.apposs.robot.harness.platform;

import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.harness.platform.dingtalk.DingTalkPlatform;
import cloud.apposs.robot.harness.setting.AIPlatformSetting;

public final class PlatformFactory {
    public static IPlatform create(String type, AIPlatformSetting setting, HarnessWorker worker) {
        if (IPlatform.PLATFORM_DINGTALK.equals(type)) {
            return new DingTalkPlatform(setting, worker);
        }
        throw new IllegalArgumentException("Unsupported platform channel type: " + type);
    }
}
