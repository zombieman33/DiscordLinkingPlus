package me.zombieman.dev.discordlinkingplus.discord;

import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import me.zombieman.dev.discordlinkingplus.manager.LoggingManager;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class DiscordLeave extends ListenerAdapter {

    private final DiscordLinkingPlus plugin;
    public DiscordLeave(DiscordLinkingPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {

        if (!plugin.canUnLink()) return;

        if (!plugin.isMainServer()) return;

        if (event.getUser().isBot()) return;

        String discordId = event.getUser().getId();
        System.out.println("discordId: " + discordId);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                UUID uuid = plugin.getApi().getUUIDFromDiscordTag(discordId);

                if (!plugin.getApi().isLinked(uuid) || uuid == null) return;

                String name = plugin.getApi().getUsernameFromDiscordTag(discordId);

                try (Jedis jedis = plugin.getJedisResource()) {
                    jedis.publish("DISCORD_LINKING", "UNLINKED:" + uuid.toString() + ":PLAYER");

                    jedis.publish("DISCORD_LINKING",
                            "MESSAGE:" + discordId + ":✅_Successfully_**unlinked**_your_accounts!~~> **Minecraft:** `%minecraft%`~> **Discord:** %discord%~> **DiscordID:** `%discord-ID%`~> **Unlinked By:** `%unlinked-by%`"
                                    .replace("%minecraft%", name)
                                    .replace("%discord%", "<@" + discordId + ">")
                                    .replace("%discord-ID%", discordId)
                                    .replace("%unlinked-by%", name));
                }

                plugin.getPlayerDatabase().updateLinkStatus(uuid, false, true);
                plugin.getRankManager().commands(name, false);

                System.out.println("DiscordLeave, unlinked: " + discordId);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
