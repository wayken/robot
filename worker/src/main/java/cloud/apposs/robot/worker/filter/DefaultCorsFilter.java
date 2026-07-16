package cloud.apposs.robot.worker.filter;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.rest.filter.common.CorsFilter;
import cloud.apposs.websocket.WSHttpRequest;
import cloud.apposs.websocket.WSHttpResponse;

@Component
public class DefaultCorsFilter extends CorsFilter<WSHttpRequest, WSHttpResponse> {
    @Override
    protected String getRemoteHost(WSHttpRequest request) {
        return request.getRemoteHost();
    }

    @Override
    protected String getHeader(WSHttpRequest request, String name) {
        return request.getHeader(name);
    }

    @Override
    protected String getMethod(WSHttpRequest request) {
        return request.getMethod();
    }

    @Override
    protected void putHeader(WSHttpResponse response, String name, String value) {
        response.putHeader(name, value);
    }

    @Override
    protected void writeEmptyResponse(WSHttpResponse response) throws Exception {
        response.write("", true);
    }
}
