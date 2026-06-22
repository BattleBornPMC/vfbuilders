package net.tfminecraft.VFBuilders.hooks;

import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import dev.lone.itemsadder.api.Events.FurnitureInteractEvent;
import net.tfminecraft.VFBuilders.core.ActiveStation;
import net.tfminecraft.VFBuilders.managers.StationManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ItemsAdderHook implements Listener {

    private final StationManager stationManager;

    public ItemsAdderHook(StationManager stationManager) {
        this.stationManager = stationManager;
    }

    @EventHandler
    public void onFurnitureInteract(FurnitureInteractEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBukkitEntity().getLocation().getBlock();
        ActiveStation station = stationManager.getStation(b);
        if (station == null) return;
        e.setCancelled(true);
        if (!station.hasBlueprint()) {
            stationManager.openCategoryView(p, station);
        }
    }

    @EventHandler
    public void onFurnitureBreak(FurnitureBreakEvent e) {
        Player p = e.getPlayer();
        if (p == null) return;
        stationManager.removeStation(e.getBukkitEntity().getLocation().getBlock().getLocation(), p);
    }
}
