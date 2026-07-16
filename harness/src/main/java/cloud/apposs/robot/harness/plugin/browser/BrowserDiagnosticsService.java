package cloud.apposs.robot.harness.plugin.browser;

import cloud.apposs.robot.harness.util.Strings;
import cloud.apposs.util.Param;
import cn.hutool.http.HttpUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BrowserDiagnosticsService {
    private static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    private static final boolean IS_LINUX = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux");

    // Shared libraries Chromium needs on Linux. Missing any is a hard block
    private static final List<String> REQUIRED_LINUX_LIBS = Collections.unmodifiableList(Arrays.asList(
            "libnss3", "libgbm", "libasound", "libxkbcommon", "libx11", "libxcomposite",
            "libxdamage", "libxrandr", "libxfixes", "libatk", "libcups", "libpango"
    ));

    private final BrowserProperties properties;

    public BrowserDiagnosticsService(BrowserProperties properties) {
        this.properties = properties;
    }

    public Report run() {
        List<Finding> findings = new ArrayList<>();
        findings.add(handleEnvironmentInspect());
        findings.add(handlConfiguredCdpInspect());
        findings.add(handleConfiguredPathInspect());
        findings.add(handleEnvPathInspect());
        findings.add(handleSystemBrowsersInspect());
        findings.add(handlePlaywrightCacheInspect());
        if (IS_LINUX) {
            findings.add(handleLinuxLibsInspect());
        }
        String overall = handleDeriveOverall(findings);
        List<String> advice = handleDeriveAdvice(findings);
        return new Report(overall, findings, advice);
    }

    public static String summarise(Report report) {
        StringBuilder builder = new StringBuilder();
        builder.append("Browser diagnostics: ").append(report.overall).append('\n');
        for (Finding finding : report.findings) {
            builder.append("  [").append(finding.status).append("] ").append(finding.id).append(" — ").append(finding.message).append('\n');
        }
        if (!report.advice.isEmpty()) {
            builder.append("Advice:\n");
            for (String a : report.advice) {
                builder.append("  - ").append(a).append('\n');
            }
        }
        return builder.toString();
    }

    private Finding handleEnvironmentInspect() {
        Param data = Param.builder();
        data.setString("os", System.getProperty("os.name"));
        data.setString("arch", System.getProperty("os.arch"));
        data.setString("user", System.getProperty("user.name"));
        data.setBoolean("container", BrowserLauncher.isRunningInContainer());
        data.setBoolean("root", BrowserLauncher.isRunningAsRoot());
        return new Finding("environment", Status.INFO, "Runtime environment", data, null);
    }

    private Finding handlConfiguredCdpInspect() {
        String url = properties.getCdpUrl();
        if (Strings.isBlank(url)) {
            return new Finding("config.cdp-url", Status.INFO, "mateclaw.browser.cdp-url not set", null, null);
        }
        Param data = Param.builder("url", url);
        try {
            String response = HttpUtil.get(handleTrailingStrip(url) + "/json/version", 2000);
            if (response != null && response.contains("webSocketDebuggerUrl")) {
                data.setBoolean("reachable", true);
                return new Finding("config.cdp-url", Status.OK, "CDP endpoint reachable", data, null);
            }
            data.setBoolean("reachable", false);
            data.setString("response", response);
            return new Finding("config.cdp-url", Status.ERROR,
                    "CDP endpoint did not return a valid /json/version payload", data,
                    "Ensure Chrome was started with --remote-debugging-port=" + handlePortPick(url) + " and /json/version is reachable.");
        } catch (Exception e) {
            data.setString("error", e.getMessage());
            return new Finding("config.cdp-url", Status.ERROR,
                    "CDP endpoint unreachable: " + e.getMessage(), data,
                    "Start Chrome with --remote-debugging-port or clear mateclaw.browser.cdp-url.");
        }
    }

    private Finding handleConfiguredPathInspect() {
        String chromePath = properties.getChromePath();
        if (Strings.isBlank(chromePath)) {
            return new Finding("config.chrome-path", Status.INFO, "mateclaw.browser.chrome-path not set", null, null);
        }
        Path path = Paths.get(chromePath);
        if (!Files.exists(path)) {
            return new Finding("config.chrome-path", Status.ERROR,
                    "Configured chrome-path does not exist: " + chromePath, Param.builder("path", chromePath),
                    "Install Chrome at that path, or clear mateclaw.browser.chrome-path.");
        }
        if (!Files.isExecutable(path)) {
            return new Finding("config.chrome-path", Status.ERROR,
                    "Configured chrome-path is not executable: " + chromePath, Param.builder("path", chromePath),
                    "chmod +x the binary, or point to the real chrome executable.");
        }
        return new Finding("config.chrome-path", Status.OK, "Configured chrome-path is valid", Param.builder("path", chromePath), null);
    }

    private Finding handleEnvPathInspect() {
        String env = System.getenv("CHROME_PATH");
        if (Strings.isBlank(env)) {
            return new Finding("env.CHROME_PATH", Status.INFO, "CHROME_PATH not set", null, null);
        }
        Path path = Paths.get(env);
        if (!Files.exists(path)) {
            return new Finding("env.CHROME_PATH", Status.WARN,
                    "CHROME_PATH points to a missing file: " + env, Param.builder("path", env),
                    "Fix CHROME_PATH or unset it to let auto-detection run.");
        }
        return new Finding("env.CHROME_PATH", Status.OK, "CHROME_PATH resolves to a real file", Param.builder("path", env), null);
    }

    private Finding handleSystemBrowsersInspect() {
        List<Map<String, Object>> found = new ArrayList<>();
        for (Path candidate : BrowserLauncher.handleSystemBrowserCandidates()) {
            if (Files.exists(candidate)) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("path", candidate.toString());
                entry.put("executable", Files.isExecutable(candidate));
                found.add(entry);
            }
        }
        if (found.isEmpty()) {
            List<String> scannedPaths = BrowserLauncher.handleSystemBrowserCandidates().stream()
                    .map(Path::toString)
                    .collect(Collectors.toList());
            return new Finding("system.browsers", Status.WARN,
                    "No system Chrome / Edge / Brave found on well-known paths", Param.builder("scanned", scannedPaths), handleBrowserAdviceInstall());
        }

        return new Finding("system.browsers", Status.OK,
                "Found " + found.size() + " system browser(s)", Param.builder("found", found), null);
    }

    private Finding handlePlaywrightCacheInspect() {
        Path cacheDir = handlePlaywrightCacheDir();
        Param data = Param.builder("cacheDir", cacheDir.toString());
        if (!Files.isDirectory(cacheDir)) {
            return new Finding("playwright.cache", Status.WARN,
                    "Playwright browser cache not found (bundled chromium unavailable)", data,
                    "Run `mvn exec:java -e -Dexec.mainClass=\"com.microsoft.playwright.CLI\" -Dexec.args=\"install chromium\"` or rely on system Chrome (recommended).");
        }
        try {
            List<String> entries = Files.list(cacheDir)
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.contains("chromium"))
                    .collect(Collectors.toList());
            data.setList("chromiumBuilds", entries);
            if (entries.isEmpty()) {
                return new Finding("playwright.cache", Status.WARN,
                        "Playwright cache has no chromium build", data,
                        "Run playwright install chromium or use system Chrome.");
            }
            return new Finding("playwright.cache", Status.OK,
                    "Playwright bundled chromium available (" + entries.size() + " build(s))", data, null);
        } catch (IOException e) {
            data.setString("error", e.getMessage());
            return new Finding("playwright.cache", Status.WARN,
                    "Failed to read Playwright cache: " + e.getMessage(), data, null);
        }
    }

    private Finding handleLinuxLibsInspect() {
        Path binary = BrowserLauncher.handleSystemBrowserCandidates().stream()
                .filter(Files::exists).findFirst().orElse(null);
        if (binary == null) {
            return new Finding("linux.libs", Status.INFO, "No system browser to ldd-check", null, null);
        }
        try {
            Process process = new ProcessBuilder("ldd", binary.toString())
                    .redirectErrorStream(true).start();
            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line).append('\n');
                }
            }
            process.waitFor(5, TimeUnit.SECONDS);
            String dump = out.toString();
            List<String> missing = new ArrayList<>();
            for (String line : dump.split("\n")) {
                if (line.contains("not found")) {
                    missing.add(line.trim());
                }
            }
            if (!missing.isEmpty()) {
                Param data = Param.builder();
                data.setString("binary", binary.toString());
                data.setList("missing", missing);
                String installCmd = "apt-get install -y " + REQUIRED_LINUX_LIBS.stream()
                        .map(l -> l + "-dev")
                        .collect(Collectors.joining(" "));
                return new Finding("linux.libs", Status.ERROR,
                        "Chromium shared libraries missing — browser will fail to start", data,
                        "apt-get install -y " + installCmd + "  (or your distro's equivalent)");
            }
            return new Finding("linux.libs", Status.OK, "All required shared libraries resolved",
                    Param.builder("binary", binary.toString()), null);
        } catch (Exception cause) {
            return new Finding("linux.libs", Status.INFO, "ldd probe failed: " + cause.getMessage(), null, null);
        }
    }

    private static Path handlePlaywrightCacheDir() {
        String override = System.getenv("PLAYWRIGHT_BROWSERS_PATH");
        if (Strings.isBlank(override) && !"0".equals(override)) {
            return Paths.get(override);
        }
        String home = System.getProperty("user.home");
        if (IS_WINDOWS) {
            String local = System.getenv("LOCALAPPDATA");
            if (Strings.isBlank(local)) {
                return Paths.get(local, "ms-playwright");
            }
            return Paths.get(home, "AppData", "Local", "ms-playwright");
        }
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac")) {
            return Paths.get(home, "Library", "Caches", "ms-playwright");
        }
        return Paths.get(home, ".cache", "ms-playwright");
    }

    private static String handleTrailingStrip(String url) {
        String s = url.trim();
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private static String handlePortPick(String url) {
        int colon = url.lastIndexOf(':');
        if (colon < 0) return "?";
        String tail = url.substring(colon + 1);
        int slash = tail.indexOf('/');
        return slash > 0 ? tail.substring(0, slash) : tail;
    }

    private static String handleBrowserAdviceInstall() {
        if (IS_WINDOWS) {
            return "Install Chrome (https://www.google.com/chrome/) or Edge, or set mateclaw.browser.chrome-path.";
        }
        if (IS_LINUX) {
            return "Install Chrome: `wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | apt-key add - && apt install google-chrome-stable` or `apt install chromium`.";
        }
        return "Install Chrome or Edge, or set mateclaw.browser.chrome-path to point at a browser binary.";
    }

    private static String handleDeriveOverall(List<Finding> findings) {
        boolean hasError = findings.stream().anyMatch(f -> f.status == Status.ERROR);
        boolean hasWarn = findings.stream().anyMatch(f -> f.status == Status.WARN);
        boolean canLaunch = findings.stream().anyMatch(
                f -> f.status == Status.OK && (f.id.equals("system.browsers")
                        || f.id.equals("config.cdp-url") || f.id.equals("config.chrome-path")
                        || f.id.equals("playwright.cache")));
        if (canLaunch && !hasError) return "healthy";
        if (canLaunch) return "warning";
        if (hasError || !canLaunch) return "error";
        return hasWarn ? "warning" : "healthy";
    }

    private static List<String> handleDeriveAdvice(List<Finding> findings) {
        List<String> out = new ArrayList<>();
        for (Finding f : findings) {
            if (f.advice != null && (f.status == Status.ERROR || f.status == Status.WARN)) {
                out.add("[" + f.id + "] " + f.advice);
            }
        }
        if (out.isEmpty()) {
            out.add("Browser stack looks healthy.");
        }
        return out;
    }

    public enum Status {
        OK,
        INFO,
        WARN,
        ERROR
    }

    public static class Finding {
        private final String id;

        private final Status status;

        private final String message;

        private final Param data;

        private final String advice;

        public Finding(String id, Status status, String message, Param data, String advice) {
            this.id = id;
            this.status = status;
            this.message = message;
            this.data = data;
            this.advice = advice;
        }

        public String getId() {
            return id;
        }

        public Status getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public Param getData() {
            return data;
        }

        public String getAdvice() {
            return advice;
        }
    }

    public static class Report {
        private final String overall;

        private final List<Finding> findings;

        private final List<String> advice;

        public Report(String overall, List<Finding> findings, List<String> advice) {
            this.overall = overall;
            this.findings = findings;
            this.advice = advice;
        }

        public String getOverall() {
            return overall;
        }

        public List<Finding> getFindings() {
            return findings;
        }

        public List<String> getAdvice() {
            return advice;
        }
    }
}
