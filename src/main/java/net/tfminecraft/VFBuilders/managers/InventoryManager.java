package net.tfminecraft.VFBuilders.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import org.bukkit.ChatColor;

import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Objects.TLibAPI;
import me.Plugins.TLibs.Objects.API.SubAPI.ItemCreator;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import me.Plugins.TLibs.Utils.TimeFormatter;
import net.tfminecraft.VFBuilders.Cache;
import net.tfminecraft.VFBuilders.VFBuilders;
import net.tfminecraft.VFBuilders.core.ActiveStation;
import net.tfminecraft.VFBuilders.core.Blueprint;
import net.tfminecraft.VFBuilders.core.BlueprintCategory;
import net.tfminecraft.VFBuilders.enums.VFBGUI;
import net.tfminecraft.VFBuilders.holders.VFBHolder;
import net.tfminecraft.VFBuilders.loaders.CategoryLoader;
import net.tfminecraft.VehicleFramework.Vehicles.Vehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.VehicleComponent;
import net.tfminecraft.VehicleFramework.Weapons.Weapon;

public class InventoryManager {
    public void categoryView(Inventory i, Player p, ActiveStation s, boolean open) {
		if(open) {
			String title = ChatColor.translateAlternateColorCodes('&', me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter.formatHex(s.getStation().getMenuTitle()));
			i = VFBuilders.plugin.getServer().createInventory(new VFBHolder(s, VFBGUI.CATEGORY), 27, title);
		}
		// First pass: place categories with explicit slots
		for(BlueprintCategory cat : CategoryLoader.get().values()) {
			if(!cat.getStation().equals(s.getStation())) continue;
			if(cat.hasPermission() && !p.hasPermission(cat.getPermission())) continue;
			if(cat.hasSlot()) {
				i.setItem(cat.getSlot(), getCategoryItem(p, cat));
			}
		}
		// Second pass: fill remaining categories sequentially into unoccupied slots
		int x = 0;
		for(BlueprintCategory cat : CategoryLoader.get().values()) {
			if(!cat.getStation().equals(s.getStation())) continue;
			if(cat.hasPermission() && !p.hasPermission(cat.getPermission())) continue;
			if(cat.hasSlot()) continue;
			while(x < i.getSize() && i.getItem(x) != null) x++;
			if(x < i.getSize()) {
				i.setItem(x, getCategoryItem(p, cat));
				x++;
			}
		}
		if(Cache.categoryMenuFillers) {
			x = 0;
			while(x < i.getSize()) {
				if(i.getItem(x) == null) {
					ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
					ItemMeta fm = fill.getItemMeta();
					fm.setDisplayName("§8 ");
					fill.setItemMeta(fm);
					i.setItem(x, fill);
				}
				x++;
			}
		}
		if(open) {
			p.openInventory(i);
		}
	}
	public void blueprintView(Inventory i, Player p, BlueprintCategory cat, ActiveStation s, boolean open) {
		if(open) {
			String title = ChatColor.translateAlternateColorCodes('&', me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter.formatHex(cat.getMenuTitle()));
			i = VFBuilders.plugin.getServer().createInventory(new VFBHolder(s, VFBGUI.BLUEPRINT), 27, title);
		}
		// First pass: place blueprints with explicit slots
		for(Blueprint b : cat.getBlueprints()) {
			if(!b.hasVehicle()) continue;
			if(b.hasPermission() && !p.hasPermission(b.getPermission())) continue;
			if(b.hasSlot()) {
				i.setItem(b.getSlot(), getBlueprintItem(b));
			}
		}
		// Second pass: fill remaining blueprints sequentially into unoccupied slots
		int x = 0;
		for(Blueprint b : cat.getBlueprints()) {
			if(!b.hasVehicle()) continue;
			if(b.hasPermission() && !p.hasPermission(b.getPermission())) continue;
			if(b.hasSlot()) continue;
			while(x < i.getSize() && i.getItem(x) != null) x++;
			if(x < i.getSize()) {
				i.setItem(x, getBlueprintItem(b));
				x++;
			}
		}
		if(cat.hasMenuFillers()) {
			x = 0;
			while(x < i.getSize()) {
				if(i.getItem(x) == null) {
					ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
					ItemMeta fm = fill.getItemMeta();
					fm.setDisplayName("§8 ");
					fill.setItemMeta(fm);
					i.setItem(x, fill);
				}
				x++;
			}
		}
		i.setItem(cat.getBackButtonSlot(), cat.getBackButton());
		if(open) {
			p.openInventory(i);
		}
	}

    private ItemStack getCategoryItem(Player p, BlueprintCategory cat) {
		ItemStack i = new ItemStack(cat.getItem());
		ItemMeta m = i.getItemMeta();
		NamespacedKey key = new NamespacedKey(VFBuilders.plugin, "vfb_category_id");
		m.getPersistentDataContainer().set(key, PersistentDataType.STRING, cat.getId());
		List<String> lore = m.hasLore() ? new ArrayList<>(m.getLore()) : new ArrayList<>();
		lore.add(" ");
		lore.add(StringFormatter.formatHex("#53db9c"+getBlueprintCount(p, cat)+" #ccbf8fBlueprints"));
		m.setLore(lore);
		i.setItemMeta(m);
		return i;
	}

	private ItemStack getBlueprintItem(Blueprint b) {
		ItemCreator creator = TLibs.getItemAPI().getCreator();
		ItemStack i = new ItemStack(b.getItem());
		ItemMeta m = i.getItemMeta();
		Vehicle v = b.getVehicle();
		m.setDisplayName(v.getName());
		NamespacedKey key = new NamespacedKey(VFBuilders.plugin, "vfb_blueprint_id");
		m.getPersistentDataContainer().set(key, PersistentDataType.STRING, b.getId());
		List<String> lore = m.hasLore() ? new ArrayList<>(m.getLore()) : new ArrayList<>();
		lore.add(" ");
		lore.add(StringFormatter.formatHex(Cache.loreLabel+"Seats§e: "+Cache.loreValue+v.getSeatHandler().getSeats().size()));
		if(v.getWeapons().size() > 0) lore.add(StringFormatter.formatHex(Cache.loreLabel+"Weapons§e: "+Cache.loreValue+v.getWeapons().size()));
		lore.add(" ");
		lore.add(StringFormatter.formatHex(Cache.loreHeader+"Components§e:"));
		for(VehicleComponent c : v.getComponentHandler().getComponents()) {
			lore.add(StringFormatter.formatHex(Cache.loreBullet+"- "+Cache.loreItemName+c.getAlias()+" §7("+Cache.loreHpLabel+"HP§e: "+Cache.loreHpValue+c.getHealthData().getHealth()+"§7)"));
		}
		for(Weapon w : v.getWeapons()) {
			lore.add(StringFormatter.formatHex(Cache.loreBullet+"- "+Cache.loreItemName+w.getName()+" §7("+Cache.loreHpLabel+"HP§e: "+Cache.loreHpValue+w.getHealthData().getHealth()+"§7)"));
		}
		lore.add(" ");
		lore.add(StringFormatter.formatHex(Cache.loreTime+"Time§e: "+Cache.loreTimeValue+TimeFormatter.formatTime(b.getTime())));
		lore.add(StringFormatter.formatHex(Cache.loreLabel+"§lInputs:"));
		for(Map.Entry<String, Integer> entry : b.getInputs().entrySet()) {
			String name = entry.getKey();
			try {
				ItemStack input = creator.getItemFromPath(entry.getKey());
				name = StringFormatter.getVanillaName(input.getType());
				if(input.getItemMeta().hasDisplayName()) name = input.getItemMeta().getDisplayName();
			} catch (Exception ignored) {}
			lore.add(StringFormatter.formatHex(Cache.loreBullet+"- "+Cache.loreInputName+name+"§e: "+Cache.loreInputAmt+entry.getValue()));
		}
		if(b.hasTools()) {
			lore.add(" ");
			lore.add(StringFormatter.formatHex(Cache.loreToolHeader+"§lTools §7(returned):"));
			for(Map.Entry<String, Integer> entry : b.getTools().entrySet()) {
				String name = entry.getKey();
				try {
					ItemStack tool = creator.getItemFromPath(entry.getKey());
					name = StringFormatter.getVanillaName(tool.getType());
					if(tool.getItemMeta().hasDisplayName()) name = tool.getItemMeta().getDisplayName();
				} catch (Exception ignored) {}
				lore.add(StringFormatter.formatHex(Cache.loreBullet+"- "+Cache.loreInputName+name+"§e: "+Cache.loreInputAmt+entry.getValue()));
			}
		}
		m.setLore(lore);
		i.setItemMeta(m);
		return i;
	}

	private int getBlueprintCount(Player p, BlueprintCategory cat) {
		int count = 0;
		for(Blueprint b : cat.getBlueprints()) {
			if(b.hasPermission()) {
				if(p.hasPermission(b.getPermission())) count++;
			} else {
				count++;
			}
		}

		return count;
	}
}
