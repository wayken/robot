package cloud.apposs.robot.harness.provider;

public enum AIProviderType {
    OPENAI("openai", "https://api.openai.com/v1/chat/completions"),
    GEMINI("gemini", "https://generativelanguage.googleapis.com/v1beta/"),
    CLAUDE("claude", "https://api.anthropic.com/v1/messages"),
    OPENROUTER("openrouter", "https://openrouter.ai/api/v1/chat/completions"),
    OLLAMA("ollama", "http://localhost:11434/v1/chat/completions");

    private final String name;

    private final String link;

    AIProviderType(String type, String link) {
        this.name = type;
        this.link = link;
    }

    public String getName() {
        return name;
    }

    public String getLink() {
        return link;
    }

    public boolean matches(String provider) {
        return this.name.equalsIgnoreCase(provider);
    }

    public static AIProviderType fromName(String provider) {
        for (AIProviderType name : AIProviderType.values()) {
            if (name.matches(provider)) {
                return name;
            }
        }
        throw new IllegalArgumentException("Unsupported AI Provider: " + provider);
    }
}
