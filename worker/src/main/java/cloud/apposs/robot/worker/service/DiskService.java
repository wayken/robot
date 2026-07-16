package cloud.apposs.robot.worker.service;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.rest.FileStream;
import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.worker.WorkerFramework;
import cloud.apposs.util.MediaType;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

@Component
public class DiskService {
    @Autowired
    private WorkerFramework framework;

    // 递归获取数据盘路径下的所有目录
    public Table<Param> getDirectories(String workerId, String parentDir) {
        Table<Param> result = Table.builder();
        HarnessWorker worker = framework.getHarness().getWorker(workerId);
        if (worker == null) {
            return result;
        }
        Path diskPath = worker.getWorkspace().disk();
        if (parentDir == null) {
            parentDir = "";
        }
        File destinationPath = diskPath.resolve(parentDir).toFile();
        if (!destinationPath.exists() || !destinationPath.isDirectory()) {
            return result;
        }
        File[] children = destinationPath.listFiles();
        if (children == null) {
            return result;
        }
        for (File child : children) {
            String path = diskPath.relativize(child.toPath()).toString();
            Param infomation = Param.builder("name", child.getName()).setString("path", path);
            if (child.isDirectory()) {
                infomation.setBoolean("directory", true);
            }
            result.add(infomation);
        }
        return result;
    }

    public FileStream readFileStream(String workerId, String filePath) throws Exception {
        Path targetPath = resolveDiskPath(workerId, filePath);
        if (targetPath == null || !Files.isRegularFile(targetPath)) {
            return null;
        }
        MediaType mediaType = resolveMediaType(targetPath.getFileName().toString());
        return FileStream.create(mediaType, targetPath.toFile());
    }

    /**
     * 在指定目录下创建新文件夹
     */
    public boolean createDirectory(String workerId, String parentPath, String name) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(workerId);
        if (worker == null) {
            return false;
        }
        Path diskPath = worker.getWorkspace().disk();
        Path targetPath = diskPath.resolve(parentPath).resolve(name).normalize();
        // 安全检查：确保路径在磁盘目录下
        if (!targetPath.startsWith(diskPath)) {
            return false;
        }
        // 目录已存在则返回失败
        if (Files.exists(targetPath)) {
            return false;
        }
        Files.createDirectories(targetPath);
        return true;
    }

    public Table<Param> getTrashFiles(String wid) {
        Table<Param> result = Table.builder();
        return result;
    }

    /**
     * 读取磁盘文件并返回 Base64 编码内容
     */
    public String readFileBase64(String workerId, String filePath) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(workerId);
        if (worker == null) {
            return null;
        }
        Path diskPath = worker.getWorkspace().disk();
        Path targetPath = diskPath.resolve(filePath).normalize();
        // 安全检查：确保路径在磁盘目录下
        if (!targetPath.startsWith(diskPath)) {
            return null;
        }
        File file = targetPath.toFile();
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        byte[] content = Files.readAllBytes(targetPath);
        return Base64.getEncoder().encodeToString(content);
    }

    /**
     * 将 Base64 编码内容写入磁盘文件
     */
    public boolean writeFileBase64(String workerId, String filePath, String base64Content) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(workerId);
        if (worker == null) {
            return false;
        }
        Path diskPath = worker.getWorkspace().disk();
        Path targetPath = diskPath.resolve(filePath).normalize();
        // 安全检查：确保路径在磁盘目录下
        if (!targetPath.startsWith(diskPath)) {
            return false;
        }
        // 确保父目录存在
        Path parentDir = targetPath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
        byte[] content = Base64.getDecoder().decode(base64Content);
        Files.write(targetPath, content);
        return true;
    }

    /**
     * 读取磁盘文本文件并返回字符串内容
     */
    public String readFileText(String workerId, String filePath) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(workerId);
        if (worker == null) {
            return null;
        }
        Path diskPath = worker.getWorkspace().disk();
        Path targetPath = diskPath.resolve(filePath).normalize();
        if (!targetPath.startsWith(diskPath)) {
            return null;
        }
        File file = targetPath.toFile();
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        return new String(Files.readAllBytes(targetPath), StandardCharsets.UTF_8);
    }

    /**
     * 将文本内容直接写入磁盘文件
     */
    public boolean writeFileText(String workerId, String filePath, String content) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(workerId);
        if (worker == null) {
            return false;
        }
        Path diskPath = worker.getWorkspace().disk();
        Path targetPath = diskPath.resolve(filePath).normalize();
        if (!targetPath.startsWith(diskPath)) {
            return false;
        }
        Files.write(targetPath, content.getBytes(StandardCharsets.UTF_8));
        return true;
    }

    /**
     * 重命名文件或文件夹
     */
    public boolean rename(String workerId, String filePath, String newName) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(workerId);
        if (worker == null) {
            return false;
        }
        Path diskPath = worker.getWorkspace().disk();
        Path sourcePath = diskPath.resolve(filePath).normalize();
        // 安全检查：确保源路径在磁盘目录下
        if (!sourcePath.startsWith(diskPath)) {
            return false;
        }
        if (!Files.exists(sourcePath)) {
            return false;
        }
        Path targetPath = sourcePath.getParent().resolve(newName).normalize();
        // 安全检查：确保目标路径在磁盘目录下
        if (!targetPath.startsWith(diskPath)) {
            return false;
        }
        if (Files.exists(targetPath)) {
            return false;
        }
        Files.move(sourcePath, targetPath);
        return true;
    }

    /**
     * 删除文件或文件夹
     */
    public boolean delete(String workerId, String filePath) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(workerId);
        if (worker == null) {
            return false;
        }
        Path diskPath = worker.getWorkspace().disk();
        Path targetPath = diskPath.resolve(filePath).normalize();
        // 安全检查：确保路径在磁盘目录下且不是磁盘根目录
        if (!targetPath.startsWith(diskPath) || targetPath.equals(diskPath)) {
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

    private Path resolveDiskPath(String workerId, String filePath) {
        HarnessWorker worker = framework.getHarness().getWorker(workerId);
        if (worker == null) {
            return null;
        }
        Path diskPath = worker.getWorkspace().disk();
        Path targetPath = diskPath.resolve(filePath == null ? "" : filePath).normalize();
        if (!targetPath.startsWith(diskPath)) {
            return null;
        }
        return targetPath;
    }

    private MediaType resolveMediaType(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        String extension = dot >= 0 && dot + 1 < filename.length() ? filename.substring(dot + 1) : null;
        MediaType mediaType = MediaType.getMediaTypeByFileExtension(extension);
        return mediaType != null ? mediaType : MediaType.APPLICATION_OCTET_STREAM;
    }
}
