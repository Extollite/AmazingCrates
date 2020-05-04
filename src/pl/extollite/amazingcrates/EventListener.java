package pl.extollite.amazingcrates;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockChest;
import cn.nukkit.block.BlockID;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.inventory.InventoryCloseEvent;
import cn.nukkit.event.player.*;
import cn.nukkit.item.Item;
import cn.nukkit.level.Location;
import cn.nukkit.level.particle.FloatingTextParticle;
import cn.nukkit.level.particle.LavaParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.scheduler.TaskHandler;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import com.nukkitx.fakeinventories.inventory.ChestFakeInventory;
import com.nukkitx.fakeinventories.inventory.FakeSlotChangeEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class EventListener implements Listener {
    public EventListener() {

    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent ev) {
        if (ev.isCancelled())
            return;
        if (!ev.getFrom().getLevel().equals(ev.getTo().getLevel())) {
            for(Map.Entry<Location, FloatingTextParticle> particle : AmazingCrates.getInstance().getParticles().entrySet()){
                if(particle.getKey().getLevel().equals(ev.getTo().getLevel())){
                    particle.getValue().setInvisible(false);
                    particle.getKey().getLevel().addParticle(particle.getValue(), ev.getPlayer());
                }
                else if(particle.getKey().getLevel().equals(ev.getFrom().getLevel())){
                    particle.getValue().setInvisible(true);
                    particle.getKey().getLevel().addParticle(particle.getValue(), ev.getPlayer());
                }
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent ev) {
        for(FloatingTextParticle particle : AmazingCrates.getInstance().getParticles().values()){
            particle.setInvisible(false);
            for(DataPacket pk : particle.encode())
                ev.getPlayer().dataPacket(pk);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent ev){
        if (ev.isCancelled())
            return;
        if(!(ev.getBlock() instanceof BlockChest))
            return;
        if(!AmazingCrates.getInstance().getCrateLocations().containsKey(ev.getBlock().getLocation()))
            return;
        Player player = ev.getPlayer();
        if (!player.isOp() && !player.hasPermission("crate.command")) {
            return;
        }
        Block block = ev.getBlock();
        AmazingCrates.getInstance().getSteps().remove(block.getLocation().add(0.5, 0, 0.5));
        FloatingTextParticle particle = AmazingCrates.getInstance().getParticles().remove(block.getLocation());
        particle.setInvisible(true);
        particle.setTitle("");
        AmazingCrates.getInstance().getCrateLocations().remove(block.getLocation());
        Config cfg = new Config(AmazingCrates.getInstance().getDataFolder()+"/locations.yml", Config.YAML);
        Set<String> keys = cfg.getKeys(false);
        for(String key : keys){
            ConfigSection cratesSection = cfg.getSection(key);
            Location location = new Location(cratesSection.getDouble("x"), cratesSection.getDouble("y"), cratesSection.getDouble("z"), AmazingCrates.getInstance().getServer().getLevelByName(cratesSection.getString("world")));
            if(block.getLocation().getLevel().getId() == location.getLevel().getId() && location.equals(block.getLocation())){
                cfg.remove(key);
                cfg.save();
                return;
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent ev) {
        if(!(ev.getBlock() instanceof BlockChest))
            return;
        if(!AmazingCrates.getInstance().getCrateLocations().containsKey(ev.getBlock().getLocation()))
            return;
        ev.setCancelled();
        Item item = ev.getItem();
        if (item != null && item.getNamedTagEntry("KeyName") != null) {
            String key = (String)item.getNamedTagEntry("KeyName").parseValue();
            Crate crate = AmazingCrates.getInstance().getCrateLocations().get(ev.getBlock().getLocation());
            if(crate.getKey().equals(key) && !AmazingCrates.getInstance().getOpenedChest().containsKey(ev.getPlayer())){
                Item newitem = item.clone();
                newitem.setCount(newitem.getCount() - 1);
                if(newitem.getCount() > 0)
                    ev.getPlayer().getInventory().setItemInHand(newitem);
                else
                    ev.getPlayer().getInventory().setItemInHand(new Item(BlockID.AIR));
                ev.getPlayer().sendAllInventories();
                UpdateRunnable runnable = new UpdateRunnable(ev.getPlayer(), crate, AmazingCrates.getInstance().getDelay(), AmazingCrates.getInstance().getScroll());
                TaskHandler task = AmazingCrates.getInstance().getServer().getScheduler().scheduleRepeatingTask(AmazingCrates.getInstance(), runnable, AmazingCrates.getInstance().getScroll());
                runnable.setId(task.getTaskId());
                AmazingCrates.getInstance().getOpenedChest().put(ev.getPlayer(), task);
                Block block = ev.getBlock();
                double cx  = block.getX() + 0.5;
                double cy = block.getY() + 1.2;
                double cz = block.getZ() + 0.5;
                int radius = 1;
                for(int i = 0; i < 361; i += 1.1) {
                    double x = cx + (radius * Math.cos(i));
                    double z = cz + (radius * Math.sin(i));
                    Vector3 pos = new Vector3(x, cy, z);
                    block.getLevel().addParticle(new LavaParticle(pos));
                }
                return;
            }
        }
        if(!AmazingCrates.getInstance().getOpenedChest().containsKey(ev.getPlayer())){
            Crate crate = AmazingCrates.getInstance().getCrateLocations().get(ev.getBlock().getLocation());
            ChestFakeInventory fakeInventory;
            fakeInventory = AmazingCrates.getInstance().getFakeInventories().createChestInventory();
            fakeInventory.setName(crate.getName());
            fakeInventory.setTitle(crate.getName());
            fakeInventory.addListener(this::onSlotChange);
            for(Map.Entry<Integer, Item> entry : crate.getDrops().entrySet()){
                Item show = entry.getValue().clone().setLore(AmazingCrates.getInstance().getChanceName() + crate.getChances().get(entry.getKey()));
                fakeInventory.addItem(show);
            }
            AmazingCrates.getInstance().getOpenedChest().put(ev.getPlayer(), null);
            ev.getPlayer().addWindow(fakeInventory);
        }
    }

    private void onSlotChange(FakeSlotChangeEvent e) {
        if (e.getInventory() instanceof ChestFakeInventory) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent ev){
        if(ev.getInventory() instanceof ChestFakeInventory){
            TaskHandler task = AmazingCrates.getInstance().getOpenedChest().get(ev.getPlayer());
            if (task != null && ((UpdateRunnable)task.getTask()).delay > 0) {
                Item reward = ((UpdateRunnable)task.getTask()).getReward();
                Crate crate = ((UpdateRunnable)task.getTask()).getCrate();
                Player player = ev.getPlayer();
                if(player.isOnline()){
                    boolean commandItem = false;
                    Map<Item, List<String>> rewards = crate.getCommands();
                    for(Map.Entry<Item, List<String>> entry : rewards.entrySet()){
                        if(entry.getKey().equals(reward)){
                            for(String command : entry.getValue()) {
                                AmazingCrates.getInstance().getServer().dispatchCommand(AmazingCrates.getInstance().getServer().getConsoleSender(), command.replace("%player_name%", player.getName()));
                            }
                            commandItem = true;
                        }
                    }
                    if(!commandItem){
                        if(player.getInventory().canAddItem(reward)){
                            player.getInventory().addItem(reward);
                            player.sendAllInventories();
                        }
                        else{
                            player.getLevel().dropItem(player, reward);
                        }
                    }
                    AmazingCrates.getInstance().getServer().broadcastMessage(AmazingCrates.getInstance().getOpenMsg().replace("%player_name%", player.getName()).replace("%crate_name%", crate.getName()));
                }
                AmazingCrates.getInstance().getServer().getScheduler().cancelTask(task.getTaskId());
                AmazingCrates.getInstance().getOpenedChest().remove(player);
            } else if(task == null && AmazingCrates.getInstance().getOpenedChest().containsKey(ev.getPlayer())){
                AmazingCrates.getInstance().getOpenedChest().remove(ev.getPlayer());
            }
        }
    }
}
