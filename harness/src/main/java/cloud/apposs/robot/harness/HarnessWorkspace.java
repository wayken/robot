package cloud.apposs.robot.harness;

import cloud.apposs.robot.harness.mind.IMind;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 智能体工作空间路径，智能体在运行过程中产生的所有数据（如工具调用结果、消息历史等）都保存在这个路径下，各目录结构如下：
 * <pre>
 *   1. root：智能体工作空间的根目录
 *   2. disk：智能体运行过程中产生的所有数据都保存在这个目录下
 *   3. delegates：子智能体代理相关数据目录下
 *   4. schedules：定时任务相关数据目录下
 *   5. 其他目录：根据{@link IMind}的实现不同，可能会有其他目录结构，如技能、记忆、会话目录等
 * </pre>
 */
public final class HarnessWorkspace {
    // 工作空间根目录
    private final Path root;

    // 数据盘目录，智能体运行过程中产生的所有数据都保存在这个目录下
    private final Path disk;

    // 知识库目录，智能体运行过程中产生的所有知识库数据都保存在这个目录下
    private final Path wiki;

    // 子智能体代理相关数据目录
    private final Path delegates;

    // 定时任务相关数据目录
    private final Path schedules;

    public HarnessWorkspace(String workspace) {
        this.root = handlePathInitialize(Paths.get(workspace));
        this.disk = handlePathInitialize(root.resolve("disk"));
        this.wiki = handlePathInitialize(root.resolve("wiki"));
        this.delegates = handlePathInitialize(root.resolve("delegates"));
        this.schedules = handlePathInitialize(root.resolve("schedules"));
    }

    public Path root() {
        return root;
    }

    public Path disk() {
        return disk;
    }

    public Path wiki() {
        return wiki;
    }

    public Path delegates() {
        return delegates;
    }

    public Path schedules() {
        return schedules;
    }

    private static Path handlePathInitialize(Path path) {
        if (!Files.exists(path)) {
            path.toFile().mkdirs();
        }
        return path;
    }
}
