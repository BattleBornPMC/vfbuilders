package net.tfminecraft.VFBuilders.core;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Objects.API.ItemAPI;
import net.tfminecraft.VFBuilders.VFBuilders;
import net.tfminecraft.VFBuilders.loaders.CategoryLoader;
import net.tfminecraft.VFBuilders.util.ItemHelper;
import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.Loaders.VehicleLoader;
import net.tfminecraft.VehicleFramework.Vehicles.Vehicle;

public class Blueprint {
    private ItemAPI api = TLibs.getItemAPI();

    private String id;
    private BlueprintCategory category;
    private ItemStack item;
    private Vehicle vehicle;

    private int time;
    private int slot;
    private HashMap<String, Integer> inputs = new HashMap<>();
    private HashMap<String, Integer> tools = new HashMap<>();

    private String craftingSound;
    private float craftingSoundVolume;
    private float craftingSoundPitch;
    private int craftingSoundInterval;

    private String permission;

    public Blueprint(String key, ConfigurationSection config) {
        id = key;
        category = CategoryLoader.getByString(config.getString("category", "none"));
        if (category != null) {
            category.addBlueprint(this);
        }
        if (config.isConfigurationSection("item")) {
            item = api.getCreator().getItemFromConfig(config.getConfigurationSection("item"));
            int cmd = config.getInt("item.custom-model-data", -1);
            if (cmd != -1) {
                ItemMeta m = item.getItemMeta();
                m.setCustomModelData(cmd);
                item.setItemMeta(m);
            }
        } else {
            item = new ItemStack(Material.DIRT, 1);
        }
        time = config.getInt("time", 10);
        slot = config.getInt("slot", -1);
        if (config.isConfigurationSection("crafting-sound")) {
            ConfigurationSection cs = config.getConfigurationSection("crafting-sound");
            craftingSound = cs.getString("sound", null);
            craftingSoundVolume = (float) cs.getDouble("volume", 1.0);
            craftingSoundPitch = (float) cs.getDouble("pitch", 1.0);
            craftingSoundInterval = Math.max(1, cs.getInt("interval", 20));
        }
        for (String s : config.getStringList("inputs")) {
            String input = s.split("\\s+")[0];
            Integer amount = 1;
            try {
                amount = Integer.parseInt(s.split("\\s+")[1]);
            } catch (Exception e) {
                VFLogger.log(VFBuilders.plugin, s + " is not a correctly formulated input for the blueprint " + key);
            }
            inputs.put(input, amount);
        }
        for (String s : config.getStringList("tools")) {
            String tool = s.split("\\s+")[0];
            Integer amount = 1;
            try {
                amount = Integer.parseInt(s.split("\\s+")[1]);
            } catch (Exception e) {
                VFLogger.log(VFBuilders.plugin, s + " is not a correctly formulated tool for the blueprint " + key);
            }
            tools.put(tool, amount);
        }
        permission = config.getString("permission", null);
        vehicle = VehicleLoader.getByString(config.getString("vehicle", "none"));
    }

    public String getId() { return id; }
    public boolean hasVehicle() { return vehicle != null; }
    public Vehicle getVehicle() { return vehicle; }
    public boolean hasPermission() { return permission != null; }
    public String getPermission() { return permission; }
    public ItemStack getItem() { return item; }
    public int getTime() { return time; }
    public int getSlot() { return slot; }
    public boolean hasSlot() { return slot >= 0; }
    public boolean hasCraftingSound() { return craftingSound != null; }
    public String getCraftingSound() { return craftingSound; }
    public float getCraftingSoundVolume() { return craftingSoundVolume; }
    public float getCraftingSoundPitch() { return craftingSoundPitch; }
    public int getCraftingSoundInterval() { return craftingSoundInterval; }
    public HashMap<String, Integer> getInputs() { return inputs; }
    public boolean hasTools() { return !tools.isEmpty(); }
    public HashMap<String, Integer> getTools() { return tools; }

    @SuppressWarnings("unchecked")
    public boolean hasRequiredTools(Player p) {
        HashMap<String, Integer> needed = (HashMap<String, Integer>) tools.clone();
        for (ItemStack i : p.getInventory().getContents()) {
            if (i == null || i.getType() == Material.AIR) continue;
            String path = ItemHelper.getPath(i);
            if (path != null && needed.containsKey(path)) {
                int current = needed.get(path) - i.getAmount();
                if (current > 0) needed.put(path, current);
                else needed.remove(path);
            }
            if (needed.isEmpty()) break;
        }
        return needed.isEmpty();
    }

    @SuppressWarnings("unchecked")
    public void takeTools(Player p) {
        HashMap<String, Integer> needed = (HashMap<String, Integer>) tools.clone();
        for (ItemStack i : p.getInventory().getContents()) {
            if (i == null || i.getType() == Material.AIR) continue;
            String path = ItemHelper.getPath(i);
            if (path == null || !needed.containsKey(path)) continue;
            int required = needed.get(path);
            int available = i.getAmount();
            if (available >= required) {
                i.setAmount(available - required);
                needed.remove(path);
            } else {
                i.setAmount(0);
                needed.put(path, required - available);
            }
            if (needed.isEmpty()) break;
        }
        p.updateInventory();
    }

    @SuppressWarnings("unchecked")
    public boolean hasInputs(Player p) {
        HashMap<String, Integer> items = (HashMap<String, Integer>) inputs.clone();
        for (ItemStack i : p.getInventory().getContents()) {
            if (i == null || i.getType() == Material.AIR) continue;
            String path = ItemHelper.getPath(i);
            if (path != null && items.containsKey(path)) {
                int current = items.get(path) - i.getAmount();
                if (current > 0) items.put(path, current);
                else items.remove(path);
            }
            if (items.isEmpty()) break;
        }
        return items.isEmpty();
    }

    @SuppressWarnings("unchecked")
    public void takeInputs(Player p) {
        HashMap<String, Integer> items = (HashMap<String, Integer>) inputs.clone();
        for (ItemStack i : p.getInventory().getContents()) {
            if (i == null || i.getType() == Material.AIR) continue;
            String path = ItemHelper.getPath(i);
            if (path == null || !items.containsKey(path)) continue;
            int required = items.get(path);
            int available = i.getAmount();
            if (available >= required) {
                i.setAmount(available - required);
                items.remove(path);
            } else {
                i.setAmount(0);
                items.put(path, required - available);
            }
            if (items.isEmpty()) break;
        }
        p.updateInventory();
    }

    public void drop(Location loc) {
        loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0.5, 1, 0.5), 15, 0.3, 0.3, 0.3, 0.01);
        loc.getWorld().spawnParticle(Particle.CRIT_MAGIC, loc.clone().add(0.5, 1, 0.5), 20, 0.2, 0.2, 0.2, 0.05);
        loc.getWorld().playSound(loc, Sound.BLOCK_BARREL_CLOSE, 1f, 1.3f);
        for (Map.Entry<String, Integer> entry : inputs.entrySet()) {
            try {
                ItemStack drop = ItemHelper.fromPath(entry.getKey());
                if (drop == null) continue;
                drop.setAmount(entry.getValue());
                loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 1, 0.5), drop);
            } catch (Exception ignored) {}
        }
    }

    public void dropTools(Location loc) {
        for (Map.Entry<String, Integer> entry : tools.entrySet()) {
            try {
                ItemStack drop = ItemHelper.fromPath(entry.getKey());
                if (drop == null) continue;
                drop.setAmount(entry.getValue());
                loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 1, 0.5), drop);
            } catch (Exception ignored) {}
        }
    }
}
