package com.merlin.spellcooldownhud.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** The mod's key mappings. */
public final class Keybinds {

    public static final String CATEGORY = "key.categories.spellcooldownhud";

    /**
     * Opens the HUD editor. K is just a default: it may clash in a large modpack, in which case
     * rebind it under Options > Controls. The editor also opens via Mods > Spell Cooldown HUD > Config.
     */
    public static final KeyMapping OPEN_EDITOR = new KeyMapping(
            "key.spellcooldownhud.open_editor",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY);

    private Keybinds() {
    }
}
