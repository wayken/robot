package cloud.apposs.robot.worker.service;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.harness.setting.AIModelSetting;
import cloud.apposs.robot.harness.setting.AIProviderSetting;
import cloud.apposs.robot.worker.WorkerFramework;
import cloud.apposs.robot.worker.WorkerManagement.ManagementProviderSetting;
import cloud.apposs.robot.worker.service.model.WorkerProviderModel;
import cloud.apposs.util.Table;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Component
public class WorkerProfileService {
    @Autowired
    private WorkerFramework framework;

    public AIProviderSetting getPrimaryProviderSetting(WorkerProviderModel.Provider request) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(request.getWid());
        if (worker == null) {
            throw new IllegalArgumentException("Worker " + request.getWid() + " not found");
        }
        return worker.getProfile().getPrimaryProvider();
    }

    /**
     * 更新provider配置并重载Worker服务
     * <pre>
     *   1. 前端传递完整的provider列表（含主模型和备用模型）
     *   2. 从管理端配置中补全keys信息
     *   3. 直接覆盖profile.yaml中的provider字段
     *   4. 调用worker.reload()使配置生效
     * </pre>
     */
    public void updateProvider(WorkerProviderModel.Update request) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(request.getWid());
        if (worker == null) {
            throw new IllegalArgumentException("Worker " + request.getWid() + " not found");
        }
        Table<WorkerProviderModel.ProviderItem> providerList = request.getProvider();
        List<AIProviderSetting> providerSettings = new ArrayList<>();
        for (WorkerProviderModel.ProviderItem data : providerList) {
            AIProviderSetting setting = new AIProviderSetting();
            setting.setName(data.getName());
            setting.setType(data.getType());
            setting.setPrimary(data.isPrimary());
            setting.setLink(data.getLink());
            AIModelSetting modelSetting = new AIModelSetting();
            modelSetting.setName(data.getModel().getString("name"));
            List<Integer> modelProperties = data.getModel().getList("properties");
            if (modelProperties != null) {
                modelSetting.setProperties(modelProperties);
            }
            setting.setModel(modelSetting);
            setting.setTemperature(data.getTemperature());
            setting.setTopP(data.getTopP());
            setting.setStream(data.isStream());
            setting.setTimeout(data.getTimeout());
            ManagementProviderSetting matched = handleProviderFindByName(data.getName());
            if (matched != null && matched.getKeys() != null && !matched.getKeys().isEmpty()) {
                setting.setKeys(new ArrayList<>(matched.getKeys()));
            }
            providerSettings.add(setting);
        }
        handleProfilePersist(worker, providerSettings);
        worker.reload();
    }

    // 从管理端provider配置中根据名称查找对应的服务商
    private ManagementProviderSetting handleProviderFindByName(String providerName) {
        List<ManagementProviderSetting> providerList = framework.getManagement().getProvider();
        if (providerList == null) {
            return null;
        }
        for (ManagementProviderSetting provider : providerList) {
            if (providerName.equals(provider.getName())) {
                return provider;
            }
        }
        return null;
    }

    // 将provider配置持久化到profile.yaml文件
    private void handleProfilePersist(HarnessWorker worker, List<AIProviderSetting> providerSettings) throws Exception {
        Path profilePath = worker.getWorkspace().root().resolve("profile.yaml");

        // 读取现有的yaml内容
        Map<String, Object> root;
        if (Files.exists(profilePath)) {
            Yaml yaml = new Yaml();
            String content = new String(Files.readAllBytes(profilePath));
            root = yaml.load(content);
            if (root == null) {
                root = new LinkedHashMap<>();
            }
        } else {
            root = new LinkedHashMap<>();
        }

        // 构建provider列表
        List<Map<String, Object>> providerList = new ArrayList<>();
        for (AIProviderSetting provider : providerSettings) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", provider.getName());
            data.put("type", provider.getType());
            data.put("primary", provider.isPrimary());
            if (provider.getLink() != null) {
                data.put("link", provider.getLink());
            }
            data.put("model", handleModelSettingToMap(provider.getModel()));
            if (provider.getKeys() != null && !provider.getKeys().isEmpty()) {
                data.put("keys", new ArrayList<>(provider.getKeys()));
            }
            data.put("temperature", provider.getTemperature());
            data.put("maxTokens", provider.getMaxTokens());
            data.put("topP", provider.getTopP());
            data.put("stream", provider.isStream());
            data.put("timeout", provider.getTimeout());
            providerList.add(data);
        }

        // 覆盖provider字段
        root.put("provider", providerList);

        // 写入yaml文件
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);
        String content = yaml.dump(root);
        Files.write(profilePath, content.getBytes());
    }

    private Map<String, Object> handleModelSettingToMap(AIModelSetting model) {
        if (model == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", model.getName());
        if (model.getProperties() != null && !model.getProperties().isEmpty()) {
            map.put("properties", new ArrayList<>(model.getProperties()));
        }
        return map;
    }
}
