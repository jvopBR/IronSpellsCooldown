package com.merlin.spellcooldownhud.client;

import com.merlin.spellcooldownhud.config.Anchor;
import com.merlin.spellcooldownhud.config.GrowDirection;

/**
 * Turns anchor, offset, direction and line wrapping into concrete coordinates.
 *
 * <p>Kept separate from the render because the editor needs the exact same math to know where to
 * draw the drag rectangle -- if the editor had its own, the HUD would jump on mouse release.
 *
 * @param originX    top-left corner of the block, with anchor and offset already applied
 * @param cellWidth  width of one entry; depends on the style
 * @param maxPerLine entries per row (horizontal direction) or per column (vertical)
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

    /** Absolute X of the entry at {@code index}. */
    public int cellX(int index) {
        int step = cellWidth + spacing;
        if (direction.horizontal()) {
            int column = index % maxPerLine;
            // Growing leftward, the first entry sits against the block's right edge.
            return direction.reversed()
                    ? originX + width - cellWidth - column * step
                    : originX + column * step;
        }
        return originX + (index / maxPerLine) * step;
    }

    /** Absolute Y of the entry at {@code index}. */
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
