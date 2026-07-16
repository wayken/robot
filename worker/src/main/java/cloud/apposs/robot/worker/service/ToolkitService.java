package cloud.apposs.robot.worker.service;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.harness.setting.AIToolkitSetting;
import cloud.apposs.robot.harness.tool.ToolFactory;
import cloud.apposs.robot.worker.WorkerFramework;
import cloud.apposs.robot.worker.service.model.ToolkitModel;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Component
public class ToolkitService {
    @Autowired
    private WorkerFramework framework;

    /**
     * 获取工具列表，返回所有内置工具及其启用状态和配置属性
     */
    public Table<Param> getToolkitList(ToolkitModel.Index request) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(request.getWid());
        if (worker == null) {
            throw new IllegalArgumentException("Worker " + request.getWid() + " not found");
        }

        List<AIToolkitSetting> toolkitSettings = worker.getProfile().getToolkit();
        Map<String, AIToolkitSetting> settingMap = new HashMap<>();
        if (toolkitSettings != null) {
            for (AIToolkitSetting setting : toolkitSettings) {
                settingMap.put(setting.getName(), setting);
            }
        }

        Table<Param> dataList = Table.builder();
        String[] builtinNames = ToolFactory.BUILDIN_TOOL_NAMES;
        for (String name : builtinNames) {
            AIToolkitSetting setting = settingMap.get(name);
            Param item = Param.builder("name", name)
                    .setBoolean("enabled", setting != null ? setting.isEnabled() : true)
                    .setBoolean("builtin", true);
            if (setting != null && setting.getProperties() != null) {
                item.setObject("properties", setting.getProperties());
            } else {
                item.setObject("properties", new Param());
            }
            dataList.add(item);
        }

        // 添加非内置工具（用户自定义的 toolkit 条目）
        if (toolkitSettings != null) {
            Set<String> builtinSet = new HashSet<>(Arrays.asList(builtinNames));
            for (AIToolkitSetting setting : toolkitSettings) {
                if (!builtinSet.contains(setting.getName())) {
                    Param item = Param.builder("name", setting.getName())
                            .setBoolean("enabled", setting.isEnabled())
                            .setBoolean("builtin", false);
                    if (setting.getProperties() != null) {
                        item.setObject("properties", setting.getProperties());
                    } else {
                        item.setObject("properties", new Param());
                    }
                    dataList.add(item);
                }
            }
        }

        return dataList;
    }

    /**
     * 切换工具启用/禁用状态
     */
    public boolean switchTool(ToolkitModel.Switch request) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(request.getWid());
        if (worker == null) {
            throw new IllegalArgumentException("Worker " + request.getWid() + " not found");
        }

        List<AIToolkitSetting> toolkitSettings = worker.getProfile().getToolkit();
        if (toolkitSettings == null) {
            toolkitSettings = new ArrayList<>();
            worker.getProfile().setToolkit(toolkitSettings);
        }

        // 查找并更新对应工具的 enabled 状态
        boolean found = false;
        for (AIToolkitSetting setting : toolkitSettings) {
            if (request.getName().equals(setting.getName())) {
                setting.setEnabled(request.isEnabled());
                found = true;
                break;
            }
        }

        // 如果没找到，说明是内置工具首次配置，新增条目
        if (!found) {
            AIToolkitSetting newSetting = new AIToolkitSetting();
            newSetting.setName(request.getName());
            newSetting.setEnabled(request.isEnabled());
            newSetting.setProperties(new Param());
            toolkitSettings.add(newSetting);
        }

        handleToolkitPersist(worker, toolkitSettings);
        worker.reload();
        return true;
    }

    /**
     * 更新工具的环境变量（properties）
     */
    public boolean updateToolProperties(ToolkitModel.UpdateProperties request) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(request.getWid());
        if (worker == null) {
            throw new IllegalArgumentException("Worker " + request.getWid() + " not found");
        }

        List<AIToolkitSetting> toolkitSettings = worker.getProfile().getToolkit();
        if (toolkitSettings == null) {
            toolkitSettings = new ArrayList<>();
            worker.getProfile().setToolkit(toolkitSettings);
        }

        // 查找并更新对应工具的 properties
        boolean found = false;
        for (AIToolkitSetting setting : toolkitSettings) {
            if (request.getName().equals(setting.getName())) {
                setting.setProperties(request.getProperties());
                found = true;
                break;
            }
        }

        // 如果没找到，新增条目
        if (!found) {
            AIToolkitSetting newSetting = new AIToolkitSetting();
            newSetting.setName(request.getName());
            newSetting.setEnabled(true);
            newSetting.setProperties(request.getProperties());
            toolkitSettings.add(newSetting);
        }

        handleToolkitPersist(worker, toolkitSettings);
        worker.reload();
        return true;
    }

    /**
     * 将 toolkit 配置持久化到 profile.yaml 文件
     */
    private void handleToolkitPersist(HarnessWorker worker, List<AIToolkitSetting> toolkitSettings) throws Exception {
        Path profilePath = worker.getWorkspace().root().resolve("profile.yaml");

        // 读取现有的 yaml 内容
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

        // 构建 toolkit 列表
        List<Map<String, Object>> toolkitList = new ArrayList<>();
        for (AIToolkitSetting setting : toolkitSettings) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", setting.getName());
            data.put("enabled", setting.isEnabled());
            if (setting.getProperties() != null && !setting.getProperties().isEmpty()) {
                data.put("properties", new LinkedHashMap<>(setting.getProperties()));
            }
            toolkitList.add(data);
        }

        // 覆盖 toolkit 字段
        root.put("toolkit", toolkitList);

        // 写入 yaml 文件
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);
        String content = yaml.dump(root);
        Files.write(profilePath, content.getBytes());
    }
}
