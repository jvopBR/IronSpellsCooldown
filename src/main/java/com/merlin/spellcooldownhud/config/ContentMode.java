package com.merlin.spellcooldownhud.config;

/** Quais magias entram na HUD. */
public enum ContentMode {
    /** So as que estao em cooldown: aparecem ao usar e somem quando ficam prontas. */
    ONLY_ON_COOLDOWN,
    /** Todas as magias equipadas, o tempo todo, indicando quais estao em cooldown. */
    ALL_EQUIPPED
}
