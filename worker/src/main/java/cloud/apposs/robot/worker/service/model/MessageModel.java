package cloud.apposs.robot.worker.service.model;

import cloud.apposs.rest.validator.checker.NotBlank;

public class MessageModel {
    public static class Index {
        @NotBlank
        private String wid;

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

    public static class Commit {
        @NotBlank
        private String wid;

        private String sid;

        @NotBlank
        private String rid;

        @NotBlank
        private String message;

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

        public String getRid() {
            return rid;
        }

        public void setRid(String rid) {
            this.rid = rid;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class Truncate {
        @NotBlank
        private String id;

        @NotBlank
        private String wid;

        private String sid;

        @NotBlank
        private String rid;

        @NotBlank
        private String message;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

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

        public String getRid() {
            return rid;
        }

        public void setRid(String rid) {
            this.rid = rid;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }


    public static class Interrupt {
        @NotBlank
        private String wid;

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

    public static class Approval {
        @NotBlank
        private String id;

        private String sid;

        private String rid;

        private Boolean approved;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSid() {
            return sid;
        }

        public void setSid(String sid) {
            this.sid = sid;
        }

        public String getRid() {
            return rid;
        }

        public void setRid(String rid) {
            this.rid = rid;
        }

        public Boolean getApproved() {
            return approved;
        }

        public void setApproved(Boolean approved) {
            this.approved = approved;
        }
    }

    public static class SecurityPermission {
        private String permission;

        public String getPermission() {
            return permission;
        }

        public void setPermission(String permission) {
            this.permission = permission;
        }
    }

    public static class Remove {
        @NotBlank
        private String wid;

        @NotBlank
        private String sid;

        @NotBlank
        private String rid;

        @NotBlank
        private String id;

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

        public String getRid() {
            return rid;
        }

        public void setRid(String rid) {
            this.rid = rid;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    public static class Clear {
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
}
