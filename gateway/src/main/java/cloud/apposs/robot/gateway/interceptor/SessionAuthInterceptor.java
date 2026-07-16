package cloud.apposs.robot.gateway.interceptor;

import cloud.apposs.bootor.BootorHttpRequest;
import cloud.apposs.bootor.BootorHttpResponse;
import cloud.apposs.bootor.WebUtil;
import cloud.apposs.bootor.interceptor.BooterInterceptorAdaptor;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.react.React;
import cloud.apposs.rest.Handler;
import cloud.apposs.rest.annotation.Order;
import cloud.apposs.robot.gateway.GatewayConstants;

import java.util.Arrays;
import java.util.List;

@Component
@Order(1024)
public class SessionAuthInterceptor extends BooterInterceptorAdaptor {
    private final List<String> whitePathList = Arrays.asList(
            "/api/v1/session",
            "/api/v1/session/login",
            "/api/v1/session/mfa",
            "/api/v1/session/logout",
            "/api/v1/node/add"
    );

    @Override
    public React<Boolean> preHandle(BootorHttpRequest request, BootorHttpResponse response, Handler handler) throws Exception {
        return React.emitter(() -> {
            // 白名单路径直接放行
            String path = WebUtil.getRequestPath(request);
            if (whitePathList.contains(path)) {
                return true;
            }
            // 只针对API接口才需校验会话，静态资源不处理
            if (!path.startsWith("/api")) {
                return true;
            }
            request.setAttribute(GatewayConstants.REQUEST_PARAMETRIC_AID, 1195326687496245248L);
            return true;
        });
    }
}
