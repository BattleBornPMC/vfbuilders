package net.tfminecraft.VFBuilders.core;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;

import me.Plugins.TLibs.Utils.TimeFormatter;
import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Managers.VehicleManager;
import net.tfminecraft.VFBuilders.loaders.BlueprintLoader;

public class ActiveStation {
    private UUID id;
    private Station station;

    private int timeLeft;
    private Blueprint blueprint;
    private Location loc;
    private Location spawnLoc;

    private ArmorStand hologramTitle;
    private ArmorStand hologramTime;

    
    public ActiveStation(Location loc, Station stored) {
        id = UUID.randomUUID();
        this.loc = loc;
        station = stored;
        timeLeft = 0;
    }

    public ActiveStation(Location loc, Station station, String blueprintId, int timeLeft, Location spawnLoc) {
        this.id = UUID.randomUUID(); // Or persist ID if needed
        this.loc = loc;
        this.station = station;
        this.timeLeft = timeLeft;

        if (blueprintId != null) {
            Blueprint blueprint = BlueprintLoader.getByString(blueprintId);
            if (blueprint != null) {
                this.blueprint = blueprint;
                updateHologram();
            }
        }

        if (spawnLoc != null) {
            setSpawnLocation(spawnLoc);
        }
    }


    public void selectBlueprint(Blueprint b) {
        blueprint = b;
        timeLeft = b.getTime();
        updateHologram();
    }

    public boolean tick() {
        if (timeLeft <= 0) {
            removeHolograms();
            return true;
        }

        timeLeft--;
        updateHologram();

        if (timeLeft == 0) {
            removeHolograms();
        }

        return timeLeft == 0;
    }

    public void complete() {
        if (spawnLoc != null) {
            // Check if any player is within 96 blocks
            boolean hasNearbyPlayer = spawnLoc.getWorld().getPlayers().stream()
                .anyMatch(p -> p.getLocation().distanceSquared(spawnLoc) <= (96 * 96));

            if (!hasNearbyPlayer) {
                return; // Delay completion until a player is nearby
            }

            // Spawn vehicle
            VehicleManager manager = VehicleFramework.getVehicleManager();
            manager.spawn(spawnLoc, blueprint.getVehicle());

            // Visual effects
            Location center = spawnLoc.clone().add(0.5, 1, 0.5);

            center.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, center, 10, 0.3, 0.3, 0.3, 0.05);
            center.getWorld().spawnParticle(Particle.CLOUD, center, 20, 0.5, 0.5, 0.5, 0.01);
            center.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, center, 40, 0.6, 1, 0.6, 0);

            center.getWorld().playSound(center, Sound.ENTITY_IRON_GOLEM_REPAIR, 1f, 1.2f);
            center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.6f);
        }

        // Only clear if completed
        spawnLoc = null;
        blueprint = null;
    }





    public UUID getUuid() {
        return id;
    }

    public Location getLocation() {
        return loc;
    }

    public Station getStation() {
        return station;
    }

    public boolean hasBlueprint() {
        return blueprint != null;
    }

    public Blueprint getBlueprint() {
        return blueprint;
    }

    public void setTimeLeft(int i) {
        timeLeft = i;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public boolean hasSpawnLocation() {
        return spawnLoc != null;
    }

    public Location getSpawnLocation() {
        return spawnLoc;
    }

    public void setSpawnLocation(Location loc) {
        spawnLoc = loc;
    }

    private void updateHologram() {
        if (!hasBlueprint()) return;
        if (loc.getWorld() == null || !loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) return;

        String title = "§eConstructing §6" + blueprint.getVehicle().getName();
        String time = "§7Time: §f" + TimeFormatter.formatTime(timeLeft);

        Location baseLoc = loc.clone().add(0.5, 1.4, 0.5);
        Location timeLoc = baseLoc.clone().subtract(0, 0.25, 0); // Slightly below

        if (hologramTitle == null || hologramTitle.isDead()) {
            hologramTitle = spawnHologram(baseLoc, title);
        } else {
            hologramTitle.teleport(baseLoc);
            hologramTitle.setCustomName(title);
        }

        if (hologramTime == null || hologramTime.isDead()) {
            hologramTime = spawnHologram(timeLoc, time);
        } else {
            hologramTime.teleport(timeLoc);
            hologramTime.setCustomName(time);
        }
    }

    private ArmorStand spawnHologram(Location loc, String text) {
        ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setCustomNameVisible(true);
        stand.setCustomName(text);
        stand.setMarker(true);
        stand.setSilent(true);
        return stand;
    }



    public void removeHolograms() {
        if (hologramTitle != null && !hologramTitle.isDead()) {
            hologramTitle.remove();
        }
        if (hologramTime != null && !hologramTime.isDead()) {
            hologramTime.remove();
        }
        hologramTitle = null;
        hologramTime = null;
    }

}
