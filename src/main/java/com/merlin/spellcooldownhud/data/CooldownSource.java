package com.merlin.spellcooldownhud.data;

import com.merlin.spellcooldownhud.config.ContentMode;

import java.util.List;

/**
 * Where the HUD gets the spells to show.
 *
 * <p>Implementations: {@link IronSpellsSource} (spell cooldowns) and {@link SpellEffectSource}
 * (active effect durations) read the real game; {@link ServerSyncedSource} decorates the cooldowns;
 * {@link DemoSource} produces fake data for the editor preview. Isolating this behind
 * an interface is what lets you tune the HUD without being in combat -- and, later, support another
 * spell mod without touching any renderer.
 */
public interface CooldownSource {

    /** This frame's entries, unsorted and uncapped -- the tracker handles that. */
    List<CooldownEntry> collect(ContentMode mode);
}
