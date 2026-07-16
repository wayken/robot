package cloud.apposs.robot.gateway.struct;

import cloud.apposs.protobuf.ProtoSchema;

import java.util.Calendar;

public final class AssistantStruct {
    public static final class Table {
        public static final String TABLE_ASSISTANT = "assistant";
    }

    public static final class Info {
        public static final String AID = "aid";
        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String NODE_ID = "node_id";
        public static final String STATUS = "status";
        public static final String MODE = "mode";
        public static final String REMARK = "remark";
        public static final String CREATED_AT = "created_at";
    }

    public static final class Protocol {
        private static final ProtoSchema INFO_ASSISTANT_SCHEMA = ProtoSchema.mapSchema();
        static {
            INFO_ASSISTANT_SCHEMA.addKey(Info.AID, Long.class);
            INFO_ASSISTANT_SCHEMA.addKey(Info.ID, Long.class);
            INFO_ASSISTANT_SCHEMA.addKey(Info.NAME, String.class);
            INFO_ASSISTANT_SCHEMA.addKey(Info.NODE_ID, String.class);
            INFO_ASSISTANT_SCHEMA.addKey(Info.STATUS, Integer.class);
            INFO_ASSISTANT_SCHEMA.addKey(Info.MODE, Integer.class);
            INFO_ASSISTANT_SCHEMA.addKey(Info.REMARK, String.class);
            INFO_ASSISTANT_SCHEMA.addKey(Info.CREATED_AT, Calendar.class);
        }
        public static ProtoSchema getInfoAssistantSchema() {
            return INFO_ASSISTANT_SCHEMA;
        }
    }
}
