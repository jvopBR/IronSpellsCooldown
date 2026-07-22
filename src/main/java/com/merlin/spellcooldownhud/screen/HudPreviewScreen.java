package com.merlin.spellcooldownhud.screen;

/**
 * Marca as telas que desenham o proprio preview da HUD.
 *
 * <p>{@link com.merlin.spellcooldownhud.client.HudLayer} pula o desenho normal enquanto uma delas
 * estiver aberta, senao a HUD sairia em dobro -- uma vez pela camada de GUI e outra pelo preview.
 */
public interface HudPreviewScreen {
}
