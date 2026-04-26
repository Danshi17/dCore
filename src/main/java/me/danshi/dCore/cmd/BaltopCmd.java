package me.danshi.dCore.cmd;

import me.danshi.dCore.DCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BaltopCmd implements CommandExecutor {
    private final DCore plugin;

    public BaltopCmd(DCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        var cur = args.length > 0 ? args[0].toLowerCase() : plugin.getEconomyManager().getEconomies().keySet().iterator().next();
        var eco = plugin.getEconomyManager().getEconomy(cur);
        if (eco == null || !eco.baltopEnabled()) {
            var msg = plugin.getMessagesCfg().getString("economy.unknown_currency", "");
            sender.sendMessage(plugin.miniMessage().deserialize(msg));
            return true;
        }

        if (eco.baltopGui() && sender instanceof Player p) {
            return true;
        }

        var header = plugin.getMessagesCfg().getString("baltop.header", "").replace("%currency%", eco.display());
        sender.sendMessage(plugin.miniMessage().deserialize(header));

        plugin.getDbManager().getTop(cur, 10).thenAccept(top -> {
            var format = plugin.getMessagesCfg().getString("baltop.format", "");
            int pos = 1;
            for (var entry : top.entrySet()) {
                var name = plugin.getServer().getOfflinePlayer(java.util.UUID.fromString(entry.getKey())).getName();
                if (name == null) name = "Unknown";
                var line = format.replace("%position%", String.valueOf(pos))
                        .replace("%player%", name)
                        .replace("%amount%", plugin.getEconomyManager().getFormatted(cur, entry.getValue()));
                sender.sendMessage(plugin.miniMessage().deserialize(line));
                pos++;
            }
        });
        return true;
    }
}