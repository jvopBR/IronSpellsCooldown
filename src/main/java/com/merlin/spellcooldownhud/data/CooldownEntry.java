package com.merlin.spellcooldownhud.data;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A spell to draw on the HUD, already translated into plain Minecraft types.
 *
 * <p>No Iron's Spells type appears here on purpose: {@link IronSpellsSource} is the only class that
 * talks to that API, and it hands back this record. That way the rest of the mod (tracker, layout,
 * renderers, editor) doesn't depend on Iron's Spells and can run on synthetic data in the editor
 * preview.
 *
 * @param spellId        registry id, e.g. {@code irons_spellbooks:fireball}
 * @param icon           icon texture, already a full path, or null if the spell has none
 * @param displayName    translated name
 * @param level          spell level; 0 when unknown
 * @param remainingTicks ticks left until ready; 0 = ready
 * @param totalTicks     total cooldown duration, to compute the fraction
 * @param schoolColor    RGB of the spell's school (no alpha)
 * @param slotIndex      position in the spellbook, for stable ordering; -1 if unknown
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

    /** 1.0 right after the cast, 0.0 when ready -- the fraction the sweep/bar covers. */
    public float remainingFraction() {
        if (totalTicks <= 0) {
            return 0.0f;
        }
        return Mth.clamp((float) remainingTicks / (float) totalTicks, 0.0f, 1.0f);
    }

    /**
     * Copy with a different remaining time.
     *
     * <p>Used by the editor preview to animate without real data, and by {@link ServerSyncedSource}
     * to replace the client's local count with the server's.
     */
    public CooldownEntry withRemaining(int ticks) {
        return new CooldownEntry(spellId, icon, displayName, level, ticks, totalTicks, schoolColor, slotIndex);
    }
}
