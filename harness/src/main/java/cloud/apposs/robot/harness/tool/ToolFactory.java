package cloud.apposs.robot.harness.tool;

import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.harness.setting.AIToolkitSetting;
import cloud.apposs.robot.harness.tool.delegate.AddDelegateWorkerTool;
import cloud.apposs.robot.harness.tool.delegate.AssignDelegateWorkerTool;
import cloud.apposs.robot.harness.tool.delegate.ListDelegateWorkerTool;
import cloud.apposs.robot.harness.tool.filesystem.EditFileTool;
import cloud.apposs.robot.harness.tool.filesystem.GlobTool;
import cloud.apposs.robot.harness.tool.filesystem.ReadFileTool;
import cloud.apposs.robot.harness.tool.filesystem.RemoveFileTool;
import cloud.apposs.robot.harness.tool.filesystem.WriteFileTool;
import cloud.apposs.robot.harness.tool.schedule.ScheduleTool;
import cloud.apposs.robot.harness.tool.session.SessionSearchTool;
import cloud.apposs.robot.harness.tool.shell.ShellTool;
import cloud.apposs.robot.harness.tool.web.WebSearchTool;

public final class ToolFactory {
    public static final String[] BUILDIN_TOOL_NAMES = new String[]{
            ReadFileTool.NAME,
            WriteFileTool.NAME,
            EditFileTool.NAME,
            RemoveFileTool.NAME,
            GlobTool.NAME,
            ShellTool.NAME,
            ScheduleTool.NAME,
            WebSearchTool.NAME,
            SessionSearchTool.NAME,
            AddDelegateWorkerTool.NAME,
            ListDelegateWorkerTool.NAME,
            AssignDelegateWorkerTool.NAME
    };

    public static ITool[] createBuildinTools(HarnessWorker worker) {
        ITool[] tools = new ITool[BUILDIN_TOOL_NAMES.length];
        for (int i = 0; i < BUILDIN_TOOL_NAMES.length; i++) {
            tools[i] = createMatchedTool(BUILDIN_TOOL_NAMES[i], worker, null);
        }
        return tools;
    }

    /**
     * 根据工具名和 worker 创建工具实例，无 properties 配置
     */
    public static ITool createMatchedTool(String name, HarnessWorker worker) {
        return createMatchedTool(name, worker, null);
    }

    /**
     * 根据工具名、worker 和 toolkit 配置创建工具实例，properties 可为 null
     */
    public static ITool createMatchedTool(String name, HarnessWorker worker, AIToolkitSetting setting) {
        switch (name) {
            case ReadFileTool.NAME:
                return new ReadFileTool(worker.getWorkspace().root());
            case WriteFileTool.NAME:
                return new WriteFileTool(worker.getWorkspace().root());
            case EditFileTool.NAME:
                return new EditFileTool(worker.getWorkspace().root());
            case RemoveFileTool.NAME:
                return new RemoveFileTool(worker.getWorkspace().root());
            case GlobTool.NAME: {
                int maxResults = setting != null ? setting.getProperties().getInt("maxResults", GlobTool.DEFAULT_MAX_RESULTS) : GlobTool.DEFAULT_MAX_RESULTS;
                return new GlobTool(worker.getWorkspace().root(), null, maxResults);
            }
            case ShellTool.NAME:
                String workingDir = worker.getWorkspace().root().toString();
                return new ShellTool(workingDir, worker.getProfile());
            case WebSearchTool.NAME: {
                String provider = setting != null ? setting.getProperties().getString("provider", WebSearchTool.DEFAULT_PROVIDER) : WebSearchTool.DEFAULT_PROVIDER;
                return new WebSearchTool(provider);
            }
            case ScheduleTool.NAME:
                return new ScheduleTool(worker.getSchedule());
            case SessionSearchTool.NAME:
                return new SessionSearchTool(worker);
            case AddDelegateWorkerTool.NAME:
                return new AddDelegateWorkerTool(worker.getDelegate());
            case ListDelegateWorkerTool.NAME:
                return new ListDelegateWorkerTool(worker.getDelegate());
            case AssignDelegateWorkerTool.NAME:
                return new AssignDelegateWorkerTool(worker.getDelegate());
            default:
                return null;
        }
    }
}
