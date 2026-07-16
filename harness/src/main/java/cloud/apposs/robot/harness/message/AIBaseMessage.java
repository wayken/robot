package cloud.apposs.robot.harness.message;

import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

import java.util.ArrayList;
import java.util.List;

public abstract class AIBaseMessage implements AIMessage {
    protected String role;

    protected String content;

    protected String reasoning;

    protected long timestamp;

    // 多模态图片列表，为null表示纯文本消息
    protected List<AIImageContent> images;

    public AIBaseMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    @Override
    public String getRole() {
        return role;
    }

    @Override
    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String getReasoning() {
        return reasoning;
    }

    @Override
    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public List<AIImageContent> getImages() {
        return images;
    }

    @Override
    public void setImages(List<AIImageContent> images) {
        this.images = images;
    }

    @Override
    public void addImage(AIImageContent image) {
        if (image == null) {
            return;
        }
        if (this.images == null) {
            this.images = new ArrayList<>();
        }
        this.images.add(image);
    }

    @Override
    public boolean hasImages() {
        return images != null && !images.isEmpty();
    }

    @Override
    public Param deserialize() {
        Param infomation = Param.builder("role", role).setLong("timestamp", timestamp);
        if (hasImages()) {
            // 多模态消息：content为数组，包含文本和图片
            Table<Param> contentParts = Table.builder();
            if (content != null && !content.isEmpty()) {
                contentParts.add(Param.builder("type", "text").setString("text", content));
            }
            for (AIImageContent image : images) {
                contentParts.add(image.deserialize());
            }
            infomation.setTable("content", contentParts);
        } else {
            infomation.setString("content", content);
        }
        return infomation;
    }
}
