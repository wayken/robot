package cloud.apposs.robot.gateway.loader;

import cloud.apposs.cachex.*;
import cloud.apposs.cachex.database.Entity;
import cloud.apposs.cachex.database.Query;
import cloud.apposs.cachex.database.Updater;
import cloud.apposs.cachex.database.Where;
import cloud.apposs.protobuf.ProtoSchema;
import cloud.apposs.robot.gateway.loader.ProviderLoader.ProviderCacheKey;
import cloud.apposs.robot.gateway.struct.ProviderStruct;
import cloud.apposs.util.Ref;
import cloud.apposs.util.Table;

public class ProviderLoader extends CacheLoaderAdapter<ProviderCacheKey, Entity> {
    @Override
    public Entity select(CacheKey<?> key, Query query, ProtoSchema schema, DBTemplate template, Object... args) throws Exception {
        Entity infomation = template.select(ProviderStruct.Table.TABLE_PROVIDER, query);
        if (infomation == null) {
            return EntityCacheX.DATA_NOT_FOUND;
        }
        return infomation;
    }

    @Override
    public int add(ProviderCacheKey key, Entity value, ProtoSchema schema, DBTemplate template, Ref<Object> idRef, Object... args) throws Exception {
        return template.insert(ProviderStruct.Table.TABLE_PROVIDER, value, idRef);
    }

    @Override
    public Table<Entity> query(CacheKey<?> key, Query query, ProtoSchema schema, DBTemplate template, Object[] args) throws Exception {
        return template.query(ProviderStruct.Table.TABLE_PROVIDER, query);
    }

    @Override
    public int update(CacheKey<?> key, Updater updater, ProtoSchema schema, DBTemplate template, Object... args) throws Exception {
        return template.update(ProviderStruct.Table.TABLE_PROVIDER, updater);
    }

    @Override
    public int delete(CacheKey<?> key, Where where, DBTemplate template, Object... args) throws Exception {
        return template.delete(ProviderStruct.Table.TABLE_PROVIDER, where);
    }

    public static final class ProviderCacheKey extends AbstractCacheKey<Long> {
        private static final String CACHE_KEY_PREFIX = "Provider-";

        private long aid;

        public ProviderCacheKey(long aid, long id) {
            super(id, CACHE_KEY_PREFIX);
            this.aid = aid;
        }

        @Override
        public String getCacheKey() {
            return prefix + aid + "-" + primary;
        }
    }
}
