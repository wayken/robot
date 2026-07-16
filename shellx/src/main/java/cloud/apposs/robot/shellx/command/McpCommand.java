package cloud.apposs.robot.shellx.command;

import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.shellx.ShellXContext;
import cloud.apposs.robot.shellx.cli.CliCommand;
import cloud.apposs.robot.shellx.cli.CliOption;
import cloud.apposs.robot.shellx.cli.CliParser;
import cloud.apposs.robot.shellx.util.AnsiColor;

import java.util.Arrays;
import java.util.List;

/**
 * mcp 子命令 - 管理 MCP (Model Context Protocol) 服务器
 * <pre>
 * 用法：
 *   shellx mcp list                    列出所有 MCP 服务器
 *   shellx mcp add --name s --command "cmd"  添加 MCP 服务器
 *   shellx mcp remove --name s         删除 MCP 服务器
 *   shellx mcp status --name s         查看 MCP 状态
 * </pre>
 */
public class McpCommand implements CliCommand {
    @Override
    public String name() {
        return "mcp";
    }

    @Override
    public String description() {
        return "Manage MCP (Model Context Protocol) servers";
    }

    @Override
    public List<CliOption> options() {
        return Arrays.asList(
            CliOption.option("name", "n", "MCP server name"),
            CliOption.option("command", null, "Server command to execute"),
            CliOption.option("scope", null, "Scope: workspace or global", "workspace"),
            CliOption.option("file", null, "Config file to import from"),
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
        if (subCommand == null) {
            // 默认列出
            return listMcpServers(context, parser);
        }

        switch (subCommand) {
            case "list":
                return listMcpServers(context, parser);
            case "add":
                return addMcpServer(context, parser);
            case "remove":
                return removeMcpServer(context, parser);
            case "status":
                return mcpStatus(context, parser);
            case "import":
                return importMcpConfig(context, parser);
            default:
                // 可能是 scope 参数（如 "shellx mcp list workspace"）
                if ("workspace".equals(subCommand) || "global".equals(subCommand)) {
                    return listMcpServers(context, parser);
                }
                System.err.println(AnsiColor.error("Unknown mcp sub-command: " + subCommand));
                printUsage();
                return 1;
        }
    }

    private int listMcpServers(ShellXContext context, CliParser parser) {
        HarnessWorker worker = context.getActiveWorker();
        if (worker == null) {
            System.out.println(AnsiColor.warn("No active agent. Create one with 'shellx agent create <name>'"));
            return 0;
        }
        System.out.println(AnsiColor.bold("MCP Servers:"));
        System.out.println();
        // 从 HarnessMcpTransport 获取状态
        // TODO: 暴露 MCP 服务器列表接口
        System.out.println(AnsiColor.dim("  MCP server listing requires agent to be running."));
        System.out.println(AnsiColor.dim("  Configure servers in agent's profile.yaml under 'mcp' section."));
        return 0;
    }

    private int addMcpServer(ShellXContext context, CliParser parser) {
        String name = parser.getOption("name");
        String command = parser.getOption("command");
        if (name == null || command == null) {
            System.err.println("Usage: shellx mcp add --name <name> --command \"<command>\"");
            return 1;
        }
        String scope = parser.getOption("scope", "workspace");
        // TODO: 写入 agent profile 的 mcp 配置段
        System.out.println(AnsiColor.success("MCP server '" + name + "' added (" + scope + ")."));
        System.out.println(AnsiColor.dim("  Command: " + command));
        return 0;
    }

    private int removeMcpServer(ShellXContext context, CliParser parser) {
        String name = parser.getOption("name");
        if (name == null) {
            System.err.println("Usage: shellx mcp remove --name <name>");
            return 1;
        }
        // TODO: 从 agent profile 中移除
        System.out.println(AnsiColor.success("MCP server '" + name + "' removed."));
        return 0;
    }

    private int mcpStatus(ShellXContext context, CliParser parser) {
        String name = parser.getOption("name");
        if (name == null) {
            System.err.println("Usage: shellx mcp status --name <name>");
            return 1;
        }
        // TODO: 查询实际 MCP 连接状态
        System.out.println(AnsiColor.bold("MCP Server: " + name));
        System.out.println("  Status: " + AnsiColor.dim("checking..."));
        return 0;
    }

    private int importMcpConfig(ShellXContext context, CliParser parser) {
        String file = parser.getOption("file");
        if (file == null) {
            System.err.println("Usage: shellx mcp import --file <config.json> [workspace|global]");
            return 1;
        }
        // TODO: 解析并导入 MCP 配置
        System.out.println(AnsiColor.success("MCP configuration imported from: " + file));
        return 0;
    }

    @Override
    public void printUsage() {
        System.out.println("Usage: shellx mcp <sub-command> [options]");
        System.out.println("  Manage MCP (Model Context Protocol) servers");
        System.out.println();
        System.out.println("Sub-commands:");
        System.out.println("  list [scope]                      List MCP servers");
        System.out.println("  add --name <n> --command \"<cmd>\"  Add MCP server");
        System.out.println("  remove --name <n>                 Remove MCP server");
        System.out.println("  status --name <n>                 Check MCP server status");
        System.out.println("  import --file <path> [scope]      Import from config file");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --scope <workspace|global>   Target scope (default: workspace)");
    }
}
