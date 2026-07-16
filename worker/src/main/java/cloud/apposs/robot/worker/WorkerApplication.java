package cloud.apposs.robot.worker;

import cloud.apposs.configure.YamlConfigParser;
import cloud.apposs.util.GetOpt;
import cloud.apposs.websocket.ApplicationContext;
import cloud.apposs.websocket.WebSocketApplication;

/**
 * AI代理服务，支持多机多实例部署，负责如下功能：
 * <pre>
 *    1. 作为Websocket服务运行，提供WS事件监听、WS RPC实时通讯等功能
 *    2. 负责AI Agent的核心逻辑处理，包括任务调度、资源管理和执行环境的维护，支持多种AI模型和算法的集成。
 *    3. 负责提供WS RPC接口，供前端调用，如配置AI Agent的SOUL/SKILLS，分配任务，查看日志等操作。
 *    4. 通过监听消息队列事件，接收来自网关的任务指令和配置更新，执行相应的操作，并将结果通过消息队列反馈给网关，确保系统的解耦和可扩展性。
 * </pre>
 * 启动AI代理服务：java -jar teambeit-robot-worker.jar -c ~/etc/robot/worker.yaml 2>&1 &
 */
public class WorkerApplication {
    public static void main(String[] args) throws Exception {
        ApplicationContext application = WebSocketApplication.build(generateConfiguration(args));
        application.setBanner(new WorkerBanner());
        application.run(WorkerApplication.class, args);
    }

    public static WorkerConfig generateConfiguration(String... args) throws Exception {
        String yamlFile = WorkerConstants.APPLICATION_CONFIG_FILE;
        // 判断是否从命令行中传递配置文件路径
        GetOpt option = new GetOpt(args);
        if (option.containsKey("c")) {
            yamlFile = option.get("c");
        }
        WorkerConfig config = new WorkerConfig();
        YamlConfigParser parser = new YamlConfigParser();
        parser.parse(config, yamlFile);
        return config;
    }
}
