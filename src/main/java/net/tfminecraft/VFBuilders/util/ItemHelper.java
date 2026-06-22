package net.tfminecraft.VFBuilders.util;

import me.Plugins.TLibs.TLibs;
import net.tfminecraft.VFBuilders.VFBuilders;
import net.tfminecraft.VFBuilders.hooks.NexoCompat;
import org.bukkit.inventory.ItemStack;

public class ItemHelper {

    public static final String NEXO_PREFIX = "nexo:";

    /**
     * Returns the string path for an ItemStack, compatible with blueprint input/tool keys.
     * Tries TLibs (ItemsAdder + vanilla) first, then Nexo if enabled.
     */
    public static String getPath(ItemStack item) {
        if (item == null) return null;
        String path = TLibs.getItemAPI().getChecker().getAsStringPath(item);
        if (path != null && !path.isEmpty()) return path;
        if (VFBuilders.nexoEnabled) {
            String nexoId = NexoCompat.getItemId(item);
            if (nexoId != null) return NEXO_PREFIX + nexoId;
        }
        return path;
    }

    /**
     * Builds an ItemStack from a path key (vanilla/ItemsAdder via TLibs, or nexo:<id>).
     * Returns null on failure.
     */
    public static ItemStack fromPath(String path) {
        if (path == null) return null;
        if (VFBuilders.nexoEnabled && path.startsWith(NEXO_PREFIX)) {
            return NexoCompat.buildItem(path.substring(NEXO_PREFIX.length()));
        }
        try {
            return TLibs.getItemAPI().getCreator().getItemFromPath(path);
        } catch (Exception e) {
            return null;
        }
    }
}
