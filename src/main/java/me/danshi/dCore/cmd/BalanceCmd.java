package me.danshi.dCore.cmd;

import me.danshi.dCore.DCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BalanceCmd implements CommandExecutor {
    private final DCore plugin;

    public BalanceCmd(DCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) return true;
        var cur = args.length > 0 ? args[0].toLowerCase() : plugin.getEconomyManager().getEconomies().keySet().iterator().next();
        var eco = plugin.getEconomyManager().getEconomy(cur);
        if (eco == null) {
            var msg = plugin.getMessagesCfg().getString("economy.unknown_currency", "").replace("%currency%", cur);
            p.sendMessage(plugin.miniMessage().deserialize(msg));
            return true;
        }
        plugin.getDbManager().getBalance(p.getUniqueId().toString(), cur).thenAccept(bal -> {
            var msg = plugin.getMessagesCfg().getString("economy.balance", "")
                    .replace("%currency%", eco.display())
                    .replace("%amount%", plugin.getEconomyManager().getFormatted(cur, bal));
            p.sendMessage(plugin.miniMessage().deserialize(msg));
        });
        return true;
    }
}