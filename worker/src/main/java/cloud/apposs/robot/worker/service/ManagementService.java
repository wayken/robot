package cloud.apposs.robot.worker.service;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.robot.worker.WorkerConfig;
import cloud.apposs.robot.worker.WorkerFramework;
import cloud.apposs.robot.worker.WorkerManagement.ManagementProviderSetting;
import cloud.apposs.util.Table;

import java.io.IOException;
import java.util.List;

@Component
public class ManagementService {
    @Autowired
    private WorkerFramework framework;

    public void syncProvider(WorkerConfig configuration, Table<ManagementProviderSetting> providers) throws IOException {
        framework.getManagement().syncProvider(configuration, providers);
    }

    public List<ManagementProviderSetting> getProviderList() {
        return framework.getManagement().getProvider();
    }
}
