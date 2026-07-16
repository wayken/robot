package cloud.apposs.robot.gateway;

import cloud.apposs.bootor.BootorConfig;
import cloud.apposs.configure.Value;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.logger.Appender;
import cloud.apposs.logger.Logger;

@Component
public class GatewayConfig extends BootorConfig {
    private String basePackage = "cloud.apposs.robot.gateway";

    /** 绑定服务器地址 */
    @Value("gateway.http.host")
    private String host = "0.0.0.0";

    /** 绑定服务器端口 */
    @Value("gateway.http.port")
    private int port = 8160;

    /** 节点相关配置 */
    @Value("gateway.node.host")
    private String nodeHost = "";
    @Value("gateway.node.port")
    private int nodePort = -1;
    @Value("gateway.node.path")
    private String nodePath = "/gateway";

    /** 数据库相关配置 */
    @Value("gateway.database.dialect")
    private String databaseDialect = "sqlite";
    @Value("gateway.database.url")
    private String databaseUrl = "";
    @Value("gateway.database.username")
    private String databaseUsername = "";
    @Value("gateway.database.password")
    private String databasePassword = "";
    // 数据库连接池配置
    @Value("gateway.database.max_pool_size")
    private int databaseMaxPoolSize = 10;
    @Value("gateway.database.min_pool_size")
    private int databaseMinPoolSize = 1;

    /** 缓存相关配置 */
    @Value("gateway.cache.type")
    private String cacheType = "jvm";
    @Value("gateway.cache.expiration_time")
    private int cacheExpirationTime = 1 * 60 * 60 * 1000;
    @Value("gateway.cache.expiration_time_random")
    private boolean cacheExpirationTimeRandom = true;

    /** 日志配置相关 */
    @Value("gateway.logger.appender")
    protected String logAppender = Appender.CONSOLE;
    @Value("gateway.logger.level")
    protected String logLevel = "INFO";
    @Value("gateway.logger.path")
    protected String logPath = "log";
    @Value("gateway.logger.format")
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

    public String getNodeHost() {
        return nodeHost;
    }

    public void setNodeHost(String nodeHost) {
        this.nodeHost = nodeHost;
    }

    public int getNodePort() {
        return nodePort;
    }

    public void setNodePort(int nodePort) {
        this.nodePort = nodePort;
    }

    public String getNodePath() {
        return nodePath;
    }

    public void setNodePath(String nodePath) {
        this.nodePath = nodePath;
    }

    public String getDatabaseDialect() {
        return databaseDialect;
    }

    public void setDatabaseDialect(String databaseDialect) {
        this.databaseDialect = databaseDialect;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public void setDatabaseUrl(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    public String getDatabaseUsername() {
        return databaseUsername;
    }

    public void setDatabaseUsername(String databaseUsername) {
        this.databaseUsername = databaseUsername;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public void setDatabasePassword(String databasePassword) {
        this.databasePassword = databasePassword;
    }

    public int getDatabaseMaxPoolSize() {
        return databaseMaxPoolSize;
    }

    public void setDatabaseMaxPoolSize(int databaseMaxPoolSize) {
        this.databaseMaxPoolSize = databaseMaxPoolSize;
    }

    public int getDatabaseMinPoolSize() {
        return databaseMinPoolSize;
    }

    public void setDatabaseMinPoolSize(int databaseMinPoolSize) {
        this.databaseMinPoolSize = databaseMinPoolSize;
    }

    public String getCacheType() {
        return cacheType;
    }

    public void setCacheType(String cacheType) {
        this.cacheType = cacheType;
    }

    public int getCacheExpirationTime() {
        return cacheExpirationTime;
    }

    public void setCacheExpirationTime(int cacheExpirationTime) {
        this.cacheExpirationTime = cacheExpirationTime;
    }

    public boolean isCacheExpirationTimeRandom() {
        return cacheExpirationTimeRandom;
    }

    public void setCacheExpirationTimeRandom(boolean cacheExpirationTimeRandom) {
        this.cacheExpirationTimeRandom = cacheExpirationTimeRandom;
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
