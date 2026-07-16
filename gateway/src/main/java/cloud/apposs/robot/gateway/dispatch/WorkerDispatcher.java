package cloud.apposs.robot.gateway.dispatch;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.logger.Logger;
import cloud.apposs.okhttp.*;
import cloud.apposs.react.IoSubscriber;
import cloud.apposs.robot.gateway.api.NodeApi;
import cloud.apposs.robot.gateway.struct.NodeStruct;
import cloud.apposs.robot.gateway.struct.ProviderStruct;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 工作节点事件分发器，负责将配置变更事件异步下发到所有存活节点
 * 支持不同事件类型：provider同步、消息渠道配置、权限更新等
 */
@Component
public class WorkerDispatcher {
    private static final long DEBOUNCE_DELAY_MS = 2000;

    @Autowired
    private NodeApi nodeApi;

    private final OkHttp okHttp;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private final Map<String, ScheduledFuture<?>> pendingSchedules = new ConcurrentHashMap<>();

    public WorkerDispatcher() throws Exception {
        this.okHttp = HttpBuilder.builder().socketTimeout(30 * 1000).retryCount(2).retrySleepTime(1000).build();
    }

    /**
     * 事件类型枚举，定义不同的节点同步事件及其对应的接口路径
     */
    public enum EventType {
        PROVIDER_CONFIGURATION_SYNC()
    }

    /**
     * 异步分发事件到所有节点（防抖：短时间内多次调用只执行最后一次）
     *
     * @param aid       账号ID
     * @param eventType 事件类型
     * @param payload   事件数据
     */
    public void dispatch(long aid, EventType eventType, Param payload) {
        String key = aid + ":" + eventType.name();
        ScheduledFuture<?> prevSchedule = pendingSchedules.get(key);
        if (prevSchedule != null) {
            prevSchedule.cancel(false);
        }
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            pendingSchedules.remove(key);
            handleEventDispatch(aid, eventType, payload);
        }, DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);
        pendingSchedules.put(key, future);
    }

    public void handleEventDispatch(long aid, EventType eventType, Param payload) {
        switch (eventType) {
            case PROVIDER_CONFIGURATION_SYNC:
                handleProviderConfigurationSync(aid, payload);
                break;
            default:
                Logger.warn("Unsupported event type: %s", eventType);
        }
    }

    private void handleProviderConfigurationSync(long aid, Param payload) {
        try {
            Table<Param> providers = payload.getTable("providers");
            Table<Param> matchedProviders = Table.builder();
            for (Param provider : providers) {
                int status = provider.getInt(ProviderStruct.Info.STATUS, 0);
                if (ProviderStruct.Status.isOn(status)) {
                    matchedProviders.add(provider);
                }
            }
            Table<Param> nodeList = nodeApi.handleDataListLoad(aid);
            for (Param node : nodeList) {
                String address = node.getString(NodeStruct.Info.ADDRESS);
                int port = node.getInt(NodeStruct.Info.PORT);
                if (!isSocketReachable(address, port)) {
                    continue;
                }
                String signature = node.getString(NodeStruct.Info.SIGNATURE);
                String remoteEndpoint = String.format("http://%s:%d/api/management/provider/sync", address, port);
                FormEntity form = FormEntity.builder(FormEntity.FORM_ENCTYPE_JSON)
                        .add("providers", matchedProviders);
                OkRequest okRequest = OkRequest.builder()
                        .header("authorization", signature)
                        .url(remoteEndpoint).post(form);
                okHttp.execute(okRequest).subscribe(new IoSubscriber<OkResponse>() {
                    @Override
                    public void onNext(OkResponse response) throws Exception {
                        Logger.info("Provider configuration sync dispatched to node %s:%d succefully", address, port);
                    }
                    @Override
                    public void onError(Throwable cause) {
                        Logger.error(cause, "Failed to dispatch provider configuration sync to node %s:%d", address, port);
                    }
                }).start();
            }
        } catch (Exception cause) {
            Logger.error(cause, "WorkerDispatcher dispatch error, aid=%d, event=%s", aid, EventType.PROVIDER_CONFIGURATION_SYNC);
        }
    }

    private boolean isSocketReachable(String address, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(address, port), 3000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
