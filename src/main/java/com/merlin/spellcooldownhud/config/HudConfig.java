package com.merlin.spellcooldownhud.config;

// Inner spec types by simple name so the code stays loader-agnostic; ForgeConfigSpec exposes the
// same inner types. Only these imports and the SPEC field type are gated by loader.
//? if <1.21 {
/*import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;
*///?} else {
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
//?}

/**
 * The mod's client config, written to {@code config/spellcooldownhud-client.toml}.
 *
 * <p>Two fully independent HUDs, each a {@link HudProfile}: {@link #COOLDOWNS} and {@link #EFFECTS}.
 * Only a few things are shared and live here directly -- the master effects toggle, F1 hiding, and
 * the cooldown-only content mode and server-time correction. The render path reads {@link #active},
 * which {@code HudLayer} points at the group it is drawing.
 */
public final class HudConfig {
    private static final Builder BUILDER = new Builder();

    // ---- shared / global ----
    public static final BooleanValue SHOW_ACTIVE_EFFECTS;
    public static final BooleanValue HIDE_IN_F1;

    // ---- cooldowns only ----
    public static final EnumValue<ContentMode> CONTENT_MODE;
    public static final BooleanValue USE_SERVER_TIME;

    // ---- the two HUDs, each fully independent ----
    public static final HudProfile COOLDOWNS;
    public static final HudProfile EFFECTS;

    //? if <1.21 {
    /*public static final net.minecraftforge.common.ForgeConfigSpec SPEC;
    *///?} else {
    public static final net.neoforged.neoforge.common.ModConfigSpec SPEC;
    //?}

    /** The profile the render path reads; set to the group being drawn by {@code HudLayer}. */
    public static HudProfile active;

    static {
        BUILDER.comment("Shared switches").push("general");
        SHOW_ACTIVE_EFFECTS = BUILDER
                .comment("Show the active-effects HUD (your buffs/debuffs with their remaining duration),",
                        "as a second HUD with its own look and position. Configure it under [effects].")
                .define("showActiveEffects", false);
        HIDE_IN_F1 = BUILDER
                .comment("Hide both HUDs when the interface is hidden (F1).")
                .define("hideWhenGuiHidden", true);
        BUILDER.pop();

        BUILDER.comment("The spell cooldown HUD").push("cooldowns");
        BUILDER.comment("What the cooldown HUD shows").push("content");
        CONTENT_MODE = BUILDER
                .comment("ONLY_ON_COOLDOWN: the icon appears when you cast a spell and fades when ready.",
                        "ALL_EQUIPPED: always shows every equipped spell.")
                .defineEnum("contentMode", ContentMode.ONLY_ON_COOLDOWN);
        USE_SERVER_TIME = BUILDER
                .comment("Show the REAL cooldown, corrected by the server clock, so the number stays",
                        "accurate on a low-TPS server. Turn off only if it seems to get in the way.")
                .define("useServerTime", true);
        BUILDER.pop();
        COOLDOWNS = new HudProfile(BUILDER, Anchor.BOTTOM_CENTER, 0, -60);
        BUILDER.pop();

        BUILDER.comment("The active-effects HUD -- its own, independent look and position").push("effects");
        EFFECTS = new HudProfile(BUILDER, Anchor.TOP_LEFT, 4, 4);
        BUILDER.pop();

        active = COOLDOWNS;
        SPEC = BUILDER.build();
    }

    private HudConfig() {
    }
}
