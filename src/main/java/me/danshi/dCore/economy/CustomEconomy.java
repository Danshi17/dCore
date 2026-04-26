package me.danshi.dCore.economy;

public record CustomEconomy(
        String id,
        String display,
        String icon,
        double startBalance,
        boolean baltopEnabled,
        boolean baltopGui,
        int baltopSize,
        String baltopTitle
) {}