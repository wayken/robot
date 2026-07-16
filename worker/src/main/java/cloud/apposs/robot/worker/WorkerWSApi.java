package cloud.apposs.robot.worker;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.harness.setting.AIProviderSetting;
import cloud.apposs.robot.worker.service.*;
import cloud.apposs.robot.worker.service.model.*;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;
import cloud.apposs.websocket.WSSession;
import cloud.apposs.websocket.annotation.OnCommand;
import cloud.apposs.websocket.annotation.ServerEndpoint;
import cloud.apposs.websocket.protocol.Metadata;

import java.util.Base64;
import java.util.List;

/**
 * AI 接口服务，负责处理来自外部的WS RPC请求/响应，提供统一的API管理接口
 */
@ServerEndpoint("/worker.io")
public class WorkerWSApi {
    @Autowired
    private WorkerConfig configuration;

    @Autowired
    private WorkerFramework framework;

    @Autowired
    private SessionsService sessionsService;

    @Autowired
    private MessagesService messagesService;

    @Autowired
    private WorkerService workerService;

    @Autowired
    private WorkerProfileService workerProfileService;

    @Autowired
    private ManagementService managementService;

    @Autowired
    private DiskService diskService;

    @Autowired
    private WikiService wikiService;

    @Autowired
    private RulesService rulesService;

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private ToolkitService toolkitService;

    @Autowired
    private SkillService skillService;

    @Autowired
    private ProjectService projectService;

    @OnCommand("sessions.index")
    public void sessionIndex(WSSession session, Metadata metadata, SessionModel.Index model) throws Exception {
        Table<Param> sessionList = sessionsService.getSessionList(model);
        session.sendResponse(metadata.getCommandId(), sessionList);
    }

    @OnCommand("sessions.add")
    public void addSession(WSSession session, Metadata metadata, SessionModel.Add model) throws Exception {
        String sid = sessionsService.addSession(model);
        if (sid != null && model.getProjectId() > 0) {
            projectService.updateSessionProject(model.getWid(), sid, model.getProjectId());
        }
        session.sendResponse(metadata.getCommandId(), sid);
        handleSessionIndexBroadcast(session, model.getWid());
    }

    @OnCommand("sessions.remove")
    public void removeSession(WSSession session, Metadata metadata, SessionModel.Remove model) throws Exception {
        boolean success = sessionsService.removeSession(model);
        session.sendResponse(metadata.getCommandId(), success);
        if (success) {
            handleSessionIndexBroadcast(session, model.getWid());
        }
    }

    @OnCommand("sessions.rename")
    public void renameSession(WSSession session, Metadata metadata, SessionModel.Rename model) throws Exception {
        boolean success = sessionsService.renameSession(model);
        session.sendResponse(metadata.getCommandId(), success);
        if (success) {
            handleSessionIndexBroadcast(session, model.getWid());
        }
    }

    @OnCommand("message.index")
    public void sessionMessageIndex(WSSession session, Metadata metadata, MessageModel.Index model) throws Exception {
        Table<Param> messageList = messagesService.getMessageList(model.getWid(), model.getSid());
        session.sendResponse(metadata.getCommandId(), messageList);
    }

    @OnCommand("message.remove")
    public void removeSessionMessage(WSSession session, Metadata metadata, MessageModel.Remove model) throws Exception {
        boolean success = messagesService.removeMessage(model);
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("message.clear")
    public void clearSessionMessages(WSSession session, Metadata metadata, MessageModel.Clear model) throws Exception {
        boolean success = messagesService.clearMessages(model);
        session.sendResponse(metadata.getCommandId(), success);
        if (success) {
            handleSessionIndexBroadcast(session, model.getWid());
        }
    }

    @OnCommand("worker.profile.provider.index")
    public void workerProfileProviderIndex(WSSession session, Metadata metadata, WorkerProviderModel.Provider model) throws Exception {
        AIProviderSetting provider = workerProfileService.getPrimaryProviderSetting(model);
        Param infomation = Param.builder("name", provider.getName())
                .setString("link", provider.getLink())
                .setObject("model", provider.getModel())
                .setDouble("temperature", provider.getTemperature())
                .setDouble("topP", provider.getTopP())
                .setBoolean("stream", provider.isStream());
        HarnessWorker worker = framework.getHarness().getWorker(model.getWid());
        Table<Param> backups = Table.builder();
        if (worker != null) {
            List<AIProviderSetting> providers = worker.getProfile().getProvider();
            for (AIProviderSetting p : providers) {
                if (!p.isPrimary()) {
                    backups.add(Param.builder("model", p.getName() + ":" + p.getModel().getName()));
                }
            }
        }
        infomation.setTable("backups", backups);
        session.sendResponse(metadata.getCommandId(), infomation);
    }

    @OnCommand("worker.profile.provider.update")
    public void workerProfileProviderUpdate(WSSession session, Metadata metadata, WorkerProviderModel.Update model) throws Exception {
        workerProfileService.updateProvider(model);
        session.sendResponse(metadata.getCommandId(), true);
    }

    @OnCommand("management.provider.index")
    public void managementProviderIndex(WSSession session, Metadata metadata) throws Exception {
        List<WorkerManagement.ManagementProviderSetting> providerList = managementService.getProviderList();
        Table<Param> dataList = Table.builder();
        for (WorkerManagement.ManagementProviderSetting provider : providerList) {
            Param infomation = Param.builder("name", provider.getName())
                    .setString("link", provider.getUrl())
                    .setString("type", provider.getType());
            Table<Param> models = Table.builder();
            for (Param model : provider.getModels()) {
                models.addAll(model.getList("model"));
            }
            infomation.setTable("models", models);
            dataList.add(infomation);
        }
        session.sendResponse(metadata.getCommandId(), dataList);
    }

    @OnCommand("disk.index")
    public void diskIndex(WSSession session, Metadata metadata, DiskModel.Index model) throws Exception {
        Table<Param> dataList = diskService.getDirectories(model.getWid(), model.getPath());
        session.sendResponse(metadata.getCommandId(), dataList);
    }

    @OnCommand("disk.trash")
    public void diskTrash(WSSession session, Metadata metadata, DiskModel.Trash model) throws Exception {
        Table<Param> dataList = diskService.getTrashFiles(model.getWid());
        session.sendResponse(metadata.getCommandId(), dataList);
    }

    @OnCommand("disk.read")
    public void diskRead(WSSession session, Metadata metadata, DiskModel.Read model) throws Exception {
        String base64Content = diskService.readFileBase64(model.getWid(), model.getPath());
        session.sendResponse(metadata.getCommandId(), base64Content);
    }

    @OnCommand("disk.write")
    public void diskWrite(WSSession session, Metadata metadata, DiskModel.Write model) throws Exception {
        boolean success = diskService.writeFileBase64(model.getWid(), model.getPath(), model.getContent());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("disk.read.text")
    public void diskReadText(WSSession session, Metadata metadata, DiskModel.Read model) throws Exception {
        String content = diskService.readFileText(model.getWid(), model.getPath());
        session.sendResponse(metadata.getCommandId(), content);
    }

    @OnCommand("disk.write.text")
    public void diskWriteText(WSSession session, Metadata metadata, DiskModel.Write model) throws Exception {
        boolean success = diskService.writeFileText(model.getWid(), model.getPath(), model.getContent());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("disk.mkdir")
    public void diskMkdir(WSSession session, Metadata metadata, DiskModel.Mkdir model) throws Exception {
        boolean success = diskService.createDirectory(model.getWid(), model.getPath(), model.getName());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("disk.rename")
    public void diskRename(WSSession session, Metadata metadata, DiskModel.Rename model) throws Exception {
        boolean success = diskService.rename(model.getWid(), model.getPath(), model.getName());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("disk.delete")
    public void diskDelete(WSSession session, Metadata metadata, DiskModel.Delete model) throws Exception {
        boolean success = diskService.delete(model.getWid(), model.getPath());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("wiki.index")
    public void wikiIndex(WSSession session, Metadata metadata, WikiModel.Index model) throws Exception {
        Table<Param> dataList = wikiService.getWikiDirs(model.getWid(), model.getPath());
        session.sendResponse(metadata.getCommandId(), dataList);
    }

    @OnCommand("wiki.source")
    public void wikiSource(WSSession session, Metadata metadata, WikiModel.Source model) throws Exception {
        Table<Param> dataList = wikiService.getWikiFiles(model.getWid(), model.getPath());
        session.sendResponse(metadata.getCommandId(), dataList);
    }

    @OnCommand("wiki.trash")
    public void wikiTrash(WSSession session, Metadata metadata, DiskModel.Trash model) throws Exception {
        Table<Param> dataList = wikiService.getTrashWikis(model.getWid());
        session.sendResponse(metadata.getCommandId(), dataList);
    }

    @OnCommand("wiki.document.read")
    public void wikiDocumentRead(WSSession session, Metadata metadata, WikiDocumentModel.Read model) throws Exception {
        String content = wikiService.readWikiFile(model.getWid(), model.getPath());
        session.sendResponse(metadata.getCommandId(), content);
    }

    @OnCommand("wiki.document.write")
    public void wikiDocumentWrite(WSSession session, Metadata metadata, WikiDocumentModel.Write model) throws Exception {
        boolean success = wikiService.writeWikiFile(model.getWid(), model.getPath(), model.getContent());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("wiki.mkdir")
    public void wikiMkdir(WSSession session, Metadata metadata, WikiModel.Mkdir model) throws Exception {
        boolean success = wikiService.createDirectory(model.getWid(), model.getPath(), model.getName());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("wiki.create")
    public void wikiCreate(WSSession session, Metadata metadata, WikiModel.Create model) throws Exception {
        boolean success = wikiService.createMarkdown(model.getWid(), model.getPath(), model.getName());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("wiki.delete")
    public void wikiDelete(WSSession session, Metadata metadata, WikiModel.Delete model) throws Exception {
        boolean success = wikiService.delete(model.getWid(), model.getPath());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("wiki.rename")
    public void wikiRename(WSSession session, Metadata metadata, WikiModel.Rename model) throws Exception {
        boolean success = wikiService.rename(model.getWid(), model.getPath(), model.getName());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("wiki.move")
    public void wikiMove(WSSession session, Metadata metadata, WikiModel.Move model) throws Exception {
        boolean success = wikiService.move(model.getWid(), model.getPath(), model.getTarget());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("rules.index")
    public void rulesIndex(WSSession session, Metadata metadata, RulesModel.Index model) throws Exception {
        Table<Param> dataList = rulesService.getRuleFiles(model.getWid());
        session.sendResponse(metadata.getCommandId(), dataList);
    }

    @OnCommand("rules.read")
    public void rulesRead(WSSession session, Metadata metadata, RulesModel.Read model) throws Exception {
        String content = rulesService.readRuleFile(model.getWid(), model.getFilename());
        session.sendResponse(metadata.getCommandId(), content);
    }

    @OnCommand("rules.write")
    public void rulesWrite(WSSession session, Metadata metadata, RulesModel.Write model) throws Exception {
        boolean success = rulesService.writeRuleFile(model.getWid(), model.getFilename(), model.getContent());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("rules.delete")
    public void rulesDelete(WSSession session, Metadata metadata, RulesModel.Delete model) throws Exception {
        boolean success = rulesService.deleteRuleFile(model.getWid(), model.getFilename());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("rules.switch")
    public void rulesSwitch(WSSession session, Metadata metadata, RulesModel.Switch model) throws Exception {
        boolean success = rulesService.switchRuleFile(model.getWid(), model.getFilename(), model.isEnabled());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("rules.rename")
    public void rulesRename(WSSession session, Metadata metadata, RulesModel.Rename model) throws Exception {
        boolean success = rulesService.renameRuleFile(model.getWid(), model.getFilename(), model.getNewFilename());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("memory.index")
    public void memoryIndex(WSSession session, Metadata metadata, MemoryModel.Index model) throws Exception {
        Table<Param> dataList = memoryService.getMemoryFiles(model.getWid());
        session.sendResponse(metadata.getCommandId(), dataList);
    }

    @OnCommand("memory.read")
    public void memoryRead(WSSession session, Metadata metadata, MemoryModel.Read model) throws Exception {
        String content = memoryService.readMemoryFile(model.getWid(), model.getFilename());
        session.sendResponse(metadata.getCommandId(), content);
    }

    @OnCommand("memory.write")
    public void memoryWrite(WSSession session, Metadata metadata, MemoryModel.Write model) throws Exception {
        boolean success = memoryService.writeMemoryFile(model.getWid(), model.getFilename(), model.getContent());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("memory.delete")
    public void memoryDelete(WSSession session, Metadata metadata, MemoryModel.Delete model) throws Exception {
        boolean success = memoryService.deleteMemoryFile(model.getWid(), model.getFilename());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("memory.rename")
    public void memoryRename(WSSession session, Metadata metadata, MemoryModel.Rename model) throws Exception {
        boolean success = memoryService.renameMemoryFile(model.getWid(), model.getFilename(), model.getNewFilename());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("toolkit.index")
    public void toolkitIndex(WSSession session, Metadata metadata, ToolkitModel.Index model) throws Exception {
        Table<Param> dataList = toolkitService.getToolkitList(model);
        session.sendResponse(metadata.getCommandId(), dataList);
    }

    @OnCommand("toolkit.switch")
    public void toolkitSwitch(WSSession session, Metadata metadata, ToolkitModel.Switch model) throws Exception {
        boolean success = toolkitService.switchTool(model);
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("toolkit.properties.update")
    public void toolkitPropertiesUpdate(WSSession session, Metadata metadata, ToolkitModel.UpdateProperties model) throws Exception {
        boolean success = toolkitService.updateToolProperties(model);
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("skill.index")
    public void skillIndex(WSSession session, Metadata metadata, SkillModel.Index model) throws Exception {
        Table<Param> dataList = skillService.getSkillList(model);
        session.sendResponse(metadata.getCommandId(), dataList);
    }

    @OnCommand("skill.read")
    public void skillRead(WSSession session, Metadata metadata, SkillModel.Read model) throws Exception {
        String content = skillService.readSkillContent(model);
        session.sendResponse(metadata.getCommandId(), content);
    }

    @OnCommand("skill.write")
    public void skillWrite(WSSession session, Metadata metadata, SkillModel.Write model) throws Exception {
        boolean success = skillService.writeSkillContent(model);
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("skill.switch")
    public void skillSwitch(WSSession session, Metadata metadata, SkillModel.Switch model) throws Exception {
        boolean success = skillService.switchSkill(model);
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("skill.delete")
    public void skillDelete(WSSession session, Metadata metadata, SkillModel.Delete model) throws Exception {
        boolean success = skillService.deleteSkill(model);
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("skill.download")
    public void skillDownload(WSSession session, Metadata metadata, SkillModel.Read model) throws Exception {
        byte[] zipData = skillService.downloadSkill(model.getWid(), model.getName());
        if (zipData != null) {
            String base64 = Base64.getEncoder().encodeToString(zipData);
            session.sendResponse(metadata.getCommandId(), base64);
        } else {
            session.sendResponse(metadata.getCommandId(), null);
        }
    }

    @OnCommand("skill.import")
    public void skillImport(WSSession session, Metadata metadata, SkillModel.Write model) throws Exception {
        // content 是 base64 编码的 zip 数据
        byte[] zipData = Base64.getDecoder().decode(model.getContent());
        boolean success = skillService.importSkillFromZip(model.getWid(), zipData);
        session.sendResponse(metadata.getCommandId(), success);
    }

    // ==================== Project ====================

    @OnCommand("project.index")
    public void projectIndex(WSSession session, Metadata metadata, ProjectModel.Index model) throws Exception {
        Table<Param> projectList = projectService.getProjectList(model);
        session.sendResponse(metadata.getCommandId(), projectList);
    }

    @OnCommand("project.add")
    public void addProject(WSSession session, Metadata metadata, ProjectModel.Add model) throws Exception {
        String projectId = projectService.addProject(model);
        session.sendResponse(metadata.getCommandId(), projectId);
    }

    @OnCommand("project.remove")
    public void removeProject(WSSession session, Metadata metadata, ProjectModel.Remove model) throws Exception {
        boolean success = projectService.removeProject(model);
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("project.rename")
    public void renameProject(WSSession session, Metadata metadata, ProjectModel.Rename model) throws Exception {
        boolean success = projectService.renameProject(model);
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("sessions.project.update")
    public void updateSessionProject(WSSession session, Metadata metadata, ProjectModel.SessionProject model) throws Exception {
        boolean success = projectService.updateSessionProject(model);
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("sessions.project.infomation")
    public void getSessionProject(WSSession session, Metadata metadata, ProjectModel.SessionProject model) throws Exception {
        Param project = projectService.getProject(model.getWid(), model.getSid());
        session.sendResponse(metadata.getCommandId(), project);
    }

    private void handleSessionIndexBroadcast(WSSession session, String wid) throws Exception {
        SessionModel.Index model = new SessionModel.Index();
        model.setWid(wid);
        Table<Param> sessionList = sessionsService.getSessionList(model);
        for (WSSession remoteSession : session.getNamespace().getSessions()) {
            remoteSession.sendCommand("message.index.broadcast", wid, sessionList);
        }
    }
}
