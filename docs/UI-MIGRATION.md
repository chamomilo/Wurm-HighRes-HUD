# Chamomilo UI migration

High-res HUD 0.2.3 embeds the canonical updater module 1.1.0, which pins the
reviewed Chamomilo UI SDK 0.4.4. The consumer contains no private updater or SDK
source copies. `chamomilo-updater.lock.properties` records the module version,
protocol and exact reference JAR SHA-256; release gates verify its bytes and
entry set inside both the mod JAR and the installable ZIP.

## Compact geometry

| Element | Previous | Chamomilo UI |
| --- | --- | --- |
| Healthbar body | 323 × 108 | 323 × 108 |
| Healthbar title/ride/footer heights | Dynamic 22/42/62, 20 per ride row, 18 footer | Unchanged |
| Select bar body | 438 × 108 | 438 × 108 |
| Select distance footer | 18 | 18 |
| Fighting HUD | 656 × 258 | 676 × 282 |
| Sleep action | 76 × 14 | 76 × 14 |
| Disembark action | 76 × 16 | 76 × 16 |
| No target action | — | 96 × 19 inside the existing portrait header |
| Combat focus action | Native icon | 126 × 24 inside the existing target card |
| Position block | 236 × 38 | 236 × 46 inside the unchanged window |
| Range/footing indicators | 18 square | 24 square |
| Select action icons | 28 × 28 | 28 × 28 |
| Combat stance/mode hit areas | 38/28 square | Unchanged |
| Special move buttons | 24 square icon hit areas | 34 × 34 buttons with 24 px native foregrounds |
| Portrait contour | 3 px | 6 px, widened inward |
| Healthbar exterior contour | 3 px | 5 px, widened inward |

The HUD intentionally uses `UiScale.BASE` for panels at every resolution to
retain its established footprint. Compact controls use an explicit 0.5 artwork
scale. They do not inherit the SDK's 32 px standard window button height.
Native combat icon foregrounds are inset inside their complete button rectangles;
their commands, availability, stance difficulty colors and hints remain native.

`HudSkin` uses the shared repeating frame, leather/walnut materials and
`UiFrameGrid` dividers. Existing value colors, portrait interaction, title
visibility settings and saved position keys are retained. Gauge effects keep
their own documented value/hit animation; ordinary textures, text and native
children ignore HUD transition alpha.

The value and glass layers now use `UiFrameGrid.cell(...)` bounds without the
old extra gauge inset. The canonical glass covers the entire well, including
the unfilled material at zero value. The portrait owns the shared vertical
rail; the adjacent grid omits its left rail to retain one contour.
Healthbar cells are clipped to its wider exterior contour while retaining the
same shared dividers and panel footprint. Caption and button bounds follow the
resulting wells.

Select quick actions begin exactly at the shelf's inner left edge, without an
extra inset. The unchanged 28 px buttons touch without overlapping; ten actions per page fit
beside the pager; every remaining action remains available on subsequent pages.
Their atlas UVs exclude the old
3 px contour, leaving Chamomilo UI as the only button frame. Press offsets the
22 px icon by one pixel without rescaling it; hotkey captions follow the same
offset. Rendering and pointer targets share the same 28 px stride. A shared
edge belongs only to the button on its right, so clicks cannot target two actions.

Fighting HUD grows by 20 px in width and 24 px in height to 676 × 282, giving
Special moves room for larger buttons and clear frame spacing. Healthbar and
Select keep their established dimensions. No target fills the former blank
portrait header. The target information grid extends to the existing combat
column, uses exact cells and full-well glass, and omits the progress lane when
idle. The relation row no longer repeats the target name. Combat focus is a
separate captioned control beside level/readiness. The combat columns begin
near the top rail; Position and Special moves have separate labelled frames
with an 8 px gap. Six Special moves controls use the actual Chamomilo button
motion: hover highlights the native foreground, press displaces it by 1 px,
release activates once and dragging away cancels. The face surrounds the icon
with 5 px of visible artwork; disabled states keep their hints and cannot fire.
Position borrows 8 px from the unused lower space of the combat columns, keeping
the outer window unchanged. Its labels use 14 px Bold body text, its heading uses
14 px Alegreya Sans SC, and its native indicators use 24 px foregrounds.
Distance has a separate 36 px numeric field measured against four digits in
the shared font. It displays the current horizontal target distance as an
integer without a unit suffix, even before fighting starts. The native range
and footing indicators retain their own distinct places and hover hints.
Weapon/shield statistics remain in the analysis card.

Combat focus, its level and readiness share one centered text row. Analysis uses
an 11 px inset and advances each line by its actual font height plus a 4 px gap,
so its heading and all five following lines remain inside the existing frame.

BEST and its yellow attack-zone outline rank only native attack buttons whose
server-controlled `hidden` flag permits selection. Availability is read for each
render, including weapon changes; inactive combat, stun and an empty allowed set
produce no recommendation. Availability does not discard learned observations.

The Focus level/message, native stun/stance restrictions and granted specials
come from existing client state/events. The client lacks the server combat-round
counter. Initial engagement is estimated from spaced combat-options packets,
shown with `~`, and reset by the server's rejection message. A positive level
or server-granted weapon special proves engagement; a click is an attempt,
not a prediction of success. No server mod or automatic Focus attempts are used.

## Captions

Groups are recorded in `button-groups.json`. Sleep bonus variants are measured
together and receive one supported Alegreya Sans SC size and ink baseline,
including both Regular and Bold. Disembark has a separate singleton group
because its control height differs. The page indicator reserves all page-count
variants at one shared size, capped at 12 px. Full captions remain in hints.
Native icon actions and small hotkey badges are not captioned typography groups.
No target and Combat focus have distinct singleton groups because their heights
and task blocks differ. Ordinary HUD text uses the kit's Alegreya Sans, and
titles use Alegreya Sans SC. `HudTextFonts` shares the native cached atlases and
adapts SDK baselines to the existing HUD lower-edge coordinates, preserving case.

Compact action buttons participate in native HUD pointer routing as child
components. Their actions complete on release inside the control. Dragging away,
disabling, or losing the pointer cancels an armed action. The kit supplies the
press/release and hover motion, unchanged background on hover, real Bold text
and warm yellow caption color.

## Verification

`verifyHudUi` exercises the production compact button against the pinned Wurm
component classes, substituting only font/GPU initialization. It checks release
activation, disabled input, drag cancellation and identical pixels at incoming
alpha 0, 0.2, 0.65 and 1. It renders `build/hud-ui-preview.png` using the real
production painters, compact artwork and fonts. Portrait/game data in that
contact sheet are placeholders, not a live game screenshot.

The probe also executes the native Healthbar, Select bar and Fighting HUD gauge painters at
0%, 50% and 100%, comparing the complete slot and glass layers. JUnit checks
the exact grid geometry and dispatches Disembark through the real controller
and native single-target HUD overload with an offline transport fixture.
The native action-row probe uses the actual resource-pack atlas, checking UV
crops, slot centring, stable pressed icon size and matching pointer targets.
It checks shared-edge clicks, full/partial action pages and native Healthbar labels with
the bundled fonts. The native interaction fixture exercises combat-target
handoff (including the selection latch, pin and corpse cases), No target and
Focus transport, pending/max Focus, and unavailable/granted/stunned special moves.
It also changes native attack availability from all zones to the unarmed grid,
restores it and disables every zone, checking BEST after each transition. The
preview runs the production Focus, Position and analysis painters with measured
text bounds, a shared Focus baseline and non-overlapping four-digit distance,
labels and enlarged icons. Model tests include learned ranking under restrictions.

The reference build independently validates updater layout/input, popup alpha,
scrolling, shared-loader order and packaging. A live Wurm GPU/input check remains
manual. No build step installs files into a game directory.
