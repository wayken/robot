package cloud.apposs.robot.harness.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtil {
    /**
     * 解析路径：支持 ~ 展开，相对路径基于 workspace 解析
     *
     * @param  path      输入路径
     * @param  workspace 工作空间路径，用于解析相对路径
     * @return 解析后的绝对路径
     */
    public static Path resolveAbsolutePath(String path, Path workspace) {
        String rawPath = path;
        if (rawPath.startsWith("~")) {
            String home = System.getProperty("user.home", "");
            rawPath = home + rawPath.substring(1);
        }
        Path ofPath = Paths.get(rawPath);
        if (!ofPath.isAbsolute() && workspace != null) {
            ofPath = workspace.resolve(ofPath);
        }
        return ofPath.toAbsolutePath().normalize();
    }
}
