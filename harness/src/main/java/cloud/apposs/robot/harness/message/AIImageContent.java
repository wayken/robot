package cloud.apposs.robot.harness.message;

import cloud.apposs.util.Param;

import java.util.Base64;

/**
 * AI多模态图片内容，支持URL和Base64两种方式传递图片，消息示例：
 * <pre>
 *     1. URL方式：{
 *         "type": "image_url",
 *         "image_url": {"url": "https://example.com/image.png", "detail": "auto"}
 *     }
 *     2. Base64方式：{
 *         "type": "image_url",
 *         "image_url": {"url": "data:image/png;base64,iVBORw0KGgo...", "detail": "auto"}
 *     }
 * </pre>
 */
public class AIImageContent {
    // 图片细节级别：自动
    public static final String DETAIL_AUTO = "auto";
    // 图片细节级别：低分辨率
    public static final String DETAIL_LOW = "low";
    // 图片细节级别：高分辨率
    public static final String DETAIL_HIGH = "high";

    // 图片URL或Base64编码的Data URL（如 data:image/png;base64,...）
    private final String url;

    // 图片细节级别，默认auto，可选值：auto、low、high
    private String detail;

    public AIImageContent(String url) {
        this(url, DETAIL_AUTO);
    }

    public AIImageContent(String url, String detail) {
        this.url = url;
        this.detail = detail;
    }

    /**
     * 通过Base64数据构建图片内容
     *
     * @param  mimeType   MIME类型，如 image/png、image/jpeg
     * @param  base64Data Base64编码的图片数据
     * @return AIImageContent实例
     */
    public static AIImageContent ofBase64(String mimeType, String base64Data) {
        return new AIImageContent("data:" + mimeType + ";base64," + base64Data);
    }

    /**
     * 通过二进制图片数据构建图片内容，自动转换为Base64编码
     *
     * @param  mimeType  MIME类型，如 image/png、image/jpeg
     * @param  imageData 二进制图片数据
     * @return AIImageContent实例
     */
    public static AIImageContent ofBytes(String mimeType, byte[] imageData) {
        String base64Data = Base64.getEncoder().encodeToString(imageData);
        return ofBase64(mimeType, base64Data);
    }

    /**
     * 通过URL构建图片内容
     *
     * @param  url 图片URL
     * @return AIImageContent实例
     */
    public static AIImageContent ofUrl(String url) {
        return new AIImageContent(url);
    }

    public String getUrl() {
        return url;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    /**
     * 将图片内容序列化为数据结构
     *
     * @return 图片内容数据结构
     */
    public Param deserialize() {
        Param imageUrl = Param.builder("url", url).setString("detail", detail);
        return Param.builder("type", "image_url").setParam("image_url", imageUrl);
    }

    /**
     * 从数据结构反序列化图片内容
     *
     * @param  data 数据结构
     * @return AIImageContent实例，数据无效时返回null
     */
    public static AIImageContent serialize(Param data) {
        if (data == null) {
            return null;
        }
        Param imageUrl = data.getParam("image_url");
        if (imageUrl == null) {
            return null;
        }
        String url = imageUrl.getString("url");
        if (url == null || url.isEmpty()) {
            return null;
        }
        String detail = imageUrl.getString("detail", DETAIL_AUTO);
        return new AIImageContent(url, detail);
    }
}
