package com.merlin.spellcooldownhud.config;

/** Direction in which entries stack from the anchor. */
public enum GrowDirection {
    RIGHT,
    LEFT,
    DOWN,
    UP;

    public boolean horizontal() {
        return this == RIGHT || this == LEFT;
    }

    /** True when entries walk in the negative axis direction (left or up). */
    public boolean reversed() {
        return this == LEFT || this == UP;
    }
}
