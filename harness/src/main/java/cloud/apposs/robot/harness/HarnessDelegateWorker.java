package cloud.apposs.robot.harness;

import cloud.apposs.configure.YamlConfigParser;
import cloud.apposs.react.React;
import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.robot.harness.delegate.DelegateWorkerIterationLoop;
import cloud.apposs.robot.harness.delegate.DelegateWorkerProfile;
import cloud.apposs.robot.harness.delegate.DelegateWorkerSubscriber;
import cloud.apposs.robot.harness.delegate.DelegateWorkerToolKit;
import cloud.apposs.robot.harness.message.AIMessages;
import cloud.apposs.robot.harness.message.kind.AISystemMessage;
import cloud.apposs.robot.harness.message.kind.AIUserMessage;
import cloud.apposs.robot.harness.provider.AIRequest;
import cloud.apposs.robot.harness.setting.AIProviderSetting;
import cloud.apposs.robot.harness.util.PromptLoader;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class HarnessDelegateWorker {
    public static final String PROFILE_NAME = "profile.yaml";

    private final HarnessWorker worker;

    public HarnessDelegateWorker(HarnessWorker worker) {
        this.worker = worker;
    }

    public String addWorker(String name, String description) throws IOException {
        UUID workerId = UUID.randomUUID();
        Path workspace = worker.getWorkspace().delegates().resolve(workerId.toString());
        DelegateWorkerProfile profile = new DelegateWorkerProfile();
        profile.setName(name);
        profile.setDescription(description);
        profile.setProvider(worker.getProfile().getProvider());
        // 转换成YAML格式并写入文件
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Representer representer = new Representer(options);
        representer.addClassTag(DelegateWorkerProfile.class, Tag.MAP);
        Yaml yaml = new Yaml(representer, options);
        String yamlContent = yaml.dump(profile);
        Path profilePath = workspace.resolve(PROFILE_NAME);
        if (!Files.exists(workspace)) {
            Files.createDirectories(workspace);
        }
        Files.write(profilePath, yamlContent.getBytes());
        return workerId.toString();
    }

    public Table<Param> getWorkers() throws Exception {
        File workspace = worker.getWorkspace().delegates().toFile();
        Table<Param> dataList = new Table<>();
        if (!workspace.exists() || !workspace.isDirectory()) {
            return dataList;
        }
        File[] delegateWorkers = workspace.listFiles();
        if (delegateWorkers == null) {
            return dataList;
        }
        YamlConfigParser yamlParser = new YamlConfigParser();
        for (File delegateWorker : delegateWorkers) {
            if (!delegateWorker.isDirectory()) {
                continue;
            }
            File profileFile = new File(delegateWorker, PROFILE_NAME);
            if (!profileFile.exists()) {
                continue;
            }
            DelegateWorkerProfile profile = new DelegateWorkerProfile();
            yamlParser.parse(profile, profileFile.getAbsolutePath());
            if (!profile.isEnabled()) {
                continue;
            }
            Param infomation = new Param();
            infomation.put("id", delegateWorker.getName());
            infomation.put("name", profile.getName());
            infomation.put("description", profile.getDescription());
            dataList.add(infomation);
        }
        return dataList;
    }

    public React<String> assignWorker(String id, String sid, String rid, String message, IMessageHook messageHook) throws Exception {
        Path workspace = worker.getWorkspace().delegates().resolve(id);
        Path profilePath = workspace.resolve(PROFILE_NAME);
        if (!Files.exists(profilePath)) {
            return React.just("Error: Profile not found for delegate worker: " + id);
        }
        YamlConfigParser yamlParser = new YamlConfigParser();
        DelegateWorkerProfile profile = new DelegateWorkerProfile();
        yamlParser.parse(profile, profilePath.toAbsolutePath().toString());
        AIProviderSetting provider = profile.getPrimaryProvider();
        if (provider == null) {
            return React.just("Error: No AI provider configured for delegate worker: " + id);
        }
        DelegateWorkerToolKit toolkit = new DelegateWorkerToolKit(profile);
        AIMessages messages = buildMessages(message, profile.getDescription());
        AIRequest request = AIRequest.of(worker.getId(), sid, rid, messages, toolkit.getToolDefinitions());
        DelegateWorkerIterationLoop iterationLoop = new DelegateWorkerIterationLoop(worker, profile, request, messageHook);
        return React.create(subscriber -> {
            try {
                DelegateWorkerSubscriber delegateWorkerSubscriber = new DelegateWorkerSubscriber(subscriber);
                worker.completions(request, provider).loop(iterationLoop).subscribe(delegateWorkerSubscriber).start();
            } catch (Exception cause) {
                subscriber.onError(cause);
            }
        });
    }

    private AIMessages buildMessages(String message, String description) throws Exception {
        AIMessages messages = new AIMessages();
        Param replacement = Param.builder("description", description);
        String prompt = PromptLoader.readPrompt("delegate/runtime", replacement);
        messages.append(new AISystemMessage(prompt));
        messages.append(new AIUserMessage(message));
        return messages;
    }
}
