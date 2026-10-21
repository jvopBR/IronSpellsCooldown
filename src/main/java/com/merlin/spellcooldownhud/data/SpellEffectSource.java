package com.merlin.spellcooldownhud.data;

import com.merlin.spellcooldownhud.config.ContentMode;
import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Active Iron's Spells effects on the player (Charge, Angel Wings, Haste, Oakskin, ...) with their
 * remaining duration, for the {@link ContentMode#ACTIVE_EFFECTS} mode.
 *
 * <p>Like {@link IronSpellsSource}, this is a boundary class: it reads the Iron's Spells / Minecraft
 * effect API and hands back plain {@link CooldownEntry}s. The duration comes from the vanilla
 * {@link MobEffectInstance} (already synced to the client), so no networking or mixin is needed.
 *
 * <p>It only reports effects that are {@code MagicMobEffect} -- Iron's Spells' own -- so vanilla
 * potions and other mods' effects stay out. These are the effects ON the player: self-buffs and
 * debuffs cast on you, not the duration of something you applied to an enemy (that lives on them).
 */
public final class SpellEffectSource implements CooldownSource {

    private static final int BENEFICIAL_TINT = 0x66BB6A;
    private static final int HARMFUL_TINT = 0xC65A5A;
    private static final int NEUTRAL_TINT = 0x8890C0;

    /**
     * Peak duration seen per effect, used as the "total" so the bar/sweep fills proportionally --
     * {@link MobEffectInstance} only exposes the remaining time, not the initial duration.
     */
    private final Map<String, Integer> peakDuration = new HashMap<>();

    @Override
    public List<CooldownEntry> collect(ContentMode mode) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            peakDuration.clear();
            return List.of();
        }

        List<CooldownEntry> entries = new ArrayList<>();
        Map<String, Integer> stillActive = new HashMap<>();

        int slot = 0;
        for (MobEffectInstance instance : player.getActiveEffects()) {
            MobEffect effect = effectValue(instance);
            if (!(effect instanceof MagicMobEffect)) {
                continue;
            }

            String id = effect.getDescriptionId();
            int remaining = Math.max(0, instance.getDuration());
            int peak = Math.max(peakDuration.getOrDefault(id, 0), remaining);
            stillActive.put(id, peak);

            // Amplifier 0 (level I) draws no badge; II and up show the number, like vanilla.
            int amplifier = instance.getAmplifier();
            int level = amplifier > 0 ? amplifier + 1 : 0;

            entries.add(new CooldownEntry(
                    id,
                    null,
                    effect.getDisplayName(),
                    level,
                    0,
                    remaining,
                    peak,
                    tintFor(effect.getCategory()),
                    slot++,
                    instance.getEffect())); // opaque handle, drawn as the effect sprite by RenderSupport
        }

        // Forget effects that ended, so a re-applied one starts a fresh "total".
        peakDuration.keySet().retainAll(stillActive.keySet());
        peakDuration.putAll(stillActive);
        return entries;
    }

    private static int tintFor(MobEffectCategory category) {
        return switch (category) {
            case BENEFICIAL -> BENEFICIAL_TINT;
            case HARMFUL -> HARMFUL_TINT;
            default -> NEUTRAL_TINT;
        };
    }

    // MobEffectInstance.getEffect() returns Holder<MobEffect> on 1.21 and MobEffect on 1.20.1.
    //? if >=1.21 {
    private static MobEffect effectValue(MobEffectInstance instance) {
        return instance.getEffect().value();
    }
    //?} else {
    /*private static MobEffect effectValue(MobEffectInstance instance) {
        return instance.getEffect();
    }
    *///?}
}
