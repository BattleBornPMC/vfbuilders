package net.tfminecraft.VFBuilders.core;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import me.Plugins.TLibs.TLibs;
import net.tfminecraft.VFBuilders.VFBuilders;
import net.tfminecraft.VFBuilders.loaders.StationLoader;

public class BlueprintCategory {
    private String id;
    private ItemStack item;
    private Station station;

    private List<Blueprint> blueprints = new ArrayList<>();

    private String permission;
    private String menuTitle;
    private boolean menuFillers;
    private ItemStack backButton;
    private int backButtonSlot;


    public BlueprintCategory(String key, ConfigurationSection config) {
        id = key;
        if (config.isConfigurationSection("item")) {
            item = TLibs.getItemAPI().getCreator().getItemFromConfig(config.getConfigurationSection("item"));
            int cmd = config.getInt("item.custom-model-data", -1);
            if (cmd != -1) {
                ItemMeta m = item.getItemMeta();
                m.setCustomModelData(cmd);
                item.setItemMeta(m);
            }
        } else {
            item = new ItemStack(Material.DIRT, 1);
        }
        station = StationLoader.getByString(config.getString("station", "none"));
        permission = config.getString("permission", null);
        menuTitle = config.getString("menu-title", "&7Select Blueprint");
        menuFillers = config.getBoolean("menu-fillers", false);

        // Back button
        Material mat = Material.BARRIER;
        String displayName = "§cBACK";
        int backCmd = -1;
        backButtonSlot = 26;
        if (config.isConfigurationSection("back-button")) {
            ConfigurationSection bb = config.getConfigurationSection("back-button");
            Material parsed = Material.matchMaterial(bb.getString("material", "BARRIER"));
            if (parsed != null) mat = parsed;
            displayName = StringFormatter.formatHex(bb.getString("display-name", "&cBACK"));
            backCmd = bb.getInt("custom-model-data", -1);
            backButtonSlot = bb.getInt("slot", 26);
        }
        backButton = new ItemStack(mat, 1);
        ItemMeta bbMeta = backButton.getItemMeta();
        bbMeta.setDisplayName(displayName);
        if (backCmd != -1) bbMeta.setCustomModelData(backCmd);
        NamespacedKey backKey = new NamespacedKey(VFBuilders.plugin, "vfb_back_button");
        bbMeta.getPersistentDataContainer().set(backKey, PersistentDataType.BYTE, (byte) 1);
        backButton.setItemMeta(bbMeta);
    }

    public String getId() {
        return id;
    }

    public void addBlueprint(Blueprint b) {
        if(blueprints.contains(b)) return;
        blueprints.add(b);
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

    public Station getStation() {
        return station;
    }

    public List<Blueprint> getBlueprints() {
        return blueprints;
    }

    public String getMenuTitle() {
        return menuTitle;
    }

    public boolean hasMenuFillers() {
        return menuFillers;
    }

    public ItemStack getBackButton() {
        return backButton.clone();
    }

    public int getBackButtonSlot() {
        return backButtonSlot;
    }
}
