package com.merlin.spellcooldownhud.config;

/** Order of entries on the HUD. */
public enum SortMode {
    /** Ready-soonest first. */
    TIME_REMAINING_ASC,
    /** Most time remaining first. */
    TIME_REMAINING_DESC,
    /** Spellbook slot order -- stable, icons don't jump around. */
    SLOT_ORDER,
    /** Alphabetical by spell name. */
    NAME
}
