package cloud.apposs.robot.harness.bus;

import cloud.apposs.react.IoEmitter;
import cloud.apposs.react.IoSubscriber;

/**
 * 危险指令令审批请求，当框架工具检测到危险命令时，会通过 {@link IMessageHook#onApprovalRequired} 将该对象传递给客户端，具体流程如下：
 * <pre>
 *     1. 框架通过 {@link IMessageHook#onApprovalRequired} 将该对象传递给客户端，客户端可通过该对象获取危险命令的相关信息并展示给用户
 *     2. 采用非阻塞回调模型：工具执行线程不阻塞，客户端在收到审批请求后可以继续处理其他消息或执行其他任务，直到用户做出决定
 *     3. 客户端在用户做出决定后调用 {@link #approve(boolean)} 驱动后续 React 流继续执行
 * </pre>
 */
public class ToolApprovalRequest {
    // 待执行的指令
    private final String command;

    // 触发审批的危险原因描述
    private final String reason;

    // 审批完成后用于驱动 React 流继续执行的订阅者回调
    private final IoSubscriber<String> subscriber;

    // 审批通过时实际执行命令的回调
    private final IoEmitter<String> function;

    public ToolApprovalRequest(String command, String reason, IoSubscriber<String> subscriber, IoEmitter<String> function) {
        this.command = command;
        this.reason = reason;
        this.subscriber = subscriber;
        this.function = function;
    }

    public String getCommand() {
        return command;
    }

    public String getReason() {
        return reason;
    }

    /**
     * 客户端在用户做出决定后调用此方法提交审批结果，会立即驱动 React 流继续执行，不阻塞任何线程。
     *
     * @param approved true 表示允许执行，false 表示拒绝执行
     */
    public void approve(boolean approved) {
        try {
            if (approved) {
                subscriber.onNext(function.call());
            } else {
                subscriber.onNext("Command execution denied by user: " + command);
            }
            subscriber.onCompleted();
        } catch (Throwable t) {
            subscriber.onError(t);
        }
    }

    @Override
    public String toString() {
        return "ShellApprovalRequest{command='" + command + "', reason='" + reason + "'}";
    }
}
