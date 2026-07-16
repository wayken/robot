package cloud.apposs.robot.harness.mcp;

import cloud.apposs.robot.harness.mcp.transport.McpSseTransport;
import cloud.apposs.robot.harness.mcp.transport.McpStdioTransport;
import cloud.apposs.robot.harness.mcp.transport.McpStreamableTransport;
import cloud.apposs.robot.harness.setting.AIMcpSetting;

public final class McpTransportFactory {
    public static final String TYPE_STDIO = "stdio";
    public static final String TYPE_SSE = "sse";
    public static final String TYPE_STREAMABLE = "streamable";

    /**
     * 根据配置创建 MCP 客户端
     *
     * @param  setting MCP服务器配置
     * @return 对应传输类型的 {@link McpTransport} 实例，不支持的类型返回 null
     */
    public static McpTransport create(AIMcpSetting setting) {
        if (setting == null) {
            return null;
        }
        String type = setting.getType();
        if (type == null || type.isEmpty()) {
            return null;
        }
        int timeout = setting.getTimeout();
        String serverName = setting.getName();
        switch (type.toLowerCase()) {
            case TYPE_STDIO:
                return new McpStdioTransport(serverName, setting.getCommand(), setting.getArguments(), setting.getEnvironment(), timeout);
            case TYPE_SSE:
                return new McpSseTransport(serverName, setting.getUrl(), timeout);
            case TYPE_STREAMABLE:
                return new McpStreamableTransport(serverName, setting.getUrl(), timeout);
            default:
                return null;
        }
    }
}
