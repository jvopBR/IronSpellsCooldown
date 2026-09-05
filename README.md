Spell Cooldown HUD
==================

**English** · [Português (BR)](README.pt-BR.md)

**Client-side** Minecraft mod (1.20.1–1.21.1, Forge & NeoForge) that shows the cooldown of your
[Iron's Spells 'n Spellbooks](https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks)
spells on a fully configurable HUD — style, position, colors, content and animations.

Two things set it apart from the built-in HUD:

- **It shows the real cooldown even when the server lags.** Iron's Spells syncs the cooldown only
  once and the client counts down on its own at 20/s, so on a low-TPS server the number hits zero
  before the spell is actually ready. This mod corrects it against the server clock
  ([details](#real-cooldown-on-a-lagging-server)).
- **It works with addon spells** (`somakespells`, `gametechbcs_spellbooks`, ...) out of the box,
  icons included: everything is resolved through the Iron's Spells `SpellRegistry`.

Because it registers no network payloads, **you can use it on servers that don't have it
installed** — NeoForge doesn't even include it in mod negotiation.

Languages
---------

The whole interface is translated and follows the game language automatically (Minecraft loads the
matching `.json`; nothing to configure):

- English (`en_us`)
- Português do Brasil (`pt_br`)
- Español — Spain (`es_es`) and Mexico (`es_mx`)
- Русский (`ru_ru`)

Minecraft does **not** fall back between regional variants: someone playing a Spanish variant with
no file of its own (e.g. `es_ar`) gets English, not `es_es`. To add a language, copy `en_us.json`,
translate the values and save it as `<code>.json` — the keys don't change. The Russian translation
still needs review by a native speaker; corrections are welcome.

Requirements
------------

Pick the download that matches your instance's **Minecraft version and loader**:

| Minecraft | Loader | Iron's Spells 'n Spellbooks |
|---|---|---|
| 1.21.1 | NeoForge 21.1.0+ (built against 21.1.241) | 1.21.1-3.16.0+ |
| 1.21 | NeoForge 21.0.0+ (built against 21.0.167) | 1.21.1-3.16.0+ |
| 1.20.1 | Forge 47+ | 1.20.1-3.16.0+ |
| 1.20.1 | NeoForge 47.1+ | 1.20.1-3.4.0+ |

**Client only** — do **not** install it on the server (it registers no network payloads, so it stays
out of mod negotiation and won't block joining a server that lacks it).

Usage
-----

- Press **K** to open the move screen: fully transparent, just drag the HUD with the mouse
  (rebindable in Options > Controls).
- From there, **Settings** opens the full editor. Also reachable via
  **Mods > Spell Cooldown HUD > Config**.
- Arrow keys nudge 1px, Shift+arrows 10px. The HUD never leaves the screen.

Configuration
-------------

Everything is editable in-game with a live preview. The file lives at
`config/spellcooldownhud-client.toml`.

**`[content]` — what shows up**
`contentMode` (`ONLY_ON_COOLDOWN` \| `ALL_EQUIPPED`) · `sortMode` (`TIME_REMAINING_ASC` \|
`TIME_REMAINING_DESC` \| `SLOT_ORDER` \| `NAME`) · `maxEntries` · `hideWhenGuiHidden` ·
`useServerTime`

**`[layout]` — where it sits**
`anchor` (9 points) · `offsetX`/`offsetY` · `growDirection` (`RIGHT` \| `LEFT` \| `DOWN` \| `UP`) ·
`iconSize` · `spacing` · `maxPerLine`

**`[style]` — how each entry is drawn**
`style` (`RADIAL` \| `BAR` \| `TEXT_LIST`) · `showIcon` · `showSpellName` · `showTimer` ·
`timerFormat` (`SECONDS` \| `TENTHS` \| `MM_SS`) · `showSpellLevel` · `drawBorder`

**`[colors]` — hex `#RRGGBB` or `#AARRGGBB`**
`useSchoolColor` (takes the spell school's color) · `backgroundColor` · `sweepColor` ·
`borderColor` · `textColor` · `readyFlashColor`

**`[effects]`**
`opacity` · `scale` · `fadeInTicks` · `fadeOutTicks` · `flashWhenReady` · `dimWhenOnCooldown`

> With `showSpellLevel` on, icons below ~18px don't have the height to fit both the timer and the
> level without overlapping (9px font, two texts). The level has a dark background exactly so it
> stays readable in that case.

Real cooldown on a lagging server
---------------------------------

Iron's Spells syncs the cooldown **once**, when it starts (`SyncCooldownPacket`); after that the
client decrements it on its own in `ClientPlayerEvents.onPlayerTick`. There is no periodic resync —
`PlayerCooldowns.syncToPlayer` only runs on login and respawn.

The client always runs at 20 ticks/s, but the server runs at its real TPS. On a 10-TPS server the
client's count runs twice as fast as the server's: **the number hits zero while the spell is still
on cooldown**, and the HUD ends up lying exactly when it matters most.

The fix (`data/ServerSyncedSource`) records, the first time it sees a cooldown, the *game time* at
which it ends — from then on the remaining time is always `end - gameTime`. When the client drops
the entry too early, the HUD keeps it on screen until the server actually finishes.

This is exact, not an estimate, because game time advances by 1 per server tick (the same pace as
the cooldown decrement) and the server corrects it on the client every 20 ticks via
`ClientboundSetTimePacket`. No mixin is needed: `ClientLevel.getGameTime()` is already the server's
authoritative clock.

The measured TPS (`client/ServerClock`, derived from the same source) does **not** enter that
calculation — it only converts the remaining ticks into seconds for display. The editor shows the
measured TPS in its footer when it drops below 19.5.

Toggle it off with `useServerTime`.

### Known limitation: `/tick freeze`

`/tick rate` is handled correctly: the client is told the new rate, so both game time and the local
decrement slow down together and the correction becomes a no-op. (This is also why `/tick rate`
**does not test** the correction: it's a synchronized slowdown, and the bug only shows up when the
server falls behind without the client knowing.)

`/tick freeze` is not: it freezes game time but **players keep ticking**, so the cooldown advances
on the server while our anchor stays put — the HUD then shows more time than the real value. It's a
debug command, not a production server condition, and the error is on the conservative side (too
much time, never too little), so it was accepted rather than handled.

Architecture
------------

The rule that holds the rest together: **`data/IronSpellsSource` is the only class that imports
`io.redspace.*`**. It translates the Iron's Spells API into `CooldownEntry`, a record that only uses
Minecraft types.

That buys three things at once: the editor runs a live preview on fake data (`data/DemoSource`)
without being in combat or even in a world; an Iron's Spells API change breaks compilation in a
single file; and another spell mod can be supported later by adding one `CooldownSource`, without
touching any renderer.

```
SpellCooldownHud            entry point, @Mod(dist = CLIENT)

config/                     ModConfigSpec + enums
    HudConfig, CachedColor, Anchor, GrowDirection,
    HudStyle, ContentMode, SortMode, TimerFormat

data/                       data, no Minecraft types beyond what's needed
    CooldownEntry           plain record (id, icon, name, ticks, school color)
    CooldownSource          interface
    IronSpellsSource        the ONLY class that talks to Iron's Spells
    ServerSyncedSource      decorator: corrects the cooldown against the server clock
    DemoSource              fake cooldowns for the editor preview

client/
    ClientEvents            game tick: clock, tracker, keybind
    ClientModEvents         GUI layer and key mapping registration
    ServerClock             game time + estimated TPS, no mixin
    CooldownTracker         animation state (fade, ready flash)
    HudLayout               anchor + offset + direction -> coordinates
    HudLayer                LayeredDraw.Layer, above the hotbar
    Keybinds
    render/                 RenderSupport + RadialRenderer, BarRenderer, TextListRenderer

screen/
    HudMoveScreen           transparent overlay, free dragging
    HudEditorScreen         full tabbed editor, with preview
    HudDragController       dragging/preview shared by both screens
    HudPreviewScreen        marks the screens that draw their own preview
```

### Implementation notes

Things learned the hard way, written down so they don't come back:

- **`Screen.render` calls `renderBackground` before the widgets**, and the default applies the
  vanilla blur plus the menu background. Since this mod's screens draw the preview before calling
  `super.render()`, that landed *on top of* the preview. Both override `renderBackground` as a no-op
  and draw their own background in the order they want.
- **The 9-argument `GuiGraphics.blit` does not scale** — it uses the same numbers for source and
  destination. Drawing a 16×16 icon at another size needs the 11-argument overload.
- **`GuiLayerManager` flattens the layer groups** and wraps each *vanilla* layer in the `hideGui`
  check. Mod layers inserted between them do **not** inherit that check, so F1 has to be checked by
  hand (`HudLayer.shouldRender`).
- **The radial sweep projects its vertices onto the square's edge**, not a circle's, to cover the
  icon all the way to the corners without spilling out the sides.
- **`neo_version` ≠ `neo_version_range`**: the former is the compile version, the latter the runtime
  minimum. Declaring the compile version as the minimum makes the mod get rejected on slightly older
  instances of the same 21.1.x line.

Building
--------

The project targets four Minecraft/loader pairs from **one source tree** with
[Stonecutter](https://stonecutter.kikugie.dev/): NeoForge 1.21.1 and 1.21 (modern ModDevGradle,
**JDK 21**), plus Forge and NeoForge 1.20.1 (ModDevGradle legacy, **JDK 17**). Version differences
live in `//?` preprocessor comments; Gradle's toolchain support fetches the JDK each node needs.

The Iron's Spells API is resolved from Modrinth (`maven.modrinth:irons-spells-n-spellbooks`) per
version, so a clean clone (CI included) builds with no manual step.

```
./gradlew build                    # builds ALL version nodes
./gradlew :1.21.1:build            # a single node
./gradlew :1.20.1-forge:build      # Forge 1.20.1
./gradlew :1.20.1-neoforge:build   # NeoForge 1.20.1
```

Each node's jar lands in `versions/<node>/build/libs/` as
`spellcooldownhud-<mc>-<loader>-<version>.jar`. The active node for `runClient` and IDE sync is set
in `stonecutter.gradle.kts` (`stonecutter active "..."`).

```
./gradlew :1.21.1:runClient          # dev client with Iron's Spells loaded (see libs/ below)
./gradlew :1.21.1:deployToInstance   # copies the jar into a CurseForge instance's mods/ folder
```

Point `deployToInstance` at an instance by setting `instance_mods_dir` in your
`~/.gradle/gradle.properties` instead of editing the build script.

### `libs/` — only for `runClient`

The dev client runs Iron's Spells for real, and for that it needs its jar and 5 dependencies.
Without them `build` still works fine; only `runClient` comes up with no spells.

`libs/*.jar` is in `.gitignore` — we don't redistribute third-party mods, which is why compilation
uses Modrinth: a clean checkout (CI included) has nothing local to compile against. To populate it
for the **active node** (`stonecutter.gradle.kts`), copy the matching jars from a CurseForge
instance. For the default `1.21.1` node these are (Iron's Spells versions come from the `nodeSpec`
block in `build.gradle.kts`, the rest from `gradle.properties`):

```
irons_spellbooks-1.21.1-3.16.2.jar
irons_lib-1.21.1-2.1.0.jar
geckolib-neoforge-1.21.1-4.8.3.jar
player-animation-lib-forge-2.0.4+1.21.1.jar
curios-neoforge-9.5.1+1.21.1.jar
```

A `1.20.1` dev client would instead need the Forge/NeoForge 1.20.1 builds of those mods; the
production jars run fine, but the legacy dev client may need remapping, so testing a built jar in a
real 1.20.1 instance is the reliable path.

Also `libs/irons_spellbooks_at.cfg`: a **sanitized** copy of the access transformer bundled inside
the Iron's Spells jar, registered in `build.gradle` via `neoForge.accessTransformers`. ModDevGradle
applies ATs to the Minecraft artifact at build time, and a runtime-classpath-only mod's AT isn't
applied on its own — without it Iron's Spells crashes with `IllegalAccessError` at boot. Two lines
of the original were dropped for using legacy SRG names (`f_59605_`, `f_129744_`) that don't exist
under Mojang mappings; regenerate and re-apply that removal when updating Iron's Spells. The AT is
only registered when `libs/` is complete, so a clean build doesn't break.

Bug reports
-----------

Found a bug or want to help translate? Open an issue on GitHub. For translations, include the
`<code>.json` file (copy `en_us.json`, keep the keys, translate the values). By submitting a
contribution you agree it may be included in the mod under the project's license.

License
-------

**All Rights Reserved** — see [LICENSE](LICENSE). The source is published for reference and bug
reports only; you may not redistribute, re-upload or create derivative works without written
permission. The official download is the project's CurseForge page. The Mojang mappings used at
compile time have their own license: https://github.com/NeoForged/NeoForm/blob/main/Mojang.md
