package com.merlin.spellcooldownhud.config;

/** Ordem das entradas na HUD. */
public enum SortMode {
    /** Quem fica pronto primeiro aparece primeiro. */
    TIME_REMAINING_ASC,
    /** Quem tem mais tempo pela frente aparece primeiro. */
    TIME_REMAINING_DESC,
    /** Ordem dos slots do spellbook -- estavel, os icones nao pulam de lugar. */
    SLOT_ORDER,
    /** Alfabetica pelo nome da magia. */
    NAME
}
