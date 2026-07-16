package cloud.apposs.robot.shellx.slash;

import java.util.*;

/**
 * Slash Command 注册表，管理所有交互内命令的注册和查找
 */
public class SlashRegistry {
    private final Map<String, ISlashCommand> commands = new LinkedHashMap<>();

    /**
     * 注册一个 Slash Command
     */
    public SlashRegistry register(ISlashCommand command) {
        commands.put(command.name(), command);
        // 注册别名
        for (String alias : command.aliases()) {
            commands.put(alias, command);
        }
        return this;
    }

    /**
     * 获取命令
     */
    public ISlashCommand getCommand(String name) {
        return commands.get(name);
    }

    /**
     * 判断是否为已注册的 Slash Command
     */
    public boolean isCommand(String input) {
        if (input == null || !input.startsWith("/")) {
            return false;
        }
        String commandName = input.split("\\s+")[0];
        return commands.containsKey(commandName);
    }

    /**
     * 获取所有唯一命令（排除别名重复）
     */
    public List<ISlashCommand> getAllCommands() {
        Set<ISlashCommand> unique = new LinkedHashSet<>(commands.values());
        return new ArrayList<>(unique);
    }

    /**
     * 获取命令名称列表（用于自动补全）
     */
    public List<String> getCommandNames() {
        return new ArrayList<>(commands.keySet());
    }
}
