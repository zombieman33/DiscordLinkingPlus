package me.zombieman.dev.discordlinkingplus.database.mysql;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import me.zombieman.dev.discordlinkingplus.database.mysql.data.DiscordData;
import me.zombieman.dev.discordlinkingplus.database.mysql.data.DiscordLinkingData;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DiscordDatabase {
    private final Dao<DiscordData, String> dataDao;
    private final ConnectionSource connectionSource;

    public DiscordDatabase(String jdbcUrl, String username, String password) throws SQLException {
        connectionSource = new JdbcConnectionSource(jdbcUrl, username, password);
        TableUtils.createTableIfNotExists(connectionSource, DiscordData.class);
        dataDao = DaoManager.createDao(connectionSource, DiscordData.class);
        System.out.println("Discord database connection established and tables checked.");
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

