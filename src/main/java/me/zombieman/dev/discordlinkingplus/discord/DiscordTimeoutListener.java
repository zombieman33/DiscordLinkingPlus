package me.zombieman.dev.discordlinkingplus.discord;

import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import me.zombieman.dev.discordlinkingplus.database.mysql.data.DiscordLinkingData;
import net.dv8tion.jda.api.events.guild.GuildTimeoutEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberUpdateEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateTimeOutEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

import java.time.format.DateTimeFormatter;

public class DiscordTimeoutListener extends ListenerAdapter {

    private final DiscordLinkingPlus plugin;

    public DiscordTimeoutListener(DiscordLinkingPlus plugin) {
        this.plugin = plugin;
        start();
        System.out.println("Initialized timeout listener");
    }

    @Override
    public void onGuildMemberUpdate(@NotNull GuildMemberUpdateEvent event) {

        if (!plugin.isMainServer()) return;

        String userId = event.getMember().getUser().getId();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                OffsetDateTime oldTimeOut = plugin.getDiscordDatabase().getPlayerData(userId).getTimeoutEnd();
                OffsetDateTime newTimeOut = event.getMember().getTimeOutEnd();

                if (oldTimeOut == null && newTimeOut == null) return;

                if (plugin.getPlayerDatabase().getUuidByDiscordTag(userId) == null) return;

                UUID uuid = plugin.getPlayerDatabase().getUuidByDiscordTag(userId);

                if (!plugin.getPlayerDatabase().getPlayerData(uuid).isLinked()) return;

                // Define a DateTimeFormatter to make the time more readable
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss O");

                String message;

                if (oldTimeOut == null && newTimeOut != null) {
                    message = "<#FF0000>You have been timed out until \n<bold>" + newTimeOut.format(formatter) + "</bold>.";
                    plugin.getDiscordDatabase().updateTime(userId, newTimeOut);
                } else if (oldTimeOut != null && newTimeOut == null) {
                    message = "<#00FF00>Your timeout has been removed.";
                    plugin.getDiscordDatabase().updateTime(userId, null);
                } else if (oldTimeOut != null && !oldTimeOut.equals(newTimeOut)) {
                    message = "<#FF0000>Your timeout got updated to:\n<bold>" + newTimeOut.format(formatter) + "</bold>.";
                    plugin.getDiscordDatabase().updateTime(userId, newTimeOut);
                } else {
                    return;
                }

                try (Jedis jedis = plugin.getJedisResource()) {
                    jedis.publish("DISCORD_LINKING", "MINECRAFT_MESSAGE:" + uuid + ":" + message);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void start() {

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {

                try {

                    for (Player player : Bukkit.getOnlinePlayers()) {

                        DiscordLinkingData playerData = plugin.getPlayerDatabase().getPlayerData(player.getUniqueId());

                        if (playerData == null) continue;
                        if (!playerData.isLinked()) continue;

                        String id = playerData.getDiscordTag();

                        if (plugin.getDiscordDatabase().getPlayerData(id).getTimeoutEnd() == null) continue;

                        String message = "n/a";

                        if (!plugin.getGuild().retrieveMemberById(id).complete().isTimedOut()) {
                            plugin.getDiscordDatabase().updateTime(id, null);
                            message = "<#00FF00>Your timeout has been removed.";

                            try (Jedis jedis = plugin.getJedisResource()) {
                                jedis.publish("DISCORD_LINKING", "MINECRAFT_MESSAGE:" + player.getUniqueId().toString() + ":" + message);
                            }
                        }


                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }, 0L, 20 * 60L);

    }
}
