package cloud.apposs.robot.harness.plugin.browser;

import cloud.apposs.logger.Logger;
import cloud.apposs.react.React;
import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.robot.harness.plugin.browser.*;
import cloud.apposs.robot.harness.tool.ITool;
import cloud.apposs.robot.harness.util.Strings;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;
import cn.hutool.http.HttpUtil;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.*;

/**
 * 浏览器自动化工具，基于 Playwright，提供多策略启动（系统 Chrome/Edge、显式路径、捆绑或外部 CDP）
 */
public class BrowserTool implements ITool {
    public static final String NAME = "browser_use";

    private static final long IDLE_TIMEOUT_MINUTES = 30;
    private static final int MAX_SNAPSHOT_LENGTH = 20_000;
    private static final int CDP_SCAN_PORT_MIN = 9000;
    private static final int CDP_SCAN_PORT_MAX = 10000;

    /**
     * 共享 Playwright 实例（Node.js 进程），Playwright.create() 启动一个 Node.js 子进程，耗时 1-2 秒，
     * 复用同一实例可将后续 start/connect_cdp 的延迟从 ~98s 降至 ~1s。
     */
    private volatile Playwright sharedPlaywright;
    private final Object playwrightLock = new Object();

    private static BrowserLauncher launcher;

    private final BrowserDiagnosticsService diagnostics;

    private final ConcurrentHashMap<String, BrowserSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread watchdog = new Thread(r, "browser-idle-watchdog");
        watchdog.setDaemon(true);
        return watchdog;
    });

    public BrowserTool(BrowserProperties properties) {
        this.diagnostics = new BrowserDiagnosticsService(properties);
        if (launcher == null) {
            synchronized (BrowserTool.class) {
                if (launcher == null) {
                    launcher = new BrowserLauncher();
                }
            }
        }
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Control a browser (Playwright with multi-strategy launch: system Chrome/Edge channel, explicit path, bundled, or external CDP).\n" +
                "Default is headless. Use headful=true with action=start for a visible window.\n" +
                "Typical flow: start → open(url) → snapshot → click/type → stop.\n" +
                "If start fails, run action=diagnose for a full report of what's missing and how to fix it.\n" +
                "When web_search is unavailable (no Serper/Tavily API key), use this tool to fetch content directly:\n" +
                "e.g. action=open url=https://news.google.com/search?q=... then action=snapshot to read the page.\n\n" +
                "Supported actions:\n" +
                "- start: Launch a new browser (tries system Chrome, system Edge, then Playwright bundled). Optional headful=true.\n" +
                "- stop: Close browser. If connected via CDP, only disconnects (Chrome keeps running).\n" +
                "- open: Navigate to a URL. Requires url parameter. Auto-starts browser if not running.\n" +
                "- snapshot: Get page text content, interactive elements, and title.\n" +
                "- screenshot: Take a screenshot. Optional path to save file; returns base64 if no path.\n" +
                "- click: Click an element. Requires selector (CSS selector).\n" +
                "- type: Type text into an element. Requires selector and text.\n" +
                "- eval: Execute JavaScript on the page. Requires code parameter.\n" +
                "- connect_cdp: Connect to an existing Chrome via CDP. Requires url (e.g. \"http://localhost:9222\").\n" +
                "- list_cdp_targets: Scan local ports (9000-10000) for CDP endpoints. Optional cdpPort for single port.\n" +
                "- navigate_back: Go back in browser history.\n" +
                "- diagnose: Run a self-check — reports which launch strategies are available and what to install if none are.";
    }

    @Override
    public Param parameters() {
        String schema = "{" +
                "  \"type\": \"object\"," +
                "  \"properties\": {" +
                "    \"action\":{" +
                "      \"type\":\"string\"," +
                "      \"enum\":[\"start\",\"stop\",\"open\",\"snapshot\",\"screenshot\",\"click\",\"type\",\"eval\",\"connect_cdp\",\"list_cdp_targets\",\"navigate_back\",\"diagnose\"]," +
                "      \"description\":\"Action to perform\"" +
                "    }," +
                "    \"url\":{" +
                "      \"type\":\"string\"," +
                "      \"description\":\"URL to navigate to (for open), or CDP base URL (for connect_cdp, e.g. http://localhost:9222)\"" +
                "    }," +
                "    \"selector\":{" +
                "      \"type\":\"string\"," +
                "      \"description\":\"CSS selector for target element (for click/type)\"" +
                "    }," +
                "    \"text\":{" +
                "      \"type\":\"string\"," +
                "      \"description\":\"Text to type (for action=type)\"" +
                "    }," +
                "    \"code\":{" +
                "      \"type\":\"string\"," +
                "      \"description\":\"JavaScript code to execute (for action=eval)\"" +
                "    }," +
                "    \"path\":{" +
                "      \"type\":\"string\"," +
                "      \"description\":\"File path to save screenshot (for action=screenshot)\"" +
                "    }," +
                "    \"headful\":{" +
                "      \"type\":\"boolean\"," +
                "      \"description\":\"Launch visible browser window (for action=start, default false)\"" +
                "    }," +
                "    \"cdpPort\":{" +
                "      \"type\":\"integer\"," +
                "      \"description\":\"Single CDP port to scan (for action=list_cdp_targets)\"" +
                "    }," +
                "  }," +
                "  \"required\": [\"action\"]" +
                "}";
        return JsonUtil.parseJsonParam(schema);
    }

    @Override
    public React<String> run(String wid, String sid, String rid, Param parameter, IMessageHook messageHook) throws Exception {
        if (parameter == null) {
            return React.just(failure("parameters must not be null."));
        }
        String action = parameter.getString("action");
        if (Strings.isBlank(action)) {
            return React.just(failure("action is required."));
        }
        String sessionKey = "default";
        switch (action.toLowerCase().trim()) {
            case "start":
                Boolean headful = parameter.getBoolean("headful", false);
                return React.just(handleBrowserStart(sessionKey, headful));
            case "open":
                String url = parameter.getString("url");
                if (Strings.isBlank(url)) {
                    return React.just(failure("url is required for action=open."));
                }
                return React.just(handleBrowserOpen(sessionKey, url));
            case "stop":
                return React.just(handleBrowserStop(sessionKey));
            case "snapshot":
                return React.just(handleBrowserSnapshot(sessionKey));
            case "screenshot":
                String path = parameter.getString("path");
                return React.just(handleBrowserScreenshot(sessionKey, path));
            case "click":
                String clickSelector = parameter.getString("selector");
                if (Strings.isBlank(clickSelector)) {
                    return React.just(failure("selector is required for action=click."));
                }
                return React.just(handleBrowserClick(sessionKey, clickSelector));
            case "type":
                String typeSelector = parameter.getString("selector");
                String text = parameter.getString("text");
                if (Strings.isBlank(typeSelector)) {
                    return React.just(failure("selector is required for action=type."));
                }
                if (text == null) {
                    return React.just(failure("text is required for action=type."));
                }
                return React.just(handleBrowserType(sessionKey, typeSelector, text));
            case "eval":
                String code = parameter.getString("code");
                if (Strings.isBlank(code)) {
                    return React.just(failure("code is required for action=eval."));
                }
                return React.just(handleBrowserEval(sessionKey, code));
            case "connect_cdp":
                String cdpUrl = parameter.getString("url");
                if (Strings.isBlank(cdpUrl)) {
                    return React.just(failure("url is required for action=connect_cdp (e.g. http://127.0.0.1:9222)"));
                }
                return React.just(handleBrowserConnectCdp(sessionKey, cdpUrl));
            case "list_cdp_targets":
                Integer cdpPort = parameter.getInt("cdpPort", null);
                return React.just(handleBrowserListCdpTargets(cdpPort));
            case "navigate_back":
                return React.just(handleBrowserNavigateBack(sessionKey));
            case "diagnose":
                return React.just(handleBrowserDiagnose());
            default:
                return React.just(failure("Unknown action: " + action + ". Supported: start, stop, open, snapshot, screenshot, click, type, eval, connect_cdp, list_cdp_targets, navigate_back, diagnose"));
        }
    }

    private String handleBrowserStart(String sessionKey, boolean headful) {
        BrowserSession existingSession = sessions.get(sessionKey);
        if (existingSession != null && existingSession.isAlive()) {
            if (existingSession.isHeadful()) {
                existingSession.touch();
                return success("Browser already running (headful=" + headful + ")");
            }
            handleBrowserStop(sessionKey);
        }
        int maxSessions = launcher.properties().getMaxSessions();
        if (maxSessions > 0 && sessions.size() >= maxSessions) {
            return failure("Maximum browser sessions reached (" + maxSessions + "). Stop an existing session first or raise mateclaw.browser.max-sessions.");
        }
        long currentTimeMillis = System.currentTimeMillis();
        Playwright playwright = handlePlaywrightGetOrCreate();
        BrowserResult result = launcher.launch(playwright, headful);
        if (!result.isSuccess()) {
            Param response = Param.builder("ok", false)
                    .setString("error", result.getFailureSummary())
                    .setString("hint", "Run action=diagnose for a detailed report and fix suggestions.");
            return JsonUtil.toJson(response, true);
        }
        BrowserSession session = new BrowserSession(result.getBrowser(), result.getContext(), result.getPage(),
                headful, result.isConnectedViaCdp(), result.getCdpUrl());
        sessions.put(sessionKey, session);
        handleScheduleIdleCheck(sessionKey);
        return success("Browser started via " + result.getStrategy() + " (headful=" + headful + ") in " +
                (System.currentTimeMillis() - currentTimeMillis) + "ms. Use action=open with url to navigate.");
    }

    private String handleBrowserOpen(String sessionKey, String url) {
        String normalizedUrl = url.trim();
        if (!normalizedUrl.matches("^https?://.*")) {
            normalizedUrl = "https://" + normalizedUrl;
        }
        if (launcher.properties().isSsrfCheckEnabled()) {
            try {
                UrlSafetyChecker.check(normalizedUrl);
            } catch (SecurityException se) {
                return failure(se.getMessage());
            }
        }
        BrowserSession session = getSession(sessionKey);
        if (session == null) {
            String startResponse = handleBrowserStart(sessionKey, false);
            session = getSession(sessionKey);
            if (session == null) {
                return startResponse;
            }
        }
        session.touch();
        Page page = session.getPage();
        page.navigate(normalizedUrl);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        String title = page.title();
        String currentUrl = page.url();
        Param response = Param.builder("ok", true)
                .setString("title", title)
                .setString("url", currentUrl)
                .setString("message", "Page loaded: " + title);
        return JsonUtil.toJson(response, true);
    }

    private String handleBrowserStop(String sessionKey) {
        BrowserSession session = sessions.remove(sessionKey);
        if (session == null) {
            return success("No browser running");
        }
        // 取消空闲看门狗，避免关闭后定时任务继续运行
        ScheduledFuture<?> watchdog = session.getIdleWatchdog();
        if (watchdog != null && !watchdog.isDone()) {
            watchdog.cancel(false);
        }
        session.release();
        if (session.isConnectedViaCdp()) {
            return success("Disconnected from CDP. Chrome process at " + session.getCdpUrl() + " keeps running.");
        }
        return success("Browser stopped and resources released");
    }

    private String handleBrowserSnapshot(String sessionKey) {
        BrowserSession session = requireSession(sessionKey);
        if (session == null) {
            return failure("No browser running. Use action=start first.");
        }
        session.touch();
        Page page = session.getPage();
        String title = page.title();
        String url = page.url();
        String function =
                "(() => {" +
                "    function getVisibleText(node, depth) {" +
                "        if (depth > 10) return '';" +
                "        const results = [];" +
                "        if (node.nodeType === Node.TEXT_NODE) {" +
                "            const text = node.textContent.trim();" +
                "            if (text) results.push(text);" +
                "        } else if (node.nodeType === Node.ELEMENT_NODE) {" +
                "            const el = node;" +
                "            const style = window.getComputedStyle(el);" +
                "            if (style.display === 'none' || style.visibility === 'hidden') return '';" +
                "            const tag = el.tagName.toLowerCase();" +
                "            if (['a', 'button', 'input', 'select', 'textarea'].includes(tag)) {" +
                "                const id = el.id ? '#' + el.id : '';" +
                "                const cls = el.className && typeof el.className === 'string'" +
                "                    ? '.' + el.className.trim().split(/\\s+/).slice(0, 2).join('.')" +
                "                    : '';" +
                "                const text = el.textContent ? el.textContent.trim().substring(0, 80) : '';" +
                "                const href = el.getAttribute('href') || '';" +
                "                const placeholder = el.getAttribute('placeholder') || '';" +
                "                const selector = tag + id + cls;" +
                "                let desc = '[' + selector + ']';" +
                "                if (text) desc += ' \"' + text + '\"';" +
                "                if (href) desc += ' href=' + href;" +
                "                if (placeholder) desc += ' placeholder=' + placeholder;" +
                "                results.push(desc);" +
                "            }" +
                "            for (const child of el.childNodes) {" +
                "                const childText = getVisibleText(child, depth + 1);" +
                "                if (childText) results.push(childText);" +
                "            }" +
                "        }" +
                "        return results.join('\\n');" +
                "    }" +
                "    const text = getVisibleText(document.body, 0);" +
                "    return text.substring(0, %d);" +
                "})()";
        String textContent = page.evaluate(String.format(function, MAX_SNAPSHOT_LENGTH)).toString();
        Param response = Param.builder("ok", true)
                .setString("title", title)
                .setString("url", url)
                .setString("content", textContent);
        return JsonUtil.toJson(response, true);
    }

    private String handleBrowserScreenshot(String sessionKey, String path) {
        BrowserSession session = requireSession(sessionKey);
        if (session == null) {
            return failure("No browser running. Use action=start first.");
        }
        session.touch();
        Page page = session.getPage();
        Page.ScreenshotOptions options = new Page.ScreenshotOptions().setFullPage(false);
        if (!Strings.isBlank(path)) {
            options.setPath(Paths.get(path));
            page.screenshot(options);
            Param response = Param.builder("ok", true)
                    .setString("path", path)
                    .setString("message", "Screenshot saved to " + path);
            return JsonUtil.toJson(response, true);
        } else {
            byte[] bytes = page.screenshot(options);
            String base64 = Base64.getEncoder().encodeToString(bytes);
            Param response = Param.builder("ok", true)
                    .setString("format", "png")
                    .setString("base64", base64)
                    .setInt("size", bytes.length)
                    .setString("message", "Screenshot captured (" + bytes.length + " bytes)");
            return JsonUtil.toJson(response, true);
        }
    }

    private String handleBrowserClick(String sessionKey, String selector) {
        BrowserSession session = requireSession(sessionKey);
        if (session == null) {
            return failure("No browser running. Use action=start first.");
        }
        session.touch();
        Page page = session.getPage();
        page.click(selector);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        String title = page.title();
        String url = page.url();
        Param response = Param.builder("ok", true)
                .setString("selector", selector)
                .setString("currentUrl", url)
                .setString("currentTitle", title)
                .setString("message", "Clicked element: " + selector);
        return JsonUtil.toJson(response, true);
    }

    private String handleBrowserType(String sessionKey, String selector, String text) {
        BrowserSession session = requireSession(sessionKey);
        if (session == null) {
            return failure("No browser running. Use action=start first.");
        }
        session.touch();
        Page page = session.getPage();
        page.fill(selector, text);
        Param response = Param.builder("ok", true)
                .setString("selector", selector)
                .setInt("textLength", text.length())
                .setString("message", "Typed " + text.length() + " characters into " + selector);
        return JsonUtil.toJson(response, true);
    }

    private String handleBrowserEval(String sessionKey, String code) {
        BrowserSession session = requireSession(sessionKey);
        if (session == null) {
            return failure("No browser running. Use action=start first.");
        }
        session.touch();
        Page page = session.getPage();
        Object evalResult = page.evaluate(code);
        String resultStr = evalResult != null ? evalResult.toString() : "null";
        if (resultStr.length() > 10_000) {
            resultStr = resultStr.substring(0, 10_000) + "\n... [truncated]";
        }
        Param response = Param.builder("ok", true)
                .setString("result", resultStr);
        return JsonUtil.toJson(response, true);
    }

    private String handleBrowserConnectCdp(String sessionKey, String cdpUrl) {
        BrowserSession existing = sessions.get(sessionKey);
        if (existing != null) {
            handleBrowserStop(sessionKey);
        }
        Playwright playwright = handlePlaywrightGetOrCreate();
        String priorCdp = launcher.properties().getCdpUrl();
        launcher.properties().setCdpUrl(cdpUrl);
        BrowserResult result;
        try {
            result = launcher.launch(playwright, true);
        } finally {
            launcher.properties().setCdpUrl(priorCdp);
        }
        if (!result.isSuccess() || !result.isConnectedViaCdp()) {
            return failure("Failed to connect to CDP at " + cdpUrl + ": " + result.getFailureSummary());
        }
        BrowserSession session = new BrowserSession(result.getBrowser(), result.getContext(), result.getPage(),
                true, true, result.getCdpUrl());
        sessions.put(sessionKey, session);
        handleScheduleIdleCheck(sessionKey);
        String title = result.getPage().title();
        String currentUrl = result.getPage().url();
        Param response = Param.builder("ok", true)
                .setString("cdpUrl", result.getCdpUrl())
                .setString("currentUrl", currentUrl)
                .setString("currentTitle", title)
                .setInt("pagesCount", result.getContext().pages().size())
                .setString("message", "Connected to Chrome via CDP at " + result.getCdpUrl() + ". Current page: " + title);
        return JsonUtil.toJson(response, true);
    }

    private String handleBrowserListCdpTargets(Integer cdpPort) {
        Table<Param> targets = Table.builder();
        if (cdpPort != null && cdpPort > 0) {
            Param target = handleCdpPortProbe(cdpPort);
            if (target != null) {
                targets.add(target);
            }
        } else {
            for (int port = CDP_SCAN_PORT_MIN; port <= CDP_SCAN_PORT_MAX; port++) {
                if (!isPortOpen(port)) {
                    continue;
                }
                Param target = handleCdpPortProbe(cdpPort);
                if (target != null) {
                    targets.add(target);
                }
            }
        }
        Param response = Param.builder("ok", true)
                .setTable("targets", targets)
                .setInt("count", targets.size());
        if (targets.isEmpty()) {
            response.setString("message", "No CDP targets found. Start Chrome with --remote-debugging-port=9222 first.");
        } else {
            response.setString("message", "Found " + targets.size() + " CDP target(s). Use connect_cdp with the url to connect.");
        }
        return JsonUtil.toJson(response, true);
    }

    private String handleBrowserNavigateBack(String sessionKey) {
        BrowserSession session = requireSession(sessionKey);
        if (session == null) {
            return failure("No browser running. Use action=start first.");
        }
        session.touch();
        session.getPage().goBack();
        String title = session.getPage().title();
        String url = session.getPage().url();
        Param result = Param.builder("ok", true)
                .setString("title", title)
                .setString("url", url)
                .setString("message", "Navigated back to: " + title);
        return JsonUtil.toJson(result, true);
    }

    private String handleBrowserDiagnose() {
        BrowserDiagnosticsService.Report report = diagnostics.run();
        Param response = Param.builder();
        response.setBoolean("ok", "healthy".equals(report.getOverall()) || "warning".equals(report.getOverall()));
        response.setString("overall", report.getOverall());

        Table<Param> findings = Table.builder();
        for (BrowserDiagnosticsService.Finding finding : report.getFindings()) {
            Param builder = Param.builder();
            builder.setString("id", finding.getId());
            builder.setString("status", finding.getStatus() != null ? finding.getStatus().name() : null);
            builder.setString("message", finding.getMessage());
            if (finding.getData() != null && !finding.getData().isEmpty()) {
                builder.setParam("data", finding.getData());
            }
            if (finding.getAdvice() != null) {
                builder.setString("advice", finding.getAdvice());
            }
            findings.add(builder);
        }
        response.setTable("findings", findings);
        response.setList("advice", report.getAdvice());
        response.setString("summary", BrowserDiagnosticsService.summarise(report));
        return JsonUtil.toJson(response, true);
    }

    private BrowserSession getSession(String sessionKey) {
        BrowserSession session = sessions.get(sessionKey);
        if (session != null && !session.isAlive()) {
            sessions.remove(sessionKey);
            session.release();
            return null;
        }
        return session;
    }

    private BrowserSession requireSession(String sessionKey) {
        return getSession(sessionKey);
    }

    private boolean isPortOpen(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 100);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取或创建共享 Playwright 实例（双重检查锁定），首次调用约 1-2s（启动 Node.js），后续调用 ~0ms
     */
    private Playwright handlePlaywrightGetOrCreate() {
        Playwright playwright = sharedPlaywright;
        if (playwright != null) {
            return playwright;
        }
        synchronized (playwrightLock) {
            playwright = sharedPlaywright;
            if (playwright != null) {
                return playwright;
            }
            playwright = Playwright.create();
            sharedPlaywright = playwright;
            return playwright;
        }
    }

    private void handleScheduleIdleCheck(String sessionKey) {
        BrowserSession session = sessions.get(sessionKey);
        if (session == null) return;
        // 取消已有的看门狗（防止 start→stop→start 导致多个定时任务累积）
        ScheduledFuture<?> watchdog = session.getIdleWatchdog();
        if (watchdog != null && !watchdog.isDone()) {
            watchdog.cancel(false);
        }
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            BrowserSession s = sessions.get(sessionKey);
            if (s == null) return;
            long idleMinutes = (System.currentTimeMillis() - s.getLastActivity()) / 60_000;
            if (idleMinutes >= IDLE_TIMEOUT_MINUTES) {
                handleBrowserStop(sessionKey);
            }
        }, IDLE_TIMEOUT_MINUTES, 5, TimeUnit.MINUTES);
        session.setIdleWatchdog(future);
    }

    private Param handleCdpPortProbe(int port) {
        try {
            String jsonUrl = "http://127.0.0.1:" + port + "/json/version";
            String response = HttpUtil.get(jsonUrl, 2000);
            if (response != null && response.contains("webSocketDebuggerUrl")) {
                Param version = JsonUtil.parseJsonParam(response);
                Param target = Param.builder("port", port)
                        .setString("url", "http://127.0.0.1:" + port)
                        .setString("browser", version.getString("Browser", "unknown"))
                        .setString("webSocketDebuggerUrl", version.getString("webSocketDebuggerUrl", ""));
                return target;
            }
        } catch (Exception e) {
            Logger.debug(e, "[BrowserUse] Port {} is not a CDP endpoint: {}", port);
        }
        return null;
    }

    private static String success(String message) {
        return Param.builder("ok", true).setString("message", message).toJson();
    }

    private static String failure(String message) {
        return Param.builder("ok", false).setString("message", message).toJson();
    }
}
