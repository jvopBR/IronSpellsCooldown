package com.merlin.spellcooldownhud;

import com.merlin.spellcooldownhud.client.ClientModEvents;
import com.merlin.spellcooldownhud.config.HudConfig;
import com.merlin.spellcooldownhud.screen.HudEditorScreen;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;

/**
 * HUD de cooldown de magias do Iron's Spells 'n Spellbooks.
 *
 * <p>Inteiramente client-side: le o estado que o Iron's Spells ja sincroniza para o cliente
 * ({@code ClientMagicData}) e o desenha. Nao registra nenhum payload de rede, o que o mantem fora
 * da negociacao de mods do NeoForge -- por isso da para usa-lo num servidor que nao o tem
 * instalado, que e justamente o caso de uso.
 */
@Mod(value = SpellCooldownHud.MODID, dist = Dist.CLIENT)
public final class SpellCooldownHud {

    public static final String MODID = "spellcooldownhud";

    public static final Logger LOGGER = LogUtils.getLogger();

    public SpellCooldownHud(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, HudConfig.SPEC);

        modEventBus.addListener(ClientModEvents::onRegisterGuiLayers);
        modEventBus.addListener(ClientModEvents::onRegisterKeyMappings);

        // Mods > Spell Cooldown HUD > Config abre o editor com preview, e nao a tela generica de
        // config do NeoForge: ajustar posicao e cores numa lista de valores seria as cegas.
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> new HudEditorScreen(parent));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
