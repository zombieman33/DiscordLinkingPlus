package me.zombieman.dev.discordlinkingplus.commands;

import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import me.zombieman.dev.discordlinkingplus.api.API;
import me.zombieman.dev.discordlinkingplus.database.mysql.data.DiscordLinkingData;
import me.zombieman.dev.discordlinkingplus.database.mysql.data.ServerData;
import me.zombieman.dev.discordlinkingplus.manager.LoggingManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;

import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AdminCmd implements CommandExecutor, TabCompleter {
    private final DiscordLinkingPlus plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public AdminCmd(DiscordLinkingPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof Player player && !player.hasPermission("discordlinkingplus.command.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to run this command");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        if (args.length >= 2) {

            String key = args[0];
            String target = args[1];

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {

                    UUID uuid = plugin.getPlayerDatabase().getUuidByUsername(target);

                    switch (key) {
                        case "unlink":

                            if (uuid == null) {
                                sender.sendMessage("This isn't a valid user!");
                                return;
                            }
                            DiscordLinkingData playerData = plugin.getPlayerDatabase().getPlayerData(uuid);

                            if (!playerData.isLinked()) {
                                sender.sendMessage(ChatColor.RED + "This user hasn't linked their accounts.");
                                return;
                            }

                            String discordTag = playerData.getDiscordTag();
                            String playerName = playerData.getUsername();

                            LoggingManager.sendEmbedMessage("Successfully Unlinked", playerName, discordTag, uuid.toString(), null, "Unlinked with `/discordadmin unlink " + target + "`\n\n> Unlinked By: **" + sender.getName() + "**\n> Force Unlink: **true**", Color.BLACK);

                            sender.sendMessage("Successfully unlinked " + target + "'s account");

                            try (Jedis jedis = plugin.getJedisResource()) {
                                jedis.publish("DISCORD_LINKING", "UNLINKED:" + uuid + ":ADMIN");
                                jedis.publish("DISCORD_LINKING",
                                        "MESSAGE:" + discordTag + ":✅_Successfully_**unlinked**_your_accounts!~~> **Minecraft:** `%minecraft%`~> **Discord:** %discord%~> **DiscordID:** `%discord-ID%`~> **Unlinked By:** `%unlinked-by%`"
                                                .replace("%minecraft%", target)
                                                .replace("%discord%", "<@" + discordTag + ">")
                                                .replace("%discord-ID%", discordTag)
                                                .replace("%unlinked-by%", "An Admin"));
                            }

                            plugin.getPlayerDatabase().updateLinkStatus(uuid, false, true);


                            plugin.getRankManager().removeRank(discordTag, false);
                            plugin.getRankManager().commands(discordTag, false);
                            plugin.getRankManager().addRank(discordTag, "unLinked");
                            return;
                        case "get":

                            if (uuid == null) {
                                sender.sendMessage("This isn't a valid user!");
                                return;
                            }
                            playerData = plugin.getPlayerDatabase().getPlayerData(uuid);

                            MiniMessage mm = MiniMessage.miniMessage();

                            sender.sendMessage(mm.deserialize("<gradient:#00BFFF:#1E90FF><bold>╔═══════ Discord Link Information ═══════╗</bold></gradient>"));

                            sendDiscordLinkMessage(sender, playerData, "<gradient:#00BFFF:#1E90FF>");

                            sender.sendMessage(mm.deserialize("<gradient:#00BFFF:#1E90FF><bold>╚══════════════════════════════╝</bold></gradient>"));

                            return;
                        case "reset", "resetserverrewardssotechdoesnotdoubthimself", "resetrewards":
                            resetRewards(sender, target);
                            return;
                        default:
                            sender.sendMessage("/discordadmin <unlink, get, reset> <player, server>");
                    }
                } catch (SQLException e) {
                    sender.sendMessage(ChatColor.RED + "There was an error while connecting to the database, please try again later.");
                    plugin.getLogger().severe("Database not responding: " + e.getMessage());
                }
            });

        } else if (args[0].equalsIgnoreCase("statistics")) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                sender.sendMessage(ChatColor.GOLD + ChatColor.BOLD.toString() + "----- Statistics -----");
                sender.sendMessage(ChatColor.GRAY + "• " + ChatColor.YELLOW + "Average Link Time: " + ChatColor.GREEN + plugin.getLinkStatisticsDatabase().getFormattedAverageLinkTime());
                sender.sendMessage(ChatColor.GRAY + "• " + ChatColor.YELLOW + "Total Linked Users: " + ChatColor.GREEN + plugin.getStatisticsManager().getTotalLinkedUsers());
                sender.sendMessage(ChatColor.GOLD + ChatColor.BOLD.toString() + "--------------------");
            });
        }


        return true;
    }

    private void resetRewards(CommandSender sender, String server) throws SQLException {
        Collection<ServerData> serverData = plugin.getServerListCache().getServers();
        List<String> servers = serverData.stream().map(ServerData::getServerID).toList();

        if (!servers.contains(server)) {
            sender.sendMessage(ChatColor.RED + "This server doesn't exist!");
            sender.sendMessage(ChatColor.RED + "Please make sure you're typing the server name correctly!");
            return;
        }

        plugin.getPlayerDatabase().resetClaimedServer(server);

        sender.sendMessage(ChatColor.GREEN + "Successfully reset all claimed rewards on the '" + server + "' server.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        List<String> completions = new ArrayList<>();
        if (!(sender instanceof Player)) {
            Player player = (Player) sender;
            if (!player.hasPermission("discordlinkingplus.command.admin")) return completions;
        }


        if (args.length == 1) {
            completions.add("unlink");
            completions.add("get");
            completions.add("resetrewards");
            completions.add("statistics");
        }

        if (args.length == 2) {

            if (args[0].equalsIgnoreCase("unlink") || args[0].equalsIgnoreCase("get")) {
                try {
                    completions.addAll(plugin.getPlayerDatabase().getAllUsernames());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            if (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("resetserverrewardssotechdoesnotdoubthimself") || args[0].equalsIgnoreCase("resetrewards")) {
                Collection<ServerData> servers = plugin.getServerListCache().getServers();
                completions.addAll(servers.stream().map(ServerData::getServerID).collect(Collectors.toList()));
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream().filter(s -> s.toLowerCase().startsWith(lastArg.toLowerCase())).collect(Collectors.toList());
    }


    private void sendDiscordLinkMessage(CommandSender sender, DiscordLinkingData playerData, String color) {
        MiniMessage mm = MiniMessage.miniMessage();

        // Create interactive components
        Component name = mm.deserialize(color + "<bold>▪ Player Name:</bold> <white>" + playerData.getUsername() + "</white>")
                .clickEvent(ClickEvent.copyToClipboard(playerData.getUsername()))
                .hoverEvent(HoverEvent.showText(mm.deserialize("<italic><gray>Click to copy name</gray></italic>")));

        Component uuid = mm.deserialize(color + "<bold>▪ UUID:</bold> <white>" + playerData.getUuid() + "</white>")
                .clickEvent(ClickEvent.copyToClipboard(playerData.getUuid()))
                .hoverEvent(HoverEvent.showText(mm.deserialize("<italic><gray>Click to copy UUID</gray></italic>")));

        String tag = "n/a";

        if (playerData.getDiscordTag() != null) {
            tag = playerData.getDiscordTag();
        }

        Component discordTag = mm.deserialize(color + "<bold>▪ Discord Tag:</bold> <white>" + tag  + "</white>")
                .clickEvent(ClickEvent.copyToClipboard(tag))
                .hoverEvent(HoverEvent.showText(mm.deserialize("<italic><gray>Click to copy Discord Tag</gray></italic>")));

        Component isLinked = mm.deserialize(color + "<bold>▪ Linked with Discord:</bold> <white>" + playerData.isLinked() + "</white>");
        Component hasLinked = mm.deserialize(color + "<bold>▪ Has Linked Before:</bold> <white>" + playerData.hasLinked() + "</white>");

        // Send the formatted messages to the sender
        sender.sendMessage(name);
        sender.sendMessage(uuid);
        sender.sendMessage(discordTag);
        sender.sendMessage(isLinked);
        sender.sendMessage(hasLinked);
    }

}