package com.merlin.spellcooldownhud.client.render;

import com.merlin.spellcooldownhud.client.CooldownTracker;
import com.merlin.spellcooldownhud.config.HudStyle;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * Desenha uma entrada de cooldown num estilo especifico.
 *
 * <p>O tamanho da celula e perguntado antes do desenho porque o {@link
 * com.merlin.spellcooldownhud.client.HudLayout} precisa dele para posicionar tudo -- e no estilo
 * de texto ele depende do conteudo, nao so do config.
 */
public interface CooldownRenderer {

    /**
     * Largura de uma celula. Recebe a lista inteira porque estilos de texto precisam medir a
     * entrada mais larga para as colunas ficarem alinhadas.
     */
    int cellWidth(int iconSize, List<CooldownTracker.TrackedEntry> entries);

    int cellHeight(int iconSize);

    /**
     * Desenha uma entrada. A largura da celula vem pronta do layout, e nao remedida aqui, para o
     * desenho usar exatamente a mesma largura que posicionou a entrada.
     */
    void render(GuiGraphics graphics, CooldownTracker.TrackedEntry tracked,
                int x, int y, int cellWidth, int iconSize);

    static CooldownRenderer forStyle(HudStyle style) {
        return switch (style) {
            case RADIAL -> RadialRenderer.INSTANCE;
            case BAR -> BarRenderer.INSTANCE;
            case TEXT_LIST -> TextListRenderer.INSTANCE;
        };
    }
}
