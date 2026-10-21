package com.merlin.spellcooldownhud.client;

import com.merlin.spellcooldownhud.client.render.CooldownRenderer;
import com.merlin.spellcooldownhud.config.HudConfig;
import com.merlin.spellcooldownhud.config.HudProfile;
import com.merlin.spellcooldownhud.screen.HudPreviewScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
// 1.21 registers the HUD as a vanilla LayeredDraw.Layer; Forge 1.20.1 as an IGuiOverlay.
//? if <1.21 {
/*import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
*///?} else {
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.LayeredDraw;
//?}

import java.util.List;

/**
 * The HUD layer itself, registered just above the hotbar.
 *
 * <p>The drawing lives in a static method so the editor can reuse it in the preview: tuning the HUD
 * on a screen that draws something different from the game would be pointless.
 */
//? if <1.21 {
/*public final class HudLayer implements IGuiOverlay {
*///?} else {
public final class HudLayer implements LayeredDraw.Layer {
//?}

    private final CooldownTracker cooldowns;
    private final CooldownTracker effects;

    public HudLayer(CooldownTracker cooldowns, CooldownTracker effects) {
        this.cooldowns = cooldowns;
        this.effects = effects;
    }

    @Override
    //? if <1.21 {
    /*public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
    *///?} else {
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
    //?}
        if (!shouldRender()) {
            return;
        }
        draw(graphics, cooldowns);
        if (HudConfig.SHOW_ACTIVE_EFFECTS.get()) {
            draw(graphics, effects);
        }
    }

    private static boolean shouldRender() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return false;
        }
        // The GuiLayerManager flattens the vanilla groups and wraps EACH vanilla layer in the
        // hideGui check -- mod layers inserted between them don't inherit it. Without this line the
        // HUD would keep showing with F1 on.
        if (HudConfig.HIDE_IN_F1.get() && minecraft.options.hideGui) {
            return false;
        }
        // With the editor or the move screen open, they draw the preview, so we don't draw it twice.
        return !(minecraft.screen instanceof HudPreviewScreen);
    }

    /**
     * Draws the HUD and returns the layout used, which the editor reuses to know where the drag
     * rectangle is.
     */
    public static HudLayout draw(GuiGraphics graphics, CooldownTracker tracker) {
        HudProfile profile = tracker.profile();
        HudConfig.active = profile; // RenderSupport and the renderers read the active profile
        List<CooldownTracker.TrackedEntry> entries = tracker.snapshot();

        CooldownRenderer renderer = CooldownRenderer.forStyle(profile.style.get());
        int iconSize = profile.iconSize.get();
        int cellWidth = renderer.cellWidth(iconSize, entries);
        int cellHeight = renderer.cellHeight(iconSize);

        // The mod's scale multiplies the game's GUI scale. We compute the anchoring in the ALREADY
        // scaled space, otherwise a right-anchored HUD would fall off-screen as the scale grows.
        float scale = profile.scale.get().floatValue();
        int screenWidth = Mth.floor(graphics.guiWidth() / scale);
        int screenHeight = Mth.floor(graphics.guiHeight() / scale);

        HudLayout layout = HudLayout.compute(
                entries.size(), cellWidth, cellHeight,
                profile.spacing.get(), profile.growDirection.get(), profile.maxPerLine.get(),
                profile.anchor.get(), profile.offsetX.get(), profile.offsetY.get(),
                screenWidth, screenHeight);

        if (entries.isEmpty()) {
            return layout;
        }

        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0f);

        for (int i = 0; i < entries.size(); i++) {
            renderer.render(graphics, entries.get(i), layout.cellX(i), layout.cellY(i), cellWidth, iconSize);
        }

        graphics.pose().popPose();
        return layout;
    }
}
