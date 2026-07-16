package cloud.apposs.robot.worker.util;

import cloud.apposs.util.IdWorker;

/**
 * ID生成器，用于生成全局唯一ID
 */
public class Ids {
    public static final int DEFAULT_WORKER_ID = 1;
    public static final int DEFAULT_IDC_ID = 1;

    private final IdWorker idWorker;

    private static Ids instance = new Ids();

    public Ids() {
        this.idWorker = IdWorker.builder(DEFAULT_WORKER_ID, DEFAULT_IDC_ID);
    }

    public static Ids getInstance() {
        return instance;
    }

    public long nextId() {
        return idWorker.nextId();
    }
}
