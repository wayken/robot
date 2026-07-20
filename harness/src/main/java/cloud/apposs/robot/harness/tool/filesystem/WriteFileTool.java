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

public class WriteFileTool implements ITool {
    public static final String NAME = "write_file";

    private final Path workspace;

    public WriteFileTool(Path workspace) {
        this.workspace = workspace;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Write content to a file at the given path. Creates parent directories if needed.";
    }

    @Override
    public Param parameters() {
        String schema = "{" +
                "  \"type\": \"object\"," +
                "  \"properties\": {" +
                "    \"path\": {" +
                "      \"type\": \"string\"," +
                "      \"description\": \"The file path to write to\"" +
                "    }," +
                "    \"content\": {" +
                "      \"type\": \"string\"," +
                "      \"description\": \"The content to write\"" +
                "    }" +
                "  }," +
                "  \"required\": [\"path\", \"content\"]" +
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
        String content = parameter.getString("content");
        if (content == null) {
            content = "";
        }
        try {
            Path pathResolved = PathUtil.resolveAbsolutePath(path.trim(), workspace);
            if (pathResolved.getParent() != null) {
                Files.createDirectories(pathResolved.getParent());
            }
            Files.write(pathResolved, content.getBytes(StandardCharsets.UTF_8));
            return React.just("File " + path + " written successfully, bytes written: " + pathResolved.toFile().length());
        } catch (Exception e) {
            return React.just("Error: unexpected error while writing file: " + e.getMessage());
        }
    }
}
