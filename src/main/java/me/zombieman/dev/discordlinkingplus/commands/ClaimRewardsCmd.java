package me.zombieman.dev.discordlinkingplus.commands;

import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import me.zombieman.dev.discordlinkingplus.data.PlayerData;
import me.zombieman.dev.discordlinkingplus.database.mysql.DiscordLinkingData;
import me.zombieman.dev.discordlinkingplus.manager.LoggingManager;
import me.zombieman.dev.discordlinkingplus.manager.RewardsManager;
import me.zombieman.dev.discordlinkingplus.utils.ServerNameUtil;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClaimRewardsCmd implements CommandExecutor {
    private final DiscordLinkingPlus plugin;

    public ClaimRewardsCmd(DiscordLinkingPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("discordlinkingplus.command.claimrewards")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to run this command!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        List<String> commands = plugin.getConfig().getStringList("LinkingRewards.commands");
        List<String> playerCommands = plugin.getConfig().getStringList("LinkingRewards.playerCommands");

        int commandsSize = commands.size();
        int playerCommandsSize = playerCommands.size();

        int rewards = commandsSize + playerCommandsSize;

        try {

            DiscordLinkingData playerData = plugin.getPlayerDatabase().getPlayerData(player.getUniqueId(), player.getName());

            if (!playerData.isLinked()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You have not linked your accounts yet!")
                        .hoverEvent(HoverEvent.showText(MiniMessage.miniMessage().deserialize("<red>Click here to link your accounts!")))
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/discordlinkingplus:link")));

                player.sendActionBar(ChatColor.RED + "You need to link your accounts before running this command!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return false;
            }

            List<String> servers = new ArrayList<>(ServerNameUtil.fromString(playerData.getServerClaimedOn()));

            String serverName = plugin.getConfig().getString("server.name");

            if (servers.contains(serverName)) {
                player.sendMessage(ChatColor.RED + "You have already claimed your rewards on this server!");
                player.sendActionBar(ChatColor.RED + "You have already claimed your rewards on this server!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return false;
            }

            String server = PlayerData.getPlayerDataConfig(plugin, player.getUniqueId()).getString("rewardsServer");

            if (server == null || !server.equalsIgnoreCase(serverName) && !server.equalsIgnoreCase("all")) {
                player.sendMessage(ChatColor.RED + "You don't have any rewards to claim on this server!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return false;
            }

            if (rewards == 0) {
                player.sendMessage(ChatColor.RED + "There aren't any rewards setup yet!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return false;
            }

            servers.add(serverName);

            plugin.getPlayerDatabase().updateServers(player.getUniqueId(), ServerNameUtil.toString(servers));

            for (String c : commands) {
                c = RewardsManager.commandReplacements(c, player);
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), c);
            }

            for (String c : playerCommands) {
                c = RewardsManager.commandReplacements(c, player);
                player.performCommand(c);
            }

            String title = ChatColor.GREEN.toString() + ChatColor.BOLD + "🌟 Reward Notification! 🌟";
            String message = ChatColor.GREEN + "🎉 Congratulations! You have claimed " + ChatColor.YELLOW + rewards + ChatColor.GREEN + " rewards! 🎊";
            String actionBarMessage = ChatColor.AQUA + "✨ You claimed " + ChatColor.YELLOW + rewards + ChatColor.AQUA + " rewards! ✨";

            player.sendTitle(title, "", 10, 70, 20);

            player.sendMessage(message);

            player.sendActionBar(actionBarMessage);

            plugin.getRankManager().assignRankAndNickname(player);

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);

            LoggingManager.sendEmbedMessage("Claimed Rewards", player.getName(), playerData.getDiscordTag(), player.getUniqueId().toString(), null, "Claimed Rewards!\n\n> Server: **" + serverName + "**\n> Has Claimed On: **" + servers + "**", Color.ORANGE);

        } catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "There was an error while connecting to the database, please try again later.");
            plugin.getLogger().severe("Database not responding: " + e.getMessage());
        }

        return true;
    }
}