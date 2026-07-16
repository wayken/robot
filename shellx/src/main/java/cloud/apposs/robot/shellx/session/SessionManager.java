package cloud.apposs.robot.shellx.session;

import cloud.apposs.logger.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 会话管理器，负责会话的创建、存储、恢复和删除
 * 会话数据存储在 ${shellx.home}/sessions/ 目录下，每个会话一个 JSON 文件
 */
public class SessionManager {
    private final String sessionsPath;

    /** 内存中的活跃会话 */
    private final Map<String, ChatSession> activeSessions = new LinkedHashMap<>();

    public SessionManager(String sessionsPath) {
        this.sessionsPath = sessionsPath;
        ensureDir();
    }

    /**
     * 创建新会话
     */
    public ChatSession createSession(String agentId, String workingDir) {
        ChatSession session = new ChatSession(agentId, workingDir);
        activeSessions.put(session.getId(), session);
        saveSession(session);
        return session;
    }

    /**
     * 获取活跃会话
     */
    public ChatSession getSession(String sessionId) {
        ChatSession session = activeSessions.get(sessionId);
        if (session == null) {
            session = loadSession(sessionId);
            if (session != null) {
                activeSessions.put(sessionId, session);
            }
        }
        return session;
    }

    /**
     * 列出所有会话
     */
    public List<ChatSession> listSessions() {
        List<ChatSession> sessions = new ArrayList<>();
        File dir = new File(sessionsPath);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".session"));
        if (files != null) {
            for (File file : files) {
                ChatSession session = loadSessionFromFile(file);
                if (session != null) {
                    sessions.add(session);
                }
            }
        }
        // 按最后活跃时间降序排列
        Collections.sort(sessions, new Comparator<ChatSession>() {
            @Override
            public int compare(ChatSession a, ChatSession b) {
                return Long.compare(b.getLastActiveAt(), a.getLastActiveAt());
            }
        });
        return sessions;
    }

    /**
     * 列出当前目录下的会话
     */
    public List<ChatSession> listSessionsByDir(String workingDir) {
        List<ChatSession> all = listSessions();
        List<ChatSession> filtered = new ArrayList<>();
        for (ChatSession s : all) {
            if (workingDir.equals(s.getWorkingDir())) {
                filtered.add(s);
            }
        }
        return filtered;
    }

    /**
     * 获取当前目录最近的会话
     */
    public ChatSession getLatestSession(String workingDir) {
        List<ChatSession> sessions = listSessionsByDir(workingDir);
        return sessions.isEmpty() ? null : sessions.get(0);
    }

    /**
     * 删除会话
     */
    public boolean deleteSession(String sessionId) {
        activeSessions.remove(sessionId);
        File file = new File(sessionsPath, sessionId + ".session");
        return file.exists() && file.delete();
    }

    /**
     * 保存会话到磁盘
     */
    public void saveSession(ChatSession session) {
        if (session == null) {
            return;
        }
        File file = new File(sessionsPath, session.getId() + ".session");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(session);
        } catch (IOException e) {
            Logger.error(e, "Failed to save session: " + session.getId());
        }
    }

    /**
     * 从磁盘加载会话
     */
    private ChatSession loadSession(String sessionId) {
        File file = new File(sessionsPath, sessionId + ".session");
        return loadSessionFromFile(file);
    }

    private ChatSession loadSessionFromFile(File file) {
        if (!file.exists()) {
            return null;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (ChatSession) ois.readObject();
        } catch (Exception e) {
            Logger.error(e, "Failed to load session from: " + file.getName());
            return null;
        }
    }

    private void ensureDir() {
        try {
            Path path = Paths.get(sessionsPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            Logger.error(e, "Failed to create sessions directory: " + sessionsPath);
        }
    }
}
