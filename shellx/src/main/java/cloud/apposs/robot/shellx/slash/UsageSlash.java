package cloud.apposs.robot.shellx.slash;

import cloud.apposs.robot.shellx.ShellXContext;
import cloud.apposs.robot.shellx.session.ChatSession;
import cloud.apposs.robot.shellx.util.AnsiColor;

/**
 * /usage 命令 - 查看用量信息
 */
public class UsageSlash implements ISlashCommand {
    @Override
    public String name() {
        return "/usage";
    }

    @Override
    public String description() {
        return "View usage and credits information";
    }

    @Override
    public String execute(ShellXContext context, ChatSession session, String args) {
        StringBuilder sb = new StringBuilder();
        sb.append(AnsiColor.bold("Usage Information")).append("\n");
        sb.append("  Agent:   ").append(context.getActiveAgentId()).append("\n");
        sb.append("  Session: ").append(session != null ? session.getId() : "none").append("\n");
        sb.append("  Effort:  ").append(context.getEffort()).append("\n");
        sb.append(AnsiColor.dim("\n  Detailed token usage tracking coming soon."));
        return sb.toString();
    }
}
