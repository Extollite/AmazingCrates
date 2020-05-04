package pl.extollite.amazingcrates;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.item.Item;
import cn.nukkit.utils.TextFormat;

import java.util.HashMap;
import java.util.Map;

public class KeyCommand extends CommandManager {

    private AmazingCrates plugin;

    public KeyCommand(AmazingCrates plugin) {
        super(plugin, "key", "key command", "/key <player_name> <name> <amount>");
        this.plugin = plugin;
        Map<String, CommandParameter[]> parameters = new HashMap<>();
        parameters.put("set", new CommandParameter[]{
                new CommandParameter("Player Name", CommandParamType.TARGET, false),
                new CommandParameter("Key Name", false, plugin.getKeys().keySet().toArray(new String[0])),
                new CommandParameter("Amount", CommandParamType.INT, true)
        });
        this.setCommandParameters(parameters);
        this.setPermission("key.command");
    }

    public boolean execute(CommandSender sender, String label, String[] args) {
        if (sender instanceof Player) {
            if (!sender.isOp() && !sender.hasPermission("key.command")) {
                return true;
            }
        }
        if (args.length >= 2) {
            Item item = plugin.getKeys().get(args[1]);
            if(item != null){
                if(args.length == 3){
                    int amount = Integer.parseInt(args[2]);
                    /*                if(amount > 64)*/
                    item.setCount(amount);
                }
                Player p = this.plugin.getServer().getPlayerExact(args[0]);
                if(p.getInventory().canAddItem(item)){
                    p.getInventory().addItem(item.clone());
                    p.sendAllInventories();
                }
                else{
                    p.dropItem(item.clone());
                }
                p.sendMessage(plugin.getPrefix()+plugin.getGiveMsg().replace("%key_name%", item.getName()));
                sender.sendMessage(plugin.getPrefix()+"Key was given to player!");
            }
            else{
                sender.sendMessage(plugin.getPrefix()+"This coupon don't exists!");
            }
            return true;
        }
        sender.sendMessage(plugin.getPrefix()+TextFormat.GREEN + "Usage: ");
        sender.sendMessage(TextFormat.GREEN + "/key <player_name> <name> <amount>");
        return true;
    }
}

