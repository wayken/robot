package cloud.apposs.robot.harness;

import cloud.apposs.robot.harness.skill.SkillLoader;
import cloud.apposs.robot.harness.skill.SkillStruct;
import cloud.apposs.robot.harness.util.PromptLoader;
import cloud.apposs.util.Param;

public class HarnessSkill {
    private final SkillLoader skillLoader;

    public HarnessSkill(HarnessWorker worker) {
        this.skillLoader = new SkillLoader(worker);
    }

    public SkillLoader getSkillLoader() {
        return skillLoader;
    }

    /**
     * 构建技能提示词，供大模型在决策时参考，内容结构包括
     * <pre>
     *     1. 常驻技能：始终可用的技能，适合提供基础能力，如长期记忆、文件存储等
     *     2. 扩展技能：提供基础技能信息，包括技能名称、技能文件路径，技能简介等，供大模型在决策时进行渐进式加载
     * </pre>
     *
     * @return 技能提示词字，以供大模型在决策时参考
     */
    public String buildPrompt() throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append(skillLoader.buildAlwaysSkillStruct());
        String summary = skillLoader.buildSkillsSummaryStruct();
        builder.append("\n\n---\n\n");
        builder.append(PromptLoader.readPrompt("skill/summary", Param.builder("summary", summary)));
        return builder.toString();
    }

    /**
     * 将插件提供的技能目录写入智能体工作空间的 skills 目录，支持两种来源：
     * <pre>
     *   1. 普通文件系统目录（file:/.../skills/xxx）
     *   2. 运行打包包内目录（jar:file:/...!/skills/xxx）
     * </pre>
     *
     * @param skill 技能结构体，包含技能名称、技能路径、技能简介等信息
     */
    public void addSkill(SkillStruct skill) throws Exception {
        skillLoader.addSkill(skill);
    }
}
