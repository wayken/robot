package cloud.apposs.robot.worker.interceptor;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.logger.Logger;
import cloud.apposs.react.React;
import cloud.apposs.rest.Handler;
import cloud.apposs.robot.worker.WorkerFramework;
import cloud.apposs.util.StrUtil;
import cloud.apposs.websocket.WSHttpRequest;
import cloud.apposs.websocket.WSHttpResponse;
import cloud.apposs.websocket.interceptor.HttpInterceptorAdapter;

import java.util.Arrays;
import java.util.List;

@Component
public class HttpAuthInterceptor extends HttpInterceptorAdapter {
    @Autowired
    private WorkerFramework framework;

    private final List<String> whitePathList = Arrays.asList(
            "/api/worker/status"
    );

    @Override
    public React<Boolean> preHandle(WSHttpRequest request, WSHttpResponse response, Handler handler) throws Exception {
        return React.emitter(() -> {
            // 白名单路径直接放行
            String path = request.getUri();
            if (whitePathList.contains(path)) {
                return true;
            }
            String apiKey = framework.getManagement().getManagementApiKey();
            String authorization = request.getHeader("authorization");
            if (StrUtil.isEmpty(authorization)) {
                authorization = request.getParameter("authorization");
            }
            if (!apiKey.equals(authorization)) {
                Logger.error("Unauthorized access to %s from %s with authorization: %s", path, request.getRemoteAddr(), authorization);
                return false;
            }
            return true;
        });
    }
}
