package cloud.apposs.robot.harness;

import cloud.apposs.robot.harness.sandbox.ISandbox;
import cloud.apposs.robot.harness.sandbox.SandboxFactory;
import cloud.apposs.robot.harness.setting.AIToolkitSetting;
import cloud.apposs.robot.harness.tool.ITool;
import cloud.apposs.robot.harness.tool.ToolFactory;
import cloud.apposs.util.Table;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HarnessToolKit {
    private final ISandbox sandbox;

    private final Map<String, ITool> tools = new HashMap<>();

    public HarnessToolKit(HarnessWorker worker) {
        this.sandbox = SandboxFactory.createSandbox(worker.getProfile().getSanboxType());
        this.reload(worker);
    }

    /**
     * 初始化工具包，清空所有已注册的工具，可用于配置文件更新后重新加载工具列表
     */
    public HarnessToolKit reload(HarnessWorker worker) {
        List<AIToolkitSetting> toolkitSettings = worker.getProfile().getToolkit();
        if (toolkitSettings == null || toolkitSettings.isEmpty()) {
            return this;
        }
        tools.clear();
        for (AIToolkitSetting setting : toolkitSettings) {
            if (!setting.isEnabled()) {
                continue;
            }
            String name = setting.getName();
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            ITool tool = ToolFactory.createMatchedTool(name, worker, setting);
            if (tool != null) {
                register(tool);
            }
        }
        return this;
    }

    public void register(ITool tool) {
        tools.put(tool.name(), tool);
    }

    public ITool getTool(String name) {
        return tools.get(name);
    }

    public boolean hasTool(String name) {
        return tools.containsKey(name);
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
