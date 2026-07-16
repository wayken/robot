package cloud.apposs.robot.shellx;

import cloud.apposs.configure.YamlConfigParser;
import cloud.apposs.robot.shellx.cli.CliRouter;
import cloud.apposs.robot.shellx.command.*;
import cloud.apposs.robot.shellx.util.AnsiColor;
import cloud.apposs.robot.shellx.util.TerminalUtil;

import java.io.File;

/**
 * ShellX 命令行入口，基于 Harness 框架的命令行 TUI 架构。
 * <p>
 * 用法：
 * <pre>
 *   shellx                           启动交互式 TUI（等同于 shellx chat）
 *   shellx chat "prompt"             带初始问题启动
 *   shellx chat --no-interactive "p" 非交互 Headless 模式
 *   shellx agent list                管理 Agent
 *   shellx mcp list                  管理 MCP 服务器
 *   shellx translate "desc"          自然语言转命令
 *   shellx doctor                    检查系统状态
 *   shellx --help                    查看帮助
 * </pre>
 * <p>
 * 启动命令：java -jar teambeit-robot-shellx.jar [-c /path/to/shellx.yaml]
 */
public class ShellXApplication {
    public static void main(String[] args) {
        try {
            int exitCode = run(args);
            if (exitCode != 0) {
                System.exit(exitCode);
            }
        } catch (Exception e) {
            System.err.println(AnsiColor.error("Fatal error: " + e.getMessage()));
            if (isVerbose(args)) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }

    private static int run(String[] args) throws Exception {
        // 1. 加载配置
        ShellXConfig config = loadConfig(args);

        // 2. 初始化颜色支持
        if (!config.isColorEnabled() || !TerminalUtil.supportsColor()) {
            AnsiColor.setEnabled(false);
        }

        // 3. 处理环境变量覆盖
        applyEnvironmentOverrides(config);

        // 4. 初始化应用上下文
        ShellXContext context = new ShellXContext(config);

        try {
            // 5. 构建命令路由
            CliRouter router = buildRouter();

            // 6. 剥离全局参数后路由到子命令
            String[] commandArgs = stripGlobalArgs(args, config, context);
            return router.route(context, commandArgs);
        } finally {
            context.close();
        }
    }

    private static ShellXConfig loadConfig(String[] args) throws Exception {
        String configFile = findConfigFile(args);
        ShellXConfig config = new ShellXConfig();
        if (configFile != null && new File(configFile).exists()) {
            YamlConfigParser parser = new YamlConfigParser();
            parser.parse(config, configFile);
        }
        return config;
    }

    private static String findConfigFile(String[] args) {
        // 从命令行参数中查找 -c 或 --config
        for (int i = 0; i < args.length - 1; i++) {
            if ("-c".equals(args[i]) || "--config".equals(args[i])) {
                return args[i + 1];
            }
        }
        // 默认路径
        String home = System.getenv(ShellXConstants.ENV_HOME);
        if (home == null) {
            home = System.getProperty("user.home") + File.separator + ".shellx";
        }
        String defaultPath = home + File.separator + ShellXConstants.DEFAULT_CONFIG_FILE;
        if (new File(defaultPath).exists()) {
            return defaultPath;
        }
        // 当前目录
        if (new File(ShellXConstants.DEFAULT_CONFIG_FILE).exists()) {
            return ShellXConstants.DEFAULT_CONFIG_FILE;
        }
        return null;
    }

    private static void applyEnvironmentOverrides(ShellXConfig config) {
        String home = System.getenv(ShellXConstants.ENV_HOME);
        if (home != null && !home.isEmpty()) {
            config.setHome(home);
        }
        String logLevel = System.getenv(ShellXConstants.ENV_LOG_LEVEL);
        if (logLevel != null && !logLevel.isEmpty()) {
            config.setLogLevel(logLevel);
        }
    }

    private static CliRouter buildRouter() {
        ChatCommand chatCommand = new ChatCommand();
        CliRouter router = new CliRouter();
        router.setDefault(chatCommand)
              .register(chatCommand)
              .register(new AgentCommand())
              .register(new McpCommand())
              .register(new TranslateCommand())
              .register(new DoctorCommand())
              .register(new SettingsCommand())
              .register(new DiagnosticCommand())
              .register(new VersionCommand())
              .register(new UpdateCommand())
              .register(new LoginCommand())
              .register(new LogoutCommand());
        return router;
    }

    /**
     * 剥离全局参数（如 -c、--config、--verbose、--agent），返回子命令部分
     */
    private static String[] stripGlobalArgs(String[] args, ShellXConfig config, ShellXContext context) {
        java.util.List<String> filtered = new java.util.ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            // 跳过 -c/--config 及其值
            if ("-c".equals(arg) || "--config".equals(arg)) {
                i++; // 跳过下一个参数（值）
                continue;
            }
            // 处理 --verbose
            if ("--verbose".equals(arg) || "-v".equals(arg)) {
                config.setLogLevel("debug");
                continue;
            }
            // 处理 --agent
            if ("--agent".equals(arg) && i + 1 < args.length) {
                context.setActiveAgentId(args[i + 1]);
                i++;
                continue;
            }
            filtered.add(arg);
        }
        return filtered.toArray(new String[0]);
    }

    private static boolean isVerbose(String[] args) {
        for (String arg : args) {
            if ("--verbose".equals(arg) || "-v".equals(arg)) {
                return true;
            }
        }
        return false;
    }
}
