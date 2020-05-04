package pl.extollite.amazingcrates;

import cn.nukkit.item.Item;

import java.util.List;
import java.util.Map;

public class Crate {
    private String name;
    private String key;
    private int fakeItems;
    private Map<Integer, Item> drops;
    private Map<Item, List<String>> commands;
    private Map<Integer, Integer> chances;
    private String effect;

    public Crate(String name, String key, String effect, Map<Integer, Item> drops, int fakeItems, Map<Item, List<String>> commands, Map<Integer, Integer> chances) {
        this.name = name;
        this.key = key;
        this.drops = drops;
        this.fakeItems = fakeItems;
        this.commands = commands;
        this.chances = chances;
        this.effect = effect;
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public Map<Integer, Item> getDrops() {
        return drops;
    }

    public Map<Item, List<String>> getCommands() {
        return commands;
    }

    public int getFakeItems() {
        return fakeItems;
    }

    public Map<Integer, Integer> getChances() {
        return chances;
    }

    public String getEffect() {
        return effect;
    }
}
