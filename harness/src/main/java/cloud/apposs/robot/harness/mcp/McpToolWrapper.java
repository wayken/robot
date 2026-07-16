package cloud.apposs.robot.harness.mcp;

import cloud.apposs.logger.Logger;
import cloud.apposs.react.React;
import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.robot.harness.mcp.schema.CallToolRequest;
import cloud.apposs.robot.harness.mcp.schema.CallToolResult;
import cloud.apposs.robot.harness.mcp.schema.ContentItem;
import cloud.apposs.robot.harness.tool.ITool;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP工具包装器，将远程MCP服务器上的工具适配为框架内部的 {@link ITool} 接口，
 * 工具名称格式为 {@code mcp_{serverName}_{toolName}}，调用时通过 {@link McpTransport} 转发到对应的MCP服务器
 */
public class McpToolWrapper implements ITool {
    // 工具名称，格式为 mcp_{serverName}_{toolName}
    private final String name;

    // 原始工具名称（不含前缀），用于向MCP服务器发起调用
    private final String rawName;

    private final String description;

    private final Param parameters;

    // 所属MCP服务器客户端，用于实际发起工具调用
    private final McpTransport transport;

    public McpToolWrapper(String serverName, String name, String description, McpTransport transport, Param parameters) {
        this.name = "mcp_" + serverName + "_" + name;
        this.transport = transport;
        this.rawName = name;
        this.description = description;
        this.parameters = parameters != null ? parameters : Param.builder("type", "object").setParam("properties", new Param());
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public Param parameters() {
        return parameters;
    }

    @Override
    public React<String> run(String wid, String sid, String rid, Param parameter, IMessageHook messageHook) throws Exception {
        try {
            // 将 Param 参数转换为 Map<String, Object> 供 MCP 客户端使用
            Map<String, Object> arguments = new HashMap<String, Object>();
            if (parameter != null) {
                for (String key : parameter.keySet()) {
                    arguments.put(key, parameter.getObject(key));
                }
            }
            CallToolRequest request = new CallToolRequest(rawName, arguments);
            CallToolResult result = transport.callTool(request);
            if (result == null) {
                return React.just("");
            }
            return React.just(handleResultExtract(result));
        } catch (Exception e) {
            Logger.error(e, "McpToolWrapper: tool [%s] call failed", name);
            return React.just("Error: MCP tool [" + name + "] call failed: " +
                    (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    // 从 {@link CallToolResult} 中提取文本内容，多段内容以换行拼接，非文本内容（图片等）序列化为 JSON 字符串
    private String handleResultExtract(CallToolResult result) {
        List<ContentItem> contents = result.getContent();
        if (contents == null || contents.isEmpty()) {
            return result.isError() ? "Error: MCP tool returned an error with no content" : "";
        }
        StringBuilder builder = new StringBuilder();
        for (ContentItem content : contents) {
            String text = null;
            if (content.isText()) {
                text = content.getText();
            } else {
                // 非文本内容序列化为 JSON 描述
                Param contentParam = Param.builder("type", content.getType());
                if (content.getData() != null) {
                    contentParam.setString("data", content.getData());
                }
                if (content.getMimeType() != null) {
                    contentParam.setString("mimeType", content.getMimeType());
                }
                text = JsonUtil.toJson(contentParam);
            }
            if (text != null && !text.isEmpty()) {
                if (builder.length() > 0) {
                    builder.append("\n");
                }
                builder.append(text);
            }
        }
        if (result.isError()) {
            return "Error: " + builder;
        }
        return builder.toString();
    }
}
