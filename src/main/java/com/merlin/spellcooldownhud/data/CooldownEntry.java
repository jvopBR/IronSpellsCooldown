package com.merlin.spellcooldownhud.data;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Uma magia a ser desenhada na HUD, ja traduzida para tipos do Minecraft puro.
 *
 * <p>Nenhum tipo do Iron's Spells aparece aqui de proposito: {@link IronSpellsSource} e a unica
 * classe que fala com aquela API, e ela entrega este record. Assim o resto do mod (tracker,
 * layout, renderers, editor) nao depende do Iron's Spells e consegue rodar com dados
 * sinteticos no preview do editor.
 *
 * @param spellId        id do registry, ex. {@code irons_spellbooks:fireball}
 * @param icon           textura do icone, ja no caminho completo, ou null se a magia nao tiver
 * @param displayName    nome traduzido
 * @param level          nivel da magia; 0 quando desconhecido
 * @param remainingTicks ticks que faltam para ficar pronta; 0 = pronta
 * @param totalTicks     duracao total do cooldown, para calcular a fracao
 * @param schoolColor    RGB da escola da magia (sem alpha)
 * @param slotIndex      posicao no spellbook, para ordenacao estavel; -1 se desconhecida
 */
public record CooldownEntry(
        String spellId,
        ResourceLocation icon,
        Component displayName,
        int level,
        int remainingTicks,
        int totalTicks,
        int schoolColor,
        int slotIndex) {

    public boolean ready() {
        return remainingTicks <= 0;
    }

    /** 1.0 logo apos o cast, 0.0 quando pronta -- e a fracao que a varredura/barra cobre. */
    public float remainingFraction() {
        if (totalTicks <= 0) {
            return 0.0f;
        }
        return Mth.clamp((float) remainingTicks / (float) totalTicks, 0.0f, 1.0f);
    }

    /**
     * Copia com outro tempo restante.
     *
     * <p>Usada pelo preview do editor para animar sem dados reais e por
     * {@link ServerSyncedSource} para substituir a contagem local do cliente pela do servidor.
     */
    public CooldownEntry withRemaining(int ticks) {
        return new CooldownEntry(spellId, icon, displayName, level, ticks, totalTicks, schoolColor, slotIndex);
    }
}
