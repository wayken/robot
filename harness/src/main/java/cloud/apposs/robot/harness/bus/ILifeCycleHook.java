package cloud.apposs.robot.harness.bus;

public interface ILifeCycleHook {
    /**
     * 智能体推理过程中的生命周期阶段枚举，定义了智能体推理过程中的不同阶段，以便在这些阶段触发生命周期回调方法
     */
    public static enum Phase {
        PHASE_MESSAGE_INBOUND,
        PHASE_MESSAGE_PROCESSING,
        PHASE_POST_COMPLETION,
        PHASE_WORK_FAILURE
    }

    /**
     * 生命周期回调方法，在智能体推理过程中的不同阶段被调用
     *
     * @param wid       当前智能体ID，唯一标识一个智能体实例
     * @param sid       当前会话ID
     * @param rid       当前迭代轮次ID
     * @param phase     当前生命周期阶段
     * @param arguments 可选的参数列表，具体内容根据生命周期阶段而定
     */
    void onLifeCycle(String wid, String sid, String rid, Phase phase, Object... arguments) throws Exception;
}
