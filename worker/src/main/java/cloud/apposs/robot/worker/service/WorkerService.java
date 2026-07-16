package cloud.apposs.robot.worker.service;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.robot.harness.struct.WorkerStruct;
import cloud.apposs.robot.worker.WorkerConfig;
import cloud.apposs.robot.worker.WorkerFramework;
import cloud.apposs.robot.worker.service.model.WorkerModel;

@Component
public class WorkerService {
    @Autowired
    private WorkerFramework framework;

    public void initialize(WorkerConfig configuration, WorkerModel.Initialize model) throws Exception {
        WorkerStruct worker = new WorkerStruct();
        worker.setId(model.getId());
        framework.getHarness().initialize(worker);
    }

    public void removeWorker(WorkerConfig configuration, WorkerModel.Remove model) throws Exception {
        framework.getHarness().remove(model.getId());
    }
}
