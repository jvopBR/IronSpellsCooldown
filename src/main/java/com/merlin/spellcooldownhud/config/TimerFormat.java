package com.merlin.spellcooldownhud.config;

import net.minecraft.util.Mth;

import java.util.Locale;

/** Format of the remaining-time number. */
public enum TimerFormat {
    /** Whole seconds, rounded up: "7s". */
    SECONDS {
        @Override
        public String format(float seconds) {
            return Mth.ceil(seconds) + "s";
        }
    },

    /** Tenths below 10s ("3.4"), whole above -- precision only matters near the end. */
    TENTHS {
        @Override
        public String format(float seconds) {
            return seconds < 10.0f
                    ? String.format(Locale.ROOT, "%.1f", seconds)
                    : String.valueOf(Mth.ceil(seconds));
        }
    },

    /** Clock, for long cooldowns: "1:05". */
    MM_SS {
        @Override
        public String format(float seconds) {
            int total = Mth.ceil(seconds);
            return String.format(Locale.ROOT, "%d:%02d", total / 60, total % 60);
        }
    };

    public abstract String format(float seconds);
}
