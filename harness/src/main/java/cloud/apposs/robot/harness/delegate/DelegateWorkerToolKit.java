package cloud.apposs.robot.harness.delegate;

import cloud.apposs.robot.harness.tool.ITool;
import cloud.apposs.util.Table;

import java.util.HashMap;
import java.util.Map;

public class DelegateWorkerToolKit {
    private Map<String, ITool> tools = new HashMap<>();

    public DelegateWorkerToolKit(DelegateWorkerProfile setting) {
    }

    public Map<String, ITool> getAllTools() {
        return tools;
    }

    public Table<ITool> getToolDefinitions() {
        Table<ITool> table = Table.builder();
        for (ITool tool : tools.values()) {
            table.add(tool);
        }
        return table;
    }
}
