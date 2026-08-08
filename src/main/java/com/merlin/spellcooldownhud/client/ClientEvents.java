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

/** Game-bus events: refreshes the tracker and listens for the editor key. */
@EventBusSubscriber(modid = SpellCooldownHud.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    /**
     * Tracker for the live HUD. The editor creates its own, with preview data.
     *
     * <p>The Iron's Spells source is wrapped in {@link ServerSyncedSource} so the displayed cooldown
     * is the server's, not the client's local count, which runs too fast when TPS drops.
     */
    public static final CooldownTracker LIVE =
            new CooldownTracker(new ServerSyncedSource(new IronSpellsSource()));

    private ClientEvents() {
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.level == null) {
            // On leaving the world, reset so we don't come back with stale cooldowns on screen.
            LIVE.clear();
            ServerClock.reset();
            return;
        }

        // Before the refresh: this tick's game time is what anchors the cooldowns.
        ServerClock.tick();
        LIVE.refresh(HudConfig.CONTENT_MODE.get());

        while (Keybinds.OPEN_EDITOR.consumeClick()) {
            // Opens straight to the move screen, the most common action; it has a button to the full editor.
            minecraft.setScreen(new HudMoveScreen(null));
        }
    }
}
