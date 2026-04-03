package net.tfminecraft.VFBuilders.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import dev.lone.itemsadder.api.Events.FurnitureInteractEvent;
import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Objects.API.SubAPI.BlockChecker;
import net.tfminecraft.VFBuilders.Cache;
import net.tfminecraft.VFBuilders.VFBuilders;
import net.tfminecraft.VFBuilders.core.ActivePlacement;
import net.tfminecraft.VFBuilders.core.ActiveStation;
import net.tfminecraft.VFBuilders.core.Blueprint;
import net.tfminecraft.VFBuilders.core.BlueprintCategory;
import net.tfminecraft.VFBuilders.core.Station;
import net.tfminecraft.VFBuilders.data.Database;
import net.tfminecraft.VFBuilders.enums.VFBGUI;
import net.tfminecraft.VFBuilders.holders.VFBHolder;
import net.tfminecraft.VFBuilders.loaders.BlueprintLoader;
import net.tfminecraft.VFBuilders.loaders.CategoryLoader;
import net.tfminecraft.VFBuilders.loaders.StationLoader;

public class StationManager implements Listener {
    private HashMap<Location, ActiveStation> stations = new HashMap<>();
    private final Map<UUID, ActivePlacement> activePlacements = new HashMap<>();

    InventoryManager inv = new InventoryManager();

    public ActiveStation getStation(Block b) {
        if(stations.containsKey(b.getLocation())) return stations.get(b.getLocation());
        BlockChecker checker = TLibs.getBlockAPI().getChecker();
		for(Station station : StationLoader.get().values()) {
            if(checker.checkBlock(b, station.getBlock())) {
                ActiveStation a = new ActiveStation(b.getLocation(), station);
                stations.put(b.getLocation(), a);
                return a;
            }
        }
        return null;
	}

    public void start() {
        stations = Database.loadStations();
        startStationTrailLoop();
        tickCycle();
    }

    public void stop() {
        Database.saveStations(stations);
    }

    public void tickCycle() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (ActiveStation station : stations.values()) {
                    if(!station.hasBlueprint()) continue;
                    if(station.tick()) station.complete();
                }
            }
        }.runTaskTimer(VFBuilders.plugin, 0L, 20L); // every 5 ticks (0.25s)
    }

    public void startStationTrailLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (ActiveStation station : stations.values()) {
                    if (!station.hasSpawnLocation()) continue;

                    Location from = station.getLocation().clone().add(0.5, 1, 0.5);
                    Location to = station.getSpawnLocation().clone().add(0, 1, 0);

                    drawParticleLine(from, to);
                }
            }
        }.runTaskTimer(VFBuilders.plugin, 0L, 5L); // every 5 ticks (0.25s)
    }


    @EventHandler
    public void stationInteract(PlayerInteractEvent e) {
        if(!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        Block b = e.getClickedBlock();
        Player p = e.getPlayer();
        ActiveStation station = getStation(b);
        if(station == null) return;
        e.setCancelled(true);
        if(!station.hasBlueprint()) {
            inv.categoryView(null, p, station, true);
        }
    }

    @EventHandler
    public void inventoryClick(InventoryClickEvent e) {
        if(!(e.getView().getTopInventory().getHolder() instanceof VFBHolder)) return;
        VFBHolder h = (VFBHolder) e.getView().getTopInventory().getHolder();
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        ItemStack i = e.getCurrentItem();
        if(i == null) return;
        if(i.getItemMeta() == null) return;
        ItemMeta m = i.getItemMeta();
        NamespacedKey backKey = new NamespacedKey(VFBuilders.plugin, "vfb_back_button");
        if (m.getPersistentDataContainer().has(backKey, PersistentDataType.BYTE)) {
            if(h.getType().equals(VFBGUI.BLUEPRINT)) {
                inv.categoryView(null, p, h.getStation(), true);
            }
            p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
            return;
        }
        NamespacedKey key = new NamespacedKey(VFBuilders.plugin, "vfb_category_id");
        String id = m.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if(id != null) {
            BlueprintCategory cat = CategoryLoader.getByString(id);
            inv.blueprintView(null, p, cat, h.getStation(), true);
            p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
            return;
        }
        key = new NamespacedKey(VFBuilders.plugin, "vfb_blueprint_id");
        id = m.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (id != null) {
            Blueprint b = BlueprintLoader.getByString(id);
            if (!b.hasInputs(p) || !b.hasRequiredTools(p)) {
                p.sendMessage("§cLacking items");
                p.playSound(p, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            p.closeInventory();

            ActivePlacement placement = new ActivePlacement(p, h.getStation(), b);
            activePlacements.put(p.getUniqueId(), placement);
            p.sendMessage("§aSelect spawn location by left-clicking within "+Cache.constructionDistance+" blocks.");
            startParticleTrail(placement); // Method defined below
        }
    }

    @EventHandler
    public void onPlayerLeftClick(PlayerInteractEvent e) {
        if (!e.getAction().name().contains("LEFT_CLICK")) return;

        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();

        if (!activePlacements.containsKey(uuid)) return;

        ActivePlacement placement = activePlacements.get(uuid);
        Location stationLoc = placement.getStation().getLocation();
        Location clickLoc = p.getLocation();

        if (clickLoc.distanceSquared(stationLoc) > Math.pow(Cache.constructionDistance, 2)) {
            p.sendMessage("§cToo far from the station! (max "+Cache.constructionDistance+" blocks)");
            return;
        }

        Blueprint blueprint = placement.getBlueprint();

        // Re-check if player still has the required items and tools
        if (!blueprint.hasInputs(p) || !blueprint.hasRequiredTools(p)) {
            activePlacements.remove(uuid);
            p.sendMessage("§cYou no longer have the required items.");
            p.playSound(p, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Take the inputs and tools, then set the location
        blueprint.takeInputs(p);
        if (blueprint.hasTools()) blueprint.takeTools(p);
        placement.getStation().setSpawnLocation(clickLoc);
        placement.setFinalSpawnLocation(clickLoc);
        ActiveStation station = placement.getStation();
        if (blueprint.hasTools()) station.setCrafter(p.getUniqueId(), blueprint.getTools());
        station.selectBlueprint(blueprint);
        p.sendMessage("§aSpawn location set!");
        p.sendTitle("", "§eStarted Constructing "+blueprint.getVehicle().getName(), 10, 60, 10);
        p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        activePlacements.remove(uuid);
    }



    private void startParticleTrail(ActivePlacement placement) {
        Player player = placement.getPlayer();
        UUID uuid = player.getUniqueId();
        Location stationLoc = placement.getStation().getLocation();

        // Particle trail updater
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!activePlacements.containsKey(uuid)) {
                    cancel(); return;
                }
                if (placement.getStation().hasBlueprint()) {
                    activePlacements.remove(uuid);
                    cancel(); return;
                }
                Location playerLoc = player.getLocation();
                if (playerLoc.distanceSquared(stationLoc) > Math.pow(Cache.constructionDistance, 2)) return;

                drawParticleLine(stationLoc.clone().add(0.5, 1, 0.5),
                                playerLoc.clone().add(0, 1.5, 0));
            }
        }.runTaskTimer(VFBuilders.plugin, 0L, 5L);

        // 30-second timeout
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activePlacements.containsKey(uuid)) {
                    activePlacements.remove(uuid);
                    player.sendMessage("§cVehicle placement cancelled (timeout).");
                }
            }
        }.runTaskLater(VFBuilders.plugin, 20L * 30); // 30 seconds
    }

    private void drawParticleLine(Location start, Location end) {
        if (start.getWorld() == null) return;

        int chunkX = start.getBlockX() >> 4;
        int chunkZ = start.getBlockZ() >> 4;

        if (!start.getWorld().isChunkLoaded(chunkX, chunkZ)) return;

        Vector direction = end.toVector().subtract(start.toVector());
        double length = direction.length();
        direction.normalize();

        for (double d = 0; d < length; d += 0.5) {
            Location point = start.clone().add(direction.clone().multiply(d));
            point.getWorld().spawnParticle(Particle.REDSTONE, point, 1,
                new Particle.DustOptions(Color.LIME, 1.2f));
        }
    }



    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (activePlacements.containsKey(uuid)) {
            activePlacements.remove(uuid);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        Location loc = block.getLocation();

        if (!stations.containsKey(loc)) return;
        e.getPlayer().sendMessage("§cStation removed.");
        remove(loc);
    }

    @EventHandler
    public void onFurnitureInteract(FurnitureInteractEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBukkitEntity().getLocation().getBlock();
        ActiveStation station = getStation(b);
        if(station == null) return;
        e.setCancelled(true);
        if(!station.hasBlueprint()) {
            inv.categoryView(null, p, station, true);
        }
    }

    @EventHandler
    public void onFurnitureBreak(FurnitureBreakEvent e) {
        Location loc = e.getBukkitEntity().getLocation().getBlock().getLocation();

        if (!stations.containsKey(loc)) return;
        e.getPlayer().sendMessage("§cStation removed.");
        remove(loc);
    }

    private void remove(Location loc) {
        ActiveStation station = stations.remove(loc);
        station.removeHolograms();

        if (station.hasBlueprint()) {
            station.getBlueprint().drop(loc.clone().add(0.5, 1, 0.5));
            station.dropPendingTools(loc);
        }

        station.setSpawnLocation(null); // Important to stop the particle trail
    }

}
