package com.merlin.spellcooldownhud.client.render;

import com.merlin.spellcooldownhud.client.CooldownTracker;
import com.merlin.spellcooldownhud.config.HudStyle;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * Draws a cooldown entry in a specific style.
 *
 * <p>The cell size is asked for before drawing because {@link
 * com.merlin.spellcooldownhud.client.HudLayout} needs it to lay everything out -- and in the text
 * style it depends on the content, not just the config.
 */
public interface CooldownRenderer {

    /**
     * Width of one cell. Takes the whole list because text styles need to measure the widest entry
     * so the columns line up.
     */
    int cellWidth(int iconSize, List<CooldownTracker.TrackedEntry> entries);

    int cellHeight(int iconSize);

    /**
     * Draws one entry. The cell width comes ready from the layout, not remeasured here, so the
     * drawing uses exactly the same width that positioned the entry.
     */
    void render(GuiGraphics graphics, CooldownTracker.TrackedEntry tracked,
                int x, int y, int cellWidth, int iconSize);

    static CooldownRenderer forStyle(HudStyle style) {
        return switch (style) {
            case RADIAL -> RadialRenderer.INSTANCE;
            case BAR -> BarRenderer.INSTANCE;
            case TEXT_LIST -> TextListRenderer.INSTANCE;
        };
    }
}
