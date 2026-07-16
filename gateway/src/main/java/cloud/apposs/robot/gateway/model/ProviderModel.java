package cloud.apposs.robot.gateway.model;

import cloud.apposs.bootor.resolver.parameter.ModelParametric;
import cloud.apposs.rest.validator.checker.Digits;
import cloud.apposs.rest.validator.checker.Id;
import cloud.apposs.rest.validator.checker.NotBlank;
import cloud.apposs.rest.validator.checker.NotEmpty;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

public class ProviderModel {
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

    public static class Add extends ModelParametric {
        @Id
        private long aid;

        @NotBlank
        private String name;

        @NotBlank
        private String type;

        @NotBlank
        private String url;

        @NotEmpty(require = false)
        private Table<Param> models;

        @NotEmpty(require = false)
        private Table<String> keys;

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

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Table<Param> getModels() {
            return models;
        }

        public void setModels(Table<Param> models) {
            this.models = models;
        }

        public Table<String> getKeys() {
            return keys;
        }

        public void setKeys(Table<String> keys) {
            this.keys = keys;
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
        private String type;

        @NotBlank(require = false)
        private String url;

        @Digits(require = false)
        private Integer status;

        @NotEmpty(require = false)
        private Table<Param> models;

        @NotEmpty(require = false)
        private Table<String> keys;

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

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public Table<Param> getModels() {
            return models;
        }

        public void setModels(Table<Param> models) {
            this.models = models;
        }

        public Table<String> getKeys() {
            return keys;
        }

        public void setKeys(Table<String> keys) {
            this.keys = keys;
        }
    }

    public static class Delete extends ModelParametric {
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
}
