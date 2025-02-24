package me.zombieman.dev.discordlinkingplus.database.mysql;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import me.zombieman.dev.discordlinkingplus.database.mysql.data.DiscordLinkingData;
import me.zombieman.dev.discordlinkingplus.database.mysql.data.statistics.LinkStatisticsData;
import me.zombieman.dev.discordlinkingplus.utils.ServerNameUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerDatabase {
    private final Dao<DiscordLinkingData, String> dataDao;
    private final ConnectionSource connectionSource;

    public PlayerDatabase(String jdbcUrl, String username, String password) throws SQLException {
        connectionSource = new JdbcConnectionSource(jdbcUrl, username, password);
        TableUtils.createTableIfNotExists(connectionSource, DiscordLinkingData.class);
        dataDao = DaoManager.createDao(connectionSource, DiscordLinkingData.class);
        System.out.println("Database connection established and tables checked.");
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

    public DiscordLinkingData getPlayerData(UUID uuid, String username) throws SQLException {
        DiscordLinkingData data = dataDao.queryForId(uuid.toString());

        if (data == null) {
            data = new DiscordLinkingData();
            data.setUuid(uuid.toString());
            data.setUsername(username);
            data.setLinked(false);
            data.setHasLinked(false);
            dataDao.create(data);
        }

        return data;
    }
    public DiscordLinkingData getPlayerData(UUID uuid) throws SQLException {
        DiscordLinkingData data = dataDao.queryForId(uuid.toString());

        if (data == null) {
            data = new DiscordLinkingData();
            data.setUuid(uuid.toString());
            data.setUsername("null");
            data.setLinked(false);
            data.setHasLinked(false);
            dataDao.create(data);
        }

        return data;
    }

    public void updateDiscordTag(UUID uuid, String discordTag) throws SQLException {
        DiscordLinkingData data = dataDao.queryForId(uuid.toString());
        if (data != null) {
            data.setDiscordTag(discordTag);
            dataDao.update(data);
        }
    }
    public void updateUsername(UUID uuid, String username) throws SQLException {
        DiscordLinkingData data = dataDao.queryForId(uuid.toString());
        if (data != null) {
            data.setUsername(username);
            dataDao.update(data);
        }
    }

    public void updateLinkStatus(UUID uuid, boolean isLinked, boolean hasLinked) throws SQLException {
        DiscordLinkingData data = dataDao.queryForId(uuid.toString());
        if (data != null) {
            data.setLinked(isLinked);
            data.setHasLinked(hasLinked);
            dataDao.update(data);
        }
    }
    public void updateServers(UUID uuid, String servers) throws SQLException {
        DiscordLinkingData data = dataDao.queryForId(uuid.toString());
        if (data != null) {
            data.setServerClaimedOn(servers);
            dataDao.update(data);
        }
    }

    public List<String> getAllUsernames() throws SQLException {
        List<DiscordLinkingData> allData = dataDao.queryForAll();

        List<String> usernames = new ArrayList<>();

        for (DiscordLinkingData data : allData) {
            usernames.add(data.getUsername());
        }

        return usernames;
    }

    public UUID getUuidByUsername(String username) throws SQLException {
        List<DiscordLinkingData> result = dataDao.queryForEq("username", username);

        if (!result.isEmpty()) {
            return UUID.fromString(result.get(0).getUuid());
        }
        return null;
    }
    public UUID getUuidByDiscordTag(String discordTag) throws SQLException {
        List<DiscordLinkingData> result = dataDao.queryForEq("discordTag", discordTag);

        if (!result.isEmpty()) {
            System.out.println(UUID.fromString(result.get(0).getUuid()));
            return UUID.fromString(result.get(0).getUuid());
        }

        return null;
    }

    public String getUsernameByDiscordTag(String discordTag) throws SQLException {
        List<DiscordLinkingData> result = dataDao.queryForEq("discordTag", discordTag);

        if (!result.isEmpty()) {
            return result.get(0).getUsername();
        }

        return null;
    }
    public boolean isDiscordIdLinked(String discordId) throws SQLException {
        List<DiscordLinkingData> result = dataDao.queryForEq("discordTag", discordId);

        if (!result.isEmpty()) {
            return result.get(0).isLinked();
        }

        return false;
    }

    public void resetClaimedServer(String server) throws SQLException {
        List<DiscordLinkingData> allData = dataDao.queryForAll();

        System.out.println(server);

        for (DiscordLinkingData data : allData) {
            String servers = data.getServerClaimedOn();

            if (servers == null) continue;

            System.out.println(servers);
            List<String> serverList = new ArrayList<>(ServerNameUtil.fromString(servers));

            System.out.println(serverList);

            if (serverList.contains(server)) {
                System.out.println("It contains");
                System.out.println("List:");
                System.out.println(serverList);

                System.out.println("Server: " +server);
                serverList.remove(server);
                String updatedServers = ServerNameUtil.toString(serverList);

                data.setServerClaimedOn(updatedServers);
                dataDao.update(data);
            }
        }

        System.out.println("All claims for server '" + server + "' have been reset.");
    }
    public void populateLinkStatisticsFromDiscordData(Dao<LinkStatisticsData, String> statisticsDao) throws SQLException {
        List<DiscordLinkingData> linkedUsers = dataDao.queryForEq("isLinked", true);

        for (DiscordLinkingData discordData : linkedUsers) {
            UUID uuid = UUID.fromString(discordData.getUuid());

            LinkStatisticsData statisticsData = statisticsDao.queryForId(uuid.toString());

            if (statisticsData == null) {
                statisticsData = new LinkStatisticsData();
                statisticsData.setUuid(uuid.toString());
                statisticsData.setTotalLinkedTime(0);
                statisticsData.setTimesLinked(1);
                statisticsData.setLastLinkedTime(System.currentTimeMillis());
                statisticsDao.create(statisticsData);
                System.out.println("Created new statistics for user: " + discordData.getUsername() + " (UUID: " + uuid + ")");
            } else {
                statisticsData.setTimesLinked(statisticsData.getTimesLinked() + 1);
                statisticsData.setLastLinkedTime(System.currentTimeMillis());
                statisticsDao.update(statisticsData);
                System.out.println("Updated statistics for user: " + discordData.getUsername() + " (UUID: " + uuid + ")");
            }
        }

        System.out.println("Populated link statistics for all linked users.");
    }


}
