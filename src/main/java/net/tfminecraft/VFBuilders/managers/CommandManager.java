package net.tfminecraft.VFBuilders.managers;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.tfminecraft.VFBuilders.VFBuilders;
import net.tfminecraft.VFBuilders.loaders.BlueprintLoader;
import net.tfminecraft.VFBuilders.loaders.CategoryLoader;
import net.tfminecraft.VFBuilders.loaders.StationLoader;

public class CommandManager implements CommandExecutor {

    public String cmd1 = "vfbuilders";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§7VFBuilders §8- §fuse §e/vfbuilders reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("vfbuilders.reload")) {
                sender.sendMessage("§cYou don't have permission to do that.");
                return true;
            }
            VFBuilders.plugin.reload();
            sender.sendMessage("§7VFBuilders §8- §aConfiguration reloaded.");
            return true;
        }

        sender.sendMessage("§cUnknown subcommand. Use §e/vfbuilders reload§c.");
        return true;
    }
}
