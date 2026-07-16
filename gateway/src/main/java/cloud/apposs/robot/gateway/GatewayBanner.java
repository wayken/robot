package cloud.apposs.robot.gateway;

import cloud.apposs.bootor.banner.Banner;

import java.io.PrintStream;

public class GatewayBanner implements Banner {
    private static final String[] BANNER = {
            "_________      _____                               ",
            "__  ____/_____ __  /________      _______ _____  __",
            "_  / __ _  __ `/  __/  _ \\_ | /| / /  __ `/_  / / /",
            "/ /_/ / / /_/ // /_ /  __/_ |/ |/ // /_/ /_  /_/ /",
            "\\____/  \\__,_/ \\__/ \\___/____/|__/ \\__,_/ _\\__, /",
            "========================================  /____/"
    };
    private static final String GATEWAY_BOOT = " :: Team Robot Gateway :: ";
    private static final int STRAT_LINE_SIZE = 38;

    @Override
    public void printBanner(PrintStream printStream) {
        for (String line : BANNER) {
            printStream.println(line);
        }
        StringBuilder padding = new StringBuilder();
        while (padding.length() < STRAT_LINE_SIZE - (GatewayConstants.GATEWAY_VERSION.length() + GATEWAY_BOOT.length())) {
            padding.append(" ");
        }
        printStream.println(GATEWAY_BOOT + padding + GatewayConstants.GATEWAY_VERSION);
        printStream.println();
        printStream.flush();
    }
}
