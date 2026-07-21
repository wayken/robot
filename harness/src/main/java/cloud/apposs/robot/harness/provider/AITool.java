package cloud.apposs.robot.harness.provider;

import cloud.apposs.robot.harness.tool.filesystem.EditFileTool;
import cloud.apposs.robot.harness.tool.filesystem.RemoveFileTool;
import cloud.apposs.robot.harness.tool.filesystem.WriteFileTool;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;

public class AITool {
    private String id;

    private String name;

    private String arguments;

    private Boolean success;

    private Boolean partial;

    private Param fileUpdate;

    public AITool(String id, String name, String arguments) {
        this.id = id;
        this.name = name;
        this.arguments = arguments;
        this.fileUpdate = handleFileUpdateBuild(name, arguments);
    }

    public static AITool of(String id, String name, String arguments) {
        return new AITool(id, name, arguments);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
        this.fileUpdate = handleFileUpdateBuild(name, arguments);
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Boolean getPartial() {
        return partial;
    }

    public void setPartial(Boolean partial) {
        this.partial = partial;
    }

    public Param getFileUpdate() {
        return fileUpdate;
    }

    public void setFileUpdate(Param fileUpdate) {
        this.fileUpdate = fileUpdate;
    }

    public Param deserialize() {
        Param data = Param.builder("id", id)
                .setString("name", name)
                .setString("arguments", arguments);
        if (fileUpdate != null) {
            data.setParam("fileUpdate", fileUpdate);
        }
        if (success != null) {
            data.setBoolean("success", success);
        }
        if (partial != null) {
            data.setBoolean("partial", partial);
        }
        return data;
    }

    public static AITool serialize(Param param) {
        String id = param.getString("id");
        String name = param.getString("name");
        String arguments = param.getString("arguments");
        AITool tool = AITool.of(id, name, arguments);
        Param fileUpdate = param.getParam("fileUpdate");
        if (fileUpdate != null) {
            tool.setFileUpdate(fileUpdate);
        }
        if (param.containsKey("success")) {
            tool.setSuccess(param.getBoolean("success", false));
        }
        if (param.containsKey("partial")) {
            tool.setPartial(param.getBoolean("partial", false));
        }
        return tool;
    }

    @Override
    public String toString() {
        return String.format("AITool{id='%s', name='%s', arguments='%s'}", id, name, arguments);
    }

    private Param handleFileUpdateBuild(String toolName, String rawArguments) {
        if (toolName == null || rawArguments == null || rawArguments.trim().isEmpty()) {
            return null;
        }
        String normalized = toolName.trim().toLowerCase();
        boolean isEdit = EditFileTool.NAME.equals(normalized);
        boolean isWrite = WriteFileTool.NAME.equals(normalized);
        boolean isDelete = RemoveFileTool.NAME.equals(normalized);
        if (!isEdit && !isWrite && !isDelete) {
            return null;
        }
        try {
            Param args = JsonUtil.parseJsonParam(rawArguments);
            if (args == null) {
                return null;
            }
            String path = useFirstNonBlank(
                    args.getString("path"),
                    args.getString("file_path"),
                    args.getString("filename"),
                    args.getString("target_path")
            );
            if (path == null) {
                return null;
            }
            int additions = 0;
            int deletions = 0;
            String operation = isDelete ? "delete" : (isWrite ? "write" : "edit");
            if (isEdit) {
                LineChangeStats stats = handleLineDiffStats(
                        useFirstNonNull(args.getString("oldText"), args.getString("old_string"), args.getString("old")),
                        useFirstNonNull(args.getString("newText"), args.getString("new_string"), args.getString("new"))
                );
                additions = stats.additions;
                deletions = stats.deletions;
            } else if (isWrite) {
                additions = handleLineCount(args.getString("content"));
            }
            return Param.builder("path", path)
                    .setString("operation", operation)
                    .setInt("additions", additions)
                    .setInt("deletions", deletions);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String useFirstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private String useFirstNonNull(String... values) {
        for (String value : values) {
            if (value != null) {
                return value;
            }
        }
        return "";
    }

    private int handleLineCount(String value) {
        return handleLines(value).length;
    }

    private LineChangeStats handleLineDiffStats(String oldText, String newText) {
        String[] oldLines = handleLines(oldText);
        String[] newLines = handleLines(newText);
        long cells = (long) oldLines.length * (long) newLines.length;
        if (cells > 200000L) {
            return new LineChangeStats(newLines.length, oldLines.length);
        }
        int[][] lcs = new int[oldLines.length + 1][newLines.length + 1];
        for (int i = oldLines.length - 1; i >= 0; i--) {
            for (int j = newLines.length - 1; j >= 0; j--) {
                if (oldLines[i].equals(newLines[j])) {
                    lcs[i][j] = lcs[i + 1][j + 1] + 1;
                } else {
                    lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
                }
            }
        }
        int common = lcs[0][0];
        return new LineChangeStats(newLines.length - common, oldLines.length - common);
    }

    private String[] handleLines(String value) {
        if (value == null || value.isEmpty()) {
            return new String[0];
        }
        String[] lines = value.split("\\R", -1);
        if (lines.length > 0 && lines[lines.length - 1].isEmpty()) {
            String[] trimmed = new String[lines.length - 1];
            System.arraycopy(lines, 0, trimmed, 0, trimmed.length);
            return trimmed;
        }
        return lines;
    }

    private static class LineChangeStats {
        private final int additions;
        private final int deletions;

        private LineChangeStats(int additions, int deletions) {
            this.additions = Math.max(additions, 0);
            this.deletions = Math.max(deletions, 0);
        }
    }
}
