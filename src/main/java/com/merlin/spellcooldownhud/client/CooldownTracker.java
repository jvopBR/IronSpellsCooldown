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
 * Keeps the entries' animation state between ticks.
 *
 * <p>It exists because the data source isn't enough on its own: in {@code ONLY_ON_COOLDOWN} mode
 * Iron's Spells removes the spell from the map the instant it becomes ready, so without our own
 * state the icon would vanish from one frame to the next, with no fade and no "ready" flash.
 *
 * <p>Times are measured on the wall clock, not in ticks, so the fade stays smooth at any framerate
 * -- 20 ticks/s would give a grainy animation on a 144 Hz monitor.
 *
 * <p>Not thread-safe on purpose: client tick and render run on the same thread.
 */
public final class CooldownTracker {

    /** Duration of the "became ready" flash. Short on purpose: it's a flash, not an animation. */
    private static final long READY_FLASH_MS = 400L;

    private static final long MS_PER_TICK = 50L;

    private final CooldownSource source;
    private final Map<String, Tracked> tracked = new LinkedHashMap<>();

    public CooldownTracker(CooldownSource source) {
        this.source = source;
    }

    /** An entry ready to draw: the data plus the resolved animation state. */
    public record TrackedEntry(CooldownEntry entry, float alpha, float readyFlash) {
    }

    private static final class Tracked {
        private CooldownEntry entry;
        private final long firstSeenMs;
        /** When the spell stopped appearing in the source; 0 while present. */
        private long goneSinceMs;
        /** When it became ready, for the flash; 0 if it hasn't yet. */
        private long becameReadyMs;

        private Tracked(CooldownEntry entry, long nowMs) {
            this.entry = entry;
            this.firstSeenMs = nowMs;
        }
    }

    public void clear() {
        tracked.clear();
    }

    /** Called every client tick (or per frame, in the editor preview). */
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

            // The "on cooldown" -> "ready" transition triggers the flash, even if the entry stays
            // in the list (the ALL_EQUIPPED case).
            if (!existing.entry.ready() && fresh.ready()) {
                existing.becameReadyMs = now;
            }
            existing.entry = fresh;
            existing.goneSinceMs = 0L;
        }

        // What the source stopped reporting: mark the exit and keep it alive during the fade-out.
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
                    // Gone from the cooldown map: in Iron's Spells that means it became ready.
                    candidate.becameReadyMs = now;
                }
            }
            // With fadeOutMs = 0 the condition holds immediately, which is exactly "no fade".
            if (now - candidate.goneSinceMs >= fadeOutMs) {
                iterator.remove();
            }
        }
    }

    /** Entries to draw this frame, already sorted, capped and with alpha resolved. */
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
        // spellId as the tiebreaker in every mode: without it, tied entries would swap places
        // between frames and the HUD would jitter.
        Comparator<CooldownEntry> tieBreaker = Comparator.comparing(CooldownEntry::spellId);

        return switch (mode) {
            case TIME_REMAINING_ASC ->
                    Comparator.comparingInt(CooldownEntry::remainingTicks).thenComparing(tieBreaker);
            case TIME_REMAINING_DESC ->
                    Comparator.comparingInt(CooldownEntry::remainingTicks).reversed().thenComparing(tieBreaker);
            case SLOT_ORDER ->
                    // slotIndex -1 (unequipped spell, e.g. scroll) goes to the end.
                    Comparator.comparingInt((CooldownEntry e) -> e.slotIndex() < 0 ? Integer.MAX_VALUE : e.slotIndex())
                            .thenComparing(tieBreaker);
            case NAME ->
                    Comparator.comparing((CooldownEntry e) -> e.displayName().getString()).thenComparing(tieBreaker);
        };
    }
}
