package cloud.apposs.robot.harness.mind.filesystem;

import cloud.apposs.robot.harness.HarnessWorkspace;
import cloud.apposs.robot.harness.util.Strings;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

public class MemoryFileSystemLoader {
    private static final List<String> CORE_FILES = Arrays.asList("MEMORY.md", "HISTORY.md");

    private final Path memoryPath;

    public MemoryFileSystemLoader(HarnessWorkspace workspace) {
        this.memoryPath = workspace.root().resolve("memory");
        if (!Files.exists(memoryPath)) {
            memoryPath.toFile().mkdirs();
        }
    }

    public String buildPrompt() throws IOException {
        Path memoryFile = memoryPath.resolve("MEMORY.md");
        if (!Files.exists(memoryFile)) {
            return "";
        }
        String content = new String(Files.readAllBytes(memoryFile), StandardCharsets.UTF_8);
        if (Strings.isBlank(content)) {
            return "";
        }
        return content;
    }

    /**
     * 追加写入历史记录到 HISTORY.md，每条记录以换行分隔
     */
    public void saveHistory(String wid, String sid, String rid, String historyEntry) throws IOException {
        if (Strings.isBlank(historyEntry)) {
            return;
        }
        Path historyFile = memoryPath.resolve("HISTORY.md");
        String entry = historyEntry.trim() + System.lineSeparator() + System.lineSeparator();
        Files.write(historyFile, entry.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * 覆盖写入长期记忆到 MEMORY.md
     */
    public void saveMemory(String wid, String sid, String rid, String memoryUpdate) throws IOException {
        if (Strings.isBlank(memoryUpdate)) {
            return;
        }
        Path memoryFile = memoryPath.resolve("MEMORY.md");
        Files.write(memoryFile, memoryUpdate.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * 获取记忆文件列表
     */
    public Table<Param> getMemoryFiles() throws IOException {
        Table<Param> result = Table.builder();
        if (!Files.exists(memoryPath) || !Files.isDirectory(memoryPath)) {
            return result;
        }
        // 先添加核心记忆文件（按固定顺序）
        for (String coreFile : CORE_FILES) {
            File file = memoryPath.resolve(coreFile).toFile();
            if (file.exists() && file.isFile()) {
                Param info = Param.builder("filename", file.getName())
                        .setBoolean("core", true)
                        .setLong("size", file.length())
                        .setLong("date", file.lastModified());
                result.add(info);
            }
        }
        // 再添加其他 .md 文件
        File[] files = memoryPath.toFile().listFiles();
        if (files == null) {
            return result;
        }
        Arrays.sort(files, (f1, f2) -> f1.getName().compareTo(f2.getName()));
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            String filename = file.getName();
            if (!filename.endsWith(".md")) {
                continue;
            }
            if (CORE_FILES.contains(filename)) {
                continue;
            }
            Param info = Param.builder("filename", filename)
                    .setBoolean("core", false)
                    .setLong("size", file.length())
                    .setLong("date", file.lastModified());
            result.add(info);
        }
        return result;
    }

    /**
     * 读取记忆文件内容
     */
    public String readMemoryFile(String filename) throws IOException {
        if (!isValidFilename(filename)) {
            return "";
        }
        File file = memoryPath.resolve(filename).toFile();
        if (!file.exists() || !file.isFile()) {
            return "";
        }
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    /**
     * 创建或更新记忆文件
     */
    public boolean writeMemoryFile(String filename, String content) throws IOException {
        if (!isValidFilename(filename)) {
            return false;
        }
        if (!Files.exists(memoryPath)) {
            memoryPath.toFile().mkdirs();
        }
        Path filePath = memoryPath.resolve(filename);
        Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
        return true;
    }

    /**
     * 删除记忆文件（仅允许删除非核心记忆文件）
     */
    public boolean deleteMemoryFile(String filename) throws IOException {
        if (!isValidFilename(filename)) {
            return false;
        }
        if (CORE_FILES.contains(filename)) {
            return false;
        }
        File file = memoryPath.resolve(filename).toFile();
        if (!file.exists() || !file.isFile()) {
            return false;
        }
        return file.delete();
    }

    /**
     * 重命名记忆文件（仅允许重命名非核心记忆文件）
     */
    public boolean renameMemoryFile(String filename, String newFilename) throws IOException {
        if (!isValidFilename(filename) || !isValidFilename(newFilename)) {
            return false;
        }
        if (CORE_FILES.contains(filename)) {
            return false;
        }
        if (CORE_FILES.contains(newFilename)) {
            return false;
        }
        File file = memoryPath.resolve(filename).toFile();
        if (!file.exists() || !file.isFile()) {
            return false;
        }
        File newFile = memoryPath.resolve(newFilename).toFile();
        if (newFile.exists()) {
            return false;
        }
        return file.renameTo(newFile);
    }

    /**
     * 校验文件名的安全性，防止路径穿越攻击
     */
    private boolean isValidFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            return false;
        }
        if (!filename.endsWith(".md")) {
            return false;
        }
        return true;
    }
}
