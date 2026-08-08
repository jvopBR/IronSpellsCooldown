package com.merlin.spellcooldownhud.config;

/**
 * Screen point the HUD is positioned from.
 *
 * <p>We store anchor + offset instead of absolute coordinates so the HUD stays put when the window
 * size or GUI scale changes: a HUD anchored at {@link #BOTTOM_RIGHT} sticks to the bottom-right
 * corner on any screen, whereas an absolute coordinate would fall off-screen on a smaller window.
 */
public enum Anchor {
    TOP_LEFT(0.0f, 0.0f),
    TOP_CENTER(0.5f, 0.0f),
    TOP_RIGHT(1.0f, 0.0f),
    MIDDLE_LEFT(0.0f, 0.5f),
    CENTER(0.5f, 0.5f),
    MIDDLE_RIGHT(1.0f, 0.5f),
    BOTTOM_LEFT(0.0f, 1.0f),
    BOTTOM_CENTER(0.5f, 1.0f),
    BOTTOM_RIGHT(1.0f, 1.0f);

    private final float xFraction;
    private final float yFraction;

    Anchor(float xFraction, float yFraction) {
        this.xFraction = xFraction;
        this.yFraction = yFraction;
    }

    public int anchorX(int screenWidth) {
        return Math.round(screenWidth * xFraction);
    }

    public int anchorY(int screenHeight) {
        return Math.round(screenHeight * yFraction);
    }

    /**
     * Offset to apply to the block so the anchor behaves as expected: anchored right, the block
     * grows leftward; centered, it ends up actually centered.
     */
    public int alignX(int blockWidth) {
        return -Math.round(blockWidth * xFraction);
    }

    public int alignY(int blockHeight) {
        return -Math.round(blockHeight * yFraction);
    }

    /**
     * Anchor for the screen region point (x, y) fell into, splitting the screen into nine thirds.
     * Used by the editor while dragging.
     *
     * <p>By region, not nearest anchor by distance: a HUD 60px above the hotbar has its center
     * closer to the MIDDLE of the screen than to the bottom edge, so the distance-based version
     * re-anchored to center on the slightest drag -- and then the HUD moved on another resolution.
     * By thirds, "it's in the bottom third" yields a bottom anchor, which is what's expected.
     */
    public static Anchor nearest(int x, int y, int screenWidth, int screenHeight) {
        int column = third(x, screenWidth);
        int row = third(y, screenHeight);
        // Constants are ordered row by row, left to right.
        return values()[row * 3 + column];
    }

    /** 0, 1 or 2 depending on whether the position falls in the first, second or third third. */
    private static int third(int position, int size) {
        if (size <= 0) {
            return 1;
        }
        return Math.min(2, Math.max(0, position * 3 / size));
    }
}
