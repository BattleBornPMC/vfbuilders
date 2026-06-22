package net.tfminecraft.VFBuilders.hooks;

import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import net.tfminecraft.VFBuilders.core.ActiveStation;
import net.tfminecraft.VFBuilders.core.Station;
import net.tfminecraft.VFBuilders.loaders.StationLoader;
import net.tfminecraft.VFBuilders.managers.StationManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class NexoHook implements Listener {

    private final StationManager stationManager;

    public NexoHook(StationManager stationManager) {
        this.stationManager = stationManager;
    }

    @EventHandler
    public void onFurnitureInteract(NexoFurnitureInteractEvent e) {
        String furnitureId = e.getMechanic().getItemID();
        Station station = findStationByNexoId(furnitureId);
        if (station == null) return;

        e.setCancelled(true);
        Location loc = e.getBaseEntity().getLocation();
        ActiveStation activeStation = stationManager.getOrCreateStation(loc, station);
        Player p = e.getPlayer();
        if (!activeStation.hasBlueprint()) {
            stationManager.openCategoryView(p, activeStation);
        }
    }

    @EventHandler
    public void onFurnitureBreak(NexoFurnitureBreakEvent e) {
        String furnitureId = e.getMechanic().getItemID();
        if (findStationByNexoId(furnitureId) == null) return;

        Player p = e.getPlayer();
        if (p == null) return;
        Location loc = e.getBaseEntity().getLocation();
        stationManager.removeStation(loc, p);
    }

    private Station findStationByNexoId(String nexoId) {
        String expected = "nexo:" + nexoId;
        for (Station station : StationLoader.get().values()) {
            if (station.getBlock().equals(expected)) return station;
        }
        return null;
    }
}
