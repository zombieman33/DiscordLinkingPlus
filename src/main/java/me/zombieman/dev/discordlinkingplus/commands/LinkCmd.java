package me.zombieman.dev.discordlinkingplus.commands;

import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import me.zombieman.dev.discordlinkingplus.discord.DiscordBot;
import me.zombieman.dev.discordlinkingplus.manager.CodeManager;
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

import java.sql.SQLException;
import java.util.Arrays;

public class LinkCmd implements CommandExecutor {
    private final DiscordLinkingPlus plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public LinkCmd(DiscordLinkingPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("discordlinkingplus.command.link")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to run this command!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {

                if (plugin.getPlayerDatabase().getPlayerData(player.getUniqueId()).isLinked()) {
                    player.sendMessage(ChatColor.AQUA.toString() + ChatColor.STRIKETHROUGH + "                                                   ");
                    player.sendMessage(ChatColor.GREEN + "You have already linked your accounts!");

                    String discordName = "n/a";

                    if (plugin.getGuild().retrieveMemberById(plugin.getPlayerDatabase().getPlayerData(player.getUniqueId()).getDiscordTag()).complete() != null) {
                        Member member = plugin.getGuild().retrieveMemberById(plugin.getPlayerDatabase().getPlayerData(player.getUniqueId()).getDiscordTag()).complete();
                        discordName = member.getUser().getEffectiveName();
                    }

                    player.sendMessage(ChatColor.AQUA + "Discord: " + discordName);
                    if (plugin.canUnLink()) {
                        player.sendMessage(miniMessage.deserialize("<aqua>/unlink to unlink your account!")
                                .hoverEvent(HoverEvent.showText(miniMessage.deserialize("<aqua>Click to unlink!")))
                                .clickEvent(ClickEvent.runCommand("/unlink")));
                    }
                    player.sendMessage(ChatColor.AQUA.toString() + ChatColor.STRIKETHROUGH + "                                                   ");
                    return;
                }

            } catch (SQLException e) {
                player.sendMessage(ChatColor.RED + "There was an error while connecting to the database, please try again later.");
                plugin.getLogger().severe("Database not responding: " + e.getMessage());
                return;
            }

            String code = null;
            try {
                code = CodeManager.createCode(player.getUniqueId(), plugin.getConfig().getInt("CodeTime", 10));
            } catch (SQLException e) {
                player.sendMessage(ChatColor.RED + "There was an error while connecting to the database, please try again later.");
                plugin.getLogger().severe("Database not responding: " + e.getMessage());
                return;
            }

            boolean needsToSendInDm = plugin.getConfig().getBoolean("NeedsToSendInDm");

            if (!needsToSendInDm) {
                String linkingChannelId = plugin.getConfig().getString("LinkingChannelID");
                String discordServerToken = plugin.guildID();

                if (discordServerToken == null) {
                    player.sendMessage(ChatColor.RED + "ERROR: The discord server token is null.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                if (linkingChannelId == null) {
                    player.sendMessage(ChatColor.RED + "ERROR: The linking channel id is null.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }

                if (plugin.getGuild() == null) {
                    player.sendMessage(ChatColor.RED + "ERROR: Invalid server token.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }

                Guild guildById = plugin.getGuild();

                if (guildById.getTextChannelById(linkingChannelId) == null) {
                    player.sendMessage(ChatColor.RED + "ERROR: Invalid channel id.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }

                TextChannel textChannelById = guildById.getTextChannelById(linkingChannelId);

                int min = plugin.getConfig().getInt("CodeTime", 10);

                Component message = miniMessage.deserialize("<green>Click to copy your code: <bold>" + code)
                        .hoverEvent(HoverEvent.showText(miniMessage.deserialize("<green>Click to copy")))
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, code));

                player.sendMessage(ChatColor.AQUA.toString() + ChatColor.STRIKETHROUGH + "                                               ");

                player.sendMessage(ChatColor.AQUA.toString() + ChatColor.BOLD + "LINKING");
                player.sendMessage(message);
                player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Your code will be deleted in " + min + " min(s)"));

                player.sendActionBar(ChatColor.GREEN + "Your code: " + ChatColor.BOLD + code);
                player.sendTitle(ChatColor.GREEN + "Your code: " + ChatColor.BOLD + code, "");

                player.sendMessage(ChatColor.GRAY + "Please send the code in");
                player.sendMessage(ChatColor.GRAY + "the following Discord channel:");

                player.sendMessage(MiniMessage.miniMessage().deserialize("<aqua>Channel: <bold>" + textChannelById.getName())
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://discord.com/channels/" + plugin.getGuild().getId() + "/" + textChannelById.getId()))
                        .hoverEvent(HoverEvent.showText(MiniMessage.miniMessage().deserialize("<aqua>Click to go to the channel."))));

                player.sendMessage(ChatColor.AQUA + "Server: " + ChatColor.BOLD + guildById.getName());

                String link = plugin.getConfig().getString("Link");
                if (link != null) {
                    String fakeLink = plugin.getConfig().getString("FakeLink");

                    if (fakeLink == null) fakeLink = link;

                    player.sendMessage(miniMessage.deserialize("<aqua>Link: <bold>" + fakeLink)
                            .hoverEvent(miniMessage.deserialize("<aqua>Click to join the discord"))
                            .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, link)));
                }

                player.sendMessage(ChatColor.AQUA.toString() + ChatColor.STRIKETHROUGH + "                                               ");


                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);

                return;
            }

            Component message = miniMessage.deserialize("<green>Click to copy your code: <bold>" + code)
                    .hoverEvent(HoverEvent.showText(miniMessage.deserialize("<green>Click to copy")))
                    .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, code));

            player.sendMessage(message);

            player.sendMessage(ChatColor.GRAY + "Please send the code to the discord bot:");
            player.sendMessage(ChatColor.AQUA + "Bot Name: " + DiscordBot.getBotName());

        });
        return true;
    }
}