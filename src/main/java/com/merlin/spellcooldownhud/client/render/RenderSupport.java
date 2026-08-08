package com.merlin.spellcooldownhud.client.render;

import com.merlin.spellcooldownhud.client.ServerClock;
import com.merlin.spellcooldownhud.config.CachedColor;
import com.merlin.spellcooldownhud.config.HudConfig;
import com.merlin.spellcooldownhud.data.CooldownEntry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/** Drawing primitives shared by the three HUD styles. */
public final class RenderSupport {

    /** Iron's Spells (and addon) spell icons are always 16x16. */
    public static final int SPELL_ICON_TEXTURE_SIZE = 16;

    /** Segments of a full circle in the radial sweep. */
    private static final int SWEEP_SEGMENTS = 72;

    private static final float TWO_PI = (float) (Math.PI * 2.0);

    private RenderSupport() {
    }

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    /**
     * Remaining time, already formatted, converting ticks to seconds by the server's REAL rate.
     *
     * <p>Dividing by a fixed 20 would show less time than the spell will actually take on a lagging
     * server. The remaining ticks already come corrected by
     * {@link com.merlin.spellcooldownhud.data.ServerSyncedSource}; only the conversion is left here.
     */
    public static String timerText(CooldownEntry entry) {
        return HudConfig.TIMER_FORMAT.get().format(entry.remainingTicks() / effectiveTps());
    }

    /**
     * Floor of 1 TPS: with the server almost stopped the division would blow the number up on
     * screen, and "a lot of time" is already conveyed well before that.
     */
    private static float effectiveTps() {
        if (!HudConfig.USE_SERVER_TIME.get()) {
            return 20.0f;
        }
        return Math.max(1.0f, ServerClock.tps());
    }

    /** Multiplies the alpha already embedded in an ARGB color. */
    public static int scaleAlpha(int argb, float factor) {
        int alpha = Mth.clamp(Math.round((argb >>> 24) * factor), 0, 255);
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    /** Combines an RGB (no alpha) with an alpha 0..1. */
    public static int rgbWithAlpha(int rgb, float alpha) {
        int a = Mth.clamp(Math.round(alpha * 255.0f), 0, 255);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    /** Entry border: the spell's school color, or the fixed config color. */
    public static int borderColorFor(CooldownEntry entry, float alpha) {
        if (HudConfig.USE_SCHOOL_COLOR.get()) {
            return rgbWithAlpha(entry.schoolColor(), alpha);
        }
        return scaleAlpha(HudConfig.BORDER_COLOR.argb(), alpha);
    }

    public static void drawIcon(GuiGraphics graphics, @Nullable ResourceLocation icon,
                                int x, int y, int size, float alpha, int fallbackRgb) {
        if (icon == null) {
            // Preview without Iron's Spells: a colored square in place of the icon.
            graphics.fill(x, y, x + size, y + size, rgbWithAlpha(fallbackRgb, alpha * 0.85f));
            return;
        }

        graphics.setColor(1.0f, 1.0f, 1.0f, alpha);
        // 11-argument overload: (x, y, width, height) is the DESTINATION rectangle and (uWidth,
        // vHeight) the source region. It's the only blit that scales -- the 9-argument one uses the
        // same numbers for source and destination and would come out with the texture cropped.
        graphics.blit(icon, x, y, size, size, 0.0f, 0.0f,
                SPELL_ICON_TEXTURE_SIZE, SPELL_ICON_TEXTURE_SIZE,
                SPELL_ICON_TEXTURE_SIZE, SPELL_ICON_TEXTURE_SIZE);
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int argb) {
        if ((argb >>> 24) == 0) {
            return;
        }
        graphics.fill(x, y, x + width, y + 1, argb);
        graphics.fill(x, y + height - 1, x + width, y + height, argb);
        graphics.fill(x, y + 1, x + 1, y + height - 1, argb);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, argb);
    }

    /**
     * Circular sweep over the icon, covering the fraction still on cooldown.
     *
     * <p>The outer vertices go to the edge of the SQUARE, not a circle: that way the sweep covers
     * the icon all the way to the corners without spilling out the sides, which is how the WoW/LoL
     * "pie" cooldown behaves.
     */
    public static void drawRadialSweep(GuiGraphics graphics, int x, int y, int size,
                                       float fraction, int argb) {
        if (fraction <= 0.0f || (argb >>> 24) == 0) {
            return;
        }
        float clamped = Mth.clamp(fraction, 0.0f, 1.0f);

        // The geometry below is drawn in immediate mode; without flushing GuiGraphics' pending
        // batch first, the icon would be drawn AFTER and cover the sweep.
        graphics.flush();

        float half = size / 2.0f;
        float centerX = x + half;
        float centerY = y + half;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance()
                .begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        buffer.addVertex(matrix, centerX, centerY, 0.0f).setColor(argb);

        int segments = Math.max(2, Mth.ceil(SWEEP_SEGMENTS * clamped));
        float sweep = clamped * TWO_PI;

        for (int i = 0; i <= segments; i++) {
            float angle = sweep * i / segments;
            // Starts at 12 o'clock and turns clockwise, like a clock.
            float dx = Mth.sin(angle);
            float dy = -Mth.cos(angle);
            // Projection onto the square's edge: on the larger axis, the reach is exactly half a side.
            float reach = half / Math.max(Math.abs(dx), Math.abs(dy));
            buffer.addVertex(matrix, centerX + dx * reach, centerY + dy * reach, 0.0f).setColor(argb);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    /** White flash that covers the entry the instant the spell becomes ready. */
    public static void drawReadyFlash(GuiGraphics graphics, int x, int y, int size, float intensity) {
        if (intensity <= 0.0f) {
            return;
        }
        int flash = HudConfig.READY_FLASH_COLOR.argb();
        graphics.fill(x, y, x + size, y + size, scaleAlpha(flash, intensity));
    }

    /** Text centered horizontally within a band of width {@code width}. */
    public static void drawCenteredText(GuiGraphics graphics, String text, int x, int y,
                                        int width, float alpha) {
        Font font = font();
        int textX = x + (width - font.width(text)) / 2;
        graphics.drawString(font, text, textX, y, scaleAlpha(HudConfig.TEXT_COLOR.argb(), alpha), true);
    }

    public static void drawText(GuiGraphics graphics, String text, int x, int y, float alpha) {
        graphics.drawString(font(), text, x, y, scaleAlpha(HudConfig.TEXT_COLOR.argb(), alpha), true);
    }

    /**
     * The cooldown number and spell level over the icon.
     *
     * <p>The two don't fit both centered: the font is 9px tall and the default icon is 20, so a
     * centered timer (rows 5 to 14) crosses a level at the bottom (rows 11 to 20). With the level
     * on, the timer moves to the top band and the level to the bottom, without crossing; on its
     * own, the timer stays centered.
     */
    public static void drawOverlays(GuiGraphics graphics, CooldownEntry entry,
                                    int x, int y, int size, float alpha) {
        Font font = font();
        boolean withLevel = HudConfig.SHOW_LEVEL.get() && entry.level() > 0;

        if (HudConfig.SHOW_TIMER.get() && !entry.ready()) {
            // With the level at the bottom, the timer is centered in the space left ABOVE it -- so
            // it follows the icon size instead of sticking to the top on large icons.
            int textY = withLevel
                    ? y + Math.max(1, (levelTop(size, font) - font.lineHeight) / 2)
                    : y + (size - font.lineHeight) / 2;
            drawCenteredText(graphics, timerText(entry), x, textY, size, alpha);
        }

        if (withLevel) {
            drawLevelBadge(graphics, entry.level(), x, y, size, alpha);
        }
    }

    /**
     * Offset, from the top of the icon, where the level band starts. It's in one function because
     * the timer needs the same number to know how far down it can go.
     */
    private static int levelTop(int size, Font font) {
        return size - font.lineHeight;
    }

    /** Spell level in the bottom-right corner of the icon, over a dark background. */
    public static void drawLevelBadge(GuiGraphics graphics, int level, int iconX, int iconY,
                                      int size, float alpha) {
        if (level <= 0) {
            return;
        }
        String text = String.valueOf(level);
        Font font = font();
        int textWidth = font.width(text);
        int textX = iconX + size - textWidth - 1;
        int textY = levelTop(size, font) + iconY;

        // Without the background, the level disappears over light icons -- and many spell icons are
        // near-white in the center. The background doesn't rise above textY: one more pixel would
        // steal the timer's last free row on 19px icons, a common size.
        graphics.fill(textX - 1, textY, textX + textWidth + 1, textY + font.lineHeight - 1,
                scaleAlpha(0xC0000000, alpha));
        graphics.drawString(font, text, textX, textY,
                scaleAlpha(HudConfig.TEXT_COLOR.argb(), alpha), true);
    }

    public static int backgroundColor(float alpha) {
        return scaleAlpha(HudConfig.BACKGROUND_COLOR.argb(), alpha);
    }

    public static int sweepColor(float alpha) {
        return scaleAlpha(HudConfig.SWEEP_COLOR.argb(), alpha);
    }

    /** Raw text color, for when the caller needs it without drawing. */
    public static int textColor(float alpha) {
        return scaleAlpha(HudConfig.TEXT_COLOR.argb(), alpha);
    }

    /** Hex of the color, handy for the editor to show the current value. */
    public static String hex(int argb) {
        return CachedColor.toHex(argb);
    }
}
