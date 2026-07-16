package cloud.apposs.robot.harness.mind.filesystem;

import cloud.apposs.robot.harness.HarnessWorkspace;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class RuleFileSystemLoader {
    private static final List<String> RULE_FILES = Arrays.asList(
            "AGENTS.md", "SOUL.md", "USER.md", "TOOLS.md"
    );

    private static final String RULES_CONFIG_FILE = ".rules.json";

    private final Path rulesPath;

    public RuleFileSystemLoader(HarnessWorkspace workspace) {
        this.rulesPath = workspace.root().resolve("rules");
        if (!Files.exists(rulesPath)) {
            rulesPath.toFile().mkdirs();
        }
    }

    /**
     * 构建规则提示词，加载所有启用的规则文件内容
     */
    public String buildPrompt() throws IOException {
        Table<String> parts = Table.builder();
        Table<String> disabledList = loadDisabledList();
        // 优先加载规则目录下的已知规则文件，作为核心规则（核心文件始终加载）
        for (String filename : RULE_FILES) {
            Path filePath = rulesPath.resolve(filename);
            if (Files.exists(filePath)) {
                byte[] bytes = Files.readAllBytes(filePath);
                String content = new String(bytes, StandardCharsets.UTF_8);
                parts.add("## " + filename + "\n\n" + content);
            }
        }
        // 加载规则目录下除已知规则文件以外的其他 .md 文件，作为补充规则
        if (Files.exists(rulesPath) && Files.isDirectory(rulesPath)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(rulesPath, "*.md")) {
                for (Path path : stream) {
                    String filename = path.getFileName().toString();
                    if (RULE_FILES.contains(filename)) {
                        continue;
                    }
                    // 跳过被禁用的文件
                    if (disabledList.contains(filename)) {
                        continue;
                    }
                    byte[] bytes = Files.readAllBytes(path);
                    String content = new String(bytes, StandardCharsets.UTF_8);
                    parts.add("## " + filename + "\n\n" + content);
                }
            }
        }
        return parts.isEmpty() ? "" : String.join("\n\n", parts);
    }

    /**
     * 获取规则文件列表
     */
    public Table<Param> getRuleFiles() throws IOException {
        Table<Param> result = Table.builder();
        if (!Files.exists(rulesPath) || !Files.isDirectory(rulesPath)) {
            return result;
        }
        File[] files = rulesPath.toFile().listFiles();
        if (files == null) {
            return result;
        }
        Table<String> disabledList = loadDisabledList();
        // 先添加核心规则文件（按固定顺序）
        for (String coreFile : RULE_FILES) {
            File file = rulesPath.resolve(coreFile).toFile();
            if (file.exists() && file.isFile()) {
                Param info = Param.builder("filename", file.getName())
                        .setBoolean("core", true)
                        .setBoolean("enabled", true)
                        .setLong("size", file.length())
                        .setLong("date", file.lastModified());
                result.add(info);
            }
        }
        // 再添加其他 .md 文件
        Arrays.sort(files, (f1, f2) -> f1.getName().compareTo(f2.getName()));
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            String filename = file.getName();
            if (!filename.endsWith(".md")) {
                continue;
            }
            if (RULE_FILES.contains(filename)) {
                continue;
            }
            boolean enabled = !disabledList.contains(filename);
            Param info = Param.builder("filename", filename)
                    .setBoolean("core", false)
                    .setBoolean("enabled", enabled)
                    .setLong("size", file.length())
                    .setLong("date", file.lastModified());
            result.add(info);
        }
        return result;
    }

    /**
     * 读取规则文件内容
     */
    public String readRuleFile(String filename) throws IOException {
        if (!isValidFilename(filename)) {
            return "";
        }
        File file = rulesPath.resolve(filename).toFile();
        if (!file.exists() || !file.isFile()) {
            return "";
        }
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    /**
     * 创建或更新规则文件
     */
    public boolean writeRuleFile(String filename, String content) throws IOException {
        if (!isValidFilename(filename)) {
            return false;
        }
        if (!Files.exists(rulesPath)) {
            rulesPath.toFile().mkdirs();
        }
        Path filePath = rulesPath.resolve(filename);
        Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
        return true;
    }

    /**
     * 删除规则文件（仅允许删除非核心规则文件）
     */
    public boolean deleteRuleFile(String filename) throws IOException {
        if (!isValidFilename(filename)) {
            return false;
        }
        if (RULE_FILES.contains(filename)) {
            return false;
        }
        File file = rulesPath.resolve(filename).toFile();
        if (!file.exists() || !file.isFile()) {
            return false;
        }
        boolean deleted = file.delete();
        if (deleted) {
            removeFromDisabledList(filename);
        }
        return deleted;
    }

    /**
     * 切换规则文件的启用/禁用状态
     */
    public boolean switchRuleFile(String filename, boolean enabled) throws IOException {
        if (!isValidFilename(filename)) {
            return false;
        }
        if (RULE_FILES.contains(filename)) {
            return false;
        }
        File file = rulesPath.resolve(filename).toFile();
        if (!file.exists() || !file.isFile()) {
            return false;
        }
        if (enabled) {
            removeFromDisabledList(filename);
        } else {
            addToDisabledList(filename);
        }
        return true;
    }

    /**
     * 重命名规则文件（仅允许重命名非核心规则文件）
     */
    public boolean renameRuleFile(String filename, String newFilename) throws IOException {
        if (!isValidFilename(filename) || !isValidFilename(newFilename)) {
            return false;
        }
        // 核心文件不可重命名
        if (RULE_FILES.contains(filename)) {
            return false;
        }
        // 不允许重命名为核心文件名
        if (RULE_FILES.contains(newFilename)) {
            return false;
        }
        File file = rulesPath.resolve(filename).toFile();
        if (!file.exists() || !file.isFile()) {
            return false;
        }
        File newFile = rulesPath.resolve(newFilename).toFile();
        if (newFile.exists()) {
            return false;
        }
        boolean renamed = file.renameTo(newFile);
        if (renamed) {
            // 如果原文件在禁用列表中，将新文件名也更新到禁用列表
            Table<String> disabledList = loadDisabledList();
            if (disabledList.contains(filename)) {
                disabledList.remove(filename);
                disabledList.add(newFilename);
                saveDisabledList(disabledList);
            }
        }
        return renamed;
    }

    /**
     * 加载禁用文件列表
     */
    private Table<String> loadDisabledList() throws IOException {
        Table<String> disabledList = Table.builder();
        Path configPath = rulesPath.resolve(RULES_CONFIG_FILE);
        if (!Files.exists(configPath)) {
            return disabledList;
        }
        String content = new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8);
        Param config = JsonUtil.parseJsonParam(content);
        if (config == null) {
            return disabledList;
        }
        Table<?> disabled = config.getTable("disabled");
        if (disabled != null) {
            for (Object item : disabled) {
                if (item instanceof String) {
                    disabledList.add((String) item);
                } else if (item instanceof Param) {
                    disabledList.add(((Param) item).getString("value"));
                }
            }
        }
        return disabledList;
    }

    /**
     * 将文件添加到禁用列表
     */
    private void addToDisabledList(String filename) throws IOException {
        Table<String> disabledList = loadDisabledList();
        if (!disabledList.contains(filename)) {
            disabledList.add(filename);
        }
        saveDisabledList(disabledList);
    }

    /**
     * 将文件从禁用列表中移除
     */
    private void removeFromDisabledList(String filename) throws IOException {
        Table<String> disabledList = loadDisabledList();
        disabledList.remove(filename);
        saveDisabledList(disabledList);
    }

    /**
     * 持久化禁用列表到配置文件
     */
    private void saveDisabledList(Table<String> disabledList) throws IOException {
        Param config = Param.builder("disabled", disabledList);
        Path configPath = rulesPath.resolve(RULES_CONFIG_FILE);
        Files.write(configPath, JsonUtil.toJson(config, true).getBytes(StandardCharsets.UTF_8));
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
