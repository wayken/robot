package cloud.apposs.robot.harness.skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析用户消息中的 $skill-name 技能调用语法，支持多个技能同时调用
 */
public class SkillInvokeParser {
    // 匹配 $skill-name 格式，技能名称支持字母、数字、连字符和下划线
    private static final Pattern SKILL_INVOKE_PATTERN = Pattern.compile("\\$(([a-zA-Z][a-zA-Z0-9_-]*))");

    private final List<String> skillNames;
    private final String message;
    private final boolean matched;

    private SkillInvokeParser(List<String> skillNames, String message, boolean matched) {
        this.skillNames = skillNames;
        this.message = message;
        this.matched = matched;
    }

    /**
     * 解析用户消息中的 $skill-name 调用，支持多个技能
     *
     * @param  message 用户输入的原始消息
     * @return 解析结果，包含技能名称列表和清理后的消息
     */
    public static SkillInvokeParser parse(String message) {
        if (message == null || message.isEmpty()) {
            return new SkillInvokeParser(Collections.emptyList(), message, false);
        }
        Matcher matcher = SKILL_INVOKE_PATTERN.matcher(message);
        List<String> names = new ArrayList<>();
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        if (names.isEmpty()) {
            return new SkillInvokeParser(Collections.emptyList(), message, false);
        }
        // 去掉消息中所有 $技能名称 标签，并清理多余空格
        String cleanedMessage = SKILL_INVOKE_PATTERN.matcher(message).replaceAll("").trim().replaceAll("\\s+", " ");
        return new SkillInvokeParser(names, cleanedMessage, true);
    }

    /**
     * 是否匹配到了技能调用
     *
     * @return true 如果匹配到了技能调用，否则 false
     */
    public boolean isMatched() {
        return matched;
    }

    /**
     * 获取解析出的第一个技能名称（向后兼容）
     *
     * @return 第一个技能名称，如果未匹配到技能调用则为 null
     */
    public String getSkillName() {
        return skillNames.isEmpty() ? null : skillNames.get(0);
    }

    /**
     * 获取解析出的所有技能名称列表
     *
     * @return 技能名称列表，如果未匹配到技能调用则为空列表
     */
    public List<String> getSkillNames() {
        return skillNames;
    }

    /**
     * 获取清理后的用户消息内容（已去掉所有 $技能名称 标签）
     *
     * @return 清理后的消息
     */
    public String getMessage() {
        return message;
    }
}
