package me.zombieman.dev.discordlinkingplus.manager;

import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.user.User;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;

import java.sql.SQLException;

public class LuckpermsManager {

    private final LuckPerms luckPerms;
    private final DiscordLinkingPlus plugin;

    public LuckpermsManager(DiscordLinkingPlus plugin) {
        this.luckPerms = LuckPermsProvider.get();
        this.plugin = plugin;
        registerListener();
        System.out.println("Initialized luckperms manager!");
    }

    public void registerListener() {
        if (Bukkit.getServer().isPrimaryThread()) {
            luckPerms.getEventBus().subscribe(UserDataRecalculateEvent.class, this::onUserDataRecalculate);
        } else {
            Bukkit.getScheduler().runTask(plugin, () ->
                    luckPerms.getEventBus().subscribe(UserDataRecalculateEvent.class, this::onUserDataRecalculate)
            );
        }
    }

    private void onUserDataRecalculate(UserDataRecalculateEvent event) {
        User user = event.getUser();

        if (Bukkit.getPlayer(user.getUniqueId()) == null) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getRankManager().assignRankAndNickname(Bukkit.getPlayer(user.getUniqueId()));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}