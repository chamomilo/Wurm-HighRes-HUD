# Wurm High-res HUD

[![Latest release](https://img.shields.io/github/v/release/chamomilo/Wurm-HighRes-HUD?display_name=tag)](https://github.com/chamomilo/Wurm-HighRes-HUD/releases/latest)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

Wurm High-res HUD is a client-side HUD replacement for **Wurm Unlimited**. It
packages a high-resolution Healthbar, Select bar, and Fighting HUD as one mod:
one loader entry point, one resource pack, and one coordinated hook/runtime
layer.

No server mod is required.

## Highlights

### Healthbar

- Large live player portrait with click-to-change pose and wheel zoom.
- Health, stamina, water, food, nutrition, CCFP, favor, fatigue, speed, FPS,
  active effects, vehicle status, and hitched-animal details.
- A right-aligned **Activate/Deactivate** Sleep bonus button.
- A live countdown until Sleep bonus can be activated again. Deactivation
  remains available during the activation cooldown.
- A Disembark button while mounted and a context menu for display options.

### Select bar

- High-resolution portraits for creatures, items, terrain, and caves.
- Name, quality/damage or creature status, distance, and action progress.
- Action buttons with hotkeys and paging when the list is longer than one row.
- Pin and close controls; an unpinned bar closes five seconds after the pointer
  leaves it.
- Drag a portrait to rotate it, use the wheel to zoom, and right-click for the
  native Wurm context menu.

### Fighting HUD

- Target portrait and health alongside Wurm's native combat controls and
  attack zones.
- Drag/wheel portrait controls and double-click-to-clear targeting.
- Optional per-character combat knowledge learned from combat and Examine
  text, including confidence ranges and inferred damage information.

## Requirements

- Wurm Unlimited client.
- [Ago's Wurm Unlimited Client Mod Launcher](https://github.com/ago1024/WurmClientModLauncher).
- The Java runtime used by Wurm/Client Mod Launcher (the project targets Java
  8 bytecode).

Do not enable the old standalone `highres-healthbar`, `highres-selectbar`, or
`highres-fighting-hud` mods at the same time. This unified package already
contains all three panels and owns their hooks.

## Installation

1. Download `highres-hud-0.1.2.zip` from the
   [latest release](https://github.com/chamomilo/Wurm-HighRes-HUD/releases/latest).
2. Close Wurm Unlimited.
3. Extract the archive into the `WurmLauncher` directory, merging its `mods`
   folder with the existing one.
4. Confirm these paths exist:
   - `WurmLauncher/mods/highres-hud.properties`
   - `WurmLauncher/mods/highres-hud/highres-hud-0.1.2.jar`
   - `WurmLauncher/mods/highres-hud/highres-hud-resources-0.1.2.jar`
5. Disable the three standalone High-res HUD mods if they are present, then
   start the game through Client Mod Launcher.

Existing window positions are retained because the panels keep their original
Wurm save-position keys.

## Controls

| Area | Control |
| --- | --- |
| Healthbar portrait | Left-click changes the pose; mouse wheel zooms |
| Healthbar | Right-click opens name, title, numeric-value, ride, and hitched-animal options |
| Sleep bonus row | `Activate`/`Deactivate` toggles `/fsleep`; the adjacent timer is the activation cooldown |
| Select portrait | Drag rotates; mouse wheel zooms; right-click opens the native object menu |
| Select actions | Click the page indicator or use the wheel over the action shelf to change pages |
| Select Pin | Keeps the bar open; when unpinned it auto-closes after the pointer leaves |
| Fighting portrait | Drag rotates; wheel zooms; double-click clears target; right-click opens target actions |

## Configuration

Edit `mods/highres-hud.properties` while the client is closed. Defaults are
shown below.

| Property | Default | Purpose |
| --- | ---: | --- |
| `healthbar.enabledByDefault` | `true` | Enable the Healthbar on first use |
| `selectbar.enabledByDefault` | `true` | Enable the Select bar on first use |
| `fightingHud.enabledByDefault` | `true` | Enable the Fighting HUD on first use |
| `healthbar.portraitRenderSize` | `256` | Player portrait render resolution, 128-1024 |
| `healthbar.portraitRotation` | `45` | Initial player portrait angle, -90 to 90 degrees |
| `healthbar.portraitFov` | `45` | Player portrait field of view, 45-60 degrees |
| `healthbar.portraitCropWidth` | `0.40` | Horizontal crop, 0.25-1.0 |
| `healthbar.portraitCropHeight` | `0.42` | Vertical crop, 0.25-1.0 |
| `selectbar.portraitRenderSize` | `512` | Selected-subject portrait resolution, 256-1024 |
| `fightingHud.portraitRenderSize` | `512` | Combat-target portrait resolution, 256-1024 |
| `portraitProfilesOverride` | `mods/highres-hud/portrait-profiles.properties` | Optional custom portrait framing profiles |
| `healthbar.fatigueRefreshSeconds` | `60` | Background fatigue refresh period, 30-600 seconds |
| `healthbar.fatigueInitialDelaySeconds` | `4` | Delay before the first fatigue refresh, 0-60 seconds |
| `healthbar.animateRisingGauges` | `true` | Animate Healthbar gauge increases |
| `selectbar.animateGaugeShine` | `true` | Animate Select bar gauge highlights |
| `fightingHud.animateGaugeShine` | `true` | Animate Fighting HUD gauge highlights |
| `fightingHud.collectCombatKnowledge` | `true` | Learn target statistics from local client text |
| `fightingHud.knowledgeDirectory` | `mods/highres-hud/knowledge` | Directory for per-character learned data |

The bundled portrait profile reference is installed as
`mods/highres-hud/portrait-profiles.bundled.properties`. Copy it to the
`portraitProfilesOverride` path before customizing it.

## Client activity and updates

The mod is client-only. To populate HUD-only information it can issue quiet
client commands such as `/fatigue`, `/titles`, and serialized Examine requests;
visible player Examine actions always take priority. Learned combat data stays
in the configured local knowledge directory.

After the HUD is ready, the shared updater checks this repository's latest
GitHub release once per client process. When an update exists, Wurm shows a
small notification window. The browser opens only if **Download** is clicked;
installation remains manual.

## Building from source

Wurm's proprietary client libraries are deliberately not committed. Copy
`local.properties.example` to `local.properties` and point
`wurmClientLibDir` at a directory containing:

- `client-patched.jar`
- `common.jar`
- `javassist.jar`
- `modlauncher.jar`

Then run:

```powershell
.\gradlew.bat clean test verifyArtifacts dist
```

The installable archive is created in `build/distributions`. The build verifies
the unified entry point, updater metadata/classes, all three panels, and the
combined resource pack before producing the ZIP.

See [ARCHITECTURE.md](ARCHITECTURE.md) for the runtime contracts and portrait
scheduling model, and [CHANGELOG.md](CHANGELOG.md) for release history.

## License

Wurm High-res HUD is released under the [GNU General Public License v3.0](LICENSE).

## Chamomilo versions

Version 0.1.2 embeds the shared Chamomilo updater. A versions window opens at every launch after the HUD is ready and lists all mods from the public GitHub catalogue, including disabled and absent installations. UPDATE opens a newer installed release; INSTALL opens a release for an absent mod. ZIP installation remains manual. The public catalogue is refreshed without requiring new client binaries; a verified copy is retained for offline startup. All Chamomilo updater copies share one window, with a thin high-resolution wood-and-metal frame.
