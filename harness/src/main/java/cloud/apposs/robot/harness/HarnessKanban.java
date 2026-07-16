package cloud.apposs.robot.harness;

/**
 * 任务追踪与规划模块，提供任务的创建、更新和查询功能，供智能体在推理过程中调用，规则如下：
 * <pre>
 *     1. 追踪工作项与多步骤计划，智能体可以将复杂目标拆解为多个任务，逐步执行和追踪进度
 *     2. 支持持久化存储、持跨对话/跨会话的任务状态保存，智能体重启后仍能恢复任务进度
 * </pre>
 */
public class HarnessKanban {
    private final HarnessWorkerProfile profile;

    public HarnessKanban(HarnessWorker worker) {
        this.profile = worker.getProfile();
    }
}
