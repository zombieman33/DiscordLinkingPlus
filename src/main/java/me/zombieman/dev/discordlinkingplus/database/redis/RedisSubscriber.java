package me.zombieman.dev.discordlinkingplus.database.redis;

import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import me.zombieman.dev.discordlinkingplus.discord.DiscordBot;
import me.zombieman.dev.discordlinkingplus.manager.RewardsManager;
import net.dv8tion.jda.api.entities.Member;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import redis.clients.jedis.JedisPubSub;

import java.sql.SQLException;
import java.util.UUID;

public class RedisSubscriber extends JedisPubSub {
    private final DiscordLinkingPlus plugin;

    public RedisSubscriber(DiscordLinkingPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessage(String channel, String message) {
        if (!plugin.isEnabled()) {
            return;
        }

        String[] parts = message.split(":", 3);
        String action = parts[0];

//        System.out.println(message);
//        System.out.println(parts);
//        System.out.println(action);

        switch (action.toUpperCase()) {
            case "LINKED":
                if (parts.length >= 3) {
                    UUID linkedUUID = UUID.fromString(parts[1]);
                    String discordName = parts[2];
                    handleLinked(linkedUUID, discordName);
                }
                break;

            case "UNLINKED":
                if (parts.length >= 3) {
                    UUID unlinkedUUID = UUID.fromString(parts[1]);
                    String unlinkedBy = parts[2];
                    try {
                        handleUnlinked(unlinkedUUID, unlinkedBy);
                    } catch (SQLException e) {
                        plugin.getLogger().severe("Database not responding: " + e.getMessage());
                        return;
                    }
                }
                break;

            case "REWARDS":
                if (parts.length >= 3) {
                    UUID userUUID = UUID.fromString(parts[1]);
                    String serverName = parts[2];
                    RewardsManager.handleReward(plugin, userUUID, serverName);
                }
                break;
            case "MESSAGE":
                if (parts.length >= 3) {
                    String discordID = parts[1];
                    String msg = parts[2];
                    handleMessage(discordID, msg);
                }
                break;
            default:
                plugin.getLogger().warning("Received unrecognized message: " + message);
                break;
        }
    }

    private void handleLinked(UUID linkedUUID, String discordName) {
        if (Bukkit.getPlayer(linkedUUID) == null) return;

        Player player = Bukkit.getPlayer(linkedUUID);

        player.sendMessage(ChatColor.GREEN.toString() + ChatColor.STRIKETHROUGH + "                              ");
        player.sendMessage(ChatColor.GREEN + "Successfully linked!");
        player.sendMessage(ChatColor.AQUA + "Minecraft: " + player.getName());
        player.sendMessage(ChatColor.AQUA + "Discord: " + discordName);
        player.sendMessage(ChatColor.GREEN.toString() + ChatColor.STRIKETHROUGH + "                              ");

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
    }
    private void handleMessage(String discordID, String message) {

        if (!plugin.getConfig().getBoolean("MainServer", false)) return;

        message = message.replace("_", " ");
        message = message.replace("~", "\n");

        Member member = plugin.getGuild().retrieveMemberById(discordID).complete();

        String finalMessage = message;
//        System.out.println(finalMessage);
        member.getUser().openPrivateChannel().queue(privateChannel -> {
            privateChannel.sendMessage(finalMessage).queue();
        });
    }

    private void handleUnlinked(UUID unlinkedUUID, String unlinkedBy) throws SQLException {

        if (Bukkit.getPlayer(unlinkedUUID) == null) return;

        Player player = Bukkit.getPlayer(unlinkedUUID);

        String message;

        if (unlinkedBy.equalsIgnoreCase("player")) {
            message = "Successfully unlinked your accounts";
        } else {
            message = "Your accounts have been\nunlinked by an admin!";
        }

        player.sendMessage(ChatColor.GREEN.toString() + ChatColor.STRIKETHROUGH + "                                        ");
        player.sendMessage(ChatColor.GREEN + message);
        player.sendMessage(ChatColor.GREEN.toString() + ChatColor.STRIKETHROUGH + "                                        ");

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        plugin.getLogger().info("Subscribed to Redis channel: " + channel);
    }

    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {
        plugin.getLogger().info("Unsubscribed from Redis channel: " + channel);
    }

    @Override
    public void onPSubscribe(String pattern, int subscribedChannels) {
        plugin.getLogger().info("Pattern subscribed: " + pattern);
    }

    @Override
    public void onPUnsubscribe(String pattern, int subscribedChannels) {
        plugin.getLogger().info("Pattern unsubscribed: " + pattern);
    }
}
