package cloud.apposs.robot.worker.service;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.robot.harness.HarnessMind;
import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.worker.WorkerFramework;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

/**
 * 规则文件管理服务，通过 IMind 接口操作规则文件，
 * 支持不同的底层实现（文件系统、openviking 等）
 */
@Component
public class RulesService {
    @Autowired
    private WorkerFramework framework;

    /**
     * 获取规则文件列表
     *
     * @param  workerId 智能体ID
     * @return 规则文件信息列表，每个文件包含 filename、core、enabled、size、date
     */
    public Table<Param> getRuleFiles(String workerId) throws Exception {
        HarnessMind mind = getMind(workerId);
        if (mind == null) {
            return Table.builder();
        }
        return mind.getRuleFiles(workerId);
    }

    /**
     * 读取规则文件内容
     *
     * @param  workerId 智能体ID
     * @param  filename 文件名
     * @return 文件内容
     */
    public String readRuleFile(String workerId, String filename) throws Exception {
        HarnessMind mind = getMind(workerId);
        if (mind == null) {
            return "";
        }
        return mind.readRuleFile(workerId, filename);
    }

    /**
     * 创建或更新规则文件
     *
     * @param  workerId 智能体ID
     * @param  filename 文件名
     * @param  content  文件内容
     * @return 是否成功
     */
    public boolean writeRuleFile(String workerId, String filename, String content) throws Exception {
        HarnessMind mind = getMind(workerId);
        if (mind == null) {
            return false;
        }
        return mind.writeRuleFile(workerId, filename, content);
    }

    /**
     * 删除规则文件（仅允许删除非核心规则文件）
     *
     * @param  workerId 智能体ID
     * @param  filename 文件名
     * @return 是否成功
     */
    public boolean deleteRuleFile(String workerId, String filename) throws Exception {
        HarnessMind mind = getMind(workerId);
        if (mind == null) {
            return false;
        }
        return mind.deleteRuleFile(workerId, filename);
    }

    /**
     * 切换规则文件的启用/禁用状态（仅对自定义规则文件有效，核心文件始终启用）
     *
     * @param  workerId 智能体ID
     * @param  filename 文件名
     * @param  enabled  是否启用
     * @return 是否成功
     */
    public boolean switchRuleFile(String workerId, String filename, boolean enabled) throws Exception {
        HarnessMind mind = getMind(workerId);
        if (mind == null) {
            return false;
        }
        return mind.switchRuleFile(workerId, filename, enabled);
    }

    /**
     * 重命名规则文件（仅允许重命名非核心规则文件）
     *
     * @param  workerId    智能体ID
     * @param  filename    原文件名
     * @param  newFilename 新文件名
     * @return 是否成功
     */
    public boolean renameRuleFile(String workerId, String filename, String newFilename) throws Exception {
        HarnessMind mind = getMind(workerId);
        if (mind == null) {
            return false;
        }
        return mind.renameRuleFile(workerId, filename, newFilename);
    }

    private HarnessMind getMind(String workerId) {
        HarnessWorker worker = framework.getHarness().getWorker(workerId);
        if (worker == null) {
            return null;
        }
        return worker.getMind();
    }
}
