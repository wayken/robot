package cloud.apposs.robot.shellx.tui;

import cloud.apposs.robot.shellx.util.AnsiColor;
import cloud.apposs.robot.shellx.util.TerminalUtil;

/**
 * TUI 加载动画/进度指示器
 */
public class TuiSpinner {
    private static final String[] FRAMES = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
    private static final long FRAME_INTERVAL = 80; // ms

    private volatile boolean running = false;
    private Thread spinnerThread;
    private String message;

    public TuiSpinner(String message) {
        this.message = message;
    }

    /**
     * 开始动画
     */
    public void start() {
        if (running) {
            return;
        }
        running = true;
        spinnerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int frameIndex = 0;
                while (running) {
                    String frame = FRAMES[frameIndex % FRAMES.length];
                    TerminalUtil.clearLine();
                    System.out.print(AnsiColor.cyan(frame) + " " + AnsiColor.dim(message));
                    System.out.flush();
                    frameIndex++;
                    try {
                        Thread.sleep(FRAME_INTERVAL);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, "TUI-Spinner");
        spinnerThread.setDaemon(true);
        spinnerThread.start();
    }

    /**
     * 停止动画
     */
    public void stop() {
        running = false;
        if (spinnerThread != null) {
            spinnerThread.interrupt();
            try {
                spinnerThread.join(200);
            } catch (InterruptedException ignored) {
            }
        }
        TerminalUtil.clearLine();
    }

    /**
     * 停止并显示完成
     */
    public void stopWithSuccess(String msg) {
        stop();
        System.out.println(AnsiColor.green("✓") + " " + msg);
    }

    /**
     * 停止并显示失败
     */
    public void stopWithError(String msg) {
        stop();
        System.out.println(AnsiColor.red("✗") + " " + msg);
    }

    /**
     * 更新消息
     */
    public void setMessage(String message) {
        this.message = message;
    }
}
