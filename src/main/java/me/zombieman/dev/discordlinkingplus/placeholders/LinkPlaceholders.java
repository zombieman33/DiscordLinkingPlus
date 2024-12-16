package me.zombieman.dev.discordlinkingplus.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import net.dv8tion.jda.api.entities.Member;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.UUID;

public class LinkPlaceholders extends PlaceholderExpansion {

    private final DiscordLinkingPlus plugin;
    public LinkPlaceholders(DiscordLinkingPlus plugin) {
        this.plugin = plugin;
    }


    @Override
    public @NotNull String getIdentifier() {
        return "discordlinking";
    }

    @Override
    public @NotNull String getAuthor() {
        return this.plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public @NotNull String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "n/a";
        }

        UUID uuid = player.getUniqueId();

        try {
            String discordTag = plugin.getPlayerDatabase().getPlayerData(uuid).getDiscordTag();
            switch (params.toLowerCase()) {
                case "linkicon":
                    if (plugin.getPlayerDatabase().getPlayerData(uuid).isLinked()) return plugin.getConfig().getString("link-icon", "🔗");

                    return "";

                case "islinked":
                    return isPlayerLinked(uuid) ? "1" : "0";
                case "discordid":
                    if (plugin.getPlayerDatabase().getPlayerData(uuid).isLinked()) return discordTag;

                    return "n/a";
                case "discordname":

                    if (plugin.getPlayerDatabase().getPlayerData(uuid).isLinked()) {
                        Member member = plugin.getGuild().retrieveMemberById(discordTag).complete();
                        return member.getUser().getName();
                    }

                    return "n/a";
                default:
                    return "Invalid format!";
            }
        } catch (SQLException e) {
            System.err.println("Error while connecting to database: " + e.getMessage());

        }
        return "n/a";
    }
    private boolean isPlayerLinked(UUID uuid) throws SQLException {
        return plugin.getPlayerDatabase().getPlayerData(uuid).isLinked();
    }
}
