package me.danshi.dCore.menu;

import me.danshi.dCore.DCore;
import me.danshi.dCore.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuRegistry {
    private final DCore plugin;
    private static final Map<String, MenuData> menus = new HashMap<>();
    private static final Map<String, Integer> playerPages = new HashMap<>();

    public MenuRegistry(DCore plugin) { this.plugin = plugin; }

    public void load() {
        menus.clear();
        var cfg = plugin.getMenusCfg();
        for (var key : cfg.getKeys(false)) {
            var sec = cfg.getConfigurationSection(key);
            if (sec == null) continue;

            String title = sec.getString("title", "Menu");
            int size = sec.getInt("size", 27);
            String perm = sec.getString("open_permission", "");
            String costCur = sec.getString("open_cost.currency", "");
            double costAmt = sec.getDouble("open_cost.amount", 0.0);

            Map<Integer, MenuItem> items = new HashMap<>();
            var itemSec = sec.getConfigurationSection("items");
            if (itemSec != null) {
                for (var itemKey : itemSec.getKeys(false)) {
                    var iSec = itemSec.getConfigurationSection(itemKey);
                    if (iSec == null) continue;

                    String mat = iSec.getString("material", "BARRIER");
                    String name = iSec.getString("name", "");
                    List<String> lore = iSec.getStringList("lore");
                    List<Integer> slots = iSec.getIntegerList("slots");
                    String clickPerm = iSec.getString("click_permission", "");
                    List<Action> actions = parseActions(iSec.getStringList("actions"));

                    ItemStack stack = ItemBuilder.build(mat, name, lore);
                    for (int slot : slots) items.put(slot, new MenuItem(stack, clickPerm, actions));
                }
            }
            menus.put(key, new MenuData(title, size, items, perm, costCur, costAmt));

            if (sec.getBoolean("register_menu_command", false)) {
                var cmds = sec.getStringList("command_open");
                if (!cmds.isEmpty()) {
                    var map = Bukkit.getCommandMap();
                    for (var cmd : cmds) {
                        if (Bukkit.getPluginCommand(cmd) == null) {
                            map.register("dcore", new MenuCommand(cmd, key));
                        }
                    }
                }
            }
        }
    }

    private List<Action> parseActions(List<String> raw) {
        List<Action> actions = new ArrayList<>();
        for (var line : raw) {
            var parts = line.split("]", 2);
            if (parts.length < 2) continue;
            var type = parts[0].replace("[", "").trim();
            var data = parts[1].trim();
            switch (type.toLowerCase()) {
                case "console_cmd" -> actions.add(new Action.Cmd(true, data));
                case "player_cmd" -> actions.add(new Action.Cmd(false, data));
                case "message" -> actions.add(new Action.Msg(data));
                case "sound" -> {
                    var s = data.split(" ");
                    actions.add(new Action.Snd(s[0], s.length > 1 ? Float.parseFloat(s[1]) : 1.0f, s.length > 2 ? Float.parseFloat(s[2]) : 1.0f));
                }
                case "close" -> actions.add(new Action.Close());
                case "give_economy" -> {
                    var e = data.split(" ");
                    actions.add(new Action.EcoGive(e[0], Double.parseDouble(e[1])));
                }
                case "take_economy" -> {
                    var e = data.split(" ");
                    actions.add(new Action.EcoTake(e[0], Double.parseDouble(e[1])));
                }
                case "next_page" -> actions.add(new Action.NextPage());
                case "prev_page" -> actions.add(new Action.PrevPage());
            }
        }
        return actions;
    }

    public static void openMenu(Player p, String menuId) {
        var data = menus.get(menuId);
        if (data == null) return;

        if (!data.perm().isEmpty() && !p.hasPermission(data.perm())) {
            var msg = DCore.get().getMessagesCfg().getString("no_permission", "").replace("%permission%", data.perm());
            p.sendMessage(DCore.get().miniMessage().deserialize(msg));
            return;
        }

        if (!data.costCur().isEmpty() && data.costAmt() > 0) {
            var bal = DCore.get().getDbManager().getBalance(p.getUniqueId().toString(), data.costCur()).join();
            if (bal < data.costAmt()) {
                var msg = DCore.get().getMessagesCfg().getString("economy.insufficient", "");
                p.sendMessage(DCore.get().miniMessage().deserialize(msg));
                return;
            }
            // Corregido: Usamos setBalance con el balance actual menos el costo
            DCore.get().getDbManager().setBalance(p.getUniqueId().toString(), data.costCur(), bal - data.costAmt());
        }

        playerPages.put(p.getName(), 1);
        buildInventory(p, data, 1);
    }

    private static void buildInventory(Player p, MenuData data, int page) {
        var inv = Bukkit.createInventory(null, data.size(), DCore.get().miniMessage().deserialize(data.title()));
        for (var entry : data.items().entrySet()) {
            inv.setItem(entry.getKey() - ((page - 1) * data.size()), entry.getValue().stack());
        }
        p.openInventory(inv);
    }

    public static void nextPage(Player p) {
        var page = playerPages.getOrDefault(p.getName(), 1) + 1;
        playerPages.put(p.getName(), page);
        rebuildCurrent(p, page);
    }

    public static void prevPage(Player p) {
        var page = Math.max(1, playerPages.getOrDefault(p.getName(), 1) - 1);
        playerPages.put(p.getName(), page);
        rebuildCurrent(p, page);
    }

    private static void rebuildCurrent(Player p, int page) {

    }

    public static MenuItem getItem(String title, int slot) {
        for (var data : menus.values()) {
            // Corregido: Se eliminó el paréntesis extra al final de la línea
            if (data.title().equals(title)) return data.items().get(slot + ((playerPages.getOrDefault("dummy", 1) - 1) * data.size()));
        }
        return null;
    }
}

record MenuData(String title, int size, Map<Integer, MenuItem> items, String perm, String costCur, double costAmt) {}
record MenuItem(ItemStack stack, String clickPerm, List<Action> actions) {}