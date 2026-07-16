package cloud.apposs.robot.shellx.command;

import cloud.apposs.robot.shellx.ShellXConstants;
import cloud.apposs.robot.shellx.ShellXContext;
import cloud.apposs.robot.shellx.cli.CliCommand;
import cloud.apposs.robot.shellx.cli.CliOption;
import cloud.apposs.robot.shellx.cli.CliParser;
import cloud.apposs.robot.shellx.util.AnsiColor;

import java.util.Arrays;
import java.util.List;

/**
 * version 子命令 - 查看版本信息
 */
public class VersionCommand implements CliCommand {
    @Override
    public String name() {
        return "version";
    }

    @Override
    public String description() {
        return "Show version information";
    }

    @Override
    public List<CliOption> options() {
        return Arrays.asList(
            CliOption.flag("changelog", null, "Show changelog")
        );
    }

    @Override
    public int execute(ShellXContext context, CliParser parser) throws Exception {
        System.out.println(AnsiColor.bold(ShellXConstants.APP_NAME) + " v" + ShellXConstants.APP_VERSION);
        System.out.println("Based on Harness Framework");
        System.out.println("Java " + System.getProperty("java.version"));

        if (parser.hasFlag("changelog")) {
            System.out.println();
            System.out.println(AnsiColor.bold("Changelog:"));
            System.out.println("  v1.0.0 - Initial release");
            System.out.println("    - CLI command framework");
            System.out.println("    - Interactive TUI chat");
            System.out.println("    - Agent management");
            System.out.println("    - MCP server integration");
            System.out.println("    - Slash commands system");
        }
        return 0;
    }
}
