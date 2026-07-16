package cloud.apposs.robot.harness;

import cloud.apposs.configure.Value;

/**
 * Harness框架配置，主要包含对每个智能体的配置信息，框架会根据这些配置信息来创建和管理智能体的生命周期和行为
 */
public class HarnessSetting {
    @Value("framework.workspace")
    private String workspace = HarnessConstants.DEFAULT_WORKSPACE;

    @Value("framework.request_log")
    private boolean requestLog = false;

    /**
     * 请求转发连接池大小，用于连接复用，减少连接创建开销，超过此连接池大小则作为短链接请求，不再回收到连接池
     * 如果连接池大小为0则不会进行连接复用，每次请求都会创建新的连接
     */
    @Value("framework.proxy_pool.pool_size")
    private int proxyPoolSize = 128;

    // 请求转发连接超时时间，单位毫秒，默认为12秒
    @Value("framework.proxy_pool.connect_timeout")
    private int proxyConnectTimeout = 12 * 1000;

    // 请求转发读写超时时间，单位毫秒，默认为60秒
    @Value("framework.proxy_pool.socket_timeout")
    private int proxySocketTimeout = 60 * 1000;

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public boolean isRequestLog() {
        return requestLog;
    }

    public void setRequestLog(boolean requestLog) {
        this.requestLog = requestLog;
    }

    public int getProxyPoolSize() {
        return proxyPoolSize;
    }

    public void setProxyPoolSize(int proxyPoolSize) {
        this.proxyPoolSize = proxyPoolSize;
    }

    public int getProxyConnectTimeout() {
        return proxyConnectTimeout;
    }

    public void setProxyConnectTimeout(int proxyConnectTimeout) {
        this.proxyConnectTimeout = proxyConnectTimeout;
    }

    public int getProxySocketTimeout() {
        return proxySocketTimeout;
    }

    public void setProxySocketTimeout(int proxySocketTimeout) {
        this.proxySocketTimeout = proxySocketTimeout;
    }
}
