package pl.extollite.amazingcrates;

import cn.nukkit.Player;
import cn.nukkit.block.BlockID;
import cn.nukkit.command.ConsoleCommandSender;
import cn.nukkit.event.Listener;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.level.Sound;
import cn.nukkit.utils.ClientChainData;
import com.nukkitx.fakeinventories.inventory.ChestFakeInventory;
import com.nukkitx.fakeinventories.inventory.FakeSlotChangeEvent;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class UpdateRunnable implements Runnable{
    Player player;
    ChestFakeInventory fakeInventory;
    Crate crate;
    int delay = 30;
    int id;
    int scroll;

    public UpdateRunnable(Player player, Crate crate, int delay, int scroll){
        this.player = player;
        this.crate = crate;
         fakeInventory = AmazingCrates.getInstance().getFakeInventories().createChestInventory();
         fakeInventory.setName(crate.getName());
         fakeInventory.setTitle(crate.getName());
         fakeInventory.addListener(this::onSlotChange);
         if(player.getLoginChainData().getUIProfile() == ClientChainData.UI_PROFILE_CLASSIC){
             int i = 0;
             while(i < 27){
                 if(i != 4 && i != 10 && i != 11 && i != 12 && i != 13 && i != 14 && i != 15 && i != 16 && i != 22){
                     fakeInventory.setItem(i, new Item(BlockID.VINE));
                 }
                 i++;
             }

             fakeInventory.setItem(4, new Item(BlockID.END_ROD));
             fakeInventory.setItem(22, new Item(BlockID.END_ROD));

             fakeInventory.setItem(10, new Item(ItemID.COOKED_CHICKEN, 0, 16));
             fakeInventory.setItem(11, new Item(ItemID.COOKED_CHICKEN, 0, 16));
             fakeInventory.setItem(12, new Item(ItemID.COOKED_CHICKEN, 0, 16));
             fakeInventory.setItem(13, new Item(ItemID.COOKED_CHICKEN, 0, 16));
             fakeInventory.setItem(14, new Item(ItemID.COOKED_CHICKEN, 0, 16));
             fakeInventory.setItem(15, new Item(ItemID.COOKED_CHICKEN, 0, 16));
             fakeInventory.setItem(16, new Item(ItemID.COOKED_CHICKEN, 0, 16));
         } else {

             int i = 0;
             while(i < 27){
                 if(i != 8 && i != 12 && i != 13 && i != 14 && i != 15 && i != 16 && i != 17 && i != 20){
                     fakeInventory.setItem(i, new Item(BlockID.VINE));
                 }
                 i++;
             }

             fakeInventory.setItem(8, new Item(BlockID.END_ROD));
             fakeInventory.setItem(20, new Item(BlockID.END_ROD));

             fakeInventory.setItem(12, new Item(ItemID.COOKED_CHICKEN, 0, 16));
             fakeInventory.setItem(13, new Item(ItemID.COOKED_CHICKEN, 0, 16));
             fakeInventory.setItem(14, new Item(ItemID.COOKED_CHICKEN, 0, 16));
             fakeInventory.setItem(15, new Item(ItemID.COOKED_CHICKEN, 0, 16));
             fakeInventory.setItem(16, new Item(ItemID.COOKED_CHICKEN, 0, 16));
             fakeInventory.setItem(17, new Item(ItemID.COOKED_CHICKEN, 0, 16));
         }

        player.addWindow(fakeInventory);
        this.delay = delay;
        this.scroll = scroll;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public void run() {
        if(delay > 0){
            player.getLevel().addSound(player, Sound.RANDOM_CLICK, 1, 1, player);
            Item reward = this.getReward();
            Map<Integer, Item> inv = fakeInventory.getContents();
            if(player.getLoginChainData().getUIProfile() == ClientChainData.UI_PROFILE_CLASSIC){
                fakeInventory.setItem(10, inv.get(11));
                fakeInventory.setItem(11, inv.get(12));
                fakeInventory.setItem(12, inv.get(13));
                fakeInventory.setItem(13, inv.get(14)); // reward
                fakeInventory.setItem(14, inv.get(15));
                fakeInventory.setItem(15, inv.get(16));
                fakeInventory.setItem(16, reward);
            } else {
                fakeInventory.setItem(12, inv.get(13));
                fakeInventory.setItem(13, inv.get(14));
                fakeInventory.setItem(14, inv.get(15)); // reward
                fakeInventory.setItem(15, inv.get(16));
                fakeInventory.setItem(16, inv.get(17));
                fakeInventory.setItem(17, reward);
            }

            fakeInventory.sendContents(player);
        }

        if(delay <= 0){
            Item reward;
            if(player.getLoginChainData().getUIProfile() == ClientChainData.UI_PROFILE_CLASSIC){
                fakeInventory.setItem(10, new Item(BlockID.AIR));
                fakeInventory.setItem(11, new Item(BlockID.AIR));
                fakeInventory.setItem(12, new Item(BlockID.AIR));
                fakeInventory.setItem(14, new Item(BlockID.AIR));
                fakeInventory.setItem(15, new Item(BlockID.AIR));
                fakeInventory.setItem(16, new Item(BlockID.AIR));
                reward = fakeInventory.getItem(13);
            } else {
                fakeInventory.setItem(12, new Item(BlockID.AIR));
                fakeInventory.setItem(13, new Item(BlockID.AIR));
                fakeInventory.setItem(15, new Item(BlockID.AIR));
                fakeInventory.setItem(16, new Item(BlockID.AIR));
                fakeInventory.setItem(17, new Item(BlockID.AIR));
                reward = fakeInventory.getItem(14);
            }
            fakeInventory.sendContents(player);
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
                player.getLevel().addSound(player, Sound.RANDOM_LEVELUP);
                AmazingCrates.getInstance().getServer().broadcastMessage(AmazingCrates.getInstance().getOpenMsg().replace("%player_name%", player.getName()).replace("%crate_name%", crate.getName()));
            }
            AmazingCrates.getInstance().getServer().getScheduler().cancelTask(this.id);
            AmazingCrates.getInstance().getOpenedChest().remove(player);
        }

        delay -= scroll;

    }

    private void onSlotChange(FakeSlotChangeEvent e) {
        if (e.getInventory() instanceof ChestFakeInventory) {
            if (e.getPlayer().equals(player)) {
                e.setCancelled(true);
            }
        }
    }

    public Item getReward(){
        int chance = new Random().nextInt(100);
        TreeMap<Integer, Item> drops = (TreeMap<Integer, Item>)crate.getDrops();
        Map.Entry<Integer, Item> entry = drops.ceilingEntry(chance);
        return entry.getValue();
    }

    public Crate getCrate() {
        return crate;
    }
}
