package cloud.apposs.robot.harness.skill;

import cloud.apposs.util.Table;

public class SkillRequirement {
    private final String name;

    private final boolean available;

    private final Table<String> requirements;

    public SkillRequirement(String name, boolean available, Table<String> requirements) {
        this.name = name;
        this.available = available;
        this.requirements = requirements;
    }

    public String getName() {
        return name;
    }

    public boolean isAvailable() {
        return available;
    }

    public Table<String> getRequirements() {
        return requirements;
    }
}
