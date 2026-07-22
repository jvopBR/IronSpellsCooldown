package com.merlin.spellcooldownhud.client;

import com.merlin.spellcooldownhud.config.Anchor;
import com.merlin.spellcooldownhud.config.GrowDirection;

/**
 * Converte ancora, offset, direcao e quebra de linha em coordenadas concretas.
 *
 * <p>Fica separado do render porque o editor precisa exatamente do mesmo calculo para saber onde
 * desenhar o retangulo de arrasto -- se o editor tivesse a propria conta, a HUD sairia do lugar
 * ao soltar o mouse.
 *
 * @param originX    canto superior esquerdo do bloco, ja com ancora e offset aplicados
 * @param cellWidth  largura de uma entrada; depende do estilo
 * @param maxPerLine entradas por linha (direcao horizontal) ou por coluna (vertical)
 */
public record HudLayout(
        int originX,
        int originY,
        int width,
        int height,
        int cellWidth,
        int cellHeight,
        int spacing,
        GrowDirection direction,
        int maxPerLine) {

    public static HudLayout compute(int count,
                                    int cellWidth,
                                    int cellHeight,
                                    int spacing,
                                    GrowDirection direction,
                                    int maxPerLine,
                                    Anchor anchor,
                                    int offsetX,
                                    int offsetY,
                                    int screenWidth,
                                    int screenHeight) {

        int perLine = Math.max(1, maxPerLine);
        int safeCount = Math.max(0, count);

        int alongLine = Math.min(safeCount, perLine);
        int lineCount = safeCount == 0 ? 0 : ceilDiv(safeCount, perLine);

        int columns = direction.horizontal() ? alongLine : lineCount;
        int rows = direction.horizontal() ? lineCount : alongLine;

        int width = span(columns, cellWidth, spacing);
        int height = span(rows, cellHeight, spacing);

        int originX = anchor.anchorX(screenWidth) + anchor.alignX(width) + offsetX;
        int originY = anchor.anchorY(screenHeight) + anchor.alignY(height) + offsetY;

        return new HudLayout(originX, originY, width, height,
                cellWidth, cellHeight, spacing, direction, perLine);
    }

    /** X absoluto da entrada de indice {@code index}. */
    public int cellX(int index) {
        int step = cellWidth + spacing;
        if (direction.horizontal()) {
            int column = index % maxPerLine;
            // Crescendo para a esquerda, a primeira entrada encosta na borda direita do bloco.
            return direction.reversed()
                    ? originX + width - cellWidth - column * step
                    : originX + column * step;
        }
        return originX + (index / maxPerLine) * step;
    }

    /** Y absoluto da entrada de indice {@code index}. */
    public int cellY(int index) {
        int step = cellHeight + spacing;
        if (direction.horizontal()) {
            return originY + (index / maxPerLine) * step;
        }
        int row = index % maxPerLine;
        return direction.reversed()
                ? originY + height - cellHeight - row * step
                : originY + row * step;
    }

    public boolean contains(double x, double y) {
        return x >= originX && x < originX + width && y >= originY && y < originY + height;
    }

    private static int span(int cells, int cellSize, int spacing) {
        return cells <= 0 ? 0 : cells * cellSize + (cells - 1) * spacing;
    }

    private static int ceilDiv(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }
}
