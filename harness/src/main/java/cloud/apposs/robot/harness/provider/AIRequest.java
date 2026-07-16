package cloud.apposs.robot.harness.provider;

import cloud.apposs.robot.harness.message.AIMessages;
import cloud.apposs.robot.harness.tool.ITool;
import cloud.apposs.util.Table;

public class AIRequest {
    private final String wid;

    private final String sid;

    private final String rid;

    private final AIMessages messages;

    private final Table<ITool> tools;

    public AIRequest(String wid, String sid, String rid, AIMessages messages, Table<ITool> tools) {
        this.wid = wid;
        this.sid = sid;
        this.rid = rid;
        this.messages = messages;
        this.tools = tools;
    }

    public static AIRequest of(String wid, String sid, String rid, AIMessages messages, Table<ITool> tools) {
        return new AIRequest(wid, sid, rid, messages, tools);
    }

    public String getWid() {
        return wid;
    }

    public String getSid() {
        return sid;
    }

    public String getRid() {
        return rid;
    }

    public AIMessages getMessages() {
        return messages;
    }

    public Table<ITool> getTools() {
        return tools;
    }
}
