package cloud.apposs.robot.harness.command;

import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.util.Table;

public class ConsolidateCommand implements ICommand {
    public static final String NAME = "/consolidate";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Consolidate sessions to memory, and clear the sessions";
    }

    /**
     * 压缩指定会话的对话消息，将历史消息进行摘要合并，具体操作流程如下：
     * <pre>
     *     1. 获取指定会话的所有对话消息
     *     2. 调用大模型接口对对话消息进行摘要提取并保存记忆库中
     *     3. 删除指定会话中原有的对话消息，减少存储空间占用和上下文长度，提升后续对话的效率和性能
     * </pre>
     */
    @Override
    public String run(HarnessWorker worker, String sessionId, Table<String> parameters) {
        try {
            worker.getMind().submitSession(worker.getId(), sessionId, true);
            return "Consolidate session success";
        } catch (Exception e) {
            return "Consolidate session failed: " + e.getMessage();
        }
    }
}
