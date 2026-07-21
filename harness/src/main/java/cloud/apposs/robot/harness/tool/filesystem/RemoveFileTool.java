package cloud.apposs.robot.harness.tool.filesystem;

import cloud.apposs.react.React;
import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.robot.harness.tool.ITool;
import cloud.apposs.robot.harness.util.PathUtil;
import cloud.apposs.robot.harness.util.Strings;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public class RemoveFileTool implements ITool {
    public static final String NAME = "remove_file";

    private final Path workspace;

    public RemoveFileTool(Path workspace) {
        this.workspace = workspace;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Remove a file at the given path. This tool only deletes regular files and does not delete directories.";
    }

    @Override
    public Param parameters() {
        String schema = "{" +
                "  \"type\": \"object\"," +
                "  \"properties\": {" +
                "    \"path\": {" +
                "      \"type\": \"string\"," +
                "      \"description\": \"The file path to remove\"" +
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
        if (!Files.isRegularFile(pathResolved)) {
            return React.just("Error: path is not a regular file: " + path);
        }
        if (!Files.isWritable(pathResolved)) {
            return React.just("Error: file is not writable: " + path);
        }
        try {
            long bytes = Files.size(pathResolved);
            Files.delete(pathResolved);
            return React.just("File " + path + " removed successfully, bytes removed: " + bytes);
        } catch (NoSuchFileException e) {
            return React.just("Error: file not found at path: " + path);
        } catch (Exception e) {
            return React.just("Error: failed to remove file at path: " + path + ". " + e.getMessage());
        }
    }
}
