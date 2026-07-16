package cloud.apposs.robot.harness;

import cloud.apposs.configure.Value;
import cloud.apposs.robot.harness.mind.IMind;
import cloud.apposs.robot.harness.sandbox.ISandbox;
import cloud.apposs.robot.harness.setting.AIMcpSetting;
import cloud.apposs.robot.harness.setting.AIPlatformSetting;
import cloud.apposs.robot.harness.setting.AIProviderSetting;
import cloud.apposs.robot.harness.setting.AIToolkitSetting;

import java.util.List;

/**
 * Harness框架智能体配置，主要包含对每个智能体的配置信息，框架会根据这些配置信息来创建和管理智能体的生命周期和行为
 */
public class HarnessWorkerProfile {
    // 智能体使用的思维模型，默认为文件系统思维模型（IMind.MIND_FILESYSTEM），
    // 框架会根据这个配置来加载对应的思维模块，可以是基于文件系统的记忆、也可以是基于OpenViking的记忆系统
    @Value("framework.memoryType")
    private String mindType = IMind.MIND_FILESYSTEM;

    // 智能体默认运行沙箱环境类型，默认为本地沙箱（ISandbox.SANDBOX_LOCALHOST），
    @Value("framework.sanboxType")
    private String sanboxType = ISandbox.SANDBOX_LOCALHOST;

    // LLM模型最大迭代次数（工具调用循环），防止无限循环
    @Value("framework.maxIterations")
    private int maxIterations = 40;

    // 是否限制智能体只能访问工作空间内的工具和资源，开启后智能体只能访问预定义的工具和资源，无法访问外部网络等非工作空间内的资源，增强安全性
    @Value("framework.workspaceRestricted")
    private boolean workspaceRestricted = false;

    // 智能体记忆窗口大小，框架会根据这个配置来控制智能体进行记忆巩固有最大上下文消息添加
    @Value("framework.memoryWindow")
    private int memoryWindow = 50;

    // 智能体可用的工具列表
    private List<AIToolkitSetting> toolkit;

    // 智能体可用的插件列表
    private List<String> plugins;

    // AI服务商配置列表
    private List<AIProviderSetting> provider;

    // MCP配置列表
    private List<AIMcpSetting> mcp;

    // 消息渠道配置列表
    private List<AIPlatformSetting> platform;

    public String getMindType() {
        return mindType;
    }

    public void setMindType(String mindType) {
        this.mindType = mindType;
    }

    public String getSanboxType() {
        return sanboxType;
    }

    public void setSanboxType(String sanboxType) {
        this.sanboxType = sanboxType;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public boolean isWorkspaceRestricted() {
        return workspaceRestricted;
    }

    public void setWorkspaceRestricted(boolean workspaceRestricted) {
        this.workspaceRestricted = workspaceRestricted;
    }

    public int getMemoryWindow() {
        return memoryWindow;
    }

    public void setMemoryWindow(int memoryWindow) {
        this.memoryWindow = memoryWindow;
    }

    public List<AIToolkitSetting> getToolkit() {
        return toolkit;
    }

    public void setToolkit(List<AIToolkitSetting> toolkit) {
        this.toolkit = toolkit;
    }

    public List<String> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<String> plugins) {
        this.plugins = plugins;
    }

    public List<AIProviderSetting> getProvider() {
        return provider;
    }

    public void setProvider(List<AIProviderSetting> provider) {
        this.provider = provider;
    }

    /**
     * 获取主服务商配置
     *
     * @return 主服务商配置，如果没有设置主服务商，则返回第一个服务商配置，如果没有任何服务商配置，则返回null
     */
    public AIProviderSetting getPrimaryProvider() {
        for (AIProviderSetting setting : provider) {
            if (setting.isPrimary()) {
                return setting;
            }
        }
        return provider.isEmpty() ? null : provider.get(0);
    }

    /**
     * 根据名称获取服务商配置
     *
     * @param  name 服务商名称
     * @return 服务商配置，如果没有找到对应名称的服务商，则返回null
     */
    public AIProviderSetting getProvider(String name) {
        for (AIProviderSetting setting : provider) {
            if (setting.getName().equals(name)) {
                return setting;
            }
        }
        return null;
    }

    public List<AIMcpSetting> getMcp() {
        return mcp;
    }

    public void setMcp(List<AIMcpSetting> mcp) {
        this.mcp = mcp;
    }

    public List<AIPlatformSetting> getPlatform() {
        return platform;
    }

    public void setPlatform(List<AIPlatformSetting> platform) {
        this.platform = platform;
    }
}
