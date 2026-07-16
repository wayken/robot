package cloud.apposs.robot.harness.mcp.schema;

import cloud.apposs.util.Param;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具调用请求
 */
public final class CallToolRequest {
    private final String name;
    private final Map<String, Object> arguments;

    public CallToolRequest(String name, Map<String, Object> arguments) {
        this.name = name;
        this.arguments = arguments != null ? arguments : new HashMap<String, Object>();
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public Param toParams() {
        Param argsParam = new Param();
        for (Map.Entry<String, Object> argument : arguments.entrySet()) {
            argsParam.put(argument.getKey(), argument.getValue());
        }
        return Param.builder("name", name).setParam("arguments", argsParam);
    }
}
