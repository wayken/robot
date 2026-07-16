package cloud.apposs.robot.shellx.command;

import cloud.apposs.robot.shellx.ShellXConstants;
import cloud.apposs.robot.shellx.ShellXContext;
import cloud.apposs.robot.shellx.cli.CliCommand;
import cloud.apposs.robot.shellx.cli.CliOption;
import cloud.apposs.robot.shellx.cli.CliParser;
import cloud.apposs.robot.shellx.util.AnsiColor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * diagnostic 子命令 - 生成诊断报告
 * <pre>
 * 用法：
 *   shellx diagnostic           生成诊断报告
 *   shellx diagnostic --force   强制重新生成
 * </pre>
 */
public class DiagnosticCommand implements CliCommand {
    @Override
    public String name() {
        return "diagnostic";
    }

    @Override
    public String description() {
        return "Generate a diagnostic report for troubleshooting";
    }

    @Override
    public List<CliOption> options() {
        return Arrays.asList(
            CliOption.flag("force", null, "Force regenerate diagnostic report"),
            CliOption.flag("help", "h", "Show help")
        );
    }

    @Override
    public int execute(ShellXContext context, CliParser parser) throws Exception {
        if (parser.hasFlag("help")) {
            printUsage();
            return 0;
        }

        System.out.println(AnsiColor.bold("Generating diagnostic report..."));
        System.out.println();

        String reportPath = context.getConfig().getLogsPath() + File.separator + "diagnostic-"
                + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".txt";

        try (PrintWriter writer = new PrintWriter(new FileWriter(reportPath))) {
            writer.println("ShellX Diagnostic Report");
            writer.println("Generated: " + new Date());
            writer.println("Version: " + ShellXConstants.APP_VERSION);
            writer.println("=========================================");
            writer.println();

            // 系统信息
            writer.println("[System Information]");
            writer.printf("  OS: %s %s (%s)%n", System.getProperty("os.name"),
                    System.getProperty("os.version"), System.getProperty("os.arch"));
            writer.printf("  Java: %s (%s)%n", System.getProperty("java.version"),
                    System.getProperty("java.vendor"));
            writer.printf("  User: %s%n", System.getProperty("user.name"));
            writer.printf("  Home: %s%n", System.getProperty("user.home"));
            writer.printf("  CWD: %s%n", System.getProperty("user.dir"));
            writer.println();

            // ShellX 配置
            writer.println("[ShellX Configuration]");
            writer.printf("  Home: %s%n", context.getConfig().getHome());
            writer.printf("  Workspace: %s%n", context.getConfig().getWorkspace());
            writer.printf("  Default Agent: %s%n", context.getConfig().getDefaultAgent());
            writer.printf("  Effort: %s%n", context.getConfig().getEffort());
            writer.printf("  Log Level: %s%n", context.getConfig().getLogLevel());
            writer.println();

            // 环境变量
            writer.println("[Environment Variables]");
            String[] envVars = {ShellXConstants.ENV_API_KEY, ShellXConstants.ENV_HOME,
                    ShellXConstants.ENV_LOG_LEVEL, "HTTP_PROXY", "HTTPS_PROXY"};
            for (String env : envVars) {
                String value = System.getenv(env);
                if (value != null && env.contains("KEY")) {
                    value = "***" + value.substring(Math.max(0, value.length() - 4));
                }
                writer.printf("  %s: %s%n", env, value != null ? value : "(not set)");
            }
            writer.println();

            // Agent 信息
            writer.println("[Agents]");
            File workspace = new File(context.getConfig().getWorkspace());
            File[] agents = workspace.listFiles(File::isDirectory);
            if (agents != null) {
                for (File agent : agents) {
                    writer.printf("  %s%n", agent.getName());
                }
            } else {
                writer.println("  (none)");
            }
            writer.println();

            writer.println("[End of Report]");
        } catch (IOException e) {
            System.err.println(AnsiColor.error("Failed to write report: " + e.getMessage()));
            return 1;
        }

        System.out.println(AnsiColor.success("Diagnostic report saved to:"));
        System.out.println("  " + reportPath);
        System.out.println();
        System.out.println(AnsiColor.dim("Share this file when reporting issues."));
        return 0;
    }
}
