package cloud.apposs.robot.shellx.slash;

import cloud.apposs.robot.shellx.ShellXContext;
import cloud.apposs.robot.shellx.session.ChatSession;
import cloud.apposs.robot.shellx.util.AnsiColor;

import java.util.List;

/**
 * /help 命令 - 显示所有可用的 Slash Commands
 */
public class HelpSlash implements ISlashCommand {
    private final SlashRegistry registry;

    public HelpSlash(SlashRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String name() {
        return "/help";
    }

    @Override
    public String description() {
        return "Show available slash commands";
    }

    @Override
    public String execute(ShellXContext context, ChatSession session, String args) {
        StringBuilder sb = new StringBuilder();
        sb.append(AnsiColor.bold("Available Slash Commands:")).append("\n\n");

        List<ISlashCommand> commands = registry.getAllCommands();
        for (ISlashCommand cmd : commands) {
            String name = cmd.name();
            String[] aliases = cmd.aliases();
            StringBuilder nameStr = new StringBuilder(name);
            if (aliases.length > 0) {
                nameStr.append(" (");
                for (int i = 0; i < aliases.length; i++) {
                    if (i > 0) nameStr.append(", ");
                    nameStr.append(aliases[i]);
                }
                nameStr.append(")");
            }
            sb.append(String.format("  %-30s %s", AnsiColor.cyan(nameStr.toString()), cmd.description()));
            sb.append("\n");
        }

        sb.append("\n").append(AnsiColor.dim("Type a command for details, e.g. /help /context"));
        return sb.toString();
    }
}
