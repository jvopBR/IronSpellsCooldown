package com.merlin.spellcooldownhud.client;

import com.merlin.spellcooldownhud.client.render.CooldownRenderer;
import com.merlin.spellcooldownhud.config.ContentMode;
import com.merlin.spellcooldownhud.config.HudConfig;
import com.merlin.spellcooldownhud.screen.HudPreviewScreen;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * A camada de HUD propriamente dita, registrada logo acima da hotbar.
 *
 * <p>O desenho vive num metodo estatico para o editor poder reaproveita-lo no preview: ajustar a
 * HUD numa tela que desenha algo diferente do jogo seria inutil.
 */
public final class HudLayer implements LayeredDraw.Layer {

    private final CooldownTracker tracker;

    public HudLayer(CooldownTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!shouldRender()) {
            return;
        }
        draw(graphics, tracker, HudConfig.CONTENT_MODE.get());
    }

    private static boolean shouldRender() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return false;
        }
        // O GuiLayerManager achata os grupos vanilla e embrulha CADA camada vanilla no teste de
        // hideGui -- camadas de mod inseridas entre elas nao herdam esse teste. Sem esta linha a
        // HUD continuaria aparecendo com o F1 ligado.
        if (HudConfig.HIDE_IN_F1.get() && minecraft.options.hideGui) {
            return false;
        }
        // Com o editor ou a tela de mover abertos quem desenha o preview sao eles, para nao
        // sair em dobro.
        return !(minecraft.screen instanceof HudPreviewScreen);
    }

    /**
     * Desenha a HUD e devolve o layout usado, que o editor aproveita para saber onde esta o
     * retangulo de arrasto.
     */
    public static HudLayout draw(GuiGraphics graphics, CooldownTracker tracker, ContentMode mode) {
        List<CooldownTracker.TrackedEntry> entries = tracker.snapshot(
                mode, HudConfig.SORT_MODE.get(), HudConfig.MAX_ENTRIES.get());

        CooldownRenderer renderer = CooldownRenderer.forStyle(HudConfig.STYLE.get());
        int iconSize = HudConfig.ICON_SIZE.get();
        int cellWidth = renderer.cellWidth(iconSize, entries);
        int cellHeight = renderer.cellHeight(iconSize);

        // A escala do mod multiplica a escala de GUI do jogo. Calculamos a ancoragem no espaco
        // JA escalado, senao uma HUD ancorada a direita sairia da tela ao aumentar a escala.
        float scale = HudConfig.SCALE.get().floatValue();
        int screenWidth = Mth.floor(graphics.guiWidth() / scale);
        int screenHeight = Mth.floor(graphics.guiHeight() / scale);

        HudLayout layout = HudLayout.compute(
                entries.size(), cellWidth, cellHeight,
                HudConfig.SPACING.get(), HudConfig.GROW_DIRECTION.get(), HudConfig.MAX_PER_LINE.get(),
                HudConfig.ANCHOR.get(), HudConfig.OFFSET_X.get(), HudConfig.OFFSET_Y.get(),
                screenWidth, screenHeight);

        if (entries.isEmpty()) {
            return layout;
        }

        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0f);

        for (int i = 0; i < entries.size(); i++) {
            renderer.render(graphics, entries.get(i), layout.cellX(i), layout.cellY(i), cellWidth, iconSize);
        }

        graphics.pose().popPose();
        return layout;
    }
}
