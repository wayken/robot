package cloud.apposs.robot.harness.setting;

import cloud.apposs.util.Param;

/**
 * 工具包条目配置，对应 profile.yaml 中 toolkit 列表项
 *
 * <pre>
 * toolkit:
 *   - name: glob
 *     enabled: true
 *     properties:
 *       maxResults: 1000
 *   - name: web_search
 *     enabled: true
 *     properties:
 *       provider: duckduckgo
 * </pre>
 */
public class AIToolkitSetting {
    // 工具名称，对应 ITool#name()，如 read_file、web_search 等
    private String name;

    // 是否启用该工具，默认启用
    private boolean enabled = true;

    // 工具的额外配置项，由各工具自行解析，如 glob 的 maxResults、web_search 的 provider 等
    private Param properties;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Param getProperties() {
        return properties;
    }

    public void setProperties(Param properties) {
        this.properties = properties;
    }
}
