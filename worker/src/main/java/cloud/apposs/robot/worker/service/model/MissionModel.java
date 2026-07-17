package cloud.apposs.robot.worker.service.model;

import cloud.apposs.rest.validator.checker.NotBlank;

public class MissionModel {
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

        private String description;

        private String path;

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

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class Remove {
        @NotBlank
        private String wid;

        @NotBlank
        private String missionId;

        public String getWid() {
            return wid;
        }

        public void setWid(String wid) {
            this.wid = wid;
        }

        public String getMissionId() {
            return missionId;
        }

        public void setMissionId(String missionId) {
            this.missionId = missionId;
        }
    }

    public static class Rename {
        @NotBlank
        private String wid;

        @NotBlank
        private String missionId;

        @NotBlank
        private String name;

        public String getWid() {
            return wid;
        }

        public void setWid(String wid) {
            this.wid = wid;
        }

        public String getMissionId() {
            return missionId;
        }

        public void setMissionId(String missionId) {
            this.missionId = missionId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Sort {
        @NotBlank
        private String wid;

        private int[] missionIds;

        public String getWid() {
            return wid;
        }

        public void setWid(String wid) {
            this.wid = wid;
        }

        public int[] getMissionIds() {
            return missionIds;
        }

        public void setMissionIds(int[] missionIds) {
            this.missionIds = missionIds;
        }
    }

    public static class SessionMission {
        @NotBlank
        private String wid;

        @NotBlank
        private String sid;

        private int missionId;

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

        public int getMissionId() {
            return missionId;
        }

        public void setMissionId(int missionId) {
            this.missionId = missionId;
        }
    }
}
