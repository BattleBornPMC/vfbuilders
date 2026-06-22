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
import net.tfminecraft.VFBuilders.hooks.NexoCompat;
import net.tfminecraft.VFBuilders.loaders.BlueprintLoader;
import net.tfminecraft.VFBuilders.loaders.CategoryLoader;
import net.tfminecraft.VFBuilders.loaders.StationLoader;

public class StationManager implements Listener {
    private HashMap<Location, ActiveStation> stations = new HashMap<>();
    private final Map<UUID, ActivePlacement> activePlacements = new HashMap<>();

    InventoryManager inv = new InventoryManager();

    // -------------------------------------------------------------------------
    // Station lookup / creation
    // -------------------------------------------------------------------------

    public ActiveStation getStation(Block b) {
        Location loc = b.getLocation();
        if (stations.containsKey(loc)) return stations.get(loc);

        BlockChecker checker = TLibs.getBlockAPI().getChecker();
        for (Station station : StationLoader.get().values()) {
            if (checker.checkBlock(b, station.getBlock())) {
                return registerStation(loc, station);
            }
            // Nexo custom blocks (note blocks, tripwires, etc.)
            if (VFBuilders.nexoEnabled && station.getBlock().startsWith("nexo:")) {
                String nexoBlockId = NexoCompat.getBlockId(b);
                if (nexoBlockId != null && ("nexo:" + nexoBlockId).equals(station.getBlock())) {
                    return registerStation(loc, station);
                }
            }
        }
        return null;
    }

    /** Used by NexoHook when a furniture entity is interacted with. */
    public ActiveStation getOrCreateStation(Location entityLoc, Station station) {
        Location loc = entityLoc.getBlock().getLocation();
        if (stations.containsKey(loc)) return stations.get(loc);
        return registerStation(loc, station);
    }

    private ActiveStation registerStation(Location loc, Station station) {
        ActiveStation a = new ActiveStation(loc, station);
        stations.put(loc, a);
        return a;
    }

    public boolean hasStationAt(Location loc) {
        return stations.containsKey(loc.getBlock().getLocation());
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

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
                    if (!station.hasBlueprint()) continue;
                    if (station.tick()) station.complete();
                }
            }
        }.runTaskTimer(VFBuilders.plugin, 0L, 20L);
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
        }.runTaskTimer(VFBuilders.plugin, 0L, 5L);
    }

    // -------------------------------------------------------------------------
    // Public helpers used by hook classes
    // -------------------------------------------------------------------------

    public void openCategoryView(Player p, ActiveStation station) {
        inv.categoryView(null, p, station, true);
    }

    public void removeStation(Location loc, Player p) {
        Location key = loc.getBlock().getLocation();
        if (!stations.containsKey(key)) return;
        p.sendMessage("§cStation removed.");
        remove(key);
    }

    // -------------------------------------------------------------------------
    // Event handlers (vanilla interact / break + inventory)
    // -------------------------------------------------------------------------

    @EventHandler
    public void stationInteract(PlayerInteractEvent e) {
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        Block b = e.getClickedBlock();
        Player p = e.getPlayer();
        ActiveStation station = getStation(b);
        if (station == null) return;
        e.setCancelled(true);
        if (!station.hasBlueprint()) {
            openCategoryView(p, station);
        }
    }

    @EventHandler
    public void inventoryClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof VFBHolder)) return;
        VFBHolder h = (VFBHolder) e.getView().getTopInventory().getHolder();
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        ItemStack i = e.getCurrentItem();
        if (i == null) return;
        if (i.getItemMeta() == null) return;
        ItemMeta m = i.getItemMeta();
        NamespacedKey backKey = new NamespacedKey(VFBuilders.plugin, "vfb_back_button");
        if (m.getPersistentDataContainer().has(backKey, PersistentDataType.BYTE)) {
            if (h.getType().equals(VFBGUI.BLUEPRINT)) {
                inv.categoryView(null, p, h.getStation(), true);
            }
            p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
            return;
        }
        NamespacedKey key = new NamespacedKey(VFBuilders.plugin, "vfb_category_id");
        String id = m.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (id != null) {
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
            p.sendMessage("§aSelect spawn location by left-clicking within " + Cache.constructionDistance + " blocks.");
            startParticleTrail(placement);
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
            p.sendMessage("§cToo far from the station! (max " + Cache.constructionDistance + " blocks)");
            return;
        }

        Blueprint blueprint = placement.getBlueprint();

        if (!blueprint.hasInputs(p) || !blueprint.hasRequiredTools(p)) {
            activePlacements.remove(uuid);
            p.sendMessage("§cYou no longer have the required items.");
            p.playSound(p, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        blueprint.takeInputs(p);
        if (blueprint.hasTools()) blueprint.takeTools(p);
        placement.getStation().setSpawnLocation(clickLoc);
        placement.setFinalSpawnLocation(clickLoc);
        ActiveStation station = placement.getStation();
        if (blueprint.hasTools()) station.setCrafter(p.getUniqueId(), blueprint.getTools());
        station.selectBlueprint(blueprint);
        p.sendMessage("§aSpawn location set!");
        p.sendTitle("", "§eStarted Constructing " + blueprint.getVehicle().getName(), 10, 60, 10);
        p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        activePlacements.remove(uuid);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        activePlacements.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Location loc = e.getBlock().getLocation();
        if (!stations.containsKey(loc)) return;
        e.getPlayer().sendMessage("§cStation removed.");
        remove(loc);
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private void remove(Location loc) {
        ActiveStation station = stations.remove(loc);
        if (station == null) return;
        station.removeHolograms();

        if (station.hasBlueprint()) {
            station.getBlueprint().drop(loc.clone().add(0.5, 1, 0.5));
            station.dropPendingTools(loc);
        }

        station.setSpawnLocation(null);
    }

    private void startParticleTrail(ActivePlacement placement) {
        Player player = placement.getPlayer();
        UUID uuid = player.getUniqueId();
        Location stationLoc = placement.getStation().getLocation();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!activePlacements.containsKey(uuid)) { cancel(); return; }
                if (placement.getStation().hasBlueprint()) {
                    activePlacements.remove(uuid);
                    cancel(); return;
                }
                Location playerLoc = player.getLocation();
                if (playerLoc.distanceSquared(stationLoc) > Math.pow(Cache.constructionDistance, 2)) return;
                drawParticleLine(stationLoc.clone().add(0.5, 1, 0.5), playerLoc.clone().add(0, 1.5, 0));
            }
        }.runTaskTimer(VFBuilders.plugin, 0L, 5L);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (activePlacements.containsKey(uuid)) {
                    activePlacements.remove(uuid);
                    player.sendMessage("§cVehicle placement cancelled (timeout).");
                }
            }
        }.runTaskLater(VFBuilders.plugin, 20L * 30);
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
}
