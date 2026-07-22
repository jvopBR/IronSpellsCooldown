package com.merlin.spellcooldownhud.client.render;

import com.merlin.spellcooldownhud.client.CooldownTracker;
import com.merlin.spellcooldownhud.config.HudConfig;
import com.merlin.spellcooldownhud.data.CooldownEntry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Lista compacta de texto: nome da magia a esquerda, tempo restante a direita.
 *
 * <p>O estilo mais leve e discreto -- util para quem quer a informacao sem icones ocupando a tela.
 */
public final class TextListRenderer implements CooldownRenderer {

    public static final TextListRenderer INSTANCE = new TextListRenderer();

    private static final int PADDING_X = 3;
    private static final int PADDING_Y = 1;

    /** Espaco minimo entre o nome e o tempo, para nao encostarem um no outro. */
    private static final int COLUMN_GAP = 8;

    private TextListRenderer() {
    }

    @Override
    public int cellWidth(int iconSize, List<CooldownTracker.TrackedEntry> entries) {
        Font font = RenderSupport.font();
        int widestName = 0;
        int widestTimer = 0;

        for (CooldownTracker.TrackedEntry tracked : entries) {
            CooldownEntry entry = tracked.entry();
            widestName = Math.max(widestName, font.width(entry.displayName().getString()));
            widestTimer = Math.max(widestTimer, font.width(timerText(entry)));
        }

        int iconWidth = HudConfig.SHOW_ICON.get() ? lineHeight() + 2 : 0;
        return iconWidth + widestName + COLUMN_GAP + widestTimer + PADDING_X * 2;
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

        // No modo texto o icone e opcional e vai pequeno, do tamanho da linha.
        if (HudConfig.SHOW_ICON.get()) {
            int small = lineHeight();
            RenderSupport.drawIcon(graphics, entry.icon(), cursorX, textY, small, alpha, entry.schoolColor());
            cursorX += small + 2;
        }

        RenderSupport.drawText(graphics, entry.displayName().getString(), cursorX, textY, alpha);

        if (HudConfig.SHOW_TIMER.get()) {
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
