package cloud.apposs.robot.harness;

import cloud.apposs.robot.harness.message.AIMessage;
import cloud.apposs.robot.harness.message.AIMessages;
import cloud.apposs.robot.harness.mind.IMind;
import cloud.apposs.robot.harness.mind.MindFactory;
import cloud.apposs.robot.harness.struct.MessageStruct;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

public final class HarnessMind {
    private final IMind mind;

    public HarnessMind(HarnessWorker worker) throws Exception {
        this.mind = MindFactory.create(worker);
    }

    public boolean buildMessages(MessageStruct message, AIMessages messages) throws Exception {
        return mind.buildMessages(message.getWid(), message, messages);
    }

    public boolean isSessionExists(String wid, String sid) throws Exception {
        return mind.isSessionExists(wid, sid);
    }

    public String addSession(String wid, String name) throws Exception {
        return mind.addSession(wid, name);
    }

    public boolean removeSession(String wid, String sid) throws Exception {
        return mind.removeSession(wid, sid);
    }

    public boolean renameSession(String wid, String sid, String name) throws Exception {
        return mind.renameSession(wid, sid, name);
    }

    public Table<Param> getSessionList(String wid) throws Exception {
        return mind.getSessionList(wid);
    }

    public void addSessionMessage(String wid, String sid, String rid, AIMessage message) throws Exception {
        mind.addSessionMessage(wid, sid, rid, message);
    }

    public void addSessionMessage(String wid, String sid, String rid, Table<AIMessage> messages) throws Exception {
        mind.addSessionMessage(wid, sid, rid, messages);
    }

    public boolean removeSessionMessage(String wid, String sid, String rid, String id) throws Exception {
        return mind.removeSessionMessage(wid, sid, rid, id);
    }

    public boolean truncateSessionMessages(String wid, String sid, String id) throws Exception {
        return mind.truncateSessionMessages(wid, sid, id);
    }

    public Table<Param> getSessionMessages(String wid, String sid) throws Exception {
        return mind.getSessionMessages(wid, sid);
    }

    public boolean clearSessionMessages(String wid, String sid) throws Exception {
        return mind.clearSessionMessages(wid, sid);
    }

    public void submitSession(String wid, String sid, boolean force) throws Exception {
        mind.submitSession(wid, sid, force);
    }

    public String forkSession(String wid, String sid, String messageId, String name) throws Exception {
        return mind.forkSession(wid, sid, messageId, name);
    }

    public Table<Param> searchSession(String query, String roleFilter, int limit) throws Exception {
        return mind.searchSession(query, roleFilter, limit);
    }

    public Table<Param> getRuleFiles(String wid) throws Exception {
        return mind.getRuleFiles(wid);
    }

    public String readRuleFile(String wid, String filename) throws Exception {
        return mind.readRuleFile(wid, filename);
    }

    public boolean writeRuleFile(String wid, String filename, String content) throws Exception {
        return mind.writeRuleFile(wid, filename, content);
    }

    public boolean deleteRuleFile(String wid, String filename) throws Exception {
        return mind.deleteRuleFile(wid, filename);
    }

    public boolean switchRuleFile(String wid, String filename, boolean enabled) throws Exception {
        return mind.switchRuleFile(wid, filename, enabled);
    }

    public boolean renameRuleFile(String wid, String filename, String newFilename) throws Exception {
        return mind.renameRuleFile(wid, filename, newFilename);
    }

    public Table<Param> getMemoryFiles(String wid) throws Exception {
        return mind.getMemoryFiles(wid);
    }

    public String readMemoryFile(String wid, String filename) throws Exception {
        return mind.readMemoryFile(wid, filename);
    }

    public boolean writeMemoryFile(String wid, String filename, String content) throws Exception {
        return mind.writeMemoryFile(wid, filename, content);
    }

    public boolean deleteMemoryFile(String wid, String filename) throws Exception {
        return mind.deleteMemoryFile(wid, filename);
    }

    public boolean renameMemoryFile(String wid, String filename, String newFilename) throws Exception {
        return mind.renameMemoryFile(wid, filename, newFilename);
    }

    public Table<Param> getMissionList(String wid) throws Exception {
        return mind.getMissionList(wid);
    }

    public String addMission(String wid, String name, String description) throws Exception {
        return mind.addMission(wid, name, description);
    }

    public boolean removeMission(String wid, String missionId) throws Exception {
        return mind.removeMission(wid, missionId);
    }

    public boolean renameMission(String wid, String missionId, String name) throws Exception {
        return mind.renameMission(wid, missionId, name);
    }

    public Param getSessionMission(String wid, String sid) throws Exception {
        return mind.getSessionMission(wid, sid);
    }

    public boolean updateMissionSortOrder(String wid, int[] missionIds) throws Exception {
        return mind.updateMissionSortOrder(wid, missionIds);
    }

    public boolean updateSessionMission(String wid, String sid, int missionId) throws Exception {
        return mind.updateSessionMission(wid, sid, missionId);
    }

    public void shutdown() {
        mind.shutdown();
    }
}
