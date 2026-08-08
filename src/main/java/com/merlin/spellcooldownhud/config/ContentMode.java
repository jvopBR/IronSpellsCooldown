package com.merlin.spellcooldownhud.config;

/** Which spells the HUD shows. */
public enum ContentMode {
    /** Only spells on cooldown: they appear on cast and vanish when ready. */
    ONLY_ON_COOLDOWN,
    /** All equipped spells, all the time, marking which are on cooldown. */
    ALL_EQUIPPED
}
