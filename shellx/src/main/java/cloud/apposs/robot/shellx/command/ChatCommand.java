package cloud.apposs.robot.shellx.command;

import cloud.apposs.robot.shellx.ShellXContext;
import cloud.apposs.robot.shellx.cli.CliCommand;
import cloud.apposs.robot.shellx.cli.CliOption;
import cloud.apposs.robot.shellx.cli.CliParser;
import cloud.apposs.robot.shellx.session.ChatSession;
import cloud.apposs.robot.shellx.session.SessionManager;
import cloud.apposs.robot.shellx.slash.*;
import cloud.apposs.robot.shellx.tui.TuiEngine;
import cloud.apposs.robot.shellx.util.AnsiColor;

import java.util.Arrays;
import java.util.List;

/**
 * chat 子命令 - 交互式/非交互式 AI 聊天会话
 * <pre>
 * 用法：
 *   shellx chat                          开启交互式 TUI
 *   shellx chat "prompt"                 带首条问题启动
 *   shellx chat --no-interactive "prompt" 非交互模式
 *   shellx chat --resume                 恢复最近会话
 *   shellx chat --list-sessions          列出会话
 *   shellx chat --delete-session <ID>    删除会话
 * </pre>
 */
public class ChatCommand implements CliCommand {
    @Override
    public String name() {
        return "chat";
    }

    @Override
    public String description() {
        return "Start an interactive or non-interactive AI chat session";
    }

    @Override
    public List<CliOption> options() {
        return Arrays.asList(
            CliOption.flag("no-interactive", null, "Run in non-interactive (headless) mode"),
            CliOption.flag("resume", "r", "Resume the most recent session in current directory"),
            CliOption.flag("resume-picker", null, "Pick a session to resume"),
            CliOption.option("resume-id", null, "Resume session by ID"),
            CliOption.flag("list-sessions", null, "List sessions in current directory"),
            CliOption.option("delete-session", null, "Delete session by ID"),
            CliOption.option("agent", null, "Use specified agent"),
            CliOption.flag("trust-all-tools", null, "Trust all tools without confirmation"),
            CliOption.option("trust-tools", null, "Comma-separated list of trusted tools"),
            CliOption.option("effort", null, "Set reasoning effort (low/medium/high)", "medium"),
            CliOption.option("wrap", null, "Line wrap mode (always/never/auto)", "auto"),
            CliOption.flag("list-models", null, "List available models"),
            CliOption.flag("require-mcp-startup", null, "Exit if MCP servers fail to start")
        );
    }

    @Override
    public int execute(ShellXContext context, CliParser parser) throws Exception {
        // 处理 --list-sessions
        if (parser.hasFlag("list-sessions")) {
            return listSessions(context);
        }

        // 处理 --delete-session
        String deleteId = parser.getOption("delete-session");
        if (deleteId != null) {
            return deleteSession(context, deleteId);
        }

        // 处理 --list-models
        if (parser.hasFlag("list-models")) {
            return listModels(context);
        }

        // 处理 --agent
        String agentName = parser.getOption("agent");
        if (agentName != null) {
            context.setActiveAgentId(agentName);
        }

        // 处理 --effort
        String effort = parser.getOption("effort");
        if (effort != null) {
            context.setEffort(effort);
        }

        // 处理 --trust-all-tools
        if (parser.hasFlag("trust-all-tools")) {
            context.setTrustAllTools(true);
        }

        // 处理 --no-interactive
        boolean nonInteractive = parser.hasFlag("no-interactive");
        context.setNonInteractive(nonInteractive);

        // 获取或创建会话
        ChatSession session = resolveSession(context, parser);
        if (session == null) {
            return 1;
        }

        // 构建 SlashRegistry
        SlashRegistry slashRegistry = buildSlashRegistry();
        TuiEngine engine = new TuiEngine(context, slashRegistry);

        if (nonInteractive) {
            // 非交互模式：从位置参数或管道获取输入
            String prompt = getPrompt(parser);
            if (prompt == null || prompt.isEmpty()) {
                System.err.println("Error: No prompt provided for non-interactive mode.");
                System.err.println("Usage: shellx chat --no-interactive \"your prompt\"");
                return 1;
            }
            String response = engine.executeNonInteractive(session, prompt);
            if (response != null) {
                System.out.println(response);
            }
        } else {
            // 交互模式
            String initialPrompt = getPrompt(parser);
            if (initialPrompt != null && !initialPrompt.isEmpty()) {
                // 先执行初始 prompt，然后进入交互模式
                engine.start(session);
            } else {
                engine.start(session);
            }
        }

        // 保存会话
        context.getSessionManager().saveSession(session);
        return 0;
    }

    private ChatSession resolveSession(ShellXContext context, CliParser parser) {
        SessionManager sm = context.getSessionManager();
        String cwd = System.getProperty("user.dir");

        // --resume: 恢复最近会话
        if (parser.hasFlag("resume")) {
            ChatSession session = sm.getLatestSession(cwd);
            if (session != null) {
                System.out.println(AnsiColor.info("Resuming session: " + session.getId()));
                return session;
            }
            System.out.println(AnsiColor.warn("No previous session found in current directory."));
            // 创建新会话
        }

        // --resume-id: 按 ID 恢复
        String resumeId = parser.getOption("resume-id");
        if (resumeId != null) {
            ChatSession session = sm.getSession(resumeId);
            if (session != null) {
                System.out.println(AnsiColor.info("Resuming session: " + session.getId()));
                return session;
            }
            System.err.println(AnsiColor.error("Session not found: " + resumeId));
            return null;
        }

        // 创建新会话
        return sm.createSession(context.getActiveAgentId(), cwd);
    }

    private int listSessions(ShellXContext context) {
        String cwd = System.getProperty("user.dir");
        List<ChatSession> sessions = context.getSessionManager().listSessionsByDir(cwd);
        if (sessions.isEmpty()) {
            System.out.println("No sessions found in current directory.");
            return 0;
        }
        System.out.println(AnsiColor.bold("Sessions in " + cwd + ":"));
        System.out.println();
        for (ChatSession s : sessions) {
            String status = s.isActive() ? AnsiColor.green("●") : AnsiColor.dim("○");
            System.out.printf("  %s %s  agent=%-12s  %s%n",
                    status, s.getId(), s.getAgentId(), s.getFormattedLastActiveAt());
        }
        return 0;
    }

    private int deleteSession(ShellXContext context, String sessionId) {
        boolean deleted = context.getSessionManager().deleteSession(sessionId);
        if (deleted) {
            System.out.println(AnsiColor.success("Session deleted: " + sessionId));
        } else {
            System.err.println(AnsiColor.error("Session not found: " + sessionId));
            return 1;
        }
        return 0;
    }

    private int listModels(ShellXContext context) {
        // TODO: 从 agent profile 中读取可用模型
        System.out.println(AnsiColor.bold("Available Models:"));
        System.out.println("  (configured via agent profile.yaml)");
        return 0;
    }

    private String getPrompt(CliParser parser) {
        List<String> positional = parser.getPositionalArgs();
        if (!positional.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String arg : positional) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(arg);
            }
            return sb.toString();
        }
        return null;
    }

    private SlashRegistry buildSlashRegistry() {
        SlashRegistry registry = new SlashRegistry();
        registry.register(new HelpSlash(registry));
        registry.register(new QuitSlash());
        registry.register(new ClearSlash());
        registry.register(new ModelSlash());
        registry.register(new SessionIdSlash());
        registry.register(new CompactSlash());
        registry.register(new EffortSlash());
        registry.register(new UsageSlash());
        return registry;
    }
}
