package com.merlin.spellcooldownhud.config;

/** How each cooldown is drawn. */
public enum HudStyle {
    /** Icon with a dark radial sweep on top, WoW/LoL style. */
    RADIAL,
    /** Icon with a horizontal progress bar below it. */
    BAR,
    /** Text only: spell name and remaining time. */
    TEXT_LIST
}
