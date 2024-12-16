package me.zombieman.dev.discordlinkingplus.listeners;


import io.papermc.paper.event.player.AsyncChatCommandDecorateEvent;
import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import me.zombieman.dev.discordlinkingplus.data.PlayerData;
import me.zombieman.dev.discordlinkingplus.database.mysql.DiscordLinkingData;
import me.zombieman.dev.discordlinkingplus.manager.LoggingManager;
import me.zombieman.dev.discordlinkingplus.manager.RankManager;
import me.zombieman.dev.discordlinkingplus.utils.ServerNameUtil;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.checkerframework.checker.units.qual.A;

import java.awt.*;
import java.nio.channels.Channel;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class PlayerJoinListener implements Listener {

    private final DiscordLinkingPlus plugin;

    public PlayerJoinListener(DiscordLinkingPlus plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        PlayerData.getPlayerDataConfig(plugin, player.getUniqueId()).set("uuid", player.getUniqueId().toString());
        PlayerData.getPlayerDataConfig(plugin, player.getUniqueId()).set("name", player.getName());
        PlayerData.savePlayerData(plugin, player.getUniqueId());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                DiscordLinkingData playerData = plugin.getPlayerDatabase().getPlayerData(player.getUniqueId(), player.getName());

                if (playerData != null && !playerData.getUsername().equals(player.getName()) && playerData.isLinked()) {
                    LoggingManager.sendEmbedMessage("Updated Name", player.getName(), playerData.getDiscordTag(), player.getUniqueId().toString(), null, "Name Update!\n\n> Old Name: **" + playerData.getUsername() + "**\n> New Name: **" + player.getName() + "**", Color.GRAY);
                    plugin.getPlayerDatabase().updateUsername(player.getUniqueId(), player.getName());
                }

                if (!plugin.getPlayerDatabase().getPlayerData(player.getUniqueId()).getUsername().equals(player.getName())) plugin.getPlayerDatabase().updateUsername(player.getUniqueId(), player.getName());

                if (playerData != null) {

                    if (playerData.isLinked()) plugin.getRankManager().assignRankAndNickname(player);

                    if (playerData.getDiscordTag() != null) {
                        plugin.getRankManager().removeRank(playerData.getDiscordTag(), playerData.isLinked());
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("Database not responding: " + e.getMessage());
                return;
            }
        });

        List<String> commands = plugin.getConfig().getStringList("LinkingRewards.commands");
        List<String> playerCommands = plugin.getConfig().getStringList("LinkingRewards.playerCommands");

        int commandsSize = commands.size();
        int playerCommandsSize = playerCommands.size();

        int rewards = commandsSize + playerCommandsSize;

        if (rewards == 0) {
            return;
        }

        try {

            DiscordLinkingData playerData = plugin.getPlayerDatabase().getPlayerData(player.getUniqueId(), player.getName());
            if (playerData.isLinked()) {

                List<String> servers = ServerNameUtil.fromString(playerData.getServerClaimedOn());

                String server = PlayerData.getPlayerDataConfig(plugin, player.getUniqueId()).getString("rewardsServer");
                String serverName = plugin.getConfig().getString("server.name");

                if (server == null) return;

                if (!server.equalsIgnoreCase(serverName) && !server.equalsIgnoreCase("all")) return;

                if (!servers.contains(serverName)) {
                    String title = ChatColor.GREEN.toString() + ChatColor.BOLD + "🌟 Reward Notification! 🌟";
                    String actionBarMessage = ChatColor.AQUA + "✨ You can claim " + ChatColor.YELLOW + rewards + ChatColor.AQUA + "! ✨";

                    player.sendTitle(title, "", 10, 70, 20);

                    if (!plugin.getConfig().getStringList("LinkingRewards.message").isEmpty()) {
                        for (String message : plugin.getConfig().getStringList("LinkingRewards.message")) {
                            message = message.replace("%reward-amount%", String.valueOf(rewards));
                            player.sendMessage(MiniMessage.miniMessage().deserialize(message)
                                    .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/claimrewards"))
                                    .hoverEvent(HoverEvent.showText(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("LinkingRewards.hover", "<green>Click here to claim rewards!")))));
                        }
                    }

                    player.sendActionBar(actionBarMessage);

                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                }

            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Database not responding: " + e.getMessage());
        }
    }
}
