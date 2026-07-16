package cloud.apposs.robot.harness.setting;

import java.util.List;

public class AIModelSetting {
    // 视觉模型
    public static final int MODEL_TYPE_VISION = 1;
    // 推理模型
    public static final int MODEL_TYPE_INFERENCE = 2;
    // 工具模型
    public static final int MODEL_TYPE_TOOL = 3;
    // 基岩模型
    public static final int MODEL_TYPE_BEDROCK = 4;

    private String name;

    private List<Integer> properties;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Integer> getProperties() {
        return properties;
    }

    public void setProperties(List<Integer> properties) {
        this.properties = properties;
    }

    public boolean hasProperty(int property) {
        return properties != null && properties.contains(property);
    }

    /**
     * 判断模型是否为视觉模型
     *
     * @return true 如果是视觉模型，否则 false
     */
    public boolean isVisionModel() {
        return hasProperty(MODEL_TYPE_VISION);
    }

    /**
     * 判断模型是否为推理模型
     *
     * @return true 如果是推理模型，否则 false
     */
    public boolean isInferenceModel() {
        return hasProperty(MODEL_TYPE_INFERENCE);
    }

    /**
     * 判断模型是否为工具模型
     *
     * @return true 如果是工具模型，否则 false
     */
    public boolean isToolModel() {
        return hasProperty(MODEL_TYPE_TOOL);
    }

    /**
     * 判断模型是否为基岩模型
     *
     * @return true 如果是基岩模型，否则 false
     */
    public boolean isBedrockModel() {
        return hasProperty(MODEL_TYPE_BEDROCK);
    }
}
