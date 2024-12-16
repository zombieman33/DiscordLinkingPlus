package me.zombieman.dev.discordlinkingplus.commands;

import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import me.zombieman.dev.discordlinkingplus.database.mysql.DiscordLinkingData;
import me.zombieman.dev.discordlinkingplus.discord.DiscordBot;
import me.zombieman.dev.discordlinkingplus.manager.CodeManager;
import me.zombieman.dev.discordlinkingplus.manager.LoggingManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
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

                    if (uuid == null) {
                        sender.sendMessage("This isn't a valid user!");
                        return;
                    }

                    DiscordLinkingData playerData = plugin.getPlayerDatabase().getPlayerData(uuid);

                    switch (key) {
                        case "unlink":

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

                            sender.sendMessage(" ");
                            sender.sendMessage(ChatColor.AQUA + "Player Name: " + ChatColor.BOLD + target);
                            sender.sendMessage(ChatColor.AQUA + "UUID: " + ChatColor.BOLD + uuid);
                            sender.sendMessage(ChatColor.AQUA + "Is Linked: " + ChatColor.BOLD + playerData.isLinked());
                            if (playerData.isLinked()) {
                                sender.sendMessage(ChatColor.AQUA + "Discord ID: " + ChatColor.BOLD + playerData.getDiscordTag());
                            }
                            sender.sendMessage(" ");
                            return;

                        default:
                            sender.sendMessage("/discordadmin <unlink, get> <player>");
                    }
                } catch (SQLException e) {
                    sender.sendMessage(ChatColor.RED + "There was an error while connecting to the database, please try again later.");
                    plugin.getLogger().severe("Database not responding: " + e.getMessage());
                }
            });

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
        }

        if (args.length == 2) {

            try {
                completions.addAll(plugin.getPlayerDatabase().getAllUsernames());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream().filter(s -> s.toLowerCase().startsWith(lastArg.toLowerCase())).collect(Collectors.toList());
    }
}