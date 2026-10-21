package com.merlin.spellcooldownhud.client;

import com.merlin.spellcooldownhud.SpellCooldownHud;
import com.merlin.spellcooldownhud.config.HudConfig;
import com.merlin.spellcooldownhud.data.IronSpellsSource;
import com.merlin.spellcooldownhud.data.ServerSyncedSource;
import com.merlin.spellcooldownhud.data.SpellEffectSource;
import com.merlin.spellcooldownhud.screen.HudMoveScreen;
import net.minecraft.client.Minecraft;
//? if <1.21 {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
*///?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
//?}

/** Game-bus events: refreshes the tracker and listens for the editor key. */
//? if <1.21 {
/*@Mod.EventBusSubscriber(modid = SpellCooldownHud.MODID, value = Dist.CLIENT)
*///?} else {
@EventBusSubscriber(modid = SpellCooldownHud.MODID, value = Dist.CLIENT)
//?}
public final class ClientEvents {

    /**
     * The two live HUD groups, each with its own position. The editor uses its own trackers with
     * preview data.
     *
     * <p>Cooldowns are wrapped in {@link ServerSyncedSource} so the displayed time is the server's,
     * not the client's local count, which runs too fast when TPS drops. Effects read their remaining
     * duration straight from the vanilla {@link SpellEffectSource}.
     */
    public static final CooldownTracker LIVE_COOLDOWNS =
            new CooldownTracker(new ServerSyncedSource(new IronSpellsSource()), HudConfig.COOLDOWNS);

    public static final CooldownTracker LIVE_EFFECTS =
            new CooldownTracker(new SpellEffectSource(), HudConfig.EFFECTS);

    private ClientEvents() {
    }

    @SubscribeEvent
    //? if <1.21 {
    /*static void onClientTick(TickEvent.ClientTickEvent event) {
        // Forge fires this at both START and END of the tick; NeoForge's .Post is END only.
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
    *///?} else {
    static void onClientTick(ClientTickEvent.Post event) {
    //?}
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.level == null) {
            // On leaving the world, reset so we don't come back with stale cooldowns on screen.
            LIVE_COOLDOWNS.clear();
            LIVE_EFFECTS.clear();
            ServerClock.reset();
            return;
        }

        // Before the refresh: this tick's game time is what anchors the cooldowns.
        ServerClock.tick();
        LIVE_COOLDOWNS.refresh(HudConfig.CONTENT_MODE.get());
        LIVE_EFFECTS.refresh(HudConfig.CONTENT_MODE.get());

        while (Keybinds.OPEN_EDITOR.consumeClick()) {
            // Opens straight to the move screen, the most common action; it has a button to the full editor.
            minecraft.setScreen(new HudMoveScreen(null));
        }
    }
}
