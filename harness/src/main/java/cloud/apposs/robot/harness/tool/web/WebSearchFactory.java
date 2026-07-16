package cloud.apposs.robot.harness.tool.web;

public final class WebSearchFactory {
    public static WebSearcher create(String type) {
        if (WebSearcher.SEARCH_TYPE_DUCKDUCKGO.equalsIgnoreCase(type)) {
            return new DuckDuckGoSearcher();
        }
        throw new IllegalArgumentException("Unsupported search type: " + type);
    }
}
