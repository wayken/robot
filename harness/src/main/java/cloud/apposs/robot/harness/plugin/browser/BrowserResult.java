package cloud.apposs.robot.harness.plugin.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;

import java.util.List;

public class BrowserResult {
    private final Browser browser;
    private final BrowserContext context;
    private final Page page;
    private final boolean connectedViaCdp;
    private final String cdpUrl;
    private final Strategy strategy;
    private final List<Attempt> attempts;
    private final boolean success;
    private final String failureSummary;

    private BrowserResult(Browser browser, BrowserContext context, Page page, boolean connectedViaCdp, String cdpUrl,
            Strategy strategy, List<Attempt> attempts, boolean success, String failureSummary) {
        this.browser = browser;
        this.context = context;
        this.page = page;
        this.connectedViaCdp = connectedViaCdp;
        this.cdpUrl = cdpUrl;
        this.strategy = strategy;
        this.attempts = attempts;
        this.success = success;
        this.failureSummary = failureSummary;
    }

    public Browser getBrowser() {
        return browser;
    }

    public BrowserContext getContext() {
        return context;
    }

    public Page getPage() {
        return page;
    }

    public boolean isConnectedViaCdp() {
        return connectedViaCdp;
    }

    public String getCdpUrl() {
        return cdpUrl;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public List<Attempt> getAttempts() {
        return attempts;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getFailureSummary() {
        return failureSummary;
    }

    static BrowserResult success(Browser browser, BrowserContext context, Page page,
                                 boolean cdp, String cdpUrl, Strategy strategy, List<Attempt> attempts) {
        return new BrowserResult(browser, context, page, cdp, cdpUrl, strategy, attempts, true, null);
    }

    static BrowserResult failure(List<Attempt> attempts, String summary) {
        return new BrowserResult(null, null, null, false, null, null, attempts, false, summary);
    }

    public enum Strategy {
        // 用户配置的 CDP 端点（mateclaw.browser.cdp-url）
        CONFIG_CDP,
        // 用户配置的可执行文件路径（mateclaw.browser.chrome-path 或环境变量 CHROME_PATH）
        CONFIG_PATH,
        // 用户配置的渠道（mateclaw.browser.channel）
        CONFIG_CHANNEL,
        // 自动检测的 Playwright 渠道（chrome、msedge）
        AUTO_CHANNEL,
        // 在常见安装路径下自动检测到的系统浏览器
        AUTO_PATH,
        // Playwright 捆绑的 Chromium（需执行 `playwright install`）
        BUNDLED,
        // 以 --remote-debugging-port=0 启动系统 Chrome 并通过 CDP 附加
        EXTERNAL_CDP
    }

    public static class Attempt {
        private final Strategy strategy;
        private final String detail;
        private final long elapsedMs;
        private final boolean success;
        private final String error;

        private Attempt(Strategy strategy, String detail, long elapsedMs, boolean success, String error) {
            this.strategy = strategy;
            this.detail = detail;
            this.elapsedMs = elapsedMs;
            this.success = success;
            this.error = error;
        }

        public static Attempt success(Strategy s, String d, long ms) {
            return new Attempt(s, d, ms, true, null);
        }

        public static Attempt failure(Strategy s, String d, long ms, String e) {
            return new Attempt(s, d, ms, false, e);
        }

        public Strategy getStrategy() {
            return strategy;
        }

        public String getDetail() {
            return detail;
        }

        public long getElapsedMs() {
            return elapsedMs;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getError() {
            return error;
        }
    }
}
