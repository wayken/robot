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
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditFileTool implements ITool {
    public static final String NAME = "edit_file";

    private final Path workspace;

    public EditFileTool(Path workspace) {
        this.workspace = workspace;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Edit a file by replacing oldText with newText. The oldText must exist exactly in the file";
    }

    @Override
    public Param parameters() {
        String schema = "{" +
                "  \"type\": \"object\"," +
                "  \"properties\": {" +
                "    \"path\": {" +
                "      \"type\": \"string\"," +
                "      \"description\": \"The file path to edit\"" +
                "    }," +
                "    \"oldText\": {" +
                "      \"type\": \"string\"," +
                "      \"description\": \"The exact text to find and replace\"" +
                "    }," +
                "    \"newText\": {" +
                "      \"type\": \"string\"," +
                "      \"description\": \"The text to replace with\"" +
                "    }," +
                "    \"replaceAll\": {" +
                "      \"type\": \"boolean\"," +
                "      \"description\": \"Whether to replace all occurrences (default false)\"" +
                "    }" +
                "  }," +
                "  \"required\": [\"path\", \"newText\", \"oldText\"]" +
                "}";
        return JsonUtil.parseJsonParam(schema);
    }

    @Override
    public React<String> run(String wid, String sid, String rid, Param parameter, IMessageHook messageHook) throws Exception {
        if (parameter == null) {
            return React.just("Error: parameters must not be null.");
        }
        String path = parameter.getString("path");
        if (Strings.isBlank(path)) {
            return React.just("Error: file path must not be empty.");
        }
        String oldText = parameter.getString("oldText");
        if (oldText == null) {
            return React.just("Error: oldText must not be null.");
        }
        String newText = parameter.getString("newText");
        if (newText == null) {
            newText = "";
        }
        boolean replaceAll = parameter.getBoolean("replaceAll", false);
        Path pathResolved = PathUtil.resolveAbsolutePath(path, workspace);
        if (!Files.exists(pathResolved)) {
            return React.just("Error: file not found at path: " + path);
        }
        if (Files.isDirectory(pathResolved)) {
            return React.just("Error: path is a directory, not a file: " + path);
        }
        if (!Files.isReadable(pathResolved) || !Files.isWritable(pathResolved)) {
            return React.just("Error: file must be readable and writable: " + path);
        }
        try {
            String content = new String(Files.readAllBytes(pathResolved), StandardCharsets.UTF_8);
            if (!content.contains(oldText)) {
                return React.just("Error: oldText not found in file: " + oldText);
            }
            if (replaceAll) {
                content = content.replace(oldText, newText);
            } else {
                content = content.replaceFirst(Pattern.quote(oldText), Matcher.quoteReplacement(newText));
            }
            Files.write(pathResolved, content.getBytes(StandardCharsets.UTF_8));
            return React.just("File edited successfully.");
        } catch (NoSuchFileException e) {
            return React.just("Error: file not found at path: " + path);
        } catch (Exception e) {
            return React.just("Error: failed to edit file at path: " + path + ". " + e.getMessage());
        }
    }
}
