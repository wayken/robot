package cloud.apposs.robot.worker.service;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.worker.WorkerFramework;
import cloud.apposs.robot.worker.service.model.ProjectModel;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

@Component
public class ProjectService {
    @Autowired
    private WorkerFramework framework;

    public Table<Param> getProjectList(ProjectModel.Index model) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(model.getWid());
        if (worker == null) {
            return null;
        }
        return worker.getMind().getProjectList(model.getWid());
    }

    public String addProject(ProjectModel.Add model) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(model.getWid());
        if (worker == null) {
            return null;
        }
        String path = model.getPath() != null ? model.getPath() : "";
        return worker.getMind().addProject(model.getWid(), model.getName(), model.getDescription(), path);
    }

    public boolean removeProject(ProjectModel.Remove model) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(model.getWid());
        if (worker == null) {
            return false;
        }
        return worker.getMind().removeProject(model.getWid(), model.getProjectId());
    }

    public boolean renameProject(ProjectModel.Rename model) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(model.getWid());
        if (worker == null) {
            return false;
        }
        return worker.getMind().renameProject(model.getWid(), model.getProjectId(), model.getName());
    }

    public boolean updateSessionProject(ProjectModel.SessionProject model) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(model.getWid());
        if (worker == null) {
            return false;
        }
        return worker.getMind().updateSessionProject(model.getWid(), model.getSid(), model.getProjectId());
    }

    public boolean updateSessionProject(String wid, String sid, int projectId) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(wid);
        if (worker == null) {
            return false;
        }
        return worker.getMind().updateSessionProject(wid, sid, projectId);
    }

    public Param getProject(String wid, String sid) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(wid);
        if (worker == null) {
            return null;
        }
        return worker.getMind().getSessionProject(wid, sid);
    }
}
