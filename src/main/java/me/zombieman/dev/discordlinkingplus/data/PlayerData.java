package me.zombieman.dev.discordlinkingplus.data;

import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerData {

    public static final String DATA_FOLDER_NAME = "playerData";
    public static final ConcurrentHashMap<UUID, FileConfiguration> playerDataCache = new ConcurrentHashMap<>();

    public static void initDataFolder(DiscordLinkingPlus plugin) {
        File playerDataFolder = new File(plugin.getDataFolder(), DATA_FOLDER_NAME);
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }
    }

    public static FileConfiguration getPlayerDataConfig(DiscordLinkingPlus plugin, Player player) {
        return getPlayerDataConfig(plugin, player.getUniqueId());
    }

    public static FileConfiguration getPlayerDataConfig(DiscordLinkingPlus plugin, UUID uuid) {
        FileConfiguration data = getCached(uuid);
        if (data != null) return data;

        File playerFile = getPlayerFile(plugin, uuid);
        if (!playerFile.exists()) {
            createFile(plugin, uuid);
        }

        data = YamlConfiguration.loadConfiguration(playerFile);
        cache(uuid, data);

        return data;
    }

    public static void createFile(DiscordLinkingPlus plugin, UUID uuid) {
        File playerFile = getPlayerFile(plugin, uuid);

        if (!playerFile.exists()) {
            try {
                playerFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean checkPlayerExist(DiscordLinkingPlus plugin, String playerName) {
        File playerDataFolder = new File(plugin.getDataFolder(), DATA_FOLDER_NAME);
        if (playerDataFolder.exists() && playerDataFolder.isDirectory()) {
            for (File playerFile : playerDataFolder.listFiles()) {
                FileConfiguration data = YamlConfiguration.loadConfiguration(playerFile);
                String name = data.getString("name");
                if (playerName.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static FileConfiguration getPlayerDataConfigByName(DiscordLinkingPlus plugin, String playerName) {
        File playerDataFolder = new File(plugin.getDataFolder(), DATA_FOLDER_NAME);
        if (playerDataFolder.exists() && playerDataFolder.isDirectory()) {
            for (File playerFile : playerDataFolder.listFiles()) {
                FileConfiguration data = YamlConfiguration.loadConfiguration(playerFile);
                String name = data.getString("name");
                if (playerName.equalsIgnoreCase(name)) {
                    return getPlayerDataConfig(plugin, UUID.fromString(playerFile.getName().replace(".yml", "")));
                }
            }
        }
        return null;
    }

    public static void savePlayerData(DiscordLinkingPlus plugin, UUID playerUUID) {
        File playerFile = getPlayerFile(plugin, playerUUID);
        FileConfiguration data = getCached(playerUUID);

        try {
            data.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        cache(playerUUID, data);
    }

    @NotNull
    private static File getPlayerFile(DiscordLinkingPlus plugin, UUID playerUUID) {
        return new File(plugin.getDataFolder(), DATA_FOLDER_NAME + "/" + playerUUID + ".yml");
    }

    private static FileConfiguration getCached(UUID uuid) {
        return playerDataCache.get(uuid);
    }

    private static void cache(UUID uuid, FileConfiguration data) {
        playerDataCache.put(uuid, data);
    }

    public static void cleanupCache(Player player) {
        playerDataCache.remove(player.getUniqueId());
    }
    public static List<String> getAllPlayerDataFiles(DiscordLinkingPlus plugin) {
        File playerDataFolder = new File(plugin.getDataFolder(), PlayerData.DATA_FOLDER_NAME);
        if (!playerDataFolder.exists() || !playerDataFolder.isDirectory()) {
            return Collections.emptyList();
        }

        File[] playerFiles = playerDataFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (playerFiles == null) {
            return Collections.emptyList();
        }

        List<String> playerFileNames = new ArrayList<>();
        for (File file : playerFiles) {
            String fileName = file.getName();
            if (fileName.endsWith(".yml")) {
                playerFileNames.add(fileName.substring(0, fileName.length() - 4));
            }
        }
        return playerFileNames;
    }
}