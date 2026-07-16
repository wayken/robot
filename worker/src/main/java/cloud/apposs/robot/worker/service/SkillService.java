package cloud.apposs.robot.worker.service;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.harness.skill.SkillLoader;
import cloud.apposs.robot.worker.WorkerFramework;
import cloud.apposs.robot.worker.service.model.SkillModel;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Component
public class SkillService {
    private static final Pattern ALWAYS_PATTERN = Pattern.compile("^always:\\s*(.+)$", Pattern.MULTILINE);

    @Autowired
    private WorkerFramework framework;

    // 获取技能列表
    public Table<Param> getSkillList(SkillModel.Index request) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(request.getWid());
        if (worker == null) {
            throw new IllegalArgumentException("Worker " + request.getWid() + " not found");
        }
        return worker.getSkill().getSkillLoader().getSkillList();
    }

    // 读取技能 SKILL.md 内容
    public String readSkillContent(SkillModel.Read request) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(request.getWid());
        if (worker == null) {
            throw new IllegalArgumentException("Worker " + request.getWid() + " not found");
        }
        return worker.getSkill().getSkillLoader().readSkillContent(request.getName());
    }

    // 写入技能 SKILL.md 内容
    public boolean writeSkillContent(SkillModel.Write request) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(request.getWid());
        if (worker == null) {
            throw new IllegalArgumentException("Worker " + request.getWid() + " not found");
        }
        return worker.getSkill().getSkillLoader().writeSkillContent(request.getName(), request.getContent());
    }

    // 切换技能状态（启用/禁用 或 切换常驻模式）
    public boolean switchSkill(SkillModel.Switch request) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(request.getWid());
        if (worker == null) {
            throw new IllegalArgumentException("Worker " + request.getWid() + " not found");
        }
        SkillLoader loader = worker.getSkill().getSkillLoader();
        // 处理启用/禁用切换
        if (request.getEnabled() != null) {
            return loader.switchSkill(request.getName(), request.getEnabled());
        }
        // 处理 always（常驻）切换
        String content = loader.readSkillContent(request.getName());
        if (content == null) {
            return false;
        }
        // 更新 frontmatter 中的 always 字段
        String newContent = handleAlwaysUpdate(content, request.isAlways());
        loader.writeSkillContent(request.getName(), newContent);
        return true;
    }

    // 删除技能
    public boolean deleteSkill(SkillModel.Delete request) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(request.getWid());
        if (worker == null) {
            throw new IllegalArgumentException("Worker " + request.getWid() + " not found");
        }
        return worker.getSkill().getSkillLoader().deleteSkill(request.getName());
    }

    /**
     * 下载技能（将技能目录打包成 zip 字节数组）
     *
     * @param  wid  Worker ID
     * @param  name 技能名称
     * @return 字节数组
     */
    public byte[] downloadSkill(String wid, String name) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(wid);
        if (worker == null) {
            throw new IllegalArgumentException("Worker " + wid + " not found");
        }
        Path skillDir = worker.getSkill().getSkillLoader().getSkillPath().resolve(name);
        if (!Files.exists(skillDir) || !Files.isDirectory(skillDir)) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            Files.walk(skillDir).forEach(path -> {
                if (Files.isRegularFile(path)) {
                    String entryName = name + "/" + skillDir.relativize(path).toString();
                    try {
                        zos.putNextEntry(new ZipEntry(entryName));
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        return baos.toByteArray();
    }

    /**
     * 导入技能（从 zip 字节数组解压到 skills 目录）
     *
     * @param  wid     Worker ID
     * @param  zipData zip 文件的字节数组
     * @return 导入是否成功
     */
    public boolean importSkillFromZip(String wid, byte[] zipData) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(wid);
        if (worker == null) {
            throw new IllegalArgumentException("Worker " + wid + " not found");
        }
        Path skillsPath = worker.getSkill().getSkillLoader().getSkillPath();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    Files.createDirectories(skillsPath.resolve(entry.getName()));
                } else {
                    Path target = skillsPath.resolve(entry.getName());
                    Files.createDirectories(target.getParent());
                    Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
        return true;
    }

    /**
     * 导入技能（从目录内容，前端传递文件列表）
     *
     * @param  wid       Worker ID
     * @param  skillName 技能名称
     * @param  files     文件列表，key 为相对路径，value 为文件内容
     * @return 是否导入成功
     */
    public boolean importSkillFromFiles(String wid, String skillName, Param files) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(wid);
        if (worker == null) {
            throw new IllegalArgumentException("Worker " + wid + " not found");
        }
        Path skillDir = worker.getSkill().getSkillLoader().getSkillPath().resolve(skillName);
        if (!Files.exists(skillDir)) {
            Files.createDirectories(skillDir);
        }
        for (String relativePath : files.keySet()) {
            String content = files.getString(relativePath);
            Path target = skillDir.resolve(relativePath);
            Files.createDirectories(target.getParent());
            Files.write(target, content.getBytes(StandardCharsets.UTF_8));
        }
        return true;
    }

    /**
     * 更新 SKILL.md 中的 always 字段
     *
     * @param  content 原始 SKILL.md 内容
     * @param  always  新的 always 值
     * @return 更新后的 SKILL.md 内容
     */
    private String handleAlwaysUpdate(String content, boolean always) {
        Matcher matcher = ALWAYS_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.replaceFirst("always: " + always);
        }
        // 如果没有 always 字段，在 frontmatter 末尾 --- 前添加
        int endIndex = content.indexOf("---", 4);
        if (endIndex > 0) {
            return content.substring(0, endIndex) + "always: " + always + "\n" + content.substring(endIndex);
        }
        return content;
    }
}
