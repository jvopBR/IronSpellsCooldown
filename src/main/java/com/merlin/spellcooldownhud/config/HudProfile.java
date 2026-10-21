package com.merlin.spellcooldownhud.config;

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
 * One HUD's complete look and layout. The mod has two profiles -- cooldowns and active effects --
 * each fully independent, so you can size, style, color and place them however you want. Built under
 * a macro-section ({@code [cooldowns.*]} / {@code [effects.*]}) with the four sub-sections below.
 *
 * <p>The render path reads the active profile ({@link HudConfig#active}); the editor edits the
 * selected one. Only truly shared settings (the master effects toggle, F1 hiding, and the
 * cooldown-only content mode / server-time correction) live on {@link HudConfig} itself.
 */
public final class HudProfile {

    // ---- layout ----
    public final EnumValue<Anchor> anchor;
    public final IntValue offsetX;
    public final IntValue offsetY;
    public final EnumValue<GrowDirection> growDirection;
    public final IntValue iconSize;
    public final IntValue spacing;
    public final IntValue maxPerLine;
    public final EnumValue<SortMode> sortMode;
    public final IntValue maxEntries;

    // ---- style ----
    public final EnumValue<HudStyle> style;
    public final BooleanValue showIcon;
    public final BooleanValue showName;
    public final BooleanValue showTimer;
    public final EnumValue<TimerFormat> timerFormat;
    public final BooleanValue showLevel;
    public final BooleanValue showMana;
    public final BooleanValue drawBorder;

    // ---- colors ----
    public final BooleanValue useSchoolColor;
    public final CachedColor backgroundColor;
    public final CachedColor sweepColor;
    public final CachedColor borderColor;
    public final CachedColor textColor;
    public final CachedColor readyFlashColor;

    // ---- effects (animation) ----
    public final DoubleValue opacity;
    public final DoubleValue scale;
    public final IntValue fadeInTicks;
    public final IntValue fadeOutTicks;
    public final BooleanValue flashWhenReady;
    public final BooleanValue dimWhenReady;

    /**
     * @param builder    the spec builder, positioned inside this profile's macro-section
     * @param defAnchor  default anchor (so the two HUDs start in different corners)
     * @param defOffsetX default horizontal offset
     * @param defOffsetY default vertical offset
     */
    HudProfile(Builder builder, Anchor defAnchor, int defOffsetX, int defOffsetY) {
        builder.comment("Where this HUD sits and how its entries are laid out").push("layout");
        anchor = builder
                .comment("Screen corner used as reference. The offset is applied from it.")
                .defineEnum("anchor", defAnchor);
        offsetX = builder
                .comment("Horizontal offset from the anchor, in GUI pixels.")
                .defineInRange("offsetX", defOffsetX, -10000, 10000);
        offsetY = builder
                .comment("Vertical offset from the anchor, in GUI pixels.")
                .defineInRange("offsetY", defOffsetY, -10000, 10000);
        growDirection = builder
                .comment("Direction in which entries stack up.")
                .defineEnum("growDirection", GrowDirection.RIGHT);
        iconSize = builder
                .comment("Icon side in GUI pixels. Iron's Spells textures are 16x16 and get scaled.")
                .defineInRange("iconSize", 20, 4, 128);
        spacing = builder
                .comment("Gap between entries, in GUI pixels.")
                .defineInRange("spacing", 4, 0, 64);
        maxPerLine = builder
                .comment("How many entries per row (or column) before wrapping.")
                .defineInRange("maxPerLine", 8, 1, 32);
        sortMode = builder
                .comment("Entry order. SLOT_ORDER is the most stable: icons never swap places.")
                .defineEnum("sortMode", SortMode.TIME_REMAINING_ASC);
        maxEntries = builder
                .comment("Maximum number of entries drawn at once.")
                .defineInRange("maxEntries", 12, 1, 64);
        builder.pop();

        builder.comment("Look of each entry").push("style");
        style = builder
                .comment("RADIAL: circular sweep over the icon.",
                        "BAR: progress bar under the icon.",
                        "TEXT_LIST: text only, no icon.")
                .defineEnum("style", HudStyle.RADIAL);
        showIcon = builder.define("showIcon", true);
        showName = builder
                .comment("Name next to the icon. Always shown in the TEXT_LIST style.")
                .define("showSpellName", false);
        showTimer = builder.define("showTimer", true);
        timerFormat = builder
                .comment("SECONDS: '7s'. TENTHS: '3.4' below 10s. MM_SS: '1:05'.")
                .defineEnum("timerFormat", TimerFormat.SECONDS);
        showLevel = builder
                .comment("Level in a corner of the icon (spell level, or effect amplifier II+).")
                .define("showSpellLevel", false);
        showMana = builder
                .comment("Mana cost in a corner of the icon. Only cooldown spells have one.")
                .define("showManaCost", false);
        drawBorder = builder.define("drawBorder", true);
        builder.pop();

        builder.comment("Colors in hex: #RRGGBB or #AARRGGBB").push("colors");
        useSchoolColor = builder
                .comment("Take the border/bar color from the spell's school (or the effect's category)",
                        "instead of borderColor.")
                .define("useSchoolColor", true);
        backgroundColor = color(builder, "backgroundColor", "#90000000", "Background behind the icon.");
        sweepColor = color(builder, "sweepColor", "#B0101014",
                "Sweep / consumed part of the bar: what covers the icon while time runs.");
        borderColor = color(builder, "borderColor", "#FF3C3C46", "Border. Ignored when useSchoolColor is on.");
        textColor = color(builder, "textColor", "#FFFFFFFF", "Numbers and names.");
        readyFlashColor = color(builder, "readyFlashColor", "#A0FFFFFF",
                "Quick flash the moment the entry becomes ready / ends.");
        builder.pop();

        builder.comment("Transparency, scale and animations").push("effects");
        opacity = builder
                .comment("Overall HUD opacity.")
                .defineInRange("opacity", 1.0, 0.05, 1.0);
        scale = builder
                .comment("Scale of this HUD, applied on top of the game's GUI scale.")
                .defineInRange("scale", 1.0, 0.25, 4.0);
        fadeInTicks = builder
                .comment("Fade-in ticks when appearing. 0 disables it (20 ticks = 1s).")
                .defineInRange("fadeInTicks", 3, 0, 100);
        fadeOutTicks = builder
                .comment("Fade-out ticks when disappearing.")
                .defineInRange("fadeOutTicks", 6, 0, 100);
        flashWhenReady = builder
                .comment("Flash the moment the entry becomes ready / the effect ends.")
                .define("flashWhenReady", true);
        dimWhenReady = builder
                .comment("Dim entries that are NOT on cooldown (ALL_EQUIPPED cooldowns).")
                .define("dimWhenOnCooldown", true);
        builder.pop();
    }

    private static CachedColor color(Builder builder, String path, String defaultHex, String... comment) {
        return new CachedColor(
                builder.comment(comment).define(path, defaultHex, CachedColor::isValid),
                CachedColor.parse(defaultHex, 0xFFFFFFFF));
    }
}
