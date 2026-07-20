package cloud.apposs.robot.worker.service;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.worker.WorkerFramework;
import cloud.apposs.robot.worker.service.model.SessionModel;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

@Component
public class SessionsService {
    @Autowired
    private WorkerFramework framework;

    public Table<Param> getSessionList(SessionModel.Index model) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(model.getWid());
        if (worker == null) {
            return null;
        }
        Table<Param> sessionList = worker.getMind().getSessionList(model.getWid());
        if (sessionList != null) {
            for (Param session : sessionList) {
                Object sid = session.getObject("id");
                session.setBoolean("running", sid != null && worker.isRunning(sid.toString()));
            }
        }
        return sessionList;
    }

    public String addSession(SessionModel.Add model) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(model.getWid());
        if (worker == null) {
            return null;
        }
        return worker.getMind().addSession(model.getWid(), model.getName());
    }

    public boolean removeSession(SessionModel.Remove model) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(model.getWid());
        if (worker == null) {
            return false;
        }
        return worker.getMind().removeSession(model.getWid(), model.getSid());
    }

    public boolean renameSession(SessionModel.Rename model) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(model.getWid());
        if (worker == null) {
            return false;
        }
        return worker.getMind().renameSession(model.getWid(), model.getSid(), model.getName());
    }

    public Table<Param> getSessionMessages(String wid, String sid) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(wid);
        if (worker == null) {
            return null;
        }
        return worker.getMind().getSessionMessages(wid, sid);
    }

    public String forkSession(SessionModel.Fork model) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(model.getWid());
        if (worker == null) {
            return null;
        }
        return worker.getMind().forkSession(model.getWid(), model.getSid(), model.getMessageId(), model.getName());
    }
}
