package cloud.apposs.robot.harness.tool.delegate;

import cloud.apposs.react.React;
import cloud.apposs.robot.harness.HarnessDelegateWorker;
import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.robot.harness.tool.ITool;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;

public class ListDelegateWorkerTool implements ITool {
    public static final String NAME = "list_delegate_worker";

    private final HarnessDelegateWorker delegate;

    public ListDelegateWorkerTool(HarnessDelegateWorker delegate) {
        this.delegate = delegate;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "List all available Delegate Worker (enabled), including id, name and description.";
    }

    @Override
    public Param parameters() {
        String schema = "{" +
                "  \"type\": \"object\"," +
                "  \"properties\": {}" +
                "}";
        return JsonUtil.parseJsonParam(schema);
    }

    /**
     * 获取当前可用的子智能体列表，即在${workspace}/delegates/${id}目录下的所有子智能体信息
     *
     * @return 子智能体列表，每个子智能体包含完整的信息（ID、名称，描述等）
     */
    @Override
    public React<String> run(String wid, String sid, String rid, Param parameter, IMessageHook messageHook) throws Exception {
        return React.just(delegate.getWorkers().toJson());
    }
}
