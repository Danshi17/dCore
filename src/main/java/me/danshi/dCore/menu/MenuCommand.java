package me.danshi.dCore.menu;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MenuCommand extends Command {
    private final String menuId;

    protected MenuCommand(@NotNull String name, String menuId) {
        super(name);
        this.menuId = menuId;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (sender instanceof Player p) MenuRegistry.openMenu(p, menuId);
        return true;
    }
}