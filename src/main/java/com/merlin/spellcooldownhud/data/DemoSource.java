package com.merlin.spellcooldownhud.data;

import com.merlin.spellcooldownhud.config.ContentMode;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Fake cooldowns that run on their own, for the editor's live preview.
 *
 * <p>This is what lets you tune position, colors and style without being in combat -- or even in a
 * world. Note this class imports nothing from Iron's Spells: it asks {@link
 * IronSpellsSource#previewSamples(int)} for sample spells and just animates the numbers.
 */
public final class DemoSource implements CooldownSource {

    /** Names used only when the registry hasn't answered yet (e.g. Iron's Spells absent). */
    private static final String[] PLACEHOLDER_NAMES = {
            "Fireball", "Ice Spike", "Blood Slash", "Teleport", "Heal", "Lightning Bolt"
    };

    private final long startedAtMs = System.currentTimeMillis();

    @Override
    public List<CooldownEntry> collect(ContentMode mode) {
        // In ALL_EQUIPPED it makes sense to show more entries, some already ready.
        int count = mode == ContentMode.ALL_EQUIPPED ? 6 : 4;

        List<CooldownEntry> samples = IronSpellsSource.previewSamples(count);
        if (samples.isEmpty()) {
            samples = placeholders(count);
        }

        long elapsedTicks = (System.currentTimeMillis() - startedAtMs) / 50L;

        List<CooldownEntry> animated = new ArrayList<>(samples.size());
        for (int i = 0; i < samples.size(); i++) {
            CooldownEntry sample = samples.get(i);
            animated.add(sample.withRemaining(animatedRemaining(sample, i, elapsedTicks)));
        }
        return animated;
    }

    /**
     * Makes the remaining time decay and loop, each entry offset from the previous one, so the
     * preview shows several cooldown stages at once.
     */
    private static int animatedRemaining(CooldownEntry sample, int index, long elapsedTicks) {
        int total = Math.max(1, sample.totalTicks());
        long offset = (long) index * (total / 4L + 5L);
        long position = Math.floorMod(elapsedTicks + offset, total);
        return (int) (total - position);
    }

    private static List<CooldownEntry> placeholders(int count) {
        List<CooldownEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String name = PLACEHOLDER_NAMES[i % PLACEHOLDER_NAMES.length];
            int total = 200 + i * 40;
            entries.add(new CooldownEntry(
                    "preview:" + name.toLowerCase(java.util.Locale.ROOT).replace(' ', '_'),
                    null, // no icon: the renderers draw a colored square instead
                    Component.literal(name),
                    1 + (i % 5),
                    total,
                    total,
                    PLACEHOLDER_COLORS[i % PLACEHOLDER_COLORS.length],
                    i));
        }
        return entries;
    }

    private static final int[] PLACEHOLDER_COLORS = {
            0xE05A2B, 0x4FC3F7, 0xC62828, 0x9C6ADE, 0x66BB6A, 0xFFD54F
    };
}
