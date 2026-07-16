package cloud.apposs.robot.harness.delegate;

public class DelegateWorkerStruct {
    private final String name;

    private final String description;

    private final String prompt;

    public DelegateWorkerStruct(String name, String description, String prompt) {
        this.name = name;
        this.description = description;
        this.prompt = prompt;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPrompt() {
        return prompt;
    }
}
