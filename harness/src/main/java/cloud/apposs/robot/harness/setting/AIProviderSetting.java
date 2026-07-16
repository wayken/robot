package cloud.apposs.robot.harness.setting;

import java.util.ArrayList;
import java.util.List;

/**
 * AI服务商配置，对应 setting.yaml 中 provider 列表项
 */
public class AIProviderSetting {
    // 服务商名称，如 silicon、openai
    private String name;

    // 服务商接口类型，如 openai、gemini
    private String type;

    // 是否为主服务商
    private boolean primary = false;

    // API请求地址
    private String link;

    // 使用的模型名称
    private AIModelSetting model;

    // API Key列表，支持多个Key轮询
    private List<String> keys = new ArrayList<>();

    // 采样温度，控制输出随机性，范围 0~2，默认 0.7
    private double temperature = 0.7;

    // 单次请求最大Token数
    private int maxTokens = 4096;

    // nucleus采样参数，范围 0~1，默认 1
    private double topP = 1.0;

    // 是否启用流式输出，默认 false
    private boolean stream = false;

    // 请求超时时间，单位秒，默认 60秒
    private int timeout = 60;

    private List<AIProxySetting> proxies;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public AIModelSetting getModel() {
        return model;
    }

    public void setModel(AIModelSetting model) {
        this.model = model;
    }

    public List<String> getKeys() {
        return keys;
    }

    public void setKeys(List<String> keys) {
        this.keys = keys;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public double getTopP() {
        return topP;
    }

    public void setTopP(double topP) {
        this.topP = topP;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public List<AIProxySetting> getProxies() {
        return proxies;
    }

    public void setProxies(List<AIProxySetting> proxies) {
        this.proxies = proxies;
    }
}
