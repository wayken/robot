package cloud.apposs.robot.harness.tool.filesystem;

import cloud.apposs.react.React;
import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.robot.harness.tool.ITool;
import cloud.apposs.robot.harness.util.PathUtil;
import cloud.apposs.robot.harness.util.Strings;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ReadFileTool implements ITool {
    private static final int DEFAULT_MAX_LINES = 1000;
    private static final int MAX_OUTPUT_BYTES = 30 * 1024;

    public static final String NAME = "read_file";

    private final Path workspace;

    public ReadFileTool(Path workspace) {
        this.workspace = workspace;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Read the contents of a file at the given path.";
    }

    @Override
    public Param parameters() {
        String schema = "{" +
                "  \"type\": \"object\"," +
                "  \"properties\": {" +
                "    \"path\": {" +
                "      \"type\": \"string\"," +
                "      \"description\": \"The file path to read\"" +
                "    }," +
                "    \"startLineNumber\": {" +
                "      \"type\": \"integer\"," +
                "      \"description\": \"The line number to start reading from (default 1)\"" +
                "    }," +
                "    \"endLineNumber\": {" +
                "      \"type\": \"integer\"," +
                "      \"description\": \"The line number to end reading (default startLineNumber + " + DEFAULT_MAX_LINES + " - 1)\"" +
                "    }" +
                "  }," +
                "  \"required\": [\"path\"]" +
                "}";
        return JsonUtil.parseJsonParam(schema);
    }

    @Override
    public React<String> run(String wid, String sid, String rid, Param parameter, IMessageHook messageHook) {
        if (parameter == null) {
            return React.just("Error: parameters must not be null.");
        }
        String path = parameter.getString("path");
        if (Strings.isBlank(path)) {
            return React.just("Error: file path must not be empty.");
        }
        Path pathResolved = PathUtil.resolveAbsolutePath(path.trim(), workspace);
        if (!Files.exists(pathResolved)) {
            return React.just("Error: file not found at path: " + path);
        }
        if (Files.isDirectory(pathResolved)) {
            return React.just("Error: path is a directory, not a file: " + path);
        }
        if (!Files.isReadable(pathResolved)) {
            return React.just("Error: file is not readable: " + path);
        }
        int startLineNumber = parameter.getInt("startLineNumber", 1);
        int endLineNumber = parameter.getInt("endLineNumber", startLineNumber + DEFAULT_MAX_LINES - 1);

        try {
            List<String> allLines = Files.readAllLines(pathResolved, StandardCharsets.UTF_8);
            int totalLines = allLines.size();
            if (startLineNumber < 1) {
                startLineNumber = 1;
            }
            if (endLineNumber > totalLines) {
                endLineNumber = totalLines;
            }
            if (startLineNumber > totalLines) {
                return React.just("Error: startLineNumber " + startLineNumber + " exceeds total lines " + totalLines + ".");
            }
            if (startLineNumber > endLineNumber) {
                return React.just("Error: startLineNumber must not be greater than endLineNumber.");
            }

            StringBuilder builder = new StringBuilder();
            for (int i = startLineNumber; i <= endLineNumber; i++) {
                builder.append(allLines.get(i - 1)).append("\n");
                if (builder.length() > MAX_OUTPUT_BYTES) {
                    builder.append("... (output truncated at ").append(MAX_OUTPUT_BYTES / 1024).append("KB)");
                    break;
                }
            }
            if (endLineNumber < totalLines) {
                builder.append("\n[Showing lines ").append(startLineNumber).append("-").append(endLineNumber)
                        .append(" of ").append(totalLines).append(" total lines]");
            }

            return React.just(builder.toString());
        } catch (Exception e) {
            return React.just("Error: unexpected error while reading file: " + e.getMessage());
        }
    }
}
