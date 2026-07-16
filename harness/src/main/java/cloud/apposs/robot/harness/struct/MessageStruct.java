package cloud.apposs.robot.harness.struct;

public class MessageStruct {
    // AI 智能体ID，一个Worker进程可以同时承载多个智能体，wid用于区分不同智能体的消息
    private String wid;

    // 当前会话ID，用于区分不同会话的消息，尤其在同一智能体同时处理多个会话时非常重要
    private String sid;

    // 当前迭代轮次ID，用于区分不同轮次的消息，一个用户消息请求下，可能会被多次迭代处理才结束
    private String rid;

    // 前端消息内容，通常是用户输入的文本消息，也可以包含其他类型的消息内容（如图片、文件等）
    private String message;

    public String getWid() {
        return wid;
    }

    public void setWid(String wid) {
        this.wid = wid;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getRid() {
        return rid;
    }

    public void setRid(String rid) {
        this.rid = rid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
