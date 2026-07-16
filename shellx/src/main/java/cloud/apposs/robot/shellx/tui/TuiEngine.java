package cloud.apposs.robot.shellx.tui;

import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.robot.harness.provider.AIResponse;
import cloud.apposs.robot.harness.struct.MessageStruct;
import cloud.apposs.robot.shellx.ShellXContext;
import cloud.apposs.robot.shellx.session.ChatSession;
import cloud.apposs.robot.shellx.slash.ISlashCommand;
import cloud.apposs.robot.shellx.slash.SlashRegistry;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TUI 交互引擎，负责主循环：读取输入 → 解析 → 执行 → 渲染输出
 */
public class TuiEngine {
    private final ShellXContext context;
    private final TuiRenderer renderer;
    private final TuiInput input;
    private final SlashRegistry slashRegistry;

    /** 当前会话 */
    private ChatSession currentSession;

    /** 是否正在运行 */
    private volatile boolean running = true;

    public TuiEngine(ShellXContext context, SlashRegistry slashRegistry) {
        this.context = context;
        this.renderer = new TuiRenderer(context.getConfig().isColorEnabled());
        this.input = new TuiInput();
        this.slashRegistry = slashRegistry;
    }

    /**
     * 启动交互式 TUI
     */
    public void start(ChatSession session) {
        this.currentSession = session;
        renderer.printBanner();
        renderer.printWelcome(context.getActiveAgentId());

        while (running) {
            renderer.printPrompt();
            String userInput;
            try {
                userInput = input.readLine();
            } catch (IOException e) {
                renderer.printError("Failed to read input: " + e.getMessage());
                break;
            }

            // EOF（Ctrl+D）
            if (userInput == null) {
                running = false;
                break;
            }

            userInput = userInput.trim();
            if (userInput.isEmpty()) {
                continue;
            }

            // 处理 Slash Commands
            if (userInput.startsWith("/")) {
                handleSlashCommand(userInput);
                continue;
            }

            // 发送到 AI Agent
            handleChat(userInput);
        }

        // 保存会话
        if (currentSession != null) {
            context.getSessionManager().saveSession(currentSession);
        }
        renderer.printInfo("Session saved. Goodbye!");
    }

    /**
     * 非交互模式执行单条消息
     */
    public String executeNonInteractive(ChatSession session, String message) {
        this.currentSession = session;
        return doChat(message);
    }

    /**
     * 处理 Slash Command
     */
    private void handleSlashCommand(String input) {
        // 解析命令名和参数
        String[] parts = input.split("\\s+", 2);
        String commandName = parts[0]; // 如 "/help"
        String args = parts.length > 1 ? parts[1] : "";

        ISlashCommand command = slashRegistry.getCommand(commandName);
        if (command == null) {
            renderer.printError("Unknown command: " + commandName + ". Type /help for available commands.");
            return;
        }

        // 检查是否为退出命令
        if ("/quit".equals(commandName) || "/exit".equals(commandName) || "/q".equals(commandName)) {
            running = false;
            return;
        }

        String result = command.execute(context, currentSession, args);
        if (result != null && !result.isEmpty()) {
            System.out.println(result);
        }
    }

    /**
     * 处理聊天消息
     */
    private void handleChat(String message) {
        String response = doChat(message);
        if (response != null) {
            renderer.printAIResponse(response);
        }
    }

    /**
     * 执行聊天，发送消息到 AI Agent 并返回回复
     */
    private String doChat(String message) {
        HarnessWorker worker = context.getActiveWorker();
        if (worker == null) {
            renderer.printError("No active agent found. Use '/agent list' to see available agents.");
            return null;
        }

        currentSession.touch();

        // 构建消息
        MessageStruct msg = new MessageStruct();
        msg.setWid(context.getActiveAgentId());
        msg.setSid(currentSession.getId());
        msg.setRid(generateRequestId());
        msg.setMessage(message);

        // 使用 spinner 指示正在处理
        TuiSpinner spinner = new TuiSpinner("Thinking...");
        spinner.start();

        final AtomicReference<String> responseRef = new AtomicReference<>();
        final AtomicReference<Throwable> errorRef = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        try {
            worker.work(msg, new IMessageHook() {
                @Override
                public void onProcessing(String sid, String rid, AIResponse response) {
                    // 流式处理中间状态
                }

                @Override
                public void onCompletion(String sid, String rid, AIResponse response) {
                    spinner.stop();
                    if (response != null && response.getContent() != null) {
                        responseRef.set(response.getContent());
                    }
                    latch.countDown();
                }

                @Override
                public void onError(Throwable cause) {
                    spinner.stop();
                    errorRef.set(cause);
                    latch.countDown();
                }
            });

            // 等待响应完成
            latch.await();
        } catch (Exception e) {
            spinner.stop();
            renderer.printError("Chat error: " + e.getMessage());
            return null;
        }

        if (errorRef.get() != null) {
            renderer.printError("AI error: " + errorRef.get().getMessage());
            return null;
        }

        return responseRef.get();
    }

    public void stop() {
        running = false;
    }

    public ChatSession getCurrentSession() {
        return currentSession;
    }

    public TuiRenderer getRenderer() {
        return renderer;
    }

    private String generateRequestId() {
        return "req_" + System.currentTimeMillis();
    }
}
