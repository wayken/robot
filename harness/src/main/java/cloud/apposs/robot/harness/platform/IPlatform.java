package cloud.apposs.robot.harness.platform;

/**
 * 聊天通道接口，定义了智能体与外部世界交互的方式和协议
 */
public interface IPlatform {
    String PLATFORM_DINGTALK = "dingtalk";

    /**
    * 获取通道ID
    *
    * @return 通道ID
    */
    String getId();

    /**
    * 获取通道名称
    *
    * @return 通道名称
    */
    String getName();

    /**
     * 判断通道是否启用
     *
     * @return true如果通道启用，否则false
     */
    boolean isEnabled();

    /**
     * 启动通道并开始监听消息，包括：
     * <pre>
     *     1. 连接到聊天平台
     *     2. 监听传入的消息
     * </pre>
     */
    void start() throws Exception;

    /**
     * 关闭通道并停止监听消息，释放资源
     */
    void shutdown();
}
