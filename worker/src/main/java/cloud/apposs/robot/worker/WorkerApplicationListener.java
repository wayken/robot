package cloud.apposs.robot.worker;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.logger.Logger;
import cloud.apposs.websocket.WSConfig;
import cloud.apposs.websocket.listener.ApplicationListenerAdapter;

@Component
public class WorkerApplicationListener extends ApplicationListenerAdapter {
    @Autowired
    private WorkerFramework framework;

    @Override
    public void onStartup(WSConfig config) {
        Logger.info("WorkerFramework initialized successfully, workspace: "
                + framework.getConfiguration().getWorkspace()
                + ", authorization: " + framework.getManagement().getManagementApiKey());
    }
}
