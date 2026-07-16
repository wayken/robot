package cloud.apposs.robot.harness.tool.schedule;

import cloud.apposs.react.React;
import cloud.apposs.robot.harness.HarnessSchedule;
import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.robot.harness.schedule.ScheduleJob;
import cloud.apposs.robot.harness.tool.ITool;
import cloud.apposs.robot.harness.util.Strings;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * 定时任务工具，提供添加、列出和删除定时任务的功能，供智能体在推理过程中调用，规则如下：
 * <pre>
 *     1. 当用户告诉智能体“提醒我明天上午10点开会”，智能体会调用 ScheduleTool 的 add 方法，添加一个定时任务，时间为明天上午10点，内容为“开会”。
 *     2. 当用户询问智能体“我有哪些定时任务”，智能体会调用 ScheduleTool 的 list 方法，列出所有定时任务的时间和内容。
 *     3. 当用户告诉智能体“取消明天上午10点的会议”，智能体会调用 ScheduleTool 的 remove 方法，删除对应时间的定时任务。
 *     4. 定时任务保存在：${workspace}/${worker_id}/schedules/${session_id}.json 文件中，智能体重启后会自动加载之前的定时任务继续执行
 * </pre>
 */
public class ScheduleTool implements ITool {
    public static final String NAME = "schedule";

    private final HarnessSchedule harnessSchedule;

    public ScheduleTool(HarnessSchedule harnessSchedule) {
        this.harnessSchedule = harnessSchedule;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Schedule reminders and recurring tasks. Actions: add, list, remove.";
    }

    @Override
    public Param parameters() {
        String schema = "{" +
                "  \"type\": \"object\"," +
                "  \"properties\": {" +
                "    \"action\":{" +
                "      \"type\":\"string\"," +
                "      \"enum\":[\"add\",\"list\",\"remove\"]," +
                "      \"description\":\"Action to perform\"" +
                "    }," +
                "    \"message\":{" +
                "      \"type\":\"string\"," +
                "      \"description\":\"Reminder message (for add)\"" +
                "    }," +
                "    \"every_seconds\":{" +
                "      \"type\":\"integer\"," +
                "      \"description\":\"Interval in seconds (for recurring tasks)\"" +
                "    }," +
                "    \"schedule_expression\":{" +
                "      \"type\":\"string\"," +
                "      \"description\":\"Schedule expression like '0 9 * * *' (for scheduled tasks)\"" +
                "    }," +
                "    \"timezone\":{" +
                "      \"type\":\"string\"," +
                "      \"description\":\"IANA timezone for schedule expressions (e.g. 'America/Vancouver')\"" +
                "    }," +
                "    \"at\":{" +
                "      \"type\":\"string\"," +
                "      \"description\":\"ISO datetime for one-time execution (e.g. '2026-02-12T10:30:00')\"" +
                "    }," +
                "    \"schedule_id\":{" +
                "      \"type\":\"string\"," +
                "      \"description\":\"Schedule ID (for remove)\"" +
                "    }" +
                "  }," +
                "  \"required\": [\"action\"]" +
                "}";
        return JsonUtil.parseJsonParam(schema);
    }

    @Override
    public React<String> run(String wid, String sid, String rid, Param parameter, IMessageHook messageHook) {
        if (parameter == null) {
            return React.just("Error: parameters must not be null.");
        }
        String action = parameter.getString("action");
        if (Strings.isBlank(action)) {
            return React.just("Error: action is required.");
        }
        switch (action.trim()) {
            case "add":
                String message = parameter.getString("message");
                Integer everySeconds = parameter.getInt("every_seconds");
                String scheduleExpression = parameter.getString("schedule_expression");
                String timezone = parameter.getString("timezone");
                String at = parameter.getString("at");
                return React.just(handleScheduleAdd(sid, message, everySeconds, scheduleExpression, timezone, at));
            case "list":
                return React.just(handleScheduleList(sid));
            case "remove":
                String scheduleId = parameter.getString("schedule_id");
                return React.just(handleScheduleRemove(sid, scheduleId));
            default:
                return React.just("Error: unsupported action '" + action + "'. Supported actions are: add, list, remove.");
        }
    }

    /**
     * 添加一个新的定时任务。可以是一次性的，也可以是周期性的
     *
     * @param  message      定时任务的提醒消息
     * @param  everySeconds 如果是周期性任务，每隔多少秒执行一次
     * @param  scheduleExpression 如果是基于cron表达式的任务，cron表达式是什么
     * @param  timezone     如果提供了cron表达式，时区是什么（默认为UTC）
     * @param  at           如果是一次性任务，具体的执行时间是什么（ISO格式）
     * @return 返回新创建的定时任务的ID，或者错误信息
     */
    private String handleScheduleAdd(String sid, String message, Integer everySeconds, String scheduleExpression, String timezone, String at) {
        if (Strings.isBlank(message)) {
            return "Error: message must not be empty for add action.";
        }
        String id = UUID.randomUUID().toString();
        ScheduleJob.Schedule schedule;
        boolean deleteAfterRun = false;
        if (!Strings.isBlank(scheduleExpression)) {
            schedule = ScheduleJob.Schedule.cron(scheduleExpression.trim(), timezone);
        } else if (everySeconds != null && everySeconds > 0) {
            schedule = ScheduleJob.Schedule.every(everySeconds * 1000L);
        } else if (!Strings.isBlank(at)) {
            try {
                ZoneId zoneId = (timezone != null && !timezone.isEmpty()) ? ZoneId.of(timezone) : ZoneId.systemDefault();
                LocalDateTime ldt = LocalDateTime.parse(at.trim());
                long atMs = ldt.atZone(zoneId).toInstant().toEpochMilli();
                schedule = ScheduleJob.Schedule.at(atMs);
                deleteAfterRun = true;
            } catch (Exception e) {
                return "Error: invalid 'at' datetime format, expected ISO format like '2026-02-12T10:30:00'. " + e.getMessage();
            }
        } else {
            return "Error: one of 'schedule_expression', 'every_seconds', or 'at' must be provided.";
        }
        ScheduleJob scheduleJob = new ScheduleJob(id, message, schedule, deleteAfterRun);
        try {
            harnessSchedule.addSchedule(sid, scheduleJob);
        } catch (Exception e) {
            return "Error: failed to add schedule: " + e.getMessage();
        }
        return "Schedule added with id: " + id;
    }

    private String handleScheduleList(String sessionId) {
        Table<ScheduleJob> scheduleList = harnessSchedule.getScheduleList(sessionId);
        if (scheduleList.isEmpty()) {
            return "No schedules found.";
        }
        StringBuilder sb = new StringBuilder("Scheduled jobs:\n");
        for (ScheduleJob schedule : scheduleList) {
            sb.append("- ").append(schedule.getMessage())
                    .append(" (id: ").append(schedule.getId())
                    .append(", ").append(schedule.getSchedule().getKind()).append(")\n");
        }
        return sb.toString().trim();
    }

    private String handleScheduleRemove(String sessionId, String scheduleId) {
        if (Strings.isBlank(scheduleId)) {
            return "Error: schedule_id is required for remove action.";
        }
        try {
            boolean success = harnessSchedule.removeSchedule(sessionId, scheduleId);
            if (!success) {
                return "No schedule found with id '" + scheduleId + "'.";
            }
            return "Schedule with id '" + scheduleId + "' removed.";
        } catch (Exception e) {
            return "Error: failed to remove schedule with id '" + scheduleId + "': " + e.getMessage();
        }
    }
}
