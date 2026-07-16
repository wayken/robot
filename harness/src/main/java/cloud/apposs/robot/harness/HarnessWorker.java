package cloud.apposs.robot.harness;

import cloud.apposs.configure.YamlConfigParser;
import cloud.apposs.okhttp.OkHttp;
import cloud.apposs.react.IoSubscription;
import cloud.apposs.react.React;
import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.robot.harness.command.ICommand;
import cloud.apposs.robot.harness.message.AIMessages;
import cloud.apposs.robot.harness.plugin.IPlugin;
import cloud.apposs.robot.harness.provider.AIProviderApi;
import cloud.apposs.robot.harness.provider.AIProviderApiFactory;
import cloud.apposs.robot.harness.provider.AIRequest;
import cloud.apposs.robot.harness.provider.AIResponse;
import cloud.apposs.robot.harness.setting.AIProviderSetting;
import cloud.apposs.robot.harness.skill.SkillStruct;
import cloud.apposs.robot.harness.struct.MessageStruct;
import cloud.apposs.robot.harness.tool.ITool;
import cloud.apposs.util.Table;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 智能体封装，负责将 LLM 响应、工具调用结果、消息历史等进行统一管理和封装，
 * 每创建一个智能体实例，就会创建一个 {@link HarnessWorker} 对象，其存储的工作目录为 ${workspace}/${UUID}
 */
public final class HarnessWorker {
    private final String id;

    private final HarnessWorkerProfile profile;

    private final HarnessFramework framework;

    private final HarnessWorkspace workspace;

    // 智能体思维引擎，负责消息记忆管理、记忆巩固、检索等功能
    private final HarnessMind mind;

    // 智能体技能管理，负责管理智能体的技能结构和技能调用
    private final HarnessSkill skill;

    // 智能体指令管理器，负责解析和执行消息中的指令，如重置指令、修改设置指令等
    private final HarnessCommands commands;

    // 智能体调度器，负责管理智能体的定时任务和周期性任务，如定时发送消息、定时执行工具等
    private final HarnessSchedule schedule;

    // 子智能体，负责通过主智能体代理进行推理，减少上下文污染
    private final HarnessDelegateWorker delegate;

    // 智能体工具包，包含所有可用工具的实例和调用方法，供 LLM 在推理过程中调用
    private final HarnessToolKit toolKit;

    // 智能体MCP传输器，负责与 MCP 服务器进行通信
    private final HarnessMcpTransport mcpTransport;

    // 智能体消息渠道管理器，负责管理智能体的消息输入输出渠道，如钉钉机器人、飞书机器人等
    private final HarnessPlatforms platforms;

    // 智能体插件管理器，供业务进行功能扩展
    private final HarnessPlugin plugin;

    // 当前活跃的会话订阅，键为会话ID，用于支持中断正在进行的 AI 迭代循环
    private final Map<String, IoSubscription> subscriptions = new ConcurrentHashMap<>();

    public HarnessWorker(String id, HarnessFramework framework) throws Exception {
        this.id = id;
        this.framework = framework;
        this.profile = new HarnessWorkerProfile();
        String workspace = framework.getSetting().getWorkspace();
        this.workspace = new HarnessWorkspace(workspace + File.separator + id);
        this.handleProfileLoad();
        this.skill = new HarnessSkill(this);
        this.mind = new HarnessMind(this);
        this.commands = new HarnessCommands(this);
        this.schedule = new HarnessSchedule(this);
        this.delegate = new HarnessDelegateWorker(this);
        this.toolKit = new HarnessToolKit(this);
        this.mcpTransport = new HarnessMcpTransport(this);
        this.platforms = new HarnessPlatforms(this);
        this.plugin = new HarnessPlugin(this);
        this.handlePluginInitialize();
    }

    public String getId() {
        return id;
    }

    public HarnessWorkspace getWorkspace() {
        return workspace;
    }

    public HarnessWorkerProfile getProfile() {
        return profile;
    }

    public HarnessFramework getFramework() {
        return framework;
    }

    public HarnessSkill getSkill() {
        return skill;
    }

    public HarnessMind getMind() {
        return mind;
    }

    public HarnessCommands getCommands() {
        return commands;
    }

    public HarnessToolKit getToolKit() {
        return toolKit;
    }

    public HarnessMcpTransport getMcpTransport() {
        return mcpTransport;
    }

    public HarnessSchedule getSchedule() {
        return schedule;
    }

    public HarnessDelegateWorker getDelegate() {
        return delegate;
    }

    public HarnessPlatforms getPlatforms() {
        return platforms;
    }

    public HarnessPlugin getPlugin() {
        return plugin;
    }

    /**
     * 启动智能体服务，主要负责如下工作：
     * <pre>
     *     1. 创建工作目录（如果不存在）
     *     2. 启动各机器人消息渠道监听（如钉钉机器人消息监听、飞书机器人消息监听等）
     * </pre>
     */
    public HarnessWorker start() throws Exception {
        // 初始化MCP服务器连接并注册工具
        mcpTransport.initialize();
        // 启动各机器人消息渠道监听
        platforms.start(this);
        return this;
    }

    /**
     * 运行 Agent 迭代循环（核心推理引擎），工作流程如下：
     * <pre>
     *     1. 调用大模型接口获取响应，将系统提示、消息历史、工具描述等作为输入
     *     2. 如果有工具调用：
     *        a. 执行所有工具
     *        b. 将结果添加到消息历史
     *        c. 继续循环（让大模型处理工具结果）
     *     3. 如果没有工具调用，返回最终答案
     *     4. 如果达到最大迭代次数，返回超时提示
     * </pre>
     * 返回：最终内容, 使用的工具列表, 完整消息历史
     *
     * @param message 输入消息
     */
    public boolean work(MessageStruct message, IMessageHook messageHook) throws Exception {
        try {
            // 判断是否为消息指令
            if (commands.isCommand(message.getMessage())) {
                String commandName = commands.parseCommandName(message.getMessage());
                Table<String> arguments = commands.parseCommandArgs(message.getMessage());
                String result = commands.runCommand(this, message.getSid(), commandName, arguments);
                messageHook.onCompletion(message.getSid(), message.getRid(), AIResponse.of(null, result, true));
                return false;
            }
            // 检查会话是否存在，不存在则直接返回错误
            if (!mind.isSessionExists(message.getWid(), message.getSid())) {
                AIResponse response = AIResponse.of(null, "Session not found: " + message.getSid(), true);
                messageHook.onCompletion(message.getSid(), message.getRid(), response);
                return false;
            }
            // 从存储系统构建消息
            AIMessages messages = AIMessages.builder();
            messages.addListener(new HarnessMessagePersistence(this, message));
            boolean success = mind.buildMessages(message, messages);
            if (!success) {
                AIResponse response = AIResponse.of(null, "Failed to build messages for session: " + message.getSid(), true);
                messageHook.onCompletion(message.getSid(), message.getRid(), response);
                return false;
            }
            // 构建AI模型请求参数
            AIRequest request = AIRequest.of(id, message.getSid(), message.getRid(), messages, toolKit.getToolDefinitions());
            HarnessSubscriber subscriber = new HarnessSubscriber(this, request, messageHook);
            HarnessIterationLoop iterationLoop = new HarnessIterationLoop(this, request, messageHook);
            IoSubscription subscription = completions(request).loop(iterationLoop).subscribe(subscriber).start();
            // 添加取消订阅回调，确保在取消订阅时能够正确清理资源
            subscription.addOnUnsubscribe(subscriber::onUnsubscribe);
            subscription.addOnUnsubscribe(() -> subscriptions.remove(message.getSid()));
            return true;
        } finally {
            framework.getLogger().print(id, message.getSid(), message.getRid(), "Processing Message: " + message.getMessage());
        }
    }

    /**
     * 中断指定会话正在进行的 AI 迭代循环
     *
     * @param  sid 会话ID
     * @return 是否成功中断（false 表示该会话当前没有正在运行的迭代）
     */
    public boolean interrupt(String sid) throws Exception {
        IoSubscription subscription = subscriptions.get(sid);
        if (subscription == null || subscription.isUnsubscribed()) {
            return false;
        }
        subscription.unsubscribe();
        return true;
    }

    /**
     * 重新加载配置，支持在不重启框架的情况下动态更新 Worker 配置
     */
    public void reload() throws Exception {
        // 加载文件配置项
        handleProfileLoad();
        // 初始化工具包
        toolKit.reload(this);
        // 重新加载MCP服务器连接
        mcpTransport.reload();
        // 重新加载插件
        plugin.reload(this);
        // 重新建立消息渠道
        platforms.reload(profile);
    }

    public React<AIResponse> completions(AIRequest request) throws Exception {
        AIProviderSetting provider = profile.getPrimaryProvider();
        return completions(request, provider);
    }

    public React<AIResponse> completions(AIRequest request, AIProviderSetting provider) throws Exception {
        String providerType = provider.getType();
        OkHttp httpClient = framework.getHttpClient();
        AIProviderApi providerApi = AIProviderApiFactory.create(providerType);
        return providerApi.completions(httpClient, provider, request);
    }

    /**
     * 停止智能体服务，负责清理各种资源，包括：
     * <pre>
     *     1. 停止定时任务
     *     2. 停止各机器人消息渠道监听
     * </pre>
     */
    public void shutdown() {
        schedule.shutdown();
        mcpTransport.shutdown();
        platforms.shutdown();
        plugin.release();
        mind.shutdown();
    }

    private void handleProfileLoad() throws Exception {
        String profilePath = workspace.root() + File.separator + HarnessConstants.PROFILE_FILE;
        YamlConfigParser parser = new YamlConfigParser();
        parser.parse(profile, profilePath);
    }

    private void handlePluginInitialize() throws Exception {
        Table<IPlugin> plugins = plugin.getPlugins();
        for (IPlugin iPlugin : plugins) {
            Table<ICommand> pluginCommands = iPlugin.getCommands();
            if (pluginCommands != null) {
                for (ICommand command : pluginCommands) {
                    commands.addCommand(command);
                }
            }
            Table<ITool> tools = iPlugin.getTools();
            if (tools != null) {
                for (ITool tool : tools) {
                    toolKit.register(tool);
                }
            }
            Table<SkillStruct> skillStructs = iPlugin.getSkills();
            if (skillStructs != null) {
                for (SkillStruct skillStruct : skillStructs) {
                    skill.addSkill(skillStruct);
                }
            }
        }
    }
}
