package cloud.apposs.robot.shellx.cli;

/**
 * 命令行选项定义
 */
public class CliOption {
    private final String longName;
    private final String shortName;
    private final String description;
    private final boolean requiresValue;
    private final String defaultValue;

    public CliOption(String longName, String shortName, String description, boolean requiresValue, String defaultValue) {
        this.longName = longName;
        this.shortName = shortName;
        this.description = description;
        this.requiresValue = requiresValue;
        this.defaultValue = defaultValue;
    }

    public static CliOption flag(String longName, String shortName, String description) {
        return new CliOption(longName, shortName, description, false, null);
    }

    public static CliOption option(String longName, String shortName, String description) {
        return new CliOption(longName, shortName, description, true, null);
    }

    public static CliOption option(String longName, String shortName, String description, String defaultValue) {
        return new CliOption(longName, shortName, description, true, defaultValue);
    }

    public String getLongName() {
        return longName;
    }

    public String getShortName() {
        return shortName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRequiresValue() {
        return requiresValue;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * 匹配给定的参数名是否对应此选项
     */
    public boolean matches(String arg) {
        if (arg == null) {
            return false;
        }
        if (longName != null && arg.equals("--" + longName)) {
            return true;
        }
        if (shortName != null && arg.equals("-" + shortName)) {
            return true;
        }
        return false;
    }
}
