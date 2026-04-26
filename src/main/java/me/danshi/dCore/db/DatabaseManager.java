package me.danshi.dCore.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOptions; // Importación necesaria
import com.zaxxer.hikari.HikariDataSource;
import me.danshi.dCore.DCore;
import org.bson.Document; // Importación necesaria
import org.h2.jdbcx.JdbcConnectionPool;
import org.bukkit.configuration.file.FileConfiguration; // Importación necesaria

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseManager {
    private final DCore plugin;
    // Cambiado a un pool fijo si no usas Java 21+.
    // Si usas Java 21, puedes volver a: Executors.newVirtualThreadPerTaskThreadPool();
    private final ExecutorService pool = Executors.newFixedThreadPool(4);
    private HikariDataSource mysqlPool;
    private JdbcConnectionPool h2Pool;
    private MongoClient mongoClient;
    private String dbType;

    public DatabaseManager(DCore plugin) { this.plugin = plugin; }

    public void init() {
        FileConfiguration cfg = plugin.getConfig();
        dbType = cfg.getString("database.type", "H2").toUpperCase();
        switch (dbType) {
            case "MYSQL" -> initMySQL(cfg);
            case "MONGODB" -> initMongo(cfg);
            default -> initH2(cfg);
        }
    }

    // Corregido: No se puede usar 'var' en parámetros de métodos
    private void initMySQL(FileConfiguration cfg) {
        mysqlPool = new HikariDataSource();
        mysqlPool.setJdbcUrl("jdbc:mysql://" + cfg.getString("database.host") + ":" + cfg.getString("database.port") + "/" + cfg.getString("database.name") + "?useSSL=false");
        mysqlPool.setUsername(cfg.getString("database.user"));
        mysqlPool.setPassword(cfg.getString("database.pass"));
        mysqlPool.setMaximumPoolSize(10);
    }

    private void initH2(FileConfiguration cfg) {
        h2Pool = JdbcConnectionPool.create("jdbc:h2:file:" + plugin.getDataFolder().getAbsolutePath() + "/data;AUTO_SERVER=TRUE", "sa", "");
    }

    private void initMongo(FileConfiguration cfg) {
        var uri = "mongodb://" + cfg.getString("database.user") + ":" + cfg.getString("database.pass") + "@" + cfg.getString("database.host") + ":" + cfg.getString("database.port");
        mongoClient = MongoClients.create(uri);
        mongoClient.getDatabase(cfg.getString("database.name")).getCollection("balances").createIndex(Indexes.ascending("uuid"));
    }

    public CompletableFuture<Void> ensureSchema(String currency) {
        return CompletableFuture.runAsync(() -> {
            if (dbType.equals("MONGODB")) return;
            try (Connection con = dbType.equals("MYSQL") ? mysqlPool.getConnection() : h2Pool.getConnection();
                 PreparedStatement ps = con.prepareStatement("CREATE TABLE IF NOT EXISTS eco_" + currency + " (uuid VARCHAR(36) PRIMARY KEY, balance DOUBLE NOT NULL)")) {
                ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, pool);
    }

    public CompletableFuture<Double> getBalance(String uuid, String currency) {
        return CompletableFuture.supplyAsync(() -> {
            if (dbType.equals("MONGODB")) return getMongoBalance(uuid, currency);
            try (Connection con = dbType.equals("MYSQL") ? mysqlPool.getConnection() : h2Pool.getConnection();
                 PreparedStatement ps = con.prepareStatement("SELECT balance FROM eco_" + currency + " WHERE uuid = ?")) {
                ps.setString(1, uuid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getDouble("balance");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0.0;
        }, pool);
    }

    public CompletableFuture<Void> setBalance(String uuid, String currency, double amount) {
        return CompletableFuture.runAsync(() -> {
            if (dbType.equals("MONGODB")) {
                setMongoBalance(uuid, currency, amount);
                return;
            }
            try (Connection con = dbType.equals("MYSQL") ? mysqlPool.getConnection() : h2Pool.getConnection();
                 PreparedStatement ps = con.prepareStatement(
                         dbType.equals("MYSQL") ?
                                 "INSERT INTO eco_" + currency + " (uuid, balance) VALUES (?, ?) ON DUPLICATE KEY UPDATE balance = ?" :
                                 "MERGE INTO eco_" + currency + " (uuid, balance) KEY(uuid) VALUES (?, ?)")) {

                ps.setString(1, uuid);
                ps.setDouble(2, amount);
                if (dbType.equals("MYSQL")) ps.setDouble(3, amount);

                ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, pool);
    }

    public CompletableFuture<Map<String, Double>> getTop(String currency, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Double> top = new LinkedHashMap<>();
            if (dbType.equals("MONGODB")) return getMongoTop(currency, limit);
            try (Connection con = dbType.equals("MYSQL") ? mysqlPool.getConnection() : h2Pool.getConnection();
                 PreparedStatement ps = con.prepareStatement("SELECT uuid, balance FROM eco_" + currency + " ORDER BY balance DESC LIMIT ?")) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) top.put(rs.getString("uuid"), rs.getDouble("balance"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return top;
        }, pool);
    }

    private double getMongoBalance(String uuid, String currency) {
        var db = mongoClient.getDatabase(plugin.getConfig().getString("database.name"));
        var doc = db.getCollection("balances").find(new Document("uuid", uuid)).first();
        if (doc == null) return 0.0;
        return doc.get(currency) instanceof Double ? doc.getDouble(currency) : 0.0;
    }

    private void setMongoBalance(String uuid, String currency, double amount) {
        var db = mongoClient.getDatabase(plugin.getConfig().getString("database.name"));
        db.getCollection("balances").updateOne(
                new Document("uuid", uuid),
                new Document("$set", new Document(currency, amount)),
                new UpdateOptions().upsert(true)
        );
    }

    private Map<String, Double> getMongoTop(String currency, int limit) {
        Map<String, Double> top = new LinkedHashMap<>();
        var db = mongoClient.getDatabase(plugin.getConfig().getString("database.name"));
        try (var cursor = db.getCollection("balances").find().sort(new Document(currency, -1)).limit(limit).iterator()) {
            while (cursor.hasNext()) {
                var doc = cursor.next();
                top.put(doc.getString("uuid"), doc.get(currency) instanceof Double ? doc.getDouble(currency) : 0.0);
            }
        }
        return top;
    }

    public void shutdown() {
        if (mysqlPool != null) mysqlPool.close();
        if (h2Pool != null) h2Pool.dispose();
        if (mongoClient != null) mongoClient.close();
        pool.shutdownNow();
    }
}