package cloud.apposs.robot.harness;

import cloud.apposs.logger.Logger;
import cloud.apposs.robot.harness.struct.MessageStruct;
import cloud.apposs.robot.harness.schedule.ScheduleJob;
import cloud.apposs.robot.harness.schedule.SchedulePath;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * 定时任务服务，一个智能体对应一个定时任务服务，提供添加、列出和删除定时任务的功能，供智能体在推理过程中调用，规则如下：
 * <pre>
 *     1. 每个会议下可以有多个定时任务，定时任务和会话绑定
 *     2. 定时任务保存在：${workspace}/${worker_id}/schedules/${session_id}.json 文件中，智能体重启后会自动加载之前的定时任务继续执行
 * </pre>
 */
public class HarnessSchedule {
    private static final String SCHEDULE_FILE_EXTENSION = ".json";

    private static final CronParser CRON_PARSER = new CronParser(
            CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)
    );

    private final HarnessWorker worker;

    private final Path schedulesDir;

    private volatile boolean running = false;

    private final ScheduledExecutorService scheduler;

    private volatile ScheduledFuture<?> pendingTick;

    private final Map<SchedulePath, Table<ScheduleJob>> scheduleJobMapping = new ConcurrentHashMap<>();

    public HarnessSchedule(HarnessWorker worker) throws Exception {
        this.worker = worker;
        this.schedulesDir = worker.getWorkspace().root().resolve("schedules");
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread schedule = new Thread(runnable, "Cron-Scheduler");
            schedule.setDaemon(true);
            return schedule;
        });
        this.start();
    }

    public synchronized void start() throws Exception {
        running = true;
        handleScheduleLoad();
        handleNextRunsRecompute();
        handleSchedulePersistence();
        handleScheduleArmTimerRun();
    }

    /**
     * 添加定时任务，保存到磁盘，并重新计算下次执行时间，调整定时器
     *
     * @param sessionId   会话ID
     * @param scheduleJob 定时任务
     */
    public synchronized void addSchedule(String sessionId, ScheduleJob scheduleJob) throws Exception {
        SchedulePath path = handleSchedulePathLoad(sessionId);
        Table<ScheduleJob> scheduleJobs = scheduleJobMapping.computeIfAbsent(path, k -> new Table<>());
        scheduleJobs.add(scheduleJob);
        handleNextRunsRecompute();
        handleSchedulePersistence();
        handleScheduleArmTimerRun();
    }

    /**
     * 列出指定会话下的所有定时任务
     *
     * @param  sessionId 会话ID
     * @return 定时任务列表，如果没有则返回空列表
     */
    public synchronized Table<ScheduleJob> getScheduleList(String sessionId) {
        for (Map.Entry<SchedulePath, Table<ScheduleJob>> data : scheduleJobMapping.entrySet()) {
            if (data.getKey().getId().equals(sessionId)) {
                return Table.builder(data.getValue());
            }
        }
        return Table.builder();
    }

    /**
     * 删除指定会话下的定时任务
     *
     * @param sessionId  会话ID
     * @param scheduleId 定时任务ID
     * @return 是否删除成功
     */
    public synchronized boolean removeSchedule(String sessionId, String scheduleId) throws Exception {
        for (Map.Entry<SchedulePath, Table<ScheduleJob>> entry : scheduleJobMapping.entrySet()) {
            if (!entry.getKey().getId().equals(sessionId)) {
                continue;
            }
            Table<ScheduleJob> scheduleJobs = entry.getValue();
            boolean removed = scheduleJobs.removeIf(data -> data.getId().equals(scheduleId));
            if (removed) {
                handleSchedulePersistence();
                handleScheduleArmTimerRun();
            }
            return removed;
        }
        return false;
    }

    /**
     * 获取或创建指定 sessionId 对应的 SchedulePath
     */
    private SchedulePath handleSchedulePathLoad(String sessionId) {
        for (SchedulePath path : scheduleJobMapping.keySet()) {
            if (path.getId().equals(sessionId)) {
                return path;
            }
        }
        Path schedulesPath = worker.getWorkspace().schedules().resolve(sessionId + SCHEDULE_FILE_EXTENSION);
        return SchedulePath.of(sessionId, schedulesPath);
    }

    /**
     * 从磁盘加载所有定时任务文件，并将定时任务信息保存在内存中，供调度器使用
     */
    private synchronized void handleScheduleLoad() throws Exception {
        // 遍历工作空间下的所有定时任务
        File[] scheduleFileList = schedulesDir.toFile().listFiles((dir, name) -> name.endsWith(SCHEDULE_FILE_EXTENSION));
        if (scheduleFileList == null || scheduleFileList.length == 0) {
            return;
        }
        for (File scheduleFile : scheduleFileList) {
            String filename = scheduleFile.getName();
            String sessionId = filename.substring(0, filename.length() - SCHEDULE_FILE_EXTENSION.length());
            SchedulePath schedulePath = SchedulePath.of(sessionId, scheduleFile.toPath());
            Set<SchedulePath> schedulePaths = scheduleJobMapping.keySet();
            boolean isSheduleNotMofified = false;
            for (SchedulePath path : schedulePaths) {
                if (path.equals(schedulePath) && !schedulePath.isModified(scheduleFile)) {
                    isSheduleNotMofified = true;
                    break;
                }
            }
            if (isSheduleNotMofified) {
                continue;
            }
            String content = new String(Files.readAllBytes(scheduleFile.toPath()), StandardCharsets.UTF_8);
            Table<Param> scheduleList = JsonUtil.parseJsonTable(content);
            if (scheduleList == null) {
                continue;
            }
            Table<ScheduleJob> scheduleJobList = Table.builder();
            for (Param data : scheduleList) {
                ScheduleJob scheduleJob = ScheduleJob.fromSource(data);
                if (scheduleJob != null) {
                    scheduleJobList.add(scheduleJob);
                }
            }
            scheduleJobMapping.put(schedulePath, scheduleJobList);
        }
    }

    /**
     * 将所有定时任务持久化到磁盘
     */
    private synchronized void handleSchedulePersistence() throws IOException {
        for (Map.Entry<SchedulePath, Table<ScheduleJob>> data : scheduleJobMapping.entrySet()) {
            SchedulePath schedulePath = data.getKey();
            Table<ScheduleJob> scheduleJobList = data.getValue();
            Table<Param> scheduleList = Table.builder();
            for (ScheduleJob scheduleJob : scheduleJobList) {
                scheduleList.add(ScheduleJob.toSource(scheduleJob));
            }
            File file = schedulePath.getPath().toFile();
            Files.write(file.toPath(), scheduleList.toJson(true).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 重新计算所有任务的下次执行时间
     */
    private void handleNextRunsRecompute() {
        long now = System.currentTimeMillis();
        for (Table<ScheduleJob> scheduleJobList : scheduleJobMapping.values()) {
            for (ScheduleJob scheduleJob : scheduleJobList) {
                if (!scheduleJob.isEnabled()) {
                    continue;
                }
                Long next = handleNextRunAtMsCompute(scheduleJob, now);
                scheduleJob.getState().setNextRunAtMs(next);
            }
        }
    }

    /**
     * 根据任务类型计算下次执行时间（毫秒时间戳）
     */
    private Long handleNextRunAtMsCompute(ScheduleJob scheduleJob, long nowMs) {
        String kind = scheduleJob.getSchedule().getKind();
        if (kind == null) {
            return null;
        }
        switch (kind) {
            case ScheduleJob.KIND_AT: {
                Long atMs = scheduleJob.getSchedule().getAtMs();
                if (atMs != null && atMs > nowMs) {
                    return atMs;
                }
                return null;
            }
            case ScheduleJob.KIND_EVERY: {
                Long everyMs = scheduleJob.getSchedule().getEveryMs();
                if (everyMs == null || everyMs <= 0) {
                    return null;
                }
                Long lastRun = scheduleJob.getState().getLastRunAtMs();
                long base = (lastRun != null) ? lastRun : nowMs;
                long next = base + everyMs;
                return next > nowMs ? next : nowMs + everyMs;
            }
            case ScheduleJob.KIND_CRON: {
                String expression = scheduleJob.getSchedule().getExpression();
                if (expression == null || expression.isEmpty()) {
                    return null;
                }
                Cron cron = CRON_PARSER.parse(expression);
                ExecutionTime time = ExecutionTime.forCron(cron);
                String timezone = scheduleJob.getSchedule().getTimezone();
                ZoneId zoneId = (timezone != null && !timezone.isEmpty()) ? ZoneId.of(timezone) : ZoneId.systemDefault();
                ZonedDateTime base = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMs), zoneId);
                Optional<ZonedDateTime> next = time.nextExecution(base);
                return next.isPresent() ? next.get().toInstant().toEpochMilli() : null;
            }
            default:
                return null;
        }
    }

    /**
     * 根据任务类型计算下次执行时间（毫秒时间戳）
     */
    private synchronized void handleScheduleArmTimerRun() {
        if (!running) {
            return;
        }
        if (pendingTick != null && !pendingTick.isDone()) {
            pendingTick.cancel(false);
            pendingTick = null;
        }
        long now = System.currentTimeMillis();
        long earliest = Long.MAX_VALUE;
        for (List<ScheduleJob> jobs : scheduleJobMapping.values()) {
            for (ScheduleJob job : jobs) {
                if (!job.isEnabled()) {
                    continue;
                }
                Long next = job.getState().getNextRunAtMs();
                if (next != null && next < earliest) {
                    earliest = next;
                }
            }
        }
        if (earliest == Long.MAX_VALUE) {
            return;
        }
        long delayMs = Math.max(0, earliest - now);
        pendingTick = scheduler.schedule(() -> {
            try {
                handleScheduleTick();
            } catch (IOException e) {
                Logger.error(e, "Schedule tick error");
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 定时器触发，执行到期任务
     */
    private void handleScheduleTick() throws IOException {
        long now = System.currentTimeMillis();
        List<ScheduleJob> scheduleToRemove = new ArrayList<>();
        synchronized (this) {
            for (Map.Entry<SchedulePath, Table<ScheduleJob>> schedulePathListEntry : scheduleJobMapping.entrySet()) {
                SchedulePath schedulePath = schedulePathListEntry.getKey();
                Table<ScheduleJob> scheduleJobList = schedulePathListEntry.getValue();
                for (ScheduleJob scheduleJob : scheduleJobList) {
                    if (!scheduleJob.isEnabled()) {
                        continue;
                    }
                    Long next = scheduleJob.getState().getNextRunAtMs();
                    if (next == null || next > now) {
                        continue;
                    }
                    MessageStruct message = new MessageStruct();
                    message.setWid(worker.getId());
                    message.setSid(schedulePath.getId());
                    String remider = String.format("[Scheduled Task] Timer finished.\n\nTask '%s' has been triggered.\nScheduled instruction: %s",
                            scheduleJob.getId(), scheduleJob.getMessage());
                    message.setMessage(remider);
                    worker.getFramework().getMessageBus().publishInboundMessage(message);
                    scheduleJob.getState().setLastRunAtMs(now);
                    scheduleJob.setUpdatedAtMs(now);
                    if (scheduleJob.isDeleteAfterRun()) {
                        scheduleToRemove.add(scheduleJob);
                    }
                }
                scheduleJobList.removeAll(scheduleToRemove);
                scheduleToRemove.clear();
            }
            handleNextRunsRecompute();
            handleSchedulePersistence();
            handleScheduleArmTimerRun();
        }
    }

    public void shutdown() {
        running = false;
        scheduler.shutdown();
    }

    public static interface ScheduleRunner {
        void run(ScheduleJob scheduleJob, long nowMs);
    }
}
