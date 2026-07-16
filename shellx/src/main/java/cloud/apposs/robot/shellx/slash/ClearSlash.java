package cloud.apposs.robot.shellx.slash;

import cloud.apposs.robot.shellx.ShellXContext;
import cloud.apposs.robot.shellx.session.ChatSession;
import cloud.apposs.robot.shellx.util.TerminalUtil;

/**
 * /clear 命令 - 清屏（不删除会话数据）
 */
public class ClearSlash implements ISlashCommand {
    @Override
    public String name() {
        return "/clear";
    }

    @Override
    public String description() {
        return "Clear screen without deleting session data";
    }

    @Override
    public String execute(ShellXContext context, ChatSession session, String args) {
        TerminalUtil.clearScreen();
        return null;
    }
}
