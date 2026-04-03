package net.tfminecraft.VFBuilders.core;

import org.bukkit.configuration.ConfigurationSection;

public class Station {
    private String id;
    private String block;
    private String menuTitle;

    public Station(String key, ConfigurationSection config) {
        id = key;
        block = config.getString("block", "v.bedrock");
        menuTitle = config.getString("menu-title", "&7Select Category");
    }

    public String getId() {
        return id;
    }

    public String getBlock() {
        return block;
    }

    public String getMenuTitle() {
        return menuTitle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Station)) return false;
        return id.equals(((Station) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
