package cloud.apposs.robot.harness;

import cloud.apposs.robot.harness.HarnessSetting;
import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.harness.plugin.IPlugin;
import cloud.apposs.robot.harness.plugin.PluginFactory;
import cloud.apposs.util.Table;

import java.nio.file.Path;
import java.util.List;

public final class HarnessPlugin {
    private final Table<IPlugin> plugins;

    public HarnessPlugin(HarnessWorker worker) {
        this.plugins = new Table<>();
        this.reload(worker);
    }

    public Table<IPlugin> getPlugins() {
        return plugins;
    }

    public HarnessPlugin reload(HarnessWorker worker) {
        List<String> newPluginNames = worker.getProfile().getPlugins();
        if (newPluginNames == null || newPluginNames.isEmpty()) {
            return this;
        }
        plugins.clear();
        if (newPluginNames.size() == 1 && "*".equals(newPluginNames.get(0).trim())) {
            IPlugin[] buildinPlugins = PluginFactory.createBuildinPlugins(worker);
            for (IPlugin plugin : buildinPlugins) {
                register(plugin);
            }
        } else {
            for (String name : newPluginNames) {
                IPlugin plugin = PluginFactory.createMatchedPlugin(name, worker);
                if (plugin != null) {
                    register(plugin);
                }
            }
        }
        return this;
    }

    public HarnessPlugin register(IPlugin plugin) {
        this.plugins.add(plugin);
        return this;
    }

    public void loadPlugin(Path pluginRuntimePath) throws Exception {
    }

    public void reload(HarnessSetting setting) {
    }

    public void release() {
        for (IPlugin plugin : plugins) {
            plugin.release();
        }
    }
}
