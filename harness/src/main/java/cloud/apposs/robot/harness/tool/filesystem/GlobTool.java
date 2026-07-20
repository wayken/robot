package cloud.apposs.robot.harness.tool.filesystem;

import cloud.apposs.react.React;
import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.robot.harness.tool.ITool;
import cloud.apposs.robot.harness.util.PathUtil;
import cloud.apposs.robot.harness.util.Strings;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * Glob 文件搜索工具，支持通过 glob 模式匹配文件路径，
 * 类似 Claude Code 的 glob 工具，可以在指定目录下递归搜索匹配的文件，
 * 支持标准 glob 语法：* 匹配单层文件名，** 跨目录递归匹配，? 匹配单个字符，{a,b} 匹配多个候选
 */
public class GlobTool implements ITool {
    public static final String NAME = "glob";

    // 单次搜索返回的最大文件数，防止结果集过大
    public static final int DEFAULT_MAX_RESULTS = 1000;

    private static final int MAX_RESULTS = DEFAULT_MAX_RESULTS;

    private final Path workspace;

    private final Path allowedDir;

    private final int maxResults;

    public GlobTool(Path workspace) {
        this(null, null, DEFAULT_MAX_RESULTS);
    }

    public GlobTool(Path workspace, Path allowedDir) {
        this(workspace, allowedDir, DEFAULT_MAX_RESULTS);
    }

    public GlobTool(Path workspace, Path allowedDir, int maxResults) {
        this.workspace = workspace;
        this.allowedDir = allowedDir;
        this.maxResults = maxResults;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Find files matching a glob pattern. Supports standard glob syntax: " +
                "* matches any filename characters (not path separators), " +
                "** recursively matches directories, " +
                "? matches a single character, " +
                "{a,b} matches alternatives. " +
                "Returns a sorted list of matching file paths.";
    }

    @Override
    public Param parameters() {
        String schema = "{" +
                "  \"type\": \"object\"," +
                "  \"properties\": {" +
                "    \"pattern\": {" +
                "      \"type\": \"string\"," +
                "      \"description\": \"Glob pattern to match files, e.g. '**/*.java', 'src/**/*.{ts,tsx}', '*.xml'\"" +
                "    }," +
                "    \"path\": {" +
                "      \"type\": \"string\"," +
                "      \"description\": \"Base directory to search in. Defaults to current working directory if not specified.\"" +
                "    }" +
                "  }," +
                "  \"required\": [\"pattern\"]" +
                "}";
        return JsonUtil.parseJsonParam(schema);
    }

    @Override
    public React<String> run(String wid, String sid, String rid, Param parameter, IMessageHook messageHook) throws Exception {
        if (parameter == null) {
            return React.just("Error: parameters must not be null.");
        }
        String pattern = parameter.getString("pattern");
        if (Strings.isBlank(pattern)) {
            return React.just("Error: glob pattern must not be empty.");
        }
        // 解析搜索根目录
        String basePath = parameter.getString("path");
        Path baseDir;
        if (Strings.isBlank(basePath)) {
            baseDir = workspace != null ? workspace : Paths.get("").toAbsolutePath();
        } else {
            baseDir = PathUtil.resolveAbsolutePath(basePath, workspace);
        }
        if (!Files.exists(baseDir)) {
            return React.just("Error: directory not found: " + baseDir);
        }
        if (!Files.isDirectory(baseDir)) {
            return React.just("Error: path is not a directory: " + baseDir);
        }
        // 安全校验
        if (allowedDir != null) {
            Path allowed = allowedDir.toAbsolutePath().normalize();
            if (!baseDir.startsWith(allowed)) {
                return React.just("Error: path is outside allowed directory: " + baseDir);
            }
        }
        try {
            List<String> matches = findMatches(baseDir, pattern);
            if (matches.isEmpty()) {
                return React.just("No files found matching pattern: " + pattern);
            }
            boolean truncated = matches.size() > maxResults;
            List<String> display = truncated ? matches.subList(0, maxResults) : matches;
            StringBuilder builder = new StringBuilder();
            for (String match : display) {
                builder.append(match).append("\n");
            }
            if (truncated) {
                builder.append("\n(Results truncated to ").append(maxResults)
                        .append(" files. Refine your pattern to see more.)");
            } else {
                // 去掉末尾多余换行
                if (builder.length() > 0 && builder.charAt(builder.length() - 1) == '\n') {
                    builder.deleteCharAt(builder.length() - 1);
                }
            }
            return React.just(builder.toString());
        } catch (PatternSyntaxException e) {
            return React.just("Error: invalid glob pattern '" + pattern + "': " + e.getMessage());
        } catch (SecurityException e) {
            return React.just("Error: " + e.getMessage());
        } catch (IOException e) {
            return React.just("Error: failed to search files: " + e.getMessage());
        }
    }

    // 在 baseDir 下递归搜索匹配 pattern 的文件，返回相对于 baseDir 的路径列表（已排序）
    private List<String> findMatches(Path baseDir, String pattern) throws IOException {
        // Java NIO PathMatcher 使用 "glob:" 前缀，将 pattern 中的路径分隔符统一为 /，再交给 PathMatcher
        String normalizedPattern = pattern.replace("\\", "/");
        PathMatcher matcher = baseDir.getFileSystem().getPathMatcher("glob:" + normalizedPattern);
        List<String> results = new ArrayList<>();
        Files.walkFileTree(baseDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                // 取相对路径进行匹配
                Path relative = baseDir.relativize(file);
                // 统一使用 / 分隔符，保证跨平台 glob 匹配一致
                Path unixRelative = handleUnixPathParse(relative);
                if (matcher.matches(unixRelative)) {
                    results.add(relative.toString().replace("\\", "/"));
                }
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                // 跳过无权限访问的文件，继续遍历
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                // 跳过隐藏目录（如 .git），加快搜索速度
                String dirName = dir.getFileName() != null ? dir.getFileName().toString() : "";
                if (!dir.equals(baseDir) && dirName.startsWith(".")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
        Collections.sort(results);
        return results;
    }

    // 将 Path 转换为使用 / 分隔符的 Path，用于跨平台 glob 匹配
    private Path handleUnixPathParse(Path path) {
        String parsedPath = path.toString().replace("\\", "/");
        return Paths.get(parsedPath);
    }
}
