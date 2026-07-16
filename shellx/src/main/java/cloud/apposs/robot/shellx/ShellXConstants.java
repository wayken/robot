package cloud.apposs.robot.shellx;

/**
 * ShellX 模块常量定义
 */
public final class ShellXConstants {
    /** 应用名称 */
    public static final String APP_NAME = "shellx";
    /** 应用版本 */
    public static final String APP_VERSION = "1.0.0";
    /** 默认配置文件路径 */
    public static final String DEFAULT_CONFIG_FILE = "shellx.yaml";
    /** 默认工作目录 */
    public static final String DEFAULT_HOME = "~/.shellx";
    /** 默认工作空间 */
    public static final String DEFAULT_WORKSPACE = "workspace";
    /** 会话存储目录 */
    public static final String SESSIONS_DIR = "sessions";
    /** Agent 配置目录 */
    public static final String AGENTS_DIR = "agents";
    /** 日志目录 */
    public static final String LOGS_DIR = "logs";

    /** 环境变量 - API Key */
    public static final String ENV_API_KEY = "SHELLX_API_KEY";
    /** 环境变量 - Home目录 */
    public static final String ENV_HOME = "SHELLX_HOME";
    /** 环境变量 - 日志级别 */
    public static final String ENV_LOG_LEVEL = "SHELLX_LOG_LEVEL";

    /** 默认 Agent 名称 */
    public static final String DEFAULT_AGENT = "default";

    /** TUI 提示符 */
    public static final String PROMPT = "> ";
    /** TUI AI回复前缀 */
    public static final String AI_PREFIX = "  ";

    private ShellXConstants() {
    }
}
