package net.tfminecraft.VFBuilders.holders;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import net.tfminecraft.VFBuilders.core.ActiveStation;
import net.tfminecraft.VFBuilders.enums.VFBGUI;

public class VFBHolder implements InventoryHolder {
    private final ActiveStation station;
    private final VFBGUI type;

    public VFBHolder(ActiveStation station, VFBGUI type) {
        this.station = station;
        this.type = type;
    }

    public ActiveStation getStation() {
        return station;
    }

    public VFBGUI getType() {
        return type;
    }

    @Override
    public Inventory getInventory() {
        return null; // Not used in this case
    }
}
