package com.merlin.spellcooldownhud.client.render;

import com.merlin.spellcooldownhud.client.CooldownTracker;
import com.merlin.spellcooldownhud.config.HudConfig;
import com.merlin.spellcooldownhud.data.CooldownEntry;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * Icon with a dark circular sweep on top and the remaining time in the center.
 *
 * <p>The most at-a-glance readable style: you can estimate how much is left without reading the number.
 */
public final class RadialRenderer implements CooldownRenderer {

    public static final RadialRenderer INSTANCE = new RadialRenderer();

    private RadialRenderer() {
    }

    @Override
    public int cellWidth(int iconSize, List<CooldownTracker.TrackedEntry> entries) {
        if (!HudConfig.SHOW_NAME.get()) {
            return iconSize;
        }
        // With names, every cell uses the widest name's width so the columns align.
        int widest = 0;
        for (CooldownTracker.TrackedEntry tracked : entries) {
            widest = Math.max(widest, RenderSupport.font().width(tracked.entry().displayName().getString()));
        }
        return iconSize + 4 + widest;
    }

    @Override
    public int cellHeight(int iconSize) {
        return iconSize;
    }

    @Override
    public void render(GuiGraphics graphics, CooldownTracker.TrackedEntry tracked,
                       int x, int y, int cellWidth, int iconSize) {
        CooldownEntry entry = tracked.entry();
        float alpha = tracked.alpha();

        graphics.fill(x, y, x + iconSize, y + iconSize, RenderSupport.backgroundColor(alpha));

        if (HudConfig.SHOW_ICON.get()) {
            RenderSupport.drawIcon(graphics, entry.icon(), x, y, iconSize, alpha, entry.schoolColor());
        }

        // The sweep covers the part still remaining, shrinking as the cooldown runs.
        RenderSupport.drawRadialSweep(graphics, x, y, iconSize,
                entry.remainingFraction(), RenderSupport.sweepColor(alpha));

        RenderSupport.drawReadyFlash(graphics, x, y, iconSize, tracked.readyFlash() * alpha);

        if (HudConfig.DRAW_BORDER.get()) {
            RenderSupport.drawBorder(graphics, x, y, iconSize, iconSize,
                    RenderSupport.borderColorFor(entry, alpha));
        }

        RenderSupport.drawOverlays(graphics, entry, x, y, iconSize, alpha);

        if (HudConfig.SHOW_NAME.get()) {
            int textY = y + (iconSize - RenderSupport.font().lineHeight) / 2;
            RenderSupport.drawText(graphics, entry.displayName().getString(), x + iconSize + 4, textY, alpha);
        }
    }
}
