package cloud.apposs.robot.harness.sandbox;

import cloud.apposs.robot.harness.sandbox.LocalHostSandbox;

public final class SandboxFactory {
    public static ISandbox createSandbox(String type) {
        if (ISandbox.SANDBOX_LOCALHOST.equals(type)) {
            return new LocalHostSandbox();
        }
        throw new IllegalArgumentException("Unsupported sandbox type: " + type);
    }
}
