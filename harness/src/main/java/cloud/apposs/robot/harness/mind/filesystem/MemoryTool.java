package cloud.apposs.robot.harness.mind.filesystem;

import cloud.apposs.react.React;
import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.robot.harness.mind.filesystem.MemoryFileSystemLoader;
import cloud.apposs.robot.harness.tool.ITool;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;

public class MemoryTool implements ITool {
    public static final String NAME = "save_memory";

    private final MemoryFileSystemLoader memoryLoader;

    public MemoryTool(MemoryFileSystemLoader memoryLoader) {
        this.memoryLoader = memoryLoader;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Save the memory consolidation result to persistent storage.";
    }

    @Override
    public Param parameters() {
        String schema = "{" +
                "  \"type\": \"object\"," +
                "  \"properties\": {" +
                "    \"historyEntry\": {" +
                "      \"type\": \"string\"," +
                "      \"description\": \"A paragraph (2-5 sentences) summarizing key events/decisions/topics. Start with [YYYY-MM-DD HH:MM]. Include detail useful for grep search.\"" +
                "    }," +
                "    \"memoryUpdate\": {" +
                "      \"type\": \"string\"," +
                "      \"description\": \"Full updated long-term memory as markdown. Include all existing facts plus new ones. Return unchanged if nothing new.\"" +
                "    }" +
                "  }," +
                "  \"required\": [\"historyEntry\", \"memoryUpdate\"]" +
                "}";
        return JsonUtil.parseJsonParam(schema);
    }

    @Override
    public React<String> run(String wid, String sid, String rid, Param parameter, IMessageHook messageHook) throws Exception {
        if (parameter == null) {
            return React.just("Error: parameters must not be null.");
        }
        String historyEntry = parameter.getString("historyEntry");
        if (historyEntry == null) {
            return React.just("Error: 'historyEntry' parameter is required.");
        }
        String memoryUpdate = parameter.getString("memoryUpdate");
        if (memoryUpdate == null) {
            return React.just("Error: 'memoryUpdate' parameter is required.");
        }
        memoryLoader.saveHistory(wid, sid, rid, historyEntry);
        memoryLoader.saveMemory(wid, sid, rid, memoryUpdate);
        return React.just("Memory saved successfully.");
    }
}
