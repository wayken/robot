package cloud.apposs.robot.shellx.tui;

import cloud.apposs.robot.shellx.ShellXConstants;
import cloud.apposs.robot.shellx.util.AnsiColor;
import cloud.apposs.robot.shellx.util.TerminalUtil;

/**
 * TUI 渲染器，负责格式化输出到终端
 */
public class TuiRenderer {
    private final boolean colorEnabled;

    public TuiRenderer(boolean colorEnabled) {
        this.colorEnabled = colorEnabled;
        AnsiColor.setEnabled(colorEnabled && TerminalUtil.supportsColor());
    }

    /**
     * 打印 Banner
     */
    public void printBanner() {
        System.out.println(AnsiColor.bold(AnsiColor.CYAN + "╭─────────────────────────────────────╮" + AnsiColor.RESET));
        System.out.println(AnsiColor.bold(AnsiColor.CYAN + "│" + AnsiColor.RESET + "   ShellX - AI Agent CLI v" + ShellXConstants.APP_VERSION + "      " + AnsiColor.bold(AnsiColor.CYAN + "│" + AnsiColor.RESET)));
        System.out.println(AnsiColor.bold(AnsiColor.CYAN + "│" + AnsiColor.RESET + "   Based on Harness Framework      " + AnsiColor.bold(AnsiColor.CYAN + "│" + AnsiColor.RESET)));
        System.out.println(AnsiColor.bold(AnsiColor.CYAN + "╰─────────────────────────────────────╯" + AnsiColor.RESET));
        System.out.println();
    }

    /**
     * 打印欢迎信息
     */
    public void printWelcome(String agentId) {
        System.out.println(AnsiColor.dim("Agent: " + agentId + " | Type /help for commands, /quit to exit"));
        System.out.println();
    }

    /**
     * 打印用户输入提示符
     */
    public void printPrompt() {
        System.out.print(AnsiColor.bold(AnsiColor.GREEN + ShellXConstants.PROMPT + AnsiColor.RESET));
        System.out.flush();
    }

    /**
     * 打印 AI 回复
     */
    public void printAIResponse(String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        System.out.println();
        // 逐行输出，添加缩进
        String[] lines = content.split("\n");
        for (String line : lines) {
            System.out.println(ShellXConstants.AI_PREFIX + line);
        }
        System.out.println();
    }

    /**
     * 打印流式 AI 回复的一段文本
     */
    public void printStreamChunk(String chunk) {
        System.out.print(chunk);
        System.out.flush();
    }

    /**
     * 结束流式回复
     */
    public void endStream() {
        System.out.println();
        System.out.println();
    }

    /**
     * 打印错误信息
     */
    public void printError(String message) {
        System.out.println(AnsiColor.error("Error: " + message));
    }

    /**
     * 打印警告信息
     */
    public void printWarning(String message) {
        System.out.println(AnsiColor.warn("Warning: " + message));
    }

    /**
     * 打印成功信息
     */
    public void printSuccess(String message) {
        System.out.println(AnsiColor.success("✓ " + message));
    }

    /**
     * 打印信息
     */
    public void printInfo(String message) {
        System.out.println(AnsiColor.info(message));
    }

    /**
     * 打印分隔线
     */
    public void printSeparator() {
        int width = TerminalUtil.getTerminalWidth();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(width, 60); i++) {
            sb.append('─');
        }
        System.out.println(AnsiColor.dim(sb.toString()));
    }

    /**
     * 打印工具调用信息
     */
    public void printToolCall(String toolName, String input) {
        System.out.println(AnsiColor.dim("  ⚙ " + toolName));
    }

    /**
     * 打印思考中
     */
    public void printThinking() {
        System.out.print(AnsiColor.dim("  Thinking..."));
        System.out.flush();
    }

    /**
     * 清除思考中提示
     */
    public void clearThinking() {
        TerminalUtil.clearLine();
    }
}
