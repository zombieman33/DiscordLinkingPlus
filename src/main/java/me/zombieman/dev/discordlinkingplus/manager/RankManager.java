package me.zombieman.dev.discordlinkingplus.manager;

import me.clip.placeholderapi.PlaceholderAPI;
import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import me.zombieman.dev.discordlinkingplus.api.API;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.sql.SQLException;
import java.util.*;

public class RankManager {

    private final DiscordLinkingPlus plugin;
    private final LuckPerms luckPerms;
    private final JDA jda;

    public RankManager(DiscordLinkingPlus plugin, JDA jda) {
        this.plugin = plugin;
        this.jda = jda;

        this.luckPerms = LuckPermsProvider.get();
        if (plugin.isMainServer()) Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::validateRoles, 0L, 20L * 60 * 60 * 12);
        if (plugin.isMainServer()) Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::validateBoosting, 0L, 20L * 60 * 60);
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

            formattedRank = formattedRank.replace("_PLUS", "+")
                    .replace("-", ".")
                    .replace("_", "");

            // Check for staff settings
            boolean isStaff = plugin.getConfig().getBoolean("ranks." + highestPriorityRank + ".check-icon", false);
            String staffPrefix = plugin.getConfig().getString("ranks." + highestPriorityRank + ".icon-prefix", "");
            String staffPermission = plugin.getConfig().getString("ranks." + highestPriorityRank + ".icon-permission", "");

            if (isStaff && player.hasPermission(staffPermission)) formattedRank = staffPrefix + formattedRank;

            String suffix = PlaceholderAPI.setPlaceholders(player, plugin.getConfig().getString("SuffixPlaceholder", "%vault_suffix%"));

            suffix = ColorManager.removeColors(suffix);

            String formattedNickname = nicknameFormat
                    .replace("%rank%", formattedRank)
                    .replace("%ingame-name%", player.getName())
                    .replace("%discord-name%", member.getEffectiveName())
                    .replace("_PLUS", "+");

            int suffixLength = suffix.length();

            if (formattedNickname.replace("%suffix%", "").length() <= (32 - suffixLength)) {
                formattedNickname = formattedNickname.replace("%suffix%", suffix);
            } else {
                System.out.println("Could not include users suffix. Nickname is too long.");
            }

            formattedNickname = formattedNickname.replace("%suffix%", "");

            // Update the nickname if necessary
            if (!formattedNickname.equals(member.getNickname())) {
                String finalFormattedNickname = formattedNickname;
                guild.modifyNickname(member, formattedNickname).queue(
                        success -> plugin.getLogger().info("Nickname updated to: " + finalFormattedNickname),
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


        if (guild.retrieveMemberById(discordID).complete() == null) {
            plugin.getLogger().warning("Member not found for Discord ID: " + discordID);
            return;
        }

        Member member = guild.retrieveMemberById(discordID).complete();


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

    public void validateRoles() {

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getLogger().info("Starting role validation task...");

            Map<String, String> roleIdToRank = loadRoleIdToRank();
            System.out.println(roleIdToRank);
            Guild guild = jda.getGuildById(plugin.guildID());
            if (guild == null) {
                plugin.getLogger().warning("Guild not found with ID: " + plugin.guildID());
                return;
            }

            plugin.getLogger().info("Checking roles for " + guild.getMembers().size() + " members...");

            for (Member member : guild.getMembers()) {
                try {
                    API api = plugin.getApi();
                    if (api == null) continue;

                    UUID uuid = api.getUUIDFromDiscordTag(member.getId());

                    if (uuid == null) continue;

                    User lpUser = getLuckPermsUser(uuid);
                    if (lpUser == null) {
                        plugin.getLogger().info("LuckPerms user not found for UUID: " + uuid);
                        continue;
                    }

                    for (Role role : member.getRoles()) {
                        String rankName = roleIdToRank.get(role.getId());
                        if (rankName == null) continue;

                        String permission = "discordlinkingplus.rank." + rankName;

                        boolean hasPermission = lpUser.getCachedData()
                                .getPermissionData()
                                .checkPermission(permission)
                                .asBoolean();

                        if (!hasPermission) {
                            plugin.getLogger().info("Removing role '" + role.getName() +
                                    "' from member '" + member.getUser().getAsTag() +
                                    "' (missing permission: " + permission + ")");
                            guild.removeRoleFromMember(member, role).queue();
//                        } else {
//                            plugin.getLogger().info("Member '" + member.getUser().getAsTag() +
//                                    "' has permission: " + permission);
                        }
                    }
                } catch (SQLException e) {
                    plugin.getLogger().severe("SQLException while validating roles for member: " + member.getUser().getAsTag());
                    e.printStackTrace();
                }
            }

            plugin.getLogger().info("Finished role validation task.");
        });
    }

    public void validateBoosting() {

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            Map<String, String> roleIdToRank = loadRoleIdToRank();
            System.out.println(roleIdToRank);
            Guild guild = jda.getGuildById(plugin.guildID());
            if (guild == null) {
                plugin.getLogger().warning("Guild not found with ID: " + plugin.guildID());
                return;
            }

            plugin.getLogger().info("Checking boosting status for " + guild.getMembers().size() + " members...");

            for (Member member : guild.getMembers()) {
                try {
                    API api = plugin.getApi();
                    if (api == null) continue;

                    UUID uuid = api.getUUIDFromDiscordTag(member.getId());

                    if (uuid == null) continue;

                    if (!member.isBoosting()) continue;

                    if (!plugin.getPlayerDatabase().getPlayerData(uuid).getBoosting()) continue;

                    plugin.getPlayerDatabase().updateBoosting(uuid, false);

                    for (String command : plugin.getConfig().getStringList("boosting.stopped")) {

                        String finalCommand = RewardsManager.commandReplacementsUUID(command, uuid);

                        System.out.println("Command: " + finalCommand);

                        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
                    }

                } catch (SQLException e) {
                    plugin.getLogger().severe("SQLException while validating boosting status for member: " + member.getUser().getAsTag());
                    e.printStackTrace();
                }
            }

            plugin.getLogger().info("Finished boosting validation task.");
        });
    }

    private Map<String, String> loadRoleIdToRank() {
        Map<String, String> map = new HashMap<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("ranks");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String roleId = section.getConfigurationSection(key).getString("discordRank");
                if (roleId != null) {
                    map.put(roleId, key);
                }
            }
        }
        return map;
    }

    private User getLuckPermsUser(UUID uuid) {
        User user = LuckPermsProvider.get().getUserManager().getUser(uuid);

        if (user == null) {
            LuckPermsProvider.get().getUserManager().loadUser(uuid).join();
            user = LuckPermsProvider.get().getUserManager().getUser(uuid);
        }

        return user;
    }

}
