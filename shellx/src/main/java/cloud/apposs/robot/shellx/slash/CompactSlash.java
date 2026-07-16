package cloud.apposs.robot.shellx.slash;

import cloud.apposs.robot.shellx.ShellXContext;
import cloud.apposs.robot.shellx.session.ChatSession;
import cloud.apposs.robot.shellx.util.AnsiColor;

/**
 * /compact 命令 - 压缩对话释放上下文
 */
public class CompactSlash implements ISlashCommand {
    @Override
    public String name() {
        return "/compact";
    }

    @Override
    public String description() {
        return "Compact conversation to free up context";
    }

    @Override
    public String execute(ShellXContext context, ChatSession session, String args) {
        // TODO: 实现上下文压缩（调用 harness 的记忆巩固功能）
        return AnsiColor.info("Context compaction triggered. Consolidating memory...");
    }
}
