package me.danshi.dCore.cmd;

import me.danshi.dCore.DCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class EcoAdminCmd implements CommandExecutor {
    private final DCore plugin;

    public EcoAdminCmd(DCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 4) {
            sender.sendMessage(plugin.miniMessage().deserialize("/economy <give|set|take> <player> <currency> <amount>"));
            return true;
        }

        var action = args[0].toLowerCase();
        var targetName = args[1];
        var currency = args[2].toLowerCase();
        var eco = plugin.getEconomyManager().getEconomy(currency);

        if (eco == null) {
            var msg = plugin.getMessagesCfg().getString("economy.unknown_currency", "").replace("%currency%", currency);
            sender.sendMessage(plugin.miniMessage().deserialize(msg));
            return true;
        }

        var target = plugin.getServer().getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore()) {
            var msg = plugin.getMessagesCfg().getString("economy.unknown_player", "").replace("%player%", targetName);
            sender.sendMessage(plugin.miniMessage().deserialize(msg));
            return true;
        }

        double amount;
        try { amount = Double.parseDouble(args[3]); } catch (Exception e) { return true; }

        var uuid = target.getUniqueId().toString();
        var db = plugin.getDbManager();

        switch (action) {
            case "give" -> db.getBalance(uuid, currency).thenCompose(bal -> db.setBalance(uuid, currency, bal + amount)).thenRun(() -> {
                sendFeedback(sender, targetName, currency, amount, "give");
                if (target.isOnline()) notifyPlayer(target.getPlayer(), currency, amount, "give");
            });
            case "take" -> db.getBalance(uuid, currency).thenCompose(bal -> db.setBalance(uuid, currency, bal - amount)).thenRun(() -> {
                sendFeedback(sender, targetName, currency, amount, "take");
                if (target.isOnline()) notifyPlayer(target.getPlayer(), currency, amount, "take");
            });
            case "set" -> db.setBalance(uuid, currency, amount).thenRun(() -> {
                sendFeedback(sender, targetName, currency, amount, "set");
                if (target.isOnline()) notifyPlayer(target.getPlayer(), currency, amount, "set");
            });
        }
        return true;
    }

    private void sendFeedback(CommandSender sender, String target, String cur, double amt, String action) {
        var path = "economy." + action;
        var msg = plugin.getMessagesCfg().getString(path, "");
        sender.sendMessage(plugin.miniMessage().deserialize(msg.replace("%player%", target).replace("%currency%", cur).replace("%amount%", String.valueOf(amt))));
    }

    private void notifyPlayer(Player p, String cur, double amt, String action) {
        var path = "economy." + action;
        var lines = plugin.getMessagesCfg().getStringList(path);
        var newBal = plugin.getDbManager().getBalance(p.getUniqueId().toString(), cur).join();
        for (var line : lines) {
            p.sendMessage(plugin.miniMessage().deserialize(line.replace("%currency%", cur).replace("%amount%", String.valueOf(amt)).replace("%new_balance%", plugin.getEconomyManager().getFormatted(cur, newBal))));
        }
    }
}