package cloud.apposs.robot.harness;

import cloud.apposs.logger.Logger;
import cloud.apposs.okhttp.HttpBuilder;
import cloud.apposs.okhttp.OkHttp;
import cloud.apposs.robot.harness.bus.ILifeCycleHook;
import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.robot.harness.provider.AIResponse;
import cloud.apposs.robot.harness.struct.MessageStruct;
import cloud.apposs.robot.harness.struct.WorkerStruct;
import cloud.apposs.robot.harness.util.ResourceUtil;
import cloud.apposs.robot.harness.util.Strings;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class HarnessFramework implements Closeable {
    // 需要从内置资源中复制到工作空间的顶层目录/文件白名单
    private static final Set<String> BUILDIN_INCLUDES = new HashSet<>(
            Arrays.asList("memory", "rules", "skills", "profile.yaml")
    );

    private final OkHttp httpClient;

    private HarnessSetting setting;

    private final HarnessMessageBus messageBus;

    private final HarnessLogger logger;

    private final Map<String, HarnessWorker> workers;

    public HarnessFramework(HarnessSetting setting) throws Exception {
        this.httpClient = HttpBuilder.builder()
                .poolConnections(setting.getProxyPoolSize())
                .connectTimeout(setting.getProxyConnectTimeout())
                .socketTimeout(setting.getProxySocketTimeout()).loopSize(1).build();
        this.setting = setting;
        this.messageBus = new HarnessMessageBus(setting);
        this.logger = new HarnessLogger(setting);
        this.workers = new ConcurrentHashMap<>();
        this.bootstrap();
    }

    public OkHttp getHttpClient() {
        return httpClient;
    }

    public HarnessSetting getSetting() {
        return setting;
    }

    public HarnessMessageBus getMessageBus() {
        return messageBus;
    }

    public HarnessLogger getLogger() {
        return logger;
    }

    public HarnessWorker getWorker(String workerId) {
        return workers.get(workerId);
    }

    /**
     * 初始化Worker工作空间
     *
     * @param model   Worker模型数据
     */
    public void initialize(WorkerStruct model) throws Exception {
        // 1. 创建工作空间
        String workspace = setting.getWorkspace() + File.separator + model.getId();
        Path workspacePath = Paths.get(workspace);
        if (Files.exists(workspacePath)) {
            throw new Exception("Worker " + model.getId() + " already exists.");
        }
        Files.createDirectories(workspacePath);
        // 2. 遍历复制内置模板文件到工作空间
        URL anchorUrl = HarnessFramework.class.getResource("/profile.yaml");
        if (anchorUrl == null) {
            throw new Exception("Buildin anchor resource 'profile.yaml' not found in harness classpath.");
        }
        String anchorStr = anchorUrl.toString();
        String rootStr = anchorStr.substring(0, anchorStr.lastIndexOf('/') + 1);
        URL buildinUrl = new URL(rootStr);
        if (buildinUrl == null) {
            throw new Exception("Buildin directory not found in classpath.");
        }
        URI buildinUri = buildinUrl.toURI();
        handleBuildinResourcesCopy(buildinUri, workspacePath);
        // 3. 创建工作实例并注册到框架中
        String workerId = model.getId();
        HarnessWorker worker = new HarnessWorker(workerId, this).start();
        workers.put(workerId, worker);
        Logger.info("Worker " + workerId + " initialized successfully at " + workspacePath);
    }

    /**
     * 启动Worker框架，加载工作空间中所有智能体
     * <pre>
     *     1. 加载工作空间中所有智能体目录列表
     *     2. 为每个智能体创建对应的Worker实例，并加载其配置参数并启动
     *     3. 启动消息总线监听（异步线程）
     * </pre>
     */
    public void bootstrap() throws Exception {
        // 1. 列出工作空间中所有智能体目录
        String workspace = setting.getWorkspace();
        File workspaceDir = new File(workspace);
        if (!workspaceDir.exists()) {
            Files.createDirectories(workspaceDir.toPath());
        }
        File[] workers = workspaceDir.listFiles(File::isDirectory);
        if (workers == null) {
            return;
        }
        // 2. 为每个智能体创建对应的Worker实例，并加载其配置参数并启动
        for (File workerDir : workers) {
            String workerId = workerDir.getName();
            HarnessWorker worker = new HarnessWorker(workerId, this).start();
            this.workers.put(workerId, worker);
            Logger.info("Harness Worker " + workerId + " loaded from workspace " + workspace + " successfully.");
        }
        // 3. 启动消息总线监听（异步线程）
        Thread messageBusThread = new Thread(this::handleMessageBusRun, "Harness-Message-Bus");
        messageBusThread.setDaemon(true);
        messageBusThread.start();
    }

    /**
     * 运行指定Worker迭代循环（核心推理引擎）
     *
     * @param message     输入消息
     * @param messageHook 消息钩子，供智能体在迭代过程中调用以发送消息或更新状态
     */
    public boolean harness(MessageStruct message, IMessageHook messageHook) throws Exception {
        String workerId = message.getWid();
        if (Strings.isBlank(workerId)) {
            throw new Exception("Harness Worker ID cannot be empty.");
        }
        HarnessWorker worker = workers.get(workerId);
        if (worker == null) {
            throw new Exception("Worker " + workerId + " not found.");
        }
        return worker.work(message, messageHook);
    }

    /**
     * 中断指定Worker中某个会话正在进行的AI迭代循环
     *
     * @param  workerId 智能体ID
     * @param  sid      会话ID
     * @return 是否成功中断（false 表示Worker不存在或该会话当前没有正在运行的迭代）
     */
    public boolean interrupt(String workerId, String sid) throws Exception {
        HarnessWorker worker = workers.get(workerId);
        if (worker == null) {
            throw new Exception("Worker " + workerId + " not found.");
        }
        return worker.interrupt(sid);
    }

    /**
     * 移除Worker工作空间，清理所有资源并删除磁盘数据
     * <pre>
     *     1. 停止并销毁 Worker 实例（关闭定时任务、MCP连接、消息渠道等）
     *     2. 从 workers 注册表中注销
     *     3. 递归删除工作空间目录
     * </pre>
     *
     * @param workerId 智能体ID
     */
    public boolean remove(String workerId) throws Exception {
        HarnessWorker worker = workers.remove(workerId);
        if (worker == null) {
            Logger.warn("Worker %s not found.", workerId);
            return false;
        }
        // 停止智能体所有后台服务
        worker.shutdown();
        // 删除工作空间目录
        String workspace = setting.getWorkspace() + File.separator + workerId;
        Path workspacePath = Paths.get(workspace);
        if (Files.exists(workspacePath)) {
            Files.walkFileTree(workspacePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    deleteWithRetry(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    deleteWithRetry(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        Logger.info("Worker %s removed successfully.", workerId);
        return true;
    }

    private static void deleteWithRetry(Path path) throws IOException {
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            try {
                Files.delete(path);
                return;
            } catch (FileSystemException e) {
                if (i == maxRetries - 1) {
                    throw e;
                }
                try {
                    Thread.sleep(200 * (i + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    /**
     * 重新加载配置，更新所有Worker的环境配置参数
     *
     * @param workerId 当前智能体ID
     */
    public void reload(String workerId) throws Exception {
        HarnessWorker worker = workers.get(workerId);
        if (worker != null) {
            worker.reload();
        }
    }

    private void handleMessageBusRun() {
        for (;;) {
            try {
                MessageStruct message = messageBus.subscribeInboundMessage();
                String wid = message.getWid();
                messageBus.triggerLifeCycleHook(wid, message.getSid(), message.getRid(), ILifeCycleHook.Phase.PHASE_MESSAGE_INBOUND, message);
                HarnessWorker worker = workers.get(wid);
                if (worker == null) {
                    Logger.warn("No worker found for message with wid: " + wid);
                    continue;
                }
                worker.work(message, new IMessageHook() {
                    @Override
                    public void onProcessing(String sid, String rid, AIResponse response) throws Exception {
                        messageBus.triggerLifeCycleHook(wid, sid, rid, ILifeCycleHook.Phase.PHASE_MESSAGE_PROCESSING, message);
                    }

                    @Override
                    public void onCompletion(String sid, String rid, AIResponse response) throws Exception {
                        messageBus.triggerLifeCycleHook(wid, sid, rid, ILifeCycleHook.Phase.PHASE_POST_COMPLETION, response);
                    }

                    @Override
                    public void onError(Throwable cause) throws Exception {
                        messageBus.triggerLifeCycleHook(wid, message.getSid(), message.getRid(), ILifeCycleHook.Phase.PHASE_WORK_FAILURE, cause);
                    }
                });
            } catch (Exception e) {
                Logger.error(e, "Harness Error processing message: " + e.getMessage());
            }
        }
    }

    private void handleBuildinResourcesCopy(URI uri, Path workspacePath) throws IOException {
        if (ResourceUtil.isSchemaResourceFile(uri.getScheme())) {
            // jar 包场景：FileSystem 必须保持打开状态直到 walkFileTree 完成
            try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                Path sourcePath = fs.getPath("/");
                handleFilesCopy(sourcePath, workspacePath);
            }
        } else {
            // 本地文件系统场景（开发期 IDE 运行）
            Path sourcePath = Paths.get(uri);
            handleFilesCopy(sourcePath, workspacePath);
        }
    }

    private void handleFilesCopy(Path sourcePath, Path workspacePath) throws IOException {
        Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                // 跳过不在白名单内的顶层目录（如 cloud/ 等编译产物目录）
                if (!dir.equals(sourcePath)) {
                    Path relative = sourcePath.relativize(dir);
                    // 只检查第一层子目录
                    if (relative.getNameCount() == 1) {
                        String dirName = relative.toString();
                        if (!BUILDIN_INCLUDES.contains(dirName)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path source, BasicFileAttributes attrs) throws IOException {
                Path relative = sourcePath.relativize(source);
                // 跳过不在白名单内的顶层文件
                if (relative.getNameCount() == 1 && !BUILDIN_INCLUDES.contains(relative.toString())) {
                    return FileVisitResult.CONTINUE;
                }
                Path destination = workspacePath.resolve(relative.toString());
                Files.createDirectories(destination.getParent());
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public void close() {
        httpClient.close();
        for (HarnessWorker worker : workers.values()) {
            worker.shutdown();
        }
    }
}
