package com.merlin.spellcooldownhud.screen;

import com.merlin.spellcooldownhud.client.ServerClock;
import com.merlin.spellcooldownhud.config.Anchor;
import com.merlin.spellcooldownhud.config.CachedColor;
import com.merlin.spellcooldownhud.config.ContentMode;
import com.merlin.spellcooldownhud.config.GrowDirection;
import com.merlin.spellcooldownhud.config.HudConfig;
import com.merlin.spellcooldownhud.config.HudStyle;
import com.merlin.spellcooldownhud.config.SortMode;
import com.merlin.spellcooldownhud.config.TimerFormat;
import com.merlin.spellcooldownhud.data.DemoSource;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The HUD editor: drag to position, tune everything else in the controls, see the result live.
 *
 * <p>The preview uses {@link DemoSource}, with fictitious cooldowns that run on their own, and draws
 * through the SAME path as the real HUD ({@link HudLayer#draw}). That means two things: you can tune
 * the HUD standing in the lobby, out of combat, and what you see here is exactly what appears in game.
 */
public final class HudEditorScreen extends Screen implements HudPreviewScreen {

    private enum Tab {
        CONTENT, LAYOUT, STYLE, COLORS, EFFECTS
    }

    private static final int PANEL_WIDTH = 192;
    private static final int MARGIN = 8;
    // Compact rows on purpose: the fullest tab has 7 controls and the panel still needs to fit,
    // with tabs and footer, on a ~270px tall screen (GUI scale 4 at 1080p).
    private static final int ROW_HEIGHT = 18;
    private static final int ROW_GAP = 2;
    private static final int TAB_GAP = 4;

    /** Arrow-key nudge step; ten times larger with Shift. */
    private static final int NUDGE_STEP = 1;
    private static final int NUDGE_STEP_FAST = 10;

    private final @Nullable Screen parent;
    private final HudDragController drag = new HudDragController();
    private final List<ColorRow> colorRows = new ArrayList<>();

    private Tab tab = Tab.CONTENT;

    private record ColorRow(String labelKey, EditBox box, int y) {
    }

    public HudEditorScreen(@Nullable Screen parent) {
        super(Component.translatable("screen.spellcooldownhud.editor"));
        this.parent = parent;
    }

    /** The game keeps running behind: the HUD needs tuning in context, not on a dead screen. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ assembly

    @Override
    protected void init() {
        colorRows.clear();

        int y = MARGIN + 14;
        y = buildTabs(y);
        y += ROW_GAP;

        switch (tab) {
            case CONTENT -> buildContentTab(y);
            case LAYOUT -> buildLayoutTab(y);
            case STYLE -> buildStyleTab(y);
            case COLORS -> buildColorsTab(y);
            case EFFECTS -> buildEffectsTab(y);
        }

        buildFooter();
    }

    private int buildTabs(int y) {
        Tab[] tabs = Tab.values();
        int perRow = 3;
        int buttonWidth = (contentWidth() - (perRow - 1) * TAB_GAP) / perRow;

        for (int i = 0; i < tabs.length; i++) {
            Tab candidate = tabs[i];
            int column = i % perRow;
            int row = i / perRow;
            int x = MARGIN + column * (buttonWidth + TAB_GAP);
            int rowY = y + row * (ROW_HEIGHT + ROW_GAP);

            Button button = Button.builder(tabLabel(candidate), b -> {
                this.tab = candidate;
                rebuildWidgets();
            }).bounds(x, rowY, buttonWidth, ROW_HEIGHT).build();

            // The current tab's button is inactive: it's the visual cue for where you are.
            button.active = candidate != tab;
            addRenderableWidget(button);
        }

        int rows = Mth.ceil(tabs.length / (float) perRow);
        return y + rows * (ROW_HEIGHT + ROW_GAP);
    }

    private void buildContentTab(int y) {
        y = addRow(enumButton(y, "option.spellcooldownhud.contentMode",
                HudConfig.CONTENT_MODE, ContentMode.values()));
        y = addRow(enumButton(y, "option.spellcooldownhud.sortMode",
                HudConfig.SORT_MODE, SortMode.values()));
        y = addRow(new IntSlider(y, "option.spellcooldownhud.maxEntries", HudConfig.MAX_ENTRIES, 1, 32));
        y = addRow(toggleButton(y, "option.spellcooldownhud.hideInF1", HudConfig.HIDE_IN_F1));
        addRow(toggleButton(y, "option.spellcooldownhud.useServerTime", HudConfig.USE_SERVER_TIME));
    }

    private void buildLayoutTab(int y) {
        y = addRow(Button.builder(Component.translatable("screen.spellcooldownhud.move"), b -> {
            if (minecraft != null) {
                minecraft.setScreen(new HudMoveScreen(this));
            }
        }).bounds(MARGIN, y, contentWidth(), ROW_HEIGHT).build());

        y = addRow(enumButton(y, "option.spellcooldownhud.anchor", HudConfig.ANCHOR, Anchor.values()));
        y = addRow(enumButton(y, "option.spellcooldownhud.growDirection",
                HudConfig.GROW_DIRECTION, GrowDirection.values()));
        y = addRow(new IntSlider(y, "option.spellcooldownhud.iconSize", HudConfig.ICON_SIZE, 8, 48));
        y = addRow(new IntSlider(y, "option.spellcooldownhud.spacing", HudConfig.SPACING, 0, 24));
        y = addRow(new IntSlider(y, "option.spellcooldownhud.maxPerLine", HudConfig.MAX_PER_LINE, 1, 16));

        addRow(Button.builder(Component.translatable("option.spellcooldownhud.resetPosition"), b -> {
            HudConfig.OFFSET_X.set(0);
            HudConfig.OFFSET_Y.set(0);
            markDirty();
        }).bounds(MARGIN, y, contentWidth(), ROW_HEIGHT).build());
    }

    private void buildStyleTab(int y) {
        y = addRow(enumButton(y, "option.spellcooldownhud.style", HudConfig.STYLE, HudStyle.values()));
        y = addRow(enumButton(y, "option.spellcooldownhud.timerFormat",
                HudConfig.TIMER_FORMAT, TimerFormat.values()));
        y = addRow(toggleButton(y, "option.spellcooldownhud.showIcon", HudConfig.SHOW_ICON));
        y = addRow(toggleButton(y, "option.spellcooldownhud.showName", HudConfig.SHOW_NAME));
        y = addRow(toggleButton(y, "option.spellcooldownhud.showTimer", HudConfig.SHOW_TIMER));
        y = addRow(toggleButton(y, "option.spellcooldownhud.showLevel", HudConfig.SHOW_LEVEL));
        addRow(toggleButton(y, "option.spellcooldownhud.drawBorder", HudConfig.DRAW_BORDER));
    }

    private void buildColorsTab(int y) {
        y = addRow(toggleButton(y, "option.spellcooldownhud.useSchoolColor", HudConfig.USE_SCHOOL_COLOR));

        y = addColorRow(y, "option.spellcooldownhud.backgroundColor", HudConfig.BACKGROUND_COLOR.raw());
        y = addColorRow(y, "option.spellcooldownhud.sweepColor", HudConfig.SWEEP_COLOR.raw());
        y = addColorRow(y, "option.spellcooldownhud.borderColor", HudConfig.BORDER_COLOR.raw());
        y = addColorRow(y, "option.spellcooldownhud.textColor", HudConfig.TEXT_COLOR.raw());
        addColorRow(y, "option.spellcooldownhud.readyFlashColor", HudConfig.READY_FLASH_COLOR.raw());
    }

    private void buildEffectsTab(int y) {
        y = addRow(new DoubleSlider(y, "option.spellcooldownhud.opacity", HudConfig.OPACITY, 0.05, 1.0));
        y = addRow(new DoubleSlider(y, "option.spellcooldownhud.scale", HudConfig.SCALE, 0.25, 3.0));
        y = addRow(new IntSlider(y, "option.spellcooldownhud.fadeIn", HudConfig.FADE_IN_TICKS, 0, 40));
        y = addRow(new IntSlider(y, "option.spellcooldownhud.fadeOut", HudConfig.FADE_OUT_TICKS, 0, 40));
        y = addRow(toggleButton(y, "option.spellcooldownhud.flashWhenReady", HudConfig.FLASH_WHEN_READY));
        addRow(toggleButton(y, "option.spellcooldownhud.dimWhenOnCooldown", HudConfig.DIM_WHEN_READY));
    }

    private void buildFooter() {
        int y = height - MARGIN - ROW_HEIGHT * 2 - ROW_GAP;

        addRenderableWidget(Button.builder(
                Component.translatable("option.spellcooldownhud.resetAll"),
                b -> {
                    resetAll();
                    rebuildWidgets();
                }).bounds(MARGIN, y, contentWidth(), ROW_HEIGHT).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                b -> onClose()).bounds(MARGIN, y + ROW_HEIGHT + ROW_GAP, contentWidth(), ROW_HEIGHT).build());
    }

    // ------------------------------------------------------------------- widgets

    private int addRow(net.minecraft.client.gui.components.AbstractWidget widget) {
        addRenderableWidget(widget);
        return widget.getY() + ROW_HEIGHT + ROW_GAP;
    }

    private int addColorRow(int y, String labelKey, ModConfigSpec.ConfigValue<String> value) {
        int boxWidth = 78;
        int boxX = MARGIN + contentWidth() - boxWidth;

        EditBox box = new EditBox(font, boxX, y, boxWidth, ROW_HEIGHT, Component.translatable(labelKey));
        box.setMaxLength(9);
        box.setValue(value.get());
        box.setResponder(text -> {
            boolean valid = CachedColor.isValid(text);
            // Red while the text isn't valid hex, instead of writing garbage to the config.
            box.setTextColor(valid ? 0xE0E0E0 : 0xFF5555);
            if (valid) {
                value.set(text);
                markDirty();
            }
        });

        addRenderableWidget(box);
        colorRows.add(new ColorRow(labelKey, box, y));
        return y + ROW_HEIGHT + ROW_GAP;
    }

    private <E extends Enum<E>> Button enumButton(int y, String labelKey,
                                                  ModConfigSpec.EnumValue<E> value, E[] options) {
        return Button.builder(enumLabel(labelKey, value.get()), b -> {
            E next = options[(value.get().ordinal() + 1) % options.length];
            value.set(next);
            b.setMessage(enumLabel(labelKey, next));
            markDirty();
        }).bounds(MARGIN, y, contentWidth(), ROW_HEIGHT).build();
    }

    private Button toggleButton(int y, String labelKey, ModConfigSpec.BooleanValue value) {
        return Button.builder(boolLabel(labelKey, value.get()), b -> {
            boolean next = !value.get();
            value.set(next);
            b.setMessage(boolLabel(labelKey, next));
            markDirty();
        }).bounds(MARGIN, y, contentWidth(), ROW_HEIGHT).build();
    }

    private final class IntSlider extends AbstractSliderButton {
        private final ModConfigSpec.IntValue config;
        private final String labelKey;
        private final int min;
        private final int max;

        private IntSlider(int y, String labelKey, ModConfigSpec.IntValue config, int min, int max) {
            super(MARGIN, y, contentWidth(), ROW_HEIGHT, Component.empty(),
                    (Mth.clamp(config.get(), min, max) - min) / (double) (max - min));
            this.config = config;
            this.labelKey = labelKey;
            this.min = min;
            this.max = max;
            updateMessage();
        }

        private int current() {
            return min + (int) Math.round(value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(labelKey).append(": " + current()));
        }

        @Override
        protected void applyValue() {
            config.set(current());
            markDirty();
        }
    }

    private final class DoubleSlider extends AbstractSliderButton {
        private final ModConfigSpec.DoubleValue config;
        private final String labelKey;
        private final double min;
        private final double max;

        private DoubleSlider(int y, String labelKey, ModConfigSpec.DoubleValue config,
                             double min, double max) {
            super(MARGIN, y, contentWidth(), ROW_HEIGHT, Component.empty(),
                    (Mth.clamp(config.get(), min, max) - min) / (max - min));
            this.config = config;
            this.labelKey = labelKey;
            this.min = min;
            this.max = max;
            updateMessage();
        }

        private double current() {
            // Rounded to two decimals: the TOML value stays readable instead of 0.7300000000000001.
            return Math.round((min + value * (max - min)) * 100.0) / 100.0;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(labelKey)
                    .append(String.format(Locale.ROOT, ": %.2f", current())));
        }

        @Override
        protected void applyValue() {
            config.set(current());
            markDirty();
        }
    }

    // -------------------------------------------------------------------- render

    /**
     * Empty on purpose.
     *
     * <p>{@code Screen.render} would call the default before the widgets, applying the vanilla blur
     * and the menu background over the HUD preview -- adding to the veil we already draw. This
     * screen's background is drawn in {@link #render}, in the right order.
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // no-op
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Outside a world there's nothing behind; inside one, just a light veil so the HUD can be
        // positioned relative to what's actually on screen.
        if (minecraft != null && minecraft.level == null) {
            super.renderBackground(graphics, mouseX, mouseY, partialTick);
        } else {
            graphics.fill(0, 0, width, height, 0x55000000);
        }

        drag.drawPreview(graphics);
        drag.drawOutline(graphics);
        drawPanelBackground(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawString(font, title, MARGIN, MARGIN, 0xFFFFFFFF, true);
        drawColorLabels(graphics);
        drawHint(graphics);
    }

    private void drawPanelBackground(GuiGraphics graphics) {
        graphics.fill(0, 0, PANEL_WIDTH, height, 0xD0101014);
        graphics.fill(PANEL_WIDTH, 0, PANEL_WIDTH + 1, height, 0xFF3C3C46);
    }

    private void drawColorLabels(GuiGraphics graphics) {
        for (ColorRow row : colorRows) {
            int textY = row.y() + (ROW_HEIGHT - font.lineHeight) / 2;
            graphics.drawString(font, Component.translatable(row.labelKey()), MARGIN, textY, 0xFFC8C8D2, false);
            drawColorSwatch(graphics, row);
        }
    }

    /**
     * Color swatch next to the field. Without it you could write black on a black background without
     * noticing -- the hex alone says nothing about readability.
     */
    private void drawColorSwatch(GuiGraphics graphics, ColorRow row) {
        int size = ROW_HEIGHT - 6;
        int x = row.box().getX() - size - 4;
        int y = row.y() + 3;

        graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF000000);
        // Light/dark checkerboard behind: that way a low-alpha color reveals itself as transparent
        // instead of looking like just some solid color.
        graphics.fill(x, y, x + size, y + size, 0xFFFFFFFF);
        graphics.fill(x, y, x + size / 2, y + size / 2, 0xFF808080);
        graphics.fill(x + size / 2, y + size / 2, x + size, y + size, 0xFF808080);

        graphics.fill(x, y, x + size, y + size, CachedColor.parse(row.box().getValue(), 0));
    }

    private void drawHint(GuiGraphics graphics) {
        int x = PANEL_WIDTH + MARGIN;
        int y = height - MARGIN - font.lineHeight;

        graphics.drawString(font, Component.translatable("screen.spellcooldownhud.hint"),
                x, y, 0xFFC8C8D2, true);

        // Measured TPS, only when the server is below nominal: it's the evidence the time correction
        // is acting, and helps diagnose "my cooldown looks weird".
        if (ServerClock.isReliable() && ServerClock.tps() < 19.5f) {
            Component tps = Component.translatable("screen.spellcooldownhud.serverTps")
                    .append(String.format(Locale.ROOT, ": %.1f", ServerClock.tps()));
            graphics.drawString(font, tps, x, y - font.lineHeight - 2, 0xFFFFC864, true);
        }
    }

    // --------------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        // The panel is opaque, so clicks on it don't become drags. To position the HUD on the left
        // side of the screen there's the move screen, which has no panel.
        return drag.beginDrag(mouseX, mouseY, PANEL_WIDTH);
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
        // Arrow-key nudge, as long as the focus isn't in a text field.
        if (!(getFocused() instanceof EditBox)) {
            int step = hasShiftDown() ? NUDGE_STEP_FAST : NUDGE_STEP;
            switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT -> {
                    nudge(-step, 0);
                    return true;
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    nudge(step, 0);
                    return true;
                }
                case GLFW.GLFW_KEY_UP -> {
                    nudge(0, -step);
                    return true;
                }
                case GLFW.GLFW_KEY_DOWN -> {
                    nudge(0, step);
                    return true;
                }
                default -> {
                    // falls through to default handling
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void nudge(int deltaX, int deltaY) {
        drag.nudge(deltaX, deltaY, width, height);
    }

    // ------------------------------------------------------------------ lifecycle

    private void resetAll() {
        List<ModConfigSpec.ConfigValue<?>> values = List.of(
                HudConfig.CONTENT_MODE, HudConfig.SORT_MODE, HudConfig.MAX_ENTRIES,
                HudConfig.HIDE_IN_F1, HudConfig.USE_SERVER_TIME,
                HudConfig.ANCHOR, HudConfig.OFFSET_X, HudConfig.OFFSET_Y, HudConfig.GROW_DIRECTION,
                HudConfig.ICON_SIZE, HudConfig.SPACING, HudConfig.MAX_PER_LINE,
                HudConfig.STYLE, HudConfig.SHOW_ICON, HudConfig.SHOW_NAME, HudConfig.SHOW_TIMER,
                HudConfig.TIMER_FORMAT, HudConfig.SHOW_LEVEL, HudConfig.DRAW_BORDER,
                HudConfig.USE_SCHOOL_COLOR,
                HudConfig.BACKGROUND_COLOR.raw(), HudConfig.SWEEP_COLOR.raw(),
                HudConfig.BORDER_COLOR.raw(), HudConfig.TEXT_COLOR.raw(),
                HudConfig.READY_FLASH_COLOR.raw(),
                HudConfig.OPACITY, HudConfig.SCALE, HudConfig.FADE_IN_TICKS, HudConfig.FADE_OUT_TICKS,
                HudConfig.FLASH_WHEN_READY, HudConfig.DIM_WHEN_READY);

        for (ModConfigSpec.ConfigValue<?> value : values) {
            restoreDefault(value);
        }
        markDirty();
    }

    /** Helper just to capture the ConfigValue's type parameter in the set call. */
    private static <T> void restoreDefault(ModConfigSpec.ConfigValue<T> value) {
        value.set(value.getDefault());
    }

    private void markDirty() {
        drag.markDirty();
    }

    @Override
    public void onClose() {
        // Save once on close, not on every slider tick, so we don't rewrite the TOML dozens of
        // times during a drag.
        if (drag.isDirty()) {
            HudConfig.SPEC.save();
            drag.clearDirty();
        }
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    // ------------------------------------------------------------------- helpers

    private static int contentWidth() {
        return PANEL_WIDTH - MARGIN * 2;
    }

    private static Component tabLabel(Tab tab) {
        return Component.translatable("screen.spellcooldownhud.tab." + tab.name().toLowerCase(Locale.ROOT));
    }

    private static Component enumLabel(String labelKey, Enum<?> value) {
        return Component.translatable(labelKey).append(": ").append(enumValueLabel(value));
    }

    private static Component enumValueLabel(Enum<?> value) {
        String type = value.getDeclaringClass().getSimpleName().toLowerCase(Locale.ROOT);
        return Component.translatable("option.spellcooldownhud." + type + "." + value.name());
    }

    private static Component boolLabel(String labelKey, boolean value) {
        return Component.translatable(labelKey).append(": ")
                .append(Component.translatable(value
                        ? "option.spellcooldownhud.on"
                        : "option.spellcooldownhud.off"));
    }
}
