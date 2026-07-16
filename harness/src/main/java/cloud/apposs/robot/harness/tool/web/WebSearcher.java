package cloud.apposs.robot.harness.tool.web;

import java.util.List;

public interface WebSearcher {
    String SEARCH_TYPE_DUCKDUCKGO = "duckduckgo";

    /**
     * 提供商唯一 ID，建议使用域名或服务名，要求全局唯一且不变，以便于区分不同来源的搜索结果
     */
    String id();

    /**
     * 执行搜索，返回结构化结果列表
     *
     * @param  query 搜索关键词
     * @return 搜索结果列表；不应返回 null，失败时抛异常
     */
    List<SearchResult> search(String query) throws Exception;
}
