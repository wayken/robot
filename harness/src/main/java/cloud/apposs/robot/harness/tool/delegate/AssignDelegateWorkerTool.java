package cloud.apposs.robot.harness.tool.delegate;

import cloud.apposs.react.React;
import cloud.apposs.robot.harness.HarnessDelegateWorker;
import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.robot.harness.tool.ITool;
import cloud.apposs.robot.harness.util.Strings;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;

public class AssignDelegateWorkerTool implements ITool {
    public static final String NAME = "assign_delegate_worker";

    private final HarnessDelegateWorker delegate;

    public AssignDelegateWorkerTool(HarnessDelegateWorker delegate) {
        this.delegate = delegate;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Assign a task to a specified Delegate Worker and run its full agent loop, returning the final result.";
    }

    @Override
    public Param parameters() {
        String schema = "{" +
                "  \"type\": \"object\"," +
                "  \"properties\": {" +
                "    \"id\": {" +
                "      \"type\": \"string\"," +
                "      \"description\": \"The unique identifier of the Delegate Worker to which the task will be assigned.\"" +
                "    }," +
                "    \"task\": {" +
                "      \"type\": \"string\"," +
                "      \"description\": \"The task to be assigned to the Delegate Worker.\"" +
                "    }" +
                "  }," +
                "  \"required\": [\"id\", \"task\"]" +
                "}";
        return JsonUtil.parseJsonParam(schema);
    }

    @Override
    public boolean validate(Param parameter) {
        if (parameter == null) {
            return false;
        }
        String workerId = parameter.getString("id");
        String taskContent = parameter.getString("task");
        return !Strings.isBlank(workerId) && !Strings.isBlank(taskContent);
    }

    @Override
    public React<String> run(String wid, String sid, String rid, Param parameter, IMessageHook messageHook) throws Exception {
        String delegateId = parameter.getString("id");
        String taskContent = parameter.getString("task");
        return delegate.assignWorker(delegateId, sid, rid, taskContent, messageHook);
    }
}
