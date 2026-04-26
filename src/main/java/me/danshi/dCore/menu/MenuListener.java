package me.danshi.dCore.menu;

import me.danshi.dCore.DCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MenuListener implements Listener {
    private final DCore plugin;

    public MenuListener(DCore plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null || e.getCurrentItem() == null) return;
        var title = plugin.miniMessage().serialize(e.getView().title());
        var item = MenuRegistry.getItem(title, e.getSlot());
        if (item == null) return;

        e.setCancelled(true);
        if (e.getWhoClicked() instanceof Player p) {
            if (!item.clickPerm().isEmpty() && !p.hasPermission(item.clickPerm())) {
                var msg = plugin.getMessagesCfg().getString("no_permission", "").replace("%permission%", item.clickPerm());
                p.sendMessage(plugin.miniMessage().deserialize(msg));
                return;
            }
            for (var action : item.actions()) action.execute(p);
        }
    }
}