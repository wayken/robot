package cloud.apposs.robot.harness.plugin;

import cloud.apposs.robot.harness.command.ICommand;
import cloud.apposs.robot.harness.skill.SkillStruct;
import cloud.apposs.robot.harness.tool.ITool;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

import java.util.Properties;

/**
 * 插件接口，提供更高层次的功能和接口，定义了插件的基本信息、工具集、指令集和技能列表等，具体插件数据结构如下：
 * <pre>
 *     {
 *         "name": "example-plugin",
 *         "version": "0.1.0",
 *         "description": "Example plugin — copy this as a starting point for your own plugin.",
 *         "author": "Author001"
 *         "tags": ["example", "template"],
 *         "tools": ["tools"],
 *         "commands": ["cmd"],
 *         "skills": ["skills/example-skill.md"],
 *         "mcp_servers": {},
 *         "homepage": "https://github.com/xxx"
 *     }
 * </pre>
 */
public interface IPlugin {
    String getName();

    String getVersion();

    String getDescription();

    default Param getMetadata() {
        return null;
    }

    default Properties getEnvironment() {
       return null;
    }

    default Table<ICommand> getCommands() {
        return null;
    }

    default Table<ITool> getTools() {
        return null;
    }

    default Table<SkillStruct> getSkills() {
        return null;
    }

    default void release() {
    }
}
