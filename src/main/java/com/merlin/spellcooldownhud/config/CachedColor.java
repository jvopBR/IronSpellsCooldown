package com.merlin.spellcooldownhud.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Locale;

/**
 * A color stored in the config as readable hex ("#AARRGGBB" or "#RRGGBB") and read as an ARGB int.
 *
 * <p>The HUD draws every frame, so reparsing the string each time would be wasteful. This class
 * keeps the last seen text and only reparses when it changes -- which, as a bonus, makes the value
 * follow config reloads and edits made in the editor without any manual invalidation.
 */
public final class CachedColor {
    private final ModConfigSpec.ConfigValue<String> value;
    private final int fallback;

    private String lastRaw;
    private int cached;

    CachedColor(ModConfigSpec.ConfigValue<String> value, int fallback) {
        this.value = value;
        this.fallback = fallback;
        this.cached = fallback;
    }

    /** The raw value, for the editor to write to. */
    public ModConfigSpec.ConfigValue<String> raw() {
        return value;
    }

    public int argb() {
        String current = value.get();
        if (!current.equals(lastRaw)) {
            lastRaw = current;
            cached = parse(current, fallback);
        }
        return cached;
    }

    public void set(int argb) {
        value.set(toHex(argb));
    }

    /** Accepts "#RRGGBB", "#AARRGGBB", with or without "#"/"0x". Invalid text falls back to default. */
    public static int parse(String raw, int fallback) {
        String hex = strip(raw);
        try {
            return switch (hex.length()) {
                case 6 -> 0xFF000000 | Integer.parseInt(hex, 16);
                // parseLong because high-alpha values overflow int's positive range
                case 8 -> (int) Long.parseLong(hex, 16);
                default -> fallback;
            };
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static String toHex(int argb) {
        return String.format(Locale.ROOT, "#%08X", argb);
    }

    /** Validator used in the config {@code define}, so the TOML rejects garbage at load time. */
    public static boolean isValid(Object raw) {
        if (!(raw instanceof String text)) {
            return false;
        }
        String hex = strip(text);
        if (hex.length() != 6 && hex.length() != 8) {
            return false;
        }
        return hex.chars().allMatch(c -> Character.digit(c, 16) >= 0);
    }

    private static String strip(String raw) {
        String hex = raw.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() > 1 && (hex.startsWith("0x") || hex.startsWith("0X"))) {
            hex = hex.substring(2);
        }
        return hex;
    }
}
