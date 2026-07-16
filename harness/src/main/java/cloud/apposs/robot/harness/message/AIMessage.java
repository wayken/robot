package cloud.apposs.robot.harness.message;

import cloud.apposs.util.Param;

import java.util.List;

public interface AIMessage {
    /**
     * 获取消息类型
     *
     * @return 消息类型
     */
    String getRole();

    /**
     * 设置消息类型
     *
     * @param role 消息类型
     */
    void setRole(String role);

    /**
     * 获取消息内容
     *
     * @return 消息内容
     */
    String getContent();

    /**
     * 设置消息内容
     *
     * @param content 消息内容
     */
    void setContent(String content);

    /**
     * 获取推理内容（部分模型如DeepSeek-R1会在reasoning_content中返回思考过程）
     *
     * @return 推理内容
     */
    String getReasoning();

    /**
     * 设置推理内容
     *
     * @param reasoning 推理内容
     */
    void setReasoning(String reasoning);

    /**
     * 获取消息时间戳
     *
     * @return 消息时间戳
     */
    long getTimestamp();

    /**
     * 设置消息时间戳
     *
     * @param timestamp 消息时间戳
     */
    void setTimestamp(long timestamp);

    /**
     * 获取多模态图片列表，为null或空表示纯文本消息
     *
     * @return 图片内容列表
     */
    List<AIImageContent> getImages();

    /**
     * 设置多模态图片列表
     *
     * @param images 图片内容列表
     */
    void setImages(List<AIImageContent> images);

    /**
     * 追加一张图片到多模态图片列表
     *
     * @param image 图片内容
     */
    void addImage(AIImageContent image);

    /**
     * 判断当前消息是否包含图片（多模态消息）
     *
     * @return 是否包含图片
     */
    boolean hasImages();

    /**
     * 将消息转换为数据结构
     *
     * @return 消息数据结构
     */
    Param deserialize();
}
