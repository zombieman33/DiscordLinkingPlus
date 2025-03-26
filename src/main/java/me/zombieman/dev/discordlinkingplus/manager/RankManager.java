package me.zombieman.dev.discordlinkingplus.manager;

import me.clip.placeholderapi.PlaceholderAPI;
import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.sql.SQLException;
import java.util.*;

public class RankManager {

    private final DiscordLinkingPlus plugin;
    private final JDA jda;

    public RankManager(DiscordLinkingPlus plugin, JDA jda) {
        this.plugin = plugin;
        this.jda = jda;
    }

    public void assignRankAndNickname(Player player) throws SQLException {
        // Retrieve the player's in-game permissions for ranks

        if (player == null) return;

        if (PlaceholderAPI.setPlaceholders(player, plugin.getConfig().getString("placeholder", "%fewernick_isnicked%")).equalsIgnoreCase("1")) {
            return;
        }

        Set<String> playerRanks = new HashSet<>();
        for (PermissionAttachmentInfo permission : player.getEffectivePermissions()) {
            String perm = permission.getPermission();
            if (perm.startsWith("discordlinkingplus.rank.")) {
                String rank = perm.replace("discordlinkingplus.rank.", "");
                if (plugin.getConfig().contains("ranks." + rank)) {
                    playerRanks.add(rank);
                }
            }
        }

        String discordId = plugin.getPlayerDatabase().getPlayerData(player.getUniqueId()).getDiscordTag();
        if (discordId == null) {
            plugin.getLogger().warning("No Discord ID found for player: " + player.getName());
            return;
        }

        Guild guild = plugin.getGuild();
        if (guild == null) {
            plugin.getLogger().warning("Guild not found.");
            return;
        }

        Member member = guild.retrieveMemberById(discordId).complete();
        if (member == null) {
            plugin.getLogger().warning("Member not found for Discord ID: " + discordId);
            return;
        }

        // Process roles for `update-role = true`
        List<Role> currentRoles = member.getRoles();
        for (String rankKey : plugin.getConfig().getConfigurationSection("ranks").getKeys(false)) {
            boolean updateRole = plugin.getConfig().getBoolean("ranks." + rankKey + ".update-role", false);
            String roleId = plugin.getConfig().getString("ranks." + rankKey + ".discordRank");
            if (roleId == null || !updateRole) continue;

            Role role = guild.getRoleById(roleId);
            if (role == null) continue;

            if (currentRoles.contains(role) && !playerRanks.contains(rankKey)) {
                guild.removeRoleFromMember(member, role).queue(
                        success -> plugin.getLogger().info("Removed role " + role.getName() + " from " + member.getEffectiveName()),
                        failure -> plugin.getLogger().warning("Failed to remove role '" + role.getName() + "' from " + member.getEffectiveName())
                );
            } else if (!currentRoles.contains(role) && playerRanks.contains(rankKey)) {
                guild.addRoleToMember(member, role).queue(
                        success -> plugin.getLogger().info("Added role " + role.getName() + " to " + member.getEffectiveName()),
                        failure -> plugin.getLogger().warning("Failed to add role '" + role.getName() + "' to " + member.getEffectiveName())
                );
            }
        }

        // Process nicknames for `update-name = true`
        List<String> validRanks = new ArrayList<>();
        for (String rankKey : playerRanks) {
            if (plugin.getConfig().getBoolean("ranks." + rankKey + ".update-name", false)) {
                validRanks.add(rankKey);
            }
        }

        if (!validRanks.isEmpty()) {
            validRanks.sort(Comparator.comparingInt(rank -> plugin.getConfig().getInt("ranks." + rank + ".priority", 0)).reversed());
            String highestPriorityRank = validRanks.get(0);

            String nicknameFormat = plugin.getConfig().getString("DiscordNickname", "%rank% | %ingame-name% %suffix%");
            boolean shouldUppercase = plugin.getConfig().getBoolean("ShouldRankBeUppercase", false);
            String formattedRank = shouldUppercase ? highestPriorityRank.toUpperCase() : highestPriorityRank;

            // Check for staff settings
            boolean isStaff = plugin.getConfig().getBoolean("ranks." + highestPriorityRank + ".check-icon", false);
            String staffPrefix = plugin.getConfig().getString("ranks." + highestPriorityRank + ".icon-prefix", "");
            String staffPermission = plugin.getConfig().getString("ranks." + highestPriorityRank + ".icon-permission", "");

            if (isStaff && (staffPermission == null || player.hasPermission(staffPermission))) {
                formattedRank = staffPrefix + formattedRank
                        .replace("_PLUS", "+")
                        .replace("-", ". ")
                        .replace("_", "");
            }

            String suffix = PlaceholderAPI.setPlaceholders(player, plugin.getConfig().getString("SuffixPlaceholder", "%vault_suffix%"));

            suffix = ColorManager.removeColors(suffix);

            String formattedNickname = nicknameFormat
                    .replace("%rank%", formattedRank)
                    .replace("%ingame-name%", player.getName())
                    .replace("%discord-name%", member.getEffectiveName())
                    .replace("%suffix%", suffix)
                    .replace("_PLUS", "+");

            // Update the nickname if necessary
            if (!formattedNickname.equals(member.getNickname())) {
                guild.modifyNickname(member, formattedNickname).queue(
                        success -> plugin.getLogger().info("Nickname updated to: " + formattedNickname),
                        failure -> plugin.getLogger().warning("Failed to update nickname for " + member.getEffectiveName())
                );
            }
        }
    }

    public void removeRank(String discordID, boolean isLinked) {
        Guild guild = plugin.getGuild();
        if (guild == null) {
            plugin.getLogger().warning("Guild not found.");
            return;
        }

        if (discordID == null) {
            plugin.getLogger().warning("Discord ID is null");
            return;
        }

        Member member = guild.retrieveMemberById(discordID).complete();
        if (member == null) {
            plugin.getLogger().warning("Member not found for Discord ID: " + discordID);
            return;
        }

        // Determine the path based on linked status
        String linkedOrNot = isLinked ? "linked" : "unLinked";
        String path = "ranksToRemove." + linkedOrNot + ".discordRanks";

        if (!plugin.getConfig().contains(path)) {
            plugin.getLogger().warning("Config path not found: " + path);
            return;
        }

        List<String> roleIds = plugin.getConfig().getStringList(path);
        if (roleIds.isEmpty()) {
            plugin.getLogger().info("No roles configured to remove for type: " + linkedOrNot);
            return;
        }

        for (String roleId : roleIds) {
            Role role = guild.getRoleById(roleId);
            if (role != null && member.getRoles().contains(role)) {
                guild.removeRoleFromMember(member, role).queue(
                        success -> plugin.getLogger().info("Successfully removed role: " + role.getName() + " from " + member.getEffectiveName()),
                        failure -> plugin.getLogger().warning("Failed to remove role: " + role.getName() + " from " + member.getEffectiveName())
                );
            }
        }
    }


    public void addRank(String discordID, String linkedOrNot) {
        Guild guild = plugin.getGuild();
        if (guild == null) {
            plugin.getLogger().warning("Guild not found.");
            return;
        }

        Member member = guild.retrieveMemberById(discordID).complete();
        if (member == null) {
            plugin.getLogger().warning("Member not found for Discord ID: " + discordID);
            return;
        }

        String path = "ranksToAdd." + linkedOrNot + ".discordRanks";
        if (!plugin.getConfig().contains(path)) {
            plugin.getLogger().warning("Config path not found: " + path);
            return;
        }

        List<String> roleIds = plugin.getConfig().getStringList(path);
        if (roleIds.isEmpty()) {
            plugin.getLogger().info("No roles configured to add for type: " + linkedOrNot);
            return;
        }

        for (String roleId : roleIds) {
            Role role = guild.getRoleById(roleId);
            if (role != null && !member.getRoles().contains(role)) {
                guild.addRoleToMember(member, role).queue(
                        success -> plugin.getLogger().info("Successfully added role: " + role.getName() + " to " + member.getEffectiveName()),
                        failure -> plugin.getLogger().warning("Failed to add role: " + role.getName() + " to " + member.getEffectiveName())
                );
            }
        }
    }

    public void commands(String playerName, boolean isLinked) {
        String linkedOrNot = isLinked ? "linked" : "unlinked";
        String path = "commands." + linkedOrNot;

        if (!plugin.getConfig().contains(path)) {
            plugin.getLogger().warning("No commands configured for type: " + linkedOrNot);
            return;
        }

        List<String> commands = plugin.getConfig().getStringList(path);
        if (commands.isEmpty()) {
            plugin.getLogger().info("No commands to execute for type: " + linkedOrNot);
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (String command : commands) {
                if (command.contains("%player%")) {
                    command = command.replace("%player%", playerName);
                }

                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to execute command: " + command);
                    e.printStackTrace();
                }

            }
        });
    }
}
