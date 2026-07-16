package cloud.apposs.robot.harness.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class Runtimes {
    public static final String CURRENT_OS = getOsName();

    public static String getOsName() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("mac") || os.contains("darwin")) {
            return "macos";
        }
        if (os.contains("win")) {
            return "windows";
        }
        if (os.contains("linux")) {
            return "linux";
        }
        return os;
    }

    public static boolean isWindows() {
        return CURRENT_OS.equals("windows");
    }

    public static boolean isCommandAvailable(String command) {
        try {
            String checkCmd = isWindows() ? "where" : "which";
            ProcessBuilder builder = new ProcessBuilder(checkCmd, command);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) { /* drain */ }
            }
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
