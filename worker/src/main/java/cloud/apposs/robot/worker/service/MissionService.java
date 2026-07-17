package cloud.apposs.robot.worker.service;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.worker.WorkerFramework;
import cloud.apposs.robot.worker.service.model.MissionModel;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

@Component
public class MissionService {
    @Autowired
    private WorkerFramework framework;

    public Table<Param> getMissionList(MissionModel.Index model) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(model.getWid());
        if (worker == null) {
            return null;
        }
        return worker.getMind().getMissionList(model.getWid());
    }

    public String addMission(MissionModel.Add model) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(model.getWid());
        if (worker == null) {
            return null;
        }
        return worker.getMind().addMission(model.getWid(), model.getName(), model.getDescription());
    }

    public boolean removeMission(MissionModel.Remove model) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(model.getWid());
        if (worker == null) {
            return false;
        }
        return worker.getMind().removeMission(model.getWid(), model.getMissionId());
    }

    public boolean sortMission(MissionModel.Sort model) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(model.getWid());
        if (worker == null) {
            return false;
        }
        return worker.getMind().updateMissionSortOrder(model.getWid(), model.getMissionIds());
    }

    public boolean renameMission(MissionModel.Rename model) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(model.getWid());
        if (worker == null) {
            return false;
        }
        return worker.getMind().renameMission(model.getWid(), model.getMissionId(), model.getName());
    }

    public boolean updateSessionMission(MissionModel.SessionMission model) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(model.getWid());
        if (worker == null) {
            return false;
        }
        return worker.getMind().updateSessionMission(model.getWid(), model.getSid(), model.getMissionId());
    }

    public boolean updateSessionMission(String wid, String sid, int missionId) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(wid);
        if (worker == null) {
            return false;
        }
        return worker.getMind().updateSessionMission(wid, sid, missionId);
    }

    public Param getMission(String wid, String sid) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(wid);
        if (worker == null) {
            return null;
        }
        return worker.getMind().getSessionMission(wid, sid);
    }
}
