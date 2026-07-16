package cloud.apposs.robot.gateway;

import cloud.apposs.bootor.ApplicationContext;
import cloud.apposs.bootor.HttpApplication;
import cloud.apposs.configure.YamlConfigParser;
import cloud.apposs.util.GetOpt;

/**
 * AI网关服务，支持多实例部署，负责如下功能：
 * <pre>
 *    1. 负责接收和处理来自外部的请求、AI Agent配置转发
 *    2. 提供统一的接口和安全认证机制，支持负载均衡和高可用性
 *    3. 负责对外提供HTTP服务
 * </pre>
 * 启动AI网关服务：java -jar teambeit-robot-gateway.jar -c ~/etc/robot/gateway.yaml 2>&1 &
 */
public class GatewayApplication {
    public static void main(String[] args) throws Exception {
        ApplicationContext application = HttpApplication.build(generateConfiguration(args));
        application.setBanner(new GatewayBanner());
        application.run(GatewayApplication.class, args);
    }

    public static GatewayConfig generateConfiguration(String... args) throws Exception {
        String yamlFile = GatewayConstants.APPLICATION_CONFIG_FILE;
        // 判断是否从命令行中传递配置文件路径
        GetOpt option = new GetOpt(args);
        if (option.containsKey("c")) {
            yamlFile = option.get("c");
        }
        GatewayConfig config = new GatewayConfig();
        YamlConfigParser parser = new YamlConfigParser();
        parser.parse(config, yamlFile);
        return config;
    }
}
