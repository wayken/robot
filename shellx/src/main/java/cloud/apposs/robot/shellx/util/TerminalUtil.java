package cloud.apposs.robot.shellx.util;

/**
 * 终端工具类，提供终端宽度检测、清屏等功能
 */
public final class TerminalUtil {
    private static final int DEFAULT_WIDTH = 80;
    private static final int DEFAULT_HEIGHT = 24;

    private TerminalUtil() {
    }

    /**
     * 获取终端宽度
     */
    public static int getTerminalWidth() {
        String columns = System.getenv("COLUMNS");
        if (columns != null) {
            try {
                return Integer.parseInt(columns.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        // 尝试通过 stty 获取（Linux/macOS）
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", "tput cols 2>/dev/null"});
            byte[] buf = new byte[32];
            int read = process.getInputStream().read(buf);
            if (read > 0) {
                return Integer.parseInt(new String(buf, 0, read).trim());
            }
        } catch (Exception ignored) {
        }
        return DEFAULT_WIDTH;
    }

    /**
     * 获取终端高度
     */
    public static int getTerminalHeight() {
        String lines = System.getenv("LINES");
        if (lines != null) {
            try {
                return Integer.parseInt(lines.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", "tput lines 2>/dev/null"});
            byte[] buf = new byte[32];
            int read = process.getInputStream().read(buf);
            if (read > 0) {
                return Integer.parseInt(new String(buf, 0, read).trim());
            }
        } catch (Exception ignored) {
        }
        return DEFAULT_HEIGHT;
    }

    /**
     * 清屏
     */
    public static void clearScreen() {
        System.out.print("\u001B[2J\u001B[H");
        System.out.flush();
    }

    /**
     * 光标移到行首
     */
    public static void carriageReturn() {
        System.out.print("\r");
        System.out.flush();
    }

    /**
     * 清除当前行
     */
    public static void clearLine() {
        System.out.print("\u001B[2K\r");
        System.out.flush();
    }

    /**
     * 光标上移 n 行
     */
    public static void cursorUp(int lines) {
        if (lines > 0) {
            System.out.print("\u001B[" + lines + "A");
            System.out.flush();
        }
    }

    /**
     * 检测当前终端是否支持 ANSI 颜色
     */
    public static boolean supportsColor() {
        // 检查常见的无颜色环境
        String term = System.getenv("TERM");
        if ("dumb".equals(term)) {
            return false;
        }
        String noColor = System.getenv("NO_COLOR");
        if (noColor != null) {
            return false;
        }
        // Windows cmd 通常不支持，但现代 Windows Terminal 支持
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            String wtSession = System.getenv("WT_SESSION");
            return wtSession != null || System.getenv("ANSICON") != null;
        }
        return true;
    }

    /**
     * 判断标准输入是否来自终端（非管道/重定向）
     */
    public static boolean isInteractiveTerminal() {
        return System.console() != null;
    }
}
