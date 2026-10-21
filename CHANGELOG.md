# Changelog

All notable changes to Spell Cooldown HUD are documented here.
This project follows [Semantic Versioning](https://semver.org/).

## 0.2.0

A big update: a brand-new Active Effects HUD, and the two HUDs are now fully independent.

### Added
- **Active Effects HUD** — a second, separate HUD that shows your active Iron's Spells effects
  (buffs and debuffs like Heartstop, Oakskin, Charge, and so on) with their remaining duration.
  Off by default; turn it on in the config (Content tab → "Active effects").
- **Two fully independent HUDs** — the cooldown HUD and the effects HUD each have their own size,
  style, colors, badges, animations, and position. Make the effects HUD bigger, or move it to the
  other side of the screen, without touching the cooldown HUD.
- **Mana cost label** — optionally show a spell's mana cost in the corner of its icon.
- **Redesigned editor** — a group switch at the top ("Editing: Cooldowns / Effects") lets you tune
  each HUD on its own, with a live preview of the selected one.
- **Move screen shows both HUDs** at once, each labeled and color-coded (cooldowns in blue, effects
  in amber), so you can drag each group to its own spot.

### Changed
- The config file is reorganized into independent `[cooldowns.*]` and `[effects.*]` sections, each
  with its own layout / style / colors / effects. A few truly shared switches (hide on F1, and the
  cooldown-only content mode and server-time correction) stay at the top level.

### Notes
- Because of the config restructure, **HUD settings reset to their defaults when you update.** Just
  reopen the in-game editor and set them up again — it only happens this once.

### Languages
- English, Português (Brasil), Español (España), Español (México), Русский.

## 0.1.1

- Initial public release: the spell cooldown HUD for Iron's Spells 'n Spellbooks, with the in-game
  editor (styles, colors, layout, position) and a live preview.
