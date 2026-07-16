package cloud.apposs.robot.harness.mcp.schema;

import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;

/**
 * JSON-RPC 响应，包含请求 ID、结果和错误信息
 */
public final class JsonRpcResponse {
    private final long id;
    private final Param result;
    private final Param error;

    public JsonRpcResponse(long id, Param result, Param error) {
        this.id = id;
        this.result = result;
        this.error = error;
    }

    public long getId() {
        return id;
    }

    public Param getResult() {
        return result;
    }

    public Param getError() {
        return error;
    }

    public boolean isError() {
        return error != null;
    }

    public static JsonRpcResponse parse(String json) {
        Param body = JsonUtil.parseJsonParam(json);
        if (body == null) {
            return null;
        }
        Object idObj = body.getObject("id");
        long id = 0;
        if (idObj instanceof Number) {
            id = ((Number) idObj).longValue();
        }
        Param error = body.getParam("error");
        Param result = body.getParam("result");
        return new JsonRpcResponse(id, result, error);
    }
}
