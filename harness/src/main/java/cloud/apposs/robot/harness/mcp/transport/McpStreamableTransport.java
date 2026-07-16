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
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于 HTTP Streamable 的 MCP 客户端，支持 MCP Streamable HTTP 传输协议（MCP 2025-03-26 规范）：
 * <pre>
 *   1. POST {url} — 发送 JSON-RPC 请求，响应可以是普通 JSON 或 SSE 流
 * </pre>
 * 与 SSE 传输的区别：Streamable HTTP 不需要预先建立 SSE 长连接，每次请求直接 POST，服务器可以选择以 SSE 流或普通 JSON 响应
 *
 * <p>典型配置示例：
 * <pre>
 *   name: my-server
 *   type: streamable
 *   url: http://localhost:3000/mcp
 * </pre>
 */
public class McpStreamableTransport implements McpTransport {
    private static final String CLIENT_NAME = "harness-worker";
    private static final String CLIENT_VERSION = "1.0.0";
    private static final String CHARSET = "UTF-8";
    private static final String CONTENT_TYPE_SSE = "text/event-stream";

    private final String serverName;
    private final String url;
    private final int timeoutSeconds;

    // 请求 ID 自增器
    private final AtomicLong idCounter = new AtomicLong(1);

    // 客户端是否已关闭
    private volatile boolean closed = false;

    // initialize 握手后服务器下发的 session ID，后续所有请求必须携带
    private volatile String sessionId = null;

    public McpStreamableTransport(String serverName, String url, int timeoutSeconds) {
        this.serverName = serverName;
        this.url = url;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public void initialize() throws Exception {
        // 发送 initialize 请求
        long id = idCounter.getAndIncrement();
        Param params = McpSchema.buildInitializeParams(CLIENT_NAME, CLIENT_VERSION);
        JsonRpcRequest rpcRequest = new JsonRpcRequest(id, "initialize", params);
        JsonRpcResponse rpcResponse = sendRequest(rpcRequest.toJson());
        if (rpcResponse == null || rpcResponse.isError()) {
            String message = rpcResponse != null && rpcResponse.getError() != null
                    ? rpcResponse.getError().getString("message", "unknown error") : "no response";
            throw new IOException("MCP Streamable [" + serverName + "] initialize failed: " + message);
        }
        // 发送 initialized 通知
        JsonRpcNotification notification = new JsonRpcNotification("notifications/initialized", null);
        sendNotification(notification.toJson());
        Logger.info("McpStreamableHttpClient: server [%s] initialized successfully", serverName);
    }

    @Override
    public ListToolsResult listTools() throws Exception {
        long id = idCounter.getAndIncrement();
        JsonRpcRequest rpcRequest = new JsonRpcRequest(id, "tools/list", null);
        JsonRpcResponse rpcResponse = sendRequest(rpcRequest.toJson());
        if (rpcResponse == null) {
            throw new IOException("MCP Streamable [" + serverName + "] tools/list: no response");
        }
        if (rpcResponse.isError()) {
            String errMsg = rpcResponse.getError().getString("message", "unknown error");
            throw new IOException("MCP Streamable [" + serverName + "] tools/list failed: " + errMsg);
        }
        return ListToolsResult.fromParam(rpcResponse.getResult());
    }

    @Override
    public CallToolResult callTool(CallToolRequest request) throws Exception {
        long id = idCounter.getAndIncrement();
        JsonRpcRequest rpcRequest = new JsonRpcRequest(id, "tools/call", request.toParams());
        JsonRpcResponse rpcResponse = sendRequest(rpcRequest.toJson());
        if (rpcResponse == null) {
            throw new IOException("MCP Streamable [" + serverName + "] tools/call [" + request.getName() + "]: no response");
        }
        if (rpcResponse.isError()) {
            String message = rpcResponse.getError().getString("message", "unknown error");
            throw new IOException("MCP Streamable [" + serverName + "] tools/call [" + request.getName() + "] failed: " + message);
        }
        return CallToolResult.fromParam(rpcResponse.getResult());
    }

    @Override
    public void close() {
        closed = true;
    }

    // 发送 JSON-RPC 请求，自动处理 JSON 和 SSE 两种响应格式
    private JsonRpcResponse sendRequest(String data) throws Exception {
        HttpURLConnection connection = openConnection();
        try {
            byte[] body = data.getBytes(CHARSET);
            connection.setRequestProperty("Content-Length", String.valueOf(body.length));
            OutputStream os = connection.getOutputStream();
            try {
                os.write(body);
                os.flush();
            } finally {
                os.close();
            }
            int status = connection.getResponseCode();
            if (status >= 400) {
                throw new IOException("MCP Streamable [" + serverName + "] POST failed with status " + status);
            }
            // 每次响应都尝试提取 Mcp-Session-Id（initialize 时服务器会下发）
            String newSessionId = connection.getHeaderField("Mcp-Session-Id");
            if (newSessionId != null && !newSessionId.isEmpty()) {
                sessionId = newSessionId;
            }
            // 204 No Content — 通知类请求的正常响应
            if (status == 204) {
                return null;
            }
            String contentType = connection.getContentType();
            if (contentType != null && contentType.toLowerCase().contains(CONTENT_TYPE_SSE)) {
                return handleSseResponse(connection);
            }
            return handleJsonResponse(connection);
        } finally {
            connection.disconnect();
        }
    }

    // 发送通知（不等待响应）
    private void sendNotification(String json) throws IOException {
        HttpURLConnection connection = openConnection();
        try {
            byte[] body = json.getBytes(CHARSET);
            connection.setRequestProperty("Content-Length", String.valueOf(body.length));
            OutputStream os = connection.getOutputStream();
            try {
                os.write(body);
                os.flush();
            } finally {
                os.close();
            }
            connection.getResponseCode(); // 触发请求发送
        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection openConnection() throws IOException {
        URL target = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) target.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setReadTimeout(timeoutSeconds * 1000);
        connection.setConnectTimeout(timeoutSeconds * 1000);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json, text/event-stream");
        // 握手完成后携带 session ID，服务器用它识别会话
        if (sessionId != null && !sessionId.isEmpty()) {
            connection.setRequestProperty("Mcp-Session-Id", sessionId);
        }
        return connection;
    }

    // 读取普通 JSON 响应
    private JsonRpcResponse handleJsonResponse(HttpURLConnection conn) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), CHARSET));
        try {
            StringBuilder builder = new StringBuilder();
            String readLine;
            while ((readLine = reader.readLine()) != null) {
                builder.append(readLine);
            }
            return JsonRpcResponse.parse(builder.toString().trim());
        } finally {
            reader.close();
        }
    }

    // 读取 SSE 流响应，提取第一个 message 事件的 data 作为 JSON-RPC 响应
    private JsonRpcResponse handleSseResponse(HttpURLConnection connection) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), CHARSET));
        try {
            String eventType = null;
            StringBuilder dataBuffer = new StringBuilder();
            String readLine;
            while ((readLine = reader.readLine()) != null) {
                if (readLine.isEmpty()) {
                    if (dataBuffer.length() > 0) {
                        String data = dataBuffer.toString().trim();
                        dataBuffer.setLength(0);
                        // 找到第一个有效的 JSON-RPC 响应就返回
                        JsonRpcResponse rpcResponse = JsonRpcResponse.parse(data);
                        if (rpcResponse != null && rpcResponse.getId() > 0) {
                            return rpcResponse;
                        }
                        eventType = null;
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
            }
            // 处理最后一个事件
            if (dataBuffer.length() > 0) {
                return JsonRpcResponse.parse(dataBuffer.toString().trim());
            }
            return null;
        } finally {
            reader.close();
        }
    }
}
