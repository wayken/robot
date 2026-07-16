package cloud.apposs.robot.harness.mcp.schema;

import cloud.apposs.robot.harness.mcp.McpSchema;
import cloud.apposs.util.Param;

/**
 * JSON-RPC 通知（无需响应）
 */
public final class JsonRpcNotification {
    private final String jsonrpc = McpSchema.JSONRPC_VERSION;
    private final String method;
    private final Param params;

    public JsonRpcNotification(String method, Param params) {
        this.method = method;
        this.params = params;
    }

    public String toJson() {
        Param body = Param.builder("jsonrpc", jsonrpc)
                .setString("method", method);
        if (params != null && !params.isEmpty()) {
            body.setParam("params", params);
        }
        return body.toJson();
    }
}
