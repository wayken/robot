package cloud.apposs.robot.harness.message.kind;

import cloud.apposs.robot.harness.message.AIBaseMessage;
import cloud.apposs.robot.harness.message.AIImageContent;

import java.util.Arrays;
import java.util.List;

/**
 * 用户消息，表示当前消息内容是用户输入的文本内容，支持纯文本和图文多模态两种形式，消息示例：
 * <pre>
 *     1. 纯文本消息：{"role": "user", "content": "Hi there!"}
 *     2. 多模态消息：{
 *         "role": "user",
 *         "content": [
 *             {"type": "text", "text": "What's in this image?"},
 *             {"type": "image_url", "image_url": {"url": "https://example.com/image.png", "detail": "auto"}}
 *         ]
 *     }
 * </pre>
 */
public class AIUserMessage extends AIBaseMessage {
    public static String ROLE = "user";

    public AIUserMessage(String content) {
        super(ROLE, content);
    }

    /**
     * 构建包含单张图片的多模态用户消息
     *
     * @param content 文本内容
     * @param image   图片内容
     */
    public AIUserMessage(String content, AIImageContent image) {
        super(ROLE, content);
        addImage(image);
    }

    /**
     * 构建包含单张二进制图片的多模态用户消息，自动转换为Base64编码
     *
     * @param content   文本内容
     * @param mimeType  MIME类型，如 image/png、image/jpeg
     * @param imageData 二进制图片数据
     */
    public AIUserMessage(String content, String mimeType, byte[] imageData) {
        super(ROLE, content);
        addImage(AIImageContent.ofBytes(mimeType, imageData));
    }

    /**
     * 构建包含多张图片的多模态用户消息
     *
     * @param content 文本内容
     * @param images  图片内容列表
     */
    public AIUserMessage(String content, List<AIImageContent> images) {
        super(ROLE, content);
        this.images = images;
    }

    /**
     * 构建包含多张图片的多模态用户消息
     *
     * @param content 文本内容
     * @param images  图片内容数组
     */
    public AIUserMessage(String content, AIImageContent... images) {
        super(ROLE, content);
        this.images = Arrays.asList(images);
    }
}
