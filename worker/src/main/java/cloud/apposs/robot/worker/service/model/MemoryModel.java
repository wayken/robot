package cloud.apposs.robot.worker.service.model;

import cloud.apposs.rest.validator.checker.NotBlank;

public class MemoryModel {
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

    public static class Read {
        @NotBlank
        private String wid;

        @NotBlank
        private String filename;

        public String getWid() {
            return wid;
        }

        public void setWid(String wid) {
            this.wid = wid;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }
    }

    public static class Write {
        @NotBlank
        private String wid;

        @NotBlank
        private String filename;

        private String content = "";

        public String getWid() {
            return wid;
        }

        public void setWid(String wid) {
            this.wid = wid;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public static class Delete {
        @NotBlank
        private String wid;

        @NotBlank
        private String filename;

        public String getWid() {
            return wid;
        }

        public void setWid(String wid) {
            this.wid = wid;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }
    }

    public static class Rename {
        @NotBlank
        private String wid;

        @NotBlank
        private String filename;

        @NotBlank
        private String newFilename;

        public String getWid() {
            return wid;
        }

        public void setWid(String wid) {
            this.wid = wid;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getNewFilename() {
            return newFilename;
        }

        public void setNewFilename(String newFilename) {
            this.newFilename = newFilename;
        }
    }
}
