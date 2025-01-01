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
        System.out.println("Getting discord tag from uuid:");
        System.out.println("UUID: " + uuid);
        return plugin.getPlayerDatabase().getPlayerData(uuid).getDiscordTag();
    }
    public boolean isLinked(UUID uuid) throws SQLException {
        System.out.println("Checking if uuid is linked:");
        System.out.println("UUID: " + uuid);
        return plugin.getPlayerDatabase().getPlayerData(uuid).isLinked();
    }
    public UUID getUUIDFromDiscordTag(String discordTag) throws SQLException {
        System.out.println("Getting uuid from discord ID:");
        System.out.println("ID: " + discordTag);
        return plugin.getPlayerDatabase().getUuidByDiscordTag(discordTag);
    }
    public String getUsernameFromDiscordTag(String discordTag) throws SQLException {
        System.out.println("Getting username from discord ID:");
        System.out.println("ID: " + discordTag);
        return plugin.getPlayerDatabase().getUsernameByDiscordTag(discordTag);
    }
    public UUID getUUIDFromUsername(String username) throws SQLException {
        System.out.println("Getting UUID from username:");
        System.out.println("Username: " + username);
        return plugin.getPlayerDatabase().getUuidByUsername(username);
    }
}
