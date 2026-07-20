package cloud.apposs.robot.harness.mind.filesystem;

import cloud.apposs.robot.harness.HarnessWorkspace;
import cloud.apposs.robot.harness.message.AIMessage;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SessionDatabaseLoader {
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String DB_DRIVER = "org.sqlite.JDBC";
    private static final String DB_FILE_NAME = "sessions.db";

    private static final String TABLE_NAME_SESSIONS = "sessions";
    private static final String TABLE_NAME_MESSAGES = "messages";
    private static final String TABLE_NAME_MESSAGES_FTS = "messages_fts";
    private static final String TABLE_NAME_MESSAGES_FTS_TRIGRAM = "messages_fts_trigram";
    private static final String TABLE_NAME_MISSIONS = "missions";

    private final String dbUrl;

    private final Path sessionPath;

    public SessionDatabaseLoader(HarnessWorkspace workspace) throws Exception {
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException cnfe) {
            Thread.currentThread().getContextClassLoader().loadClass(DB_DRIVER);
        }
        this.sessionPath = workspace.root().resolve("sessions");
        if (!Files.exists(sessionPath)) {
            sessionPath.toFile().mkdirs();
        }
        this.dbUrl = "jdbc:sqlite:" + sessionPath.resolve(DB_FILE_NAME).toString();
        try (Connection conn = getConnection()) {
            handleCreateTableIfNotExists(conn);
            handleCreateFtsTablesIfNotExists(conn);
        }
    }

    private Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA busy_timeout=5000");
        }
        return conn;
    }

    public Table<Param> getSessionList(String wid) throws Exception {
        Table<Param> sessions = Table.builder();
        String querySql = "SELECT s.id, s.name, s.created, s.updated, " +
                "s.mission_id, " +
                "p.name AS mission_name, " +
                "COALESCE(p.sort_order, 0) AS mission_sort_order, " +
                "COALESCE(mc.cnt, 0) AS message_count " +
                "FROM " + TABLE_NAME_SESSIONS + " s " +
                "LEFT JOIN (SELECT sid, COUNT(*) AS cnt FROM " + TABLE_NAME_MESSAGES + " GROUP BY sid) mc " +
                "ON mc.sid = CAST(s.id AS TEXT) " +
                "LEFT JOIN " + TABLE_NAME_MISSIONS + " p ON p.id = s.mission_id " +
                "ORDER BY s.updated DESC";
        try (Connection conn = getConnection();
             Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(querySql)) {
            while (resultSet.next()) {
                Param param = new Param();
                param.put("id", resultSet.getInt("id"));
                param.put("name", resultSet.getString("name"));
                String date = resultSet.getString("updated");
                long timestamp = LocalDateTime.parse(date, dateTimeFormatter).toInstant(ZoneOffset.UTC).toEpochMilli();
                param.put("date", timestamp);
                param.put("size", resultSet.getInt("message_count"));
                param.put("missionId", resultSet.getInt("mission_id"));
                param.put("missionName", resultSet.getString("mission_name"));
                param.put("missionSortOrder", resultSet.getInt("mission_sort_order"));
                sessions.add(param);
            }
        }
        return sessions;
    }

    public Param getSessionInfo(String wid, String sid) throws Exception {
        String querySql = "SELECT id, name, consolidated, created, updated FROM " + TABLE_NAME_SESSIONS + " WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(querySql)) {
            pstmt.setInt(1, Integer.parseInt(sid));
            try (ResultSet resultSet = pstmt.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                Param infomation = new Param();
                infomation.put("id", resultSet.getInt("id"));
                infomation.put("name", resultSet.getString("name"));
                infomation.put("consolidate", resultSet.getInt("consolidated"));
                String date = resultSet.getString("updated");
                long timestamp = LocalDateTime.parse(date, dateTimeFormatter).toInstant(ZoneOffset.UTC).toEpochMilli();
                infomation.put("date", timestamp);
                return infomation;
            }
        }
    }

    // 创建会话,返回会话ID
    public String addSession(String wid, String name) throws Exception {
        String sql = "INSERT INTO " + TABLE_NAME_SESSIONS + " (name, created, updated) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            String now = LocalDateTime.now().format(dateTimeFormatter);
            pstmt.setString(1, name);
            pstmt.setString(2, now);
            pstmt.setString(3, now);
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return String.valueOf(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating session failed, no ID obtained.");
                }
            }
        }
    }

    public boolean isSessionExists(String wid, String sid) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME_SESSIONS + " WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(sid));
            try (ResultSet resultSet = pstmt.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public Table<Param> getSessionMessages(String wid, String sid) throws Exception {
        Table<Param> infomation = Table.builder();
        String sql = "SELECT id, sid, rid, message FROM " + TABLE_NAME_MESSAGES + " WHERE sid = ? ORDER BY id ASC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(sid));
            try (ResultSet resultSet = pstmt.executeQuery()) {
                while (resultSet.next()) {
                    String messageJson = resultSet.getString("message");
                    Param data = Param.builder("id", resultSet.getInt("id"))
                            .setString("sid", resultSet.getString("sid"))
                            .setString("rid", resultSet.getString("rid"))
                            .setParam("message", JsonUtil.parseJsonParam(messageJson));
                    infomation.add(data);
                }
            }
        }
        return infomation;
    }

    public void addSessionMessage(String wid, String sid, String rid, Table<AIMessage> messages) throws Exception {
        String insertSql = "INSERT INTO " + TABLE_NAME_MESSAGES + " (sid, rid, message, created) VALUES (?, ?, ?, ?)";
        String updateSql = "UPDATE " + TABLE_NAME_SESSIONS + " SET updated = ? WHERE id = ?";
        String now = LocalDateTime.now().format(dateTimeFormatter);
        try (Connection conn = getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (AIMessage message : messages) {
                    pstmt.setString(1, sid);
                    pstmt.setString(2, rid);
                    pstmt.setString(3, JsonUtil.toJson(message.deserialize()));
                    pstmt.setString(4, now);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setString(1, now);
                pstmt.setInt(2, Integer.parseInt(sid));
                pstmt.executeUpdate();
            }
        }
    }

    public void updateConsolidated(String wid, String sid, int consolidated) throws Exception {
        String sql = "UPDATE " + TABLE_NAME_SESSIONS + " SET consolidated = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, consolidated);
            pstmt.setInt(2, Integer.parseInt(sid));
            pstmt.executeUpdate();
        }
    }

    public boolean removeSession(String wid, String sid) throws Exception {
        try (Connection conn = getConnection()) {
            String deleteMessagesSql = "DELETE FROM " + TABLE_NAME_MESSAGES + " WHERE sid = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteMessagesSql)) {
                pstmt.setInt(1, Integer.parseInt(sid));
                pstmt.executeUpdate();
            }
            String deleteSessionSql = "DELETE FROM " + TABLE_NAME_SESSIONS + " WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteSessionSql)) {
                pstmt.setInt(1, Integer.parseInt(sid));
                int affected = pstmt.executeUpdate();
                return affected > 0;
            }
        }
    }

    public boolean renameSession(String wid, String sid, String name) throws Exception {
        String sql = "UPDATE " + TABLE_NAME_SESSIONS + " SET name = ?, updated = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String now = LocalDateTime.now().format(dateTimeFormatter);
            pstmt.setString(1, name);
            pstmt.setString(2, now);
            pstmt.setInt(3, Integer.parseInt(sid));
            int affected = pstmt.executeUpdate();
            return affected > 0;
        }
    }

    /**
     * 全文搜索会话消息，搜索策略如下：
     * <pre>
     *   1. 若查询语句包含 CJK 字符且 CJK 字符数 ≥ 3，使用 {@code messages_fts_trigram}进行分词子串匹配，支持中文/日文/韩文精确短语检索
     *   2. 若查询语句包含 CJK 字符但 CJK 字符数 &lt; 3（短 CJK），使用 {@code LIKE} 子串匹配
     *   3. 其余情况使用 {@code messages_fts} 进行标准 FTS5 全文检索，支持 FTS5 查询语法（短语、布尔、前缀等）
     * </pre>
     * <p>每条匹配结果附带前后各 1 条上下文消息（context），并去除完整 content 字段，仅保留 snippet 以节省 token。
     *
     * @param query      搜索关键词，支持 FTS5 语法（非 CJK 时）
     * @param roleFilter 角色过滤，逗号分隔（如 {@code "user,assistant"}），传 {@code null} 不过滤
     * @param limit      最多返回条数
     * @return JSON 字符串，格式为 {@code [{id, sid, role, snippet, timestamp, context:[...]}, ...]}
     */
    public Table<Param> searchSession(String query, String roleFilter, int limit) throws Exception {
        if (query == null || query.trim().isEmpty()) {
            return Table.builder();
        }
        String sanitized = handleFts5QuerySanitize(query.trim());
        if (sanitized.isEmpty()) {
            return Table.builder();
        }
        String[] filterRoles = null;
        if (roleFilter != null && !roleFilter.trim().isEmpty()) {
            filterRoles = roleFilter.trim().split("\\s*,\\s*");
        }
        try (Connection conn = getConnection()) {
            Table<Param> matches;
            boolean hasCjk = isCjkContains(sanitized);
            if (hasCjk) {
                String raw = sanitized.replaceAll("^\"|\"$", "").trim();
                int cjkCount = handlCountCjk(raw);
                boolean anyShortCjk = false;
                for (String tok : raw.split("\\s+")) {
                    if (!tok.equalsIgnoreCase("AND") && !tok.equalsIgnoreCase("OR")
                            && !tok.equalsIgnoreCase("NOT") && isCjkContains(tok)
                            && handlCountCjk(tok) < 3) {
                        anyShortCjk = true;
                        break;
                    }
                }
                if (cjkCount >= 3 && !anyShortCjk) {
                    matches = handleSearchWithTrigram(conn, raw, filterRoles, limit);
                } else {
                    matches = handleSearchWithLike(conn, raw, filterRoles, limit);
                }
            } else {
                matches = handleSearchWithFts(conn, sanitized, filterRoles, limit);
            }
            for (Map<String, Object> match : matches) {
                match.put("context", handleContextFetch(conn, (int) match.get("id")));
                match.remove("content");
            }
            return matches;
        }
    }

    public boolean removeSessionMessage(String wid, String sid, String rid, String id) throws Exception {
        try (Connection conn = getConnection()) {
            int rowPosition = -1;
            String rankQuery = "SELECT COUNT(*) FROM " + TABLE_NAME_MESSAGES + " WHERE sid = ? AND id < ?";
            try (PreparedStatement pstmt = conn.prepareStatement(rankQuery)) {
                pstmt.setInt(1, Integer.parseInt(sid));
                pstmt.setInt(2, Integer.parseInt(id));
                try (ResultSet resultSet = pstmt.executeQuery()) {
                    if (resultSet.next()) {
                        rowPosition = resultSet.getInt(1);
                    }
                }
            }
            String deleteQuery = "DELETE FROM " + TABLE_NAME_MESSAGES + " WHERE id = ? AND sid = ? AND rid = ?";
            boolean rowDeleted = false;
            try (PreparedStatement pstmt = conn.prepareStatement(deleteQuery)) {
                pstmt.setInt(1, Integer.parseInt(id));
                pstmt.setInt(2, Integer.parseInt(sid));
                pstmt.setString(3, rid);
                rowDeleted = pstmt.executeUpdate() > 0;
            }
            if (rowDeleted && rowPosition >= 0) {
                handleSessionMessageConsolidateUpdateAfterDelete(conn, sid, rowPosition);
            }
            return rowDeleted;
        }
    }

    public boolean truncateSessionMessages(String wid, String sid, String id) throws Exception {
        try (Connection conn = getConnection()) {
            int rowPosition = -1;
            String rankQuery = "SELECT COUNT(*) FROM " + TABLE_NAME_MESSAGES + " WHERE sid = ? AND id < ?";
            try (PreparedStatement pstmt = conn.prepareStatement(rankQuery)) {
                pstmt.setInt(1, Integer.parseInt(sid));
                pstmt.setInt(2, Integer.parseInt(id));
                try (ResultSet resultSet = pstmt.executeQuery()) {
                    if (resultSet.next()) {
                        rowPosition = resultSet.getInt(1);
                    }
                }
            }
            String deleteQuery = "DELETE FROM " + TABLE_NAME_MESSAGES + " WHERE sid = ? AND id >= ?";
            boolean rowDeleted;
            try (PreparedStatement pstmt = conn.prepareStatement(deleteQuery)) {
                pstmt.setInt(1, Integer.parseInt(sid));
                pstmt.setInt(2, Integer.parseInt(id));
                rowDeleted = pstmt.executeUpdate() > 0;
            }
            if (rowDeleted) {
                if (rowPosition >= 0) {
                    String consolidateSql = "UPDATE " + TABLE_NAME_SESSIONS + " SET consolidated = min(consolidated, ?), updated = ? WHERE id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(consolidateSql)) {
                        pstmt.setInt(1, rowPosition);
                        pstmt.setString(2, LocalDateTime.now().format(dateTimeFormatter));
                        pstmt.setInt(3, Integer.parseInt(sid));
                        pstmt.executeUpdate();
                    }
                }
            }
            return rowDeleted;
        }
    }

    public void clearSessionMessages(String wid, String sid) throws Exception {
        String sql = "DELETE FROM " + TABLE_NAME_MESSAGES + " WHERE sid = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(sid));
            pstmt.executeUpdate();
        }
    }

    public Table<Param> getMissionList(String wid) throws Exception {
        Table<Param> missions = Table.builder();
        String sql = "SELECT id, name, description, sort_order, created, updated FROM " + TABLE_NAME_MISSIONS + " ORDER BY sort_order ASC, updated DESC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet resultSet = stmt.executeQuery(sql)) {
            while (resultSet.next()) {
                Param param = new Param();
                param.put("id", resultSet.getInt("id"));
                param.put("name", resultSet.getString("name"));
                param.put("description", resultSet.getString("description"));
                param.put("sortOrder", resultSet.getInt("sort_order"));
                String date = resultSet.getString("updated");
                long timestamp = LocalDateTime.parse(date, dateTimeFormatter).toInstant(ZoneOffset.UTC).toEpochMilli();
                param.put("date", timestamp);
                missions.add(param);
            }
        }
        return missions;
    }

    public String addMission(String wid, String name, String description) throws Exception {
        String sql = "INSERT INTO " + TABLE_NAME_MISSIONS + " (name, description, created, updated) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            String now = LocalDateTime.now().format(dateTimeFormatter);
            pstmt.setString(1, name);
            pstmt.setString(2, description != null ? description : "");
            pstmt.setString(3, now);
            pstmt.setString(4, now);
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return String.valueOf(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating mission failed, no ID obtained.");
                }
            }
        }
    }

    public boolean removeMission(String wid, String missionId) throws Exception {
        try (Connection conn = getConnection()) {
            // 将引用该项目的会话重置为默认项目
            String resetSql = "UPDATE " + TABLE_NAME_SESSIONS + " SET mission_id = 0 WHERE mission_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(resetSql)) {
                pstmt.setInt(1, Integer.parseInt(missionId));
                pstmt.executeUpdate();
            }
            // 删除项目
            String deleteSql = "DELETE FROM " + TABLE_NAME_MISSIONS + " WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                pstmt.setInt(1, Integer.parseInt(missionId));
                int affected = pstmt.executeUpdate();
                return affected > 0;
            }
        }
    }

    public boolean renameMission(String wid, String missionId, String name) throws Exception {
        String sql = "UPDATE " + TABLE_NAME_MISSIONS + " SET name = ?, updated = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String now = LocalDateTime.now().format(dateTimeFormatter);
            pstmt.setString(1, name);
            pstmt.setString(2, now);
            pstmt.setInt(3, Integer.parseInt(missionId));
            int affected = pstmt.executeUpdate();
            return affected > 0;
        }
    }

    public boolean updateMissionSortOrder(String wid, int[] missionIds) throws Exception {
        String sql = "UPDATE " + TABLE_NAME_MISSIONS + " SET sort_order = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < missionIds.length; i++) {
                pstmt.setInt(1, i);
                pstmt.setInt(2, missionIds[i]);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            return true;
        }
    }

    public boolean updateSessionMission(String wid, String sid, int missionId) throws Exception {
        String sql = "UPDATE " + TABLE_NAME_SESSIONS + " SET mission_id = ?, updated = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String now = LocalDateTime.now().format(dateTimeFormatter);
            pstmt.setInt(1, missionId);
            pstmt.setString(2, now);
            pstmt.setInt(3, Integer.parseInt(sid));
            int affected = pstmt.executeUpdate();
            return affected > 0;
        }
    }

    public Param getSessionMission(String wid, String sid) throws Exception {
        String sql = "SELECT p.id, p.name, p.description, p.created, p.updated " +
                "FROM " + TABLE_NAME_SESSIONS + " s " +
                "LEFT JOIN " + TABLE_NAME_MISSIONS + " p ON p.id = s.mission_id " +
                "WHERE s.id = ? AND s.mission_id > 0";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(sid));
            try (ResultSet resultSet = pstmt.executeQuery()) {
                if (resultSet.next()) {
                    Param param = new Param();
                    param.put("id", resultSet.getInt("id"));
                    param.put("name", resultSet.getString("name"));
                    param.put("description", resultSet.getString("description"));
                    String date = resultSet.getString("updated");
                    if (date != null) {
                        long timestamp = LocalDateTime.parse(date, dateTimeFormatter).toInstant(ZoneOffset.UTC).toEpochMilli();
                        param.put("date", timestamp);
                    }
                    return param;
                }
                return null;
            }
        }
    }

    /**
     * Fork会话：创建一个新的会话，并将源会话中指定消息ID及之前的所有消息复制到新会话中
     *
     * @param  wid       智能体ID
     * @param  sid       源会话ID
     * @param  messageId 截止消息ID（含），复制该消息及之前的所有消息
     * @param  name      新会话名称
     * @return 新会话的ID，如果失败返回null
     */
    public String forkSession(String wid, String sid, String messageId, String name) throws Exception {
        try (Connection conn = getConnection()) {
            // 1. 创建新会话
            String now = LocalDateTime.now().format(dateTimeFormatter);
            String insertSessionSql = "INSERT INTO " + TABLE_NAME_SESSIONS + " (name, mission_id, created, updated) " +
                    "SELECT ?, mission_id, ?, ? FROM " + TABLE_NAME_SESSIONS + " WHERE id = ?";
            int newSid;
            try (PreparedStatement pstmt = conn.prepareStatement(insertSessionSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, name);
                pstmt.setString(2, now);
                pstmt.setString(3, now);
                pstmt.setInt(4, Integer.parseInt(sid));
                pstmt.executeUpdate();
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        newSid = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Fork session failed, no ID obtained.");
                    }
                }
            }
            // 2. 复制源会话中messageId及之前的所有消息到新会话
            String copyMessagesSql = "INSERT INTO " + TABLE_NAME_MESSAGES + " (sid, rid, message, created) " +
                    "SELECT ?, rid, message, created FROM " + TABLE_NAME_MESSAGES + " WHERE sid = ? AND id <= ? ORDER BY id ASC";
            try (PreparedStatement pstmt = conn.prepareStatement(copyMessagesSql)) {
                pstmt.setString(1, String.valueOf(newSid));
                pstmt.setInt(2, Integer.parseInt(sid));
                pstmt.setInt(3, Integer.parseInt(messageId));
                pstmt.executeUpdate();
            }
            return String.valueOf(newSid);
        }
    }

    public void close() {
    }

    private boolean isTableExists(Connection connection, String tableName) throws SQLException {
        ResultSet resultSet = connection.getMetaData().getTables(null, null, tableName, null);
        return resultSet.next();
    }

    private void handleCreateTableIfNotExists(Connection connection) throws SQLException {
        if (!isTableExists(connection, TABLE_NAME_SESSIONS)) {
            String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_SESSIONS + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "consolidated INTEGER NOT NULL DEFAULT 0," +
                    "mission_id INTEGER NOT NULL DEFAULT 0," +
                    "created TEXT NOT NULL," +
                    "updated TEXT NOT NULL" +
                    ")";
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(sql);
            }
        }
        if (!isTableExists(connection, TABLE_NAME_MESSAGES)) {
            String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_MESSAGES + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "sid TEXT NOT NULL, " +
                    "rid TEXT NOT NULL, " +
                    "message TEXT NOT NULL, " +
                    "created TEXT NOT NULL" +
                    ")";
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(sql);
            }
        }
        if (!isTableExists(connection, TABLE_NAME_MISSIONS)) {
            String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_MISSIONS + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "description TEXT DEFAULT '', " +
                    "sort_order INTEGER DEFAULT 0," +
                    "created TEXT NOT NULL," +
                    "updated TEXT NOT NULL" +
                    ")";
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(sql);
            }
        }
    }

    /**
     * 创建 FTS5 全文搜索虚拟表及对应触发器（如果尚未存在），以支持消息内容的全文搜索功能
     * <pre>
     *   1. {@code messages_fts} — 标准 unicode61 分词，用于普通全文检索
     *   2. {@code messages_fts_trigram} — trigram 分词，支持 CJK / 任意子串检索
     * </pre>
     * 两张虚拟表均通过 INSERT / DELETE / UPDATE 触发器与 {@code messages} 表保持同步，索引内容为 {@code message}
     */
    private void handleCreateFtsTablesIfNotExists(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // 普通全文检索表，标准 unicode61 分词
            stmt.execute("CREATE VIRTUAL TABLE IF NOT EXISTS " + TABLE_NAME_MESSAGES_FTS + " USING fts5(content)");
            stmt.execute(
                    "CREATE TRIGGER IF NOT EXISTS messages_fts_insert" +
                            " AFTER INSERT ON " + TABLE_NAME_MESSAGES + " BEGIN" +
                            "  INSERT INTO " + TABLE_NAME_MESSAGES_FTS + "(rowid, content)" +
                            "  VALUES (new.id, COALESCE(new.message, ''));" +
                            " END"
            );
            stmt.execute(
                    "CREATE TRIGGER IF NOT EXISTS messages_fts_delete" +
                            " AFTER DELETE ON " + TABLE_NAME_MESSAGES + " BEGIN" +
                            "  DELETE FROM " + TABLE_NAME_MESSAGES_FTS + " WHERE rowid = old.id;" +
                            " END"
            );
            stmt.execute(
                    "CREATE TRIGGER IF NOT EXISTS messages_fts_update" +
                            " AFTER UPDATE ON " + TABLE_NAME_MESSAGES + " BEGIN" +
                            "  DELETE FROM " + TABLE_NAME_MESSAGES_FTS + " WHERE rowid = old.id;" +
                            "  INSERT INTO " + TABLE_NAME_MESSAGES_FTS + "(rowid, content)" +
                            "  VALUES (new.id, COALESCE(new.message, ''));" +
                            " END"
            );
            // 任意子串检索表，支持 CJK / 子串搜索
            stmt.execute("CREATE VIRTUAL TABLE IF NOT EXISTS " + TABLE_NAME_MESSAGES_FTS_TRIGRAM + " USING fts5(content, tokenize='trigram')");
            stmt.execute(
                    "CREATE TRIGGER IF NOT EXISTS messages_fts_trigram_insert" +
                            " AFTER INSERT ON " + TABLE_NAME_MESSAGES + " BEGIN" +
                            "  INSERT INTO " + TABLE_NAME_MESSAGES_FTS_TRIGRAM + "(rowid, content)" +
                            "  VALUES (new.id, COALESCE(new.message, ''));" +
                            " END"
            );
            stmt.execute(
                    "CREATE TRIGGER IF NOT EXISTS messages_fts_trigram_delete" +
                            " AFTER DELETE ON " + TABLE_NAME_MESSAGES + " BEGIN" +
                            "  DELETE FROM " + TABLE_NAME_MESSAGES_FTS_TRIGRAM + " WHERE rowid = old.id;" +
                            " END"
            );
            stmt.execute(
                    "CREATE TRIGGER IF NOT EXISTS messages_fts_trigram_update" +
                            " AFTER UPDATE ON " + TABLE_NAME_MESSAGES + " BEGIN" +
                            "  DELETE FROM " + TABLE_NAME_MESSAGES_FTS_TRIGRAM + " WHERE rowid = old.id;" +
                            "  INSERT INTO " + TABLE_NAME_MESSAGES_FTS_TRIGRAM + "(rowid, content)" +
                            "  VALUES (new.id, COALESCE(new.message, ''));" +
                            " END"
            );
        }
    }

    private void handleSessionMessageConsolidateUpdateAfterDelete(Connection connection, String sid, int position) throws Exception {
        String query = "SELECT consolidated FROM " + TABLE_NAME_SESSIONS + " WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, Integer.parseInt(sid));
            try (ResultSet resultSet = pstmt.executeQuery()) {
                if (!resultSet.next()) {
                    return;
                }
                int consolidated = resultSet.getInt(1);
                if (position < consolidated) {
                    String updateSql = "UPDATE " + TABLE_NAME_SESSIONS + " SET consolidated = consolidated - 1 WHERE id = ?";
                    try (PreparedStatement updatePstmt = connection.prepareStatement(updateSql)) {
                        updatePstmt.setInt(1, Integer.parseInt(sid));
                        updatePstmt.executeUpdate();
                    }
                }
            }
        }
    }

    // 统计字符串中 CJK 字符的数量
    private int handlCountCjk(String text) {
        if (text == null) return 0;
        int count = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if (isCjkCodepoint(cp)) count++;
            i += Character.charCount(cp);
        }
        return count;
    }

    // 判断 Unicode 码点是否属于 CJK 范围
    private boolean isCjkCodepoint(int cp) {
        return (cp >= 0x4E00 && cp <= 0x9FFF)    // CJK 统一表意文字
                || (cp >= 0x3400 && cp <= 0x4DBF)    // CJK 扩展 A
                || (cp >= 0x20000 && cp <= 0x2A6DF)  // CJK 扩展 B
                || (cp >= 0x3000 && cp <= 0x303F)    // CJK 符号和标点
                || (cp >= 0x3040 && cp <= 0x309F)    // 平假名
                || (cp >= 0x30A0 && cp <= 0x30FF)    // 片假名
                || (cp >= 0xAC00 && cp <= 0xD7AF);   // 韩文音节
    }

    private Table<Param> handleExecuteSearchQuery(Connection connection, String sql, List<Object> params) throws SQLException {
        Table<Param> results = Table.builder();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                Object val = params.get(i);
                if (val instanceof Integer) {
                    pstmt.setInt(i + 1, (Integer) val);
                } else {
                    pstmt.setString(i + 1, String.valueOf(val));
                }
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                while (rs.next()) {
                    Param row = Param.builder();
                    for (int i = 1; i <= colCount; i++) {
                        row.put(meta.getColumnLabel(i), rs.getObject(i));
                    }
                    results.add(row);
                }
            }
        }
        return results;
    }

    // 清理 FTS5 查询字符串，去除可能导致语法错误的特殊字符，并合并多余空白
    private String handleFts5QuerySanitize(String query) {
        if (query == null) return "";
        // 去除 FTS5 不支持的特殊字符，保留字母、数字、空格、引号、*、-、CJK 字符
        String cleaned = query.replaceAll("[\\^~{}\\[\\]]", " ").trim();
        // 合并多余空白
        cleaned = cleaned.replaceAll("\\s{2,}", " ");
        return cleaned;
    }

    // 判断字符串是否包含 CJK 字符（中文/日文/韩文）
    private boolean isCjkContains(String text) {
        if (text == null) return false;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if (isCjkCodepoint(cp)) return true;
            i += Character.charCount(cp);
        }
        return false;
    }

    // 追加角色过滤条件到 SQL，同时向 params 添加对应占位符值
    // role 存储在 message JSON 中，使用 json_extract 提取
    private void handleRoleFilterAppend(StringBuilder sql, List<Object> params, String[] roles) {
        if (roles != null && roles.length > 0) {
            sql.append(" AND json_extract(m.message, '$.role') IN (");
            for (int i = 0; i < roles.length; i++) {
                if (i > 0) sql.append(',');
                sql.append('?');
                params.add(roles[i].trim());
            }
            sql.append(')');
        }
    }

    // 搜索路径：标准 FTS5（unicode61）
    private Table<Param> handleSearchWithFts(Connection conn, String query, String[] roles, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT m.id, m.sid, json_extract(m.message, '$.role') AS role," +
                        " snippet(" + TABLE_NAME_MESSAGES_FTS + ", 0, '>>>', '<<<', '...', 40) AS snippet," +
                        " m.created AS timestamp" +
                        " FROM " + TABLE_NAME_MESSAGES_FTS +
                        " JOIN " + TABLE_NAME_MESSAGES + " m ON m.id = " + TABLE_NAME_MESSAGES_FTS + ".rowid" +
                        " WHERE " + TABLE_NAME_MESSAGES_FTS + " MATCH ?"
        );
        List<Object> params = new ArrayList<>();
        params.add(query);
        handleRoleFilterAppend(sql, params, roles);
        sql.append(" ORDER BY rank LIMIT ?");
        params.add(limit);
        return handleExecuteSearchQuery(conn, sql.toString(), params);
    }

    // 搜索路径：Trigram FTS5（CJK 子串）
    private Table<Param> handleSearchWithTrigram(Connection conn, String rawQuery, String[] roles, int limit) throws SQLException {
        // 将每个非运算符 token 用双引号包裹，避免 FTS5 特殊字符干扰
        String[] tokens = rawQuery.split("\\s+");
        StringBuilder trigramQuery = new StringBuilder();
        for (String tok : tokens) {
            if (trigramQuery.length() > 0) trigramQuery.append(' ');
            if (tok.equalsIgnoreCase("AND") || tok.equalsIgnoreCase("OR")
                    || tok.equalsIgnoreCase("NOT")) {
                trigramQuery.append(tok);
            } else {
                trigramQuery.append('"').append(tok.replace("\"", "\"\"")).append('"');
            }
        }
        StringBuilder sql = new StringBuilder(
                "SELECT m.id, m.sid, json_extract(m.message, '$.role') AS role," +
                        " snippet(" + TABLE_NAME_MESSAGES_FTS_TRIGRAM + ", 0, '>>>', '<<<', '...', 40) AS snippet," +
                        " m.created AS timestamp" +
                        " FROM " + TABLE_NAME_MESSAGES_FTS_TRIGRAM +
                        " JOIN " + TABLE_NAME_MESSAGES + " m ON m.id = " + TABLE_NAME_MESSAGES_FTS_TRIGRAM + ".rowid" +
                        " WHERE " + TABLE_NAME_MESSAGES_FTS_TRIGRAM + " MATCH ?"
        );
        List<Object> params = new ArrayList<>();
        params.add(trigramQuery.toString());
        handleRoleFilterAppend(sql, params, roles);
        sql.append(" ORDER BY rank LIMIT ?");
        params.add(limit);
        return handleExecuteSearchQuery(conn, sql.toString(), params);
    }

    // 搜索路径 3：LIKE 子串（短 CJK，trigram 无法匹配）
    private Table<Param> handleSearchWithLike(Connection conn, String rawQuery, String[] roles, int limit) throws SQLException {
        // 多 token OR 查询：每个非运算符 token 独立生成一个 LIKE 条件
        List<String> nonOpTokens = new ArrayList<>();
        for (String tok : rawQuery.split("\\s+")) {
            if (!tok.equalsIgnoreCase("AND") && !tok.equalsIgnoreCase("OR")
                    && !tok.equalsIgnoreCase("NOT")) {
                nonOpTokens.add(tok);
            }
        }
        if (nonOpTokens.isEmpty()) nonOpTokens.add(rawQuery);
        List<Object> params = new ArrayList<>();
        List<String> tokenClauses = new ArrayList<>();
        for (String tok : nonOpTokens) {
            String esc = tok.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            tokenClauses.add("(m.message LIKE ? ESCAPE '\\')");
            params.add("%" + esc + "%");
        }
        StringBuilder sql = new StringBuilder(
                "SELECT m.id, m.sid, json_extract(m.message, '$.role') AS role," +
                        " substr(m.message, max(1, instr(m.message, ?) - 40), 120) AS snippet," +
                        " m.created AS timestamp" +
                        " FROM " + TABLE_NAME_MESSAGES + " m" +
                        " WHERE (" + String.join(" OR ", tokenClauses) + ")"
        );
        // instr() 用第一个 token 定位 snippet 起始位置
        params.add(0, nonOpTokens.get(0));
        handleRoleFilterAppend(sql, params, roles);
        sql.append(" ORDER BY m.created DESC LIMIT ?");
        params.add(limit);
        return handleExecuteSearchQuery(conn, sql.toString(), params);
    }

    // 上下文查询：匹配消息前后各 1 条
    private List<Map<String, Object>> handleContextFetch(Connection conn, int messageId) throws SQLException {
        String sql =
                "WITH target AS (" +
                        "  SELECT sid, created, id FROM " + TABLE_NAME_MESSAGES + " WHERE id = ?" +
                        ")" +
                        "SELECT json_extract(message, '$.role') AS role," +
                        "       json_extract(message, '$.content') AS content FROM (" +
                        "  SELECT m.id, m.created, m.message" +
                        "  FROM " + TABLE_NAME_MESSAGES + " m JOIN target t ON t.sid = m.sid" +
                        "  WHERE m.created < t.created OR (m.created = t.created AND m.id < t.id)" +
                        "  ORDER BY m.created DESC, m.id DESC LIMIT 1" +
                        ") UNION ALL " +
                        "SELECT json_extract(message, '$.role') AS role," +
                        "       json_extract(message, '$.content') AS content" +
                        " FROM " + TABLE_NAME_MESSAGES + " WHERE id = ?" +
                        " UNION ALL " +
                        "SELECT json_extract(message, '$.role') AS role," +
                        "       json_extract(message, '$.content') AS content FROM (" +
                        "  SELECT m.id, m.created, m.message" +
                        "  FROM " + TABLE_NAME_MESSAGES + " m JOIN target t ON t.sid = m.sid" +
                        "  WHERE m.created > t.created OR (m.created = t.created AND m.id > t.id)" +
                        "  ORDER BY m.created ASC, m.id ASC LIMIT 1" +
                        ")";
        List<Map<String, Object>> context = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, messageId);
            pstmt.setInt(2, messageId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String raw = rs.getString("content");
                    // 截取前 200 字符作为预览，与 Python 实现保持一致
                    String preview = raw != null ? raw.substring(0, Math.min(raw.length(), 200)) : "";
                    Map<String, Object> msg = new LinkedHashMap<>();
                    msg.put("role", rs.getString("role"));
                    msg.put("content", preview);
                    context.add(msg);
                }
            }
        }
        return context;
    }
}
