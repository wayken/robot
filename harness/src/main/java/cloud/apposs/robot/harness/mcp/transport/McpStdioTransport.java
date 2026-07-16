package cloud.apposs.robot.harness.mcp.transport;

import cloud.apposs.logger.Logger;
import cloud.apposs.robot.harness.mcp.McpSchema;
import cloud.apposs.robot.harness.mcp.McpTransport;
import cloud.apposs.robot.harness.mcp.schema.*;
import cloud.apposs.util.Param;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于 stdio 的 MCP 客户端，通过子进程标准输入输出与 MCP 服务器通信，协议为 JSON-RPC 2.0，每行一个 JSON 消息
 * <p>典型配置示例：
 * <pre>
 *   name: filesystem
 *   type: stdio
 *   command: npx
 *   args: ["-y", "@modelcontextprotocol/server-filesystem", "/workspace"]
 * </pre>
 */
public class McpStdioTransport implements McpTransport {
    private static final String CLIENT_NAME = "harness-worker";
    private static final String CLIENT_VERSION = "1.0.0";

    private final String serverName;
    private final String command;
    private final List<String> arguments;
    private final Map<String, String> environment;
    private final int timeoutSeconds;

    // 子进程
    private Process process;

    // 向子进程写入 JSON-RPC 消息
    private BufferedWriter writer;

    // 从子进程读取 JSON-RPC 消息的后台线程
    private Thread readerThread;

    // 请求 ID 自增器
    private final AtomicLong idCounter = new AtomicLong(1);

    // 等待中的请求：id -> PendingRequest，后台读取线程收到响应后通过 CountDownLatch 唤醒等待线程
    private final Map<Long, PendingRequest> pendingRequests = new ConcurrentHashMap<Long, PendingRequest>();

    // 客户端是否已关闭
    private volatile boolean closed = false;

    public McpStdioTransport(String serverName, String command,
                             List<String> arguments, Map<String, String> environment, int timeoutSeconds) {
        this.serverName = serverName;
        this.command = command;
        this.arguments = arguments != null ? arguments : new ArrayList<String>();
        this.environment = environment;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public void initialize() throws Exception {
        // 启动子进程
        List<String> cmd = new ArrayList<String>();
        cmd.add(command);
        cmd.addAll(arguments);
        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.redirectErrorStream(false);
        if (environment != null && !environment.isEmpty()) {
            builder.environment().putAll(environment);
        }
        process = builder.start();
        writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        // 启动后台读取线程，持续读取子进程 stdout
        readerThread = new Thread(() -> {
            handleReaderLoop();
        }, "mcp-stdio-reader-" + serverName);
        readerThread.setDaemon(true);
        readerThread.start();
        // 发送 initialize 请求
        long id = idCounter.getAndIncrement();
        Param params = McpSchema.buildInitializeParams(CLIENT_NAME, CLIENT_VERSION);
        JsonRpcRequest initReq = new JsonRpcRequest(id, "initialize", params);
        JsonRpcResponse initResp = sendRequest(id, initReq.toJson());
        if (initResp == null || initResp.isError()) {
            String errMsg = initResp != null && initResp.getError() != null
                    ? initResp.getError().getString("message", "unknown error") : "no response";
            throw new IOException("MCP stdio [" + serverName + "] initialize failed: " + errMsg);
        }
        // 发送 initialized 通知（无需等待响应）
        JsonRpcNotification notification = new JsonRpcNotification("notifications/initialized", null);
        sendNotification(notification.toJson());
        Logger.info("McpStdioClient: server [%s] initialized successfully", serverName);
    }

    @Override
    public ListToolsResult listTools() throws Exception {
        long id = idCounter.getAndIncrement();
        JsonRpcRequest req = new JsonRpcRequest(id, "tools/list", null);
        JsonRpcResponse resp = sendRequest(id, req.toJson());
        if (resp == null) {
            throw new IOException("MCP stdio [" + serverName + "] tools/list: no response");
        }
        if (resp.isError()) {
            String errMsg = resp.getError().getString("message", "unknown error");
            throw new IOException("MCP stdio [" + serverName + "] tools/list failed: " + errMsg);
        }
        return ListToolsResult.fromParam(resp.getResult());
    }

    @Override
    public CallToolResult callTool(CallToolRequest request) throws Exception {
        long id = idCounter.getAndIncrement();
        JsonRpcRequest req = new JsonRpcRequest(id, "tools/call", request.toParams());
        JsonRpcResponse resp = sendRequest(id, req.toJson());
        if (resp == null) {
            throw new IOException("MCP stdio [" + serverName + "] tools/call [" + request.getName() + "]: no response");
        }
        if (resp.isError()) {
            String errMsg = resp.getError().getString("message", "unknown error");
            throw new IOException("MCP stdio [" + serverName + "] tools/call [" + request.getName() + "] failed: " + errMsg);
        }
        return CallToolResult.fromParam(resp.getResult());
    }

    @Override
    public void close() {
        closed = true;
        // 唤醒所有等待中的请求，避免线程泄漏
        for (PendingRequest pending : pendingRequests.values()) {
            pending.latch.countDown();
        }
        pendingRequests.clear();
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {
            }
        }
        if (process != null) {
            process.destroy();
        }
        if (readerThread != null) {
            readerThread.interrupt();
        }
    }

    // 发送 JSON-RPC 请求并同步等待响应
    private JsonRpcResponse sendRequest(long id, String json) throws Exception {
        PendingRequest pending = new PendingRequest();
        pendingRequests.put(id, pending);
        try {
            writeLine(json);
            boolean received = pending.latch.await(timeoutSeconds, TimeUnit.SECONDS);
            if (!received) {
                throw new IOException("MCP stdio [" + serverName + "] request timeout (id=" + id + ")");
            }
            return pending.response;
        } finally {
            pendingRequests.remove(id);
        }
    }

    // 发送 JSON-RPC 通知（不等待响应）
    private void sendNotification(String json) throws IOException {
        writeLine(json);
    }

    private synchronized void writeLine(String json) throws IOException {
        writer.write(json);
        writer.newLine();
        writer.flush();
    }

    // 后台读取线程主循环，持续从子进程 stdout 读取 JSON-RPC 消息并分发给等待中的请求
    private void handleReaderLoop() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String readLine;
            while (!closed && (readLine = reader.readLine()) != null) {
                readLine = readLine.trim();
                if (readLine.isEmpty()) {
                    continue;
                }
                handleMessageDispatch(readLine);
            }
        } catch (IOException e) {
            if (!closed) {
                Logger.warn("McpStdioClient [%s] reader loop error: %s", serverName, e.getMessage());
            }
        } finally {
            // 进程退出时唤醒所有等待中的请求
            for (PendingRequest pending : pendingRequests.values()) {
                pending.latch.countDown();
            }
        }
    }

    // 解析并分发收到的 JSON-RPC 消息
    private void handleMessageDispatch(String json) {
        try {
            JsonRpcResponse response = JsonRpcResponse.parse(json);
            if (response == null) {
                return;
            }
            // 通知消息（无 id 或 id=0）直接忽略
            long id = response.getId();
            if (id <= 0) {
                return;
            }
            PendingRequest pending = pendingRequests.get(id);
            if (pending != null) {
                pending.response = response;
                pending.latch.countDown();
            }
        } catch (Exception e) {
            Logger.warn("McpStdioClient [%s] failed to dispatch message: %s", serverName, e.getMessage());
        }
    }

    // 等待中的请求容器
    private static class PendingRequest {
        final CountDownLatch latch = new CountDownLatch(1);
        volatile JsonRpcResponse response;
    }
}
