package cloud.apposs.robot.harness.mind.filesystem;

import cloud.apposs.robot.harness.HarnessWorkspace;
import cloud.apposs.robot.harness.util.PromptLoader;
import cloud.apposs.robot.harness.util.Runtimes;
import cloud.apposs.util.Param;

public class IdentityFileLoader {
    private final HarnessWorkspace workspace;

    public IdentityFileLoader(HarnessWorkspace workspace) {
        this.workspace = workspace;
    }

    public String buildPrompt() throws Exception {
        String osName = Runtimes.CURRENT_OS;
        String runtime = osName + " " + System.getProperty("os.arch");
        String platformPolicy;
        if (Runtimes.isWindows()) {
            platformPolicy = PromptLoader.readPrompt("identity/windows");
        } else {
            platformPolicy = PromptLoader.readPrompt("identity/postfix");
        }
        String workspacePath = workspace.root().toString();
        Param replacement = Param.builder("runtime", runtime)
                .setString("workspace", workspacePath)
                .setString("platform", platformPolicy);
        return PromptLoader.readPrompt("identity/runtime", replacement);
    }
}
