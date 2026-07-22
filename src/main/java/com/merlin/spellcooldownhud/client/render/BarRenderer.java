package com.merlin.spellcooldownhud.client.render;

import com.merlin.spellcooldownhud.client.CooldownTracker;
import com.merlin.spellcooldownhud.config.HudConfig;
import com.merlin.spellcooldownhud.data.CooldownEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Icone com barra de progresso horizontal embaixo.
 *
 * <p>Combina melhor com a estetica retangular do Minecraft do que a varredura circular, e a barra
 * e mais facil de acompanhar de canto de olho em cooldowns longos.
 */
public final class BarRenderer implements CooldownRenderer {

    public static final BarRenderer INSTANCE = new BarRenderer();

    private static final int BAR_HEIGHT = 3;
    private static final int BAR_GAP = 1;

    /** Quanto o icone escurece enquanto a magia esta em cooldown, no modo ALL_EQUIPPED. */
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
            // Sem varredura por cima, o escurecimento e o que diferencia "em cooldown" de "pronta".
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

    /** Trilho escuro com a porcao restante preenchida na cor da escola (ou da borda). */
    private static void drawBar(GuiGraphics graphics, CooldownEntry entry,
                                int x, int y, int width, float alpha) {
        graphics.fill(x, y, x + width, y + BAR_HEIGHT, RenderSupport.sweepColor(alpha));

        float fraction = entry.remainingFraction();
        if (fraction <= 0.0f) {
            return;
        }
        // Pelo menos 1px enquanto houver cooldown, para a barra nao "piscar" para nada no fim.
        int filled = Math.max(1, Mth.ceil(width * fraction));
        graphics.fill(x, y, x + Math.min(width, filled), y + BAR_HEIGHT,
                RenderSupport.borderColorFor(entry, alpha));
    }
}
