package com.merlin.spellcooldownhud.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Teclas do mod. */
public final class Keybinds {

    public static final String CATEGORY = "key.categories.spellcooldownhud";

    /**
     * Abre o editor da HUD. K e so um padrao: num modpack grande pode conflitar, e ai basta
     * remapear em Opcoes > Controles. O editor tambem abre por Mods > Spell Cooldown HUD > Config.
     */
    public static final KeyMapping OPEN_EDITOR = new KeyMapping(
            "key.spellcooldownhud.open_editor",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY);

    private Keybinds() {
    }
}
