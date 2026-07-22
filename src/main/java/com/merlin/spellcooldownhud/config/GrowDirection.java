package com.merlin.spellcooldownhud.config;

/** Direcao em que as entradas se acumulam a partir da ancora. */
public enum GrowDirection {
    RIGHT,
    LEFT,
    DOWN,
    UP;

    public boolean horizontal() {
        return this == RIGHT || this == LEFT;
    }

    /** True quando as entradas caminham no sentido negativo do eixo (esquerda ou para cima). */
    public boolean reversed() {
        return this == LEFT || this == UP;
    }
}
