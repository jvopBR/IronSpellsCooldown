package com.merlin.spellcooldownhud.client.render;

import com.merlin.spellcooldownhud.client.CooldownTracker;
import com.merlin.spellcooldownhud.config.HudConfig;
import com.merlin.spellcooldownhud.data.CooldownEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Icon with a horizontal progress bar below it.
 *
 * <p>Fits Minecraft's rectangular look better than the circular sweep, and the bar is easier to
 * track out of the corner of your eye on long cooldowns.
 */
public final class BarRenderer implements CooldownRenderer {

    public static final BarRenderer INSTANCE = new BarRenderer();

    private static final int BAR_HEIGHT = 3;
    private static final int BAR_GAP = 1;

    /** How much the icon dims while the spell is on cooldown, in ALL_EQUIPPED mode. */
    private static final float DIM_STRENGTH = 0.55f;

    private BarRenderer() {
    }

    @Override
    public int cellWidth(int iconSize, List<CooldownTracker.TrackedEntry> entries) {
        if (!HudConfig.SHOW_NAME.get()) {
            return iconSize;
        }
        int widest = 0;
        for (CooldownTracker.TrackedEntry tracked : entries) {
            widest = Math.max(widest, RenderSupport.font().width(tracked.entry().displayName().getString()));
        }
        return iconSize + 4 + widest;
    }

    @Override
    public int cellHeight(int iconSize) {
        return iconSize + BAR_GAP + BAR_HEIGHT;
    }

    @Override
    public void render(GuiGraphics graphics, CooldownTracker.TrackedEntry tracked,
                       int x, int y, int cellWidth, int iconSize) {
        CooldownEntry entry = tracked.entry();
        float alpha = tracked.alpha();

        graphics.fill(x, y, x + iconSize, y + iconSize, RenderSupport.backgroundColor(alpha));

        if (HudConfig.SHOW_ICON.get()) {
            // With no sweep on top, the dimming is what tells "on cooldown" from "ready".
            float iconAlpha = HudConfig.DIM_WHEN_READY.get() && !entry.ready()
                    ? alpha * (1.0f - DIM_STRENGTH)
                    : alpha;
            RenderSupport.drawIcon(graphics, entry.icon(), x, y, iconSize, iconAlpha, entry.schoolColor());
        }

        RenderSupport.drawReadyFlash(graphics, x, y, iconSize, tracked.readyFlash() * alpha);

        if (HudConfig.DRAW_BORDER.get()) {
            RenderSupport.drawBorder(graphics, x, y, iconSize, iconSize,
                    RenderSupport.borderColorFor(entry, alpha));
        }

        drawBar(graphics, entry, x, y + iconSize + BAR_GAP, iconSize, alpha);

        RenderSupport.drawOverlays(graphics, entry, x, y, iconSize, alpha);

        if (HudConfig.SHOW_NAME.get()) {
            int textY = y + (iconSize - RenderSupport.font().lineHeight) / 2;
            RenderSupport.drawText(graphics, entry.displayName().getString(), x + iconSize + 4, textY, alpha);
        }
    }

    /** Dark rail with the remaining portion filled in the school color (or the border color). */
    private static void drawBar(GuiGraphics graphics, CooldownEntry entry,
                                int x, int y, int width, float alpha) {
        graphics.fill(x, y, x + width, y + BAR_HEIGHT, RenderSupport.sweepColor(alpha));

        float fraction = entry.remainingFraction();
        if (fraction <= 0.0f) {
            return;
        }
        // At least 1px while there's any cooldown, so the bar doesn't "blink" to nothing at the end.
        int filled = Math.max(1, Mth.ceil(width * fraction));
        graphics.fill(x, y, x + Math.min(width, filled), y + BAR_HEIGHT,
                RenderSupport.borderColorFor(entry, alpha));
    }
}
