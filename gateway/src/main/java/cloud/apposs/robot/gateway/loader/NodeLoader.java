package cloud.apposs.robot.gateway.loader;

import cloud.apposs.cachex.*;
import cloud.apposs.cachex.database.Entity;
import cloud.apposs.cachex.database.Query;
import cloud.apposs.cachex.database.Updater;
import cloud.apposs.cachex.database.Where;
import cloud.apposs.protobuf.ProtoSchema;
import cloud.apposs.robot.gateway.loader.NodeLoader.NodeCacheKey;
import cloud.apposs.robot.gateway.struct.NodeStruct;
import cloud.apposs.util.Ref;
import cloud.apposs.util.Table;

public class NodeLoader extends CacheLoaderAdapter<NodeCacheKey, Entity> {
    @Override
    public Entity select(CacheKey<?> key, Query query, ProtoSchema schema, DBTemplate template, Object... args) throws Exception {
        Entity infomation = template.select(NodeStruct.Table.TABLE_NODE, query);
        if (infomation == null) {
            return EntityCacheX.DATA_NOT_FOUND;
        }
        return infomation;
    }

    @Override
    public int add(NodeCacheKey key, Entity value, ProtoSchema schema, DBTemplate template, Ref<Object> idRef, Object... args) throws Exception {
        return template.insert(NodeStruct.Table.TABLE_NODE, value, idRef);
    }

    @Override
    public Table<Entity> query(CacheKey<?> key, Query query, ProtoSchema schema, DBTemplate template, Object[] args) throws Exception {
        return template.query(NodeStruct.Table.TABLE_NODE, query);
    }

    @Override
    public int update(CacheKey<?> key, Updater updater, ProtoSchema schema, DBTemplate template, Object... args) throws Exception {
        return template.update(NodeStruct.Table.TABLE_NODE, updater);
    }

    @Override
    public int delete(CacheKey<?> key, Where where, DBTemplate template, Object... args) throws Exception {
        return template.delete(NodeStruct.Table.TABLE_NODE, where);
    }

    public static final class NodeCacheKey extends AbstractCacheKey<Long>{
        private static final String CACHE_KEY_PREFIX = "Node-";

        private long aid;

        public NodeCacheKey(long aid, long id) {
            super(id, CACHE_KEY_PREFIX);
            this.aid = aid;
        }

        @Override
        public String getCacheKey() {
            return prefix + aid + "-" + primary;
        }
    }
}
