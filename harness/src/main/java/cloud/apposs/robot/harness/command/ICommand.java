package cloud.apposs.robot.harness.command;

import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.util.Table;

/**
 * 指令接口，通过聊天消息触发，
 * 如用户发送了/help指令，框架会解析到这个指令并调用对应的实现类来执行指令逻辑
 */
public interface ICommand {
    /**
     * 获取指令名称，如/help、/status等
     *
     * @return 指令名称
     */
    String name();

    /**
     * 获取指令描述信息
     *
     * @return 指令描述信息
     */
    String description();

    /**
     * 执行指令逻辑
     *
     * @param  worker     当前指令执行的智能体实例，可以通过它访问智能体的状态和功能
     * @param  sessionId  当前会话ID，尤其在同一智能体同时处理多个会话时非常重要，用于区分不同会话的指令执行
     * @param  parameters 指令参数
     * @return 指令执行结果，返回字符串形式的结果或错误信息
     */
    String run(HarnessWorker worker, String sessionId, Table<String> parameters);
}
