package cloud.apposs.robot.harness.mcp.schema;

import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * 列出工具结果
 */
public final class ListToolsResult {
    private final List<Tool> tools;

    public ListToolsResult(List<Tool> tools) {
        this.tools = tools != null ? tools : new ArrayList<Tool>();
    }

    public List<Tool> getTools() {
        return tools;
    }

    public static ListToolsResult fromParam(Param result) {
        List<Tool> tools = new ArrayList<Tool>();
        if (result == null) {
            return new ListToolsResult(tools);
        }
        Table<Param> toolsParam = result.getTable("tools");
        if (toolsParam != null) {
            for (Param toolParam : toolsParam) {
                Tool tool = Tool.fromParam(toolParam);
                if (tool != null && tool.getName() != null) {
                    tools.add(tool);
                }
            }
        }
        return new ListToolsResult(tools);
    }
}
