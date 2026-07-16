package cloud.apposs.robot.harness.tool.session;

import cloud.apposs.react.React;
import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.robot.harness.tool.ITool;
import cloud.apposs.robot.harness.util.Strings;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SessionSearchTool implements ITool {
    // FTS5 原始匹配条数上限，用于从中去重出足够多的唯一会话
    private static final int FTS_RAW_LIMIT = 50;

    public static final String NAME = "session_search";

    private final HarnessWorker worker;

    public SessionSearchTool(HarnessWorker worker) {
        this.worker = worker;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Search your long-term memory of past conversations, or browse recent sessions. This is your recall -- " +
                "every past session is searchable, and this tool summarizes what happened.\n\n" +
                "TWO MODES:\n" +
                "1. Recent sessions (no query): Call with no arguments to see what was worked on recently. " +
                "Returns titles, previews, and timestamps. Zero LLM cost, instant. " +
                "Start here when the user asks what were we working on or what did we do recently.\n" +
                "2. Keyword search (with query): Search for specific topics across all past sessions. " +
                "Returns LLM-generated summaries of matching sessions.\n\n" +
                "USE THIS PROACTIVELY when:\n" +
                "- The user says 'we did this before', 'remember when', 'last time', 'as I mentioned'\n" +
                "- The user asks about a topic you worked on before but don't have in current context\n" +
                "- The user references a project, person, or concept that seems familiar but isn't in memory\n" +
                "- You want to check if you've solved a similar problem before\n" +
                "- The user asks 'what did we do about X?' or 'how did we fix Y?'\n\n" +
                "Don't hesitate to search when it is actually cross-session -- it's fast and cheap. " +
                "Better to search and confirm than to guess or ask the user to repeat themselves.\n\n" +
                "Search syntax: keywords joined with OR for broad recall (elevenlabs OR baseten OR funding), " +
                "phrases for exact match (\"docker networking\"), boolean (python NOT java), prefix (deploy*). " +
                "IMPORTANT: Use OR between keywords for best results — FTS5 defaults to AND which misses " +
                "sessions that only mention some terms. If a broad OR query returns nothing, try individual " +
                "keyword searches in parallel. Returns summaries of the top matching sessions.";
    }

    @Override
    public Param parameters() {
        String schema = "{" +
                "  \"type\": \"object\"," +
                "  \"properties\": {" +
                "    \"query\": {" +
                "      \"type\": \"string\"," +
                "      \"description\": \"Search query — keywords, phrases, or boolean expressions to find in past sessions (returns titles, previews, timestamps with no LLM cost).\"" +
                "    }," +
                "    \"roleFilter\": {" +
                "      \"type\": \"string\"," +
                "      \"description\": \"Optional: only search messages from specific roles (comma-separated). E.g. 'user,assistant' to skip tool outputs.\"" +
                "    }," +
                "    \"limit\": {" +
                "      \"type\": \"integer\"," +
                "      \"description\": \"Max sessions to summarize (default: 3, max: 5).\"," +
                "      \"default\": 3" +
                "    }" +
                "  }," +
                "  \"required\": [\"query\"]" +
                "}";
        return JsonUtil.parseJsonParam(schema);
    }

    @Override
    public React<String> run(String wid, String sid, String rid, Param parameter, IMessageHook messageHook) throws Exception {
        if (parameter == null) {
            return React.just("Error: parameters must not be null.");
        }
        String query = parameter.getString("query");
        if (Strings.isBlank(query)) {
            return React.just("Error: query parameter is required and must not be empty.");
        }
        String roleFilter = parameter.getString("roleFilter");
        Integer limit = parameter.getInt("limit", 3);
        Table<Param> result = worker.getMind().searchSession(query, roleFilter, limit);
        if (result == null || result.isEmpty()) {
            return React.just("No matching sessions found for query: " + query);
        }
        // 关键词搜索 → FTS5 匹配后按会话去重，返回摘要
        // 取足够多的原始匹配行，以便从中凑出 limit 个唯一会话
        Table<Param> rawMatches = worker.getMind().searchSession(query, roleFilter, FTS_RAW_LIMIT);
        if (rawMatches == null || rawMatches.isEmpty()) {
            Param response = Param.builder("success", true)
                    .setString("query", query)
                    .setTable("results", Table.builder())
                    .setInt("count", 0)
                    .setString("message", "No matching sessions found.");
            return React.just(JsonUtil.toJson(response));
        }
        // 按 sid 去重，保留每个会话的所有匹配片段（有序，先出现的优先）
        Map<String, List<Param>> sessionMatches = new LinkedHashMap<>();
        for (Param match : rawMatches) {
            String matchSid = match.getString("sid");
            if (matchSid == null) {
                continue;
            }
            // 跳过当前会话自身（agent 已有当前上下文）
            if (matchSid.equals(wid)) {
                continue;
            }
            sessionMatches.computeIfAbsent(matchSid, k -> new ArrayList<>()).add(match);
            if (sessionMatches.size() >= limit) {
                break;
            }
        }
        // 构建每个会话的结果条目
        Table<Param> results = Table.builder();
        for (Map.Entry<String, List<Param>> entry : sessionMatches.entrySet()) {
            String matchSid = entry.getKey();
            List<Param> matches = entry.getValue();
            // 收集该会话下所有匹配片段
            List<Map<String, Object>> snippets = new ArrayList<>();
            for (Param match : matches) {
                Map<String, Object> snippetEntry = new LinkedHashMap<>();
                snippetEntry.put("role", match.getString("role"));
                snippetEntry.put("snippet", match.getString("snippet"));
                snippetEntry.put("timestamp", match.getString("timestamp"));
                // 附加上下文消息（前后各 1 条，由 searchSession 已填充）
                Object ctx = match.get("context");
                if (ctx != null) {
                    snippetEntry.put("context", ctx);
                }
                snippets.add(snippetEntry);
            }
            Param sessionEntry = Param.builder("session_id", matchSid)
                    .setInt("match_count", matches.size())
                    .setList("snippets", snippets);
            results.add(sessionEntry);
        }
        Param response = Param.builder("success", true)
                .setString("query", query)
                .setTable("results", results)
                .setInt("count", results.size())
                .setInt("sessions_searched", sessionMatches.size());
        return React.just(JsonUtil.toJson(response));
    }
}
