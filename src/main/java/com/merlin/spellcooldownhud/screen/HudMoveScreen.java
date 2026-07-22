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
 * Tela transparente para arrastar a HUD com o mouse.
 *
 * <p>Sem painel de controles de proposito: no editor, o painel da esquerda ocupa 192px e bloqueia
 * cliques, o que tornava impossivel posicionar a HUD naquele lado da tela. Aqui a tela inteira e
 * area de arrasto.
 *
 * <p>As nove ancoras aparecem como marcas discretas, com a ativa destacada, para ficar visivel a
 * que canto a HUD vai se prender -- e portanto como ela vai se comportar em outra resolucao.
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

    /** O jogo segue rodando atras: posicionar a HUD sem ver o jogo seria posicionar as cegas. */
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
     * Vazio de proposito: esta tela e totalmente transparente.
     *
     * <p>{@code Screen.render} chama {@code renderBackground} antes dos widgets, e o padrao aplica
     * o blur do vanilla mais o fundo de menu -- por cima do preview da HUD, ja que o preview e
     * desenhado antes. Sem neutralizar isto, a tela sai escura e borrada.
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // no-op
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Fora de um mundo nao ha nada para ver atraves, entao o fundo padrao e melhor que vazio.
        // Desenhado aqui, e nao no renderBackground, para ficar ANTES do preview em vez de cobri-lo.
        if (minecraft != null && minecraft.level == null) {
            super.renderBackground(graphics, mouseX, mouseY, partialTick);
        }

        drawAnchorGuides(graphics);
        drag.drawPreview(graphics);
        drag.drawOutline(graphics);

        // Desenha os botoes; o renderBackground acima ja e o no-op transparente.
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
        // minX = 0: a tela inteira e agarravel, inclusive a borda esquerda.
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
