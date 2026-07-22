package cloud.apposs.robot.worker.message;

import cloud.apposs.util.Param;

public class WorkerSecurityPolicy {
    public static final String REQUEST_APPROVAL = "request_approval";
    public static final String FULL_ACCESS = "full_access";

    private static volatile String permission = REQUEST_APPROVAL;

    public static String getPermission() {
        return permission;
    }

    public static boolean isFullAccess() {
        return FULL_ACCESS.equals(permission);
    }

    public static String setPermission(String value) {
        if (FULL_ACCESS.equals(value)) {
            permission = FULL_ACCESS;
        } else {
            permission = REQUEST_APPROVAL;
        }
        return permission;
    }

    public static Param toParam() {
        return Param.builder("permission", permission);
    }
}
