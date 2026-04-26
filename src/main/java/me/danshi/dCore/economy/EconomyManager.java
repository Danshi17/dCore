package me.danshi.dCore.economy;


import me.danshi.dCore.DCore;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyManager {
    private final DCore plugin;
    private final Map<String, CustomEconomy> economies = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Double>> topCache = new ConcurrentHashMap<>();

    public EconomyManager(DCore plugin) {
        this.plugin = plugin;
        loadEconomies();
    }

    private void loadEconomies() {
        var sec = plugin.getConfig().getConfigurationSection("economies");
        if (sec == null) return;
        for (var key : sec.getKeys(false)) {
            economies.put(key.toLowerCase(), new CustomEconomy(
                    key.toLowerCase(),
                    sec.getString(key + ".display", "$"),
                    sec.getString(key + ".icon", "PAPER"),
                    sec.getDouble(key + ".start_balance", 0.0),
                    sec.getBoolean(key + ".baltop.enabled", true),
                    sec.getBoolean(key + ".baltop.gui", false),
                    sec.getInt(key + ".baltop.size", 27),
                    sec.getString(key + ".baltop.title", "Top")
            ));
            plugin.getDbManager().ensureSchema(key.toLowerCase());
        }
    }

    public String getFormatted(String currency, double amount) {
        var eco = economies.get(currency.toLowerCase());
        var bal = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).toString();
        return eco == null ? bal : eco.display() + bal;
    }

    public CustomEconomy getEconomy(String name) { return economies.get(name.toLowerCase()); }
    public Map<String, CustomEconomy> getEconomies() { return economies; }
    public Map<String, Map<String, Double>> getTopCache() { return topCache; }

    public void cacheTop() {
        for (var eco : economies.values()) {
            if (eco.baltopEnabled()) {
                plugin.getDbManager().getTop(eco.id(), 10).thenAccept(map -> topCache.put(eco.id(), map));
            }
        }
    }
}