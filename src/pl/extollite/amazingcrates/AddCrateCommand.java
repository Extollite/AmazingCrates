package pl.extollite.amazingcrates;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockChest;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.item.Item;
import cn.nukkit.level.Location;
import cn.nukkit.level.particle.FloatingTextParticle;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import cn.nukkit.utils.TextFormat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AddCrateCommand extends CommandManager {

    private AmazingCrates plugin;

    public AddCrateCommand(AmazingCrates plugin) {
        super(plugin, "addcrate", "addcrate command", "/addcrate <key name> <crate name>");
        this.plugin = plugin;
        Map<String, CommandParameter[]> parameters = new HashMap<>();
        parameters.put("set", new CommandParameter[]{
                new CommandParameter("Key Name", false, plugin.getKeys().keySet().toArray(new String[0])),
                new CommandParameter("Crate Name", false, plugin.getCrates().keySet().toArray(new String[0])),
        });
        this.setCommandParameters(parameters);
        this.setPermission("crate.command");
    }

    public boolean execute(CommandSender sender, String label, String[] args) {
        if (sender instanceof Player) {
            if (!sender.isOp() && !sender.hasPermission("crate.command")) {
                return true;
            }
        }
        if (args.length == 2) {
            String key = args[0];
            if(!plugin.getKeys().containsKey(key)){
                sender.sendMessage(TextFormat.RED + "There is no such key!");
                return true;
            }
            Player player = (Player) sender;
            Block block = player.getTargetBlock(5);
            if(!(block instanceof BlockChest)){
                sender.sendMessage(TextFormat.RED + "You need to look at chest!");
                return true;
            }
            Location location = block.getLocation();
            if(plugin.getCrateLocations().containsKey(location)){
                sender.sendMessage(TextFormat.RED + "There is crate with same location!");
                return true;
            }
            Crate crate = plugin.getCrates().get(args[1]);
            FloatingTextParticle particle = new FloatingTextParticle(location.add(0.5, 2, 0.5), crate.getName());
            plugin.getParticles().put(location, particle);
            plugin.getSteps().put(location.add(0.5, 0, 0.5), 0);
            location.getLevel().addParticle(particle);
            plugin.getCrateLocations().put(block.getLocation(), crate);
            Config cfg = new Config(AmazingCrates.getInstance().getDataFolder()+"/locations.yml", Config.YAML);
            ConfigSection section = new ConfigSection();
            section.put("world", location.getLevel().getName());
            section.put("crate", args[1]);
            section.put("x", location.getX());
            section.put("y", location.getY());
            section.put("z", location.getZ());
            String[] keys = cfg.getKeys(false).toArray(new String[0]);
            cfg.set((keys.length == 0 ? "1" : keys[keys.length-1])+"s", section);
            cfg.save();
            return true;
        }
        sender.sendMessage(plugin.getPrefix()+TextFormat.GREEN + "Usage: ");
        sender.sendMessage(TextFormat.GREEN + "/key <player_name> <name>");
        return true;
    }
}

