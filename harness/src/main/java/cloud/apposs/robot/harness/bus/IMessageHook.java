package cloud.apposs.robot.harness.bus;

import cloud.apposs.logger.Logger;
import cloud.apposs.robot.harness.bus.ToolApprovalRequest;
import cloud.apposs.robot.harness.provider.AIResponse;

/**
 * LLM 推理过程中的消息监听器接口，设计用于订阅和处理智能体在推理过程中产生的各种消息事件，包括：
 * <pre>
 *     1. 在AI模型推理过程中产生的消息时的回调方法，可用于实时处理和展示AI模型的思考内容等场景
 *     2. 框架该次AI模型迭代完成的消息回调方法，即最终的AI模型响应结果
 *     3. 框架在AI模型推理过程中发生错误时的回调方法，可用于处理和记录推理过程中的异常情况
 * </pre>
 */
public interface IMessageHook {
    /**
     * 框架在AI模型推理过程中产生的消息时的回调方法，可用于实时处理和展示AI模型的思考内容等场景
     *
     * @param sid 当前会话唯一标识符
     * @param rid 当前迭代轮次ID
     * @param response AI模型响应的消息内容
     */
    void onProcessing(String sid, String rid, AIResponse response) throws Exception;

    /**
     * 框架工具检测到危险命令时的审批回调方法，框架会将审批请求传递给客户端，
     * 客户端需要向用户展示命令内容和危险原因（如推送消息给用户并等待点击），
     * 并在用户做出决定后调用 {@link ToolApprovalRequest#approve(boolean)} 提交结果，
     * 审批结果会直接驱动 React 流继续执行，不阻塞任何线程，
     * 默认实现为拒绝执行（安全优先），子类可覆盖此方法实现交互式审批
     *
     * @param sid 当前会话唯一标识符
     * @param request 审批请求，包含待执行命令和危险原因
     */
    default void onApprovalRequired(String sid, String rid, ToolApprovalRequest request) throws Exception {
        request.approve(false);
    }

   /**
    * 框架该次AI模型迭代完成的消息回调方法，即最终的AI模型响应结果
    *
    * @param sid 当前会话唯一标识符
    * @param response AI模型响应的消息内容
    */
    void onCompletion(String sid, String rid, AIResponse response) throws Exception;

    /**
     * 框架在AI模型推理过程中发生错误时的回调方法，可用于处理和记录推理过程中的异常情况
     *
     * @param cause 发生的异常信息
     */
    default void onError(Throwable cause) throws Exception {
        Logger.error(cause, "Error occurred in AI inference process");
    }
}
