package cloud.apposs.robot.harness.sandbox;

public interface ISandbox {
    String SANDBOX_LOCALHOST = "localhost";

    /**
    * 执行工具调用，智能体通过这个方法来调用工具，沙箱会根据工具名称和参数来执行相应的工具逻辑，并返回结果
    *
    * @param name      工具名称
    * @param arguments 工具参数，通常是一个JSON字符串，包含工具调用所需的各种参数
    * @return 工具调用结果，包含工具执行后的结果数据
    */
    String runTool(String name, String arguments);
}
