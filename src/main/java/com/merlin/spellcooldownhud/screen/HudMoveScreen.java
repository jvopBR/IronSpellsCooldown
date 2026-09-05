package com.merlin.spellcooldownhud.screen;

import com.merlin.spellcooldownhud.config.Anchor;
import com.merlin.spellcooldownhud.config.HudConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Transparent screen for dragging the HUD with the mouse.
 *
 * <p>No control panel on purpose: in the editor, the left panel takes 192px and blocks clicks,
 * which made it impossible to position the HUD on that side of the screen. Here the whole screen is
 * a drag area.
 *
 * <p>The nine anchors show as discreet marks, with the active one highlighted, so it's visible
 * which corner the HUD will stick to -- and therefore how it will behave at another resolution.
 */
public final class HudMoveScreen extends Screen implements HudPreviewScreen {

    private static final int BUTTON_WIDTH = 110;
    private static final int BUTTON_HEIGHT = 20;
    private static final int MARGIN = 8;

    private static final int NUDGE_STEP = 1;
    private static final int NUDGE_STEP_FAST = 10;

    private static final int GUIDE_SIZE = 5;

    private final @Nullable Screen parent;
    private final HudDragController drag = new HudDragController();

    public HudMoveScreen(@Nullable Screen parent) {
        super(Component.translatable("screen.spellcooldownhud.move"));
        this.parent = parent;
    }

    /** The game keeps running behind: positioning the HUD without seeing the game would be blind. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int y = height - MARGIN - BUTTON_HEIGHT;

        addRenderableWidget(Button.builder(
                Component.translatable("screen.spellcooldownhud.openSettings"),
                b -> minecraft.setScreen(new HudEditorScreen(parent)))
                .bounds(width / 2 - BUTTON_WIDTH - 2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                b -> onClose())
                .bounds(width / 2 + 2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    /**
     * Empty on purpose: this screen is fully transparent.
     *
     * <p>{@code Screen.render} calls {@code renderBackground} before the widgets, and the default
     * applies the vanilla blur plus the menu background -- on top of the HUD preview, since the
     * preview is drawn first. Without neutralizing this, the screen comes out dark and blurred.
     */
    @Override
    //? if >=1.21 {
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    //?} else {
    /*public void renderBackground(GuiGraphics graphics) {
    *///?}
        // no-op
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Outside a world there's nothing to see through, so the default background beats empty.
        // Drawn here, not in renderBackground, so it comes BEFORE the preview instead of covering it.
        if (minecraft != null && minecraft.level == null) {
            //? if >=1.21 {
            super.renderBackground(graphics, mouseX, mouseY, partialTick);
            //?} else {
            /*super.renderBackground(graphics);
            *///?}
        }

        drawAnchorGuides(graphics);
        drag.drawPreview(graphics);
        drag.drawOutline(graphics);

        // Draws the buttons; the renderBackground above is already the transparent no-op.
        super.render(graphics, mouseX, mouseY, partialTick);

        drawHint(graphics);
    }

    private void drawAnchorGuides(GuiGraphics graphics) {
        Anchor active = HudConfig.ANCHOR.get();
        float scale = HudConfig.SCALE.get().floatValue();
        int scaledWidth = Mth.floor(width / scale);
        int scaledHeight = Mth.floor(height / scale);

        for (Anchor anchor : Anchor.values()) {
            int x = Math.round(anchor.anchorX(scaledWidth) * scale);
            int y = Math.round(anchor.anchorY(scaledHeight) * scale);
            int color = anchor == active ? 0xFF8CD98C : 0x50FFFFFF;

            graphics.fill(x - GUIDE_SIZE, y - 1, x + GUIDE_SIZE, y + 1, color);
            graphics.fill(x - 1, y - GUIDE_SIZE, x + 1, y + GUIDE_SIZE, color);
        }
    }

    private void drawHint(GuiGraphics graphics) {
        Component hint = Component.translatable("screen.spellcooldownhud.moveHint");
        graphics.drawString(font, hint, (width - font.width(hint)) / 2, MARGIN, 0xFFFFFFFF, true);

        Component anchorLine = Component.translatable("screen.spellcooldownhud.currentAnchor")
                .append(": ")
                .append(Component.translatable("option.spellcooldownhud.anchor."
                        + HudConfig.ANCHOR.get().name()))
                .append(String.format("  (%d, %d)", HudConfig.OFFSET_X.get(), HudConfig.OFFSET_Y.get()));
        graphics.drawString(font, anchorLine,
                (width - font.width(anchorLine)) / 2, MARGIN + font.lineHeight + 2, 0xFFC8C8D2, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        // minX = 0: the whole screen is grabbable, including the left edge.
        return drag.beginDrag(mouseX, mouseY, 0);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (drag.isDragging()) {
            drag.drag(mouseX, mouseY, width, height);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (drag.isDragging()) {
            drag.endDrag();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int step = hasShiftDown() ? NUDGE_STEP_FAST : NUDGE_STEP;
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> {
                drag.nudge(-step, 0, width, height);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                drag.nudge(step, 0, width, height);
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                drag.nudge(0, -step, width, height);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                drag.nudge(0, step, width, height);
                return true;
            }
            default -> {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }

    @Override
    public void onClose() {
        if (drag.isDirty()) {
            HudConfig.SPEC.save();
            drag.clearDirty();
        }
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
