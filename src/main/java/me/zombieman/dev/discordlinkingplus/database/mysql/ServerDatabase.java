package me.zombieman.dev.discordlinkingplus.database.mysql;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import me.zombieman.dev.discordlinkingplus.database.mysql.data.ServerData;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.List;

public class ServerDatabase {
    private final Dao<ServerData, String> dataDao;
    private final ConnectionSource connectionSource;
    private final HikariDataSource dataSource;
    private final DiscordLinkingPlus plugin;

    public ServerDatabase(DiscordLinkingPlus plugin, String jdbcUrl, String username, String password) throws SQLException {
        this.plugin = plugin;

        // HikariCP
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(600000);
        config.setConnectionTimeout(30000);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("DiscordHikariPool-ServerDatabase");

        dataSource = new HikariDataSource(config);

        connectionSource = new DataSourceConnectionSource(dataSource, jdbcUrl);
        TableUtils.createTableIfNotExists(connectionSource, ServerData.class);
        dataDao = DaoManager.createDao(connectionSource, ServerData.class);

        System.out.println("HikariCP server database connection established and tables checked.");
        start();
    }

    public void close() {
        try {
            if (connectionSource != null) {
                connectionSource.close();
            }
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                System.out.println("HikariCP connection pool closed.");
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
        long cutoff = System.currentTimeMillis() - 60000;
        dataDao.executeRaw("DELETE FROM " + dataDao.getTableName() + " WHERE lastUpdated < " + cutoff);
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
