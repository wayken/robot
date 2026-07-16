package cloud.apposs.robot.gateway.api;

import cloud.apposs.bootor.cachex.DataSource;
import cloud.apposs.cachex.CacheKey;
import cloud.apposs.cachex.CacheXConfig;
import cloud.apposs.cachex.NoCacheKey;
import cloud.apposs.cachex.database.Entity;
import cloud.apposs.cachex.database.Query;
import cloud.apposs.cachex.database.Updater;
import cloud.apposs.cachex.database.Where;
import cloud.apposs.logger.Logger;
import cloud.apposs.okhttp.FormEntity;
import cloud.apposs.okhttp.HttpBuilder;
import cloud.apposs.okhttp.OkHttp;
import cloud.apposs.okhttp.OkRequest;
import cloud.apposs.react.React;
import cloud.apposs.react.actor.Actor;
import cloud.apposs.rest.annotation.Executor;
import cloud.apposs.rest.annotation.Model;
import cloud.apposs.rest.annotation.Request;
import cloud.apposs.rest.annotation.RestAction;
import cloud.apposs.robot.gateway.GatewayConfig;
import cloud.apposs.robot.gateway.loader.NodeLoader;
import cloud.apposs.robot.gateway.loader.NodeLoader.NodeCacheKey;
import cloud.apposs.robot.gateway.model.NodeModel;
import cloud.apposs.robot.gateway.struct.CommonStruct;
import cloud.apposs.robot.gateway.struct.NodeStruct;
import cloud.apposs.util.*;

import java.util.Calendar;
import java.util.List;

@Executor
@RestAction
public class NodeApi {
    public static final int DEFAULT_WORKER_ID = 1;
    public static final int DEFAULT_IDC_ID = 1;

    private final DataSource<NodeCacheKey> nodeSource;

    private final IdWorker idWorker;

    private final Actor actor;

    private final GatewayConfig config;

    private final OkHttp okHttp;

    public NodeApi(GatewayConfig config) throws Exception {
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
        this.nodeSource = new DataSource<>(cacheXConfig, new NodeLoader());
        this.actor = new Actor("Node-Actor-");
        this.okHttp = HttpBuilder.builder().socketTimeout(120 * 1000).retryCount(3).retrySleepTime(2000).build();
    }

    @Request.Read("/api/node/list")
    public React<StandardResult> list(@Model NodeModel.List request) {
        return React.emitter(() -> {
            Table<Param> dataList = handleDataListLoad(request.getAid());
            return StandardResult.success(dataList);
        });
    }

    @Request.Read("/api/node/infomation")
    public React<StandardResult> infomation(@Model NodeModel.Information request) {
        return React.emitter(() -> {
            Param infomation = handleDataLoadById(request.getAid(), request.getId());
            if (infomation == null) {
                return StandardResult.error(Errno.ENOT_FOUND);
            }
            return StandardResult.success(infomation);
        });
    }

    @Request.Read("/api/node/add")
    public React<StandardResult> add(@Model NodeModel.Add request) throws Exception {
        long nodeId = idWorker.nextId();
        FormEntity form = FormEntity.builder(FormEntity.FORM_ENCTYPE_JSON).add("id", nodeId);
        String remoteEndpoint = String.format("http://%s:%d/api/worker/system", request.getAddress(), request.getPort());
        OkRequest okRequest = OkRequest.builder().header("authorization", request.getSignature()).url(remoteEndpoint).post(form);
        return okHttp.execute(okRequest).handle(response -> {
            return StandardResult.parseHttpParamResult(response.getContent());
        }).map(result -> {
            Calendar currentTime = Calendar.getInstance();
            Param nodeData = result.getParamResult();
            NodeCacheKey cacheKey = new NodeCacheKey(request.getAid(), nodeId);
            Entity infomation = Entity.builder(NodeStruct.Info.ID, nodeId);
            String hostname = nodeData.getString("hostname");
            infomation.setLong(NodeStruct.Info.AID, request.getAid())
                    .setString(NodeStruct.Info.NAME, request.getName())
                    .setString(NodeStruct.Info.ADDRESS, request.getAddress())
                    .setInt(NodeStruct.Info.PORT, request.getPort())
                    .setString(NodeStruct.Info.HOSTNAME, hostname)
                    .setString(NodeStruct.Info.OS, nodeData.getString("os"))
                    .setString(NodeStruct.Info.SIGNATURE, request.getSignature())
                    .setCalendar(NodeStruct.Info.CREATED_AT, currentTime);
            int count = nodeSource.put(cacheKey, infomation, NodeStruct.Protocol.getInfoNodeSchema());
            if (count < 0) {
                return StandardResult.error(Errno.ERROR);
            }
            Param response = Param.builder().setLong(NodeStruct.Info.ID, nodeId)
                    .setString(NodeStruct.Info.ADDRESS, request.getAddress())
                    .setInt(NodeStruct.Info.PORT, request.getPort())
                    .setString(NodeStruct.Info.HOSTNAME, nodeData.getString("hostname"))
                    .setString(NodeStruct.Info.OS, nodeData.getString("os"));
            Logger.info("Node added successfully: aid=%s, id=%s, address=%s, port=%d, hostname=%s", request.getAid(), nodeId, request.getAddress(), request.getPort(), hostname);
            return StandardResult.success(response);
        });
    }

    // 更新节点信息
    @Request.Read("/api/node/update")
    public React<StandardResult> update(@Model NodeModel.Update request) {
        return React.emitter(() -> {
            NodeCacheKey cacheKey = new NodeCacheKey(request.getAid(), request.getId());
            Updater updater = Updater.builder()
                    .add(NodeStruct.Info.NAME, request.getName())
                    .add(NodeStruct.Info.ADDRESS, request.getAddress())
                    .add(NodeStruct.Info.PORT, request.getPort())
                    .add(NodeStruct.Info.SIGNATURE, request.getSignature())
                    .add(NodeStruct.Info.REMARK, request.getRemark());
            updater.where(NodeStruct.Info.AID, Where.EQ, request.getAid())
                    .and(NodeStruct.Info.ID, Where.EQ, request.getId());
            int count = nodeSource.update(cacheKey, updater, NodeStruct.Protocol.getInfoNodeSchema());
            if (count < 0) {
                return StandardResult.error(Errno.ERROR);
            }
            Param response = Param.builder().setLong(NodeStruct.Info.ID, request.getId());
            Logger.info("Node updated successfully: aid=%s, id=%s", request.getAid(), request.getId());
            return StandardResult.success(response);
        });
    }

    // 删除节点
    @Request.Read("/api/node/delete")
    public React<StandardResult> delete(@Model NodeModel.Delete request) {
        return React.emitter(() -> {
            CacheKey<?> cacheKey = new NodeCacheKey(request.getAid(), request.getId());
            Where where = Where.builder(NodeStruct.Info.AID, Where.EQ, request.getAid())
                    .and(NodeStruct.Info.ID, Where.EQ, request.getId());
            int count = nodeSource.delete(cacheKey, where);
            if (count < 0) {
                return StandardResult.error(Errno.ERROR);
            }
            Param response = Param.builder().setLong(NodeStruct.Info.ID, request.getId());
            Logger.info("Node deleted successfully: aid=%s, id=%s", request.getAid(), request.getId());
            return StandardResult.success(response);
        });
    }

    public Param handleDataLoadById(long aid, String id) throws Exception {
        NodeCacheKey cacheKey = new NodeCacheKey(aid, Parser.parseLong(id));
        Where where = Where.builder(NodeStruct.Info.AID, Where.EQ, aid)
                .and(NodeStruct.Info.ID, Where.EQ, id);
        Query query = Query.builder(where).limit(0, 1);
        Entity data = nodeSource.select(cacheKey, query, NodeStruct.Protocol.getInfoNodeSchema());
        if (data == null) {
            return null;
        }
        return handleDataFormat(data);
    }

    public Table<Param> handleDataListLoad(long aid) throws Exception {
        CacheKey<?> cacheKey = NoCacheKey.getInstance();
        Where where = Where.builder(NodeStruct.Info.AID, Where.EQ, aid);
        Query query = Query.builder(where);
        query.orderBy(NodeStruct.Info.CREATED_AT);
        Table<Param> dataList = Table.builder();
        List<Entity> result = nodeSource.query(cacheKey, query, NodeStruct.Protocol.getInfoNodeSchema());
        if (result == null) {
            return dataList;
        }
        for (Entity data : result) {
            Param infomation = handleDataFormat(data);
            dataList.add(infomation);
        }
        return dataList;
    }

    private Param handleDataFormat(Entity data) {
        Param infomation = Param.builder(data,
                NodeStruct.Info.ID,
                NodeStruct.Info.AID,
                NodeStruct.Info.NAME,
                NodeStruct.Info.ADDRESS,
                NodeStruct.Info.PORT,
                NodeStruct.Info.HOSTNAME,
                NodeStruct.Info.OS,
                NodeStruct.Info.VERSION,
                NodeStruct.Info.SIGNATURE,
                NodeStruct.Info.REMARK
        );
        infomation.setInt(NodeStruct.Info.STATUS, 0);
        Calendar createTime = data.getCalendar(NodeStruct.Info.CREATED_AT);
        infomation.setLong(CommonStruct.Info.DATE, createTime.getTimeInMillis());
        return infomation;
    }
}
