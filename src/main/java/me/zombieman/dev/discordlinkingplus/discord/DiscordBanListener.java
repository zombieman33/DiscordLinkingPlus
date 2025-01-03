package me.zombieman.dev.discordlinkingplus.discord;

import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;

import java.sql.SQLException;
import java.util.UUID;

public class DiscordBanListener extends ListenerAdapter {

    private final DiscordLinkingPlus plugin;

    public DiscordBanListener(DiscordLinkingPlus plugin) {
        this.plugin = plugin;
        System.out.println("Initialized ban listener");
    }

    @Override
    public void onGuildBan(@NotNull GuildBanEvent event) {

        if (!plugin.isMainServer()) return;

        String userId = event.getUser().getId();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                UUID uuid = plugin.getPlayerDatabase().getUuidByDiscordTag(userId);

                if (uuid == null) return;

                if (!plugin.getPlayerDatabase().getPlayerData(uuid).isLinked()) return;

                String message = "<#FF0000>You have been <bold>banned</bold> from\nour discord server.";

                try (Jedis jedis = plugin.getJedisResource()) {
                    jedis.publish("DISCORD_LINKING", "MINECRAFT_MESSAGE:" + uuid + ":" + message);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
    @Override
    public void onGuildUnban(@NotNull GuildUnbanEvent event) {

        if (!plugin.isMainServer()) return;

        String userId = event.getUser().getId();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                UUID uuid = plugin.getPlayerDatabase().getUuidByDiscordTag(userId);

                if (uuid == null) return;

                if (!plugin.getPlayerDatabase().getPlayerData(uuid).isLinked()) return;

                String message = "<#00FF00>You have been <bold>unbanned</bold> from\nour Discord server.";

                try (Jedis jedis = plugin.getJedisResource()) {
                    jedis.publish("DISCORD_LINKING", "MINECRAFT_MESSAGE:" + uuid + ":" + message);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
