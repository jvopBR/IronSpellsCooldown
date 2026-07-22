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
 * A UNICA classe do mod que fala com a API do Iron's Spells 'n Spellbooks.
 *
 * <p>Todo o resto trabalha com {@link CooldownEntry}, que so usa tipos do Minecraft. Manter esta
 * fronteira num arquivo so significa que uma mudanca de API do Iron's Spells quebra a compilacao
 * aqui e em nenhum outro lugar.
 *
 * <p>Nao ha rede envolvida: o Iron's Spells ja sincroniza os cooldowns para o cliente e os expoe
 * via {@link ClientMagicData}. Este mod so le esse estado.
 */
public final class IronSpellsSource implements CooldownSource {

    /** Usada quando a magia nao tem escola (ex. magias de addon sem escola declarada). */
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
     * Le direto o mapa de cooldowns. Pega qualquer magia em cooldown, inclusive as lancadas por
     * pergaminho ou item que nao estao equipadas no momento.
     */
    private static List<CooldownEntry> fromActiveCooldowns(LocalPlayer player,
                                                           Map<String, CooldownInstance> active) {
        if (active.isEmpty()) {
            return List.of();
        }

        // O mapa de cooldowns guarda so o id, sem o nivel. Buscamos o nivel nas magias equipadas
        // para o badge ficar correto; quando a magia nao estiver equipada, fica 0 (nao desenhado).
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

    /** Percorre as magias equipadas e casa cada uma com seu cooldown, se houver. */
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
            // Sem cooldown ativo nao ha duracao gravada; usamos a da magia para a fracao fazer sentido.
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
            // A mesma magia pode estar em dois slots com niveis diferentes; o maior manda.
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
     * Ja vem no caminho completo ({@code <namespace>:textures/gui/spell_icons/<nome>.png}), inclusive
     * para magias de addon -- e por isso que as magias customizadas do servidor funcionam sem
     * nenhum tratamento especial.
     */
    private static ResourceLocation iconOf(AbstractSpell spell) {
        try {
            return spell.getSpellIconResource();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * O parametro Player existe porque algumas magias variam o nome conforme quem olha. Fora do
     * mundo (preview aberto pelo menu de mods) nao ha player, entao caimos no id da magia.
     */
    private static Component displayNameOf(AbstractSpell spell, LocalPlayer player) {
        if (player != null) {
            try {
                return spell.getDisplayName(player);
            } catch (RuntimeException ignored) {
                // cai no fallback abaixo
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
     * Magias reais do registry com tempos ficticios, para o preview do editor.
     *
     * <p>Fica aqui, e nao no {@link DemoSource}, para nao furar a regra de que so esta classe
     * importa {@code io.redspace}. O preview usa icones de verdade, entao o que voce ajusta e
     * exatamente o que vai ver em jogo.
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
            // Cooldowns escalonados para o preview mostrar varios estagios de varredura de uma vez.
            int total = 200 + i * 40;
            int remaining = (int) (total * (1.0f - (i / (float) Math.max(1, count))));
            samples.add(toEntry(player, spell, 1 + (i % 5), remaining, total, i));
        }
        return samples;
    }
}
