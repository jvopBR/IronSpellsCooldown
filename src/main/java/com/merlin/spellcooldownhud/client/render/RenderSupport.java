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

/** Primitivas de desenho compartilhadas pelos tres estilos de HUD. */
public final class RenderSupport {

    /** Os icones de magia do Iron's Spells (e dos addons) sao sempre 16x16. */
    public static final int SPELL_ICON_TEXTURE_SIZE = 16;

    /** Segmentos de um circulo completo na varredura radial. */
    private static final int SWEEP_SEGMENTS = 72;

    private static final float TWO_PI = (float) (Math.PI * 2.0);

    private RenderSupport() {
    }

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    /**
     * Tempo restante ja formatado, convertendo ticks em segundos pela taxa REAL do servidor.
     *
     * <p>Dividir por 20 fixo mostraria menos tempo do que a magia realmente vai levar num servidor
     * lagado. Os ticks restantes ja vem corrigidos por
     * {@link com.merlin.spellcooldownhud.data.ServerSyncedSource}; aqui so falta a conversao.
     */
    public static String timerText(CooldownEntry entry) {
        return HudConfig.TIMER_FORMAT.get().format(entry.remainingTicks() / effectiveTps());
    }

    /**
     * Piso de 1 TPS: com o servidor praticamente parado a divisao explodiria o numero na tela, e
     * "muito tempo" ja esta comunicado bem antes disso.
     */
    private static float effectiveTps() {
        if (!HudConfig.USE_SERVER_TIME.get()) {
            return 20.0f;
        }
        return Math.max(1.0f, ServerClock.tps());
    }

    /** Multiplica o alpha ja embutido numa cor ARGB. */
    public static int scaleAlpha(int argb, float factor) {
        int alpha = Mth.clamp(Math.round((argb >>> 24) * factor), 0, 255);
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    /** Combina um RGB (sem alpha) com um alpha 0..1. */
    public static int rgbWithAlpha(int rgb, float alpha) {
        int a = Mth.clamp(Math.round(alpha * 255.0f), 0, 255);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    /** Borda da entrada: cor da escola da magia ou a cor fixa do config. */
    public static int borderColorFor(CooldownEntry entry, float alpha) {
        if (HudConfig.USE_SCHOOL_COLOR.get()) {
            return rgbWithAlpha(entry.schoolColor(), alpha);
        }
        return scaleAlpha(HudConfig.BORDER_COLOR.argb(), alpha);
    }

    public static void drawIcon(GuiGraphics graphics, @Nullable ResourceLocation icon,
                                int x, int y, int size, float alpha, int fallbackRgb) {
        if (icon == null) {
            // Preview sem Iron's Spells: um quadrado colorido no lugar do icone.
            graphics.fill(x, y, x + size, y + size, rgbWithAlpha(fallbackRgb, alpha * 0.85f));
            return;
        }

        graphics.setColor(1.0f, 1.0f, 1.0f, alpha);
        // Overload de 11 argumentos: (x, y, width, height) e o retangulo de DESTINO e
        // (uWidth, vHeight) a regiao de origem. E o unico blit que escala -- o de 9 argumentos
        // usa os mesmos numeros para origem e destino e sairia com a textura cortada.
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
     * Varredura circular sobre o icone, cobrindo a fracao ainda em cooldown.
     *
     * <p>Os vertices externos vao para a borda do QUADRADO, nao de um circulo: assim a varredura
     * cobre o icone ate os cantos sem vazar pelas laterais, que e como o cooldown "pie" de
     * WoW/LoL se comporta.
     */
    public static void drawRadialSweep(GuiGraphics graphics, int x, int y, int size,
                                       float fraction, int argb) {
        if (fraction <= 0.0f || (argb >>> 24) == 0) {
            return;
        }
        float clamped = Mth.clamp(fraction, 0.0f, 1.0f);

        // A geometria abaixo e desenhada em modo imediato; sem liberar o batch pendente do
        // GuiGraphics primeiro, o icone seria desenhado DEPOIS e cobriria a varredura.
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
            // Comeca as 12 horas e gira no sentido horario, como um relogio.
            float dx = Mth.sin(angle);
            float dy = -Mth.cos(angle);
            // Projecao na borda do quadrado: no maior eixo, o alcance e exatamente meio lado.
            float reach = half / Math.max(Math.abs(dx), Math.abs(dy));
            buffer.addVertex(matrix, centerX + dx * reach, centerY + dy * reach, 0.0f).setColor(argb);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    /** Brilho branco que cobre a entrada no instante em que a magia fica pronta. */
    public static void drawReadyFlash(GuiGraphics graphics, int x, int y, int size, float intensity) {
        if (intensity <= 0.0f) {
            return;
        }
        int flash = HudConfig.READY_FLASH_COLOR.argb();
        graphics.fill(x, y, x + size, y + size, scaleAlpha(flash, intensity));
    }

    /** Texto centralizado horizontalmente numa faixa de largura {@code width}. */
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
     * Numero do cooldown e nivel da magia sobre o icone.
     *
     * <p>Os dois nao cabem centralizados: a fonte tem 9px de altura e o icone padrao tem 20, entao
     * um tempo centralizado (linhas 5 a 14) cruza com um nivel no rodape (linhas 11 a 20). Com o
     * nivel ligado, o tempo sobe para a faixa de cima e o nivel fica na de baixo, sem se cruzarem;
     * sozinho, o tempo continua centralizado.
     */
    public static void drawOverlays(GuiGraphics graphics, CooldownEntry entry,
                                    int x, int y, int size, float alpha) {
        Font font = font();
        boolean withLevel = HudConfig.SHOW_LEVEL.get() && entry.level() > 0;

        if (HudConfig.SHOW_TIMER.get() && !entry.ready()) {
            // Com o nivel embaixo, o tempo e centralizado no espaco que sobra ACIMA dele -- assim
            // acompanha o tamanho do icone em vez de ficar colado no topo em icones grandes.
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
     * Deslocamento, a partir do topo do icone, onde comeca a faixa do nivel. Fica numa funcao so
     * porque o tempo precisa do mesmo numero para saber ate onde pode descer.
     */
    private static int levelTop(int size, Font font) {
        return size - font.lineHeight;
    }

    /** Nivel da magia no canto inferior direito do icone, sobre um fundo escuro. */
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

        // Sem o fundo, o nivel desaparece sobre icones claros -- e varios icones de magia sao
        // quase brancos no centro. O fundo nao sobe acima de textY: 1px a mais roubaria a
        // ultima linha livre do tempo em icones de 19px, que e um tamanho comum.
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

    /** Cor de texto crua, para quando o chamador precisa dela sem desenhar. */
    public static int textColor(float alpha) {
        return scaleAlpha(HudConfig.TEXT_COLOR.argb(), alpha);
    }

    /** Hex da cor, util para o editor mostrar o valor atual. */
    public static String hex(int argb) {
        return CachedColor.toHex(argb);
    }
}
