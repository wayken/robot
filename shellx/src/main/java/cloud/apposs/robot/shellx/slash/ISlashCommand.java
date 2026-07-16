package cloud.apposs.robot.shellx.slash;

import cloud.apposs.robot.shellx.ShellXContext;
import cloud.apposs.robot.shellx.session.ChatSession;

/**
 * 交互式会话内的 Slash Command 接口
 * 在 TUI 交互模式中，用户输入以 "/" 开头的命令将路由到对应的 ISlashCommand 实现
 */
public interface ISlashCommand {
    /**
     * 命令名称，含前缀，如 "/help"、"/quit"
     */
    String name();

    /**
     * 命令别名列表，如 /quit 的别名为 /exit、/q
     */
    default String[] aliases() {
        return new String[0];
    }

    /**
     * 命令描述
     */
    String description();

    /**
     * 执行命令
     *
     * @param context 应用上下文
     * @param session 当前会话
     * @param args    命令参数字符串
     * @return 输出结果，null 或空表示无需输出
     */
    String execute(ShellXContext context, ChatSession session, String args);
}
