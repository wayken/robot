package cloud.apposs.robot.shellx.slash;

import cloud.apposs.robot.shellx.ShellXContext;
import cloud.apposs.robot.shellx.session.ChatSession;

/**
 * /session-id 命令 - 打印当前会话 ID
 */
public class SessionIdSlash implements ISlashCommand {
    @Override
    public String name() {
        return "/session-id";
    }

    @Override
    public String description() {
        return "Print current session ID";
    }

    @Override
    public String execute(ShellXContext context, ChatSession session, String args) {
        if (session == null) {
            return "No active session.";
        }
        return session.getId();
    }
}
