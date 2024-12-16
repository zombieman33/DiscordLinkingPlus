package me.zombieman.dev.discordlinkingplus.manager;

import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.Instant;

public class LoggingManager {

    private static DiscordLinkingPlus plugin;

    public LoggingManager(DiscordLinkingPlus plugin) {
        LoggingManager.plugin = plugin;
    }


    public static void sendMessage(String message) {
        if (getLoggingChannel() == null) return;

        TextChannel loggingChannel = getLoggingChannel();

        loggingChannel.sendMessage(message).queue();
    }
    public static void sendEmbedMessage(String title, String playerName, String discordId, String uuid, @Nullable String code, @Nullable String message, Color color) {
        if (getLoggingChannel() == null) return;

        TextChannel loggingChannel = getLoggingChannel();

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(title)
                .setColor(color)
                .addField("Player Name", playerName, false)
                .addField("Discord ID", "`" + discordId + "`", false)
                .addField("Discord", "<@" + discordId + ">", false)
                .addField("UUID", "`" + uuid + "`", false)
                .setTimestamp(Instant.now());

        if (code != null) {
            embed.addField("Code: ", "`" + code + "`", false);
        }
        if (message != null) {
            embed.setDescription(message);
        }

        // Send the embed message
        loggingChannel.sendMessageEmbeds(embed.build()).queue();
    }

    public static TextChannel getLoggingChannel() {
        String discordLogsChannelID = plugin.getConfig().getString("DiscordLogsChannelID");

        if (discordLogsChannelID == null) {
            return null;
        }

        return plugin.getGuild().getTextChannelById(discordLogsChannelID);
    }
}
