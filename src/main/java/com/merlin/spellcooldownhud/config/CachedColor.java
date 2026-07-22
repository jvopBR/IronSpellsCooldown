package com.merlin.spellcooldownhud.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Locale;

/**
 * Cor guardada no config como hex legivel ("#AARRGGBB" ou "#RRGGBB") e lida como int ARGB.
 *
 * <p>A HUD desenha a cada frame, entao reparsear a string toda vez seria desperdicio. Esta classe
 * guarda o ultimo texto visto e so reparseia quando ele muda -- o que, de quebra, faz o valor
 * acompanhar reloads de config e edicoes feitas no editor sem nenhuma invalidacao manual.
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

    /** O valor cru, para o editor escrever. */
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

    /** Aceita "#RRGGBB", "#AARRGGBB", com ou sem "#"/"0x". Texto invalido cai no padrao. */
    public static int parse(String raw, int fallback) {
        String hex = strip(raw);
        try {
            return switch (hex.length()) {
                case 6 -> 0xFF000000 | Integer.parseInt(hex, 16);
                // parseLong porque valores com alpha alto estouram o range positivo de int
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

    /** Validador usado no {@code define} do config, para o TOML rejeitar lixo na hora de carregar. */
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
