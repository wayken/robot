package cloud.apposs.robot.worker;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.robot.harness.HarnessFramework;
import cloud.apposs.robot.harness.HarnessSetting;

@Component
public class WorkerFramework {
    private final HarnessSetting setting;

    private final WorkerConfig configuration;

    private final HarnessFramework harness;

    private final WorkerManagement management;

    public WorkerFramework(WorkerConfig configuration) throws Exception {
        this.setting = new HarnessSetting();
        this.configuration = configuration;
        setting.setWorkspace(configuration.getWorkspace());
        setting.setRequestLog(configuration.isRequestLog());
        setting.setProxyPoolSize(configuration.getProxyPoolSize());
        setting.setProxyConnectTimeout(configuration.getProxyConnectTimeout());
        setting.setProxySocketTimeout(configuration.getProxySocketTimeout());
        this.harness = new HarnessFramework(setting);
        this.management = WorkerManagement.initialize(configuration);
    }

    public HarnessSetting getSetting() {
        return setting;
    }

    public WorkerConfig getConfiguration() {
        return configuration;
    }

    public HarnessFramework getHarness() {
        return harness;
    }

    public WorkerManagement getManagement() {
        return management;
    }
}
