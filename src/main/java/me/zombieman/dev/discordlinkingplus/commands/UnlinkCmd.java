package me.zombieman.dev.discordlinkingplus.commands;

import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import me.zombieman.dev.discordlinkingplus.discord.DiscordBot;
import me.zombieman.dev.discordlinkingplus.manager.CodeManager;
import me.zombieman.dev.discordlinkingplus.manager.LoggingManager;
import me.zombieman.dev.discordlinkingplus.manager.RewardsManager;
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
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;

import java.awt.*;
import java.sql.SQLException;

public class UnlinkCmd implements CommandExecutor {
    private final DiscordLinkingPlus plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public UnlinkCmd(DiscordLinkingPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("discordlinkingplus.command.unlink")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to run this command!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        if (!plugin.canUnLink()) {
            player.sendMessage(ChatColor.RED.toString() + ChatColor.STRIKETHROUGH + "                              ");
            player.sendMessage(ChatColor.RED + "The unlinking feature");
            player.sendMessage(ChatColor.RED + "is disabled!");
            player.sendMessage(ChatColor.RED.toString() + ChatColor.STRIKETHROUGH + "                              ");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {

                if (!plugin.getPlayerDatabase().getPlayerData(player.getUniqueId()).isLinked()) {
                    player.sendMessage(ChatColor.GREEN.toString() + ChatColor.STRIKETHROUGH + "                                            ");
                    player.sendMessage(ChatColor.GREEN + "You haven't linked your accounts yet!");
                    player.sendMessage(miniMessage.deserialize("<aqua>/link to link your account! (click)")
                            .hoverEvent(HoverEvent.showText(miniMessage.deserialize("<aqua>Click to link!")))
                            .clickEvent(ClickEvent.runCommand("/link")));
                    player.sendMessage(ChatColor.GREEN.toString() + ChatColor.STRIKETHROUGH + "                                            ");
                    return;
                }
                String discordTag;

                try {
                    discordTag = plugin.getPlayerDatabase().getPlayerData(player.getUniqueId()).getDiscordTag();
                } catch (SQLException e) {
                    player.sendMessage(ChatColor.RED + "There was an error while connecting to the database, please try again later!");
                    plugin.getLogger().severe("Failed to remove Discord rank: " + e.getMessage());
                    return;
                }

                plugin.getRankManager().removeRank(discordTag, false);
                plugin.getRankManager().addRank(discordTag, "unLinked");

//                Member member = plugin.getGuild().retrieveMemberById(discordTag).complete();
//
//                if (member != null) {
//                    member.getUser().openPrivateChannel().queue(privateChannel -> {
//                        privateChannel.sendMessage("""
//                                ✅ Successfully **unlinked** your accounts!
//
//                                > **Minecraft:** `%minecraft%`
//                                > **Discord:** %discord%
//                                > **DiscordID:** `%discord-ID%`
//                                """.replace("%minecraft%", player.getName())
//                                .replace("%discord%", member.getAsMention())
//                                .replace("%discord-ID%", member.getUser().getId())).queue();
//                    });
//                }

                LoggingManager.sendEmbedMessage("Successfully Unlinked", player.getName(), discordTag, player.getUniqueId().toString(), null, "Unlinked with /unlink!", Color.RED);

                try (Jedis jedis = plugin.getJedisResource()) {
                    jedis.publish("DISCORD_LINKING", "UNLINKED:" + player.getUniqueId() + ":PLAYER");

                    jedis.publish("DISCORD_LINKING",
                            "MESSAGE:" + discordTag + ":✅_Successfully_**unlinked**_your_accounts!~~> **Minecraft:** `%minecraft%`~> **Discord:** %discord%~> **DiscordID:** `%discord-ID%`~> **Unlinked By:** `%unlinked-by%`"
                                    .replace("%minecraft%", player.getName())
                                    .replace("%discord%", "<@" + discordTag + ">")
                                    .replace("%discord-ID%", discordTag)
                                    .replace("%unlinked-by%", player.getName()));
                }

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);

                plugin.getPlayerDatabase().updateLinkStatus(player.getUniqueId(), false, true);

                plugin.getRankManager().commands(player.getName(), false);

            } catch (SQLException e) {
                player.sendMessage(ChatColor.RED + "There was an error while connecting to the database, please try again later.");
                plugin.getLogger().severe("Database not responding: " + e.getMessage());
                return;
            }
        });
        return true;
    }
}