package cloud.apposs.robot.shellx.command;

import cloud.apposs.robot.shellx.ShellXContext;
import cloud.apposs.robot.shellx.cli.CliCommand;
import cloud.apposs.robot.shellx.cli.CliOption;
import cloud.apposs.robot.shellx.cli.CliParser;
import cloud.apposs.robot.shellx.util.AnsiColor;

import java.util.Arrays;
import java.util.List;

/**
 * translate 子命令 - 自然语言转 shell 命令
 * <pre>
 * 用法：
 *   shellx translate "find Python files modified today"
 *   shellx translate "list all docker containers"
 * </pre>
 */
public class TranslateCommand implements CliCommand {
    @Override
    public String name() {
        return "translate";
    }

    @Override
    public String description() {
        return "Translate natural language to shell commands";
    }

    @Override
    public List<CliOption> options() {
        return Arrays.asList(
            CliOption.flag("execute", "e", "Execute the translated command directly"),
            CliOption.flag("help", "h", "Show help")
        );
    }

    @Override
    public int execute(ShellXContext context, CliParser parser) throws Exception {
        if (parser.hasFlag("help")) {
            printUsage();
            return 0;
        }

        List<String> positional = parser.getPositionalArgs();
        if (positional.isEmpty()) {
            System.err.println("Usage: shellx translate \"natural language description\"");
            return 1;
        }

        StringBuilder query = new StringBuilder();
        for (String arg : positional) {
            if (query.length() > 0) query.append(" ");
            query.append(arg);
        }

        System.out.println(AnsiColor.dim("Translating: " + query));
        System.out.println();

        // TODO: 调用 AI Agent 进行翻译，需要专用的 translate prompt
        // 临时输出占位
        System.out.println(AnsiColor.bold("Suggested command:"));
        System.out.println(AnsiColor.cyan("  # Translation requires an active agent with translate skill"));
        System.out.println();

        if (parser.hasFlag("execute")) {
            System.out.println(AnsiColor.warn("Auto-execute mode. Please confirm before running."));
        }

        return 0;
    }

    @Override
    public void printUsage() {
        System.out.println("Usage: shellx translate [options] \"description\"");
        System.out.println("  Translate natural language to shell commands");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -e, --execute    Execute the translated command directly");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  shellx translate \"find all Python files\"");
        System.out.println("  shellx translate \"list docker containers\" -e");
    }
}
