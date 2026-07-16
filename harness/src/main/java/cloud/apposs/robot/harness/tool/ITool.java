package cloud.apposs.robot.harness.tool;

import cloud.apposs.react.React;
import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.util.Param;

/**
 * 工具定义接口，每个工具需要实现该接口，定义工具的名称、描述、参数结构以及执行逻辑，
 * 工具是智能体在对话过程中可以调用的外部功能模块，
 * 如搜索工具、计算工具等，智能体可以根据对话内容和上下文来决定是否调用工具以及调用哪个工具，工具调用的结果会返回给智能体，智能体可以根据工具调用的结果来调整对话策略和生成更准确的响应
 */
public interface ITool {
    /**
     * 工具名称，智能体调用工具时会使用该名称来指定要调用哪个工具，
     * 工具名称应该具有描述性和唯一性，能够清晰地表达工具的功能和用途
     */
    String name();

    /**
     * 工具描述，提供工具的功能说明和使用指南，帮助智能体理解工具的作用和如何正确调用工具，
     * 描述信息可以包括工具的功能、参数要求、使用示例等内容，智能体可以根据描述信息来判断是否需要调用该工具以及如何构造调用参数
     */
    String description();

    /**
     * 工具参数结构，定义工具调用时需要传递的参数信息，包括参数名称、类型、是否必填、参数描述等
     */
    Param parameters();

    /**
     * 验证工具参数合法性，工具在执行前可以对传入的参数进行验证，确保参数符合预期的格式和要求
     */
    default boolean validate(Param parameter) {
        return true;
    }

    /**
     * 工具执行，智能体在调用工具时会传递参数信息，工具根据参数来执行相应的功能，并异步执行返回结果
     *
     * @param  wid 工具调用所属的工作空间ID，智能体在对话过程中可能会涉及多个工作空间，工具可以通过该ID来区分不同的工作上下文和调用场景
     * @param  sid 工具调用所属的会话ID，智能体在对话过程中可能会涉及多个会话，每个会话对应一个唯一的会话ID，工具可以通过该ID来区分不同的对话上下文和调用场景
     * @param  rid 当前迭代轮次ID，用于区分不同轮次的消息，一个用户消息请求下，可能会被多次迭代处理才结束
     * @param  parameter 工具调用参数
     * @param  messageHook 工具执行过程中，智能体可以注册一个消息钩子，工具可以主动触发对应的消息回调
     * @return 工具异步执行结果，通常是一个字符串，可以是工具执行的输出、状态信息或者错误信息等，智能体会根据工具执行结果来调整对话策略和生成响应
     */
    React<String> run(String wid, String sid, String rid, Param parameter, IMessageHook messageHook) throws Exception;
}
