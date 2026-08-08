package com.merlin.spellcooldownhud.screen;

/**
 * Marks the screens that draw their own HUD preview.
 *
 * <p>{@link com.merlin.spellcooldownhud.client.HudLayer} skips the normal drawing while one of them
 * is open, otherwise the HUD would show twice -- once from the GUI layer and once from the preview.
 */
public interface HudPreviewScreen {
}
