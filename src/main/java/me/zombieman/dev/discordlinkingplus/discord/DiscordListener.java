package me.zombieman.dev.discordlinkingplus.discord;

import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import me.zombieman.dev.discordlinkingplus.manager.CodeManager;
import me.zombieman.dev.discordlinkingplus.manager.LoggingManager;
import me.zombieman.dev.discordlinkingplus.manager.RankManager;
import me.zombieman.dev.discordlinkingplus.manager.RewardsManager;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;

import java.awt.*;
import java.sql.SQLException;
import java.util.UUID;

public class DiscordListener extends ListenerAdapter {

    private final DiscordLinkingPlus plugin;

    public DiscordListener(DiscordLinkingPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String code = event.getMessage().getContentRaw();
        boolean needsToSendInDm = plugin.getConfig().getBoolean("NeedsToSendInDm");

        // Check if the message was sent in the correct channel
        if (event.getChannelType() == ChannelType.TEXT && event.getChannel().getId().equals(plugin.getConfig().getString("LinkingChannelID")) && !needsToSendInDm) {

            if (event.getMessage() != null) {
                try {
                    event.getMessage().delete().queue();
                } catch (Exception e) {
                    System.err.println("Unexpected error while deleting message: " + e.getMessage());
                }
            }

            handleCodeInPrivateMessage(event, code);

        } else if (event.getChannelType() == ChannelType.PRIVATE && needsToSendInDm) {
            handleCodeInPrivateMessage(event, code);
        }
    }

    private void handleCodeInPrivateMessage(MessageReceivedEvent event, String code) {


        try {
            if (plugin.getPlayerDatabase().isDiscordIdLinked(event.getAuthor().getId())) {
                sendMessage(event, "❌ Error linking account, this discord account is already linked.");
                return;
            }
        } catch (SQLException e) {
            sendMessage(event, "❌ Error linking account. Please try again later.");
            plugin.getLogger().severe("Failed to send message when account is trying to link: " + e.getMessage());
            return;
        }


        if (!isValidCode(code)) {
            sendMessage(event, "❌ Invalid code. Please try again.");
            return;
        }
        UUID uuid = CodeManager.getPlayerWithCode(code);
        if (uuid == null) {
            sendMessage(event, "❌ Invalid code. Please try again.");
            return;
        }

        try {

            if (plugin.getPlayerDatabase().getPlayerData(uuid).isLinked()) {
                sendMessage(event, "❌ Error linking account, this minecraft account is already linked. Please try again with a unlinked account.");
                return;
            }
        } catch (SQLException e) {
            sendMessage(event, "❌ Error linking account. Please try again later.");
            plugin.getLogger().severe("Failed to update link status: " + e.getMessage());
            return;
        }
        try {
            plugin.getPlayerDatabase().updateDiscordTag(uuid, event.getAuthor().getId());
        } catch (SQLException e) {
            sendMessage(event, "❌ Error updating Discord tag. Please try again later.");
            plugin.getLogger().severe("Failed to update Discord tag: " + e.getMessage());
            return;
        }

        String playerName = "n/a";

        if (Bukkit.getPlayer(uuid) != null) {
            playerName = Bukkit.getPlayer(uuid).getName();
            try {
                plugin.getRankManager().assignRankAndNickname(Bukkit.getPlayer(uuid));
            } catch (SQLException e) {

                sendMessage(event, "❌ Error updating rank. Please try again later.");

                plugin.getLogger().severe("Failed to update Discord rank: " + e.getMessage());
                return;
            }
        }

        if (playerName.equals("n/a")) {

            try {
                playerName = plugin.getPlayerDatabase().getPlayerData(uuid).getUsername();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error connecting to database: " + e.getMessage());
                return;
            }
        }

        try {
            plugin.getRankManager().removeRank(plugin.getPlayerDatabase().getPlayerData(uuid).getDiscordTag(), true);
            plugin.getRankManager().addRank(plugin.getPlayerDatabase().getPlayerData(uuid).getDiscordTag(), "linked");
        } catch (SQLException e) {

            sendMessage(event, "❌ Error removing and adding ranks. Please try again later.");
            plugin.getLogger().severe("Failed to remove/add Discord rank: " + e.getMessage());
            return;
        }

        CodeManager.removeCode(code);

        try {
            try (Jedis jedis = plugin.getJedisResource()) {

                jedis.publish("DISCORD_LINKING", "LINKED:" + uuid + ":" + event.getAuthor().getName());

                jedis.publish("DISCORD_LINKING",
                        "MESSAGE:" + event.getAuthor().getId() + ":✅_Successfully_**linked**_your_accounts!~~> **Minecraft:** `%minecraft%`~> **Discord:** %discord%~> **DiscordID:** `%discord-ID%`"
                        .replace("%minecraft%", playerName)
                        .replace("%discord%", event.getAuthor().getAsMention())
                        .replace("%discord-ID%", event.getAuthor().getId()));

                if (!plugin.getPlayerDatabase().getPlayerData(uuid).hasLinked()) {
                    System.out.println("Sending rewards");
                    jedis.publish("DISCORD_LINKING", "REWARDS:" + uuid + ":" + plugin.getConfig().getString("server.rewardsTo"));
                }
            }

            plugin.getPlayerDatabase().updateLinkStatus(uuid, true, true);
            plugin.getRankManager().commands(playerName, true);
        } catch (SQLException e) {
            sendMessage(event, "❌ Error connecting to the database. Please try again later.");
            plugin.getLogger().severe("Failed to connect to the database: " + e.getMessage());
            return;
        }

        LoggingManager.sendEmbedMessage("Successfully Linked", playerName, event.getAuthor().getId(), uuid.toString(), code, null, Color.GREEN);
    }


    public static String commandReplacements(String command, String name, UUID uuid) {
        command = command.replace("%player%", name);
        command = command.replace("%name%", name);
        command = command.replace("%player's name%", name);
        command = command.replace("%players name%", name);
        command = command.replace("%player's uuid%", uuid.toString());
        command = command.replace("%players uuid%", uuid.toString());
        command = command.replace("%uuid%", uuid.toString());
        return command;
    }
    private void sendMessage(MessageReceivedEvent event, String message) {
        if (!plugin.isMainServer()) return;

        event.getAuthor().openPrivateChannel().queue(privateChannel -> {
            privateChannel.sendMessage(message).queue();
        });
    }

    private boolean isValidCode(String code) {
        return CodeManager.checkCode(code);
    }
}
