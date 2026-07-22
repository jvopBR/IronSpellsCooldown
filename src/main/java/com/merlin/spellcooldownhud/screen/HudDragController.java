package com.merlin.spellcooldownhud.screen;

import com.merlin.spellcooldownhud.client.CooldownTracker;
import com.merlin.spellcooldownhud.client.HudLayer;
import com.merlin.spellcooldownhud.client.HudLayout;
import com.merlin.spellcooldownhud.config.Anchor;
import com.merlin.spellcooldownhud.config.HudConfig;
import com.merlin.spellcooldownhud.data.DemoSource;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * Preview e arrasto da HUD, compartilhados pelo editor e pela tela de mover.
 *
 * <p>Fica numa classe so de proposito: duas implementacoes de arrasto acabariam divergindo, e a
 * mesma acao do mouse passaria a posicionar a HUD em lugares diferentes dependendo da tela.
 */
public final class HudDragController {

    private final CooldownTracker preview = new CooldownTracker(new DemoSource());

    private boolean dragging;
    private double grabOffsetX;
    private double grabOffsetY;
    private boolean dirty;

    private @Nullable HudLayout lastLayout;

    /** Desenha o preview com dados ficticios e guarda o layout resultante. */
    public void drawPreview(GuiGraphics graphics) {
        preview.refresh(HudConfig.CONTENT_MODE.get());
        lastLayout = HudLayer.draw(graphics, preview, HudConfig.CONTENT_MODE.get());
    }

    public @Nullable HudLayout layout() {
        return lastLayout;
    }

    public boolean isDragging() {
        return dragging;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        dirty = true;
    }

    public void clearDirty() {
        dirty = false;
    }

    /**
     * Tenta agarrar a HUD.
     *
     * @param minX borda esquerda utilizavel; o editor passa a largura do painel para cliques sobre
     *             ele nao virarem arrasto. A tela de mover passa 0, e por isso nela da para levar
     *             a HUD ate a borda esquerda.
     * @return true se o arrasto comecou
     */
    public boolean beginDrag(double mouseX, double mouseY, int minX) {
        if (lastLayout == null || mouseX < minX) {
            return false;
        }
        float scale = scale();
        double scaledX = mouseX / scale;
        double scaledY = mouseY / scale;

        if (!lastLayout.contains(scaledX, scaledY)) {
            return false;
        }
        dragging = true;
        grabOffsetX = scaledX - lastLayout.originX();
        grabOffsetY = scaledY - lastLayout.originY();
        return true;
    }

    public void drag(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        if (!dragging || lastLayout == null) {
            return;
        }
        float scale = scale();
        applyOrigin(mouseX / scale - grabOffsetX, mouseY / scale - grabOffsetY, screenWidth, screenHeight);
    }

    public void endDrag() {
        dragging = false;
    }

    /**
     * Ajuste fino pelas setas. Passa pelo mesmo caminho do arrasto para herdar o travamento na
     * tela e a re-ancoragem, em vez de somar no offset por fora e divergir do mouse.
     */
    public void nudge(int deltaX, int deltaY, int screenWidth, int screenHeight) {
        if (lastLayout == null) {
            return;
        }
        applyOrigin(lastLayout.originX() + deltaX, lastLayout.originY() + deltaY, screenWidth, screenHeight);
    }

    /**
     * Converte a posicao arrastada de volta em ancora + offset, invertendo exatamente o calculo de
     * {@link HudLayout#compute}.
     */
    private void applyOrigin(double originX, double originY, int screenWidth, int screenHeight) {
        float scale = scale();
        int scaledWidth = Mth.floor(screenWidth / scale);
        int scaledHeight = Mth.floor(screenHeight / scale);

        int blockWidth = lastLayout.width();
        int blockHeight = lastLayout.height();

        // Preso a tela: a area agarravel e o proprio bloco, entao deixa-lo sair seria perde-lo.
        double clampedX = Mth.clamp(originX, 0.0, Math.max(0.0, scaledWidth - (double) blockWidth));
        double clampedY = Mth.clamp(originY, 0.0, Math.max(0.0, scaledHeight - (double) blockHeight));

        int centerX = (int) Math.round(clampedX + blockWidth / 2.0);
        int centerY = (int) Math.round(clampedY + blockHeight / 2.0);
        Anchor anchor = Anchor.nearest(centerX, centerY, scaledWidth, scaledHeight);

        HudConfig.ANCHOR.set(anchor);
        HudConfig.OFFSET_X.set((int) Math.round(
                clampedX - (anchor.anchorX(scaledWidth) + anchor.alignX(blockWidth))));
        HudConfig.OFFSET_Y.set((int) Math.round(
                clampedY - (anchor.anchorY(scaledHeight) + anchor.alignY(blockHeight))));
        dirty = true;
    }

    /** Contorno da area agarravel. */
    public void drawOutline(GuiGraphics graphics) {
        if (lastLayout == null) {
            return;
        }
        float scale = scale();
        int x0 = Math.round(lastLayout.originX() * scale) - 2;
        int y0 = Math.round(lastLayout.originY() * scale) - 2;
        int x1 = Math.round((lastLayout.originX() + lastLayout.width()) * scale) + 2;
        int y1 = Math.round((lastLayout.originY() + lastLayout.height()) * scale) + 2;

        int color = dragging ? 0xFF8CD98C : 0x9078A0FF;
        graphics.fill(x0, y0, x1, y0 + 1, color);
        graphics.fill(x0, y1 - 1, x1, y1, color);
        graphics.fill(x0, y0, x0 + 1, y1, color);
        graphics.fill(x1 - 1, y0, x1, y1, color);
    }

    private static float scale() {
        return HudConfig.SCALE.get().floatValue();
    }
}
