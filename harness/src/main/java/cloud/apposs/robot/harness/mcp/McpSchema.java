package cloud.apposs.robot.harness.mcp;

import cloud.apposs.util.Param;

public final class McpSchema {
    // JSON-RPC 版本
    public static final String JSONRPC_VERSION = "2.0";

    // MCP 协议版本
    public static final String PROTOCOL_VERSION = "2024-11-05";

    /**
     * 构建请求参数
     *
     * @param clientName    客户端名称
     * @param clientVersion 客户端版本
     */
    public static Param buildInitializeParams(String clientName, String clientVersion) {
        Param clientInfo = Param.builder("name", clientName).setString("version", clientVersion);
        Param capabilities = Param.builder();
        return Param.builder("protocolVersion", PROTOCOL_VERSION)
                .setParam("clientInfo", clientInfo)
                .setParam("capabilities", capabilities);
    }
}
