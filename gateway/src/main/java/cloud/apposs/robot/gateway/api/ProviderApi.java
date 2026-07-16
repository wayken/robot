package cloud.apposs.robot.gateway.api;

import cloud.apposs.bootor.cachex.DataSource;
import cloud.apposs.cachex.CacheKey;
import cloud.apposs.cachex.CacheXConfig;
import cloud.apposs.cachex.NoCacheKey;
import cloud.apposs.cachex.database.Entity;
import cloud.apposs.cachex.database.Query;
import cloud.apposs.cachex.database.Updater;
import cloud.apposs.cachex.database.Where;
import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.react.React;
import cloud.apposs.rest.annotation.Executor;
import cloud.apposs.rest.annotation.Model;
import cloud.apposs.rest.annotation.Request;
import cloud.apposs.rest.annotation.RestAction;
import cloud.apposs.robot.gateway.GatewayConfig;
import cloud.apposs.robot.gateway.dispatch.WorkerDispatcher;
import cloud.apposs.robot.gateway.loader.ProviderLoader;
import cloud.apposs.robot.gateway.loader.ProviderLoader.ProviderCacheKey;
import cloud.apposs.robot.gateway.model.ProviderModel;
import cloud.apposs.robot.gateway.struct.ProviderStruct;
import cloud.apposs.util.*;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Executor
@RestAction
public class ProviderApi {
    private static final String RESOURCE_FILE = "providers.json";

    @Autowired
    private WorkerDispatcher dispatcher;

    public static final Map<Long, Param> AI_SYSTEM_PROVIDERS = new HashMap<>();
    static {
        InputStream resource = ProviderApi.class.getClassLoader().getResourceAsStream(RESOURCE_FILE);
        Table<Param> providers = JsonUtil.parseJsonTable(resource);
        for (Param provider : providers) {
            long id = provider.getInt(ProviderStruct.Info.ID);
            String name = provider.getString(ProviderStruct.Info.NAME);
            String type = provider.getString(ProviderStruct.Info.TYPE);
            int system = provider.getInt(ProviderStruct.Info.SYSTEM, 0);
            String url = provider.getString(ProviderStruct.Info.URL);
            Table<String> models = provider.getTable(ProviderStruct.Info.MODELS);
            Table<Param> keys = provider.getTable(ProviderStruct.Info.API_KEYS);
            Param infomation = Param.builder(ProviderStruct.Info.ID, id)
                    .setString(ProviderStruct.Info.NAME, name)
                    .setString(ProviderStruct.Info.TYPE, type)
                    .setInt(ProviderStruct.Info.SYSTEM, system)
                    .setString(ProviderStruct.Info.URL, url)
                    .setTable(ProviderStruct.Info.MODELS, models)
                    .setTable(ProviderStruct.Info.API_KEYS, keys);
            AI_SYSTEM_PROVIDERS.put(id, infomation);
        }
    }

    public static final int DEFAULT_WORKER_ID = 1;
    public static final int DEFAULT_IDC_ID = 1;

    private final DataSource<ProviderCacheKey> providerSource;

    private final IdWorker idWorker;

    public ProviderApi(GatewayConfig config) throws Exception {
        this.idWorker = IdWorker.builder(DEFAULT_WORKER_ID, DEFAULT_IDC_ID);
        CacheXConfig cacheXConfig = new CacheXConfig();
        CacheXConfig.DbConfig dbConfig = cacheXConfig.getDbConfig();
        dbConfig.setDialect(config.getDatabaseDialect());
        dbConfig.setJdbcUrl(config.getDatabaseUrl());
        dbConfig.setUsername(config.getDatabaseUsername());
        dbConfig.setPassword(config.getDatabasePassword());
        dbConfig.setMinConnections(config.getDatabaseMinPoolSize());
        dbConfig.setMaxConnections(config.getDatabaseMaxPoolSize());
        this.providerSource = new DataSource<>(cacheXConfig, new ProviderLoader());
    }

    /**
     * 获取模型服务商列表
     * 返回系统内置服务商 + 数据库中用户自定义的服务商
     */
    @Request.Read("/api/provider/list")
    public React<StandardResult> list(@Model ProviderModel.List request) {
        return React.emitter(() -> {
            Table<Param> dataList = handleDataListLoad(request.getAid());
            return StandardResult.success(dataList);
        });
    }

    /**
     * 新增模型服务商
     */
    @Request.Read("/api/provider/add")
    public React<StandardResult> add(@Model ProviderModel.Add request) {
        return React.emitter(() -> {
            String providerId = String.valueOf(idWorker.nextId());
            ProviderCacheKey cacheKey = new ProviderCacheKey(request.getAid(), Long.parseLong(providerId));
            Entity infomation = Entity.builder(ProviderStruct.Info.ID, providerId);
            infomation.setLong(ProviderStruct.Info.AID, request.getAid())
                    .setString(ProviderStruct.Info.NAME, request.getName())
                    .setString(ProviderStruct.Info.TYPE, request.getType())
                    .setInt(ProviderStruct.Info.SYSTEM, 0)
                    .setString(ProviderStruct.Info.URL, request.getUrl())
                    .setTable(ProviderStruct.Info.MODELS, request.getModels())
                    .setTable(ProviderStruct.Info.API_KEYS, request.getKeys());
            int count = providerSource.put(cacheKey, infomation, ProviderStruct.Protocol.getInfoProviderSchema());
            if (count < 0) {
                return StandardResult.error(Errno.ERROR);
            }
            Param response = Param.builder().setString(ProviderStruct.Info.ID, providerId);
            return StandardResult.success(response);
        });
    }

    /**
     * 更新模型服务商
     * 如果数据库中不存在（仅在providers.json中），则插入一条新记录
     */
    @Request.Read("/api/provider/update")
    public React<StandardResult> update(@Model ProviderModel.Update request) {
        return React.emitter(() -> {
            ProviderCacheKey cacheKey = new ProviderCacheKey(request.getAid(), request.getId());
            Updater updater = Updater.builder();
            updater.add(ProviderStruct.Info.NAME, request.getName())
                    .add(ProviderStruct.Info.TYPE, request.getType())
                    .add(ProviderStruct.Info.URL, request.getUrl())
                    .add(ProviderStruct.Info.STATUS, request.getStatus())
                    .add(ProviderStruct.Info.MODELS, JsonUtil.toJson(request.getModels()))
                    .add(ProviderStruct.Info.API_KEYS, JsonUtil.toJson(request.getKeys()));
            updater.where(ProviderStruct.Info.AID, Where.EQ, request.getAid())
                    .and(ProviderStruct.Info.ID, Where.EQ, request.getId());
            int count = providerSource.update(cacheKey, updater, ProviderStruct.Protocol.getInfoProviderSchema());
            if (count <= 0) {
                // 数据库中不存在，从系统配置中获取基础信息后插入
                Param matchedSystemProvider = AI_SYSTEM_PROVIDERS.get(request.getId());
                if (matchedSystemProvider == null) {
                    return StandardResult.error(Errno.ENOT_FOUND);
                }
                Entity infomation = Entity.builder(ProviderStruct.Info.ID, request.getId());
                infomation.setLong(ProviderStruct.Info.AID, request.getAid())
                        .setString(ProviderStruct.Info.NAME, request.getName() != null ? request.getName() : matchedSystemProvider.getString(ProviderStruct.Info.NAME))
                        .setString(ProviderStruct.Info.TYPE, request.getType() != null ? request.getType() : matchedSystemProvider.getString(ProviderStruct.Info.TYPE))
                        .setInt(ProviderStruct.Info.SYSTEM, 1)
                        .setString(ProviderStruct.Info.URL, request.getUrl() != null ? request.getUrl() : matchedSystemProvider.getString(ProviderStruct.Info.URL))
                        .setInt(ProviderStruct.Info.STATUS, request.getStatus());
                Table<Param> models = request.getModels() != null ? request.getModels() : matchedSystemProvider.getTable(ProviderStruct.Info.MODELS);
                if (models != null) {
                    infomation.setString(ProviderStruct.Info.MODELS, JsonUtil.toJson(models));
                }
                Table<String> keys = request.getKeys() != null ? request.getKeys() : matchedSystemProvider.getTable(ProviderStruct.Info.API_KEYS);
                if (keys != null) {
                    infomation.setString(ProviderStruct.Info.API_KEYS, JsonUtil.toJson(keys));
                }
                count = providerSource.put(cacheKey, infomation, ProviderStruct.Protocol.getInfoProviderSchema());
            }
            if (count < 0) {
                return StandardResult.error(Errno.ERROR);
            }
            handleProviderSyncToNodes(request.getAid());
            Param response = Param.builder().setLong(ProviderStruct.Info.ID, request.getId());
            return StandardResult.success(response);
        });
    }

    /**
     * 删除模型服务商
     */
    @Request.Read("/api/provider/delete")
    public React<StandardResult> delete(@Model ProviderModel.Delete request) {
        return React.emitter(() -> {
            CacheKey<?> cacheKey = new ProviderCacheKey(request.getAid(), Long.parseLong(request.getId()));
            Where where = Where.builder(ProviderStruct.Info.AID, Where.EQ, request.getAid())
                    .and(ProviderStruct.Info.ID, Where.EQ, request.getId());
            int count = providerSource.delete(cacheKey, where);
            if (count < 0) {
                return StandardResult.error(Errno.ERROR);
            }
            handleProviderSyncToNodes(request.getAid());
            Param response = Param.builder().setString(ProviderStruct.Info.ID, request.getId());
            return StandardResult.success(response);
        });
    }

    public Table<Param> handleDataListLoad(long aid) throws Exception {
        Table<Param> dataList = Table.builder();
        // 加载系统内置服务商
        dataList.addAll(AI_SYSTEM_PROVIDERS.values());
        // 加载数据库中用户自定义的服务商
        CacheKey<?> cacheKey = NoCacheKey.getInstance();
        Where where = Where.builder(ProviderStruct.Info.AID, Where.EQ, aid);
        Query query = Query.builder(where);
        List<Entity> result = providerSource.query(cacheKey, query, ProviderStruct.Protocol.getInfoProviderSchema());
        if (result == null) {
            return dataList;
        }
        for (Entity data : result) {
            Param infomation = handleDataFormat(data);
            long id = infomation.getLong(ProviderStruct.Info.ID);
            if (AI_SYSTEM_PROVIDERS.containsKey(id)) {
                Param matchedData = AI_SYSTEM_PROVIDERS.get(id);
                matchedData.putAll(infomation);
                continue;
            }
            dataList.add(infomation);
        }
        return dataList;
    }

    private Param handleDataFormat(Entity data) {
        Param infomation = Param.builder(data,
                ProviderStruct.Info.ID,
                ProviderStruct.Info.AID,
                ProviderStruct.Info.NAME,
                ProviderStruct.Info.TYPE,
                ProviderStruct.Info.URL,
                ProviderStruct.Info.STATUS,
                ProviderStruct.Info.MODELS,
                ProviderStruct.Info.API_KEYS
        );
        boolean isSystem = data.getInt(ProviderStruct.Info.SYSTEM, 0) == 1;
        infomation.setBoolean(ProviderStruct.Info.SYSTEM, isSystem);
        String models = data.getString(ProviderStruct.Info.MODELS);
        if (models != null) {
            infomation.setTable(ProviderStruct.Info.MODELS, JsonUtil.parseJsonTable(models));
        }
        String keys = data.getString(ProviderStruct.Info.API_KEYS);
        if (keys != null) {
            infomation.setTable(ProviderStruct.Info.API_KEYS, JsonUtil.parseJsonTable(keys));
        }
        return infomation;
    }

    // 同步provider配置到所有存活的节点
    private void handleProviderSyncToNodes(long aid) throws Exception {
        Table<Param> providerList = handleDataListLoad(aid);
        Param payload = Param.builder()
                .setTable("providers", providerList);
        dispatcher.dispatch(aid, WorkerDispatcher.EventType.PROVIDER_CONFIGURATION_SYNC, payload);
    }
}
