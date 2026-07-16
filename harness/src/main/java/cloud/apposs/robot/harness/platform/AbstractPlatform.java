package cloud.apposs.robot.harness.platform;

import cloud.apposs.robot.harness.setting.AIPlatformSetting;
import cloud.apposs.util.Param;

public abstract class AbstractPlatform implements IPlatform {
    protected final String id;

    protected final String name;

    protected final boolean enabled;

    protected final AIPlatformSetting setting;

    protected AbstractPlatform(AIPlatformSetting setting) {
        this.setting = setting;
        this.id = setting.getId();
        this.name = setting.getName();
        this.enabled = setting.isEnabled();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    protected Param getProperties() {
        return setting.getProperties();
    }
}
