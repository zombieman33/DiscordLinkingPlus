package me.zombieman.dev.discordlinkingplus.discord;

import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import me.zombieman.dev.discordlinkingplus.database.mysql.data.DiscordLinkingData;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateTimeOutEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class DiscordTimeoutListener extends ListenerAdapter {

    private final DiscordLinkingPlus plugin;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss O");

    public DiscordTimeoutListener(DiscordLinkingPlus plugin) {
        this.plugin = plugin;
        startTimeoutChecker();
        System.out.println("Initialized DiscordTimeoutListener for timeouts.");
    }

    @Override
    public void onGuildMemberUpdateTimeOut(@NotNull GuildMemberUpdateTimeOutEvent event) {
        if (!plugin.isMainServer()) return;

        String userId = event.getUser().getId();
        OffsetDateTime newTimeout = event.getNewTimeOutEnd();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                OffsetDateTime oldTimeout = plugin.getDiscordDatabase().getPlayerData(userId).getTimeoutEnd();
                UUID uuid = plugin.getPlayerDatabase().getUuidByDiscordTag(userId);

                if (uuid == null) return;

                DiscordLinkingData playerData = plugin.getPlayerDatabase().getPlayerData(uuid);
                if (playerData == null || !playerData.isLinked()) return;

                String message = null;

                if (oldTimeout == null && newTimeout != null) {
                    message = "<#FF0000>You have been timed out until:\n<bold>" + newTimeout.format(formatter) + "</bold>.";
                    plugin.getDiscordDatabase().updateTime(userId, newTimeout);

                } else if (oldTimeout != null && newTimeout == null) {
                    message = "<#00FF00>Your timeout has been removed.";
                    plugin.getDiscordDatabase().updateTime(userId, null);

                } else if (oldTimeout != null && !oldTimeout.equals(newTimeout)) {
                    message = "<#FF0000>Your timeout was updated to:\n<bold>" + newTimeout.format(formatter) + "</bold>.";
                    plugin.getDiscordDatabase().updateTime(userId, newTimeout);
                }

                if (message != null) {
                    try (Jedis jedis = plugin.getJedisResource()) {
                        jedis.publish("DISCORD_LINKING", "MINECRAFT_MESSAGE:" + uuid + ":" + message);
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void startTimeoutChecker() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    DiscordLinkingData playerData = plugin.getPlayerDatabase().getPlayerData(player.getUniqueId());

                    if (playerData == null || !playerData.isLinked()) continue;

                    String discordId = playerData.getDiscordTag();
                    OffsetDateTime storedTimeout = plugin.getDiscordDatabase().getPlayerData(discordId).getTimeoutEnd();

                    if (storedTimeout == null) continue;

                    boolean stillTimedOut = plugin.getGuild()
                            .retrieveMemberById(discordId)
                            .complete()
                            .isTimedOut();

                    if (!stillTimedOut) {
                        plugin.getDiscordDatabase().updateTime(discordId, null);

                        try (Jedis jedis = plugin.getJedisResource()) {
                            jedis.publish("DISCORD_LINKING", "MINECRAFT_MESSAGE:" + player.getUniqueId() + ":<#00FF00>Your timeout has been removed.");
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, 0L, 20 * 60L); // Every 60 seconds
    }
}
