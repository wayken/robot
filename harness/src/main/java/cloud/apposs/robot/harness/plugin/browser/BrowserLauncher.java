package cloud.apposs.robot.harness.plugin.browser;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.robot.harness.plugin.browser.BrowserProperties;
import cloud.apposs.robot.harness.util.Strings;
import cloud.apposs.util.Table;
import com.microsoft.playwright.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Component
public class BrowserLauncher {
    private static final boolean IS_MACOS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    private static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");

    private final BrowserProperties properties;

    public BrowserLauncher() {
        this.properties = new BrowserProperties();
    }

    public BrowserProperties properties() {
        return properties;
    }

    /**
     * 启动一个浏览器会话，依次尝试所有可用策略，直到某个策略成功为止
     *
     * @param  playwright Playwright 实例，由外层共享管理
     * @param  headful    是否需要有界面，如果为 false 则优先尝试浏览器无头模式
     * @return 追踪记录，调用方可据此了解"最终使用了哪种方式"
     */
    public BrowserResult launch(Playwright playwright, boolean headful) {
        Table<BrowserResult.Attempt> tracert = Table.builder();
        // 1. 显式 CDP 端点 — 由用户自行管理 Chrome 进程
        String cdpUrl = properties.getCdpUrl();
        if (Strings.isBlank(cdpUrl)) {
            BrowserResult result = handleCdpTryLaunch(playwright, cdpUrl, tracert, BrowserResult.Strategy.CONFIG_CDP);
            if (result != null) {
                return result;
            }
        }
        // 2. 显式可执行文件路径（配置项或环境变量）
        String explicitPath = handleFirstNonBlank(properties.getChromePath(), System.getenv("CHROME_PATH"));
        if (explicitPath != null) {
            BrowserResult result = handleExecutablePathTryLaunch(playwright, explicitPath, headful, tracert, BrowserResult.Strategy.CONFIG_PATH);
            if (result != null) {
                return result;
            }
        }
        // 3. 显式渠道（chrome / msedge 等）
        String channel = properties.getChannel();
        if (Strings.isBlank(channel)) {
            BrowserResult result = handleChannelTryLaunch(playwright, channel, headful, tracert, BrowserResult.Strategy.CONFIG_CHANNEL);
            if (result != null) {
                return result;
            }
        }
        // 4. 通过渠道自动检测优先使用系统浏览器（先 chrome，再 msedge）
        if (properties.isPreferSystem()) {
            for (String autoChannel : new String[]{"chrome", "msedge"}) {
                BrowserResult result = handleChannelTryLaunch(playwright, autoChannel, headful, tracert, BrowserResult.Strategy.AUTO_CHANNEL);
                if (result != null) {
                    return result;
                }
            }
            // 5. 扫描常见安装路径，通过 executablePath 启动
            for (Path candidate : handleSystemBrowserCandidates()) {
                BrowserResult result = handleExecutablePathTryLaunch(playwright, candidate.toString(), headful, tracert, BrowserResult.Strategy.AUTO_PATH);
                if (result != null) {
                    return result;
                }
            }
        }
        // 6. Playwright 捆绑的 Chromium（需执行 `playwright install`）
        BrowserResult bundled = handleBundledTryLaunch(playwright, headful, tracert);
        if (bundled != null) {
            return bundled;
        }
        // 7. 最后兜底：以 --remote-debugging-port=0 启动系统 Chrome 并通过 CDP 附加
        // 完全绕过 Playwright 的 Node 启动器 — 适用于 Playwright install 损坏的场景
        if (properties.isAllowExternalCdpFallback()) {
            BrowserResult result = handleExternalCdpTryLaunch(playwright, headful, tracert);
            if (result != null) {
                return result;
            }
        }
        // 所有策略均失败
        return BrowserResult.failure(tracert, handleFailureSummarise(tracert));
    }

    private BrowserResult handleCdpTryLaunch(Playwright playwright, String url, Table<BrowserResult.Attempt> tracert, BrowserResult.Strategy strategy) {
        long currentTimeMillis = System.currentTimeMillis();
        String normalized = handleCdpUrlNormalize(url);
        try {
            Page page;
            BrowserContext context;
            Browser browser = playwright.chromium().connectOverCDP(normalized);
            List<BrowserContext> contexts = browser.contexts();
            if (!contexts.isEmpty()) {
                context = contexts.get(0);
                List<Page> pages = context.pages();
                page = pages.isEmpty() ? context.newPage() : pages.get(0);
            } else {
                context = browser.newContext();
                page = context.newPage();
            }
            tracert.add(BrowserResult.Attempt.success(strategy,
                    "connectOverCDP(" + normalized + ")", System.currentTimeMillis() - currentTimeMillis));
            return BrowserResult.success(browser, context, page, true, normalized, strategy, tracert);
        } catch (Exception cause) {
            tracert.add(BrowserResult.Attempt.failure(strategy,
                    "connectOverCDP(" + normalized + ")", System.currentTimeMillis() - currentTimeMillis, cause.getMessage()));
            return null;
        }
    }

    private BrowserResult handleExecutablePathTryLaunch(Playwright playwright, String path,
            boolean headful, Table<BrowserResult.Attempt> tracert, BrowserResult.Strategy strategy) {
        if (!Files.exists(Paths.get(path))) {
            tracert.add(BrowserResult.Attempt.failure(strategy, "ExecutablePath=" + path, 0, "File not found"));
            return null;
        }
        long currentTimeMillis = System.currentTimeMillis();
        try {
            BrowserType.LaunchOptions options = handleBaseLaunchOptionsLoad(headful)
                    .setExecutablePath(Paths.get(path));
            Browser browser = playwright.chromium().launch(options);
            BrowserResult result = handleLocalBrowserWrap(browser, strategy, "ExecutablePath=" + path,
                    System.currentTimeMillis() - currentTimeMillis, tracert);
            return result;
        } catch (PlaywrightException cause) {
            tracert.add(BrowserResult.Attempt.failure(strategy, "ExecutablePath=" + path,
                    System.currentTimeMillis() - currentTimeMillis, cause.getMessage()));
            return null;
        }
    }

    private BrowserResult handleChannelTryLaunch(Playwright playwright, String channel,
                boolean headful, List<BrowserResult.Attempt> tracert, BrowserResult.Strategy strategy) {
        long currentTimeMillis = System.currentTimeMillis();
        try {
            BrowserType.LaunchOptions options = handleBaseLaunchOptionsLoad(headful).setChannel(channel);
            Browser browser = playwright.chromium().launch(options);
            return handleLocalBrowserWrap(browser, strategy, "channel=" + channel,
                    System.currentTimeMillis() - currentTimeMillis, tracert);
        } catch (PlaywrightException e) {
            tracert.add(BrowserResult.Attempt.failure(strategy, "channel=" + channel,
                    System.currentTimeMillis() - currentTimeMillis, e.getMessage()));
            return null;
        }
    }

    public static List<Path> handleSystemBrowserCandidates() {
        List<Path> paths = new ArrayList<>();
        if (IS_WINDOWS) {
            String pf = System.getenv("ProgramFiles");
            String pf86 = System.getenv("ProgramFiles(x86)");
            String local = System.getenv("LOCALAPPDATA");
            for (String root : new String[]{ pf, pf86 }) {
                if (Strings.isBlank(root)) continue;
                paths.add(Paths.get(root, "Google", "Chrome", "Application", "chrome.exe"));
                paths.add(Paths.get(root, "Microsoft", "Edge", "Application", "msedge.exe"));
                paths.add(Paths.get(root, "BraveSoftware", "Brave-Browser", "Application", "brave.exe"));
            }
            if (!Strings.isBlank(local)) {
                paths.add(Paths.get(local, "Google", "Chrome", "Application", "chrome.exe"));
                paths.add(Paths.get(local, "Microsoft", "Edge", "Application", "msedge.exe"));
            }
        } else if (IS_MACOS) {
            paths.add(Paths.get("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"));
            paths.add(Paths.get("/Applications/Chromium.app/Contents/MacOS/Chromium"));
            paths.add(Paths.get("/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge"));
            paths.add(Paths.get("/Applications/Brave Browser.app/Contents/MacOS/Brave Browser"));
        } else {
            // Linux
            paths.add(Paths.get("/usr/bin/google-chrome"));
            paths.add(Paths.get("/usr/bin/google-chrome-stable"));
            paths.add(Paths.get("/usr/bin/chromium"));
            paths.add(Paths.get("/usr/bin/chromium-browser"));
            paths.add(Paths.get("/snap/bin/chromium"));
            paths.add(Paths.get("/usr/bin/microsoft-edge"));
            paths.add(Paths.get("/usr/bin/microsoft-edge-stable"));
            paths.add(Paths.get("/usr/bin/brave-browser"));
        }
        return paths;
    }

    private BrowserResult handleBundledTryLaunch(Playwright playwright, boolean headful, List<BrowserResult.Attempt> tracert) {
        long currentTimeMillis = System.currentTimeMillis();
        try {
            Browser browser = playwright.chromium().launch(handleBaseLaunchOptionsLoad(headful));
            return handleLocalBrowserWrap(browser, BrowserResult.Strategy.BUNDLED, "playwright-bundled-chromium",
                    System.currentTimeMillis() - currentTimeMillis, tracert);
        } catch (PlaywrightException cause) {
            tracert.add(BrowserResult.Attempt.failure(BrowserResult.Strategy.BUNDLED, "playwright-bundled-chromium",
                    System.currentTimeMillis() - currentTimeMillis, cause.getMessage()));
            return null;
        }
    }

    /**
     * 以 {@code --remote-debugging-port=0} 自行启动系统浏览器，解析 stderr 获取实际的 DevTools WebSocket URL，再通过 Playwright 的 CDP 客户端附加
     * 这是 openfang 的模式 — 完全绕过 Playwright 基于 Node 的启动器，因此在未执行 {@code playwright install} 或 Node 不稳定时仍可正常工作。
     */
    private BrowserResult handleExternalCdpTryLaunch(Playwright playwright, boolean headful, List<BrowserResult.Attempt> tracert) {
        long currentTimeMillis = System.currentTimeMillis();
        Path browserBin = null;
        for (Path candidate : handleSystemBrowserCandidates()) {
            if (Files.exists(candidate)) {
                browserBin = candidate;
                break;
            }
        }
        if (browserBin == null) {
            tracert.add(BrowserResult.Attempt.failure(BrowserResult.Strategy.EXTERNAL_CDP, "external-chrome-spawn",
                    System.currentTimeMillis() - currentTimeMillis, "No system browser found in common paths"));
            return null;
        }
        List<String> command = new ArrayList<>();
        command.add(browserBin.toString());
        command.add("--remote-debugging-port=0");
        command.add("--no-first-run");
        command.add("--no-default-browser-check");
        command.add("--disable-extensions");
        command.add("--disable-background-networking");
        if (properties.isHeadless() && !headful) {
            command.add("--headless=new");
        }
        if (isRunningAsRoot() || IS_WINDOWS) {
            command.add("--no-sandbox");
        }
        if (isRunningInContainer()) {
            command.add("--disable-dev-shm-usage");
        }
        command.add("about:blank");

        ProcessBuilder pb = new ProcessBuilder(command).redirectErrorStream(false);
        // 安全：不将父进程的敏感信息（API Key 等）泄露给 Chrome
        // 仅保留 Chrome 运行所需的环境变量，与 openfang 通过 env_clear 的做法一致
        java.util.Map<String, String> env = pb.environment();
        java.util.Map<String, String> keep = new java.util.LinkedHashMap<>();
        for (String key : new String[]{"PATH", "HOME", "USERPROFILE", "SYSTEMROOT", "TEMP", "TMP", "TMPDIR",
                "APPDATA", "LOCALAPPDATA", "XDG_CONFIG_HOME", "XDG_CACHE_HOME", "DISPLAY", "WAYLAND_DISPLAY"}) {
            String v = env.get(key);
            if (v != null) keep.put(key, v);
        }
        env.clear();
        env.putAll(keep);
        Process proc;
        try {
            proc = pb.start();
        } catch (Exception cause) {
            tracert.add(BrowserResult.Attempt.failure(BrowserResult.Strategy.EXTERNAL_CDP, browserBin + " --remote-debugging-port",
                    System.currentTimeMillis() - currentTimeMillis, "Launch failed: " + cause.getMessage()));
            return null;
        }

        String wsUrl;
        try {
            wsUrl = handleDevToolsUrlRead(proc, properties.getCdpTimeoutSeconds());
        } catch (Exception cause) {
            proc.destroyForcibly();
            tracert.add(BrowserResult.Attempt.failure(BrowserResult.Strategy.EXTERNAL_CDP, browserBin.toString(),
                    System.currentTimeMillis() - currentTimeMillis, cause.getMessage()));
            return null;
        }

        // 推导 http base — Playwright 的 connectOverCDP 可直接接受 ws://，但 http:// 更稳妥。
        String cdpBase = wsUrl.replaceFirst("^ws://", "http://").replaceFirst("/devtools/.*", "");
        try {
            Browser browser = playwright.chromium().connectOverCDP(cdpBase);
            BrowserContext context = browser.contexts().isEmpty()
                    ? browser.newContext()
                    : browser.contexts().get(0);
            Page page = context.pages().isEmpty() ? context.newPage() : context.pages().get(0);
            long elapsed = System.currentTimeMillis() - currentTimeMillis;
            tracert.add(BrowserResult.Attempt.success(BrowserResult.Strategy.EXTERNAL_CDP, browserBin + " + ConnectOverCDP(" + cdpBase + ")", elapsed));
            return BrowserResult.success(browser, context, page, true, cdpBase, BrowserResult.Strategy.EXTERNAL_CDP, tracert);
        } catch (Exception e) {
            proc.destroyForcibly();
            tracert.add(BrowserResult.Attempt.failure(BrowserResult.Strategy.EXTERNAL_CDP, "ConnectOverCDP(" + cdpBase + ")",
                    System.currentTimeMillis() - currentTimeMillis, e.getMessage()));
            return null;
        }
    }

    private BrowserType.LaunchOptions handleBaseLaunchOptionsLoad(boolean headful) {
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(!headful);
        List<String> arguments = handleChromiumLaunchArgsLoad();
        if (!arguments.isEmpty()) {
            options.setArgs(arguments);
        }
        return options;
    }

    private BrowserResult handleLocalBrowserWrap(Browser browser, BrowserResult.Strategy strategy,
                String description, long elapsedMs, List<BrowserResult.Attempt> tracert) {
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(properties.getViewportWidth(), properties.getViewportHeight())
                .setLocale(properties.getLocale()));
        Page page = context.newPage();
        tracert.add(BrowserResult.Attempt.success(strategy, description, elapsedMs));
        return BrowserResult.success(browser, context, page, false, null, strategy, tracert);
    }

    private static String handleCdpUrlNormalize(String url) {
        String s = url.trim();
        if (!s.startsWith("http")) {
            s = "http://" + s;
        }
        s = s.replace("://localhost:", "://127.0.0.1:");
        s = s.replace("://localhost/", "://127.0.0.1/");
        if (s.endsWith("://localhost")) {
            s = s.replace("://localhost", "://127.0.0.1");
        }
        return s;
    }

    private static String handleFirstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (!Strings.isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    public static List<String> handleChromiumLaunchArgsLoad() {
        List<String> arguments = new ArrayList<>();
        boolean inContainer = isRunningInContainer();
        boolean asRoot = isRunningAsRoot();
        if (IS_WINDOWS || inContainer || asRoot) {
            arguments.add("--no-sandbox");
        }
        if (inContainer) {
            arguments.add("--disable-dev-shm-usage");
        }
        if (IS_WINDOWS) {
            arguments.add("--disable-gpu");
        }
        return arguments;
    }

    public static boolean isRunningInContainer() {
        try {
            if (Files.exists(Paths.get("/.dockerenv"))) return true;
            Path cgroup = Paths.get("/proc/1/cgroup");
            if (Files.exists(cgroup)) {
                String content = new String(Files.readAllBytes(cgroup), StandardCharsets.UTF_8);;
                return content.contains("docker") || content.contains("kubepods") || content.contains("containerd");
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static boolean isRunningAsRoot() {
        if (IS_WINDOWS) {
            return false;
        }
        try {
            Path self = Paths.get("/proc/self/status");
            if (Files.exists(self)) {
                for (String line : Files.readAllLines(self)) {
                    if (line.startsWith("Uid:")) {
                        String[] parts = line.split("\\s+");
                        return parts.length > 1 && "0".equals(parts[1]);
                    }
                }
            }
            String userName = System.getProperty("user.name", "");
            return "root".equals(userName);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String handleDevToolsUrlRead(Process proc, int timeoutSeconds) throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder accumulated = new StringBuilder();
            String line;
            while (System.currentTimeMillis() < deadline) {
                if (!reader.ready()) {
                    if (!proc.isAlive()) {
                        throw new IllegalStateException("Chromium exited before printing DevTools URL. stderr=" + accumulated);
                    }
                    Thread.sleep(50);
                    continue;
                }
                line = reader.readLine();
                if (line == null) break;
                accumulated.append(line).append('\n');
                int idx = line.indexOf("DevTools listening on ");
                if (idx >= 0) {
                    return line.substring(idx + "DevTools listening on ".length()).trim();
                }
            }
        }
        throw new IllegalStateException("Timed out (" + timeoutSeconds + "s) waiting for 'DevTools listening on' from chromium stderr");
    }

    private static String handleFailureSummarise(List<BrowserResult.Attempt> tracert) {
        StringBuilder builder = new StringBuilder("Browser launch failed. Tried: ");
        for (int i = 0; i < tracert.size(); i++) {
            if (i > 0) builder.append("; ");
            BrowserResult.Attempt a = tracert.get(i);
            builder.append(a.getStrategy().name()).append(" ").append(a.isSuccess() ? "ok" : "(" + handleLineBrief(a.getError()) + ")");
        }
        return builder.toString();
    }

    private static String handleLineBrief(String value) {
        if (value == null) return "unknown";
        int firstLinesIndex = value.indexOf('\n');
        String firstLine = firstLinesIndex >= 0 ? value.substring(0, firstLinesIndex) : value;
        return firstLine.length() > 120 ? firstLine.substring(0, 120) + "..." : firstLine;
    }
}
