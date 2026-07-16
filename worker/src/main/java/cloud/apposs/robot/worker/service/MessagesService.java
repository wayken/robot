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

    public boolean removeMessage(MessageModel.Remove model) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(model.getWid());
        if (worker == null) {
            return false;
        }
        return worker.getMind().removeSessionMessage(model.getWid(), model.getSid(), model.getRid(), model.getId());
    }

    public boolean clearMessages(MessageModel.Clear model) throws Exception {
        HarnessWorker worker = framework.getHarness().getWorker(model.getWid());
        if (worker == null) {
            return false;
        }
        return worker.getMind().clearSessionMessages(model.getWid(), model.getSid());
    }
}
