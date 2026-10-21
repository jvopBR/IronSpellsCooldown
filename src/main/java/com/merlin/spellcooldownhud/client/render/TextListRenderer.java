package com.merlin.spellcooldownhud.client.render;

import com.merlin.spellcooldownhud.client.CooldownTracker;
import com.merlin.spellcooldownhud.config.HudConfig;
import com.merlin.spellcooldownhud.data.CooldownEntry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Compact text list: spell name on the left, remaining time on the right.
 *
 * <p>The lightest, most discreet style -- handy for those who want the info without icons taking up
 * the screen.
 */
public final class TextListRenderer implements CooldownRenderer {

    public static final TextListRenderer INSTANCE = new TextListRenderer();

    private static final int PADDING_X = 3;
    private static final int PADDING_Y = 1;

    /** Minimum gap between the name and the time, so they don't touch. */
    private static final int COLUMN_GAP = 8;

    /** Gap between the name and the mana cost, when shown. */
    private static final int MANA_GAP = 3;

    private TextListRenderer() {
    }

    @Override
    public int cellWidth(int iconSize, List<CooldownTracker.TrackedEntry> entries) {
        Font font = RenderSupport.font();
        boolean showMana = HudConfig.active.showMana.get();
        int widestName = 0;
        int widestTimer = 0;
        int widestMana = 0;

        for (CooldownTracker.TrackedEntry tracked : entries) {
            CooldownEntry entry = tracked.entry();
            widestName = Math.max(widestName, font.width(entry.displayName().getString()));
            widestTimer = Math.max(widestTimer, font.width(timerText(entry)));
            if (showMana && entry.manaCost() > 0) {
                widestMana = Math.max(widestMana, font.width(String.valueOf(entry.manaCost())));
            }
        }

        int iconWidth = HudConfig.active.showIcon.get() ? lineHeight() + 2 : 0;
        int manaWidth = widestMana > 0 ? MANA_GAP + widestMana : 0;
        return iconWidth + widestName + manaWidth + COLUMN_GAP + widestTimer + PADDING_X * 2;
    }

    @Override
    public int cellHeight(int iconSize) {
        return lineHeight() + PADDING_Y * 2;
    }

    @Override
    public void render(GuiGraphics graphics, CooldownTracker.TrackedEntry tracked,
                       int x, int y, int cellWidth, int iconSize) {
        CooldownEntry entry = tracked.entry();
        float alpha = tracked.alpha();
        Font font = RenderSupport.font();

        graphics.fill(x, y, x + cellWidth, y + cellHeight(iconSize), RenderSupport.backgroundColor(alpha));

        int textY = y + PADDING_Y;
        int cursorX = x + PADDING_X;

        // In text mode the icon is optional and drawn small, the height of a line.
        if (HudConfig.active.showIcon.get()) {
            int small = lineHeight();
            RenderSupport.drawIcon(graphics, entry, cursorX, textY, small, alpha);
            cursorX += small + 2;
        }

        RenderSupport.drawText(graphics, entry.displayName().getString(), cursorX, textY, alpha);

        if (HudConfig.active.showMana.get() && entry.manaCost() > 0) {
            int manaX = cursorX + font.width(entry.displayName().getString()) + MANA_GAP;
            RenderSupport.drawManaText(graphics, String.valueOf(entry.manaCost()), manaX, textY, alpha);
        }

        if (HudConfig.active.showTimer.get()) {
            String timer = timerText(entry);
            RenderSupport.drawText(graphics, timer, x + cellWidth - PADDING_X - font.width(timer), textY, alpha);
        }
    }

    private static String timerText(CooldownEntry entry) {
        return entry.ready()
                ? Component.translatable("hud.spellcooldownhud.ready").getString()
                : RenderSupport.timerText(entry);
    }

    private static int lineHeight() {
        return RenderSupport.font().lineHeight;
    }
}
