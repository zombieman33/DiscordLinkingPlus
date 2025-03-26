package me.zombieman.dev.discordlinkingplus.manager;

import java.util.regex.Pattern;

public class ColorManager {

    private static final Pattern HEX_PATTERN = Pattern.compile("<#[a-fA-F0-9]{6}>");

    private static final Pattern AMPERSAND_HEX_PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");

    private static final Pattern COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-ORX]|&[0-9A-FK-OR]");

    public static String removeColors(String input) {
        if (input == null) return "";

        input = HEX_PATTERN.matcher(input).replaceAll("");

        input = AMPERSAND_HEX_PATTERN.matcher(input).replaceAll("");

        input = COLOR_PATTERN.matcher(input).replaceAll("");

        return input;
    }
}
