package me.zombieman.dev.discordlinkingplus.api;

import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;

import java.sql.SQLException;
import java.util.UUID;

public class API {

    private final DiscordLinkingPlus plugin;

    public API(DiscordLinkingPlus plugin) {
        this.plugin = plugin;
    }

    public String getDiscordTag(UUID uuid) throws SQLException {
        System.out.println("uuid = " + uuid);
        System.out.println("API.getDiscordTag");
        return plugin.getPlayerDatabase().getPlayerData(uuid).getDiscordTag();
    }
    public boolean isLinked(UUID uuid) throws SQLException {
        System.out.println("uuid = " + uuid);
        System.out.println("API.isLinked");
        return plugin.getPlayerDatabase().getPlayerData(uuid).isLinked();
    }
    public UUID getUUIDFromDiscordTag(String discordTag) throws SQLException {
        System.out.println("discordTag = " + discordTag);
        System.out.println("API.getUUIDFromDiscordTag");
        return plugin.getPlayerDatabase().getUuidByDiscordTag(discordTag);
    }
    public String getUsernameFromDiscordTag(String discordTag) throws SQLException {
        System.out.println("discordTag = " + discordTag);
        System.out.println("API.getUsernameFromDiscordTag");
        return plugin.getPlayerDatabase().getUsernameByDiscordTag(discordTag);
    }
    public UUID getUUIDFromUsername(String username) throws SQLException {
        System.out.println("username = " + username);
        System.out.println("API.getUUIDFromUsername");
        return plugin.getPlayerDatabase().getUuidByUsername(username);
    }
}
