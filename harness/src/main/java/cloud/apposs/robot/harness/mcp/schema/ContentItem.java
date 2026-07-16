package cloud.apposs.robot.harness.mcp.schema;

import cloud.apposs.util.Param;

/**
 * 工具调用结果内容项
 */
public final class ContentItem {
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_RESOURCE = "resource";

    private final String type;
    private final String text;
    private final String data;
    private final String mimeType;

    public ContentItem(String type, String text, String data, String mimeType) {
        this.type = type;
        this.text = text;
        this.data = data;
        this.mimeType = mimeType;
    }

    public String getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public String getData() {
        return data;
    }

    public String getMimeType() {
        return mimeType;
    }

    public boolean isText() {
        return TYPE_TEXT.equals(type);
    }

    public static ContentItem fromParam(Param param) {
        if (param == null) {
            return null;
        }
        String type = param.getString("type", TYPE_TEXT);
        String text = param.getString("text");
        String data = param.getString("data");
        String mimeType = param.getString("mimeType");
        return new ContentItem(type, text, data, mimeType);
    }
}
