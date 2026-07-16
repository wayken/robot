package cloud.apposs.robot.worker;

import cloud.apposs.websocket.banner.Banner;

import java.io.PrintStream;

public class WorkerBanner implements Banner {
    private static final String[] BANNER = {
            "___       ______________________ __________________ \n" +
            "__ |     / /_  __ \\__  __ \\__  //_/__  ____/__  __ \\\n" +
            "__ | /| / /_  / / /_  /_/ /_  ,<  __  __/  __  /_/ /\n" +
            "__ |/ |/ / / /_/ /_  _, _/_  /| | _  /___  _  _, _/ \n" +
            "____/|__/  \\____/ /_/ |_| /_/ |_| /_____/  /_/ |_|"
    };
    private static final String GATEWAY_BOOT = " :: Teambeit Robot Worker :: ";
    private static final int STRAT_LINE_SIZE = 38;

    @Override
    public void printBanner(PrintStream printStream) {
        for (String line : BANNER) {
            printStream.println(line);
        }
        StringBuilder padding = new StringBuilder();
        while (padding.length() < STRAT_LINE_SIZE - (WorkerConstants.WORKER_VERSION.length() + GATEWAY_BOOT.length())) {
            padding.append(" ");
        }
        printStream.println(GATEWAY_BOOT + padding + WorkerConstants.WORKER_VERSION);
        printStream.println();
        printStream.flush();
    }
}
