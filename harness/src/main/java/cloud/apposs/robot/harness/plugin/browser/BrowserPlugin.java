package cloud.apposs.robot.harness.plugin.browser;

import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.harness.plugin.AbstractPlugin;
import cloud.apposs.robot.harness.plugin.browser.BrowserProperties;
import cloud.apposs.robot.harness.tool.ITool;
import cloud.apposs.util.Table;

/**
 * 浏览器自动化插件，基于 Playwright，实现浏览器自动化 API
 */
public class BrowserPlugin extends AbstractPlugin {
    public static final String NAME = "BrowserPlugin";
    public static final String VERSION = "1.0.0";
    public static final String DESCRIPTION = "A plugin for controlling a web browser to perform tasks such as web automation, data extraction, and testing.";

    private final BrowserProperties properties = new BrowserProperties();

    public BrowserPlugin(HarnessWorker worker) {
        super(NAME, VERSION, DESCRIPTION, worker);
    }

    @Override
    public Table<ITool> getTools() {
        Table<ITool> tools = new Table<>();
        tools.add(new BrowserTool(properties));
        return tools;
    }
}
