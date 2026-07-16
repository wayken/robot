---
name: schedule
description: Schedule reminders and recurring tasks.
---

# schedule

Use the `schedule` tool to schedule reminders or recurring tasks.

## Three Modes

1. **Reminder** - message is sent directly to user
2. **Task** - message is a task description, agent executes and sends result
3. **One-time** - runs once at a specific time, then auto-deletes

## Examples

Fixed reminder:
```
schedule(action="add", message="Time to take a break!", every_seconds=1200)
```

Dynamic task (agent executes each time):
```
schedule(action="add", message="Check GitHub stars and report", every_seconds=600)
```

One-time scheduled task (compute ISO datetime from current time):
```
schedule(action="add", message="Remind me about the meeting", at="<ISO datetime>")
```

Timezone-aware schedule:
```
schedule(action="add", message="Morning standup", schedule_expr="0 9 * * 1-5", tz="America/Vancouver")
```

List/remove:
```
schedule(action="list")
schedule(action="remove", job_id="abc123")
```

## Time Expressions

| User says | Parameters |
|-----------|------------|
| every 20 minutes | every_seconds: 1200 |
| every hour | every_seconds: 3600 |
| every day at 8am | schedule_expr: "0 8 * * *" |
| weekdays at 5pm | schedule_expr: "0 17 * * 1-5" |
| 9am Vancouver time daily | schedule_expr: "0 9 * * *", tz: "America/Vancouver" |
| at a specific time | at: ISO datetime string (compute from current time) |

## Timezone

Use `tz` with `schedule_expr` to schedule in a specific IANA timezone. Without `tz`, the server's local timezone is used.
