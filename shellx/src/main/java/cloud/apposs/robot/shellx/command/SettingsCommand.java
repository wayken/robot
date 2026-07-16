package cloud.apposs.robot.shellx.command;

import cloud.apposs.robot.shellx.ShellXConfig;
import cloud.apposs.robot.shellx.ShellXContext;
import cloud.apposs.robot.shellx.cli.CliCommand;
import cloud.apposs.robot.shellx.cli.CliOption;
import cloud.apposs.robot.shellx.cli.CliParser;
import cloud.apposs.robot.shellx.util.AnsiColor;

import java.util.Arrays;
import java.util.List;

/**
 * settings 子命令 - 管理配置
 * <pre>
 * 用法：
 *   shellx settings list         列出当前配置
 *   shellx settings list --all   列出所有配置（含默认值）
 *   shellx settings set <key> <value>  设置配置项
 * </pre>
 */
public class SettingsCommand implements CliCommand {
    @Override
    public String name() {
        return "settings";
    }

    @Override
    public String description() {
        return "Manage ShellX configuration settings";
    }

    @Override
    public List<CliOption> options() {
        return Arrays.asList(
            CliOption.flag("all", null, "Show all settings including defaults"),
            CliOption.flag("help", "h", "Show help")
        );
    }

    @Override
    public int execute(ShellXContext context, CliParser parser) throws Exception {
        if (parser.hasFlag("help")) {
            printUsage();
            return 0;
        }

        String subCommand = parser.getSubCommand();
        if (subCommand == null || "list".equals(subCommand)) {
            return listSettings(context, parser.hasFlag("all"));
        } else if ("set".equals(subCommand)) {
            return setSetting(context, parser);
        } else {
            System.err.println(AnsiColor.error("Unknown settings sub-command: " + subCommand));
            return 1;
        }
    }

    private int listSettings(ShellXContext context, boolean showAll) {
        ShellXConfig config = context.getConfig();
        System.out.println(AnsiColor.bold("ShellX Settings:"));
        System.out.println();
        System.out.printf("  %-24s %s%n", "home", config.getHome());
        System.out.printf("  %-24s %s%n", "default_agent", config.getDefaultAgent());
        System.out.printf("  %-24s %s%n", "effort", config.getEffort());
        System.out.printf("  %-24s %s%n", "display.color", String.valueOf(config.isColorEnabled()));
        System.out.printf("  %-24s %s%n", "display.wrap", config.getWrapMode());
        System.out.printf("  %-24s %s%n", "logger.level", config.getLogLevel());

        if (showAll) {
            System.out.println();
            System.out.println(AnsiColor.dim("  --- Advanced ---"));
            System.out.printf("  %-24s %s%n", "request_log", String.valueOf(config.isRequestLog()));
            System.out.printf("  %-24s %d%n", "proxy_pool.pool_size", config.getProxyPoolSize());
            System.out.printf("  %-24s %d ms%n", "proxy_pool.connect_timeout", config.getProxyConnectTimeout());
            System.out.printf("  %-24s %d ms%n", "proxy_pool.socket_timeout", config.getProxySocketTimeout());
        }
        return 0;
    }

    private int setSetting(ShellXContext context, CliParser parser) {
        List<String> subArgs = parser.getSubArgs();
        if (subArgs.size() < 2) {
            System.err.println("Usage: shellx settings set <key> <value>");
            return 1;
        }
        String key = subArgs.get(0);
        String value = subArgs.get(1);
        ShellXConfig config = context.getConfig();

        switch (key) {
            case "effort":
                config.setEffort(value);
                break;
            case "display.color":
                config.setColorEnabled(Boolean.parseBoolean(value));
                break;
            case "display.wrap":
                config.setWrapMode(value);
                break;
            case "logger.level":
                config.setLogLevel(value);
                break;
            case "default_agent":
                config.setDefaultAgent(value);
                break;
            default:
                System.err.println(AnsiColor.error("Unknown setting: " + key));
                return 1;
        }
        System.out.println(AnsiColor.success(key + " = " + value));
        return 0;
    }

    @Override
    public void printUsage() {
        System.out.println("Usage: shellx settings <sub-command> [options]");
        System.out.println("  Manage ShellX configuration settings");
        System.out.println();
        System.out.println("Sub-commands:");
        System.out.println("  list [--all]          List current settings");
        System.out.println("  set <key> <value>     Update a setting");
        System.out.println();
        System.out.println("Available keys:");
        System.out.println("  effort, display.color, display.wrap, logger.level, default_agent");
    }
}
