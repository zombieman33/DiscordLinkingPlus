package me.zombieman.dev.discordlinkingplus.commands;

import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import me.zombieman.dev.discordlinkingplus.api.API;
import me.zombieman.dev.discordlinkingplus.database.mysql.data.DiscordLinkingData;
import me.zombieman.dev.discordlinkingplus.database.mysql.data.ServerData;
import me.zombieman.dev.discordlinkingplus.manager.LoggingManager;
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

                            plugin.getRankManager().removeRank(discordTag, false);
                            plugin.getRankManager().commands(discordTag, false);
                            plugin.getRankManager().addRank(discordTag, "unLinked");

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

                            return;
                        case "get":

                            if (uuid == null) {
                                sender.sendMessage("This isn't a valid user!");
                                return;
                            }
                            playerData = plugin.getPlayerDatabase().getPlayerData(uuid);

                            sender.sendMessage(" ");
                            sender.sendMessage(ChatColor.AQUA + "Player Name: " + ChatColor.BOLD + target);
                            sender.sendMessage(ChatColor.AQUA + "UUID: " + ChatColor.BOLD + uuid);
                            sender.sendMessage(ChatColor.AQUA + "Is Linked: " + ChatColor.BOLD + playerData.isLinked());
                            sender.sendMessage(ChatColor.AQUA + "Has Linked: " + ChatColor.BOLD + playerData.hasLinked());
                            sender.sendMessage(ChatColor.AQUA + "Claimed on: " + ChatColor.BOLD + playerData.getServerClaimedOn());
                            if (playerData.isLinked()) {
                                sender.sendMessage(ChatColor.AQUA + "Discord ID: " + ChatColor.BOLD + playerData.getDiscordTag());
                            }
                            sender.sendMessage(" ");
                            return;
                        case "reset":

                            String server = args[1];

                            Collection<ServerData> serverData = plugin.getServerListCache().getServers();
                            List<String> servers = serverData.stream().map(ServerData::getServerID).collect(Collectors.toList());

                            if (!servers.contains(server)) {
                                sender.sendMessage(ChatColor.RED + "This server doesn't exist!");
                                return;
                            }

                            plugin.getPlayerDatabase().resetClaimedServer(server);

                            sender.sendMessage(ChatColor.GREEN + "Successfully reset all claimed rewards on the '" + server + "' server.");
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

            try {
                API api = plugin.getApi();
                if (api == null) {
                    sender.sendMessage("API NULL");
                    return false;
                }

                String discordTag = plugin.getPlayerDatabase().getPlayerData(UUID.fromString("9c6e22dc-fc6a-46c5-84f5-1a7f62bd46bd")).getDiscordTag();
                api.sendMessageToDiscordUser(discordTag,
                        ":trophy: **Karma Leaderboard** :trophy: <@" + discordTag + ">[NEW-LINE][NEW-LINE]> **1st**: `zombyman`[NEW-LINE]> **2nd**: `Tech`[NEW-LINE]> **3rd**: `Harry`[NEW-LINE][NEW-LINE]> **10th**: `Alfie`[NEW-LINE][NEW-LINE][NEW-LINE]For coming top **#1** you will receive:[NEW-LINE][NEW-LINE]* **Suffix [Karma Master]** :white_check_mark: [NEW-LINE]* **400 Gems** :white_check_mark: [NEW-LINE]* **15% Store Credit** :white_check_mark:[NEW-LINE] (Please open a [ticket here](https://discord.com/channels/927172634830045264/1327717498912505866/1327720840967749796) to claim your store credit)[NEW-LINE][NEW-LINE][NEW-LINE]**Note:**[NEW-LINE]This is still in beta. If you haven't received your rewards, please open a ticket, and we'll get it sorted for you!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }


        return true;
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
            completions.add("reset");
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

            if (args[0].equalsIgnoreCase("reset")) {
                Collection<ServerData> servers = plugin.getServerListCache().getServers();
                completions.addAll(servers.stream().map(ServerData::getServerID).collect(Collectors.toList()));
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream().filter(s -> s.toLowerCase().startsWith(lastArg.toLowerCase())).collect(Collectors.toList());
    }
}