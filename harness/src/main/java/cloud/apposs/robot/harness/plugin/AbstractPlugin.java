package cloud.apposs.robot.harness.plugin;

import cloud.apposs.robot.harness.HarnessWorker;

public abstract class AbstractPlugin implements IPlugin {
    protected final String name;

    protected final String version;

    protected final String description;

    protected final HarnessWorker worker;

    protected AbstractPlugin(String name, String version, String description, HarnessWorker worker) {
        this.name = name;
        this.version = version;
        this.description = description;
        this.worker = worker;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
