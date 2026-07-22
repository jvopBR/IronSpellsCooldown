package com.merlin.spellcooldownhud.client;

import com.merlin.spellcooldownhud.config.ContentMode;
import com.merlin.spellcooldownhud.config.HudConfig;
import com.merlin.spellcooldownhud.config.SortMode;
import com.merlin.spellcooldownhud.data.CooldownEntry;
import com.merlin.spellcooldownhud.data.CooldownSource;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mantem o estado de animacao das entradas entre um tick e outro.
 *
 * <p>Existe porque a fonte de dados nao basta sozinha: no modo {@code ONLY_ON_COOLDOWN} o Iron's
 * Spells remove a magia do mapa no instante em que ela fica pronta, entao sem guardar estado
 * proprio o icone sumiria de um frame para o outro, sem fade e sem o brilho de "pronta".
 *
 * <p>Os tempos sao medidos em relogio de parede, nao em ticks, para o fade ficar suave em
 * qualquer framerate -- 20 ticks/s dariam uma animacao granulada num monitor de 144 Hz.
 *
 * <p>Nao e thread-safe de proposito: tick de cliente e render rodam na mesma thread.
 */
public final class CooldownTracker {

    /** Duracao do brilho de "ficou pronta". Curto de proposito: e um flash, nao uma animacao. */
    private static final long READY_FLASH_MS = 400L;

    private static final long MS_PER_TICK = 50L;

    private final CooldownSource source;
    private final Map<String, Tracked> tracked = new LinkedHashMap<>();

    public CooldownTracker(CooldownSource source) {
        this.source = source;
    }

    /** Uma entrada pronta para desenhar: o dado + o estado de animacao ja resolvido. */
    public record TrackedEntry(CooldownEntry entry, float alpha, float readyFlash) {
    }

    private static final class Tracked {
        private CooldownEntry entry;
        private final long firstSeenMs;
        /** Quando a magia deixou de aparecer na fonte; 0 enquanto presente. */
        private long goneSinceMs;
        /** Quando ficou pronta, para o brilho; 0 se ainda nao ficou. */
        private long becameReadyMs;

        private Tracked(CooldownEntry entry, long nowMs) {
            this.entry = entry;
            this.firstSeenMs = nowMs;
        }
    }

    public void clear() {
        tracked.clear();
    }

    /** Chamado a cada tick de cliente (ou por frame, no preview do editor). */
    public void refresh(ContentMode mode) {
        long now = System.currentTimeMillis();
        long fadeOutMs = HudConfig.FADE_OUT_TICKS.get() * MS_PER_TICK;

        Set<String> present = new HashSet<>();

        for (CooldownEntry fresh : source.collect(mode)) {
            present.add(fresh.spellId());

            Tracked existing = tracked.get(fresh.spellId());
            if (existing == null) {
                tracked.put(fresh.spellId(), new Tracked(fresh, now));
                continue;
            }

            // Transicao de "em cooldown" para "pronta" dispara o brilho, mesmo que a entrada
            // continue na lista (caso do modo ALL_EQUIPPED).
            if (!existing.entry.ready() && fresh.ready()) {
                existing.becameReadyMs = now;
            }
            existing.entry = fresh;
            existing.goneSinceMs = 0L;
        }

        // O que a fonte parou de reportar: marca a saida e mantem vivo durante o fade-out.
        Iterator<Map.Entry<String, Tracked>> iterator = tracked.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Tracked> mapping = iterator.next();
            if (present.contains(mapping.getKey())) {
                continue;
            }

            Tracked candidate = mapping.getValue();
            if (candidate.goneSinceMs == 0L) {
                candidate.goneSinceMs = now;
                if (candidate.becameReadyMs == 0L) {
                    // Sumiu do mapa de cooldowns: no Iron's Spells isso significa que ficou pronta.
                    candidate.becameReadyMs = now;
                }
            }
            // Com fadeOutMs = 0 a condicao vale de imediato, que e justamente "sem fade".
            if (now - candidate.goneSinceMs >= fadeOutMs) {
                iterator.remove();
            }
        }
    }

    /** Entradas a desenhar neste frame, ja ordenadas, cortadas e com alpha resolvido. */
    public List<TrackedEntry> snapshot(ContentMode mode, SortMode sortMode, int maxEntries) {
        long now = System.currentTimeMillis();
        float globalOpacity = HudConfig.OPACITY.get().floatValue();
        long fadeInMs = HudConfig.FADE_IN_TICKS.get() * MS_PER_TICK;
        long fadeOutMs = HudConfig.FADE_OUT_TICKS.get() * MS_PER_TICK;
        boolean flashEnabled = HudConfig.FLASH_WHEN_READY.get();

        List<CooldownEntry> visible = new ArrayList<>(tracked.size());
        for (Tracked candidate : tracked.values()) {
            visible.add(candidate.entry);
        }
        visible.sort(comparatorFor(sortMode));

        int limit = Math.min(maxEntries, visible.size());
        List<TrackedEntry> out = new ArrayList<>(limit);

        for (int i = 0; i < limit; i++) {
            CooldownEntry entry = visible.get(i);
            Tracked state = tracked.get(entry.spellId());
            if (state == null) {
                continue;
            }

            float alpha = globalOpacity
                    * fadeInFactor(state, now, fadeInMs)
                    * fadeOutFactor(state, now, fadeOutMs);
            if (alpha <= 0.01f) {
                continue;
            }

            float flash = flashEnabled ? flashFactor(state, now) : 0.0f;
            out.add(new TrackedEntry(entry, Mth.clamp(alpha, 0.0f, 1.0f), flash));
        }
        return out;
    }

    private static float fadeInFactor(Tracked state, long now, long fadeInMs) {
        if (fadeInMs <= 0L) {
            return 1.0f;
        }
        return Mth.clamp((now - state.firstSeenMs) / (float) fadeInMs, 0.0f, 1.0f);
    }

    private static float fadeOutFactor(Tracked state, long now, long fadeOutMs) {
        if (state.goneSinceMs == 0L) {
            return 1.0f;
        }
        if (fadeOutMs <= 0L) {
            return 0.0f;
        }
        return Mth.clamp(1.0f - (now - state.goneSinceMs) / (float) fadeOutMs, 0.0f, 1.0f);
    }

    private static float flashFactor(Tracked state, long now) {
        if (state.becameReadyMs == 0L) {
            return 0.0f;
        }
        long since = now - state.becameReadyMs;
        if (since >= READY_FLASH_MS) {
            return 0.0f;
        }
        return Mth.clamp(1.0f - since / (float) READY_FLASH_MS, 0.0f, 1.0f);
    }

    private static Comparator<CooldownEntry> comparatorFor(SortMode mode) {
        // spellId como desempate em todos os modos: sem isso, entradas empatadas trocariam de
        // lugar entre frames e a HUD ficaria tremendo.
        Comparator<CooldownEntry> tieBreaker = Comparator.comparing(CooldownEntry::spellId);

        return switch (mode) {
            case TIME_REMAINING_ASC ->
                    Comparator.comparingInt(CooldownEntry::remainingTicks).thenComparing(tieBreaker);
            case TIME_REMAINING_DESC ->
                    Comparator.comparingInt(CooldownEntry::remainingTicks).reversed().thenComparing(tieBreaker);
            case SLOT_ORDER ->
                    // slotIndex -1 (magia nao equipada, ex. pergaminho) vai para o fim.
                    Comparator.comparingInt((CooldownEntry e) -> e.slotIndex() < 0 ? Integer.MAX_VALUE : e.slotIndex())
                            .thenComparing(tieBreaker);
            case NAME ->
                    Comparator.comparing((CooldownEntry e) -> e.displayName().getString()).thenComparing(tieBreaker);
        };
    }
}
