package com.merlin.spellcooldownhud.data;

import com.merlin.spellcooldownhud.config.ContentMode;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.CooldownInstance;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerCooldowns;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The ONLY class in the mod that talks to the Iron's Spells 'n Spellbooks API.
 *
 * <p>Everything else works with {@link CooldownEntry}, which uses only Minecraft types. Keeping
 * this boundary in a single file means an Iron's Spells API change breaks compilation here and
 * nowhere else.
 *
 * <p>No networking involved: Iron's Spells already syncs the cooldowns to the client and exposes
 * them via {@link ClientMagicData}. This mod only reads that state.
 */
public final class IronSpellsSource implements CooldownSource {

    /** Used when the spell has no school (e.g. addon spells with no declared school). */
    private static final int DEFAULT_SCHOOL_COLOR = 0xB0B0C0;

    @Override
    public List<CooldownEntry> collect(ContentMode mode) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return List.of();
        }

        PlayerCooldowns cooldowns = ClientMagicData.getCooldowns();
        if (cooldowns == null) {
            return List.of();
        }
        Map<String, CooldownInstance> active = cooldowns.getSpellCooldowns();

        return switch (mode) {
            case ONLY_ON_COOLDOWN -> fromActiveCooldowns(player, active);
            case ALL_EQUIPPED -> fromEquippedSpells(player, active);
        };
    }

    /**
     * Reads the cooldown map directly. Picks up any spell on cooldown, including ones cast from a
     * scroll or item that aren't currently equipped.
     */
    private static List<CooldownEntry> fromActiveCooldowns(LocalPlayer player,
                                                           Map<String, CooldownInstance> active) {
        if (active.isEmpty()) {
            return List.of();
        }

        // The cooldown map holds only the id, not the level. We look the level up in the equipped
        // spells so the badge is correct; when the spell isn't equipped it stays 0 (not drawn).
        Map<String, Integer> levels = equippedLevels();

        List<CooldownEntry> entries = new ArrayList<>(active.size());
        for (Map.Entry<String, CooldownInstance> mapping : active.entrySet()) {
            AbstractSpell spell = resolve(mapping.getKey());
            if (spell == null) {
                continue;
            }
            CooldownInstance cooldown = mapping.getValue();
            entries.add(toEntry(
                    player,
                    spell,
                    levels.getOrDefault(mapping.getKey(), 0),
                    cooldown.getCooldownRemaining(),
                    cooldown.getSpellCooldown(),
                    -1));
        }
        return entries;
    }

    /** Walks the equipped spells and matches each with its cooldown, if any. */
    private static List<CooldownEntry> fromEquippedSpells(LocalPlayer player,
                                                          Map<String, CooldownInstance> active) {
        SpellSelectionManager manager = ClientMagicData.getSpellSelectionManager();
        if (manager == null) {
            return List.of();
        }

        List<SpellSelectionManager.SelectionOption> options = manager.getAllSpells();
        List<CooldownEntry> entries = new ArrayList<>(options.size());

        for (SpellSelectionManager.SelectionOption option : options) {
            SpellData data = option.spellData;
            if (data == null) {
                continue;
            }
            AbstractSpell spell = data.getSpell();
            if (spell == null || isNone(spell)) {
                continue;
            }

            CooldownInstance cooldown = active.get(spell.getSpellId());
            int remaining = cooldown == null ? 0 : cooldown.getCooldownRemaining();
            // With no active cooldown there's no recorded duration; use the spell's so the fraction makes sense.
            int total = cooldown == null ? spell.getSpellCooldown() : cooldown.getSpellCooldown();

            entries.add(toEntry(player, spell, data.getLevel(), remaining, total, option.globalIndex));
        }
        return entries;
    }

    private static CooldownEntry toEntry(LocalPlayer player, AbstractSpell spell, int level,
                                         int remainingTicks, int totalTicks, int slotIndex) {
        return new CooldownEntry(
                spell.getSpellId(),
                iconOf(spell),
                displayNameOf(spell, player),
                level,
                Math.max(0, remainingTicks),
                Math.max(0, totalTicks),
                schoolColorOf(spell),
                slotIndex);
    }

    private static Map<String, Integer> equippedLevels() {
        SpellSelectionManager manager = ClientMagicData.getSpellSelectionManager();
        if (manager == null) {
            return Map.of();
        }

        Map<String, Integer> levels = new HashMap<>();
        for (SpellSelectionManager.SelectionOption option : manager.getAllSpells()) {
            SpellData data = option.spellData;
            if (data == null) {
                continue;
            }
            AbstractSpell spell = data.getSpell();
            if (spell == null || isNone(spell)) {
                continue;
            }
            // The same spell can be in two slots at different levels; the higher one wins.
            levels.merge(spell.getSpellId(), data.getLevel(), Math::max);
        }
        return levels;
    }

    private static AbstractSpell resolve(String spellId) {
        AbstractSpell spell = SpellRegistry.getSpell(spellId);
        return spell == null || isNone(spell) ? null : spell;
    }

    private static boolean isNone(AbstractSpell spell) {
        return spell == SpellRegistry.none();
    }

    /**
     * Comes back as a full path ({@code <namespace>:textures/gui/spell_icons/<name>.png}), including
     * for addon spells -- which is why the server's custom spells work with no special handling.
     */
    private static ResourceLocation iconOf(AbstractSpell spell) {
        try {
            return spell.getSpellIconResource();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * The Player parameter exists because some spells vary their name by who's looking. Outside a
     * world (preview opened from the mods menu) there's no player, so we fall back to the spell id.
     */
    private static Component displayNameOf(AbstractSpell spell, LocalPlayer player) {
        if (player != null) {
            try {
                return spell.getDisplayName(player);
            } catch (RuntimeException ignored) {
                // falls through to the fallback below
            }
        }
        String id = spell.getSpellId();
        int separator = id.indexOf(':');
        return Component.literal(separator < 0 ? id : id.substring(separator + 1));
    }

    private static int schoolColorOf(AbstractSpell spell) {
        SchoolType school;
        try {
            school = spell.getSchoolType();
        } catch (RuntimeException e) {
            return DEFAULT_SCHOOL_COLOR;
        }
        if (school == null) {
            return DEFAULT_SCHOOL_COLOR;
        }

        Vector3f color = school.getTargetingColor();
        if (color == null) {
            return DEFAULT_SCHOOL_COLOR;
        }
        int r = Mth.clamp(Math.round(color.x * 255.0f), 0, 255);
        int g = Mth.clamp(Math.round(color.y * 255.0f), 0, 255);
        int b = Mth.clamp(Math.round(color.z * 255.0f), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    /**
     * Real registry spells with fictitious times, for the editor preview.
     *
     * <p>It lives here, not in {@link DemoSource}, so as not to break the rule that only this class
     * imports {@code io.redspace}. The preview uses real icons, so what you tune is exactly what
     * you'll see in game.
     */
    public static List<CooldownEntry> previewSamples(int count) {
        List<AbstractSpell> spells;
        try {
            spells = SpellRegistry.getEnabledSpells();
        } catch (RuntimeException e) {
            return List.of();
        }
        if (spells == null || spells.isEmpty()) {
            return List.of();
        }

        LocalPlayer player = Minecraft.getInstance().player;
        List<CooldownEntry> samples = new ArrayList<>(count);

        for (int i = 0; i < Math.min(count, spells.size()); i++) {
            AbstractSpell spell = spells.get(i);
            if (spell == null || isNone(spell)) {
                continue;
            }
            // Staggered cooldowns so the preview shows several sweep stages at once.
            int total = 200 + i * 40;
            int remaining = (int) (total * (1.0f - (i / (float) Math.max(1, count))));
            samples.add(toEntry(player, spell, 1 + (i % 5), remaining, total, i));
        }
        return samples;
    }
}
