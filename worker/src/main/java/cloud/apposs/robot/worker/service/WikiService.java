package cloud.apposs.robot.worker.service;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.worker.WorkerFramework;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@Component
public class WikiService {
    @Autowired
    private WorkerFramework framework;

    /**
     * 递归获取数据盘路径下的所有目录
     *
     * @param  workerId  智能体ID
     * @param  parentDir 父目录路径，根目录传空字符串
     * @return 目录信息列表，每个目录包含名称、路径和子目录列表
     */
    public Table<Param> getWikiDirs(String workerId, String parentDir) {
        Table<Param> result = Table.builder();
        HarnessWorker worker = framework.getHarness().getWorker(workerId);
        if (worker == null) {
            return result;
        }
        Path wikiPath = worker.getWorkspace().wiki();
        File destinationPath = wikiPath.resolve(parentDir).toFile();
        if (!destinationPath.exists() || !destinationPath.isDirectory()) {
            return result;
        }
        File[] files = destinationPath.listFiles();
        if (files == null) {
           return result;
        }
        for (File file : files) {
            if (!file.isDirectory()) {
                continue;
            }
            String path = wikiPath.relativize(file.toPath()).toString();
            path = path.replace(File.separator, "/");
            Table<Param> children = getWikiDirs(workerId, path);
            Param infomation = Param.builder("name", file.getName())
                    .setString("path", path)
                    .setLong("date", file.lastModified())
                    .setTable("children", children);
            result.add(infomation);
        }
        return result;
    }

    /**
     * 获取指定目录下的所有子目录和文件列表
     *
     * @param  workerId  智能体ID
     * @param  parentDir 父目录路径，根目录传空字符串
     * @return 目录和文件信息列表，每个信息包含名称、路径和是否为目录的标志
     */
    public Table<Param> getWikiFiles(String workerId, String parentDir) {
        Table<Param> result = Table.builder();
        HarnessWorker worker = framework.getHarness().getWorker(workerId);
        if (worker == null) {
            return result;
        }
        Path wikiPath = worker.getWorkspace().wiki();
        // 根目录标识：/ 或空字符串均视为wiki根目录
        File destinationPath;
        if (parentDir == null || parentDir.isEmpty() || "/".equals(parentDir)) {
            destinationPath = wikiPath.toFile();
        } else {
            destinationPath = wikiPath.resolve(parentDir).toFile();
        }
        if (!destinationPath.exists() || !destinationPath.isDirectory()) {
            return result;
        }
        File[] files = destinationPath.listFiles();
        if (files == null) {
           return result;
        }
        // 对文件列表进行排序，目录优先，文件次之
        Arrays.sort(files, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) {
                return -1;
            } else if (!f1.isDirectory() && f2.isDirectory()) {
                return 1;
            } else {
                return f1.getName().compareTo(f2.getName());
            }
        });
        for (File file : files) {
            String path = wikiPath.relativize(file.toPath()).toString();
            path = path.replace(File.separator, "/");
            Param infomation = Param.builder("name", file.getName())
                    .setString("path", path)
                    .setBoolean("isDirectory", file.isDirectory())
                    .setLong("date", file.lastModified());
            result.add(infomation);
        }
        return result;
    }

    // 读取文件内容
    public String readWikiFile(String workerId, String filePath) throws IOException {
        HarnessWorker worker = framework.getHarness().getWorker(workerId);
        if (worker == null) {
            return "";
        }
        Path wikiPath = worker.getWorkspace().wiki();
        File file = wikiPath.resolve(filePath).toFile();
        if (!file.exists() || !file.isFile()) {
            return "";
        }
        return new String(Files.readAllBytes(file.toPath()));
    }

    /**
     * 写入文件内容
     *
     * @param  workerId 智能体ID
     * @param  filePath 文件相对路径
     * @param  content  文件内容
     * @return 是否写入成功
     */
    public boolean writeWikiFile(String workerId, String filePath, String content) throws IOException {
        HarnessWorker worker = framework.getHarness().getWorker(workerId);
        if (worker == null) {
            return false;
        }
        Path wikiPath = worker.getWorkspace().wiki();
        Path targetPath = wikiPath.resolve(filePath).normalize();
        // 安全检查：确保路径在 wiki 目录下
        if (!targetPath.startsWith(wikiPath)) {
            return false;
        }
        if (!Files.exists(targetPath) || Files.isDirectory(targetPath)) {
            return false;
        }
        Files.write(targetPath, (content != null ? content : "").getBytes());
        return true;
    }

    public Table<Param> getTrashWikis(String wid) {
        Table<Param> result = Table.builder();
        return result;
    }

    /**
     * 在 wiki 目录下创建子目录
     *
     * @param  workerId   智能体ID
     * @param  parentPath 父目录相对路径
     * @param  name       新目录名称
     * @return 是否创建成功
     */
    public boolean createDirectory(String workerId, String parentPath, String name) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(workerId);
        if (worker == null) {
            return false;
        }
        Path wikiPath = worker.getWorkspace().wiki();
        Path targetPath = wikiPath.resolve(parentPath).resolve(name).normalize();
        // 安全检查：确保路径在 wiki 目录下
        if (!targetPath.startsWith(wikiPath)) {
            return false;
        }
        if (Files.exists(targetPath)) {
            return false;
        }
        Files.createDirectories(targetPath);
        return true;
    }

    /**
     * 在 wiki 目录下创建 Markdown 文件
     *
     * @param  workerId   智能体ID
     * @param  parentPath 父目录相对路径
     * @param  name       文件名称
     * @return 是否创建成功
     */
    public boolean createMarkdown(String workerId, String parentPath, String name) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(workerId);
        if (worker == null) {
            return false;
        }
        Path wikiPath = worker.getWorkspace().wiki();
        Path targetPath = wikiPath.resolve(parentPath).resolve(name).normalize();
        // 安全检查：确保路径在 wiki 目录下
        if (!targetPath.startsWith(wikiPath)) {
            return false;
        }
        if (Files.exists(targetPath)) {
            return false;
        }
        // 确保父目录存在
        Path parent = targetPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Files.createFile(targetPath);
        return true;
    }

    /**
     * 重命名 wiki 目录下的文件或目录
     *
     * @param  workerId 智能体ID
     * @param  filePath 要重命名的文件/目录相对路径
     * @param  newName  新名称
     * @return 是否重命名成功
     */
    public boolean rename(String workerId, String filePath, String newName) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(workerId);
        if (worker == null) {
            return false;
        }
        Path wikiPath = worker.getWorkspace().wiki();
        Path targetPath = wikiPath.resolve(filePath).normalize();
        // 安全检查：确保路径在 wiki 目录下且不是 wiki 根目录
        if (!targetPath.startsWith(wikiPath) || targetPath.equals(wikiPath)) {
            return false;
        }
        if (!Files.exists(targetPath)) {
            return false;
        }
        Path newPath = targetPath.getParent().resolve(newName).normalize();
        // 安全检查：确保新路径在 wiki 目录下
        if (!newPath.startsWith(wikiPath)) {
            return false;
        }
        if (Files.exists(newPath)) {
            return false;
        }
        Files.move(targetPath, newPath);
        return true;
    }

    /**
     * 移动 wiki 目录下的文件或目录到目标目录
     *
     * @param  workerId   智能体ID
     * @param  filePath   要移动的文件/目录相对路径
     * @param  targetDir  目标目录相对路径
     * @return 是否移动成功
     */
    public boolean move(String workerId, String filePath, String targetDir) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(workerId);
        if (worker == null) {
            return false;
        }
        Path wikiPath = worker.getWorkspace().wiki();
        Path sourcePath = wikiPath.resolve(filePath).normalize();
        // 安全检查：确保源路径在 wiki 目录下且不是 wiki 根目录
        if (!sourcePath.startsWith(wikiPath) || sourcePath.equals(wikiPath)) {
            return false;
        }
        if (!Files.exists(sourcePath)) {
            return false;
        }
        // 解析目标目录
        Path targetPath;
        if (targetDir == null || targetDir.isEmpty() || "/".equals(targetDir)) {
            targetPath = wikiPath;
        } else {
            targetPath = wikiPath.resolve(targetDir).normalize();
        }
        // 安全检查：确保目标路径在 wiki 目录下
        if (!targetPath.startsWith(wikiPath)) {
            return false;
        }
        // 确保目标目录存在
        if (!Files.exists(targetPath)) {
            Files.createDirectories(targetPath);
        }
        // 不能移动到自身或自身的子目录下
        if (targetPath.startsWith(sourcePath)) {
            return false;
        }
        // 计算最终目标文件路径
        String fileName = sourcePath.getFileName().toString();
        Path destination = targetPath.resolve(fileName).normalize();
        // 目标已存在则不移动
        if (Files.exists(destination)) {
            return false;
        }
        Files.move(sourcePath, destination);
        return true;
    }

    /**
     * 删除 wiki 目录下的文件或目录
     *
     * @param  workerId 智能体ID
     * @param  filePath 要删除的文件/目录相对路径
     * @return 是否删除成功
     */
    public boolean delete(String workerId, String filePath) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(workerId);
        if (worker == null) {
            return false;
        }
        Path wikiPath = worker.getWorkspace().wiki();
        Path targetPath = wikiPath.resolve(filePath).normalize();
        // 安全检查：确保路径在 wiki 目录下且不是 wiki 根目录
        if (!targetPath.startsWith(wikiPath) || targetPath.equals(wikiPath)) {
            return false;
        }
        if (!Files.exists(targetPath)) {
            return false;
        }
        // 递归删除目录或删除文件
        if (Files.isDirectory(targetPath)) {
            deleteDirectory(targetPath);
        } else {
            Files.delete(targetPath);
        }
        return true;
    }

    /**
     * 递归删除目录及其内容
     */
    private void deleteDirectory(Path directory) throws Exception {
        File[] children = directory.toFile().listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    deleteDirectory(child.toPath());
                } else {
                    Files.delete(child.toPath());
                }
            }
        }
        Files.delete(directory);
    }
}
