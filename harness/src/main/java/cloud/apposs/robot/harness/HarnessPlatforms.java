package cloud.apposs.robot.harness;

import cloud.apposs.logger.Logger;
import cloud.apposs.robot.harness.platform.IPlatform;
import cloud.apposs.robot.harness.platform.PlatformFactory;
import cloud.apposs.robot.harness.setting.AIPlatformSetting;

import java.util.ArrayList;
import java.util.List;

public class HarnessPlatforms {
    private final String wid;

    private HarnessWorkerProfile profile;

    private List<IPlatform> platforms = new ArrayList<>();

    public HarnessPlatforms(HarnessWorker worker) {
        this.wid = worker.getId();
        this.profile = worker.getProfile();
    }

    public void start(HarnessWorker worker) throws Exception {
        List<AIPlatformSetting> platformSettings = profile.getPlatform();
        for (AIPlatformSetting platformSetting : platformSettings) {
            if (!platformSetting.isEnabled()) {
                continue;
            }
            IPlatform platform = PlatformFactory.create(platformSetting.getName(), platformSetting, worker);
            platform.start();
            platforms.add(platform);
            Logger.info("Harness Platform " + platformSetting.getName() + " from worker " + wid + " started successfully.");
        }
    }

    public void reload(HarnessWorkerProfile profile) {
        this.profile = profile;
    }

    public void shutdown() {
        for (IPlatform platform : platforms) {
            try {
                platform.shutdown();
            } catch (Exception e) {
                Logger.error("Failed to shutdown platform " + platform.getName() + " from worker " + wid, e);
            }
        }
    }
}
