package cloud.apposs.robot.shellx.cli;

import java.util.*;

/**
 * 命令行参数解析器，支持解析如下格式：
 * <pre>
 *   shellx chat "prompt" --no-interactive --agent my-agent --effort high
 *   shellx agent list
 *   shellx mcp add --name server --command "node server.js"
 * </pre>
 */
public class CliParser {
    /** 解析后的命名选项 */
    private final Map<String, String> options = new LinkedHashMap<>();

    /** 解析后的位置参数（非选项参数） */
    private final List<String> positionalArgs = new ArrayList<>();

    /** 解析后的 flags（布尔选项） */
    private final Set<String> flags = new HashSet<>();

    /** 原始参数 */
    private final String[] rawArgs;

    public CliParser(String[] args) {
        this.rawArgs = args != null ? args : new String[0];
    }

    /**
     * 根据预定义的选项列表解析命令行参数
     *
     * @param definedOptions 预定义选项
     * @return this
     */
    public CliParser parse(List<CliOption> definedOptions) {
        int i = 0;
        while (i < rawArgs.length) {
            String arg = rawArgs[i];
            if (arg.startsWith("--") || (arg.startsWith("-") && arg.length() == 2)) {
                // 查找匹配的选项定义
                CliOption matched = findOption(definedOptions, arg);
                if (matched != null) {
                    if (matched.isRequiresValue()) {
                        if (i + 1 < rawArgs.length) {
                            options.put(matched.getLongName(), rawArgs[i + 1]);
                            i += 2;
                        } else {
                            // 缺少值，当作 flag
                            flags.add(matched.getLongName());
                            i++;
                        }
                    } else {
                        flags.add(matched.getLongName());
                        i++;
                    }
                } else {
                    // 未定义的选项，尝试解析为 key=value 或 flag
                    String key = arg.startsWith("--") ? arg.substring(2) : arg.substring(1);
                    if (key.contains("=")) {
                        String[] parts = key.split("=", 2);
                        options.put(parts[0], parts[1]);
                    } else {
                        flags.add(key);
                    }
                    i++;
                }
            } else {
                positionalArgs.add(arg);
                i++;
            }
        }
        return this;
    }

    /**
     * 简单解析（不需要预定义选项）
     */
    public CliParser parse() {
        return parse(Collections.<CliOption>emptyList());
    }

    public String getOption(String name) {
        return options.get(name);
    }

    public String getOption(String name, String defaultValue) {
        String value = options.get(name);
        return value != null ? value : defaultValue;
    }

    public boolean hasFlag(String name) {
        return flags.contains(name);
    }

    public boolean hasOption(String name) {
        return options.containsKey(name);
    }

    public List<String> getPositionalArgs() {
        return Collections.unmodifiableList(positionalArgs);
    }

    /**
     * 获取子命令名（第一个位置参数）
     */
    public String getSubCommand() {
        return positionalArgs.isEmpty() ? null : positionalArgs.get(0);
    }

    /**
     * 获取子命令后的剩余位置参数
     */
    public List<String> getSubArgs() {
        if (positionalArgs.size() <= 1) {
            return Collections.emptyList();
        }
        return positionalArgs.subList(1, positionalArgs.size());
    }

    public String[] getRawArgs() {
        return rawArgs;
    }

    private CliOption findOption(List<CliOption> definedOptions, String arg) {
        for (CliOption option : definedOptions) {
            if (option.matches(arg)) {
                return option;
            }
        }
        return null;
    }
}
