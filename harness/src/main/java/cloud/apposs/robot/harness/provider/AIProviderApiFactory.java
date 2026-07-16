package cloud.apposs.robot.harness.provider;

import cloud.apposs.robot.harness.provider.claude.ClaudeAIProviderApi;
import cloud.apposs.robot.harness.provider.gemini.GeminiAIProviderApi;
import cloud.apposs.robot.harness.provider.openai.OpenAIProviderApi;

public final class AIProviderApiFactory {
    public static AIProviderApi create(String provider) {
        if (AIProviderType.OPENAI.matches(provider)) {
            return new OpenAIProviderApi();
        } else if (AIProviderType.GEMINI.matches(provider)) {
            return new GeminiAIProviderApi();
        } else if (AIProviderType.CLAUDE.matches(provider)) {
            return new ClaudeAIProviderApi();
        } else if (AIProviderType.OPENROUTER.matches(provider)) {
            return new OpenAIProviderApi();
        } else if (AIProviderType.OLLAMA.matches(provider)) {
            return new OpenAIProviderApi();
        }
        throw new IllegalArgumentException("Unsupported AI Provider: " + provider);
    }
}
