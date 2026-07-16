package cloud.apposs.robot.gateway.struct;

import cloud.apposs.protobuf.ProtoSchema;

public final class ProviderStruct {
    public static final class Table {
        public static final String TABLE_PROVIDER = "provider";
    }

    public static final class Info {
        public static final String AID = "aid";
        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String TYPE = "type";
        public static final String SYSTEM = "is_system";
        public static final String URL = "url";
        public static final String STATUS = "status";
        public static final String MODELS = "models";
        public static final String API_KEYS = "api_keys";
    }

    public static final class Protocol {
        private static final ProtoSchema INFO_PROVIDER_SCHEMA = ProtoSchema.mapSchema();
        static {
            INFO_PROVIDER_SCHEMA.addKey(Info.AID, Long.class);
            INFO_PROVIDER_SCHEMA.addKey(Info.ID, Long.class);
            INFO_PROVIDER_SCHEMA.addKey(Info.NAME, String.class);
            INFO_PROVIDER_SCHEMA.addKey(Info.TYPE, String.class);
            INFO_PROVIDER_SCHEMA.addKey(Info.SYSTEM, Integer.class);
            INFO_PROVIDER_SCHEMA.addKey(Info.URL, String.class);
            INFO_PROVIDER_SCHEMA.addKey(Info.STATUS, Integer.class);
            INFO_PROVIDER_SCHEMA.addKey(Info.MODELS, String.class);
            INFO_PROVIDER_SCHEMA.addKey(Info.API_KEYS, String.class);
        }

        public static ProtoSchema getInfoProviderSchema() {
            return INFO_PROVIDER_SCHEMA;
        }
    }

    public static final class Status {
        public static final int IS_ON = 1;
        public static final int IS_OFF = 0;

        public static boolean isOn(int status) {
            return status == IS_ON;
        }
    }
}
