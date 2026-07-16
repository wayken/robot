package cloud.apposs.robot.gateway.model;

import cloud.apposs.bootor.resolver.parameter.ModelParametric;
import cloud.apposs.rest.validator.checker.Digits;
import cloud.apposs.rest.validator.checker.Id;
import cloud.apposs.rest.validator.checker.NotBlank;

public class NodeModel {
    public static class Add extends ModelParametric {
        @Id
        private long aid;

        @NotBlank
        private String name;

        @NotBlank
        private String address;

        @Digits
        private int port;

        @NotBlank
        private String signature;

        @NotBlank(require = false)
        private String remark;

        public long getAid() {
            return aid;
        }

        public void setAid(long aid) {
            this.aid = aid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }

    public static class List extends ModelParametric {
        @Id
        private long aid;

        public long getAid() {
            return aid;
        }

        public void setAid(long aid) {
            this.aid = aid;
        }
    }

    public static class Information extends ModelParametric {
        @Id
        private long aid;

        @NotBlank
        private String id;

        public long getAid() {
            return aid;
        }

        public void setAid(long aid) {
            this.aid = aid;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    public static class Update extends ModelParametric {
        @Id
        private long aid;

        @Id
        private long id;

        @NotBlank(require = false)
        private String name;

        @NotBlank(require = false)
        private String address;

        @Digits(require = false)
        private int port = -1;

        @NotBlank(require = false)
        private String signature;

        @NotBlank(require = false)
        private String remark;

        public long getAid() {
            return aid;
        }

        public void setAid(long aid) {
            this.aid = aid;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }

    public static class Delete extends ModelParametric {
        @Id
        private long aid;

        @Id
        private long id;

        public long getAid() {
            return aid;
        }

        public void setAid(long aid) {
            this.aid = aid;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }
    }
}
