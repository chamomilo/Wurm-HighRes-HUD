# Wurm High-res HUD

[![Latest release](https://img.shields.io/github/v/release/chamomilo/Wurm-HighRes-HUD?display_name=tag)](https://github.com/chamomilo/Wurm-HighRes-HUD/releases/latest)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

Wurm High-res HUD is a client-side HUD replacement for **Wurm Unlimited**. It
packages a high-resolution Healthbar, Select bar, and Fighting HUD as one mod:
one loader entry point, one resource pack, and one coordinated hook/runtime
layer.

No server mod is required.

Version 0.2.3 uses Chamomilo UI 0.4.4. Healthbar and Select retain their compact
dimensions; Fighting HUD grows slightly to 676 × 282 for 34 px Special moves buttons.
Walnut/leather materials, shared frames and wooden action buttons come from the
reviewed SDK. Compact captions use bundled Alegreya Sans SC Regular/Bold;
Sleep bonus labels share their font size and baseline across both states.
All HUD text now uses shared Alegreya Sans body fonts and Alegreya Sans SC
titles/buttons, retaining the existing compact geometry.

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
- Touching action buttons aligned with the shelf edge, with hotkeys and ten actions per page.
- Select closes as soon as the same creature becomes the displayed combat target.
- Pin and close controls; an unpinned bar closes five seconds after the pointer
  leaves it.
- Drag a portrait to rotate it, use the wheel to zoom, and right-click for the
  native Wurm context menu.

### Fighting HUD

- Target portrait and health alongside Wurm's native combat controls and
  attack zones.
- Drag/wheel portrait controls and double-click-to-clear targeting.
- **No target** button above the portrait.
- **Combat focus** button with the server's exact level/message and a readiness
  status on one line. `~` marks the estimated initial engagement gate; server feedback
  remains authoritative. The control disables while unavailable or focusing.
- Separate **Position** (distance/footing) and **Special moves** blocks with
  an 8 px frame gap and hover explanations. Special moves are animated buttons
  and activate only when granted by the server. Target distance is shown as a
  whole number without a unit suffix, with room for four digits.
- Larger Position labels and 24 px range/footing indicators fit inside the
  existing window. Analysis text has a clear inset from its frame.
- Complete Chamomilo health/progress wells and glass; the progress row appears
  only during an action, and the target name appears once.
- Optional per-character combat knowledge learned from combat and Examine
  text, including confidence ranges and inferred damage information.
- BEST ranks only currently available attack zones and updates with weapon
  restrictions. When no zone can be selected, it offers no attack recommendation.

## Requirements

- Wurm Unlimited client.
- [Ago's Wurm Unlimited Client Mod Launcher](https://github.com/ago1024/WurmClientModLauncher).
- The Java runtime used by Wurm/Client Mod Launcher (the project targets Java
  8 bytecode).

Do not enable the old standalone `highres-healthbar`, `highres-selectbar`, or
`highres-fighting-hud` mods at the same time. This unified package already
contains all three panels and owns their hooks.

## Installation

1. Download `highres-hud-0.2.3.zip` from the
   [latest release](https://github.com/chamomilo/Wurm-HighRes-HUD/releases/latest).
2. Close Wurm Unlimited.
3. Extract the archive into the `WurmLauncher` directory, merging its `mods`
   folder with the existing one.
4. Confirm these paths exist:
   - `WurmLauncher/mods/highres-hud.properties`
   - `WurmLauncher/mods/highres-hud/highres-hud-0.2.3.jar`
   - `WurmLauncher/mods/highres-hud/highres-hud-resources-0.2.3.jar`
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

After the HUD is ready, the canonical shared updater opens one Chamomilo mod
registry for all participating mods. It remains available through the native
Mod updates menu, with a saved startup visibility preference. **Download**
opens the release page; **Latest** marks an up-to-date installation.
ZIP installation remains manual. This mod embeds reference module 1.1.0 and
its reviewed Chamomilo UI 0.4.4 SDK as one verified module.

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
.\gradlew.bat clean build dist
```

The installable archive is created in `build/distributions`. The build cleanly
rebuilds `C:/projects/updater` (override with `-PchamomiloUpdaterReference=...`),
checks its module version and SHA-256, then verifies exact reference entries in
the consumer JAR and ZIP. It also verifies native compact button input, fixed
alpha, all three panels and the resource pack. `build/hud-ui-preview.png` is an
offline production-artwork preview with placeholder game data.

See [ARCHITECTURE.md](ARCHITECTURE.md) for the runtime contracts and portrait
scheduling model, and [CHANGELOG.md](CHANGELOG.md) for release history.

## License

Wurm High-res HUD is released under the [GNU General Public License v3.0](LICENSE).

## Chamomilo versions

High-res HUD 0.2.3 embeds updater module 1.1.0 (protocol 1) and Chamomilo UI 0.4.4.
See [UI migration](docs/UI-MIGRATION.md) for geometry, typography and verification.
