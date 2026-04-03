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
	}
    
}
