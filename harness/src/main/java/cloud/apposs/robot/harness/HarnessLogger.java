package cloud.apposs.robot.harness;

import cloud.apposs.logger.Logger;
import cloud.apposs.robot.harness.util.Strings;

public final class HarnessLogger {
    private final HarnessSetting setting;

    public HarnessLogger(HarnessSetting setting) {
        this.setting = setting;
    }

    public void print(String wid, String sid, String rid, String message) {
        if (!setting.isRequestLog()) {
            return;
        }
        message = Strings.truncate(message, 1024);
        Logger.info("Env: %s:%s:%s - Stat: %s", wid, sid, rid, message);
    }
}
