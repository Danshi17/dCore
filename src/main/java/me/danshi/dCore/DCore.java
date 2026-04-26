package me.danshi.dCore;

import me.arcaniax.hdb.api.DatabaseLoadEvent;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.danshi.dCore.cmd.BalanceCmd;
import me.danshi.dCore.cmd.BaltopCmd;
import me.danshi.dCore.cmd.EcoAdminCmd;
import me.danshi.dCore.db.DatabaseManager;
import me.danshi.dCore.economy.EconomyManager;
import me.danshi.dCore.hook.PAPIHook;
import me.danshi.dCore.menu.MenuListener;
import me.danshi.dCore.menu.MenuRegistry;
import me.danshi.dCore.util.ItemBuilder;
import net.byteflux.libby.BukkitLibraryManager;
import net.byteflux.libby.Library; // Importante añadir esto
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class DCore extends JavaPlugin implements Listener {
    private static DCore instance;
    private DatabaseManager dbManager;
    private EconomyManager economyManager;
    private MenuRegistry menuRegistry;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private FileConfiguration messagesCfg;
    private FileConfiguration menusCfg;

    @Override
    public void onEnable() {
        instance = this;
        setupConfigs();
        initLibby();

        dbManager = new DatabaseManager(this);
        dbManager.init(); // Corregido: Removido el .thenRun() ya que es un método void

        economyManager = new EconomyManager(this);
        economyManager.cacheTop();

        menuRegistry = new MenuRegistry(this);
        menuRegistry.load();

        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(this, this);

        getCommand("dcore").setExecutor(new EcoAdminCmd(this));
        getCommand("economy").setExecutor(new EcoAdminCmd(this));
        getCommand("balance").setExecutor(new BalanceCmd(this));
        getCommand("baltop").setExecutor(new BaltopCmd(this));

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIHook(this).register(); // Recuerda hacer que PAPIHook extienda PlaceholderExpansion
        }
    }

    private void setupConfigs() {
        saveDefaultConfig();

        var msgsFile = new File(getDataFolder(), "messages.yml");
        if (!msgsFile.exists()) saveResource("messages.yml", false);
        messagesCfg = YamlConfiguration.loadConfiguration(msgsFile);

        var menusFile = new File(getDataFolder(), "menus.yml");
        if (!menusFile.exists()) saveResource("menus.yml", false);
        menusCfg = YamlConfiguration.loadConfiguration(menusFile);
    }

    private void initLibby() {
        var libby = new BukkitLibraryManager(this);
        libby.addMavenCentral();

        // Corregido: Uso correcto del Library Builder de Libby
        libby.loadLibrary(Library.builder().groupId("com.zaxxer").artifactId("HikariCP").version("5.1.0").build());
        libby.loadLibrary(Library.builder().groupId("com.mysql").artifactId("mysql-connector-j").version("8.3.0").build());
        libby.loadLibrary(Library.builder().groupId("com.h2database").artifactId("h2").version("2.2.224").build());
        libby.loadLibrary(Library.builder().groupId("org.mongodb").artifactId("mongodb-driver-sync").version("4.11.1").build());
    }

    @EventHandler
    public void onHDBLoad(DatabaseLoadEvent e) {
        ItemBuilder.setHdbApi(new HeadDatabaseAPI());
    }

    @Override
    public void onDisable() {
        if (dbManager != null) dbManager.shutdown();
    }

    public static DCore get() { return instance; }
    public DatabaseManager getDbManager() { return dbManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public MenuRegistry getMenuRegistry() { return menuRegistry; }
    public MiniMessage miniMessage() { return mm; }
    public FileConfiguration getMessagesCfg() { return messagesCfg; }
    public FileConfiguration getMenusCfg() { return menusCfg; }
}