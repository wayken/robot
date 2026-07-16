package cloud.apposs.robot.harness.tool.delegate;

import cloud.apposs.react.React;
import cloud.apposs.robot.harness.HarnessDelegateWorker;
import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.robot.harness.tool.ITool;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;

public class AddDelegateWorkerTool implements ITool {
    public static final String NAME = "add_delegate_worker";

    private final HarnessDelegateWorker delegate;

    public AddDelegateWorkerTool(HarnessDelegateWorker delegate) {
        this.delegate = delegate;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Add a Delegate Worker with the given name and description, and return the unique identifier of the created Delegate Worker.";
    }

    @Override
    public Param parameters() {
        String schema = "{" +
                "  \"type\": \"object\"," +
                "  \"properties\": {" +
                "    \"name\": {" +
                "      \"type\": \"string\"," +
                "      \"description\": \"The name of the Delegate Worker.\"" +
                "    }," +
                "    \"description\": {" +
                "      \"type\": \"string\"," +
                "      \"description\": \"A brief description of the Delegate Worker.\"" +
                "    }" +
                "  }" +
                "}";
        return JsonUtil.parseJsonParam(schema);
    }

    @Override
    public React<String> run(String wid, String sid, String rid, Param parameter, IMessageHook messageHook) throws Exception {
        String name = parameter.getString("name");
        String description = parameter.getString("description");
        return React.just(delegate.addWorker(name, description));
    }
}
