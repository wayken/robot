package cloud.apposs.robot.worker;

import cloud.apposs.configure.Value;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.logger.Appender;
import cloud.apposs.logger.Logger;
import cloud.apposs.websocket.WSConfig;

import java.io.File;

@Component
public class WorkerConfig extends WSConfig {
    private String basePackage = "cloud.apposs.robot.worker";

    /** 绑定服务器地址，端口 */
    @Value("worker.framework.http.host")
    private String host = WorkerConstants.DEFAULT_HTTP_HOST;
    @Value("worker.framework.http.port")
    private int port = WorkerConstants.DEFAULT_HTTP_PORT;

    /** 半连接队列数 */
    @Value("worker.network.backlog")
    private int backlog = 1024;

    /**
     * SO_REUSEADDR对应于套接字选项中的SO_REUSEADDR，这个参数表示允许重复使用本地地址和端口，
     * 比如，某个服务器进程占用了TCP的80端口进行监听，此时再次监听该端口就会返回错误，使用该参数就可以解决问题，
     * 该参数允许共用该端口，这个在服务器程序中比较常使用，
     * 比如某个进程非正常退出，该程序占用的端口可能要被占用一段时间才能允许其他进程使用，
     * 而且程序死掉以后，内核一需要一定的时间才能够释放此端口，不设置SO_REUSEADDR就无法正常使用该端口
     */
    @Value("worker.network.reuse-address")
    private boolean reuseAddress = true;

    /**
     * 开启此参数，那么客户端在每次发送数据时，无论数据包的大小都会将这些数据发送出去
     * 参考：
     * http://blog.csdn.net/huang_xw/article/details/7340241
     * http://www.open-open.com/lib/view/open1412994697952.html
     */
    @Value("worker.network.tcp_nodelay")
    private boolean tcpNoDelay = true;

    /**
     * 多少个EventLoop轮询器，主要用于处理各自网络读写数据，
     * 当服务性能不足可提高此配置提升对网络IO的并发处理，但注意EventLoop业务层必须要做到异步，不能有同步阻塞请求
     */
    @Value("worker.network.eventloop_count")
    private int numOfGroup = Runtime.getRuntime().availableProcessors() + 1;

    /**
     * 工作线程池数量
     */
    @Value("worker.network.worker_count")
    private int workerCount = Runtime.getRuntime().availableProcessors() << 1;

    /**
     * 是否采用Linux底层Epoll网络模型，针对底层为NETTY
     * Netty底层会通过Native方法为调用底层Epoll函数，可以提升性能，减少GC
     */
    @Value("worker.network.use_linux_epoll")
    protected boolean useLinuxEpoll = false;

    @Value("worker.framework.charset")
    private String charset = WorkerConstants.DEFAULT_CHARSET;

    /** WebSocket最大帧内容长度，默认50MB */
    @Value("worker.network.max_content_length")
    private int maxHttpContentLength = 50 * 1024 * 1024;

    @Value("worker.framework.request_log")
    private boolean requestLog = false;

    @Value("worker.framework.workhome")
    private String workhome = WorkerConstants.DEFAULT_WORKHOME;

    /**
     * 请求转发连接池大小，用于连接复用，减少连接创建开销，超过此连接池大小则作为短链接请求，不再回收到连接池
     * 如果连接池大小为0则不会进行连接复用，每次请求都会创建新的连接
     */
    @Value("worker.framework.proxy_pool.pool_size")
    private int proxyPoolSize = 128;

    /**
     * 请求转发连接超时时间，单位毫秒，默认为12秒
     */
    @Value("worker.framework.proxy_pool.connect_timeout")
    private int proxyConnectTimeout = 12 * 1000;

    /**
     * 请求转发读写超时时间，单位毫秒，默认为60秒
     */
    @Value("worker.framework.proxy_pool.socket_timeout")
    private int proxySocketTimeout = 60 * 1000;

    /** 日志配置相关 */
    @Value("worker.logger.appender")
    protected String logAppender = Appender.CONSOLE;
    @Value("worker.logger.level")
    protected String logLevel = "INFO";
    @Value("worker.logger.path")
    protected String logPath = "log";
    @Value("worker.logger.format")
    protected String logFormat = Logger.DEFAULT_LOG_FORMAT;

    @Override
    public String getBasePackage() {
        return basePackage;
    }

    @Override
    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public int getBacklog() {
        return backlog;
    }

    @Override
    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    @Override
    public boolean isReuseAddress() {
        return reuseAddress;
    }

    @Override
    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    @Override
    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    @Override
    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    @Override
    public int getNumOfGroup() {
        return numOfGroup;
    }

    @Override
    public void setNumOfGroup(int numOfGroup) {
        this.numOfGroup = numOfGroup;
    }

    @Override
    public int getWorkerCount() {
        return workerCount;
    }

    @Override
    public void setWorkerCount(int workerCount) {
        this.workerCount = workerCount;
    }

    public boolean isUseLinuxEpoll() {
        return useLinuxEpoll;
    }

    public void setUseLinuxEpoll(boolean useLinuxEpoll) {
        this.useLinuxEpoll = useLinuxEpoll;
    }

    @Override
    public String getCharset() {
        return charset;
    }

    @Override
    public void setCharset(String charset) {
        this.charset = charset;
    }

    @Override
    public int getMaxHttpContentLength() {
        return maxHttpContentLength;
    }

    @Override
    public void setMaxHttpContentLength(int maxHttpContentLength) {
        this.maxHttpContentLength = maxHttpContentLength;
    }

    public boolean isRequestLog() {
        return requestLog;
    }

    public void setRequestLog(boolean requestLog) {
        this.requestLog = requestLog;
    }

    public String getWorkhome() {
        return workhome;
    }

    public void setWorkhome(String workhome) {
        if (workhome.startsWith("~")) {
            workhome = System.getProperty("user.home") + workhome.substring(1);
        }
        this.workhome = workhome;
    }

    public String getWorkspace() {
        return workhome + File.separator + WorkerConstants.DEFAULT_WORKSPACE;
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

    @Override
    public String getLogAppender() {
        return logAppender;
    }

    @Override
    public void setLogAppender(String logAppender) {
        this.logAppender = logAppender;
    }

    @Override
    public String getLogLevel() {
        return logLevel;
    }

    @Override
    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    @Override
    public String getLogPath() {
        return logPath;
    }

    @Override
    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    @Override
    public String getLogFormat() {
        return logFormat;
    }

    @Override
    public void setLogFormat(String logFormat) {
        this.logFormat = logFormat;
    }
}
