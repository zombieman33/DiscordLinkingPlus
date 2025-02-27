package me.zombieman.dev.discordlinkingplus.api;

import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import org.bukkit.Bukkit;
import redis.clients.jedis.Jedis;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class API {

    private final DiscordLinkingPlus plugin;

    public API(DiscordLinkingPlus plugin) {
        this.plugin = plugin;
    }

    public String getDiscordTag(UUID uuid) throws SQLException {
        return plugin.getPlayerDatabase().getPlayerData(uuid).getDiscordTag();
    }
    public boolean isLinked(UUID uuid) throws SQLException {
        return plugin.getPlayerDatabase().getPlayerData(uuid).isLinked();
    }
    public UUID getUUIDFromDiscordTag(String discordTag) throws SQLException {
        return plugin.getPlayerDatabase().getUuidByDiscordTag(discordTag);
    }
    public String getUsernameFromDiscordTag(String discordTag) throws SQLException {
        return plugin.getPlayerDatabase().getUsernameByDiscordTag(discordTag);
    }
    public UUID getUUIDFromUsername(String username) throws SQLException {
        return plugin.getPlayerDatabase().getUuidByUsername(username);
    }

    public void sendMessageToDiscordUser(String discordTag, String message) {

        message = message.replace(" ", "[SPACE]")
                .replace("\n", "[NEW-LINE]")
                .replace("*", "[STAR]");

        String finalMessage = message;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Jedis jedis = plugin.getJedisResource()) {
                jedis.publish("DISCORD_LINKING", "DISCORD_MESSAGE:" + discordTag + ":" + finalMessage);
            }
        });
    }

    public String formatMessage(String message) {

        message = message.replace(" ", "[SPACE]")
                .replace("\n", "[NEW-LINE]")
                .replace("*", "[STAR]");

        return message;
    }
}
