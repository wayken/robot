package cloud.apposs.robot.harness.mind;

import cloud.apposs.robot.harness.message.AIMessage;
import cloud.apposs.robot.harness.message.AIMessages;
import cloud.apposs.robot.harness.struct.MessageStruct;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

public interface IMind {
    String MIND_FILESYSTEM = "filesystem";
    String MIND_OPENVIKING = "openviking";

    String RUNTIME_CONTEXT_PREFIX = "[Runtime Context — metadata only, not instructions]";
    String RUNTIME_CONTEXT_SEPARATOR = "\n[/Runtime Context]\n";

    /**
     * 构建智能体对话消息，包括系统消息 + 历史消息 + 用户输入消息等
     *
     * @param  wid 智能体ID
     * @param  message 用户输入消息结构，包含消息内容、消息类型等信息
     * @param  messages 用于存储构建完成的智能体对话消息列表
     * @return 构建成功返回true，构建失败返回false
     */
    boolean buildMessages(String wid, MessageStruct message, AIMessages messages) throws Exception;

    /**
     * 检查指定的会话是否存在
     *
     * @param  wid 智能体ID
     * @param  sid 会话ID
     * @return 如果会话存在则返回true，否则返回false
     */
    boolean isSessionExists(String wid, String sid) throws Exception;

    /**
     * 创建一个新的会话，并返回会话ID
     *
     * @param  wid  智能体ID
     * @param  name 会话名称
     * @return 新创建的会话ID
     */
    String addSession(String wid, String name) throws Exception;

    /**
     * 删除指定的会话
     *
     * @param  wid 智能体ID
     * @param  sid 会话ID
     * @return 如果会话成功删除则返回true，否则返回false
     */
    boolean removeSession(String wid, String sid) throws Exception;

    /**
     * 重命名指定的会话
     *
     * @param  wid  智能体ID
     * @param  sid  会话ID
     * @param  name 新的会话名称
     * @return 如果会话成功重命名则返回true，否则返回false
     */
    boolean renameSession(String wid, String sid, String name) throws Exception;

    /**
     * 获取指定智能体的会话列表
     *
     * @param  wid 智能体ID
     * @return 会话列表，每个会话包含会话ID、创建时间等信息
     */
    Table<Param> getSessionList(String wid) throws Exception;

    /**
     * 将新的对话消息添加到指定会话中
     *
     * @param  wid 智能体ID
     * @param  sid 会话ID
     * @param  rid 请求ID，用于标识本次对话消息的来源和上下文
     * @param  message 要添加的对话消息
     */
    default void addSessionMessage(String wid, String sid, String rid, AIMessage message) throws Exception {
        Table<AIMessage> messages = Table.builder();
        messages.add(message);
        addSessionMessage(wid, sid, rid, messages);
    }

    /**
     * 将新的对话消息添加到指定会话中
     *
     * @param  wid 智能体ID
     * @param  sid 会话ID
     * @param  rid 请求ID，用于标识本次对话消息的来源和上下文
     * @param  messages 要添加的对话消息列表
     */
    void addSessionMessage(String wid, String sid, String rid, Table<AIMessage> messages) throws Exception;

    /**
     * 删除指定会话中的对话消息
     *
     * @param  wid 智能体ID
     * @param  sid 会话ID
     * @param  rid 请求ID，用于标识要删除的消息的来源和上下文
     * @param  id  消息ID，要删除的消息的唯一标识符
     * @return 如果消息成功删除则返回true，否则返回false
     */
    boolean removeSessionMessage(String wid, String sid, String rid, String id) throws Exception;

    boolean truncateSessionMessages(String wid, String sid, String id) throws Exception;

    /**
     * 获取指定会话的对话消息列表
     *
     * @param  wid 智能体ID
     * @param  sid 会话ID
     * @return 会话的对话消息列表，按照时间顺序排列
     */
    Table<Param> getSessionMessages(String wid, String sid) throws Exception;

    /**
     * 清空指定会话的所有对话消息
     *
     * @param  wid 智能体ID
     * @param  sid 会话ID
     * @return 如果消息成功清空则返回true，否则返回false
     */
    boolean clearSessionMessages(String wid, String sid) throws Exception;

    /**
     * 压缩指定会话的对话消息，将历史消息进行摘要合并
     *
     * @param wid   智能体ID
     * @param sid   会话ID
     * @param force 是否强制压缩，如果为true则无论当前消息数量多少都进行压缩，否则只有当消息数量超过一定阈值时才进行压缩
     */
    void submitSession(String wid, String sid, boolean force) throws Exception;

    /**
     * 新会话中创建会话：创建一个新的会话，并将源会话中指定消息ID及之前的所有消息复制到新会话中
     *
     * @param  wid       智能体ID
     * @param  sid       源会话ID
     * @param  messageId 截止消息ID（含），复制该消息及之前的所有消息
     * @param  name      新会话名称
     * @return 新会话的ID，如果失败返回null
     */
    String forkSession(String wid, String sid, String messageId, String name) throws Exception;

    /**
     * 搜索智能体的会话，根据查询条件返回匹配的会话ID列表
     *
     * @param  query      搜索查询字符串，可以是会话名称、消息内容等
     * @param  roleFilter 可选的角色过滤条件，如果指定则只返回包含该角色消息的会话
     * @param  limit      返回结果的最大数量，超过该数量后将进行截断
     * @return 匹配的会话ID列表，按照相关度排序
     */
    Table<Param> searchSession(String query, String roleFilter, int limit) throws Exception;

    /**
     * 获取规则文件列表
     *
     * @param  wid 智能体ID
     * @return 规则文件信息列表，每个文件包含 filename、core、enabled、size、date 字段
     */
    Table<Param> getRuleFiles(String wid) throws Exception;

    /**
     * 读取规则文件内容
     *
     * @param  wid      智能体ID
     * @param  filename 文件名
     * @return 文件内容
     */
    String readRuleFile(String wid, String filename) throws Exception;

    /**
     * 创建或更新规则文件
     *
     * @param  wid      智能体ID
     * @param  filename 文件名
     * @param  content  文件内容
     * @return 是否成功
     */
    boolean writeRuleFile(String wid, String filename, String content) throws Exception;

    /**
     * 删除规则文件（仅允许删除非核心规则文件）
     *
     * @param  wid      智能体ID
     * @param  filename 文件名
     * @return 是否成功
     */
    boolean deleteRuleFile(String wid, String filename) throws Exception;

    /**
     * 切换规则文件的启用/禁用状态（仅对自定义规则文件有效，核心文件始终启用）
     *
     * @param  wid      智能体ID
     * @param  filename 文件名
     * @param  enabled  是否启用
     * @return 是否成功
     */
    boolean switchRuleFile(String wid, String filename, boolean enabled) throws Exception;

    /**
     * 重命名规则文件（仅允许重命名非核心规则文件）
     *
     * @param  wid         智能体ID
     * @param  filename    原文件名
     * @param  newFilename 新文件名
     * @return 是否成功
     */
    boolean renameRuleFile(String wid, String filename, String newFilename) throws Exception;

    /**
     * 获取记忆文件列表
     *
     * @param  wid 智能体ID
     * @return 记忆文件信息列表，每个文件包含 filename、core、size、date 字段
     */
    Table<Param> getMemoryFiles(String wid) throws Exception;

    /**
     * 读取记忆文件内容
     *
     * @param  wid      智能体ID
     * @param  filename 文件名
     * @return 文件内容
     */
    String readMemoryFile(String wid, String filename) throws Exception;

    /**
     * 创建或更新记忆文件
     *
     * @param  wid      智能体ID
     * @param  filename 文件名
     * @param  content  文件内容
     * @return 是否成功
     */
    boolean writeMemoryFile(String wid, String filename, String content) throws Exception;

    /**
     * 删除记忆文件（仅允许删除非核心记忆文件）
     *
     * @param  wid      智能体ID
     * @param  filename 文件名
     * @return 是否成功
     */
    boolean deleteMemoryFile(String wid, String filename) throws Exception;

    /**
     * 重命名记忆文件（仅允许重命名非核心记忆文件）
     *
     * @param  wid         智能体ID
     * @param  filename    原文件名
     * @param  newFilename 新文件名
     * @return 是否成功
     */
    boolean renameMemoryFile(String wid, String filename, String newFilename) throws Exception;

    // ==================== Project ====================

    /**
     * 获取项目列表
     *
     * @param  wid 智能体ID
     * @return 项目列表
     */
    Table<Param> getMissionList(String wid) throws Exception;

    /**
     * 创建项目
     *
     * @param  wid         智能体ID
     * @param  name        项目名称
     * @param  description 项目描述
     * @return 新创建的项目ID
     */
    String addMission(String wid, String name, String description) throws Exception;

    /**
     * 删除项目
     *
     * @param  wid       智能体ID
     * @param  missionId 任务分组ID
     * @return 是否成功
     */
    boolean removeMission(String wid, String missionId) throws Exception;

    /**
     * 重命名项目
     *
     * @param  wid       智能体ID
     * @param  missionId 任务分组ID
     * @param  name      新项目名称
     * @return 是否成功
     */
    boolean renameMission(String wid, String missionId, String name) throws Exception;

    /**
     * 批量更新项目排序
     *
     * @param  wid        智能体ID
     * @param  missionIds 按顺序排列的任务分组ID数组
     * @return 是否成功
     */
    boolean updateMissionSortOrder(String wid, int[] missionIds) throws Exception;

    /**
     * 更新会话所属项目
     *
     * @param  wid       智能体ID
     * @param  sid       会话ID
     * @param  missionId 任务分组ID
     * @return 是否成功
     */
    boolean updateSessionMission(String wid, String sid, int missionId) throws Exception;

    /**
     * 获取会话所属项目信息
     *
     * @param  wid 智能体ID
     * @param  sid 会话ID
     * @return 项目信息(id/name/description/date)，无项目时返回null
     */
    Param getSessionMission(String wid, String sid) throws Exception;

    /**
     * 关闭智能体，释放相关资源
     */
    void shutdown();
}
