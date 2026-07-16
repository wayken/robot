package cloud.apposs.robot.shellx.slash;

import cloud.apposs.robot.shellx.ShellXContext;
import cloud.apposs.robot.shellx.session.ChatSession;
import cloud.apposs.robot.shellx.util.AnsiColor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * /effort 命令 - 设置推理强度
 */
public class EffortSlash implements ISlashCommand {
    private static final Set<String> VALID_LEVELS = new HashSet<>(
            Arrays.asList("low", "medium", "high", "xhigh", "max")
    );

    @Override
    public String name() {
        return "/effort";
    }

    @Override
    public String description() {
        return "Set reasoning effort level (low/medium/high/xhigh/max)";
    }

    @Override
    public String execute(ShellXContext context, ChatSession session, String args) {
        if (args == null || args.trim().isEmpty()) {
            return AnsiColor.info("Current effort: ") + context.getEffort()
                    + "\n" + AnsiColor.dim("Usage: /effort <low|medium|high|xhigh|max>");
        }

        String level = args.trim().toLowerCase();
        if (!VALID_LEVELS.contains(level)) {
            return AnsiColor.error("Invalid effort level: " + level + ". Valid: low, medium, high, xhigh, max");
        }

        context.setEffort(level);
        return AnsiColor.success("Effort set to: " + level);
    }
}
