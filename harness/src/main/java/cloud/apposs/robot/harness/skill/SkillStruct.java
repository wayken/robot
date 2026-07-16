package cloud.apposs.robot.harness.skill;

import cloud.apposs.robot.harness.plugin.IPlugin;

public class SkillStruct {
    private final String name;

    private final String path;

    private final Class<? extends IPlugin> loader;

    public SkillStruct(String name, String path) {
        this(name, path, null);
    }

    public SkillStruct(String name, String path, Class<? extends IPlugin> loader) {
        this.name = name;
        this.path = path;
        this.loader = loader;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public Class<? extends IPlugin> getLoader() {
        return loader;
    }
}
