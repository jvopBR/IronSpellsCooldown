package com.merlin.spellcooldownhud.data;

import com.merlin.spellcooldownhud.config.ContentMode;

import java.util.List;

/**
 * Where the HUD gets the spells to show.
 *
 * <p>There are two implementations: {@link IronSpellsSource}, which reads the real game, and
 * {@link DemoSource}, which produces fake cooldowns for the editor preview. Isolating this behind
 * an interface is what lets you tune the HUD without being in combat -- and, later, support another
 * spell mod without touching any renderer.
 */
public interface CooldownSource {

    /** This frame's entries, unsorted and uncapped -- the tracker handles that. */
    List<CooldownEntry> collect(ContentMode mode);
}
