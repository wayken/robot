package cloud.apposs.robot.harness;

import cloud.apposs.logger.Logger;
import cloud.apposs.robot.harness.mcp.McpToolWrapper;
import cloud.apposs.robot.harness.mcp.McpTransport;
import cloud.apposs.robot.harness.mcp.McpTransportFactory;
import cloud.apposs.robot.harness.mcp.schema.ListToolsResult;
import cloud.apposs.robot.harness.mcp.schema.Tool;
import cloud.apposs.robot.harness.setting.AIMcpSetting;
import cloud.apposs.util.Param;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP传输管理器，负责根据配置初始化各MCP服务器的客户端连接，并将远程MCP工具注册到 {@link HarnessToolKit} 中供智能体调用
 */
public class HarnessMcpTransport {
    private final HarnessWorker worker;

    // 已连接的MCP客户端，键为服务器名称
    private final Map<String, McpTransport> transports = new ConcurrentHashMap<String, McpTransport>();

    public HarnessMcpTransport(HarnessWorker worker) {
        this.worker = worker;
    }

    /**
     * 初始化所有已启用的MCP服务器连接，并将其工具注册到工具包中
     */
    public void initialize() {
        List<AIMcpSetting> mcpSettings = worker.getProfile().getMcp();
        if (mcpSettings == null || mcpSettings.isEmpty()) {
            return;
        }
        for (AIMcpSetting setting : mcpSettings) {
            if (!setting.isEnabled()) {
                continue;
            }
            try {
                handleServerConnect(setting);
            } catch (Exception e) {
                Logger.error(e, "HarnessMcpTransport: failed to connect MCP server [%s] ", setting.getName());
            }
        }
    }

    /**
     * 获取指定名称的MCP客户端
     *
     * @param  serverName MCP服务器名称
     * @return 对应的 {@link McpTransport}，不存在则返回 null
     */
    public McpTransport getTransport(String serverName) {
        return transports.get(serverName);
    }

    /**
     * 重新加载MCP服务器连接，关闭旧连接并重新建立，通常在配置热更新时调用
     */
    public void reload() {
        shutdown();
        initialize();
    }

    /**
     * 关闭所有MCP客户端连接并清理资源
     */
    public void shutdown() {
        for (Map.Entry<String, McpTransport> transport : transports.entrySet()) {
            try {
                transport.getValue().close();
            } catch (Exception e) {
                Logger.warn("HarnessMcpTransport: error closing MCP client [%s]: %s", transport.getKey(), e.getMessage());
            }
        }
        transports.clear();
    }

    /**
     * 根据配置建立单个MCP服务器连接，并将其工具注册到工具包
     */
    private void handleServerConnect(AIMcpSetting setting) throws Exception {
        String type = setting.getType();
        String serverName = setting.getName();
        if (type == null || type.isEmpty()) {
            Logger.warn("HarnessMcpTransport: MCP server [%s] has no type configured, skipping", serverName);
            return;
        }
        McpTransport transport = McpTransportFactory.create(setting);
        if (transport == null) {
            Logger.warn("HarnessMcpTransport: unsupported MCP transport type [%s] for server [%s]", type, serverName);
            return;
        }
        transport.initialize();
        // 列出该服务器上的所有工具并注册到工具包
        ListToolsResult toolsResult = transport.listTools();
        int registered = 0;
        for (Tool tool : toolsResult.getTools()) {
            Param parameters = handleSchemaConvert(tool.getInputSchema());
            McpToolWrapper wrapper = new McpToolWrapper(serverName, tool.getName(), tool.getDescription(), transport, parameters);
            worker.getToolKit().register(wrapper);
            registered++;
        }
        Logger.info("HarnessMcpTransport: connected to MCP server [%s] (%s), registered %d tools", serverName, type, registered);
        transports.put(serverName, transport);
    }

    /**
     * 将 MCP 工具的 JSON Schema（Param 格式）转换为框架内部的 {@link Param} 格式，
     * 如果 schema 为空则返回默认的空 object schema
     */
    private Param handleSchemaConvert(Param schema) {
        if (schema != null && !schema.isEmpty()) {
            return schema;
        }
        return Param.builder("type", "object").setParam("properties", new Param());
    }
}
