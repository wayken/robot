package cloud.apposs.robot.harness;

import cloud.apposs.robot.harness.bus.ILifeCycleHook;
import cloud.apposs.robot.harness.struct.MessageStruct;
import cloud.apposs.util.Table;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class HarnessMessageBus {
    private final Table<ILifeCycleHook> iLifeCycleHooks;

    // 消息入站队列
    private final BlockingQueue<MessageStruct> inBoundMessageQueue;

    public HarnessMessageBus(HarnessSetting setting) {
        this.iLifeCycleHooks = Table.builder();
        this.inBoundMessageQueue = new LinkedBlockingQueue<>();
    }

    /**
     * 判断是否注册了生命周期钩子函数
     *
     * @param  hook 生命周期钩子函数
     * @return 如果注册了生命周期钩子函数，则返回true；否则返回false
     */
    public boolean isLifeCycleHookRegistered(ILifeCycleHook hook) {
        return iLifeCycleHooks.contains(hook);
    }

    /**
     * 注册生命周期钩子函数
     *
     * @param hook 生命周期钩子函数，设计用于在智能体推理过程中的不同阶段触发回调方法，以便开发者能够在这些阶段执行自定义的逻辑，例如：记录日志、收集数据、调整推理参数等
     */
    public void registerLifeCycleHook(ILifeCycleHook hook) {
        iLifeCycleHooks.add(hook);
    }

    /**
     * 注销生命周期钩子函数
     *
     * @param hook 生命周期钩子函数
     */
    public void unregisterLifeCycleHook(ILifeCycleHook hook) {
        iLifeCycleHooks.remove(hook);
    }

    /**
     * 触发生命周期回调方法，在智能体推理过程中的不同阶段被调用
     *
     * @param wid       当前智能体ID，唯一标识一个智能体实例
     * @param sid       当前会话ID
     * @param rid       当前迭代轮次ID
     * @param phase     当前生命周期阶段
     * @param arguments 可选的参数列表，具体内容根据生命周期阶段而定
     */
    public void triggerLifeCycleHook(String wid, String sid, String rid, ILifeCycleHook.Phase phase, Object... arguments) throws Exception {
        for (ILifeCycleHook hook : iLifeCycleHooks) {
            hook.onLifeCycle(wid, sid, rid, phase, arguments);
        }
    }

    /**
     * 通道将消息推送到入站队列，智能体的思维引擎会从入站队列中取出消息进行处理
     */
    public void publishInboundMessage(MessageStruct message) {
        inBoundMessageQueue.offer(message);
    }

    /**
     * 智能体的思维引擎从入站队列中订阅消息进行处理，如果入站队列为空，则线程会被阻塞，直到有新的消息被推送到入站队列中
     *
     * @throws InterruptedException 线程被中断时抛出
     */
    public MessageStruct subscribeInboundMessage() throws InterruptedException {
        return inBoundMessageQueue.take();
    }
}
