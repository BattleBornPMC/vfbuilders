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

    private String permission;

    public Blueprint(String key, ConfigurationSection config) {
        id = key;
        category = CategoryLoader.getByString(config.getString("category", "none"));
        if(category != null) {
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
        for(String s : config.getStringList("inputs")) {
            String input = s.split("\\s+")[0];
            Integer amount = 1;
            try {
                amount = Integer.parseInt(s.split("\\s+")[1]);
            } catch (Exception e) {
                VFLogger.log(VFBuilders.plugin, s+" is not a correctly formulated input for the blueprint "+key);
            }
            inputs.put(input, amount);
        }
        permission = config.getString("permission", null);
        vehicle = VehicleLoader.getByString(config.getString("vehicle", "none"));
    }

    public String getId() {
        return id;
    }

    public boolean hasVehicle() {
        return vehicle != null;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public boolean hasPermission() {
        return permission != null;
    }

    public String getPermission() {
        return permission;
    }

    public ItemStack getItem() {
        return item;
    }

    public int getTime() {
        return time;
    }

    public int getSlot() {
        return slot;
    }

    public boolean hasSlot() {
        return slot >= 0;
    }

    public HashMap<String, Integer> getInputs() {
        return inputs;
    }

    @SuppressWarnings("unchecked")
    public boolean hasInputs(Player p) {
        HashMap<String, Integer> items = (HashMap<String, Integer>) inputs.clone();
        for(ItemStack i : p.getInventory().getContents()) {
            if (i == null || i.getType() == Material.AIR) continue;
            String path = api.getChecker().getAsStringPath(i);
            if(items.containsKey(path)) {
                int current = items.get(path);
                current-=i.getAmount();
                if(current > 0) {
                    items.put(path, current);
                } else {
                    items.remove(path);
                }
            }
            // Break early if we've hit the target
            if (items.isEmpty()) break;
        }
        return items.size() == 0;
    }

    @SuppressWarnings("unchecked")
    public void takeInputs(Player p) {
        HashMap<String, Integer> items = (HashMap<String, Integer>) inputs.clone();

        for (ItemStack i : p.getInventory().getContents()) {
            if (i == null || i.getType() == Material.AIR) continue;

            String path = api.getChecker().getAsStringPath(i);
            if (!items.containsKey(path)) continue;

            int required = items.get(path);
            int available = i.getAmount();

            if (available >= required) {
                i.setAmount(available - required); // Decrease the stack
                items.remove(path); // All required amount taken
            } else {
                i.setAmount(0); // Remove entire stack
                items.put(path, required - available); // Still need more of this item
            }

            // Break early if we've taken all required items
            if (items.isEmpty()) break;
        }

        // Optionally update inventory to reflect changes
        p.updateInventory();
    }

    public void drop(Location loc) {
        // Play a cool burst sound and particles
        loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0.5, 1, 0.5), 15, 0.3, 0.3, 0.3, 0.01);
        loc.getWorld().spawnParticle(Particle.CRIT_MAGIC, loc.clone().add(0.5, 1, 0.5), 20, 0.2, 0.2, 0.2, 0.05);
        loc.getWorld().playSound(loc, Sound.BLOCK_BARREL_CLOSE, 1f, 1.3f);

        // Drop all items
        for (Map.Entry<String, Integer> entry : inputs.entrySet()) {
            String path = entry.getKey();
            int amount = entry.getValue();

            ItemStack item = api.getCreator().getItemFromPath(path);
            item.setAmount(amount);

            loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 1, 0.5), item);
        }
    }
}
