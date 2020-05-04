package pl.extollite.amazingcrates;

import cn.nukkit.command.Command;
import cn.nukkit.command.PluginIdentifiableCommand;

public abstract class CommandManager extends Command implements PluginIdentifiableCommand {
    private AmazingCrates plugin;

    public CommandManager(AmazingCrates plugin, String name, String desc, String usage) {
        super(name, desc, usage);

        this.plugin = plugin;
    }

    public AmazingCrates getPlugin() {
        return plugin;
    }
}
