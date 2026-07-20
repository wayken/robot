package cloud.apposs.robot.worker.service.model;

import cloud.apposs.rest.validator.checker.NotBlank;

public class SessionModel {
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

    public static class Add {
        @NotBlank
        private String wid;

        @NotBlank
        private String name;

        private int missionId;

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

        public int getMissionId() {
            return missionId;
        }

        public void setMissionId(int missionId) {
            this.missionId = missionId;
        }
    }

    public static class Remove {
        @NotBlank
        private String wid;

        @NotBlank
        private String sid;

        public String getWid() {
            return wid;
        }

        public void setWid(String wid) {
            this.wid = wid;
        }

        public String getSid() {
            return sid;
        }

        public void setSid(String sid) {
            this.sid = sid;
        }
    }

    public static class Rename {
        @NotBlank
        private String wid;

        @NotBlank
        private String sid;

        @NotBlank
        private String name;

        public String getWid() {
            return wid;
        }

        public void setWid(String wid) {
            this.wid = wid;
        }

        public String getSid() {
            return sid;
        }

        public void setSid(String sid) {
            this.sid = sid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Fork {
        @NotBlank
        private String wid;

        @NotBlank
        private String sid;

        @NotBlank
        private String messageId;

        @NotBlank
        private String name;

        public String getWid() {
            return wid;
        }

        public void setWid(String wid) {
            this.wid = wid;
        }

        public String getSid() {
            return sid;
        }

        public void setSid(String sid) {
            this.sid = sid;
        }

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
