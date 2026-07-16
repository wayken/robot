package cloud.apposs.robot.worker.service.model;

import cloud.apposs.rest.validator.checker.NotBlank;
import cloud.apposs.util.Param;

public class ToolkitModel {
    public static class Index {
        @NotBlank
        private String wid;

        public String getWid() {
            return wid;
        }

        public void setWid(String wid) {
            this.wid = wid;
        }
    }

    public static class Switch {
        @NotBlank
        private String wid;

        @NotBlank
        private String name;

        private boolean enabled;

        public String getWid() {
            return wid;
        }

        public void setWid(String wid) {
            this.wid = wid;
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
    }

    public static class UpdateProperties {
        @NotBlank
        private String wid;

        @NotBlank
        private String name;

        private Param properties;

        public String getWid() {
            return wid;
        }

        public void setWid(String wid) {
            this.wid = wid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Param getProperties() {
            return properties;
        }

        public void setProperties(Param properties) {
            this.properties = properties;
        }
    }
}
