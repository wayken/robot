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
 * agent 子命令 - 管理 AI Agent 配置
 * <pre>
 * 用法：
 *   shellx agent list              列出所有 agents
 *   shellx agent create <name>     创建 agent
 *   shellx agent edit <name>       编辑 agent
 *   shellx agent validate <path>   校验配置
 *   shellx agent set-default <name> 设置默认 agent
 * </pre>
 */
public class AgentCommand implements CliCommand {
    @Override
    public String name() {
        return "agent";
    }

    @Override
    public String description() {
        return "Manage AI Agent configurations";
    }

    @Override
    public List<CliOption> options() {
        return Arrays.asList(
            CliOption.flag("help", "h", "Show agent command help")
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
            printUsage();
            return 0;
        }

        switch (subCommand) {
            case "list":
                return listAgents(context);
            case "create":
                return createAgent(context, parser);
            case "edit":
                return editAgent(context, parser);
            case "validate":
                return validateAgent(context, parser);
            case "set-default":
                return setDefault(context, parser);
            default:
                System.err.println(AnsiColor.error("Unknown agent sub-command: " + subCommand));
                printUsage();
                return 1;
        }
    }

    private int listAgents(ShellXContext context) {
        System.out.println(AnsiColor.bold("Registered Agents:"));
        System.out.println();
        // 通过 HarnessFramework 列出所有 worker
        // 由于 HarnessFramework 未暴露 workers 列表，使用配置目录扫描
        java.io.File agentsDir = new java.io.File(context.getConfig().getWorkspace());
        if (!agentsDir.exists()) {
            System.out.println("  (no agents found)");
            return 0;
        }
        java.io.File[] dirs = agentsDir.listFiles(java.io.File::isDirectory);
        if (dirs == null || dirs.length == 0) {
            System.out.println("  (no agents found)");
            return 0;
        }
        String defaultAgent = context.getConfig().getDefaultAgent();
        for (java.io.File dir : dirs) {
            String agentId = dir.getName();
            boolean isDefault = agentId.equals(defaultAgent);
            String marker = isDefault ? AnsiColor.green("* ") : "  ";
            HarnessWorker worker = context.getFramework().getWorker(agentId);
            String status = worker != null ? AnsiColor.green("active") : AnsiColor.dim("inactive");
            System.out.printf("%s%-20s %s%n", marker, agentId, status);
        }
        System.out.println();
        System.out.println(AnsiColor.dim("  * = default agent"));
        return 0;
    }

    private int createAgent(ShellXContext context, CliParser parser) throws Exception {
        List<String> subArgs = parser.getSubArgs();
        if (subArgs.isEmpty()) {
            System.err.println("Usage: shellx agent create <agent-name>");
            return 1;
        }
        String agentName = subArgs.get(0);
        // 使用 HarnessFramework 初始化新的工作空间
        cloud.apposs.robot.harness.struct.WorkerStruct model = new cloud.apposs.robot.harness.struct.WorkerStruct();
        model.setId(agentName);
        try {
            context.getFramework().initialize(model);
            System.out.println(AnsiColor.success("Agent '" + agentName + "' created successfully."));
            System.out.println(AnsiColor.dim("  Workspace: " + context.getConfig().getWorkspace() + "/" + agentName));
        } catch (Exception e) {
            System.err.println(AnsiColor.error("Failed to create agent: " + e.getMessage()));
            return 1;
        }
        return 0;
    }

    private int editAgent(ShellXContext context, CliParser parser) {
        List<String> subArgs = parser.getSubArgs();
        String agentName = subArgs.isEmpty() ? context.getActiveAgentId() : subArgs.get(0);

        String profilePath = context.getConfig().getWorkspace() + "/" + agentName + "/profile.yaml";
        java.io.File profileFile = new java.io.File(profilePath);
        if (!profileFile.exists()) {
            System.err.println(AnsiColor.error("Agent not found: " + agentName));
            return 1;
        }

        // 使用 $EDITOR 打开配置文件
        String editor = System.getenv("EDITOR");
        if (editor == null || editor.isEmpty()) {
            editor = "vi";
        }
        System.out.println(AnsiColor.info("Opening " + profilePath + " with " + editor));
        try {
            ProcessBuilder pb = new ProcessBuilder(editor, profilePath);
            pb.inheritIO();
            Process p = pb.start();
            p.waitFor();
            // 编辑完成后重载配置
            context.getFramework().reload(agentName);
            System.out.println(AnsiColor.success("Agent configuration reloaded."));
        } catch (Exception e) {
            System.err.println(AnsiColor.error("Failed to edit agent: " + e.getMessage()));
            return 1;
        }
        return 0;
    }

    private int validateAgent(ShellXContext context, CliParser parser) {
        List<String> subArgs = parser.getSubArgs();
        if (subArgs.isEmpty()) {
            System.err.println("Usage: shellx agent validate <profile-path>");
            return 1;
        }
        String path = subArgs.get(0);
        java.io.File file = new java.io.File(path);
        if (!file.exists()) {
            System.err.println(AnsiColor.error("File not found: " + path));
            return 1;
        }
        // 尝试解析配置文件验证格式
        try {
            cloud.apposs.configure.YamlConfigParser parser2 = new cloud.apposs.configure.YamlConfigParser();
            cloud.apposs.robot.harness.HarnessWorkerProfile profile = new cloud.apposs.robot.harness.HarnessWorkerProfile();
            parser2.parse(profile, path);
            System.out.println(AnsiColor.success("Configuration is valid."));
        } catch (Exception e) {
            System.err.println(AnsiColor.error("Validation failed: " + e.getMessage()));
            return 1;
        }
        return 0;
    }

    private int setDefault(ShellXContext context, CliParser parser) {
        List<String> subArgs = parser.getSubArgs();
        if (subArgs.isEmpty()) {
            System.err.println("Usage: shellx agent set-default <agent-name>");
            return 1;
        }
        String agentName = subArgs.get(0);
        // 检查 agent 是否存在
        String workspace = context.getConfig().getWorkspace() + "/" + agentName;
        if (!new java.io.File(workspace).exists()) {
            System.err.println(AnsiColor.error("Agent not found: " + agentName));
            return 1;
        }
        context.getConfig().setDefaultAgent(agentName);
        System.out.println(AnsiColor.success("Default agent set to: " + agentName));
        return 0;
    }

    @Override
    public void printUsage() {
        System.out.println("Usage: shellx agent <sub-command> [options]");
        System.out.println("  Manage AI Agent configurations");
        System.out.println();
        System.out.println("Sub-commands:");
        System.out.println("  list                 List all registered agents");
        System.out.println("  create <name>        Create a new agent");
        System.out.println("  edit [name]          Edit agent profile (uses $EDITOR)");
        System.out.println("  validate <path>      Validate agent configuration file");
        System.out.println("  set-default <name>   Set the default agent");
    }
}
