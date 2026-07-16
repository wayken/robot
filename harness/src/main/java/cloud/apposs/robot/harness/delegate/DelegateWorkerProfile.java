package cloud.apposs.robot.harness.delegate;

import cloud.apposs.robot.harness.setting.AIProviderSetting;

import java.util.List;

public class DelegateWorkerProfile {
    private String name;

    private String description;

    private boolean enabled = true;

    private int maxIterations = 4;

    private List<AIProviderSetting> provider;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public List<AIProviderSetting> getProvider() {
        return provider;
    }

    public void setProvider(List<AIProviderSetting> provider) {
        this.provider = provider;
    }

    public AIProviderSetting getPrimaryProvider() {
        for (AIProviderSetting setting : provider) {
            if (setting.isPrimary()) {
                return setting;
            }
        }
        return provider.isEmpty() ? null : provider.get(0);
    }
}
