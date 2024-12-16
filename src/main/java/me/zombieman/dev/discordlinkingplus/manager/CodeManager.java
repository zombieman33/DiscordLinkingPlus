package me.zombieman.dev.discordlinkingplus.manager;

import me.zombieman.dev.discordlinkingplus.DiscordLinkingPlus;
import org.bukkit.Bukkit;

import java.util.*;

public class CodeManager {

    private static final List<String> codes = new ArrayList<>();
    private static final Map<String, UUID> playersCode = new HashMap<>();
    private static final Random random = new Random();
    private static final String CHARACTERS = "ABCDEFGHIJKLMNPQRSTUVXYZ123456789";

    public static String createCode(DiscordLinkingPlus plugin, UUID uuid) {
        String code;

        do {
            code = generateRandomCode();
        } while (codes.contains(code));

        codes.add(code);
        playersCode.put(code, uuid);

        String finalCode = code;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            codes.remove(finalCode);
            playersCode.remove(finalCode);
        }, 60 * 20L);

        return code;
    }

    public static boolean checkCode(String code) {
        return codes.contains(code);
    }
    public static void removeCode(String code) {
        codes.remove(code);
        playersCode.remove(code);
    }

    public static UUID getPlayerWithCode(String code) {
        return playersCode.get(code);
    }

    private static String generateRandomCode() {
        StringBuilder codeBuilder = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            int index = random.nextInt(CHARACTERS.length());
            codeBuilder.append(CHARACTERS.charAt(index));
        }
        return codeBuilder.toString();
    }
}
