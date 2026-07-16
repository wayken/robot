package cloud.apposs.robot.worker;

import cloud.apposs.configure.Value;
import cloud.apposs.configure.YamlConfigParser;
import cloud.apposs.robot.worker.util.Ids;
import cloud.apposs.util.Param;
import cloud.apposs.util.StrUtil;
import cloud.apposs.util.Table;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作节点配置管理模块，负责接收管理端下发的配置，并将其写入工作空间`${workspace}/management.yaml`，同时提供配置的读取和更新功能
 */
public class WorkerManagement {
    // 工作节点配置文件路径，位于工作空间下的`management.yaml`文件
    private static final String RESOURCE_PATH = "management.yaml";

    // 工作节点API Key，主要用于管理端和工作节点之间的通信认证，工作节点服务会通过此Key来验证管理端请求的合法性
    @Value("management.api_key")
    private String managementApiKey = "";

    // 工作节点ID
    @Value("management.node_id")
    private Long managementNodeId;

    // AI服务商配置列表
    @Value("management.provider")
    private Table<ManagementProviderSetting> provider = Table.builder();

    /**
     * 从工作空间`${workspace}/management.yaml`检测配置是否未初始化，若是则生成值并回写
     *
     * @param config WorkerConfig对象
     */
    public static WorkerManagement initialize(WorkerConfig config) throws Exception {
        WorkerManagement management = new WorkerManagement();
        Path filePath = Paths.get(config.getWorkhome(), RESOURCE_PATH);
        File file = filePath.toFile();
        if (file.exists()) {
            YamlConfigParser parser = new YamlConfigParser();
            parser.parse(management, file.getAbsolutePath());
        } else {
            if (management.managementNodeId == null) {
                management.managementNodeId = Ids.getInstance().nextId();
            }
            if (StrUtil.isEmpty(management.managementApiKey)) {
                management.managementApiKey = "sk-" + handleRandomStringGenerate(32);
            }
            handleYamlPersist(filePath, management);
        }
        return management;
    }

    public String getManagementApiKey() {
        return managementApiKey;
    }

    public void setManagementApiKey(String managementApiKey) {
        this.managementApiKey = managementApiKey;
    }

    public Long getManagementNodeId() {
        return managementNodeId;
    }

    public void setManagementNodeId(Long managementNodeId) {
        this.managementNodeId = managementNodeId;
    }

    public List<ManagementProviderSetting> getProvider() {
        return provider;
    }

    public void setProvider(Table<ManagementProviderSetting> provider) {
        this.provider = provider;
    }

    public void syncProvider(WorkerConfig config, Table<ManagementProviderSetting> provider) throws IOException {
        this.provider = provider;
        Path filePath = Paths.get(config.getWorkhome(), RESOURCE_PATH);
        handleYamlPersist(filePath, this);
    }

    public static class ManagementProviderSetting {
        // 服务商名称，如 silicon、openai
        private String name;

        // 服务商接口类型，如 openai、gemini
        private String type;

        // API请求地址
        private String url;

        // 使用的模型列表
        private List<Param> models;

        // API Key列表，支持多个Key轮询
        private List<String> keys = new ArrayList<>();

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

        public List<Param> getModels() {
            return models;
        }

        public void setModels(List<Param> models) {
            this.models = models;
        }

        public List<String> getKeys() {
            return keys;
        }

        public void setKeys(List<String> keys) {
            this.keys = keys;
        }
    }

    private static String handleRandomStringGenerate(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static void handleYamlPersist(Path filePath, WorkerManagement management) throws IOException {
        Map<String, Object> managementMap = new LinkedHashMap<>();
        managementMap.put("node_id", management.managementNodeId);
        managementMap.put("api_key", management.managementApiKey);
        List<Map<String, Object>> providerList = new ArrayList<>();
        if (management.provider != null) {
            for (ManagementProviderSetting setting : management.provider) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", setting.getName());
                item.put("type", setting.getType());
                item.put("url", setting.getUrl());
                item.put("models", setting.getModels());
                item.put("keys", setting.getKeys());
                providerList.add(item);
            }
        }
        managementMap.put("provider", providerList);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("management", managementMap);
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        String content = yaml.dump(root);
        if (!Files.exists(filePath.getParent())) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath, content.getBytes());
    }
}
