package cloud.apposs.robot.shellx.command;

import cloud.apposs.robot.shellx.ShellXConfig;
import cloud.apposs.robot.shellx.ShellXConstants;
import cloud.apposs.robot.shellx.ShellXContext;
import cloud.apposs.robot.shellx.cli.CliCommand;
import cloud.apposs.robot.shellx.cli.CliOption;
import cloud.apposs.robot.shellx.cli.CliParser;
import cloud.apposs.robot.shellx.util.AnsiColor;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * doctor 子命令 - 检查并修复常见问题
 * <pre>
 * 用法：
 *   shellx doctor         运行基础检查
 *   shellx doctor --all   运行所有检查
 * </pre>
 */
public class DoctorCommand implements CliCommand {
    @Override
    public String name() {
        return "doctor";
    }

    @Override
    public String description() {
        return "Check and fix common issues";
    }

    @Override
    public List<CliOption> options() {
        return Arrays.asList(
            CliOption.flag("all", null, "Run all checks including optional ones"),
            CliOption.flag("fix", null, "Attempt to fix found issues"),
            CliOption.flag("help", "h", "Show help")
        );
    }

    @Override
    public int execute(ShellXContext context, CliParser parser) throws Exception {
        if (parser.hasFlag("help")) {
            printUsage();
            return 0;
        }

        System.out.println(AnsiColor.bold("ShellX Doctor v" + ShellXConstants.APP_VERSION));
        System.out.println(AnsiColor.dim("Running diagnostics..."));
        System.out.println();

        int issues = 0;
        ShellXConfig config = context.getConfig();

        // 1. 检查 Home 目录
        issues += checkDirectory("Home directory", config.getHome());

        // 2. 检查工作空间
        issues += checkDirectory("Workspace", config.getWorkspace());

        // 3. 检查会话目录
        issues += checkDirectory("Sessions directory", config.getSessionsPath());

        // 4. 检查 Agent 目录
        issues += checkDirectory("Agents directory", config.getAgentsPath());

        // 5. 检查 Java 版本
        issues += checkJavaVersion();

        // 6. 检查终端支持
        issues += checkTerminal();

        // 7. 检查 Agent 是否存在
        issues += checkAgents(config);

        if (parser.hasFlag("all")) {
            // 可选检查
            issues += checkNetwork();
            issues += checkApiKey();
        }

        System.out.println();
        if (issues == 0) {
            System.out.println(AnsiColor.success("All checks passed! No issues found."));
        } else {
            System.out.println(AnsiColor.warn(issues + " issue(s) found."));
            if (!parser.hasFlag("fix")) {
                System.out.println(AnsiColor.dim("Run 'shellx doctor --fix' to attempt automatic fixes."));
            }
        }

        return issues > 0 ? 1 : 0;
    }

    private int checkDirectory(String label, String path) {
        File dir = new File(path);
        if (dir.exists() && dir.isDirectory()) {
            printCheck(true, label + " exists: " + path);
            return 0;
        } else {
            printCheck(false, label + " missing: " + path);
            return 1;
        }
    }

    private int checkJavaVersion() {
        String version = System.getProperty("java.version");
        if (version != null) {
            printCheck(true, "Java version: " + version);
            return 0;
        }
        printCheck(false, "Unable to determine Java version");
        return 1;
    }

    private int checkTerminal() {
        boolean hasConsole = System.console() != null;
        if (hasConsole) {
            printCheck(true, "Interactive terminal detected");
        } else {
            printCheck(true, "Running in non-interactive mode (pipe/redirect)");
        }
        return 0;
    }

    private int checkAgents(ShellXConfig config) {
        File workspace = new File(config.getWorkspace());
        File[] agents = workspace.listFiles(File::isDirectory);
        if (agents != null && agents.length > 0) {
            printCheck(true, agents.length + " agent(s) found in workspace");
            return 0;
        } else {
            printCheck(false, "No agents found. Create one with 'shellx agent create <name>'");
            return 1;
        }
    }

    private int checkNetwork() {
        try {
            java.net.InetAddress.getByName("api.openai.com");
            printCheck(true, "Network connectivity: OK");
            return 0;
        } catch (Exception e) {
            printCheck(false, "Network connectivity: Cannot resolve API endpoints");
            return 1;
        }
    }

    private int checkApiKey() {
        String apiKey = System.getenv(ShellXConstants.ENV_API_KEY);
        if (apiKey != null && !apiKey.isEmpty()) {
            printCheck(true, "API key configured via " + ShellXConstants.ENV_API_KEY);
            return 0;
        } else {
            printCheck(false, "No API key found in " + ShellXConstants.ENV_API_KEY + " (optional for local mode)");
            return 0; // 不算错误，因为可以在 profile 中配置
        }
    }

    private void printCheck(boolean pass, String message) {
        String icon = pass ? AnsiColor.green("✓") : AnsiColor.red("✗");
        System.out.println("  " + icon + " " + message);
    }
}
