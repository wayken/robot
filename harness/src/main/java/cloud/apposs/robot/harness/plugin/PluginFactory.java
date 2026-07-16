package cloud.apposs.robot.harness.plugin;

import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.harness.plugin.browser.BrowserPlugin;

public final class PluginFactory {
    public static final String[] BUILDIN_PLUGIN_NAMES = new String[] {
            BrowserPlugin.NAME
    };

    public static IPlugin[] createBuildinPlugins(HarnessWorker worker) {
        IPlugin[] iPlugins = new IPlugin[BUILDIN_PLUGIN_NAMES.length];
        for (int i = 0; i < BUILDIN_PLUGIN_NAMES.length; i++) {
            iPlugins[i] = createMatchedPlugin(BUILDIN_PLUGIN_NAMES[i], worker);
        }
        return iPlugins;
    }

    public static IPlugin createMatchedPlugin(String name, HarnessWorker worker) {
        switch (name) {
            case BrowserPlugin.NAME:
                return new BrowserPlugin(worker);
            default:
                return null;
        }
    }
}
