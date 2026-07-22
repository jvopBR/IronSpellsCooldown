package com.merlin.spellcooldownhud.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Config client do mod, gravado em {@code config/spellcooldownhud-client.toml}.
 *
 * <p>Todos os valores sao lidos direto durante o render. Isso e barato porque
 * {@link ModConfigSpec.ConfigValue#get()} guarda o valor em cache e so o invalida em reload; as
 * cores, que precisariam de parse, passam por {@link CachedColor}.
 */
public final class HudConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ---------------------------------------------------------------- conteudo
    public static final ModConfigSpec.EnumValue<ContentMode> CONTENT_MODE;
    public static final ModConfigSpec.EnumValue<SortMode> SORT_MODE;
    public static final ModConfigSpec.IntValue MAX_ENTRIES;
    public static final ModConfigSpec.BooleanValue HIDE_IN_F1;
    public static final ModConfigSpec.BooleanValue USE_SERVER_TIME;

    // ------------------------------------------------------------------ layout
    public static final ModConfigSpec.EnumValue<Anchor> ANCHOR;
    public static final ModConfigSpec.IntValue OFFSET_X;
    public static final ModConfigSpec.IntValue OFFSET_Y;
    public static final ModConfigSpec.EnumValue<GrowDirection> GROW_DIRECTION;
    public static final ModConfigSpec.IntValue ICON_SIZE;
    public static final ModConfigSpec.IntValue SPACING;
    public static final ModConfigSpec.IntValue MAX_PER_LINE;

    // ------------------------------------------------------------------ estilo
    public static final ModConfigSpec.EnumValue<HudStyle> STYLE;
    public static final ModConfigSpec.BooleanValue SHOW_ICON;
    public static final ModConfigSpec.BooleanValue SHOW_NAME;
    public static final ModConfigSpec.BooleanValue SHOW_TIMER;
    public static final ModConfigSpec.EnumValue<TimerFormat> TIMER_FORMAT;
    public static final ModConfigSpec.BooleanValue SHOW_LEVEL;
    public static final ModConfigSpec.BooleanValue DRAW_BORDER;

    // ------------------------------------------------------------------- cores
    public static final ModConfigSpec.BooleanValue USE_SCHOOL_COLOR;
    public static final CachedColor BACKGROUND_COLOR;
    public static final CachedColor SWEEP_COLOR;
    public static final CachedColor BORDER_COLOR;
    public static final CachedColor TEXT_COLOR;
    public static final CachedColor READY_FLASH_COLOR;

    // ------------------------------------------------------------------ efeitos
    public static final ModConfigSpec.DoubleValue OPACITY;
    public static final ModConfigSpec.DoubleValue SCALE;
    public static final ModConfigSpec.IntValue FADE_IN_TICKS;
    public static final ModConfigSpec.IntValue FADE_OUT_TICKS;
    public static final ModConfigSpec.BooleanValue FLASH_WHEN_READY;
    public static final ModConfigSpec.BooleanValue DIM_WHEN_READY;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.comment("O que aparece na HUD").push("content");
        CONTENT_MODE = BUILDER
                .comment("ONLY_ON_COOLDOWN: o icone aparece ao usar a magia e some quando fica pronta.",
                        "ALL_EQUIPPED: mostra sempre todas as magias equipadas.")
                .defineEnum("contentMode", ContentMode.ONLY_ON_COOLDOWN);
        SORT_MODE = BUILDER
                .comment("Ordem das entradas. SLOT_ORDER e a mais estavel: os icones nao trocam de lugar.")
                .defineEnum("sortMode", SortMode.TIME_REMAINING_ASC);
        MAX_ENTRIES = BUILDER
                .comment("Numero maximo de entradas desenhadas de uma vez.")
                .defineInRange("maxEntries", 12, 1, 64);
        HIDE_IN_F1 = BUILDER
                .comment("Esconder a HUD quando a interface estiver oculta (F1).")
                .define("hideWhenGuiHidden", true);
        USE_SERVER_TIME = BUILDER
                .comment("Mostrar o cooldown REAL, corrigido pelo relogio do servidor.",
                        "O Iron's Spells sincroniza o cooldown so no inicio e depois o cliente conta",
                        "sozinho a 20/s. Num servidor com TPS baixo o servidor conta mais devagar, e",
                        "sem esta correcao o numero chega a zero antes da magia ficar pronta.",
                        "Desligue apenas se suspeitar que a correcao esta atrapalhando.")
                .define("useServerTime", true);
        BUILDER.pop();

        BUILDER.comment("Onde a HUD fica e como as entradas se distribuem").push("layout");
        ANCHOR = BUILDER
                .comment("Canto da tela usado como referencia. O offset e aplicado a partir dele.")
                .defineEnum("anchor", Anchor.BOTTOM_CENTER);
        OFFSET_X = BUILDER
                .comment("Deslocamento horizontal a partir da ancora, em pixels de GUI.")
                .defineInRange("offsetX", 0, -10000, 10000);
        OFFSET_Y = BUILDER
                .comment("Deslocamento vertical a partir da ancora, em pixels de GUI.")
                .defineInRange("offsetY", -60, -10000, 10000);
        GROW_DIRECTION = BUILDER
                .comment("Direcao em que as entradas se acumulam.")
                .defineEnum("growDirection", GrowDirection.RIGHT);
        ICON_SIZE = BUILDER
                .comment("Lado do icone em pixels de GUI. As texturas do Iron's Spells sao 16x16 e sao escaladas.")
                .defineInRange("iconSize", 20, 4, 128);
        SPACING = BUILDER
                .comment("Espaco entre entradas, em pixels de GUI.")
                .defineInRange("spacing", 4, 0, 64);
        MAX_PER_LINE = BUILDER
                .comment("Quantas entradas por linha (ou coluna) antes de quebrar.")
                .defineInRange("maxPerLine", 8, 1, 32);
        BUILDER.pop();

        BUILDER.comment("Aparencia de cada entrada").push("style");
        STYLE = BUILDER
                .comment("RADIAL: varredura circular sobre o icone.",
                        "BAR: barra de progresso sob o icone.",
                        "TEXT_LIST: so texto, sem icone.")
                .defineEnum("style", HudStyle.RADIAL);
        SHOW_ICON = BUILDER.define("showIcon", true);
        SHOW_NAME = BUILDER
                .comment("Nome da magia ao lado do icone. Sempre visivel no estilo TEXT_LIST.")
                .define("showSpellName", false);
        SHOW_TIMER = BUILDER.define("showTimer", true);
        TIMER_FORMAT = BUILDER
                .comment("SECONDS: '7s'. TENTHS: '3.4' abaixo de 10s. MM_SS: '1:05'.")
                .defineEnum("timerFormat", TimerFormat.SECONDS);
        SHOW_LEVEL = BUILDER
                .comment("Nivel da magia num canto do icone.")
                .define("showSpellLevel", false);
        DRAW_BORDER = BUILDER.define("drawBorder", true);
        BUILDER.pop();

        BUILDER.comment("Cores em hex: #RRGGBB ou #AARRGGBB").push("colors");
        USE_SCHOOL_COLOR = BUILDER
                .comment("Tirar a cor da borda/barra da escola da magia (fogo, gelo, sangue...)",
                        "em vez de usar borderColor. Cor vem do proprio Iron's Spells.")
                .define("useSchoolColor", true);
        BACKGROUND_COLOR = color("backgroundColor", "#90000000",
                "Fundo atras do icone.");
        SWEEP_COLOR = color("sweepColor", "#B0101014",
                "Cor da varredura/parte consumida da barra: o que cobre o icone enquanto o cooldown corre.");
        BORDER_COLOR = color("borderColor", "#FF3C3C46",
                "Borda. Ignorada quando useSchoolColor esta ligado.");
        TEXT_COLOR = color("textColor", "#FFFFFFFF",
                "Numeros e nomes.");
        READY_FLASH_COLOR = color("readyFlashColor", "#A0FFFFFF",
                "Brilho rapido no instante em que a magia fica pronta.");
        BUILDER.pop();

        BUILDER.comment("Transparencia, escala e animacoes").push("effects");
        OPACITY = BUILDER
                .comment("Opacidade geral da HUD.")
                .defineInRange("opacity", 1.0, 0.05, 1.0);
        SCALE = BUILDER
                .comment("Escala da HUD inteira, aplicada sobre a escala de GUI do jogo.")
                .defineInRange("scale", 1.0, 0.25, 4.0);
        FADE_IN_TICKS = BUILDER
                .comment("Ticks de fade ao surgir. 0 desliga (20 ticks = 1s).")
                .defineInRange("fadeInTicks", 3, 0, 100);
        FADE_OUT_TICKS = BUILDER
                .comment("Ticks de fade ao sumir, no modo ONLY_ON_COOLDOWN.")
                .defineInRange("fadeOutTicks", 6, 0, 100);
        FLASH_WHEN_READY = BUILDER
                .comment("Brilho no instante em que a magia fica pronta.")
                .define("flashWhenReady", true);
        DIM_WHEN_READY = BUILDER
                .comment("No modo ALL_EQUIPPED, escurecer as magias que NAO estao prontas.")
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
