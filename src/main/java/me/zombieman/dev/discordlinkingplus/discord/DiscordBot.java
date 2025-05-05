package me.zombieman.dev.discordlinkingplus.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

public class DiscordBot {
    private static JDA jdaInstance;

    private DiscordBot() {}

    public static JDA getInstance(String token, String playing) {
        if (jdaInstance == null) {
            try {
                jdaInstance = JDABuilder.createDefault(token)
                        .enableIntents(
                                GatewayIntent.GUILD_MESSAGES,
                                GatewayIntent.MESSAGE_CONTENT,
                                GatewayIntent.DIRECT_MESSAGES,
                                GatewayIntent.GUILD_MEMBERS,
                                GatewayIntent.GUILD_MODERATION
                        )
                        .setChunkingFilter(ChunkingFilter.ALL)
                        .setMemberCachePolicy(MemberCachePolicy.ALL)
                        .setActivity(Activity.playing(playing))
                        .setEventPassthrough(true)
                        .build();

                jdaInstance.awaitReady();
                System.out.println("Discord bot initialized with " +
                        jdaInstance.getGuilds().size() + " guilds.");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return jdaInstance;
    }


    public static void shutdown() {
        if (jdaInstance != null) {
            jdaInstance.shutdown();
            jdaInstance = null;
            System.out.println("Discord bot shut down.");
        }
    }

    public static JDA getBot() {
        return jdaInstance;
    }

    public static String getBotName() {
        if (jdaInstance != null) {
            User selfUser = jdaInstance.getSelfUser();
            return selfUser.getName();
        }
        return null;
    }
}
