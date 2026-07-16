package cloud.apposs.robot.shellx;

import cloud.apposs.robot.harness.HarnessFramework;
import cloud.apposs.robot.harness.HarnessSetting;
import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.shellx.session.SessionManager;

import java.io.Closeable;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * ShellX 运行时上下文，贯穿整个应用生命周期，
 * 持有 HarnessFramework 实例、配置、会话管理器等核心对象
 */
public class ShellXContext implements Closeable {
    private final ShellXConfig config;

    private final HarnessFramework framework;

    private final SessionManager sessionManager;

    /** 当前活跃的 Agent ID */
    private String activeAgentId;

    /** 是否为非交互模式 */
    private boolean nonInteractive = false;

    /** 是否信任所有工具 */
    private boolean trustAllTools = false;

    /** 推理强度 */
    private String effort;

    public ShellXContext(ShellXConfig config) throws Exception {
        this.config = config;
        this.effort = config.getEffort();
        // 确保工作目录存在
        ensureDirectories();
        // 初始化 Harness 框架
        HarnessSetting harnessSetting = new HarnessSetting();
        harnessSetting.setWorkspace(config.getWorkspace());
        harnessSetting.setRequestLog(config.isRequestLog());
        harnessSetting.setProxyPoolSize(config.getProxyPoolSize());
        harnessSetting.setProxyConnectTimeout(config.getProxyConnectTimeout());
        harnessSetting.setProxySocketTimeout(config.getProxySocketTimeout());
        this.framework = new HarnessFramework(harnessSetting);
        // 初始化会话管理器
        this.sessionManager = new SessionManager(config.getSessionsPath());
        // 设置默认 Agent
        this.activeAgentId = config.getDefaultAgent();
    }

    public ShellXConfig getConfig() {
        return config;
    }

    public HarnessFramework getFramework() {
        return framework;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public String getActiveAgentId() {
        return activeAgentId;
    }

    public void setActiveAgentId(String activeAgentId) {
        this.activeAgentId = activeAgentId;
    }

    public HarnessWorker getActiveWorker() {
        return framework.getWorker(activeAgentId);
    }

    public boolean isNonInteractive() {
        return nonInteractive;
    }

    public void setNonInteractive(boolean nonInteractive) {
        this.nonInteractive = nonInteractive;
    }

    public boolean isTrustAllTools() {
        return trustAllTools;
    }

    public void setTrustAllTools(boolean trustAllTools) {
        this.trustAllTools = trustAllTools;
    }

    public String getEffort() {
        return effort;
    }

    public void setEffort(String effort) {
        this.effort = effort;
    }

    private void ensureDirectories() throws Exception {
        String[] dirs = {
            config.getHome(),
            config.getWorkspace(),
            config.getSessionsPath(),
            config.getAgentsPath(),
            config.getLogsPath()
        };
        for (String dir : dirs) {
            File f = new File(dir);
            if (!f.exists()) {
                Files.createDirectories(Paths.get(dir));
            }
        }
    }

    @Override
    public void close() {
        if (framework != null) {
            framework.close();
        }
    }
}
