package cloud.apposs.robot.shellx;

import cloud.apposs.configure.Value;

import java.io.File;

/**
 * ShellX 配置类，从 shellx.yaml 加载配置
 */
public class ShellXConfig {
    /** 工作主目录 */
    @Value("shellx.home")
    private String home = ShellXConstants.DEFAULT_HOME;

    /** 默认 Agent 名称 */
    @Value("shellx.default_agent")
    private String defaultAgent = ShellXConstants.DEFAULT_AGENT;

    /** 是否开启请求日志 */
    @Value("shellx.request_log")
    private boolean requestLog = false;

    /** 连接池大小 */
    @Value("shellx.proxy_pool.pool_size")
    private int proxyPoolSize = 12;

    /** 连接超时 */
    @Value("shellx.proxy_pool.connect_timeout")
    private int proxyConnectTimeout = 12000;

    /** 读写超时 */
    @Value("shellx.proxy_pool.socket_timeout")
    private int proxySocketTimeout = 300000;

    /** 日志级别 */
    @Value("shellx.logger.level")
    private String logLevel = "info";

    /** 是否启用彩色输出 */
    @Value("shellx.display.color")
    private boolean colorEnabled = true;

    /** 文本换行模式：always/never/auto */
    @Value("shellx.display.wrap")
    private String wrapMode = "auto";

    /** 推理强度：low/medium/high */
    @Value("shellx.effort")
    private String effort = "medium";

    public String getHome() {
        return resolvePath(home);
    }

    public void setHome(String home) {
        this.home = home;
    }

    public String getDefaultAgent() {
        return defaultAgent;
    }

    public void setDefaultAgent(String defaultAgent) {
        this.defaultAgent = defaultAgent;
    }

    public boolean isRequestLog() {
        return requestLog;
    }

    public void setRequestLog(boolean requestLog) {
        this.requestLog = requestLog;
    }

    public int getProxyPoolSize() {
        return proxyPoolSize;
    }

    public void setProxyPoolSize(int proxyPoolSize) {
        this.proxyPoolSize = proxyPoolSize;
    }

    public int getProxyConnectTimeout() {
        return proxyConnectTimeout;
    }

    public void setProxyConnectTimeout(int proxyConnectTimeout) {
        this.proxyConnectTimeout = proxyConnectTimeout;
    }

    public int getProxySocketTimeout() {
        return proxySocketTimeout;
    }

    public void setProxySocketTimeout(int proxySocketTimeout) {
        this.proxySocketTimeout = proxySocketTimeout;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public boolean isColorEnabled() {
        return colorEnabled;
    }

    public void setColorEnabled(boolean colorEnabled) {
        this.colorEnabled = colorEnabled;
    }

    public String getWrapMode() {
        return wrapMode;
    }

    public void setWrapMode(String wrapMode) {
        this.wrapMode = wrapMode;
    }

    public String getEffort() {
        return effort;
    }

    public void setEffort(String effort) {
        this.effort = effort;
    }

    /**
     * 获取工作空间路径
     */
    public String getWorkspace() {
        return getHome() + File.separator + ShellXConstants.DEFAULT_WORKSPACE;
    }

    /**
     * 获取会话存储路径
     */
    public String getSessionsPath() {
        return getHome() + File.separator + ShellXConstants.SESSIONS_DIR;
    }

    /**
     * 获取 Agent 配置路径
     */
    public String getAgentsPath() {
        return getHome() + File.separator + ShellXConstants.AGENTS_DIR;
    }

    /**
     * 获取日志路径
     */
    public String getLogsPath() {
        return getHome() + File.separator + ShellXConstants.LOGS_DIR;
    }

    private String resolvePath(String path) {
        if (path != null && path.startsWith("~")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }
}
