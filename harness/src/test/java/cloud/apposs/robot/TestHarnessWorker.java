package cloud.apposs.robot;

import cloud.apposs.robot.harness.HarnessFramework;
import cloud.apposs.robot.harness.HarnessSetting;
import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.robot.harness.provider.AIResponse;
import cloud.apposs.robot.harness.struct.MessageStruct;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class TestHarnessWorker {
    private HarnessSetting setting;

    private HarnessFramework framework;

    @Before
    public void init() throws Exception {
        setting = new HarnessSetting();
        framework = new HarnessFramework(setting);
    }

    @Test
    public void testHarnessFramework() throws Exception {
        Thread.sleep(2000);
        CountDownLatch latch = new CountDownLatch(1);
        WorkerMessageHook messageHook = new WorkerMessageHook(latch);
        MessageStruct struct = new MessageStruct();
        struct.setRid("1");
        struct.setSid("1");
        struct.setWid("1522188361127825408");
        struct.setMessage("用$sandtable-rehearsal 帮忙如何在阿里云备案");
        framework.harness(struct, messageHook);
        latch.await();
    }

    @After
    public void destroy() {
        framework.close();
    }

    private final class WorkerMessageHook implements IMessageHook {
        private final CountDownLatch latch;

        private WorkerMessageHook(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onProcessing(String sid, String rid, AIResponse response) throws Exception {
            System.out.println("Message Procession " + response.getContent());
        }

        @Override
        public void onCompletion(String sid, String rid, AIResponse response) throws Exception {
            System.out.println("Message Completion " + response);
            latch.countDown();
        }

        @Override
        public void onError(Throwable cause) throws Exception {
            System.err.println("Message Error " + cause.getMessage());
            cause.printStackTrace();
            latch.countDown();
        }
    }
}
