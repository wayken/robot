package cloud.apposs.robot.harness.mcp.transport;

import cloud.apposs.logger.Logger;
import cloud.apposs.robot.harness.mcp.McpSchema;
import cloud.apposs.robot.harness.mcp.McpTransport;
import cloud.apposs.robot.harness.mcp.schema.*;
import cloud.apposs.util.Param;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于 HTTP SSE 的 MCP 客户端，支持 MCP SSE 传输协议：
 * <pre>
 *   1. GET {url} — 建立 SSE 长连接，接收服务器推送的 JSON-RPC 响应
 *   2. POST {endpoint} — 发送 JSON-RPC 请求（endpoint 由 SSE 连接建立后服务器通过 endpoint 事件下发）
 * </pre>
 * 典型配置示例：
 * <pre>
 *   name: my-server
 *   type: sse
 *   url: http://localhost:3000/sse
 * </pre>
 */
public class McpSseTransport implements McpTransport {
    private static final String CLIENT_NAME = "harness-worker";
    private static final String CLIENT_VERSION = "1.0.0";
    private static final String CHARSET = "UTF-8";

    private final String serverName;
    private final String sseUrl;
    private final int timeoutSeconds;

    // 服务器通过 SSE endpoint 事件下发的 POST 端点 URL
    private volatile String postEndpoint;

    // SSE 连接建立完成的信号（收到 endpoint 事件后触发）
    private final CountDownLatch endpointLatch = new CountDownLatch(1);

    // 请求 ID 自增器
    private final AtomicLong idCounter = new AtomicLong(1);

    // 等待中的请求：id -> PendingRequest
    private final Map<Long, PendingRequest> pendingRequests = new ConcurrentHashMap<Long, PendingRequest>();

    // SSE 读取后台线程
    private Thread sseThread;

    // 客户端是否已关闭
    private volatile boolean closed = false;

    // SSE 连接句柄，用于关闭时断开
    private volatile HttpURLConnection sseConnection;

    public McpSseTransport(String serverName, String sseUrl, int timeoutSeconds) {
        this.serverName = serverName;
        this.sseUrl = sseUrl;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public void initialize() throws Exception {
        // 启动 SSE 后台连接线程
        sseThread = new Thread(() -> {
            handleSseLoop();
        }, "mcp-sse-reader-" + serverName);
        sseThread.setDaemon(true);
        sseThread.start();
        // 等待服务器下发 endpoint 事件（最多等 timeoutSeconds 秒）
        boolean received = endpointLatch.await(timeoutSeconds, TimeUnit.SECONDS);
        if (!received || postEndpoint == null) {
            close();
            throw new IOException("MCP SSE [" + serverName + "] did not receive endpoint event within " + timeoutSeconds + "s");
        }
        // 发送 initialize 请求
        long id = idCounter.getAndIncrement();
        Param params = McpSchema.buildInitializeParams(CLIENT_NAME, CLIENT_VERSION);
        JsonRpcRequest initReq = new JsonRpcRequest(id, "initialize", params);
        JsonRpcResponse initResp = sendRequest(id, initReq.toJson());
        if (initResp == null || initResp.isError()) {
            String errMsg = initResp != null && initResp.getError() != null
                    ? initResp.getError().getString("message", "unknown error") : "no response";
            throw new IOException("MCP SSE [" + serverName + "] initialize failed: " + errMsg);
        }
        // 发送 initialized 通知
        JsonRpcNotification notification = new JsonRpcNotification("notifications/initialized", null);
        sendPost(notification.toJson());
        Logger.info("McpSseClient: server [%s] initialized successfully via SSE", serverName);
    }

    @Override
    public ListToolsResult listTools() throws Exception {
        long id = idCounter.getAndIncrement();
        JsonRpcRequest request = new JsonRpcRequest(id, "tools/list", null);
        JsonRpcResponse response = sendRequest(id, request.toJson());
        if (response == null) {
            throw new IOException("MCP SSE [" + serverName + "] tools/list: no response");
        }
        if (response.isError()) {
            String errMsg = response.getError().getString("message", "unknown error");
            throw new IOException("MCP SSE [" + serverName + "] tools/list failed: " + errMsg);
        }
        return ListToolsResult.fromParam(response.getResult());
    }

    @Override
    public CallToolResult callTool(CallToolRequest request) throws Exception {
        long id = idCounter.getAndIncrement();
        JsonRpcRequest rpcRequest = new JsonRpcRequest(id, "tools/call", request.toParams());
        JsonRpcResponse rpcResponse = sendRequest(id, rpcRequest.toJson());
        if (rpcResponse == null) {
            throw new IOException("MCP SSE [" + serverName + "] tools/call [" + request.getName() + "]: no response");
        }
        if (rpcResponse.isError()) {
            String errMsg = rpcResponse.getError().getString("message", "unknown error");
            throw new IOException("MCP SSE [" + serverName + "] tools/call [" + request.getName() + "] failed: " + errMsg);
        }
        return CallToolResult.fromParam(rpcResponse.getResult());
    }

    @Override
    public void close() {
        closed = true;
        // 唤醒所有等待中的请求
        for (PendingRequest pending : pendingRequests.values()) {
            pending.latch.countDown();
        }
        pendingRequests.clear();
        endpointLatch.countDown();
        if (sseConnection != null) {
            sseConnection.disconnect();
        }
        if (sseThread != null) {
            sseThread.interrupt();
        }
    }

    // 发送 JSON-RPC 请求并同步等待 SSE 响应
    private JsonRpcResponse sendRequest(long id, String json) throws Exception {
        PendingRequest pending = new PendingRequest();
        pendingRequests.put(id, pending);
        try {
            sendPost(json);
            boolean received = pending.latch.await(timeoutSeconds, TimeUnit.SECONDS);
            if (!received) {
                throw new IOException("MCP SSE [" + serverName + "] request timeout (id=" + id + ")");
            }
            return pending.response;
        } finally {
            pendingRequests.remove(id);
        }
    }

    // 向 postEndpoint 发送 JSON-RPC 消息（POST application/json）
    private void sendPost(String json) throws IOException {
        URL url = new URL(postEndpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(timeoutSeconds * 1000);
        connection.setReadTimeout(timeoutSeconds * 1000);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json, text/event-stream");
        byte[] body = json.getBytes(CHARSET);
        connection.setRequestProperty("Content-Length", String.valueOf(body.length));
        OutputStream os = connection.getOutputStream();
        try {
            os.write(body);
            os.flush();
        } finally {
            os.close();
        }
        int status = connection.getResponseCode();
        connection.disconnect();
        if (status >= 400) {
            throw new IOException("MCP SSE [" + serverName + "] POST failed with status " + status);
        }
    }

    // SSE 后台读取主循环，持续读取服务器推送的事件并分发
    private void handleSseLoop() {
        try {
            URL url = new URL(sseUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeoutSeconds * 1000);
            // SSE 长连接，读超时设为 0（无限等待）
            connection.setReadTimeout(0);
            connection.setRequestProperty("Accept", "text/event-stream");
            connection.setRequestProperty("Cache-Control", "no-cache");
            sseConnection = connection;
            int status = connection.getResponseCode();
            if (status != 200) {
                Logger.error("McpSseClient [%s] SSE connection failed with status %d", serverName, status);
                endpointLatch.countDown();
                return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), CHARSET));
            try {
                handleSseEventLoop(reader);
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            if (!closed) {
                Logger.warn("McpSseClient [%s] SSE loop error: %s", serverName, e.getMessage());
            }
        } finally {
            // 确保 endpointLatch 被释放，避免 initialize() 永久阻塞
            endpointLatch.countDown();
            // 唤醒所有等待中的请求
            for (PendingRequest pending : pendingRequests.values()) {
                pending.latch.countDown();
            }
        }
    }

    /**
     * 解析 SSE 事件流，格式：
     * <pre>
     *   event: endpoint
     *   data: /messages?sessionId=xxx
     *
     *   event: message
     *   data: {"jsonrpc":"2.0","id":1,"result":{...}}
     * </pre>
     */
    private void handleSseEventLoop(BufferedReader reader) throws IOException {
        String eventType = null;
        StringBuilder dataBuffer = new StringBuilder();
        String readLine;
        while (!closed && (readLine = reader.readLine()) != null) {
            if (readLine.isEmpty()) {
                // 空行表示一个 SSE 事件结束
                if (dataBuffer.length() > 0) {
                    handleSseEvent(eventType, dataBuffer.toString().trim());
                    eventType = null;
                    dataBuffer.setLength(0);
                }
                continue;
            }
            if (readLine.startsWith("event:")) {
                eventType = readLine.substring(6).trim();
            } else if (readLine.startsWith("data:")) {
                if (dataBuffer.length() > 0) {
                    dataBuffer.append('\n');
                }
                dataBuffer.append(readLine.substring(5).trim());
            }
            // 忽略 id: 和 : (comment) 行
        }
        // 处理最后一个未以空行结尾的事件
        if (dataBuffer.length() > 0) {
            handleSseEvent(eventType, dataBuffer.toString().trim());
        }
    }

    // 处理单个 SSE 事件
    private void handleSseEvent(String eventType, String data) {
        if (data == null || data.isEmpty()) {
            return;
        }
        if ("endpoint".equals(eventType)) {
            // 服务器下发 POST 端点，可能是相对路径，需要拼接 base URL
            postEndpoint = handleEndpointResolve(data);
            Logger.info("McpSseClient [%s] received endpoint: %s", serverName, postEndpoint);
            endpointLatch.countDown();
            return;
        }
        // message 事件：JSON-RPC 响应
        try {
            JsonRpcResponse response = JsonRpcResponse.parse(data);
            if (response == null) {
                return;
            }
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
            Logger.warn("McpSseClient [%s] failed to dispatch SSE event: %s", serverName, e.getMessage());
        }
    }

    // 将服务器下发的 endpoint 解析为完整 URL，如果是相对路径则拼接 sseUrl 的 base（scheme + host + port）
    private String handleEndpointResolve(String endpoint) {
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            return endpoint;
        }
        // 提取 base URL（去掉 sseUrl 的路径部分）
        try {
            URL base = new URL(sseUrl);
            int port = base.getPort();
            String portStr = port > 0 ? ":" + port : "";
            String baseUrl = base.getProtocol() + "://" + base.getHost() + portStr;
            if (!endpoint.startsWith("/")) {
                endpoint = "/" + endpoint;
            }
            return baseUrl + endpoint;
        } catch (Exception e) {
            return endpoint;
        }
    }

    // 等待中的请求容器
    private static class PendingRequest {
        final CountDownLatch latch = new CountDownLatch(1);
        volatile JsonRpcResponse response;
    }
}
