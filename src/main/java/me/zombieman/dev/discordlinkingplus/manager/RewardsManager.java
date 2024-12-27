package me.zombieman.dev.discordlinkingplus.manager;

import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import me.zombieman.dev.discordlinkingplus.data.PlayerData;
import me.zombieman.dev.discordlinkingplus.utils.ServerNameUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RewardsManager {

    public static void handleReward(DiscordLinkingPlus plugin, UUID uuid, String server) {
        if (!server.equalsIgnoreCase(plugin.getConfig().getString("server.name")) && !server.equalsIgnoreCase("all")) return;

        if (Bukkit.getPlayer(uuid) == null) {
            PlayerData.createFile(plugin, uuid);
            PlayerData.getPlayerDataConfig(plugin, uuid).set("rewardsServer", server);
            PlayerData.savePlayerData(plugin, uuid);
            return;
        }

        List<String> commands = plugin.getConfig().getStringList("LinkingRewards.commands");
        List<String> playerCommands = plugin.getConfig().getStringList("LinkingRewards.playerCommands");

        int commandsSize = commands.size();
        int playerCommandsSize = playerCommands.size();

        int rewards = commandsSize + playerCommandsSize;

        if (rewards == 0) {
            return;
        }

        Player player = Bukkit.getPlayer(uuid);

        try {
            List<String> serversClaimed = new ArrayList<>();

            serversClaimed.addAll(ServerNameUtil.fromString(plugin.getPlayerDatabase().getPlayerData(uuid, player.getName()).getServerClaimedOn()));

            if (serversClaimed.contains(server)) return;

            serversClaimed.add(plugin.getConfig().getString("server.name", server));

            plugin.getPlayerDatabase().updateServers(uuid, ServerNameUtil.toString(serversClaimed));

        } catch (SQLException e) {
            plugin.getLogger().severe("Database not responding: " + e.getMessage());
            player.sendMessage(ChatColor.RED + "There was an error connecting to the database, please try again later.");
            player.sendMessage(ChatColor.RED + "Please run /claimrewards if you keep getting this error.");
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (String command : commands) {
                command = commandReplacements(command, player);
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
            }

            for (String command : playerCommands) {
                command = commandReplacements(command, player);

                player.performCommand(command);
            }
        });

        try {
            plugin.getRankManager().assignRankAndNickname(player);
        } catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "There was an error assigning your ranks and giving you a nickname. You can rejoin to try again.");
            plugin.getLogger().severe("Database not responding: " + e.getMessage());
        }

        player.sendMessage(MiniMessage.miniMessage().deserialize("""
                <aqua><bold>LINKING<reset>
                <green><bold>Successfully your linked accounts!</bold>
                
                You have been given %rewards%x rewards!
                """.replace("%rewards%", String.valueOf(rewards))));

    }

    @NotNull
    public static String commandReplacements(String command, Player player) {
        command = command.replace("%player%", player.getName());
        command = command.replace("%name%", player.getName());
        command = command.replace("%player's name%", player.getName());
        command = command.replace("%players name%", player.getName());
        command = command.replace("%player's uuid%", player.getUniqueId().toString());
        command = command.replace("%players uuid%", player.getUniqueId().toString());
        command = command.replace("%uuid%", player.getUniqueId().toString());
        return command;
    }

}
