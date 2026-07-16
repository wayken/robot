package cloud.apposs.robot.shellx.slash;

import cloud.apposs.robot.shellx.ShellXContext;
import cloud.apposs.robot.shellx.session.ChatSession;
import cloud.apposs.robot.shellx.util.AnsiColor;

/**
 * /model 命令 - 查看/切换当前模型
 */
public class ModelSlash implements ISlashCommand {
    @Override
    public String name() {
        return "/model";
    }

    @Override
    public String description() {
        return "View or switch the AI model";
    }

    @Override
    public String execute(ShellXContext context, ChatSession session, String args) {
        if (args == null || args.trim().isEmpty()) {
            // 显示当前模型信息
            return AnsiColor.info("Current model: ") + "auto (configured in agent profile)";
        }
        // TODO: 实现模型切换
        return AnsiColor.info("Model switching not yet implemented. Configure in agent profile.yaml.");
    }
}
