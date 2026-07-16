package cloud.apposs.robot.harness.tool.web;

import cloud.apposs.robot.harness.util.Strings;
import javafx.scene.chart.ChartBuilder;

public class SearchResult {
    // 提供该结果的服务ID
    private String id;
    // 结果链接
    private String url;
    // 结果标题
    private String title;
    // 摘要/片段
    private String snippet;
    // 来源域名
    private String source;
    // 发布时间（原始字符串，尽可能保留）
    private String date;

    public static SearchResult builder() {
        return new SearchResult();
    }

    public String getId() {
        return id;
    }

    public SearchResult setId(String id) {
        this.id = id;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public SearchResult setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public SearchResult setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getSnippet() {
        return snippet;
    }

    public SearchResult setSnippet(String snippet) {
        this.snippet = snippet;
        return this;
    }

    public String getSource() {
        return source;
    }

    public SearchResult setSource(String source) {
        this.source = source;
        return this;
    }

    public String getDate() {
        return date;
    }

    public SearchResult setDate(String date) {
        this.date = date;
        return this;
    }

    /**
     * 格式化为 Markdown 行，用于返回给 LLM
     */
    public String toMarkdown() {
        StringBuilder builder = new StringBuilder();
        builder.append("**").append(title != null ? title : "Untitled").append("**");
        if (source != null || date != null) {
            builder.append(" — ");
            if (source != null) builder.append(source);
            if (source != null && date != null) builder.append(" | ");
            if (date != null) builder.append(date);
        }
        builder.append("\n");
        if (snippet != null && !Strings.isBlank(snippet)) {
            builder.append(snippet).append("\n");
        }
        if (url != null && !Strings.isBlank(url)) {
            builder.append("🔗 ").append(url).append("\n");
        }
        return builder.toString();
    }
}
