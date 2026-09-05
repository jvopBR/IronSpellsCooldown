package com.merlin.spellcooldownhud.config;

// Inner spec types by simple name so the field declarations stay loader-agnostic (ForgeConfigSpec
// exposes the same inner types). Only these imports and the SPEC field type are gated by loader.
//? if <1.21 {
/*import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
*///?} else {
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
//?}

/**
 * The mod's client config, written to {@code config/spellcooldownhud-client.toml}.
 *
 * <p>Every value is read directly during render. That's cheap because {@code ConfigValue.get()}
 * caches the value and only invalidates it on reload; colors, which would need parsing, go through
 * {@link CachedColor}.
 */
public final class HudConfig {
    private static final Builder BUILDER = new Builder();

    // ---------------------------------------------------------------- conteudo
    public static final EnumValue<ContentMode> CONTENT_MODE;
    public static final EnumValue<SortMode> SORT_MODE;
    public static final IntValue MAX_ENTRIES;
    public static final BooleanValue HIDE_IN_F1;
    public static final BooleanValue USE_SERVER_TIME;

    // ------------------------------------------------------------------ layout
    public static final EnumValue<Anchor> ANCHOR;
    public static final IntValue OFFSET_X;
    public static final IntValue OFFSET_Y;
    public static final EnumValue<GrowDirection> GROW_DIRECTION;
    public static final IntValue ICON_SIZE;
    public static final IntValue SPACING;
    public static final IntValue MAX_PER_LINE;

    // ------------------------------------------------------------------ estilo
    public static final EnumValue<HudStyle> STYLE;
    public static final BooleanValue SHOW_ICON;
    public static final BooleanValue SHOW_NAME;
    public static final BooleanValue SHOW_TIMER;
    public static final EnumValue<TimerFormat> TIMER_FORMAT;
    public static final BooleanValue SHOW_LEVEL;
    public static final BooleanValue DRAW_BORDER;

    // ------------------------------------------------------------------- cores
    public static final BooleanValue USE_SCHOOL_COLOR;
    public static final CachedColor BACKGROUND_COLOR;
    public static final CachedColor SWEEP_COLOR;
    public static final CachedColor BORDER_COLOR;
    public static final CachedColor TEXT_COLOR;
    public static final CachedColor READY_FLASH_COLOR;

    // ------------------------------------------------------------------ efeitos
    public static final DoubleValue OPACITY;
    public static final DoubleValue SCALE;
    public static final IntValue FADE_IN_TICKS;
    public static final IntValue FADE_OUT_TICKS;
    public static final BooleanValue FLASH_WHEN_READY;
    public static final BooleanValue DIM_WHEN_READY;

    //? if <1.21 {
    /*public static final net.minecraftforge.common.ForgeConfigSpec SPEC;
    *///?} else {
    public static final net.neoforged.neoforge.common.ModConfigSpec SPEC;
    //?}

    static {
        BUILDER.comment("What shows up on the HUD").push("content");
        CONTENT_MODE = BUILDER
                .comment("ONLY_ON_COOLDOWN: the icon appears when you cast a spell and fades when it is ready.",
                        "ALL_EQUIPPED: always shows every equipped spell.")
                .defineEnum("contentMode", ContentMode.ONLY_ON_COOLDOWN);
        SORT_MODE = BUILDER
                .comment("Entry order. SLOT_ORDER is the most stable: icons never swap places.")
                .defineEnum("sortMode", SortMode.TIME_REMAINING_ASC);
        MAX_ENTRIES = BUILDER
                .comment("Maximum number of entries drawn at once.")
                .defineInRange("maxEntries", 12, 1, 64);
        HIDE_IN_F1 = BUILDER
                .comment("Hide the HUD when the interface is hidden (F1).")
                .define("hideWhenGuiHidden", true);
        USE_SERVER_TIME = BUILDER
                .comment("Show the REAL cooldown, corrected by the server clock.",
                        "Iron's Spells only syncs the cooldown at the start, then the client counts down",
                        "on its own at 20/s. On a low-TPS server the server counts slower, and without",
                        "this correction the number hits zero before the spell is actually ready.",
                        "Turn off only if you suspect the correction is getting in the way.")
                .define("useServerTime", true);
        BUILDER.pop();

        BUILDER.comment("Where the HUD sits and how entries are laid out").push("layout");
        ANCHOR = BUILDER
                .comment("Screen corner used as reference. The offset is applied from it.")
                .defineEnum("anchor", Anchor.BOTTOM_CENTER);
        OFFSET_X = BUILDER
                .comment("Horizontal offset from the anchor, in GUI pixels.")
                .defineInRange("offsetX", 0, -10000, 10000);
        OFFSET_Y = BUILDER
                .comment("Vertical offset from the anchor, in GUI pixels.")
                .defineInRange("offsetY", -60, -10000, 10000);
        GROW_DIRECTION = BUILDER
                .comment("Direction in which entries stack up.")
                .defineEnum("growDirection", GrowDirection.RIGHT);
        ICON_SIZE = BUILDER
                .comment("Icon side in GUI pixels. Iron's Spells textures are 16x16 and get scaled.")
                .defineInRange("iconSize", 20, 4, 128);
        SPACING = BUILDER
                .comment("Gap between entries, in GUI pixels.")
                .defineInRange("spacing", 4, 0, 64);
        MAX_PER_LINE = BUILDER
                .comment("How many entries per row (or column) before wrapping.")
                .defineInRange("maxPerLine", 8, 1, 32);
        BUILDER.pop();

        BUILDER.comment("Look of each entry").push("style");
        STYLE = BUILDER
                .comment("RADIAL: circular sweep over the icon.",
                        "BAR: progress bar under the icon.",
                        "TEXT_LIST: text only, no icon.")
                .defineEnum("style", HudStyle.RADIAL);
        SHOW_ICON = BUILDER.define("showIcon", true);
        SHOW_NAME = BUILDER
                .comment("Spell name next to the icon. Always shown in the TEXT_LIST style.")
                .define("showSpellName", false);
        SHOW_TIMER = BUILDER.define("showTimer", true);
        TIMER_FORMAT = BUILDER
                .comment("SECONDS: '7s'. TENTHS: '3.4' below 10s. MM_SS: '1:05'.")
                .defineEnum("timerFormat", TimerFormat.SECONDS);
        SHOW_LEVEL = BUILDER
                .comment("Spell level in a corner of the icon.")
                .define("showSpellLevel", false);
        DRAW_BORDER = BUILDER.define("drawBorder", true);
        BUILDER.pop();

        BUILDER.comment("Colors in hex: #RRGGBB or #AARRGGBB").push("colors");
        USE_SCHOOL_COLOR = BUILDER
                .comment("Take the border/bar color from the spell's school (fire, ice, blood...)",
                        "instead of borderColor. The color comes from Iron's Spells itself.")
                .define("useSchoolColor", true);
        BACKGROUND_COLOR = color("backgroundColor", "#90000000",
                "Background behind the icon.");
        SWEEP_COLOR = color("sweepColor", "#B0101014",
                "Sweep / consumed part of the bar: what covers the icon while the cooldown runs.");
        BORDER_COLOR = color("borderColor", "#FF3C3C46",
                "Border. Ignored when useSchoolColor is on.");
        TEXT_COLOR = color("textColor", "#FFFFFFFF",
                "Numbers and names.");
        READY_FLASH_COLOR = color("readyFlashColor", "#A0FFFFFF",
                "Quick flash the moment the spell becomes ready.");
        BUILDER.pop();

        BUILDER.comment("Transparency, scale and animations").push("effects");
        OPACITY = BUILDER
                .comment("Overall HUD opacity.")
                .defineInRange("opacity", 1.0, 0.05, 1.0);
        SCALE = BUILDER
                .comment("Scale of the whole HUD, applied on top of the game's GUI scale.")
                .defineInRange("scale", 1.0, 0.25, 4.0);
        FADE_IN_TICKS = BUILDER
                .comment("Fade-in ticks when appearing. 0 disables it (20 ticks = 1s).")
                .defineInRange("fadeInTicks", 3, 0, 100);
        FADE_OUT_TICKS = BUILDER
                .comment("Fade-out ticks when disappearing, in ONLY_ON_COOLDOWN mode.")
                .defineInRange("fadeOutTicks", 6, 0, 100);
        FLASH_WHEN_READY = BUILDER
                .comment("Flash the moment the spell becomes ready.")
                .define("flashWhenReady", true);
        DIM_WHEN_READY = BUILDER
                .comment("In ALL_EQUIPPED mode, dim the spells that are NOT ready.")
                .define("dimWhenOnCooldown", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private static CachedColor color(String path, String defaultHex, String... comment) {
        return new CachedColor(
                BUILDER.comment(comment).define(path, defaultHex, CachedColor::isValid),
                CachedColor.parse(defaultHex, 0xFFFFFFFF));
    }

    private HudConfig() {
    }
}
