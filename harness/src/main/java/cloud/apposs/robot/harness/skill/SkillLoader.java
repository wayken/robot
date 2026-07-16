package cloud.apposs.robot.harness.skill;

import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.harness.util.ResourceUtil;
import cloud.apposs.robot.harness.util.Runtimes;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkillLoader {
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile("^---\\r?\\n(.*?)\\r?\\n---", Pattern.DOTALL);
    private static final Pattern BACKMATTER_PATTERN = Pattern.compile("^---\\r?\\n.*?\\r?\\n---\\r?\\n", Pattern.DOTALL);

    private static final String SKILLS_CONFIG_FILE = ".skills.json";

    private final Path skillPath;

    public SkillLoader(HarnessWorker worker) {
        this.skillPath = worker.getWorkspace().root().resolve("skills");
        if (!Files.exists(skillPath)) {
            skillPath.toFile().mkdirs();
        }
    }

    /**
     * 获取技能根目录路径
     *
     * @return 技能根目录路径
     */
    public Path getSkillPath() {
        return skillPath;
    }

    /**
     * 构建常驻技能的结构体，供大模型在决策时参考，内容结构包括
     * <pre>
     *     1. 技能名称：技能的唯一标识，建议使用英文或拼音，避免使用特殊字符
     *     2. 技能简介：技能的简要描述，建议控制在一句话内，突出技能的核心能力和适用场景
     *     3. 技能内容：技能的详细说明，包含技能的功能、使用方法、输入输出示例等信息，建议使用 Markdown 格式进行编写，便于大模型进行解析和理解
     * </pre>
     *
     * @return 常驻技能的结构体，以供大模型在决策时参考
     */
    public String buildAlwaysSkillStruct() throws Exception {
        StringBuilder builder = new StringBuilder();
        Table<SkillStruct> skills = handleSkillsLoad(false);
        if (skills == null || skills.isEmpty()) {
            return "";
        }
        Table<String> disabledList = loadDisabledList();
        builder.append("# Active Skills\n\n");
        for (SkillStruct skill : skills) {
            // 跳过被禁用的技能
            if (disabledList.contains(skill.getName())) {
                continue;
            }
            Param frontmatter = handleSkillFrontmatterLoad(skill.getName());
            if (frontmatter == null) {
                continue;
            }
            boolean always = frontmatter.getBoolean("always", false);
            if (!always) {
                continue;
            }
            String content = handleSkillContentLoad(skill.getName());
            content = handleSkillContentBackmatter(content);
            if (content != null && !content.isEmpty()) {
                builder.append("### Skill: ").append(skill.getName()).append("\n\n").append(content);
            }
        }
        return builder.toString();
    }

    /**
     * 构建所有技能的摘要信息，供大模型在决策时参考，
     * 采用渐进式加载，智能体可以在需要时根据技能名称和简介信息，选择性地加载对应技能的详细内容进行使用
     *
     * @return XML 格式的技能摘要
     */
    public String buildSkillsSummaryStruct() throws Exception {
        Table<SkillStruct> skills = handleSkillsLoad(false);
        if (skills == null || skills.isEmpty()) {
            return "";
        }
        Table<String> disabledList = loadDisabledList();
        StringBuilder builder = new StringBuilder().append("<skills>\n");
        for (SkillStruct skill : skills) {
            // 跳过被禁用的技能
            if (disabledList.contains(skill.getName())) {
                continue;
            }
            Param frontmatter = handleSkillFrontmatterLoad(skill.getName());
            Param metadata = frontmatter.getParam("metadata");
            SkillRequirement requiment = handleSkillMetadataRequirement(skill.getName(), metadata);
            boolean available = true;
            Table<String> mission = null;
            if (requiment != null) {
                available = requiment.isAvailable();
                mission = requiment.getRequirements();
            }
            String description = frontmatter.getString("description", "");
            builder.append("  <skill available=\"").append(available).append("\">\n");
            builder.append("    <name>").append(skill.getName()).append("</name>\n");
            builder.append("    <description>").append(description).append("</description>\n");
            builder.append("    <location>").append(skill.getPath()).append("</location>\n");
            if (mission != null && !mission.isEmpty()) {
                builder.append("    <requires>").append(JsonUtil.toJson(mission)).append("</requires>\n");
            }
            builder.append("  </skill>\n");
        }
        builder.append("</skills>");
        return builder.toString();
    }

    /**
     * 列出所有已安装的技能及其元数据
     *
     * @return 技能列表，包含技能名称、描述和是否为常驻技能
     */
    public Table<Param> getSkillList() throws Exception {
        Table<Param> result = Table.builder();
        Table<String> disabledList = loadDisabledList();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(skillPath)) {
            for (Path path : paths) {
                if (!Files.isDirectory(path)) {
                    continue;
                }
                Path skillFile = path.resolve("SKILL.md");
                if (!Files.exists(skillFile)) {
                    continue;
                }
                String name = path.getFileName().toString();
                Param frontmatter = handleSkillFrontmatterLoad(name);
                if (frontmatter == null) {
                    frontmatter = Param.builder();
                }
                boolean enabled = !disabledList.contains(name);
                Param infomation = Param.builder("name", name)
                        .setString("description", frontmatter.getString("description", ""))
                        .setBoolean("always", frontmatter.getBoolean("always", false))
                        .setBoolean("enabled", enabled);
                result.add(infomation);
            }
        }
        return result;
    }

    /**
     * 添加技能到技能目录中
     *
     * @param skill 技能结构体，包含技能名称、路径和加载器等信息
     */
    public void addSkill(SkillStruct skill) throws Exception {
        Path destinationDir = skillPath.resolve(skill.getName());
        if (!Files.exists(destinationDir)) {
            Files.createDirectories(destinationDir);
        }
        // 插件技能：通过 loader 的 ClassLoader 定位资源路径
        Class<?> loader = skill.getLoader();
        if (loader != null) {
            handleSkillAddFromLoader(skill);
        } else {
            handleSkillAddFromDirectory(skill);
        }
    }

    /**
     * 删除指定名称的技能目录
     *
     * @param  name 技能名称
     * @return 如果技能目录存在并成功删除返回 true，否则返回 false
     */
    public boolean deleteSkill(String name) throws IOException {
        Path target = skillPath.resolve(name);
        if (!Files.exists(target) || !Files.isDirectory(target)) {
            return false;
        }
        Files.walk(target)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete: " + p, e);
                    }
                });
        // 同时从禁用列表中移除
        removeFromDisabledList(name);
        return true;
    }

    /**
     * 切换技能的启用/禁用状态
     *
     * @param  name    技能名称
     * @param  enabled 是否启用
     * @return 如果操作成功返回 true，否则返回 false
     */
    public boolean switchSkill(String name, boolean enabled) throws IOException {
        Path target = skillPath.resolve(name);
        if (!Files.exists(target) || !Files.isDirectory(target)) {
            return false;
        }
        Path skillFile = target.resolve("SKILL.md");
        if (!Files.exists(skillFile)) {
            return false;
        }
        if (enabled) {
            removeFromDisabledList(name);
        } else {
            addToDisabledList(name);
        }
        return true;
    }

    /**
     * 读取技能的 SKILL.md 内容
     *
     * @param  name 技能名称
     * @return 技能的 SKILL.md 内容，如果不存在则返回空字符串
     */
    public String readSkillContent(String name) throws IOException {
        return handleSkillContentLoad(name);
    }

    /**
     * 写入技能的 SKILL.md 内容
     *
     * @param  name    技能名称
     * @param  content SKILL.md 内容
     * @return 如果写入成功返回 true，否则抛出异常
     */
    public boolean writeSkillContent(String name, String content) throws IOException {
        Path target = skillPath.resolve(name).resolve("SKILL.md");
        if (!Files.exists(target.getParent())) {
            Files.createDirectories(target.getParent());
        }
        Files.write(target, content.getBytes(StandardCharsets.UTF_8));
        return true;
    }

    // 处理从资源路径添加技能的逻辑
    private void handleSkillAddFromLoader(SkillStruct skill) throws Exception {
        String sourcePath = skill.getPath();
        if (sourcePath == null || sourcePath.isEmpty()) {
            return;
        }
        Class<?> loader = skill.getLoader();
        // 使用 ClassLoader.getResource() 从 classpath 根路径查找，去掉开头的 / 以兼容该方法的路径规则
        String resourcePath = sourcePath.startsWith("/") ? sourcePath.substring(1) : sourcePath;
        URL resourceUrl = loader.getClassLoader().getResource(resourcePath);
        if (resourceUrl == null) {
            return;
        }
        URI uri = resourceUrl.toURI();
        String scheme = uri.getScheme();
        Path destinationDir = skillPath.resolve(skill.getName());
        if (ResourceUtil.isSchemaResourceFile(scheme)) {
            handleCopyFromResource(uri, destinationDir);
        } else {
            Path sourceDir = Paths.get(uri);
            if (Files.exists(sourceDir) && Files.isDirectory(sourceDir)) {
                handleCopyFromDirectory(sourceDir, destinationDir);
            }
        }
    }

    // 处理从普通文件系统路径添加技能的逻辑
    private void handleSkillAddFromDirectory(SkillStruct skill) throws Exception {
        // 内置技能：直接使用 path 作为文件系统路径或 URI
        String sourcePath = skill.getPath();
        if (sourcePath == null || sourcePath.isEmpty()) {
            return;
        }
        URI uri;
        try {
            uri = new URI(sourcePath);
        } catch (Exception e) {
            // 普通文件路径
            uri = Paths.get(sourcePath).toUri();
        }
        String scheme = uri.getScheme();
        Path destinationDir = skillPath.resolve(skill.getName());
        if (ResourceUtil.isSchemaResourceFile(scheme)) {
            // jar 包内路径，格式：jar:file:/path/to/plugin.jar!/skills/skill-name
            handleCopyFromResource(uri, destinationDir);
        } else {
            // 普通文件系统路径
            Path sourceDir = Paths.get(uri);
            if (Files.exists(sourceDir) && Files.isDirectory(sourceDir)) {
                handleCopyFromDirectory(sourceDir, destinationDir);
            }
        }
    }

    // 列出当前工作空间所有技能
    private Table<SkillStruct> handleSkillsLoad(boolean filterUnavailable) throws Exception {
        Table<SkillStruct> skills = Table.builder();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(skillPath)) {
            for (Path path : paths) {
                if (!Files.isDirectory(path)) {
                    continue;
                }
                Path skillFile = path.resolve("SKILL.md");
                if (!Files.exists(skillFile)) {
                    continue;
                }
                String name = path.getFileName().toString();
                skills.add(new SkillStruct(name, skillFile.toString()));
            }
        }
        return skills;
    }

    // 从 SKILL.md 文件中提取元数据
    private Param handleSkillFrontmatterLoad(String name) throws IOException {
        String content = handleSkillContentLoad(name);
        if (content == null) {
            return null;
        }
        content = handleSkillContentFrontmatter(content);
        if (content == null) {
            return null;
        }
        Param frontmatter = Param.builder();
        for (String readLine : content.split("\n")) {
            int index = readLine.indexOf(":");
            if (index <= 0) {
                continue;
            }
            String key = readLine.substring(0, index).trim();
            String value = readLine.substring(index + 1).trim();
            // 去除引号
            if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }
            frontmatter.put(key, value);
        }
        if (frontmatter.containsKey("metadata")) {
            String metadataStr = frontmatter.getString("metadata");
            frontmatter.setParam("metadata", JsonUtil.parseJsonParam(metadataStr));
        }
        return frontmatter;
    }

    // 按名称加载技能内容，未找到返回 null
    private String handleSkillContentLoad(String name) throws IOException {
        Path workspaceSkill = skillPath.resolve(name).resolve("SKILL.md");
        if (!Files.exists(workspaceSkill)) {
            return null;
        }
        return new String(Files.readAllBytes(workspaceSkill), StandardCharsets.UTF_8);
    }

    // 从资源路径复制文件到目标目录
    private void handleCopyFromResource(URI sourceUri, Path destinationDir) throws IOException {
        // jarUri 示例：jar:file:/path/to/plugin.jar!/skills/skill-name
        String uriStr = sourceUri.toString();
        int separator = uriStr.indexOf("!/");
        if (separator < 0) {
            return;
        }
        String resourceFilePart = uriStr.substring(0, separator);
        String entryPath = uriStr.substring(separator + 1);
        URI resourceFileUri = URI.create(resourceFilePart);
        try (FileSystem fs = FileSystems.newFileSystem(resourceFileUri, Collections.emptyMap())) {
            Path sourceInPath = fs.getPath(entryPath);
            if (!Files.exists(sourceInPath)) {
                return;
            }
            handleCopyFromDirectory(sourceInPath, destinationDir);
        }
    }

    // 递归复制目录下所有文件到目标目录（已存在的文件直接覆盖）
    private void handleCopyFromDirectory(Path source, Path destination) throws IOException {
        Files.walk(source).forEach(src -> {
            Path relative = source.relativize(src);
            Path dest = destination.resolve(relative.toString());
            try {
                if (Files.isDirectory(src)) {
                    if (!Files.exists(dest)) {
                        Files.createDirectories(dest);
                    }
                } else {
                    if (!Files.exists(dest)) {
                        Files.copy(src, dest);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to copy skill file: " + src, e);
            }
        });
    }

    // 从技能内容中提取 frontmatter（YAML 格式的元数据）
    private String handleSkillContentFrontmatter(String content) {
        if (content == null || !content.startsWith("---")) {
            return null;
        }
        Matcher matcher = FRONTMATTER_PATTERN.matcher(content);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }

    private String handleSkillContentBackmatter(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        if (!content.startsWith("---")) {
            return content.trim();
        }
        Matcher matcher = BACKMATTER_PATTERN.matcher(content);
        if (!matcher.find()) {
            return content.trim();
        }
        return content.substring(matcher.end()).trim();
    }

    private SkillRequirement handleSkillMetadataRequirement(String name, Param metadata) throws IOException {
        if (metadata == null || !metadata.containsKey("requires")) {
            return null;
        }
        Param requires = metadata.getParam("requires");
        boolean available = true;
        Table<String> requirements = Table.builder();
        Table<String> bins = requires.getTable("bins");
        Table<String> env = requires.getTable("env");
        if (bins != null) {
            for (String bin : bins) {
                if (!Runtimes.isCommandAvailable(bin)) {
                    requirements.add("CLI:" + bin);
                    available = false;
                }
            }
        }
        if (env != null) {
            for (String envVar : env) {
                if (System.getenv(envVar) == null) {
                    requirements.add("ENV:" + envVar);
                    available = false;
                }
            }
        }
        return new SkillRequirement(name, available, requirements);
    }

    /**
     * 加载禁用技能列表
     */
    private Table<String> loadDisabledList() throws IOException {
        Table<String> disabledList = Table.builder();
        Path configPath = skillPath.resolve(SKILLS_CONFIG_FILE);
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
     * 将技能添加到禁用列表
     */
    private void addToDisabledList(String name) throws IOException {
        Table<String> disabledList = loadDisabledList();
        if (!disabledList.contains(name)) {
            disabledList.add(name);
        }
        saveDisabledList(disabledList);
    }

    /**
     * 将技能从禁用列表中移除
     */
    private void removeFromDisabledList(String name) throws IOException {
        Table<String> disabledList = loadDisabledList();
        disabledList.remove(name);
        saveDisabledList(disabledList);
    }

    /**
     * 持久化禁用列表到配置文件
     */
    private void saveDisabledList(Table<String> disabledList) throws IOException {
        Param config = Param.builder("disabled", disabledList);
        Path configPath = skillPath.resolve(SKILLS_CONFIG_FILE);
        Files.write(configPath, JsonUtil.toJson(config, true).getBytes(StandardCharsets.UTF_8));
    }
}
