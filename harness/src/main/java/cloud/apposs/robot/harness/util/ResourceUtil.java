package cloud.apposs.robot.harness.util;

public final class ResourceUtil {
    public static boolean isSchemaResourceFile(String schema) {
        return "jar".equals(schema);
    }
}
