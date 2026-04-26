package me.danshi.dCore.util;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.danshi.dCore.DCore;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ItemBuilder {
    private static HeadDatabaseAPI hdbApi;

    public static void setHdbApi(HeadDatabaseAPI api) { hdbApi = api; }

    public static ItemStack build(String matStr, String name, List<String> lore) {
        ItemStack item;
        if (matStr.startsWith("hdb-")) {
            item = parseHDB(matStr.substring(4));
        } else {
            item = XMaterial.matchXMaterial(matStr).orElse(XMaterial.BARRIER).parseItem();
        }
        if (item == null) item = XMaterial.BARRIER.parseItem();
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(DCore.get().miniMessage().deserialize(name));
            meta.lore(lore.stream().map(l -> DCore.get().miniMessage().deserialize(l)).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack parseHDB(String id) {
        if (hdbApi == null) return XMaterial.BARRIER.parseItem();
        try {
            return hdbApi.getItemHead(id);
        } catch (NullPointerException e) {
            DCore.get().getLogger().warning("HDB Head missing for ID: " + id);
            return XMaterial.BARRIER.parseItem();
        }
    }
}