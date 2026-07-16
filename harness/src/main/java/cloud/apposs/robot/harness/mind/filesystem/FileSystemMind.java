package cloud.apposs.robot.harness.mind.filesystem;

import cloud.apposs.logger.Logger;
import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.harness.HarnessWorkspace;
import cloud.apposs.robot.harness.message.AIMessage;
import cloud.apposs.robot.harness.message.AIMessages;
import cloud.apposs.robot.harness.message.kind.AISystemMessage;
import cloud.apposs.robot.harness.message.kind.AIUserMessage;
import cloud.apposs.robot.harness.mind.IMind;
import cloud.apposs.robot.harness.provider.AIRequest;
import cloud.apposs.robot.harness.provider.AITool;
import cloud.apposs.robot.harness.skill.SkillInvokeParser;
import cloud.apposs.robot.harness.skill.SkillLoader;
import cloud.apposs.robot.harness.struct.MessageStruct;
import cloud.apposs.robot.harness.tool.ITool;
import cloud.apposs.robot.harness.util.PromptLoader;
import cloud.apposs.robot.harness.util.Strings;
import cloud.apposs.util.HttpStatus;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class FileSystemMind implements IMind {
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 智能体
    private final HarnessWorker worker;

    // 智能体工作空间
    private final HarnessWorkspace workspace;

    // 身份信息加载器
    private final IdentityFileLoader identityFileLoader;

    // 规则内容加载器
    private final RuleFileSystemLoader ruleFileSystemLoader;

    // 记忆内容加载器
    private final MemoryFileSystemLoader memoryFileSystemLoader;

    // 会话内容加载器
    private final SessionDatabaseLoader sessionDatabaseLoader;

    public FileSystemMind(HarnessWorker worker) throws Exception {
        this.worker = worker;
        this.workspace = worker.getWorkspace();
        this.identityFileLoader = new IdentityFileLoader(this.workspace);
        this.ruleFileSystemLoader = new RuleFileSystemLoader(this.workspace);
        this.memoryFileSystemLoader = new MemoryFileSystemLoader(this.workspace);
        this.sessionDatabaseLoader = new SessionDatabaseLoader(this.workspace);
    }

    /**
     * 构建智能体对话消息，构造步骤如下:
     * <pre>
     *     1. 加载智能体身份信息、规则、记忆、技能等系统消息
     *     2. 加载历史会话消息
     *     3. 构建用户消息，如果用户消息中包含 $skill-name 格式的技能调用引用，
     *        且对应技能存在，则在用户消息中追加指令提示大模型调用工具读取技能文件，由 LLM 主动获取技能内容后按照技能指引执行任务
     * </pre>
     *
     * @param  wid 智能体ID
     * @param  message 用户输入消息结构，包含消息内容、消息类型等信息
     * @return 构建完成的智能体对话消息列表
     */
    @Override
    public boolean buildMessages(String wid, MessageStruct message, AIMessages messages) throws Exception {
        messages.append(buildSystemMessage(message.getSid()));
        AIMessages historyMessages = buildHistoryMessages(message.getSid());
        messages.append(historyMessages);
        AIMessage userMessage = buildUserParsedMessage(message.getMessage());
        messages.append(userMessage, true);
        return true;
    }

    @Override
    public boolean isSessionExists(String wid, String sid) {
        return sessionDatabaseLoader.isSessionExists(wid, sid);
    }

    @Override
    public String addSession(String wid, String name) throws Exception {
        return sessionDatabaseLoader.addSession(wid, name);
    }

    @Override
    public boolean removeSession(String wid, String sid) throws Exception {
        return sessionDatabaseLoader.removeSession(wid, sid);
    }

    @Override
    public boolean renameSession(String wid, String sid, String name) throws Exception {
        return sessionDatabaseLoader.renameSession(wid, sid, name);
    }

    @Override
    public Table<Param> getSessionList(String wid) throws Exception {
        return sessionDatabaseLoader.getSessionList(wid);
    }

    @Override
    public void addSessionMessage(String wid, String sid, String rid, Table<AIMessage> messages) throws Exception {
        sessionDatabaseLoader.addSessionMessage(wid, sid, rid, messages);
    }

    @Override
    public boolean removeSessionMessage(String wid, String sid, String rid, String id) throws Exception {
        return sessionDatabaseLoader.removeSessionMessage(wid, sid, rid, id);
    }

    @Override
    public Table<Param> getSessionMessages(String wid, String sid) throws Exception {
        return sessionDatabaseLoader.getSessionMessages(wid, sid);
    }

    @Override
    public boolean clearSessionMessages(String wid, String sid) throws Exception {
        sessionDatabaseLoader.clearSessionMessages(wid, sid);
        // 重置会话的巩固索引
        sessionDatabaseLoader.updateConsolidated(wid, sid, 0);
        return true;
    }

    @Override
    public void submitSession(String wid, String sid, boolean force) throws Exception {
        Table<Param> sessionMessages = sessionDatabaseLoader.getSessionMessages(wid, sid);
        if (sessionMessages == null) {
            return;
        }
        Param sessionInfo = sessionDatabaseLoader.getSessionInfo(wid, sid);
        int consolidate = sessionInfo.getInt("consolidate", 0);
        int memoryWindow = worker.getProfile().getMemoryWindow();
        if (!force) {
            // 未达到窗口阈值时不触发巩固
            if (sessionMessages.size() - consolidate <= memoryWindow) {
                return;
            }
        }
        // 只取从上次巩固位置起的前半个窗口的消息进行本次巩固
        int keepCount = memoryWindow / 2;
        int endIndex = Math.min(consolidate + keepCount, sessionMessages.size());
        Table<Param> consolidateMessages = Table.builder();
        for (int i = consolidate; i < endIndex; i++) {
            consolidateMessages.add(sessionMessages.get(i));
        }
        handleSessionMessagesConsolidate(wid, sid, consolidateMessages, endIndex);
    }

    @Override
    public Table<Param> searchSession(String query, String roleFilter, int limit) throws Exception {
        return sessionDatabaseLoader.searchSession(query, roleFilter, limit);
    }

    @Override
    public Table<Param> getRuleFiles(String wid) throws Exception {
        return ruleFileSystemLoader.getRuleFiles();
    }

    @Override
    public String readRuleFile(String wid, String filename) throws Exception {
        return ruleFileSystemLoader.readRuleFile(filename);
    }

    @Override
    public boolean writeRuleFile(String wid, String filename, String content) throws Exception {
        return ruleFileSystemLoader.writeRuleFile(filename, content);
    }

    @Override
    public boolean deleteRuleFile(String wid, String filename) throws Exception {
        return ruleFileSystemLoader.deleteRuleFile(filename);
    }

    @Override
    public boolean switchRuleFile(String wid, String filename, boolean enabled) throws Exception {
        return ruleFileSystemLoader.switchRuleFile(filename, enabled);
    }

    @Override
    public boolean renameRuleFile(String wid, String filename, String newFilename) throws Exception {
        return ruleFileSystemLoader.renameRuleFile(filename, newFilename);
    }

    @Override
    public Table<Param> getMemoryFiles(String wid) throws Exception {
        return memoryFileSystemLoader.getMemoryFiles();
    }

    @Override
    public String readMemoryFile(String wid, String filename) throws Exception {
        return memoryFileSystemLoader.readMemoryFile(filename);
    }

    @Override
    public boolean writeMemoryFile(String wid, String filename, String content) throws Exception {
        return memoryFileSystemLoader.writeMemoryFile(filename, content);
    }

    @Override
    public boolean deleteMemoryFile(String wid, String filename) throws Exception {
        return memoryFileSystemLoader.deleteMemoryFile(filename);
    }

    @Override
    public boolean renameMemoryFile(String wid, String filename, String newFilename) throws Exception {
        return memoryFileSystemLoader.renameMemoryFile(filename, newFilename);
    }

    @Override
    public Table<Param> getProjectList(String wid) throws Exception {
        return sessionDatabaseLoader.getProjectList(wid);
    }

    @Override
    public String addProject(String wid, String name, String description) throws Exception {
        return sessionDatabaseLoader.addProject(wid, name, description);
    }

    @Override
    public String addProject(String wid, String name, String description, String path) throws Exception {
        return sessionDatabaseLoader.addProject(wid, name, description, path);
    }

    @Override
    public boolean removeProject(String wid, String projectId) throws Exception {
        return sessionDatabaseLoader.removeProject(wid, projectId);
    }

    @Override
    public boolean renameProject(String wid, String projectId, String name) throws Exception {
        return sessionDatabaseLoader.renameProject(wid, projectId, name);
    }

    @Override
    public boolean updateProjectSortOrder(String wid, int[] projectIds) throws Exception {
        return sessionDatabaseLoader.updateProjectSortOrder(wid, projectIds);
    }

    @Override
    public boolean updateSessionProject(String wid, String sid, int projectId) throws Exception {
        return sessionDatabaseLoader.updateSessionProject(wid, sid, projectId);
    }

    @Override
    public Param getSessionProject(String wid, String sid) throws Exception {
        return sessionDatabaseLoader.getSessionProject(wid, sid);
    }

    @Override
    public void shutdown() {
        sessionDatabaseLoader.close();
    }

    /**
     * 构建系统提示词：整合身份、引导文件、记忆和技能
     *
     * @return 系统提示词消息
     */
    private AIMessage buildSystemMessage(String sid) throws Exception {
        Table<String> messages = Table.builder();
        // 根据会话关联的项目确定工作空间路径
        String projectPath = null;
        if (sid != null) {
            Param project = sessionDatabaseLoader.getSessionProject(worker.getId(), sid);
            if (project != null) {
                projectPath = project.getString("path");
                if (projectPath != null && projectPath.isEmpty()) {
                    projectPath = null;
                }
            }
        }
        // 加载身份信息
        messages.add(identityFileLoader.buildPrompt(projectPath));
        // 加载规则文件
        messages.add(ruleFileSystemLoader.buildPrompt());
        // 加载记忆文件
        messages.add(memoryFileSystemLoader.buildPrompt());
        // 加载技能列表
        messages.add(worker.getSkill().buildPrompt());
        return new AISystemMessage(String.join("---\n\n", messages));
    }

    /**
     * 构建历史消息：从会话数据库加载历史消息，包含用户和助手的对话记录
     *
     * @return 历史消息列表
     */
    private AIMessages buildHistoryMessages(String sid) throws Exception {
        AIMessages messages = new AIMessages();
        Param sessionInfo = sessionDatabaseLoader.getSessionInfo(worker.getId(), sid);
        int consolidate = sessionInfo.getInt("consolidate", 0);
        Table<Param> sessionMessages = sessionDatabaseLoader.getSessionMessages(worker.getId(), sid);
        if (sessionMessages == null || sessionMessages.isEmpty()) {
            return messages;
        }
        // 只加载从上次巩固位置之后的消息，避免重复加载之前已经巩固过的消息
        for (int i = consolidate; i < sessionMessages.size(); i++) {
            Param messageInfo = sessionMessages.get(i);
            AIMessage message = AIMessages.serialize(messageInfo.getParam("message"));
            messages.append(message);
        }
        return messages;
    }

    /**
     * 构建用户消息，构造流程如下：
     * <pre>
     *     1. 解析用户输入的消息，判断是否包含技能调用，如果包含则构建技能调用提示，否则直接构建用户消息
     * </pre>
     *
     * @param  message 用户输入的消息
     * @return 用户消息对象
     */
    private AIMessage buildUserParsedMessage(String message) throws Exception {
        SkillInvokeParser invokeParser = SkillInvokeParser.parse(message);
        if (!invokeParser.isMatched()) {
            return buildUserRuntimeMessage(message, null);
        }
        String skillHint = null;
        List<String> skillNames = invokeParser.getSkillNames();
        SkillLoader skillLoader = worker.getSkill().getSkillLoader();
        StringBuilder skillHintBuilder = new StringBuilder();
        for (String skillName : skillNames) {
            String skillContent = skillLoader.readSkillContent(skillName);
            if (Strings.isBlank(skillContent)) {
                continue;
            }
            String skillFilePath = skillLoader.getSkillPath().resolve(skillName).resolve("SKILL.md").toString();
            String hint = PromptLoader.readPrompt("skill/hint", Param.builder("skillName", skillName)
                    .setString("skillFilePath", skillFilePath));
            if (hint != null) {
                if (skillHintBuilder.length() > 0) {
                    skillHintBuilder.append("\n\n");
                }
                skillHintBuilder.append(hint);
            }
        }
        if (skillHintBuilder.length() > 0) {
            skillHint = skillHintBuilder.toString();
        }
        // 使用去掉 $技能名称 标签后的清理文本
        String userContent = invokeParser.getMessage();
        return buildUserRuntimeMessage(userContent, skillHint);
    }

    /**
     * 构建用户消息：包含当前时间和时区等运行时上下文信息，
     *
     * @param  content 用户输入的消息内容
     * @param  hints   扩展调用提示信息，可为空，不会展现在前端用户消息列表中
     * @return 包含运行时上下文信息的用户消息
     */
    private AIMessage buildUserRuntimeMessage(String content, String... hints) {
        ZonedDateTime now = ZonedDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm (EEEE)");
        String nowStr = now.format(dateTimeFormatter);
        DateTimeFormatter dateZoneFormatter = DateTimeFormatter.ofPattern("z");
        String dateZoneStr = now.format(dateZoneFormatter);
        if (Strings.isBlank(dateZoneStr)) {
            dateZoneStr = "UTC";
        }
        String runtimeContext = String.format("%s\nCurrent Time: %s (%s)", RUNTIME_CONTEXT_PREFIX, nowStr, dateZoneStr);
        if (hints != null) {
            String formatedHints = Strings.join(hints, "\n");
            runtimeContext += "\n" + formatedHints;
        }
        content = runtimeContext + RUNTIME_CONTEXT_SEPARATOR + content;
        return new AIUserMessage(content);
    }

    private Table<String> handleSessionMessageSubmitFormat(Table<Param> messages) throws Exception {
        Table<String> result = Table.builder();
        for (Param message : messages) {
            String role = message.getString("role");
            String content = message.getString("content");
            if (Strings.isBlank(role) || Strings.isBlank(content)) {
                continue;
            }
            long timestamp = message.getLong("timestamp");
            // 将timestamp转换为日期时间字符串即2024-06-01 14:30:00格式
            String timeStr = dateTimeFormatter.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()));
            String line = String.format("[%s] %s: %s", timeStr, role.toUpperCase(), content);
            result.add(line);
        }
        return result;
    }

    private void handleSessionMessagesConsolidate(String wid, String sid, Table<Param> sessionMessages, int newConsolidate) throws Exception {
        Table<String> formatedMessages = handleSessionMessageSubmitFormat(sessionMessages);
        Param replacement = Param.builder("memory", memoryFileSystemLoader.buildPrompt())
                .setString("conversation", Strings.join(formatedMessages, "\n"));
        String userPrompt = PromptLoader.readPrompt("memory/consolidate", replacement);
        AIMessages messages = new AIMessages();
        String systemPrompt = "You are a memory consolidation agent. Call the save_memory tool with your consolidation of the conversation.";
        messages.append(new AISystemMessage(systemPrompt));
        messages.append(new AIUserMessage(userPrompt));
        MemoryTool memoryTool = new MemoryTool(memoryFileSystemLoader);
        Table<ITool> tools = Table.builder();
        tools.add(memoryTool);
        String rid = UUID.randomUUID().toString();
        AIRequest request = AIRequest.of(worker.getId(), sid, rid, messages, tools);
        worker.completions(request).subscribe(response -> {
            if (response.getStatus() != HttpStatus.HTTP_STATUS_200.getCode()) {
                Logger.error("Failed to consolidate memory for session " + sid + ": " + response.getContent());
            }
            if (!response.isFinished() || !response.hasToolCall()) {
                return;
            }
            Table<AITool> responseTools = response.getTools();
            AITool toolCall = responseTools.get(0);
            if (!MemoryTool.NAME.equals(toolCall.getName())) {
                return;
            }
            // 调用记忆工具，保存记忆内容，并更新当前会话巩固的记忆位置索引，以便下次提交会话时从该位置开始获取新的会话消息进行巩固
            Param arguments = JsonUtil.parseJsonParam(toolCall.getArguments());
            memoryTool.run(worker.getId().toString(), sid, rid, arguments, null);
            try {
                sessionDatabaseLoader.updateConsolidated(wid, sid, newConsolidate);
            } catch (Exception e) {
                Logger.error("Failed to update consolidated index for session " + sid + ": " + e.getMessage());
            }
        }).start();
    }
}
