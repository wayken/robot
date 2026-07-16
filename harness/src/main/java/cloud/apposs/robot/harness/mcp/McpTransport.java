package cloud.apposs.robot.harness.mcp;

import cloud.apposs.robot.harness.mcp.schema.CallToolRequest;
import cloud.apposs.robot.harness.mcp.schema.CallToolResult;
import cloud.apposs.robot.harness.mcp.schema.ListToolsResult;

/**
 * MCP客户端接口，定义与MCP服务器通信的核心操作，支持 StdIO / HTTP SSE / STREAMABLE HTTP 等多种通信方式
 */
public interface McpTransport {
    /**
     * 初始化连接，完成 MCP 握手（initialize + initialized 通知）
     *
     * @throws Exception 连接或握手失败时抛出
     */
    void initialize() throws Exception;

    /**
     * 获取服务器名称（对应配置中的 name 字段）
     */
    String getServerName();

    /**
     * 列出服务器上所有可用工具
     *
     * @return 工具列表结果
     * @throws Exception 请求失败时抛出
     */
    ListToolsResult listTools() throws Exception;

    /**
     * 调用指定工具
     *
     * @param  request 工具调用请求，包含工具名称和参数
     * @return 工具调用结果
     * @throws Exception 调用失败时抛出
     */
    CallToolResult callTool(CallToolRequest request) throws Exception;

    /**
     * 优雅关闭客户端连接，释放相关资源
     */
    void close();
}
