package cloud.apposs.robot.harness.plugin.browser;

/**
 * 浏览器启动配置。支持多种降级策略，以便在 Playwright 捆绑的 Chromium 无法下载的环境（离线 CI、企业防火墙、精简容器等）中仍能正常启动浏览器
 * 启动时的优先级（从高到低）：
 * <pre>
 *   1. {@link #cdpUrl} — 通过 DevTools Protocol 连接已运行的 Chrome
 *   2. {@link #chromePath} 或环境变量 {@code CHROME_PATH} — 指定可执行文件路径
 *   3. {@link #channel} — Playwright 渠道（"chrome"、"msedge" 等）
 *   4. 在常见路径下自动检测系统已安装的 Chrome/Edge/Brave
 *   5. Playwright 捆绑的 Chromium（需执行 {@code playwright install}）
 *   6. 外部进程 CDP 启动（以 --remote-debugging-port 启动系统 Chrome 后附加）
 * </pre>
 */
public class BrowserProperties {
    // 已启动的 Chrome CDP 端点（如 http://127.0.0.1:9222），设置后优先级最高
    private String cdpUrl = "";

    // chrome.exe / google-chrome / msedge 的绝对路径，优先于 channel 和自动检测
    private String chromePath = "";

    // Playwright 渠道：chrome | msedge | chrome-beta | chrome-dev | msedge-beta | msedge-dev
    private String channel = "";

    // 在使用 Playwright 捆绑的 Chromium 之前，优先尝试系统已安装的浏览器（渠道 + 路径扫描）
    private boolean preferSystem = true;

    // 自动启动会话时默认使用无头模式。{@code action=start headed=true} 可覆盖此设置
    private boolean headless = true;

    // 启动浏览器的语言环境，如 "en-US"、"zh-CN"，默认为 "en-US"
    private String locale = "en-US";

    // 启用最后兜底策略：以 --remote-debugging-port=0 启动 Chrome 并通过 CDP 连接
    private boolean allowExternalCdpFallback = true;

    // CDP / 外部 CDP 附加的连接超时时间（秒）
    private int cdpTimeoutSeconds = 20;

    // 所有 Agent 的最大并发浏览器会话数，防止内存占用失控
    private int maxSessions = 5;

    // 阻止导航到回环地址、私有网络、链路本地及云元数据主机，防止 SSRF 攻击
    private boolean ssrfCheckEnabled = true;

    // 启动浏览器的视口宽度（像素）
    private int viewportWidth = 1280;

    // 启动浏览器的视口高度（像素）
    private int viewportHeight = 800;

    public String getCdpUrl() {
        return cdpUrl;
    }

    public void setCdpUrl(String cdpUrl) {
        this.cdpUrl = cdpUrl;
    }

    public String getChromePath() {
        return chromePath;
    }

    public void setChromePath(String chromePath) {
        this.chromePath = chromePath;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public boolean isPreferSystem() {
        return preferSystem;
    }

    public void setPreferSystem(boolean preferSystem) {
        this.preferSystem = preferSystem;
    }

    public boolean isHeadless() {
        return headless;
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public boolean isAllowExternalCdpFallback() {
        return allowExternalCdpFallback;
    }

    public void setAllowExternalCdpFallback(boolean allowExternalCdpFallback) {
        this.allowExternalCdpFallback = allowExternalCdpFallback;
    }

    public int getCdpTimeoutSeconds() {
        return cdpTimeoutSeconds;
    }

    public void setCdpTimeoutSeconds(int cdpTimeoutSeconds) {
        this.cdpTimeoutSeconds = cdpTimeoutSeconds;
    }

    public int getMaxSessions() {
        return maxSessions;
    }

    public void setMaxSessions(int maxSessions) {
        this.maxSessions = maxSessions;
    }

    public boolean isSsrfCheckEnabled() {
        return ssrfCheckEnabled;
    }

    public void setSsrfCheckEnabled(boolean ssrfCheckEnabled) {
        this.ssrfCheckEnabled = ssrfCheckEnabled;
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public void setViewportWidth(int viewportWidth) {
        this.viewportWidth = viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    public void setViewportHeight(int viewportHeight) {
        this.viewportHeight = viewportHeight;
    }
}
