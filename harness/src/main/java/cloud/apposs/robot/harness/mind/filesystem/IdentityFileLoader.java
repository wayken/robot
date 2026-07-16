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
        return buildPrompt(null);
    }

    public String buildPrompt(String projectPath) throws Exception {
        String osName = Runtimes.CURRENT_OS;
        String runtime = osName + " " + System.getProperty("os.arch");
        String platformPolicy;
        if (Runtimes.isWindows()) {
            platformPolicy = PromptLoader.readPrompt("identity/windows");
        } else {
            platformPolicy = PromptLoader.readPrompt("identity/postfix");
        }
        String workspacePath;
        if (projectPath != null && !projectPath.isEmpty()) {
            workspacePath = workspace.disk().resolve(projectPath).toString();
        } else {
            workspacePath = workspace.root().toString();
        }
        Param replacement = Param.builder("runtime", runtime)
                .setString("workhome", workspace.root().toString())
                .setString("workspace", workspacePath)
                .setString("platform", platformPolicy);
        return PromptLoader.readPrompt("identity/runtime", replacement);
    }
}
