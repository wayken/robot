package cloud.apposs.robot.harness;

import java.io.File;

public final class HarnessConstants {
    public static final String DEFAULT_WORKSPACE = System.getProperty("user.home") + File.separator + ".worker" + File.separator + "workspace";
    public static final String PROFILE_FILE = "profile.yaml";
}
