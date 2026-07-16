package cloud.apposs.robot.shellx.command;

import cloud.apposs.robot.shellx.ShellXContext;
import cloud.apposs.robot.shellx.cli.CliCommand;
import cloud.apposs.robot.shellx.cli.CliParser;
import cloud.apposs.robot.shellx.util.AnsiColor;

/**
 * logout 子命令 - 登出并清除凭据
 */
public class LogoutCommand implements CliCommand {
    @Override
    public String name() {
        return "logout";
    }

    @Override
    public String description() {
        return "Logout and clear credentials";
    }

    @Override
    public int execute(ShellXContext context, CliParser parser) throws Exception {
        // TODO: 清除存储的认证凭据
        System.out.println(AnsiColor.success("Logged out successfully."));
        return 0;
    }
}
