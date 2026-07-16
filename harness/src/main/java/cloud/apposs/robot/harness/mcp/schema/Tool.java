package cloud.apposs.robot.harness.mcp.schema;

import cloud.apposs.util.Param;

public final class Tool {
    private final String name;
    private final String description;
    private final Param inputSchema;

    public Tool(String name, String description, Param inputSchema) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Param getInputSchema() {
        return inputSchema;
    }

    public static Tool fromParam(Param param) {
        if (param == null) {
            return null;
        }
        String name = param.getString("name");
        String description = param.getString("description");
        Param inputSchema = param.getParam("inputSchema");
        return new Tool(name, description, inputSchema);
    }
}
