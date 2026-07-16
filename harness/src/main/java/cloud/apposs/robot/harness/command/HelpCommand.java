package cloud.apposs.robot.harness.command;

import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.util.Table;

public class HelpCommand implements ICommand {
    public static final String NAME = "/help";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Display help information about available commands.";
    }

    @Override
    public String run(HarnessWorker worker, String sessionId, Table<String> parameters) {
        return "Slash commands in REPL:\n" +
                "/help - Display this help information.\n" +
                "/status - Display the current status of the application";
    }
}
