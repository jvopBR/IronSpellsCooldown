package com.merlin.spellcooldownhud.screen;

import com.merlin.spellcooldownhud.client.CooldownTracker;
import com.merlin.spellcooldownhud.client.HudLayer;
import com.merlin.spellcooldownhud.client.HudLayout;
import com.merlin.spellcooldownhud.config.Anchor;
import com.merlin.spellcooldownhud.config.HudConfig;
import com.merlin.spellcooldownhud.config.HudProfile;
import com.merlin.spellcooldownhud.data.DemoSource;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * HUD preview and dragging, shared by the editor and the move screen.
 *
 * <p>In a single class on purpose: two drag implementations would end up diverging, and the same
 * mouse action would position the HUD differently depending on the screen.
 */
public final class HudDragController {

    private final CooldownTracker preview;

    /** Which group this controller positions: false = cooldowns HUD, true = active-effects HUD. */
    private final boolean effects;

    private boolean dragging;
    private double grabOffsetX;
    private double grabOffsetY;
    private boolean dirty;

    private @Nullable HudLayout lastLayout;

    public HudDragController(boolean effects) {
        this.effects = effects;
        this.preview = new CooldownTracker(new DemoSource(), profile());
    }

    /** This group's profile -- it carries its own anchor/offset (and the rest of the look). */
    private HudProfile profile() {
        return effects ? HudConfig.EFFECTS : HudConfig.COOLDOWNS;
    }

    private Anchor anchor() {
        return profile().anchor.get();
    }

    private int offsetX() {
        return profile().offsetX.get();
    }

    private int offsetY() {
        return profile().offsetY.get();
    }

    private void setAnchor(Anchor a) {
        profile().anchor.set(a);
    }

    private void setOffsetX(int x) {
        profile().offsetX.set(x);
    }

    private void setOffsetY(int y) {
        profile().offsetY.set(y);
    }

    /** This group's current anchor/offset, for the move screen's guides and hint. */
    public Anchor currentAnchor() {
        return anchor();
    }

    public int currentOffsetX() {
        return offsetX();
    }

    public int currentOffsetY() {
        return offsetY();
    }

    public float currentScale() {
        return scale();
    }

    /** Draws the preview with fake data and stores the resulting layout. */
    public void drawPreview(GuiGraphics graphics) {
        preview.refresh(HudConfig.CONTENT_MODE.get());
        lastLayout = HudLayer.draw(graphics, preview);
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
     * Tries to grab the HUD.
     *
     * @param minX usable left edge; the editor passes the panel width so clicks on it don't become
     *             drags. The move screen passes 0, which is why there you can take the HUD all the
     *             way to the left edge.
     * @return true if the drag started
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
     * Arrow-key nudge. Goes through the same path as dragging so it inherits the on-screen clamp
     * and the re-anchoring, instead of adding to the offset externally and diverging from the mouse.
     */
    public void nudge(int deltaX, int deltaY, int screenWidth, int screenHeight) {
        if (lastLayout == null) {
            return;
        }
        applyOrigin(lastLayout.originX() + deltaX, lastLayout.originY() + deltaY, screenWidth, screenHeight);
    }

    /**
     * Converts the dragged position back into anchor + offset, inverting exactly the math of
     * {@link HudLayout#compute}.
     */
    private void applyOrigin(double originX, double originY, int screenWidth, int screenHeight) {
        float scale = scale();
        int scaledWidth = Mth.floor(screenWidth / scale);
        int scaledHeight = Mth.floor(screenHeight / scale);

        int blockWidth = lastLayout.width();
        int blockHeight = lastLayout.height();

        // Clamped to the screen: the grabbable area is the block itself, so letting it leave would lose it.
        double clampedX = Mth.clamp(originX, 0.0, Math.max(0.0, scaledWidth - (double) blockWidth));
        double clampedY = Mth.clamp(originY, 0.0, Math.max(0.0, scaledHeight - (double) blockHeight));

        int centerX = (int) Math.round(clampedX + blockWidth / 2.0);
        int centerY = (int) Math.round(clampedY + blockHeight / 2.0);
        Anchor anchor = Anchor.nearest(centerX, centerY, scaledWidth, scaledHeight);

        setAnchor(anchor);
        setOffsetX((int) Math.round(
                clampedX - (anchor.anchorX(scaledWidth) + anchor.alignX(blockWidth))));
        setOffsetY((int) Math.round(
                clampedY - (anchor.anchorY(scaledHeight) + anchor.alignY(blockHeight))));
        dirty = true;
    }

    /** Outline of the grabbable area. */
    public void drawOutline(GuiGraphics graphics) {
        if (lastLayout == null) {
            return;
        }
        float scale = scale();
        int x0 = Math.round(lastLayout.originX() * scale) - 2;
        int y0 = Math.round(lastLayout.originY() * scale) - 2;
        int x1 = Math.round((lastLayout.originX() + lastLayout.width()) * scale) + 2;
        int y1 = Math.round((lastLayout.originY() + lastLayout.height()) * scale) + 2;

        int color = outlineColor();
        graphics.fill(x0, y0, x1, y0 + 1, color);
        graphics.fill(x0, y1 - 1, x1, y1, color);
        graphics.fill(x0, y0, x0 + 1, y1, color);
        graphics.fill(x1 - 1, y0, x1, y1, color);
    }

    // Cooldowns = blue, effects = amber, so the two groups read apart; brighter while dragging.
    private int outlineColor() {
        if (dragging) {
            return effects ? 0xFFFFC266 : 0xFF8CD98C;
        }
        return effects ? 0xA0FFB24D : 0x9078A0FF;
    }

    /** A small label centered above the group's outline, in its color, so you know which is which. */
    public void drawLabel(GuiGraphics graphics, Font font, Component label) {
        if (lastLayout == null) {
            return;
        }
        float scale = scale();
        int originX = Math.round(lastLayout.originX() * scale);
        int blockWidth = Math.round(lastLayout.width() * scale);
        int textX = originX + (blockWidth - font.width(label)) / 2;
        int textY = Math.round(lastLayout.originY() * scale) - font.lineHeight - 3;
        int color = effects ? 0xFFFFC266 : 0xFFAFC0FF;
        graphics.drawString(font, label, textX, Math.max(1, textY), color, true);
    }

    private float scale() {
        return profile().scale.get().floatValue();
    }
}
