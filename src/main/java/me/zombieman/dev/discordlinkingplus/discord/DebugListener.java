package me.zombieman.dev.discordlinkingplus.discord;

import net.dv8tion.jda.api.events.GenericEvent;
import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class DebugListener extends ListenerAdapter {

    private final DiscordLinkingPlus plugin;

    public DebugListener(DiscordLinkingPlus plugin) {
        this.plugin = plugin;
        System.out.println("Initialized debug listener");
    }

    @Override
    public void onGenericEvent(@NotNull GenericEvent event) {
        System.out.println("Received event: " + event.getClass().getSimpleName());
    }
}
