package cloud.apposs.robot.harness.setting;

import java.util.List;
import java.util.Map;

/**
 * MCP服务器配置，对应 setting.yaml 中 mcp 列表项，支持 stdio 和 sse/streamable-http 两种传输类型
 */
public class AIMcpSetting {
    // MCP服务器名称，用于标识和前缀工具名
    private String name;

    // 是否启用该MCP服务器，默认启用
    private boolean enabled = true;

    /**
     * 传输类型，支持：
     * <pre>
     *   1. {@code stdio} — 通过子进程标准输入输出通信
     *   2. {@code sse} — 通过 HTTP SSE 通信
     *   3. {@code streamable} — 通过 HTTP Streamable 通信
     * </pre>
     */
    private String type;

    // stdio 类型时的可执行命令，如 "npx"
    private String command;

    // stdio 类型时的命令参数列表，如 ["-y", "@modelcontextprotocol/server-filesystem"]
    private List<String> arguments;

    // stdio 类型时的环境变量
    private Map<String, String> environment;

    // sse/streamable 类型时的服务器 URL
    private String url;

    // 工具调用超时时间（秒），默认 30 秒
    private int timeout = 30;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
