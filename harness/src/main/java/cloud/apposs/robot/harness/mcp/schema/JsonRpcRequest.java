package cloud.apposs.robot.harness.mcp.schema;

import cloud.apposs.robot.harness.mcp.McpSchema;
import cloud.apposs.util.Param;

/**
 * JSON-RPC 请求，包含请求 ID、方法名和参数
 */
public final class JsonRpcRequest {
    private final String jsonrpc = McpSchema.JSONRPC_VERSION;
    private final long id;
    private final String method;
    private final Param params;

    public JsonRpcRequest(long id, String method, Param params) {
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public String toJson() {
        Param body = Param.builder("jsonrpc", jsonrpc)
                .setLong("id", id)
                .setString("method", method);
        if (params != null && !params.isEmpty()) {
            body.setParam("params", params);
        }
        return body.toJson();
    }
}
