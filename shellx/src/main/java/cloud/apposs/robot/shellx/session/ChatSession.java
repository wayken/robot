package cloud.apposs.robot.shellx.session;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 聊天会话，记录一次交互的完整上下文
 */
public class ChatSession implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 会话ID */
    private String id;

    /** 关联的 Agent ID */
    private String agentId;

    /** 会话创建时间 */
    private long createdAt;

    /** 会话最后活跃时间 */
    private long lastActiveAt;

    /** 会话标题 */
    private String title;

    /** 工作目录（创建会话时的目录） */
    private String workingDir;

    /** 会话状态：active、closed */
    private String status;

    public ChatSession() {
        this.id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        this.createdAt = System.currentTimeMillis();
        this.lastActiveAt = this.createdAt;
        this.status = "active";
    }

    public ChatSession(String agentId, String workingDir) {
        this();
        this.agentId = agentId;
        this.workingDir = workingDir;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(long lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public void touch() {
        this.lastActiveAt = System.currentTimeMillis();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isActive() {
        return "active".equals(status);
    }

    public String getFormattedCreatedAt() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(createdAt));
    }

    public String getFormattedLastActiveAt() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(lastActiveAt));
    }

    @Override
    public String toString() {
        return String.format("[%s] agent=%s title=%s (%s)", id, agentId, title, getFormattedLastActiveAt());
    }
}
