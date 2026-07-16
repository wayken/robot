package cloud.apposs.robot.worker.service.model;

import cloud.apposs.rest.validator.checker.NotBlank;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

public class WorkerProviderModel {
    public static class Provider {
        @NotBlank
        private String wid;

        public String getWid() {
            return wid;
        }

        public void setWid(String wid) {
            this.wid = wid;
        }
    }

    public static class Update {
        @NotBlank
        private String wid;

        // 完整的provider配置列表
        private Table<ProviderItem> provider;

        public String getWid() {
            return wid;
        }

        public void setWid(String wid) {
            this.wid = wid;
        }

        public Table<ProviderItem> getProvider() {
            return provider;
        }

        public void setProvider(Table<ProviderItem> provider) {
            this.provider = provider;
        }
    }

    public static class ProviderItem {
        // 服务商名称，如 silicon、openai
        private String name;

        // 服务商接口类型，如 openai、gemini
        private String type;

        // 是否为主服务商
        private boolean primary = false;

        // API请求地址
        private String link;

        // 使用的模型
        private Param model;

        // 采样温度，范围 0~2
        private double temperature = 0.7;

        // nucleus采样参数，范围 0~1
        private double topP = 1.0;

        // 是否启用流式输出
        private boolean stream = true;

        // 请求超时时间，单位秒
        private int timeout = 60;

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

        public Param getModel() {
            return model;
        }

        public void setModel(Param model) {
            this.model = model;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
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
    }
}
