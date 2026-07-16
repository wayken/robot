package cloud.apposs.robot.harness.tool.shell;

import cloud.apposs.react.IoSubscriber;
import cloud.apposs.react.React;
import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.robot.harness.bus.ToolApprovalRequest;
import cloud.apposs.robot.harness.HarnessWorkerProfile;
import cloud.apposs.robot.harness.tool.ITool;
import cloud.apposs.robot.harness.util.Strings;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shell 命令执行工具，支持超时控制、危险命令拦截、工作目录限制和路径遍历防护
 */
public class ShellTool implements ITool {
    public static final String NAME = "exec_shell";

    private static final int DEFAULT_TIMEOUT_SECONDS = 120;
    private static final int DEFAULT_MAX_OUTPUT_LENGTH = 10240;

    /** 默认危险命令拦截规则 */
    private static final List<String> DEFAULT_DENY_PATTERNS = Arrays.asList(
            "\\brm\\s+-[rf]{1,2}\\b",           // rm -r, rm -rf, rm -fr
            "\\bdel\\s+/[fq]\\b",               // del /f, del /q
            "\\brmdir\\s+/s\\b",                // rmdir /s
            "(?:^|[;&|]\\s*)format\\b",         // format (standalone)
            "\\b(mkfs|diskpart)\\b",            // disk operations
            "\\bdd\\s+if=",                     // dd
            ">\\s*/dev/sd",                     // write to disk
            "\\b(shutdown|reboot|poweroff)\\b", // system power
            ":\\(\\)\\s*\\{.*\\};\\s*:"         // fork bomb
    );

    private final int timeout;
    private final String workingDir;
    private final List<Pattern> denyPatterns;
    private final List<Pattern> allowPatterns;
    private final boolean restrictToWorkspace;

    public ShellTool() {
        this(DEFAULT_TIMEOUT_SECONDS, null, null, null, false);
    }

    public ShellTool(String workingDir, HarnessWorkerProfile profile) {
        this(
            DEFAULT_TIMEOUT_SECONDS,
            workingDir,
            null,
            null,
            profile.isWorkspaceRestricted()
        );
    }

    public ShellTool(int timeout, String workingDir, List<String> denyPatterns, List<String> allowPatterns, boolean restrictToWorkspace) {
        this.timeout = timeout;
        this.workingDir = workingDir;
        this.restrictToWorkspace = restrictToWorkspace;

        List<String> denies = (denyPatterns != null) ? denyPatterns : DEFAULT_DENY_PATTERNS;
        this.denyPatterns = new ArrayList<>();
        for (String p : denies) {
            this.denyPatterns.add(Pattern.compile(p, Pattern.CASE_INSENSITIVE));
        }

        this.allowPatterns = new ArrayList<>();
        if (allowPatterns != null) {
            for (String p : allowPatterns) {
                this.allowPatterns.add(Pattern.compile(p, Pattern.CASE_INSENSITIVE));
            }
        }
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Execute a shell command and return its output. Use with caution.";
    }

    @Override
    public Param parameters() {
        String schema = "{"
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "  \"command\":{\"type\":\"string\",\"description\":\"The shell command to execute\"},"
                + "  \"working_dir\":{\"type\":\"string\",\"description\":\"Optional working directory for the command\"}"
                + "},"
                + "\"required\":[\"command\"]"
                + "}";
        return JsonUtil.parseJsonParam(schema);
    }

    @Override
    public React<String> run(String wid, String sid, String rid, Param parameter, IMessageHook messageHook) {
        if (parameter == null) {
            return React.just("Error: parameters must not be null.");
        }
        String command = parameter.getString("command");
        if (Strings.isBlank(command)) {
            return React.just("Error: command must not be empty.");
        }
        String cwd = parameter.getString("working_dir");
        if (Strings.isBlank(cwd)) {
            cwd = (workingDir != null) ? workingDir : System.getProperty("user.dir");
        }
        final String cmd = command.trim();
        final String workDir = cwd.trim();

        // 路径遍历和工作目录限制：直接拒绝，不走审批
        String guardError = handleHardGuard(cmd, workDir);
        if (guardError != null) {
            return React.just(guardError);
        }

        // 检测危险命令：命中则发起非阻塞审批，由客户端用户决定后驱动 React 流继续
        String dangerReason = handleDangerDetect(command);
        if (dangerReason != null) {
            if (messageHook == null) {
                return React.just("Error: Command blocked by safety guard (dangerous pattern detected): " + dangerReason);
            }
            return React.create(subscriber -> {
                @SuppressWarnings("unchecked")
                ToolApprovalRequest request = new ToolApprovalRequest(cmd, dangerReason,
                        (IoSubscriber<String>) subscriber, () -> handleCommandExecute(cmd, workDir));
                try {
                    messageHook.onApprovalRequired(sid, rid, request);
                } catch (Exception e) {
                    subscriber.onNext("Error: Failed to request approval: " + e.getMessage());
                }
            });
        }

        return React.just(handleCommandExecute(command, cwd));
    }

    private String handleCommandExecute(String command, String cwd) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            ProcessBuilder builder;
            if (os.contains("win")) {
                builder = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                builder = new ProcessBuilder("sh", "-c", command);
            }
            builder.directory(new File(cwd));
            builder.redirectErrorStream(false);

            Process process = builder.start();

            // 并发读取 stdout / stderr，防止缓冲区阻塞
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Thread stdoutThread = new Thread(() -> handleStreamRead(process.getInputStream(), stdout));
            Thread stderrThread = new Thread(() -> handleStreamRead(process.getErrorStream(), stderr));
            stdoutThread.start();
            stderrThread.start();

            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                stdoutThread.interrupt();
                stderrThread.interrupt();
                return "Error: Command timed out after " + timeout + " seconds";
            }

            stdoutThread.join(2000);
            stderrThread.join(2000);

            StringBuilder result = new StringBuilder();
            if (stdout.length() > 0) {
                result.append(stdout);
            }
            if (stderr.length() > 0 && !stderr.toString().trim().isEmpty()) {
                if (result.length() > 0) result.append("\n");
                result.append("STDERR:\n").append(stderr);
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                result.append("\nExit code: ").append(exitCode);
            }

            String output = result.length() > 0 ? result.toString() : "(no output)";
            if (output.length() > DEFAULT_MAX_OUTPUT_LENGTH) {
                int extra = output.length() - DEFAULT_MAX_OUTPUT_LENGTH;
                output = output.substring(0, DEFAULT_MAX_OUTPUT_LENGTH) + "\n... (truncated, " + extra + " more chars)";
            }
            return output;

        } catch (Exception e) {
            return "Error executing command: " + e.getMessage();
        }
    }

    private void handleStreamRead(java.io.InputStream is, StringBuilder builder) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 硬性安全守卫：路径遍历和工作目录限制，这类问题直接拒绝，不走审批流程
     */
    private String handleHardGuard(String command, String cwd) {
        if (!restrictToWorkspace) {
            return null;
        }
        if (command.contains("..\\") || command.contains("../")) {
            return "Error: Command blocked by safety guard (path traversal detected)";
        }
        Path cwdPath = Paths.get(cwd).toAbsolutePath().normalize();
        for (String raw : handleAbsolutePathsExtract(command)) {
            try {
                Path path = Paths.get(raw.trim()).toAbsolutePath().normalize();
                if (path.isAbsolute() && !path.startsWith(cwdPath)) {
                    return "Error: Command blocked by safety guard (path outside working dir)";
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * 危险命令检测，命中则发起审批，由客户端用户决定后驱动 React 流继续
     *
     * @param  command 指令
     * @return 返回危险原因描述，未命中则返回 null
     */
    private String handleDangerDetect(String command) {
        String lower = command.toLowerCase();
        for (Pattern pattern : denyPatterns) {
            if (pattern.matcher(lower).find()) {
                return "dangerous pattern matched: " + pattern.pattern();
            }
        }
        if (!allowPatterns.isEmpty()) {
            boolean allowed = false;
            for (Pattern pattern : allowPatterns) {
                if (pattern.matcher(lower).find()) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                return "command not in allowlist";
            }
        }
        return null;
    }

    private static List<String> handleAbsolutePathsExtract(String command) {
        List<String> paths = new ArrayList<>();
        // Windows: C:\...
        Matcher winMatcher = Pattern.compile("[A-Za-z]:\\\\[^\\s\"'|><;]+").matcher(command);
        while (winMatcher.find()) paths.add(winMatcher.group());
        // POSIX: /absolute
        Matcher posixMatcher = Pattern.compile("(?:^|[\\s|>])(/[^\\s\"'>]+)").matcher(command);
        while (posixMatcher.find()) paths.add(posixMatcher.group(1));
        return paths;
    }
}
