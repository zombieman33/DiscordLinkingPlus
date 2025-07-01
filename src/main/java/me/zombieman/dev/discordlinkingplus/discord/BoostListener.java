package me.zombieman.dev.discordlinkingplus.discord;

import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import me.zombieman.dev.discordlinkingplus.manager.RewardsManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateBoostTimeEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;

import java.nio.Buffer;
import java.sql.SQLException;
import java.util.UUID;

public class BoostListener extends ListenerAdapter {

    private final DiscordLinkingPlus plugin;

    public BoostListener(DiscordLinkingPlus plugin) {
        this.plugin = plugin;
        System.out.println("Initialized boost listener");
    }

    @Override
    public void onGuildMemberUpdateBoostTime(@NotNull GuildMemberUpdateBoostTimeEvent event) {

        System.out.println(" ");
        System.out.println("Someone SHOULD have boosted the server now!");
        System.out.println("Info:");
        System.out.println("Member: " + event.getMember());
        System.out.println(" ");
        System.out.println("New time boosted: " + event.getNewTimeBoosted());
        System.out.println("Old time boosted: " + event.getOldTimeBoosted());
        System.out.println("RawData: " + event.getRawData());
        System.out.println(" ");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {

                String id = event.getUser().getId();
                UUID uuid = plugin.getPlayerDatabase().getUuidByDiscordTag(id);

                if (uuid == null) return;

                String stopped = "started";

                boolean isOnline = Bukkit.getPlayer(uuid) != null;


                Guild guild = plugin.getGuild();
                if (guild != null) {
                    Member member = guild.retrieveMemberById(id).complete();

                    if (member != null) {
                        if (plugin.getPlayerDatabase().getPlayerData(uuid).getBoosting() && !member.isBoosting()) stopped = "stopped";
                    }
                }

                for (String command : plugin.getConfig().getStringList("boosting." + stopped)) {

                    if (isOnline) command = RewardsManager.commandReplacements(command, Bukkit.getPlayer(uuid));
                    if (!isOnline) command = RewardsManager.commandReplacementsUUID(command, uuid);

                    String finalCommand = command;

                    System.out.println("Commands: " + finalCommand);

                    Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));

                }

                boolean start = !stopped.equalsIgnoreCase("stopped");

                plugin.getPlayerDatabase().updateBoosting(uuid, start);

                sendMessage(uuid, stopped);

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void sendMessage(UUID uuid, String started) {
        String message = "<#D16EFF>You have stopped <bold>boosting</bold> \nour discord server.";

        if (started.equalsIgnoreCase("started")) {
            message = "<#D16EFF>You have started <bold>boosting</bold> \nour discord server.";
        }

        try (Jedis jedis = plugin.getJedisResource()) {
            jedis.publish("DISCORD_LINKING", "MINECRAFT_MESSAGE:" + uuid + ":" + message);
        }
    }

}
