package me.danshi.dCore.api;



import me.danshi.dCore.DCore;
import me.danshi.dCore.economy.CustomEconomy;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class DCoreAPI {
    public static CompletableFuture<Double> getBalance(String uuid, String currency) {
        return DCore.get().getDbManager().getBalance(uuid, currency);
    }

    public static CompletableFuture<Void> setBalance(String uuid, String currency, double amount) {
        return DCore.get().getDbManager().setBalance(uuid, currency, amount);
    }

    public static Map<String, CustomEconomy> getEconomies() {
        return DCore.get().getEconomyManager().getEconomies();
    }

    public static String formatBalance(String currency, double amount) {
        return DCore.get().getEconomyManager().getFormatted(currency, amount);
    }
}