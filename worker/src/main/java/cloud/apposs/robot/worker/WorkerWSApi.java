package cloud.apposs.robot.worker;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.harness.setting.AIProviderSetting;
import cloud.apposs.robot.worker.message.WorkerMessageHook;
import cloud.apposs.robot.worker.message.WorkerSecurityPolicy;
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
    private MissionService missionService;

    @OnCommand("sessions.index")
    public void sessionIndex(WSSession session, Metadata metadata, SessionModel.Index request) throws Exception {
        Table<Param> sessionList = sessionsService.getSessionList(request);
        session.sendResponse(metadata.getCommandId(), sessionList);
    }

    @OnCommand("sessions.add")
    public void addSession(WSSession session, Metadata metadata, SessionModel.Add request) throws Exception {
        String sid = sessionsService.addSession(request);
        if (sid != null && request.getMissionId() > 0) {
            missionService.updateSessionMission(request.getWid(), sid, request.getMissionId());
        }
        session.sendResponse(metadata.getCommandId(), sid);
        handleSessionIndexBroadcast(session, request.getWid());
    }

    @OnCommand("sessions.remove")
    public void removeSession(WSSession session, Metadata metadata, SessionModel.Remove request) throws Exception {
        boolean success = sessionsService.removeSession(request);
        session.sendResponse(metadata.getCommandId(), success);
        if (success) {
            handleSessionIndexBroadcast(session, request.getWid());
        }
    }

    @OnCommand("sessions.rename")
    public void renameSession(WSSession session, Metadata metadata, SessionModel.Rename request) throws Exception {
        boolean success = sessionsService.renameSession(request);
        session.sendResponse(metadata.getCommandId(), success);
        if (success) {
            handleSessionIndexBroadcast(session, request.getWid());
        }
    }

    @OnCommand("sessions.fork")
    public void forkSession(WSSession session, Metadata metadata, SessionModel.Fork request) throws Exception {
        String newSid = sessionsService.forkSession(request);
        session.sendResponse(metadata.getCommandId(), newSid);
        if (newSid != null) {
            handleSessionIndexBroadcast(session, request.getWid());
        }
    }

    @OnCommand("message.index")
    public void sessionMessageIndex(WSSession session, Metadata metadata, MessageModel.Index request) throws Exception {
        Table<Param> messageList = messagesService.getMessageList(request.getWid(), request.getSid());
        for (Param approval : WorkerMessageHook.pending(request.getSid())) {
            messageList.add(approval);
        }
        session.sendResponse(metadata.getCommandId(), messageList);
    }

    @OnCommand("security.permission.index")
    public void securityPermissionIndex(WSSession session, Metadata metadata) throws Exception {
        session.sendResponse(metadata.getCommandId(), WorkerSecurityPolicy.toParam());
    }

    @OnCommand("security.permission.update")
    public void securityPermissionUpdate(WSSession session, Metadata metadata, MessageModel.SecurityPermission request) throws Exception {
        String permission = WorkerSecurityPolicy.setPermission(request.getPermission());
        Param infomation = WorkerSecurityPolicy.toParam();
        session.sendResponse(metadata.getCommandId(), infomation);
        handleSecurityPermissionBroadcast(session, infomation);
        if (WorkerSecurityPolicy.FULL_ACCESS.equals(permission)) {
            for (Param approval : WorkerMessageHook.onApprovePending(true)) {
                handleMessageResponseBroadcast(session, approval);
            }
        }
    }

    @OnCommand("message.remove")
    public void removeSessionMessage(WSSession session, Metadata metadata, MessageModel.Remove request) throws Exception {
        boolean success = messagesService.removeMessage(request);
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("message.clear")
    public void clearSessionMessages(WSSession session, Metadata metadata, MessageModel.Clear request) throws Exception {
        boolean success = messagesService.clearMessages(request);
        session.sendResponse(metadata.getCommandId(), success);
        if (success) {
            handleSessionIndexBroadcast(session, request.getWid());
        }
    }

    @OnCommand("worker.profile.provider.index")
    public void workerProfileProviderIndex(WSSession session, Metadata metadata, WorkerProviderModel.Provider request) throws Exception {
        AIProviderSetting provider = workerProfileService.getPrimaryProviderSetting(request);
        Param infomation = Param.builder("name", provider.getName())
                .setString("link", provider.getLink())
                .setObject("model", provider.getModel())
                .setDouble("temperature", provider.getTemperature())
                .setDouble("topP", provider.getTopP())
                .setBoolean("stream", provider.isStream());
        HarnessWorker worker = framework.getHarness().getWorker(request.getWid());
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
    public void workerProfileProviderUpdate(WSSession session, Metadata metadata, WorkerProviderModel.Update request) throws Exception {
        workerProfileService.updateProvider(request);
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
    public void diskIndex(WSSession session, Metadata metadata, DiskModel.Index request) throws Exception {
        Table<Param> dataList = diskService.getDirectories(request.getWid(), request.getPath());
        session.sendResponse(metadata.getCommandId(), dataList);
    }

    @OnCommand("disk.trash")
    public void diskTrash(WSSession session, Metadata metadata, DiskModel.Trash request) throws Exception {
        Table<Param> dataList = diskService.getTrashFiles(request.getWid());
        session.sendResponse(metadata.getCommandId(), dataList);
    }

    @OnCommand("disk.read")
    public void diskRead(WSSession session, Metadata metadata, DiskModel.Read request) throws Exception {
        String base64Content = diskService.readFileBase64(request.getWid(), request.getPath());
        session.sendResponse(metadata.getCommandId(), base64Content);
    }

    @OnCommand("disk.write")
    public void diskWrite(WSSession session, Metadata metadata, DiskModel.Write request) throws Exception {
        boolean success = diskService.writeFileBase64(request.getWid(), request.getPath(), request.getContent());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("disk.read.text")
    public void diskReadText(WSSession session, Metadata metadata, DiskModel.Read request) throws Exception {
        String content = diskService.readFileText(request.getWid(), request.getPath());
        session.sendResponse(metadata.getCommandId(), content);
    }

    @OnCommand("disk.write.text")
    public void diskWriteText(WSSession session, Metadata metadata, DiskModel.Write request) throws Exception {
        boolean success = diskService.writeFileText(request.getWid(), request.getPath(), request.getContent());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("disk.mkdir")
    public void diskMkdir(WSSession session, Metadata metadata, DiskModel.Mkdir request) throws Exception {
        boolean success = diskService.createDirectory(request.getWid(), request.getPath(), request.getName());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("disk.rename")
    public void diskRename(WSSession session, Metadata metadata, DiskModel.Rename request) throws Exception {
        boolean success = diskService.rename(request.getWid(), request.getPath(), request.getName());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("disk.delete")
    public void diskDelete(WSSession session, Metadata metadata, DiskModel.Delete request) throws Exception {
        boolean success = diskService.delete(request.getWid(), request.getPath());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("disk.move")
    public void diskMove(WSSession session, Metadata metadata, DiskModel.Move request) throws Exception {
        boolean success = diskService.move(request.getWid(), request.getPath(), request.getTarget());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("wiki.index")
    public void wikiIndex(WSSession session, Metadata metadata, WikiModel.Index request) throws Exception {
        Table<Param> dataList = wikiService.getWikiDirs(request.getWid(), request.getPath());
        session.sendResponse(metadata.getCommandId(), dataList);
    }

    @OnCommand("wiki.source")
    public void wikiSource(WSSession session, Metadata metadata, WikiModel.Source request) throws Exception {
        Table<Param> dataList = wikiService.getWikiFiles(request.getWid(), request.getPath());
        session.sendResponse(metadata.getCommandId(), dataList);
    }

    @OnCommand("wiki.trash")
    public void wikiTrash(WSSession session, Metadata metadata, DiskModel.Trash request) throws Exception {
        Table<Param> dataList = wikiService.getTrashWikis(request.getWid());
        session.sendResponse(metadata.getCommandId(), dataList);
    }

    @OnCommand("wiki.document.read")
    public void wikiDocumentRead(WSSession session, Metadata metadata, WikiDocumentModel.Read request) throws Exception {
        String content = wikiService.readWikiFile(request.getWid(), request.getPath());
        session.sendResponse(metadata.getCommandId(), content);
    }

    @OnCommand("wiki.document.write")
    public void wikiDocumentWrite(WSSession session, Metadata metadata, WikiDocumentModel.Write request) throws Exception {
        boolean success = wikiService.writeWikiFile(request.getWid(), request.getPath(), request.getContent());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("wiki.mkdir")
    public void wikiMkdir(WSSession session, Metadata metadata, WikiModel.Mkdir request) throws Exception {
        boolean success = wikiService.createDirectory(request.getWid(), request.getPath(), request.getName());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("wiki.create")
    public void wikiCreate(WSSession session, Metadata metadata, WikiModel.Create request) throws Exception {
        boolean success = wikiService.createMarkdown(request.getWid(), request.getPath(), request.getName());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("wiki.delete")
    public void wikiDelete(WSSession session, Metadata metadata, WikiModel.Delete request) throws Exception {
        boolean success = wikiService.delete(request.getWid(), request.getPath());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("wiki.rename")
    public void wikiRename(WSSession session, Metadata metadata, WikiModel.Rename request) throws Exception {
        boolean success = wikiService.rename(request.getWid(), request.getPath(), request.getName());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("wiki.move")
    public void wikiMove(WSSession session, Metadata metadata, WikiModel.Move request) throws Exception {
        boolean success = wikiService.move(request.getWid(), request.getPath(), request.getTarget());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("rules.index")
    public void rulesIndex(WSSession session, Metadata metadata, RulesModel.Index request) throws Exception {
        Table<Param> dataList = rulesService.getRuleFiles(request.getWid());
        session.sendResponse(metadata.getCommandId(), dataList);
    }

    @OnCommand("rules.read")
    public void rulesRead(WSSession session, Metadata metadata, RulesModel.Read request) throws Exception {
        String content = rulesService.readRuleFile(request.getWid(), request.getFilename());
        session.sendResponse(metadata.getCommandId(), content);
    }

    @OnCommand("rules.write")
    public void rulesWrite(WSSession session, Metadata metadata, RulesModel.Write request) throws Exception {
        boolean success = rulesService.writeRuleFile(request.getWid(), request.getFilename(), request.getContent());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("rules.delete")
    public void rulesDelete(WSSession session, Metadata metadata, RulesModel.Delete request) throws Exception {
        boolean success = rulesService.deleteRuleFile(request.getWid(), request.getFilename());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("rules.switch")
    public void rulesSwitch(WSSession session, Metadata metadata, RulesModel.Switch request) throws Exception {
        boolean success = rulesService.switchRuleFile(request.getWid(), request.getFilename(), request.isEnabled());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("rules.rename")
    public void rulesRename(WSSession session, Metadata metadata, RulesModel.Rename request) throws Exception {
        boolean success = rulesService.renameRuleFile(request.getWid(), request.getFilename(), request.getNewFilename());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("memory.index")
    public void memoryIndex(WSSession session, Metadata metadata, MemoryModel.Index request) throws Exception {
        Table<Param> dataList = memoryService.getMemoryFiles(request.getWid());
        session.sendResponse(metadata.getCommandId(), dataList);
    }

    @OnCommand("memory.read")
    public void memoryRead(WSSession session, Metadata metadata, MemoryModel.Read request) throws Exception {
        String content = memoryService.readMemoryFile(request.getWid(), request.getFilename());
        session.sendResponse(metadata.getCommandId(), content);
    }

    @OnCommand("memory.write")
    public void memoryWrite(WSSession session, Metadata metadata, MemoryModel.Write request) throws Exception {
        boolean success = memoryService.writeMemoryFile(request.getWid(), request.getFilename(), request.getContent());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("memory.delete")
    public void memoryDelete(WSSession session, Metadata metadata, MemoryModel.Delete request) throws Exception {
        boolean success = memoryService.deleteMemoryFile(request.getWid(), request.getFilename());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("memory.rename")
    public void memoryRename(WSSession session, Metadata metadata, MemoryModel.Rename request) throws Exception {
        boolean success = memoryService.renameMemoryFile(request.getWid(), request.getFilename(), request.getNewFilename());
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("toolkit.index")
    public void toolkitIndex(WSSession session, Metadata metadata, ToolkitModel.Index request) throws Exception {
        Table<Param> dataList = toolkitService.getToolkitList(request);
        session.sendResponse(metadata.getCommandId(), dataList);
    }

    @OnCommand("toolkit.switch")
    public void toolkitSwitch(WSSession session, Metadata metadata, ToolkitModel.Switch request) throws Exception {
        boolean success = toolkitService.switchTool(request);
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("toolkit.properties.update")
    public void toolkitPropertiesUpdate(WSSession session, Metadata metadata, ToolkitModel.UpdateProperties request) throws Exception {
        boolean success = toolkitService.updateToolProperties(request);
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("skill.index")
    public void skillIndex(WSSession session, Metadata metadata, SkillModel.Index request) throws Exception {
        Table<Param> dataList = skillService.getSkillList(request);
        session.sendResponse(metadata.getCommandId(), dataList);
    }

    @OnCommand("skill.read")
    public void skillRead(WSSession session, Metadata metadata, SkillModel.Read request) throws Exception {
        String content = skillService.readSkillContent(request);
        session.sendResponse(metadata.getCommandId(), content);
    }

    @OnCommand("skill.write")
    public void skillWrite(WSSession session, Metadata metadata, SkillModel.Write request) throws Exception {
        boolean success = skillService.writeSkillContent(request);
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("skill.switch")
    public void skillSwitch(WSSession session, Metadata metadata, SkillModel.Switch request) throws Exception {
        boolean success = skillService.switchSkill(request);
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("skill.delete")
    public void skillDelete(WSSession session, Metadata metadata, SkillModel.Delete request) throws Exception {
        boolean success = skillService.deleteSkill(request);
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("skill.download")
    public void skillDownload(WSSession session, Metadata metadata, SkillModel.Read request) throws Exception {
        byte[] zipData = skillService.downloadSkill(request.getWid(), request.getName());
        if (zipData != null) {
            String base64 = Base64.getEncoder().encodeToString(zipData);
            session.sendResponse(metadata.getCommandId(), base64);
        } else {
            session.sendResponse(metadata.getCommandId(), null);
        }
    }

    @OnCommand("skill.import")
    public void skillImport(WSSession session, Metadata metadata, SkillModel.Write request) throws Exception {
        // content 是 base64 编码的 zip 数据
        byte[] zipData = Base64.getDecoder().decode(request.getContent());
        boolean success = skillService.importSkillFromZip(request.getWid(), zipData);
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("mission.index")
    public void missionIndex(WSSession session, Metadata metadata, MissionModel.Index request) throws Exception {
        Table<Param> missionList = missionService.getMissionList(request);
        session.sendResponse(metadata.getCommandId(), missionList);
    }

    @OnCommand("mission.add")
    public void addMission(WSSession session, Metadata metadata, MissionModel.Add request) throws Exception {
        String missionId = missionService.addMission(request);
        session.sendResponse(metadata.getCommandId(), missionId);
    }

    @OnCommand("mission.remove")
    public void removeMission(WSSession session, Metadata metadata, MissionModel.Remove request) throws Exception {
        boolean success = missionService.removeMission(request);
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("mission.rename")
    public void renameMission(WSSession session, Metadata metadata, MissionModel.Rename request) throws Exception {
        boolean success = missionService.renameMission(request);
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("mission.sort")
    public void sortMission(WSSession session, Metadata metadata, MissionModel.Sort request) throws Exception {
        boolean success = missionService.sortMission(request);
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("sessions.mission.update")
    public void updateSessionMission(WSSession session, Metadata metadata, MissionModel.SessionMission request) throws Exception {
        boolean success = missionService.updateSessionMission(request);
        session.sendResponse(metadata.getCommandId(), success);
    }

    @OnCommand("sessions.mission.infomation")
    public void getSessionMission(WSSession session, Metadata metadata, MissionModel.SessionMission request) throws Exception {
        Param mission = missionService.getMission(request.getWid(), request.getSid());
        session.sendResponse(metadata.getCommandId(), mission);
    }

    private void handleSessionIndexBroadcast(WSSession session, String wid) throws Exception {
        SessionModel.Index request = new SessionModel.Index();
        request.setWid(wid);
        Table<Param> sessionList = sessionsService.getSessionList(request);
        for (WSSession remoteSession : session.getNamespace().getSessions()) {
            remoteSession.sendCommand("message.index.broadcast", wid, sessionList);
        }
    }

    private void handleSecurityPermissionBroadcast(WSSession session, Param infomation) throws Exception {
        for (WSSession remoteSession : session.getNamespace().getSessions()) {
            remoteSession.sendCommand("security.permission.broadcast", infomation);
        }
    }

    private void handleMessageResponseBroadcast(WSSession session, Param infomation) throws Exception {
        for (WSSession remoteSession : session.getNamespace().getSessions()) {
            remoteSession.sendCommand(WorkerMessageApi.COMMAND_MESSAGE_RESPONSE, infomation);
        }
    }
}
