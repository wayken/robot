package cloud.apposs.robot.harness.mcp.schema;

import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具调用结果
 */
public class CallToolResult {
    private final List<ContentItem> content;
    private final boolean error;

    public CallToolResult(List<ContentItem> content, boolean error) {
        this.content = content != null ? content : new ArrayList<ContentItem>();
        this.error = error;
    }

    public List<ContentItem> getContent() {
        return content;
    }

    public boolean isError() {
        return error;
    }

    /**
     * 从 JSON-RPC result Param 解析工具调用结果
     */
    public static CallToolResult fromParam(Param result) {
        if (result == null) {
            return new CallToolResult(null, false);
        }
        List<ContentItem> items = new ArrayList<ContentItem>();
        Table<Param> contentTable = result.getTable("content");
        if (contentTable != null) {
            for (Param itemParam : contentTable) {
                ContentItem item = ContentItem.fromParam(itemParam);
                if (item != null) {
                    items.add(item);
                }
            }
        }
        Boolean isError = result.getBoolean("isError", false);
        return new CallToolResult(items, isError != null && isError);
    }
}
