package net.tfminecraft.VFBuilders;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import net.tfminecraft.VFBuilders.hooks.ItemsAdderHook;
import net.tfminecraft.VFBuilders.hooks.NexoHook;
import net.tfminecraft.VFBuilders.loaders.BlueprintLoader;
import net.tfminecraft.VFBuilders.loaders.CategoryLoader;
import net.tfminecraft.VFBuilders.loaders.ConfigLoader;
import net.tfminecraft.VFBuilders.loaders.StationLoader;
import net.tfminecraft.VFBuilders.managers.CommandManager;
import net.tfminecraft.VFBuilders.managers.StationManager;
import net.tfminecraft.VehicleFramework.VFLogger;

public class VFBuilders extends JavaPlugin {
    public static VFBuilders plugin;

    public static boolean itemsAdderEnabled = false;
    public static boolean nexoEnabled = false;

    private final CommandManager commandManager = new CommandManager();
    private final StationManager stationManager = new StationManager();

    private final ConfigLoader configLoader = new ConfigLoader();
    private final StationLoader stationLoader = new StationLoader();
    private final BlueprintLoader blueprintLoader = new BlueprintLoader();
    private final CategoryLoader categoryLoader = new CategoryLoader();

    @Override
    public void onEnable() {
        plugin = this;
        VFLogger.info(plugin, "Loading...");
        itemsAdderEnabled = Bukkit.getPluginManager().isPluginEnabled("ItemsAdder");
        nexoEnabled = Bukkit.getPluginManager().isPluginEnabled("Nexo");
        if (itemsAdderEnabled) VFLogger.info(plugin, "ItemsAdder found — furniture support enabled.");
        if (nexoEnabled) VFLogger.info(plugin, "Nexo found — Nexo furniture/block support enabled.");
        createFolders();
        createConfigs();
        loadConfigs();
        registerListeners();
        startManagers();
    }

    @Override
    public void onDisable() {
        stationManager.stop();
    }

    public void registerListeners() {
        getServer().getPluginManager().registerEvents(stationManager, this);
        if (itemsAdderEnabled) {
            getServer().getPluginManager().registerEvents(new ItemsAdderHook(stationManager), this);
        }
        if (nexoEnabled) {
            getServer().getPluginManager().registerEvents(new NexoHook(stationManager), this);
        }
        getCommand(commandManager.cmd1).setExecutor(commandManager);
    }

    public void startManagers() {
        stationManager.start();
    }

    public void createFolders() {
        if (!getDataFolder().exists()) getDataFolder().mkdir();
        File subFolder = new File(getDataFolder(), "data");
        if (!subFolder.exists()) subFolder.mkdir();
        subFolder = new File(getDataFolder(), "blueprints");
        if (!subFolder.exists()) subFolder.mkdir();
    }

    public void reload() {
        StationLoader.map.clear();
        CategoryLoader.map.clear();
        BlueprintLoader.map.clear();
        loadConfigs();
        VFLogger.info(plugin, "Reloaded.");
    }

    public void loadConfigs() {
        configLoader.load(new File(getDataFolder(), "config.yml"));
        stationLoader.load(new File(getDataFolder(), "stations.yml"));
        categoryLoader.load(new File(getDataFolder(), "categories.yml"));
        File folder = new File(getDataFolder(), "blueprints");
        VFLogger.info(this, "Loading blueprints...");
        for (final File file : folder.listFiles()) {
            if (!file.isDirectory()) {
                blueprintLoader.load(file);
            }
        }
    }

    public void createConfigs() {
        String[] files = {
            "config.yml",
            "categories.yml",
            "stations.yml",
            "blueprints/example.yml"
        };
        for (String s : files) {
            File newConfigFile = new File(getDataFolder(), s);
            if (!newConfigFile.exists()) {
                newConfigFile.getParentFile().mkdirs();
                saveResource(s, false);
            }
        }
    }
}
