package com.merlin.spellcooldownhud.screen;

import com.merlin.spellcooldownhud.client.ServerClock;
import com.merlin.spellcooldownhud.config.Anchor;
import com.merlin.spellcooldownhud.config.CachedColor;
import com.merlin.spellcooldownhud.config.ContentMode;
import com.merlin.spellcooldownhud.config.GrowDirection;
import com.merlin.spellcooldownhud.config.HudConfig;
import com.merlin.spellcooldownhud.config.HudProfile;
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
//? if <1.21 {
/*import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
*///?} else {
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
//?}
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
    private final List<ColorRow> colorRows = new ArrayList<>();

    private Tab tab = Tab.CONTENT;

    /** Which HUD the editor is tuning: false = cooldowns, true = active effects. */
    private boolean editingEffects;
    // Not final: it's recreated when the group switches, so its preview tracks the selected HUD.
    private HudDragController drag = new HudDragController(false);

    private record ColorRow(String labelKey, EditBox box, int y) {
    }

    public HudEditorScreen(@Nullable Screen parent) {
        super(Component.translatable("screen.spellcooldownhud.editor"));
        this.parent = parent;
    }

    /** The profile being edited -- every per-HUD control reads and writes through here. */
    private HudProfile profile() {
        return editingEffects ? HudConfig.EFFECTS : HudConfig.COOLDOWNS;
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

        // The effects HUD can be turned off from the content tab; don't stay stuck editing a hidden one.
        if (editingEffects && !HudConfig.SHOW_ACTIVE_EFFECTS.get()) {
            editingEffects = false;
            drag = new HudDragController(false);
        }

        int y = MARGIN + 14;
        y = buildGroupSwitch(y);
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

    /**
     * Group selector at the top: which of the two HUDs this editor is tuning. Only shown when the
     * effects HUD exists; otherwise there's only one HUD and nothing to switch between.
     */
    private int buildGroupSwitch(int y) {
        if (!HudConfig.SHOW_ACTIVE_EFFECTS.get()) {
            return y;
        }
        addRenderableWidget(Button.builder(groupSwitchLabel(), b -> {
            editingEffects = !editingEffects;
            drag = new HudDragController(editingEffects);
            rebuildWidgets();
        }).bounds(MARGIN, y, contentWidth(), ROW_HEIGHT).build());
        return y + ROW_HEIGHT + ROW_GAP;
    }

    private Component groupSwitchLabel() {
        String groupKey = editingEffects
                ? "screen.spellcooldownhud.groupEffects" : "screen.spellcooldownhud.groupCooldowns";
        return Component.translatable("screen.spellcooldownhud.editingGroup")
                .append(": ").append(Component.translatable(groupKey));
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
        // contentMode picks WHAT the cooldown HUD lists; the effects HUD always lists active effects.
        if (!editingEffects) {
            y = addRow(enumButton(y, "option.spellcooldownhud.contentMode",
                    HudConfig.CONTENT_MODE, ContentMode.values()));
        }
        y = addRow(enumButton(y, "option.spellcooldownhud.sortMode",
                profile().sortMode, SortMode.values()));
        y = addRow(new IntSlider(y, "option.spellcooldownhud.maxEntries", profile().maxEntries, 1, 32));

        // The rest are shared/global switches; they live on the cooldowns tab so they appear once.
        if (!editingEffects) {
            y = addRow(toggleButton(y, "option.spellcooldownhud.hideInF1", HudConfig.HIDE_IN_F1));
            y = addRow(toggleButton(y, "option.spellcooldownhud.useServerTime", HudConfig.USE_SERVER_TIME));
            // Rebuilds the screen so the group switch above appears/disappears with this toggle.
            addRenderableWidget(Button.builder(
                    boolLabel("option.spellcooldownhud.showActiveEffects", HudConfig.SHOW_ACTIVE_EFFECTS.get()),
                    b -> {
                        HudConfig.SHOW_ACTIVE_EFFECTS.set(!HudConfig.SHOW_ACTIVE_EFFECTS.get());
                        markDirty();
                        rebuildWidgets();
                    }).bounds(MARGIN, y, contentWidth(), ROW_HEIGHT).build());
        }
    }

    private void buildLayoutTab(int y) {
        y = addRow(Button.builder(Component.translatable("screen.spellcooldownhud.move"), b -> {
            if (minecraft != null) {
                minecraft.setScreen(new HudMoveScreen(this));
            }
        }).bounds(MARGIN, y, contentWidth(), ROW_HEIGHT).build());

        y = addRow(enumButton(y, "option.spellcooldownhud.anchor", profile().anchor, Anchor.values()));
        y = addRow(enumButton(y, "option.spellcooldownhud.growDirection",
                profile().growDirection, GrowDirection.values()));
        y = addRow(new IntSlider(y, "option.spellcooldownhud.iconSize", profile().iconSize, 8, 48));
        y = addRow(new IntSlider(y, "option.spellcooldownhud.spacing", profile().spacing, 0, 24));
        y = addRow(new IntSlider(y, "option.spellcooldownhud.maxPerLine", profile().maxPerLine, 1, 16));

        addRow(Button.builder(Component.translatable("option.spellcooldownhud.resetPosition"), b -> {
            profile().offsetX.set(0);
            profile().offsetY.set(0);
            markDirty();
        }).bounds(MARGIN, y, contentWidth(), ROW_HEIGHT).build());
    }

    private void buildStyleTab(int y) {
        y = addRow(enumButton(y, "option.spellcooldownhud.style", profile().style, HudStyle.values()));
        y = addRow(enumButton(y, "option.spellcooldownhud.timerFormat",
                profile().timerFormat, TimerFormat.values()));
        y = addRow(toggleButton(y, "option.spellcooldownhud.showIcon", profile().showIcon));
        y = addRow(toggleButton(y, "option.spellcooldownhud.showName", profile().showName));
        y = addRow(toggleButton(y, "option.spellcooldownhud.showTimer", profile().showTimer));
        y = addRow(toggleButton(y, "option.spellcooldownhud.showLevel", profile().showLevel));
        y = addRow(toggleButton(y, "option.spellcooldownhud.showManaCost", profile().showMana));
        addRow(toggleButton(y, "option.spellcooldownhud.drawBorder", profile().drawBorder));
    }

    private void buildColorsTab(int y) {
        y = addRow(toggleButton(y, "option.spellcooldownhud.useSchoolColor", profile().useSchoolColor));

        y = addColorRow(y, "option.spellcooldownhud.backgroundColor", profile().backgroundColor.raw());
        y = addColorRow(y, "option.spellcooldownhud.sweepColor", profile().sweepColor.raw());
        y = addColorRow(y, "option.spellcooldownhud.borderColor", profile().borderColor.raw());
        y = addColorRow(y, "option.spellcooldownhud.textColor", profile().textColor.raw());
        addColorRow(y, "option.spellcooldownhud.readyFlashColor", profile().readyFlashColor.raw());
    }

    private void buildEffectsTab(int y) {
        y = addRow(new DoubleSlider(y, "option.spellcooldownhud.opacity", profile().opacity, 0.05, 1.0));
        y = addRow(new DoubleSlider(y, "option.spellcooldownhud.scale", profile().scale, 0.25, 3.0));
        y = addRow(new IntSlider(y, "option.spellcooldownhud.fadeIn", profile().fadeInTicks, 0, 40));
        y = addRow(new IntSlider(y, "option.spellcooldownhud.fadeOut", profile().fadeOutTicks, 0, 40));
        y = addRow(toggleButton(y, "option.spellcooldownhud.flashWhenReady", profile().flashWhenReady));
        addRow(toggleButton(y, "option.spellcooldownhud.dimWhenOnCooldown", profile().dimWhenReady));
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

    private int addColorRow(int y, String labelKey, ConfigValue<String> value) {
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
                                                  EnumValue<E> value, E[] options) {
        return Button.builder(enumLabel(labelKey, value.get()), b -> {
            E next = options[(value.get().ordinal() + 1) % options.length];
            value.set(next);
            b.setMessage(enumLabel(labelKey, next));
            markDirty();
        }).bounds(MARGIN, y, contentWidth(), ROW_HEIGHT).build();
    }

    private Button toggleButton(int y, String labelKey, BooleanValue value) {
        return Button.builder(boolLabel(labelKey, value.get()), b -> {
            boolean next = !value.get();
            value.set(next);
            b.setMessage(boolLabel(labelKey, next));
            markDirty();
        }).bounds(MARGIN, y, contentWidth(), ROW_HEIGHT).build();
    }

    private final class IntSlider extends AbstractSliderButton {
        private final IntValue config;
        private final String labelKey;
        private final int min;
        private final int max;

        private IntSlider(int y, String labelKey, IntValue config, int min, int max) {
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
        private final DoubleValue config;
        private final String labelKey;
        private final double min;
        private final double max;

        private DoubleSlider(int y, String labelKey, DoubleValue config,
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
    //? if >=1.21 {
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    //?} else {
    /*public void renderBackground(GuiGraphics graphics) {
    *///?}
        // no-op
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Outside a world there's nothing behind; inside one, just a light veil so the HUD can be
        // positioned relative to what's actually on screen.
        if (minecraft != null && minecraft.level == null) {
            //? if >=1.21 {
            super.renderBackground(graphics, mouseX, mouseY, partialTick);
            //?} else {
            /*super.renderBackground(graphics);
            *///?}
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
        // The selected HUD's own look -- the only thing "Reset all" touches when editing effects.
        List<ConfigValue<?>> values = new ArrayList<>(List.of(
                profile().sortMode, profile().maxEntries,
                profile().anchor, profile().offsetX, profile().offsetY, profile().growDirection,
                profile().iconSize, profile().spacing, profile().maxPerLine,
                profile().style, profile().showIcon, profile().showName, profile().showTimer,
                profile().timerFormat, profile().showLevel, profile().showMana, profile().drawBorder,
                profile().useSchoolColor,
                profile().backgroundColor.raw(), profile().sweepColor.raw(),
                profile().borderColor.raw(), profile().textColor.raw(),
                profile().readyFlashColor.raw(),
                profile().opacity, profile().scale, profile().fadeInTicks, profile().fadeOutTicks,
                profile().flashWhenReady, profile().dimWhenReady));

        // The shared/global switches belong to the cooldowns tab, so only reset them from there.
        if (!editingEffects) {
            values.add(HudConfig.CONTENT_MODE);
            values.add(HudConfig.HIDE_IN_F1);
            values.add(HudConfig.USE_SERVER_TIME);
            values.add(HudConfig.SHOW_ACTIVE_EFFECTS);
        }

        for (ConfigValue<?> value : values) {
            restoreDefault(value);
        }
        markDirty();
    }

    /** Helper just to capture the ConfigValue's type parameter in the set call. */
    private static <T> void restoreDefault(ConfigValue<T> value) {
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
