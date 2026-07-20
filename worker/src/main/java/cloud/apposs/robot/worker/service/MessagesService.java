package cloud.apposs.robot.worker.service;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.worker.WorkerFramework;
import cloud.apposs.robot.worker.service.model.MessageModel;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

@Component
public class MessagesService {
    @Autowired
    private WorkerFramework framework;

    public Table<Param> getMessageList(String wid, String sid) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(wid);
        if (worker == null) {
            return null;
        }
        return worker.getMind().getSessionMessages(wid, sid);
    }

    public boolean removeMessage(MessageModel.Remove request) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(request.getWid());
        if (worker == null) {
            return false;
        }
        return worker.getMind().removeSessionMessage(request.getWid(), request.getSid(), request.getRid(), request.getId());
    }

    public boolean truncateMessages(MessageModel.Truncate request) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(request.getWid());
        if (worker == null) {
            return false;
        }
        return worker.getMind().truncateSessionMessages(request.getWid(), request.getSid(), request.getId());
    }

    public boolean clearMessages(MessageModel.Clear request) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(request.getWid());
        if (worker == null) {
            return false;
        }
        return worker.getMind().clearSessionMessages(request.getWid(), request.getSid());
    }
}
