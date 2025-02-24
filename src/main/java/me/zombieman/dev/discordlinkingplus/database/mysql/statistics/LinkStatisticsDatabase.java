package me.zombieman.dev.discordlinkingplus.database.mysql.statistics;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import me.zombieman.dev.discordlinkingplus.database.mysql.data.DiscordLinkingData;
import me.zombieman.dev.discordlinkingplus.database.mysql.data.statistics.LinkStatisticsData;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LinkStatisticsDatabase {
    private final Dao<LinkStatisticsData, String> statisticsDao;
    private final ConnectionSource connectionSource;
    private final DiscordLinkingPlus plugin;

    public LinkStatisticsDatabase(DiscordLinkingPlus plugin, String jdbcUrl, String username, String password) throws SQLException {
        this.plugin = plugin;
        connectionSource = new JdbcConnectionSource(jdbcUrl, username, password);
        TableUtils.createTableIfNotExists(connectionSource, LinkStatisticsData.class);
        statisticsDao = DaoManager.createDao(connectionSource, LinkStatisticsData.class);
        System.out.println("Statistics database initialized.");
    }

    public void close() {
        try {
            if (connectionSource != null && connectionSource.isOpen("default")) {
                connectionSource.close();
                System.out.println("Statistics database connection closed.");
            }
        } catch (Exception e) {
            System.out.println("Failed to close statistics database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private LinkStatisticsData getOrCreateStatistics(UUID uuid) throws SQLException {
        LinkStatisticsData data = statisticsDao.queryForId(uuid.toString());

        if (data == null) {
            data = new LinkStatisticsData();
            data.setUuid(uuid.toString());
            data.setTotalLinkedTime(0);
            data.setTimesLinked(0);
            data.setLastLinkedTime(0);
            statisticsDao.create(data);
        }

        return data;
    }

    public void trackLink(UUID uuid) throws SQLException {
        LinkStatisticsData data = getOrCreateStatistics(uuid);
        data.setTimesLinked(data.getTimesLinked() + 1);
        data.setLastLinkedTime(System.currentTimeMillis());
        statisticsDao.update(data);
    }

    public void trackUnlink(UUID uuid) throws SQLException {
        LinkStatisticsData data = getOrCreateStatistics(uuid);

        long linkedDuration;
        if (data.getLastLinkedTime() != 0) {
            linkedDuration = System.currentTimeMillis() - data.getLastLinkedTime();
            data.setTotalLinkedTime(data.getTotalLinkedTime() + linkedDuration);
        }

        statisticsDao.update(data);
    }

    public String getFormattedAverageLinkTime() {
        try {
            List<LinkStatisticsData> allData = statisticsDao.queryForAll();
            if (allData.isEmpty()) return "No data available";

            long totalTime = 0;
            long count = 0;

            for (LinkStatisticsData data : allData) {
                if (data.getTimesLinked() > 0) {
                    long currentDuration = System.currentTimeMillis() - data.getLastLinkedTime();
                    totalTime += data.getTotalLinkedTime() + currentDuration;
                    count++;
                }
            }

            if (count == 0) return "No linked users";

            long averageTime = totalTime / count;

            return formatTime(averageTime);
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error retrieving data";
        }
    }


    public Dao<LinkStatisticsData, String> getStatisticsDao() {
        return statisticsDao;
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;

        if (weeks > 0) return weeks + "w " + (days % 7) + "d";
        if (days > 0) return days + "d " + (hours % 24) + "h";
        if (hours > 0) return hours + "h " + (minutes % 60) + "m";
        if (minutes > 0) return minutes + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }
}
