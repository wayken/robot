package cloud.apposs.robot.harness.mind;

import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.harness.HarnessWorkerProfile;
import cloud.apposs.robot.harness.mind.filesystem.FileSystemMind;

public class MindFactory {
    public static IMind create(HarnessWorker worker) throws Exception {
        HarnessWorkerProfile profile = worker.getProfile();
        String mindType = profile.getMindType().toLowerCase();
        if (IMind.MIND_FILESYSTEM.equals(mindType)) {
            return new FileSystemMind(worker);
        } else if (IMind.MIND_OPENVIKING.equals(mindType)) {
        }
        throw new IllegalArgumentException("Unsupported Mind Type: " + mindType);
    }
}
