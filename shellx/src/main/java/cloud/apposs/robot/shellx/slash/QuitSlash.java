package cloud.apposs.robot.shellx.slash;

import cloud.apposs.robot.shellx.ShellXContext;
import cloud.apposs.robot.shellx.session.ChatSession;

/**
 * /quit 命令 - 退出交互模式
 */
public class QuitSlash implements ISlashCommand {
    @Override
    public String name() {
        return "/quit";
    }

    @Override
    public String[] aliases() {
        return new String[]{"/exit", "/q"};
    }

    @Override
    public String description() {
        return "Exit the interactive session";
    }

    @Override
    public String execute(ShellXContext context, ChatSession session, String args) {
        // 实际退出逻辑在 TuiEngine 中处理
        return null;
    }
}
