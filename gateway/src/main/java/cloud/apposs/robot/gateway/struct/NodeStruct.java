package cloud.apposs.robot.gateway.struct;

import cloud.apposs.protobuf.ProtoSchema;

import java.util.Calendar;

public final class NodeStruct {
    public static final class Table {
        public static final String TABLE_NODE = "node";
    }

    public static final class Info {
        public static final String AID = "aid";
        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String ADDRESS = "address";
        public static final String PORT = "port";
        public static final String HOSTNAME = "hostname";
        public static final String OS = "os";
        public static final String VERSION = "version";
        public static final String STATUS = "status";
        public static final String SIGNATURE = "signature";
        public static final String REGION = "region";
        public static final String REMARK = "remark";
        public static final String CREATED_AT = "created_at";
    }

    public static final class Protocol {
        private static final ProtoSchema INFO_NODE_SCHEMA = ProtoSchema.mapSchema();
        static {
            INFO_NODE_SCHEMA.addKey(Info.AID, Long.class);
            INFO_NODE_SCHEMA.addKey(Info.ID, Long.class);
            INFO_NODE_SCHEMA.addKey(Info.NAME, String.class);
            INFO_NODE_SCHEMA.addKey(Info.ADDRESS, String.class);
            INFO_NODE_SCHEMA.addKey(Info.PORT, Integer.class);
            INFO_NODE_SCHEMA.addKey(Info.HOSTNAME, String.class);
            INFO_NODE_SCHEMA.addKey(Info.OS, String.class);
            INFO_NODE_SCHEMA.addKey(Info.VERSION, String.class);
            INFO_NODE_SCHEMA.addKey(Info.SIGNATURE, String.class);
            INFO_NODE_SCHEMA.addKey(Info.REGION, String.class);
            INFO_NODE_SCHEMA.addKey(Info.REMARK, String.class);
            INFO_NODE_SCHEMA.addKey(Info.CREATED_AT, Calendar.class);
        }

        public static ProtoSchema getInfoNodeSchema() {
            return INFO_NODE_SCHEMA;
        }
    }

    public static final class Status {
        public static final int OFFLINE = 0;
        public static final int ONLINE = 1;

        public static boolean isOnline(int status) {
            return status == ONLINE;
        }
    }
}
