package cloud.apposs.robot.harness.provider;

import cloud.apposs.util.Param;

public class AITool {
    private String id;

    private String name;

    private String arguments;

    public AITool(String id, String name, String arguments) {
        this.id = id;
        this.name = name;
        this.arguments = arguments;
    }

    public AITool(String name, String arguments) {
        this(null, name, arguments);
    }

    public static AITool of(String id, String name, String arguments) {
        return new AITool(id, name, arguments);
    }

    public static AITool of(String name, String arguments) {
        return new AITool(name, arguments);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    @Override
    public String toString() {
        return String.format("AITool{id='%s', name='%s', arguments='%s'}", id, name, arguments);
    }

    public Param deserialize() {
        return Param.builder("id", id)
                .setString("name", name)
                .setString("arguments", arguments);
    }

    public static AITool serialize(Param param) {
        String id = param.getString("id");
        String name = param.getString("name");
        String arguments = param.getString("arguments");
        return AITool.of(id, name, arguments);
    }
}
