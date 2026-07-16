package cloud.apposs.robot.harness.plugin.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;

import java.util.concurrent.ScheduledFuture;

/**
 * 浏览器会话（不持有 Playwright 实例，Playwright 由外层共享管理）
 */
public class BrowserSession {
    private final Browser browser;

    private final BrowserContext context;

    private volatile Page page;

    private final boolean headful;

    private final boolean connectedViaCdp;

    private final String cdpUrl;

    private volatile long lastActivity;

    // 空闲看门狗定时任务（stop 时取消，避免泄漏）
    private volatile ScheduledFuture<?> idleWatchdog;

    BrowserSession(Browser browser, BrowserContext context, Page page, boolean headful, boolean connectedViaCdp, String cdpUrl) {
        this.browser = browser;
        this.context = context;
        this.page = page;
        this.headful = headful;
        this.connectedViaCdp = connectedViaCdp;
        this.cdpUrl = cdpUrl;
        this.lastActivity = System.currentTimeMillis();
    }

    public Page getPage() {
        return page;
    }

    public boolean isHeadful() {
        return headful;
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }

    public ScheduledFuture<?> getIdleWatchdog() {
        return idleWatchdog;
    }

    public void setIdleWatchdog(ScheduledFuture<?> idleWatchdog) {
        this.idleWatchdog = idleWatchdog;
    }

    public boolean isConnectedViaCdp() {
        return connectedViaCdp;
    }

    public String getCdpUrl() {
        return cdpUrl;
    }

    public void touch() {
        this.lastActivity = System.currentTimeMillis();
    }

    public boolean isAlive() {
        return browser != null && browser.isConnected();
    }

    /**
     * 关闭浏览器会话（不关闭共享 Playwright），根据连接方式不同执行不同的清理逻辑：
     * <pre>
     *  1. CDP 模式：仅断开连接，Chrome 进程继续运行
     *  2. Launch 模式：关闭 context + browser（终止 Chromium 进程）
     * </pre>
     */
    public void release() {
        if (!connectedViaCdp) {
            try {
                if (context != null) context.close();
            } catch (Exception ignored) {
            }
        }
        try {
            if (browser != null) browser.close();
        } catch (Exception ignored) {
        }
    }
}
