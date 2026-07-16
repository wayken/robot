package cloud.apposs.robot.harness.setting;

import cloud.apposs.util.Param;

/**
 * 消息渠道配置，主要包含对智能体消息渠道的配置信息，框架会根据这些配置信息来创建和管理智能体的消息渠道，
 * 支持多种消息平台（如钉钉、企业微信、Slack等），每个渠道可以配置不同的参数和行为，以满足不同的使用场景和需求
 */
public class AIPlatformSetting {
    // 频道ID，主要用于存储每个渠道的会话信息
    private String id;

    // 频道名称，如feishu、wechat、dingding
    private String name;

    // 是否启用该频道，默认为false
    private boolean enabled = false;

    // 渠道扩展属性，存放各渠道差异化的配置参数
    private Param properties;

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
