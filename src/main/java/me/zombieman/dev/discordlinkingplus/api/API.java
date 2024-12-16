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
}
