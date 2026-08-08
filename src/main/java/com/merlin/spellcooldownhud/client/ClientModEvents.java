package com.merlin.spellcooldownhud.client;

import com.merlin.spellcooldownhud.SpellCooldownHud;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Mod-bus events: registration of the HUD layer and the key mappings.
 *
 * <p>Wired up by hand in {@link SpellCooldownHud}'s constructor instead of via
 * {@code @EventBusSubscriber(bus = MOD)}, which is deprecated for removal in NeoForge 21.1.
 */
public final class ClientModEvents {

    private ClientModEvents() {
    }

    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        // Above the hotbar and below the chat: the HUD covers neither the conversation nor the items.
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                SpellCooldownHud.id("spell_cooldowns"),
                new HudLayer(ClientEvents.LIVE));
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(Keybinds.OPEN_EDITOR);
    }
}
