package cloud.apposs.robot.worker.service;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.robot.worker.WorkerFramework;

@Component
public class KanbanService {
    @Autowired
    private WorkerFramework framework;
}
