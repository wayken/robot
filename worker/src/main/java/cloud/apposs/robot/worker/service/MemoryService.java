package cloud.apposs.robot.worker.service;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.robot.harness.HarnessMind;
import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.worker.WorkerFramework;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

/**
 * 记忆文件管理服务，通过 IMind 接口操作记忆文件，
 * 核心文件为 MEMORY.md 和 HISTORY.md，不允许删除和重命名
 */
@Component
public class MemoryService {
    @Autowired
    private WorkerFramework framework;

    /**
     * 获取记忆文件列表
     *
     * @param  workerId 智能体ID
     * @return 记忆文件信息列表，每个文件包含 filename、core、size、date
     */
    public Table<Param> getMemoryFiles(String workerId) throws Exception {
        HarnessMind mind = getMind(workerId);
        if (mind == null) {
            return Table.builder();
        }
        return mind.getMemoryFiles(workerId);
    }

    /**
     * 读取记忆文件内容
     *
     * @param  workerId 智能体ID
     * @param  filename 文件名
     * @return 文件内容
     */
    public String readMemoryFile(String workerId, String filename) throws Exception {
        HarnessMind mind = getMind(workerId);
        if (mind == null) {
            return "";
        }
        return mind.readMemoryFile(workerId, filename);
    }

    /**
     * 创建或更新记忆文件
     *
     * @param  workerId 智能体ID
     * @param  filename 文件名
     * @param  content  文件内容
     * @return 是否成功
     */
    public boolean writeMemoryFile(String workerId, String filename, String content) throws Exception {
        HarnessMind mind = getMind(workerId);
        if (mind == null) {
            return false;
        }
        return mind.writeMemoryFile(workerId, filename, content);
    }

    /**
     * 删除记忆文件（仅允许删除非核心记忆文件）
     *
     * @param  workerId 智能体ID
     * @param  filename 文件名
     * @return 是否成功
     */
    public boolean deleteMemoryFile(String workerId, String filename) throws Exception {
        HarnessMind mind = getMind(workerId);
        if (mind == null) {
            return false;
        }
        return mind.deleteMemoryFile(workerId, filename);
    }

    /**
     * 重命名记忆文件（仅允许重命名非核心记忆文件）
     *
     * @param  workerId    智能体ID
     * @param  filename    原文件名
     * @param  newFilename 新文件名
     * @return 是否成功
     */
    public boolean renameMemoryFile(String workerId, String filename, String newFilename) throws Exception {
        HarnessMind mind = getMind(workerId);
        if (mind == null) {
            return false;
        }
        return mind.renameMemoryFile(workerId, filename, newFilename);
    }

    private HarnessMind getMind(String workerId) {
        HarnessWorker worker = framework.getHarness().getWorker(workerId);
        if (worker == null) {
            return null;
        }
        return worker.getMind();
    }
}
