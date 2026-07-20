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

    public Table<Param> getMissionList(MissionModel.Index request) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(request.getWid());
        if (worker == null) {
            return null;
        }
        return worker.getMind().getMissionList(request.getWid());
    }

    public String addMission(MissionModel.Add request) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(request.getWid());
        if (worker == null) {
            return null;
        }
        return worker.getMind().addMission(request.getWid(), request.getName(), request.getDescription());
    }

    public boolean removeMission(MissionModel.Remove request) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(request.getWid());
        if (worker == null) {
            return false;
        }
        return worker.getMind().removeMission(request.getWid(), request.getMissionId());
    }

    public boolean sortMission(MissionModel.Sort request) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(request.getWid());
        if (worker == null) {
            return false;
        }
        return worker.getMind().updateMissionSortOrder(request.getWid(), request.getMissionIds());
    }

    public boolean renameMission(MissionModel.Rename request) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(request.getWid());
        if (worker == null) {
            return false;
        }
        return worker.getMind().renameMission(request.getWid(), request.getMissionId(), request.getName());
    }

    public boolean updateSessionMission(MissionModel.SessionMission request) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(request.getWid());
        if (worker == null) {
            return false;
        }
        return worker.getMind().updateSessionMission(request.getWid(), request.getSid(), request.getMissionId());
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
