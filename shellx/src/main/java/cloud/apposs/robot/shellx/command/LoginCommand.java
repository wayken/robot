package cloud.apposs.robot.shellx.command;

import cloud.apposs.robot.shellx.ShellXContext;
import cloud.apposs.robot.shellx.cli.CliCommand;
import cloud.apposs.robot.shellx.cli.CliParser;
import cloud.apposs.robot.shellx.util.AnsiColor;

/**
 * login 子命令 - 登录认证
 */
public class LoginCommand implements CliCommand {
    @Override
    public String name() {
        return "login";
    }

    @Override
    public String description() {
        return "Authenticate with the service";
    }

    @Override
    public int execute(ShellXContext context, CliParser parser) throws Exception {
        System.out.println(AnsiColor.bold("ShellX Login"));
        System.out.println();
        // TODO: 实现实际的认证流程（OAuth、API Key 等）
        System.out.println(AnsiColor.info("Authentication can be configured in profile.yaml"));
        System.out.println(AnsiColor.dim("  Or set environment variable: SHELLX_API_KEY"));
        return 0;
    }
}
