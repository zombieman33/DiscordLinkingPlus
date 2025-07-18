package me.zombieman.dev.discordlinkingplus.database.mysql;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.zombieman.dev.discordlinkingplus.database.mysql.data.DiscordData;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class DiscordDatabase {
    private final Dao<DiscordData, String> dataDao;
    private final HikariDataSource hikari;
    private final ConnectionSource connectionSource;

    public DiscordDatabase(String jdbcUrl, String username, String password) throws SQLException {
        // HikariCP
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setIdleTimeout(600_000); // 10 min
        config.setMaxLifetime(1_800_000); // 30 min
        config.setConnectionTimeout(30_000); // 30s
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("DiscordHikariPool-DiscordDatabase");

        hikari = new HikariDataSource(config);

        connectionSource = new DataSourceConnectionSource(hikari, jdbcUrl);
        TableUtils.createTableIfNotExists(connectionSource, DiscordData.class);
        dataDao = DaoManager.createDao(connectionSource, DiscordData.class);
        System.out.println("Discord database connection established and tables checked.");
    }

    public void close() {
        try {
            if (connectionSource != null) {
                connectionSource.close();
            }
            if (hikari != null) {
                hikari.close();
            }
            System.out.println("Discord database connection closed.");
        } catch (Exception e) {
            System.out.println("Failed to close Discord database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public DiscordData getPlayerData(String ID) throws SQLException {
        DiscordData data = dataDao.queryForId(ID);
        if (data == null) {
            data = new DiscordData();
            data.setDiscordID(ID);
            data.setTimeoutEnd(null);
            dataDao.create(data);
        }
        return data;
    }

    public void updateTime(String ID, OffsetDateTime offsetDateTime) throws SQLException {
        DiscordData data = dataDao.queryForId(ID);
        if (data != null) {
            data.setTimeoutEnd(offsetDateTime);
            dataDao.update(data);
        }
    }

    public List<String> getAllIDs() throws SQLException {
        List<DiscordData> allData = dataDao.queryForAll();
        List<String> ids = new ArrayList<>();
        for (DiscordData data : allData) {
            ids.add(data.getDiscordID());
        }
        return ids;
    }
}
