package pl.extollite.amazingcrates;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.level.Location;
import cn.nukkit.level.particle.*;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.StringTag;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.service.RegisteredServiceProvider;
import cn.nukkit.scheduler.TaskHandler;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import cn.nukkit.utils.Hash;
import cn.nukkit.utils.TextFormat;
import com.nukkitx.fakeinventories.inventory.FakeInventories;

import java.util.*;

public class AmazingCrates extends PluginBase {
    private static final String format = "yyyy-MM-dd HH:mm:ss Z";

    private static AmazingCrates instance;

    static AmazingCrates getInstance() {
        return instance;
    }

    private String prefix;
    private String giveMsg;
    private String openMsg;
    private String chanceName;
    private int delay;
    private int scroll;
    private Map<String, Item> keys = new HashMap<>();
    private Map<String, Crate> crates = new HashMap<>();
    private FakeInventories fakeInventories;
    private Map<Location, FloatingTextParticle> particles = new HashMap<>();
    private Map<Location, Crate> crateLocations = new HashMap<>();
    private Map<Location, Integer> steps = new HashMap<>();
    private Map<Player, TaskHandler> openedChest = new HashMap<>();

    private int numParticles = 150;
    private double size = 1;
    private double xFactor = 1.0, yFactor = 1.0, zFactor = 1.0;
    private double xOffset = 0.0, yOffset = -1.0, zOffset = 0.0;

    private static double[] cos, sin;

    static {
        cos = new double[32];
        sin = new double[32];

        int i = 0;
        for (double n = 0; n < Math.PI * 2; n += Math.PI / 16) {
            cos[i] = Math.sin(n);
            sin[i] = Math.cos(n);
            i++;
        }
    }

    @Override
    public void onEnable() {
        RegisteredServiceProvider<FakeInventories> provider = getServer().getServiceManager().getProvider(FakeInventories.class);

        if (provider == null || provider.getProvider() == null) {
            this.getLogger().error(TextFormat.RED + "FakeInventories not provided! Turning off!");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        fakeInventories = provider.getProvider();
        this.saveDefaultConfig();
        instance = this;
        List<String> authors = this.getDescription().getAuthors();
        this.getLogger().info(TextFormat.DARK_GREEN + "Plugin by " + authors.get(0));
        parseConfig();
        this.getServer().getPluginManager().registerEvents(new EventListener(), this);
        this.getServer().getCommandMap().register("key", new KeyCommand(this));
        this.getServer().getCommandMap().register("addcrate", new AddCrateCommand(this));
    }

    private void parseConfig() {
        Config configFile = getConfig();
        prefix = configFile.getString("prefix");
        giveMsg = configFile.getString("giveMessage");
        openMsg = configFile.getString("openMessage");
        chanceName = configFile.getString("chanceName");
        delay = configFile.getInt("tickDelay") * 20;
        scroll = configFile.getInt("scrollSpeed");
        Config cfg = new Config(getDataFolder() + "/keys.yml", Config.YAML);
        Set<String> keys = cfg.getKeys(false);
        if (keys.isEmpty()) {
            cfg.load(getResource("keys.yml"));
            cfg.save();
            keys = cfg.getKeys(false);
        }
        for (String key : keys) {
            ConfigSection couponSection = cfg.getSection(key);
            Item item = Item.get(couponSection.getInt("id"));
            item.setCustomName(couponSection.getString("name"));
            item.setLore(couponSection.getStringList("lore").toArray(new String[0]));
            item.addEnchantment(Enchantment.get(0));
            item.setNamedTag(item.getNamedTag().putString("KeyName", key));
            this.keys.put(key, item.clone());
        }
        cfg = new Config(getDataFolder() + "/crates.yml", Config.YAML);
        keys = cfg.getKeys(false);
        if (keys.isEmpty()) {
            cfg.load(getResource("crates.yml"));
            cfg.save();
            keys = cfg.getKeys(false);
        }
        for (String key : keys) {
            ConfigSection cratesSection = cfg.getSection(key);
            String name = cratesSection.getString("name");
            Map<Integer, Item> items = new TreeMap<>();
            Map<Item, List<String>> commands = new HashMap<>();
            Map<Integer, Integer> chances = new HashMap<>();
            String effect = cratesSection.getString("effect");
            loadItems(items, commands, chances, cratesSection.getSection("drops"));
            Crate crate = new Crate(name, cratesSection.getString("key"), effect, items, cratesSection.getInt("fakeItems"), commands, chances);
            this.crates.put(key, crate);
        }
        cfg = new Config(getDataFolder() + "/locations.yml", Config.YAML);
        keys = cfg.getKeys(false);
        for (String key : keys) {
            ConfigSection cratesSection = cfg.getSection(key);
            Location location = new Location(cratesSection.getDouble("x"), cratesSection.getDouble("y"), cratesSection.getDouble("z"), this.getServer().getLevelByName(cratesSection.getString("world")));
            String name = cratesSection.getString("crate");
            Crate crate = crates.get(name);
            FloatingTextParticle particle = new FloatingTextParticle(location.add(0.5, 2, 0.5), crate.getName());
            location.getLevel().addParticle(particle);
            particles.put(location, particle);
            steps.put(location.add(0.5, 0, 0.5), 0);
            this.crateLocations.put(location, crate);
        }
        initParticles();
    }

    public String getPrefix() {
        return prefix;
    }

    public String getGiveMsg() {
        return giveMsg;
    }

    public Map<String, Item> getKeys() {
        return keys;
    }

    void loadItems(Map<Integer, Item> drops, Map<Item, List<String>> commands, Map<Integer, Integer> chances, ConfigSection rewards) {
        int chanceSum = 0;
        Map<String, Object> reward = rewards.getAllMap();
        for (Map.Entry<String, Object> serialize : reward.entrySet()) {
            if (serialize.getValue() instanceof Map) {
                Map<String, Object> toSerialize = ((Map) serialize.getValue());
                Item rewardItem = new Item((int) toSerialize.get("id"), (int) toSerialize.get("meta"), (int) toSerialize.get("count"));
                if (!toSerialize.get("customName").equals("Default")) {
                    rewardItem.setCustomName(toSerialize.get("customName").toString());
                }
                if (toSerialize.containsKey("enchantments")) {
                    List<String> enchants = (List<String>) toSerialize.get("enchantments");
                    for (String enchant : enchants) {
                        String[] sep = enchant.split(":");
                        rewardItem.addEnchantment(Enchantment.getEnchantment(Integer.parseInt(sep[0])).setLevel(Integer.parseInt(sep[1])));
                    }
                }
                if (toSerialize.containsKey("lore")) {
                    List<String> lore = (List<String>) toSerialize.get("lore");
                    rewardItem.setLore(lore.toArray(new String[0]));
                }
                int chance = (int) toSerialize.get("chance");
                chanceSum += chance;
                drops.put(chanceSum, rewardItem);
                chances.put(chanceSum, chance);
                if (toSerialize.containsKey("commands")) {
                    commands.put(rewardItem, (List<String>) toSerialize.get("commands"));
                }
            }
        }
    }

    public void initParticles() {
        this.getServer().getScheduler().scheduleRepeatingTask(this, () -> {
            for (Map.Entry<Location, Integer> crate : this.steps.entrySet()) {
                Crate crate1 = this.getCrateLocations().get(crate.getKey().subtract(0.5, 0, 0.5));
                String effect = crate1.getEffect();
                Location location = crate.getKey();
                int step = crate.getValue();
                double radius = 1 * (1 - (double) step / 32);
                for (int i = 0; i < 2; i++) {
                    double angle = step * 16 + (2 * Math.PI * i / 2);
                    Vector3 v = new Vector3(Math.cos(angle) * radius, step * 0.08, Math.sin(angle) * radius);
                    if (effect.equals("flame")) {
                        location.getLevel().addParticle(new FlameParticle(location.clone().add(v)));
                    }
                    else if(effect.equals("portal")){
                        location.getLevel().addParticle(new PortalParticle(location.clone().add(v)));
                    }
                    else if(effect.equals("rainsplash")){
                        location.getLevel().addParticle(new RainSplashParticle(location.clone().add(v)));
                    }
                    else if(effect.equals("redstone")){
                        location.getLevel().addParticle(new RedstoneParticle(location.clone().add(v)));
                        location.getLevel().addParticle(new RedstoneParticle(location.clone().add(v)));
                    }
                    else if(effect.equals("ink")){
                        location.getLevel().addParticle(new InkParticle(location.clone().add(v)));
                    }
                    else if(effect.equals("heart")){
                        location.getLevel().addParticle(new HeartParticle(location.clone().add(v)));
                    }
                    else if(effect.equals("bubble")){
                        location.getLevel().addParticle(new BubbleParticle(location.clone().add(v)));
                    }
                }
                step++;
                if (step > 32) {
                    step = 0;
                }
//            player.getServer().getLogger().info(""+step);
                this.steps.put(location, step);
            }
        }, 2);
    }

    public Map<String, Crate> getCrates() {
        return crates;
    }

    public Map<Location, Crate> getCrateLocations() {
        return crateLocations;
    }

    public Map<Location, FloatingTextParticle> getParticles() {
        return particles;
    }

    public FakeInventories getFakeInventories() {
        return fakeInventories;
    }

    public int getDelay() {
        return delay;
    }

    public int getScroll() {
        return scroll;
    }

    public String getOpenMsg() {
        return openMsg;
    }

    public Map<Player, TaskHandler> getOpenedChest() {
        return openedChest;
    }

    public String getChanceName() {
        return chanceName;
    }

    public Map<Location, Integer> getSteps() {
        return steps;
    }
}
