package cloud.apposs.robot.shellx.cli;

import cloud.apposs.robot.shellx.ShellXConstants;
import cloud.apposs.robot.shellx.ShellXContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CLI 命令路由器，解析顶层参数并将子命令分发到对应的 CliCommand 实现
 */
public class CliRouter {
    private final Map<String, CliCommand> commands = new LinkedHashMap<>();

    /** 默认命令（无子命令时执行） */
    private CliCommand defaultCommand;

    public CliRouter register(CliCommand command) {
        commands.put(command.name(), command);
        return this;
    }

    public CliRouter setDefault(CliCommand command) {
        this.defaultCommand = command;
        return this;
    }

    /**
     * 路由并执行命令
     *
     * @param context 应用上下文
     * @param args    原始命令行参数
     * @return 退出码
     */
    public int route(ShellXContext context, String[] args) throws Exception {
        if (args == null || args.length == 0) {
            // 无参数，执行默认命令（chat 交互模式）
            if (defaultCommand != null) {
                return defaultCommand.execute(context, new CliParser(new String[0]).parse());
            }
            printHelp();
            return 0;
        }

        String first = args[0];

        // 处理全局标志
        if ("--help".equals(first) || "-h".equals(first)) {
            printHelp();
            return 0;
        }
        if ("--version".equals(first) || "-V".equals(first)) {
            printVersion();
            return 0;
        }
        if ("--help-all".equals(first)) {
            printHelpAll();
            return 0;
        }

        // 查找子命令
        CliCommand command = commands.get(first);
        if (command != null) {
            // 去除第一个参数（子命令名）后解析剩余参数
            String[] subArgs = new String[args.length - 1];
            System.arraycopy(args, 1, subArgs, 0, subArgs.length);
            CliParser parser = new CliParser(subArgs).parse(command.options());
            return command.execute(context, parser);
        }

        // 未匹配子命令，尝试作为 chat 命令的快捷输入
        if (!first.startsWith("-")) {
            // 如 "shellx '解释这个项目'" 等价于 "shellx chat '解释这个项目'"
            if (defaultCommand != null) {
                CliParser parser = new CliParser(args).parse(defaultCommand.options());
                return defaultCommand.execute(context, parser);
            }
        }

        // 全局参数处理
        CliParser globalParser = new CliParser(args).parse();
        if (globalParser.hasFlag("verbose") || globalParser.hasFlag("v")) {
            // 设置详细日志模式
            context.getConfig().setLogLevel("debug");
        }

        System.err.println("Unknown command: " + first);
        System.err.println("Run 'shellx --help' for usage information.");
        return 1;
    }

    public void printHelp() {
        System.out.println(ShellXConstants.APP_NAME + " v" + ShellXConstants.APP_VERSION);
        System.out.println("AI Agent command-line interface based on Harness framework");
        System.out.println();
        System.out.println("Usage: shellx [command] [options]");
        System.out.println();
        System.out.println("Commands:");
        for (Map.Entry<String, CliCommand> entry : commands.entrySet()) {
            String name = entry.getKey();
            String desc = entry.getValue().description();
            System.out.printf("  %-16s %s%n", name, desc);
        }
        System.out.println();
        System.out.println("Global Options:");
        System.out.println("  -h, --help       Show help information");
        System.out.println("  -V, --version    Show version");
        System.out.println("  -v, --verbose    Increase log verbosity");
        System.out.println("  --agent <name>   Use specified agent");
        System.out.println("  --help-all       Show all sub-commands help");
        System.out.println();
        System.out.println("Run 'shellx <command> --help' for more information on a command.");
    }

    public void printVersion() {
        System.out.println(ShellXConstants.APP_NAME + " v" + ShellXConstants.APP_VERSION);
    }

    public void printHelpAll() {
        printHelp();
        System.out.println();
        System.out.println("--- Detailed Command Help ---");
        for (CliCommand command : commands.values()) {
            System.out.println();
            command.printUsage();
        }
    }
}
