package net.tfminecraft.VFBuilders.loaders;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import net.tfminecraft.VFBuilders.Cache;
import net.tfminecraft.VFBuilders.VFBuilders;
import net.tfminecraft.VehicleFramework.VFLogger;

public class ConfigLoader {

	public void load(File configFile) {
		VFLogger.info(VFBuilders.plugin, "Loading config...");
		FileConfiguration config = new YamlConfiguration();
        try {
        	config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
        Cache.constructionDistance = config.getInt("construction-max-distance", 8);
        Cache.categoryMenuFillers = config.getBoolean("category-menu-fillers", false);

        Cache.loreLabel      = config.getString("lore-colors.label",       "#ccbf8f");
        Cache.loreValue      = config.getString("lore-colors.value",       "#86d672");
        Cache.loreHeader     = config.getString("lore-colors.header",      "#66ad89");
        Cache.loreBullet     = config.getString("lore-colors.bullet",      "#85817b");
        Cache.loreItemName   = config.getString("lore-colors.item-name",   "#c9a151");
        Cache.loreHpLabel    = config.getString("lore-colors.hp-label",    "#5ad17e");
        Cache.loreHpValue    = config.getString("lore-colors.hp-value",    "#84d19b");
        Cache.loreTime       = config.getString("lore-colors.time",        "#b38372");
        Cache.loreTimeValue  = config.getString("lore-colors.time-value",  "#8fb6c2");
        Cache.loreInputName  = config.getString("lore-colors.input-name",  "#c2b9ac");
        Cache.loreInputAmt   = config.getString("lore-colors.input-amount","#d7d964");
        Cache.loreToolHeader = config.getString("lore-colors.tool-header", "#8fb6c2");
	}
    
}
