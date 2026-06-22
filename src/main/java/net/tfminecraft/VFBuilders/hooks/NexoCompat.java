package net.tfminecraft.VFBuilders.hooks;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.NexoItems;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

/**
 * Wrapper around Nexo API. Only loaded (and only ever called) when Nexo is enabled,
 * so it's safe to import Nexo classes directly here.
 */
public class NexoCompat {

    /** Returns the Nexo item ID for the given item, or null if it is not a Nexo item. */
    public static String getItemId(ItemStack item) {
        return NexoItems.idFromItem(item);
    }

    /** Builds an ItemStack from a Nexo item ID, or null if the ID is unknown. */
    public static ItemStack buildItem(String nexoId) {
        var wrapper = NexoItems.itemFromId(nexoId);
        return wrapper != null ? wrapper.build() : null;
    }

    /** Returns the Nexo block/furniture ID for the block at this location, or null. */
    public static String getBlockId(Block block) {
        var mechanic = NexoBlocks.customBlockMechanic(block);
        return mechanic != null ? mechanic.getItemID() : null;
    }
}
