package cloud.apposs.robot.harness;

import cloud.apposs.robot.harness.command.ConsolidateCommand;
import cloud.apposs.robot.harness.command.HelpCommand;
import cloud.apposs.robot.harness.command.ICommand;
import cloud.apposs.util.Table;

import java.util.HashMap;
import java.util.Map;

public final class HarnessCommands {
    private static final Map<String, ICommand> BUILDIN_COMMANDS = new HashMap<String, ICommand>() {{
        put(HelpCommand.NAME, new HelpCommand());
        put(ConsolidateCommand.NAME, new ConsolidateCommand());
    }};

    private final Map<String, ICommand> commands = new HashMap<>();

    public HarnessCommands(HarnessWorker worker) {
        for (Map.Entry<String, ICommand> command : BUILDIN_COMMANDS.entrySet()) {
            commands.put(command.getKey(), command.getValue());
        }
    }

    /**
     * 注册自定义命令
     *
     * @param command 用户定义的命令实现
     */
    public void addCommand(ICommand command) {
        if (command != null && command.name() != null) {
            commands.put(command.name(), command);
        }
    }

    public String runCommand(HarnessWorker worker, String sessionId, String commandName, Table<String> parameters) {
        ICommand command = commands.get(commandName);
        if (command != null) {
            return command.run(worker, sessionId, parameters);
        } else {
            return "Command not found: " + commandName;
        }
    }

    /**
     * 判断用户发送的消息是否为命令
     *
     * @param  message 用户发送的消息
     * @return 如果消息以 "/" 开头且命令名称在注册的命令列表中，则返回 true；否则返回 false
     */
    public boolean isCommand(String message) {
        if (message == null || !message.startsWith("/")) {
            return false;
        }
        String commandName = message.trim().split("\\s+")[0];
        return commands.containsKey(commandName);
    }

    /**
     * 从消息中解析命令名称，如 "/work aa bb" 返回 "/work"
     *
     * @param  message 用户发送的消息
     * @return 命令名称
     */
    public String parseCommandName(String message) {
        return message.trim().split("\\s+")[0];
    }

    /**
     * 从消息中解析命令参数，如 "/work aa bb" 返回包含 "aa"、"bb" 的 Table
     *
     * @param  message 用户发送的消息
     * @return 命令参数列表
     */
    public Table<String> parseCommandArgs(String message) {
        Table<String> arguments = Table.builder();
        String[] parts = message.trim().split("\\s+");
        for (int i = 1; i < parts.length; i++) {
            arguments.setObject(parts[i]);
        }
        return arguments;
    }
}
