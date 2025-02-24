package me.zombieman.dev.discordlinkingplus.manager;

import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import me.zombieman.dev.discordlinkingplus.database.mysql.data.DiscordLinkingData;
import me.zombieman.dev.discordlinkingplus.database.mysql.data.statistics.LinkStatisticsData;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class StatisticsManager {

    private final DiscordLinkingPlus plugin;

    public StatisticsManager(DiscordLinkingPlus plugin) {
        this.plugin = plugin;
    }

    public int getTotalLinkedUsers() {
        try {
            List<LinkStatisticsData> allStatisticsData = plugin.getLinkStatisticsDatabase().getStatisticsDao().queryForAll();
            int linkedCount = 0;

            for (LinkStatisticsData stats : allStatisticsData) {
                UUID uuid = UUID.fromString(stats.getUuid());
                DiscordLinkingData playerData = plugin.getPlayerDatabase().getPlayerData(uuid);

                if (playerData != null && playerData.isLinked()) {
                    linkedCount++;
                }
            }

            return linkedCount;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
