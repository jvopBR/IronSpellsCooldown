package com.merlin.spellcooldownhud.client;

import com.merlin.spellcooldownhud.SpellCooldownHud;
import com.merlin.spellcooldownhud.config.HudConfig;
import com.merlin.spellcooldownhud.data.IronSpellsSource;
import com.merlin.spellcooldownhud.data.ServerSyncedSource;
import com.merlin.spellcooldownhud.screen.HudMoveScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Eventos do barramento do jogo: atualiza o tracker e escuta a tecla do editor. */
@EventBusSubscriber(modid = SpellCooldownHud.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    /**
     * Tracker da HUD real. O editor cria o proprio, com dados de preview.
     *
     * <p>A fonte do Iron's Spells vai embrulhada em {@link ServerSyncedSource} para o cooldown
     * exibido ser o do servidor, e nao a contagem local do cliente, que corre rapido demais
     * quando o TPS cai.
     */
    public static final CooldownTracker LIVE =
            new CooldownTracker(new ServerSyncedSource(new IronSpellsSource()));

    private ClientEvents() {
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.level == null) {
            // Ao sair do mundo, zera para nao voltar com cooldowns velhos na tela.
            LIVE.clear();
            ServerClock.reset();
            return;
        }

        // Antes do refresh: o tempo de jogo deste tick e o que ancora os cooldowns.
        ServerClock.tick();
        LIVE.refresh(HudConfig.CONTENT_MODE.get());

        while (Keybinds.OPEN_EDITOR.consumeClick()) {
            // Abre direto a tela de arrastar, que e a acao mais comum; ela tem botao para o
            // editor completo.
            minecraft.setScreen(new HudMoveScreen(null));
        }
    }
}
