package com.merlin.spellcooldownhud.client;

import com.merlin.spellcooldownhud.SpellCooldownHud;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Eventos do barramento do mod: registro da camada de HUD e das teclas.
 *
 * <p>Sao ligados a mao no construtor de {@link SpellCooldownHud} em vez de por
 * {@code @EventBusSubscriber(bus = MOD)}, que esta deprecado para remocao no NeoForge 21.1.
 */
public final class ClientModEvents {

    private ClientModEvents() {
    }

    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        // Acima da hotbar e abaixo do chat: a HUD nao cobre a conversa nem some atras dos itens.
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                SpellCooldownHud.id("spell_cooldowns"),
                new HudLayer(ClientEvents.LIVE));
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(Keybinds.OPEN_EDITOR);
    }
}
