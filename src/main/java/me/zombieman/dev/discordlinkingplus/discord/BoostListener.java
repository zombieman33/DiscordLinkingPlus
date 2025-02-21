package me.zombieman.dev.discordlinkingplus.discord;

import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import me.zombieman.dev.discordlinkingplus.manager.RewardsManager;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateBoostTimeEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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
    public void onGuildMemberUpdateBoostTime(GuildMemberUpdateBoostTimeEvent event) {

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

                String stopped = "stopped";

                boolean isOnline = Bukkit.getPlayer(uuid) != null;

                if (event.getNewTimeBoosted() != null) stopped = "started";

                // Stopped boosting

                for (String command : plugin.getConfig().getStringList("boosting." + stopped)) {

                    if (isOnline) command = RewardsManager.commandReplacements(command, Bukkit.getPlayer(uuid));
                    if (!isOnline) command = RewardsManager.commandReplacementsUUID(command, uuid);

                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }

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
