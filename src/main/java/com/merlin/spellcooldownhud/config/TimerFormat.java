package com.merlin.spellcooldownhud.config;

import net.minecraft.util.Mth;

import java.util.Locale;

/** Formato do numero de tempo restante. */
public enum TimerFormat {
    /** Segundos inteiros, arredondados para cima: "7s". */
    SECONDS {
        @Override
        public String format(float seconds) {
            return Mth.ceil(seconds) + "s";
        }
    },

    /** Decimos abaixo de 10s ("3.4"), inteiros acima -- a precisao so importa perto do fim. */
    TENTHS {
        @Override
        public String format(float seconds) {
            return seconds < 10.0f
                    ? String.format(Locale.ROOT, "%.1f", seconds)
                    : String.valueOf(Mth.ceil(seconds));
        }
    },

    /** Relogio, para cooldowns longos: "1:05". */
    MM_SS {
        @Override
        public String format(float seconds) {
            int total = Mth.ceil(seconds);
            return String.format(Locale.ROOT, "%d:%02d", total / 60, total % 60);
        }
    };

    public abstract String format(float seconds);
}
