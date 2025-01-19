package me.zombieman.dev.discordlinkingplus.database.mysql;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import me.zombieman.dev.discordlinkingplus.database.mysql.data.ServerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ServerDatabase {
    private final Dao<ServerData, String> dataDao;
    private final ConnectionSource connectionSource;
    private final DiscordLinkingPlus plugin;

    public ServerDatabase(DiscordLinkingPlus plugin, String jdbcUrl, String username, String password) throws SQLException {
        this.plugin = plugin;
        connectionSource = new JdbcConnectionSource(jdbcUrl, username, password);
        TableUtils.createTableIfNotExists(connectionSource, ServerData.class);
        dataDao = DaoManager.createDao(connectionSource, ServerData.class);
        System.out.println("Database connection established and tables checked.");
        start();
    }

    public void close() {
        try {
            if (connectionSource != null && connectionSource.isOpen("default")) {
                connectionSource.close();
                System.out.println("Database connection closed.");
            }
        } catch (Exception e) {
            System.out.println("Failed to close database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<ServerData> getData() throws SQLException {
        return dataDao.queryForAll();
    }

    public void purge() throws SQLException {

        dataDao.executeRaw("DELETE FROM %s WHERE lastUpdated < %s".formatted(dataDao.getTableName(), String.valueOf(System.currentTimeMillis() - 60000)));

    }

    public void update() throws SQLException {
        ServerData data = new ServerData();
        data.setUuid(plugin.sessionID.toString());
        data.setServerID(plugin.getConfig().getString("server.name", "n/a"));
        data.setLastUpdated(System.currentTimeMillis());
        dataDao.createOrUpdate(data);
    }

    public void start() {
        System.out.println("Started server checking...");
        new BukkitRunnable() {
            @Override
            public void run() {

                try {

                    update();

                    purge();

                    plugin.getServerListCache().fetchServers();

                } catch (SQLException e) {
                    e.printStackTrace();
                }

            }
        }.runTaskTimerAsynchronously(plugin, 0L, 20L * 30);
    }
}