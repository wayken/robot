package cloud.apposs.robot.shellx.cli;

import cloud.apposs.robot.shellx.ShellXContext;

import java.util.Collections;
import java.util.List;

/**
 * 顶层 CLI 命令接口，每个子命令（如 chat、agent、mcp 等）实现此接口
 */
public interface CliCommand {
    /**
     * 命令名称，如 "chat"、"agent"、"mcp"
     */
    String name();

    /**
     * 命令描述
     */
    String description();

    /**
     * 命令支持的选项定义
     */
    default List<CliOption> options() {
        return Collections.emptyList();
    }

    /**
     * 执行命令
     *
     * @param context 应用上下文
     * @param parser  已解析的命令行参数
     * @return 退出码，0 表示成功
     */
    int execute(ShellXContext context, CliParser parser) throws Exception;

    /**
     * 打印命令用法
     */
    default void printUsage() {
        System.out.println("Usage: shellx " + name() + " [options]");
        System.out.println("  " + description());
        List<CliOption> opts = options();
        if (!opts.isEmpty()) {
            System.out.println();
            System.out.println("Options:");
            for (CliOption opt : opts) {
                StringBuilder sb = new StringBuilder("  ");
                if (opt.getShortName() != null) {
                    sb.append("-").append(opt.getShortName()).append(", ");
                } else {
                    sb.append("    ");
                }
                sb.append("--").append(opt.getLongName());
                if (opt.isRequiresValue()) {
                    sb.append(" <value>");
                }
                // 对齐描述
                while (sb.length() < 36) {
                    sb.append(' ');
                }
                sb.append(opt.getDescription());
                System.out.println(sb.toString());
            }
        }
    }
}
