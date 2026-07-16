package cloud.apposs.robot.gateway.loader;

import cloud.apposs.cachex.*;
import cloud.apposs.cachex.database.Entity;
import cloud.apposs.cachex.database.Query;
import cloud.apposs.cachex.database.Updater;
import cloud.apposs.cachex.database.Where;
import cloud.apposs.protobuf.ProtoSchema;
import cloud.apposs.robot.gateway.loader.AssistantLoader.AssistantCacheKey;
import cloud.apposs.robot.gateway.struct.AssistantStruct;
import cloud.apposs.util.Ref;
import cloud.apposs.util.Table;

public class AssistantLoader extends CacheLoaderAdapter<AssistantCacheKey, Entity> {
    @Override
    public Entity select(CacheKey<?> key, Query query, ProtoSchema schema, DBTemplate template, Object... args) throws Exception {
        Entity infomation = template.select(AssistantStruct.Table.TABLE_ASSISTANT, query);
        if (infomation == null) {
            return EntityCacheX.DATA_NOT_FOUND;
        }
        return infomation;
    }

    @Override
    public int add(AssistantCacheKey key, Entity value, ProtoSchema schema, DBTemplate template, Ref<Object> idRef, Object... args) throws Exception {
        return template.insert(AssistantStruct.Table.TABLE_ASSISTANT, value, idRef);
    }

    @Override
    public Table<Entity> query(CacheKey<?> key, Query query, ProtoSchema schema, DBTemplate template, Object[] args) throws Exception {
        return template.query(AssistantStruct.Table.TABLE_ASSISTANT, query);
    }

    @Override
    public int update(CacheKey<?> key, Updater updater, ProtoSchema schema, DBTemplate template, Object... args) throws Exception {
        return template.update(AssistantStruct.Table.TABLE_ASSISTANT, updater);
    }

    @Override
    public int delete(CacheKey<?> key, Where where, DBTemplate template, Object... args) throws Exception {
        return template.delete(AssistantStruct.Table.TABLE_ASSISTANT, where);
    }

    public static final class AssistantCacheKey extends AbstractCacheKey<Long>{
        private static final String CACHE_KEY_PREFIX = "Assistant-";

        private long aid;

        public AssistantCacheKey(long aid, long id) {
            super(id, CACHE_KEY_PREFIX);
            this.aid = aid;
        }

        @Override
        public String getCacheKey() {
            return prefix + aid + "-" + primary;
        }
    }
}
