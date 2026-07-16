package cloud.apposs.robot.worker.interceptor;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.robot.worker.WorkerFramework;
import cloud.apposs.websocket.interceptor.CommandarInterceptorAdapter;
import cloud.apposs.websocket.protocol.HandshakeData;

import java.util.Map;

@Component
public class SessionAuthInterceptor extends CommandarInterceptorAdapter {
    @Autowired
    private WorkerFramework framework;

    @Override
    public boolean isAuthorized(HandshakeData data) throws Exception {
        String apiKey = framework.getManagement().getManagementApiKey();
        Map<String, String> params = data.getParameters();
        String authorization = params != null ? params.get("authorization") : null;
        return apiKey.equals(authorization);
    }
}
