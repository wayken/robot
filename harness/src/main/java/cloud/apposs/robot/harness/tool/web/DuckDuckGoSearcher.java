package cloud.apposs.robot.harness.tool.web;

import cloud.apposs.robot.harness.util.Strings;
import cn.hutool.http.HttpUtil;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DuckDuckGoSearcher implements WebSearcher {
    private static final String SEARCH_URL = "https://html.duckduckgo.com/html/";

    // 匹配 DuckDuckGo HTML 搜索结果
    private static final Pattern RESULT_PATTERN = Pattern.compile(
            "<a[^>]+class=\"result__a\"[^>]+href=\"([^\"]+)\"[^>]*>(.+?)</a>",
            Pattern.DOTALL);
    private static final Pattern SNIPPET_PATTERN = Pattern.compile(
            "<a[^>]+class=\"result__snippet\"[^>]*>(.+?)</a>",
            Pattern.DOTALL);
    private static final Pattern RESULT_BLOCK_PATTERN = Pattern.compile(
            "<div[^>]+class=\"result results_links[^\"]*\"[^>]*>(.*?)</div>\\s*(?=<div[^>]+class=\"result |$)",
            Pattern.DOTALL);

    @Override
    public String id() {
        return WebSearcher.SEARCH_TYPE_DUCKDUCKGO;
    }

    @Override
    public List<SearchResult> search(String query) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
        StringBuilder builder = new StringBuilder("q=").append(encoded);
        String response = null;
        int maxAttempts = 2;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                response = HttpUtil.createPost(SEARCH_URL)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Accept", "text/html")
                        .header("Accept-Language", "en-US,en;q=0.9,zh-CN;q=0.8")
                        .body(builder.toString())
                        .timeout(15000)
                        .execute()
                        .body();
                break;
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    throw e;
                }
                Thread.sleep(1000);
            }
        }
        return handleHtmlResultsParse(response, 5);
    }

    private List<SearchResult> handleHtmlResultsParse(String response, int limit) throws Exception {
        List<SearchResult> results = new ArrayList<>();
        if (response == null || Strings.isBlank(response)) {
            return results;
        }
        // 提取每个结果块
        Matcher titleMatcher = RESULT_PATTERN.matcher(response);
        Matcher snippetMatcher = SNIPPET_PATTERN.matcher(response);
        int count = 0;
        while (titleMatcher.find() && count < limit) {
            String rawUrl = titleMatcher.group(1);
            String rawTitle = titleMatcher.group(2);
            String url = handleUrlDecode(rawUrl);
            String title = handleHtmlStrip(rawTitle);
            String snippet = "";
            if (snippetMatcher.find()) {
                snippet = handleHtmlStrip(snippetMatcher.group(1));
            }
            if (Strings.isBlank(title) && Strings.isBlank(snippet)) {
                continue;
            }
            results.add(SearchResult.builder()
                    .setId(id())
                    .setTitle(title)
                    .setUrl(url)
                    .setSnippet(snippet)
                    .setSource(handleDomainExtract(url)));
            count++;
        }
        return results;
    }

    private String handleUrlDecode(String rawUrl) throws Exception {
        if (rawUrl == null) return "";
        // DuckDuckGo 使用 redirect URL
        if (rawUrl.contains("uddg=")) {
            String encoded = rawUrl.substring(rawUrl.indexOf("uddg=") + 5);
            int ampIndex = encoded.indexOf('&');
            if (ampIndex > 0) encoded = encoded.substring(0, ampIndex);
            return URLDecoder.decode(encoded, StandardCharsets.UTF_8.name());
        }
        return rawUrl;
    }

    private String handleHtmlStrip(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", "").replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<").replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"").replaceAll("&#39;", "'")
                .trim();
    }

    private String handleDomainExtract(String url) {
        try {
            return URI.create(url).getHost();
        } catch (Exception e) {
            return null;
        }
    }
}
