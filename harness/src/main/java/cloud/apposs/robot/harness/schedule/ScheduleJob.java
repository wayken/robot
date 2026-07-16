package cloud.apposs.robot.harness.schedule;

import cloud.apposs.util.Param;

/**
 * 定时任务，包含定时任务的基本信息和属性，数据结构如下：
 * <pre>
 *     {
 *       "id": "aab9d439",
 *       "message": "💧 喝水时间到了！记得补充水分哦~"
 *       "enabled": true,
 *       "schedule": {
 *         "kind": "cron",
 *         "atMs": null,
 *         "everyMs": null,
 *         "expression": "xxxxxxxx",
 *         "timezone": "Asia/Shanghai"
 *       },
 *       "state": {
 *         "nextRunAtMs": 1776837300000,
 *         "lastRunAtMs": null,
 *         "lastStatus": null,
 *         "lastError": null
 *       },
 *       "createdAtMs": 1776751774223,
 *       "updatedAtMs": 1776751774223,
 *       "deleteAfterRun": false
 *     }
 * </pre>
 */
public class ScheduleJob {
    public static final String KIND_AT = "at";
    public static final String KIND_CRON = "cron";
    public static final String KIND_EVERY = "every";

    private final String id;

    private final String message;

    private volatile boolean enabled = true;

    private final State state = new State();

    private final Schedule schedule;

    private final long createdAtMs;

    private volatile long updatedAtMs;

    private final boolean deleteAfterRun;

    public ScheduleJob(String id, String message, Schedule schedule, boolean deleteAfterRun) {
        this.id = id;
        this.message = message;
        this.schedule = schedule;
        this.createdAtMs = System.currentTimeMillis();
        this.updatedAtMs = this.createdAtMs;
        this.deleteAfterRun = deleteAfterRun;
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public State getState() {
        return state;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public long getCreatedAtMs() {
        return createdAtMs;
    }

    public long getUpdatedAtMs() {
        return updatedAtMs;
    }

    public void setUpdatedAtMs(long updatedAtMs) {
        this.updatedAtMs = updatedAtMs;
    }

    public boolean isDeleteAfterRun() {
        return deleteAfterRun;
    }

    public static ScheduleJob fromSource(Param data) {
        if (data == null) {
            return null;
        }
        String id = data.getString("id");
        String message = data.getString("message");
        if (id == null || message == null) {
            return null;
        }
        Schedule schedule = null;
        Param scheduleData = data.getParam("schedule");
        if (scheduleData != null) {
            String kind = scheduleData.getString("kind");
            Long atMs = scheduleData.getLong("atMs");
            Long everyMs = scheduleData.getLong("everyMs");
            String expression = scheduleData.getString("expression");
            String timezone = scheduleData.getString("timezone");
            schedule = new Schedule(kind, atMs, everyMs, expression, timezone);
        }
        boolean deleteAfterRun = data.getBoolean("deleteAfterRun", false);
        ScheduleJob infomation = new ScheduleJob(id, message, schedule, deleteAfterRun);
        infomation.enabled = data.getBoolean("enabled", true);
        return infomation;
    }

    public static Param toSource(ScheduleJob scheduleJob) {
        if (scheduleJob == null) {
            return null;
        }
        Param infomation = Param.builder();
        infomation.put("id", scheduleJob.id);
        infomation.put("enabled", scheduleJob.enabled);
        infomation.put("message", scheduleJob.message);
        infomation.put("deleteAfterRun", scheduleJob.deleteAfterRun);
        Schedule schedule = scheduleJob.schedule;
        if (schedule != null) {
            Param scheduleData = Param.builder();
            scheduleData.put("kind", schedule.getKind());
            scheduleData.put("atMs", schedule.getAtMs());
            scheduleData.put("everyMs", schedule.getEveryMs());
            scheduleData.put("expression", schedule.getExpression());
            scheduleData.put("timezone", schedule.getTimezone());
            infomation.put("schedule", scheduleData);
        }
        State state = scheduleJob.state;
        if (state != null) {
            Param stateData = Param.builder();
            stateData.put("nextRunAtMs", state.getNextRunAtMs());
            stateData.put("lastRunAtMs", state.getLastRunAtMs());
            stateData.put("lastStatus", state.getLastStatus());
            stateData.put("lastError", state.getLastError());
            infomation.put("state", stateData);
        }
        return infomation;
    }

    public static class State {
        private Long nextRunAtMs;

        private Long lastRunAtMs;

        // "ok" | "error" | "skipped"
        private String lastStatus;

        private String lastError;

        public Long getNextRunAtMs() {
            return nextRunAtMs;
        }

        public void setNextRunAtMs(Long nextRunAtMs) {
            this.nextRunAtMs = nextRunAtMs;
        }

        public Long getLastRunAtMs() {
            return lastRunAtMs;
        }

        public void setLastRunAtMs(Long lastRunAtMs) {
            this.lastRunAtMs = lastRunAtMs;
        }

        public String getLastStatus() {
            return lastStatus;
        }

        public void setLastStatus(String lastStatus) {
            this.lastStatus = lastStatus;
        }

        public String getLastError() {
            return lastError;
        }

        public void setLastError(String lastError) {
            this.lastError = lastError;
        }
    }

    public static class Schedule {
        private final String kind;

        private final Long atMs;

        private final Long everyMs;

        private final String expression;

        private final String timezone;

        private Schedule(String kind, Long atMs, Long everyMs, String expression, String timezone) {
            this.kind = kind;
            this.atMs = atMs;
            this.everyMs = everyMs;
            this.expression = expression;
            this.timezone = timezone;
        }

        public static Schedule at(long atMs) {
            return new Schedule(KIND_AT, atMs, null, null, null);
        }

        public static Schedule every(long everyMs) {
            return new Schedule(KIND_EVERY, null, everyMs, null, null);
        }

        public static Schedule cron(String expression, String timezone) {
            return new Schedule(KIND_CRON, null, null, expression, timezone);
        }

        public String getKind() {
            return kind;
        }

        public Long getAtMs() {
            return atMs;
        }

        public Long getEveryMs() {
            return everyMs;
        }

        public String getExpression() {
            return expression;
        }

        public String getTimezone() {
            return timezone;
        }
    }
}
