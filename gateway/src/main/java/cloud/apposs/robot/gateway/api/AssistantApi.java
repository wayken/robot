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
import cloud.apposs.logger.Logger;
import cloud.apposs.okhttp.*;
import cloud.apposs.react.IoFunction;
import cloud.apposs.react.React;
import cloud.apposs.react.actor.Actor;
import cloud.apposs.rest.annotation.Executor;
import cloud.apposs.rest.annotation.Model;
import cloud.apposs.rest.annotation.Request;
import cloud.apposs.rest.annotation.RestAction;
import cloud.apposs.robot.gateway.GatewayConfig;
import cloud.apposs.robot.gateway.loader.AssistantLoader;
import cloud.apposs.robot.gateway.loader.AssistantLoader.AssistantCacheKey;
import cloud.apposs.robot.gateway.model.AssistantModel;
import cloud.apposs.robot.gateway.struct.AssistantStruct;
import cloud.apposs.robot.gateway.struct.CommonStruct;
import cloud.apposs.robot.gateway.struct.NodeStruct;
import cloud.apposs.util.*;

import java.util.Calendar;
import java.util.List;

@Executor
@RestAction
public class AssistantApi {
    public static final int DEFAULT_WORKER_ID = 1;
    public static final int DEFAULT_IDC_ID = 1;

    @Autowired
    private NodeApi nodeApi;

    private final DataSource<AssistantCacheKey> assistantSource;

    private final IdWorker idWorker;

    private final Actor actor;

    private final GatewayConfig config;

    private final OkHttp okHttp;

    public AssistantApi(GatewayConfig config) throws Exception {
        this.config = config;
        this.idWorker = IdWorker.builder(DEFAULT_WORKER_ID, DEFAULT_IDC_ID);
        CacheXConfig cacheXConfig = new CacheXConfig();
        CacheXConfig.DbConfig dbConfig = cacheXConfig.getDbConfig();
        dbConfig.setDialect(config.getDatabaseDialect());
        dbConfig.setJdbcUrl(config.getDatabaseUrl());
        dbConfig.setUsername(config.getDatabaseUsername());
        dbConfig.setPassword(config.getDatabasePassword());
        dbConfig.setMinConnections(config.getDatabaseMinPoolSize());
        dbConfig.setMaxConnections(config.getDatabaseMaxPoolSize());
        this.assistantSource = new DataSource<>(cacheXConfig, new AssistantLoader());
        this.actor = new Actor("Assistant-Actor-");
        this.okHttp = HttpBuilder.builder().socketTimeout(120 * 1000).retryCount(3).retrySleepTime(2000).build();
    }

    // 获取单个智能体详情
    @Request.Read("/api/assistant/infomation")
    public React<StandardResult> infomation(@Model AssistantModel.Infomation request) {
        return React.emitter(() -> {
            AssistantCacheKey cacheKey = new AssistantCacheKey(request.getAid(), request.getId());
            Where where = Where.builder(AssistantStruct.Info.AID, Where.EQ, request.getAid())
                    .and(AssistantStruct.Info.ID, Where.EQ, request.getId());
            Query query = Query.builder(where).limit(0, 1);
            Entity data = assistantSource.select(cacheKey, query, AssistantStruct.Protocol.getInfoAssistantSchema());
            if (data == null) {
                return StandardResult.error(Errno.ENOT_FOUND);
            }
            Param infomation = handleDataFormat(data);
            return StandardResult.success(infomation);
        });
    }

    // 查询智能体列表
    @Request.Read("/api/assistant/list")
    public React<StandardResult> list(@Model AssistantModel.List request) {
        return React.emitter(() -> {
            CacheKey<?> cacheKey = NoCacheKey.getInstance();
            Where where = Where.builder(AssistantStruct.Info.AID, Where.EQ, request.getAid());
            Query query = Query.builder(where);
            query.orderBy(AssistantStruct.Info.CREATED_AT);
            List<Entity> result = assistantSource.query(cacheKey, query, AssistantStruct.Protocol.getInfoAssistantSchema());
            if (result == null) {
                return StandardResult.error(Errno.ENOT_FOUND);
            }
            Table<Param> dataList = Table.builder();
            for (Entity data : result) {
                Param infomation = handleDataFormat(data);
                dataList.add(infomation);
            }
            return StandardResult.success(dataList);
        });
    }

    // 新增智能体
    @Request.Read("/api/assistant/add")
    public React<StandardResult> add(@Model AssistantModel.Add request) throws Exception {
        Param nodeData = nodeApi.handleDataLoadById(request.getAid(), request.getNodeId());
        if (nodeData == null) {
            return React.just(StandardResult.error(Errno.ENOT_FOUND));
        }
        String host = nodeData.getString(NodeStruct.Info.ADDRESS);
        int port = nodeData.getInt(NodeStruct.Info.PORT);
        String signature = nodeData.getString(NodeStruct.Info.SIGNATURE);
        long assistantId = idWorker.nextId();
        String remoteEndpoint = String.format("http://%s:%d/api/worker/initial", host, port);
        FormEntity form = FormEntity.builder(FormEntity.FORM_ENCTYPE_JSON)
                .add("id", assistantId);
        OkRequest okRequest = OkRequest.builder().header("authorization", signature).url(remoteEndpoint).post(form);
        return okHttp.execute(okRequest).handle(response -> {
            return StandardResult.parseHttpParamResult(response.getContent());
        }).map(result -> {
            AssistantCacheKey cacheKey = new AssistantCacheKey(request.getAid(), assistantId);
            Calendar currentTime = Calendar.getInstance();
            Entity infomation = Entity.builder(AssistantStruct.Info.ID, assistantId);
            infomation.setLong(AssistantStruct.Info.AID, request.getAid())
                    .setString(AssistantStruct.Info.NAME, request.getName())
                    .setString(AssistantStruct.Info.NODE_ID, request.getNodeId())
                    .setInt(AssistantStruct.Info.STATUS, 0)
                    .setInt(AssistantStruct.Info.MODE, request.getMode())
                    .setString(AssistantStruct.Info.REMARK, request.getRemark())
                    .setCalendar(AssistantStruct.Info.CREATED_AT, currentTime);
            int count = assistantSource.put(cacheKey, infomation, AssistantStruct.Protocol.getInfoAssistantSchema());
            if (count < 0) {
                return StandardResult.error(Errno.ERROR);
            }
            Param response = Param.builder().setLong(AssistantStruct.Info.ID, assistantId);
            Logger.info("AssistantApi.add: add assistant success, aid=%s, id=%s", request.getAid(), assistantId);
            return StandardResult.success(response);
        });
    }

    // 更新智能体信息
    @Request.Read("/api/assistant/update")
    public React<StandardResult> update(@Model AssistantModel.Update request) {
        return React.emitter(() -> {
            AssistantCacheKey cacheKey = new AssistantCacheKey(request.getAid(), request.getId());
            Updater updater = Updater.builder();
            if (request.getName() != null) {
                updater.add(AssistantStruct.Info.NAME, request.getName());
            }
            if (request.getNodeId() != null) {
                updater.add(AssistantStruct.Info.NODE_ID, request.getNodeId());
            }
            if (request.getMode() >= 0) {
                updater.add(AssistantStruct.Info.MODE, request.getMode());
            }
            if (request.getRemark() != null) {
                updater.add(AssistantStruct.Info.REMARK, request.getRemark());
            }
            updater.where(AssistantStruct.Info.AID, Where.EQ, request.getAid())
                    .and(AssistantStruct.Info.ID, Where.EQ, request.getId());
            int count = assistantSource.update(cacheKey, updater, AssistantStruct.Protocol.getInfoAssistantSchema());
            if (count < 0) {
                return StandardResult.error(Errno.ERROR);
            }
            Param response = Param.builder().setLong(AssistantStruct.Info.ID, request.getId());
            return StandardResult.success(response);
        });
    }

    // 删除智能体
    @Request.Read("/api/assistant/delete")
    public React<StandardResult> delete(@Model AssistantModel.Delete request) throws Exception {
        return React.emitter(() -> {
            // 查询智能体信息获取所属节点
            CacheKey<?> cacheKey = new AssistantCacheKey(request.getAid(), request.getId());
            Where where = Where.builder(AssistantStruct.Info.AID, Where.EQ, request.getAid())
                    .and(AssistantStruct.Info.ID, Where.EQ, request.getId());
            Query query = Query.builder(where).limit(0, 1);
            Entity data = assistantSource.select(cacheKey, query, AssistantStruct.Protocol.getInfoAssistantSchema());
            if (data == null) {
                return StandardResult.error(Errno.ENOT_FOUND);
            }
            return StandardResult.success(data);
        }).request((IoFunction<StandardResult, React<OkResponse>>) standardResult -> {
            // 删除远程节点上的智能体实例
            Param nodeInfo = standardResult.getParamResult();
            Param nodeData = nodeApi.handleDataLoadById(request.getAid(), nodeInfo.getString("node_id"));
            String host = nodeData.getString(NodeStruct.Info.ADDRESS);
            int port = nodeData.getInt(NodeStruct.Info.PORT);
            String signature = nodeData.getString(NodeStruct.Info.SIGNATURE);
            String remoteEndpoint = String.format("http://%s:%d/api/worker/remove", host, port);
            FormEntity form = FormEntity.builder(FormEntity.FORM_ENCTYPE_JSON)
                    .add("id", request.getId());
            OkRequest okRequest = OkRequest.builder().header("authorization", signature).url(remoteEndpoint).post(form);
            return okHttp.execute(okRequest);
        }).handle(response -> {
            return StandardResult.parseHttpParamResult(response.getContent());
        }).map(resp -> {
            // 删除数据库记录
            CacheKey<?> cacheKey = new AssistantCacheKey(request.getAid(), request.getId());
            Where where = Where.builder(AssistantStruct.Info.AID, Where.EQ, request.getAid())
                    .and(AssistantStruct.Info.ID, Where.EQ, request.getId());
            int count = assistantSource.delete(cacheKey, where);
            if (count < 0) {
                return StandardResult.error(Errno.ERROR);
            }
            Param response = Param.builder().setLong(AssistantStruct.Info.ID, request.getId());
            Logger.info("Assistant deleted successfully: aid=%s, id=%s", request.getAid(), request.getId());
            return StandardResult.success(response);
        });
    }

    private Param handleDataFormat(Entity data) {
        Param infomation = Param.builder(data,
                AssistantStruct.Info.AID,
                AssistantStruct.Info.ID,
                AssistantStruct.Info.NAME,
                AssistantStruct.Info.NODE_ID,
                AssistantStruct.Info.STATUS,
                AssistantStruct.Info.MODE,
                AssistantStruct.Info.REMARK
        );
        Calendar createTime = data.getCalendar(AssistantStruct.Info.CREATED_AT);
        infomation.setLong(CommonStruct.Info.DATE, createTime.getTimeInMillis());
        return infomation;
    }
}
