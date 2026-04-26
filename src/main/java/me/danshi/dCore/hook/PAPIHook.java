package me.danshi.dCore.hook;


import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.danshi.dCore.DCore;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PAPIHook extends PlaceholderExpansion {
    private final DCore plugin;

    public PAPIHook(DCore plugin) { this.plugin = plugin; }

    @Override
    public @NotNull String getIdentifier() { return "dCore"; }
    @Override
    public @NotNull String getAuthor() { return "Danshi"; }
    @Override
    public @NotNull String getVersion() { return "1.0.0"; }
    @Override
    public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.startsWith("balance_") && params.endsWith("_formatted")) {
            var currency = params.replace("balance_", "").replace("_formatted", "");
            var eco = plugin.getEconomyManager().getEconomy(currency);
            if (eco == null) return "0.00";
            if (player == null) return "0.00";
            return plugin.getEconomyManager().getFormatted(currency, plugin.getDbManager().getBalance(player.getUniqueId().toString(), currency).join());
        }
        return null;
    }
}