# Changelog

## 0.2.3 — 2026-10-10

- Rank only attack zones currently available in the native client. BEST and its highlight choose the next best allowed zone when a weapon disables head attacks, update immediately when availability changes, and show no recommendation when every attack is unavailable. Stored combat observations remain intact.
- Put Combat focus, its level and readiness on one line inside the existing target card.
- Enlarge Position labels to 14 px and range/footing indicators to 24 px, expanding its frame inward while retaining the 676 × 282 window and room for four distance digits.
- Keep all six analysis lines within their frame with an 11 px inset and measured row spacing.
- Embed and verify the complete current reference updater 1.1.0 with the pinned Chamomilo UI 0.4.4 SDK.

## 0.2.2 — 2026-10-10

- Make all six Special moves controls Chamomilo icon buttons with visible faces, hover highlighting, press/release motion and drag cancellation; preserve native commands and server readiness.
- Grow Fighting HUD from 656 × 258 to 676 × 282 for 34 px Special moves buttons with 24 px native foregrounds; retain 8 px gaps between the combat, Position and Special moves frames.
- Display live target distance as an integer without a unit suffix, reserving a separate four-digit field before the native range and footing indicators.
- Audit Healthbar, Select and Fighting HUD text against the shared Chamomilo font roles, including numeric distance and unavailable-control text.
- Embed the complete current reference updater 1.0.3, including its Chamomilo typography fixes and exact JAR/ZIP verification.

## 0.2.1 — 2026-10-10

- Move the first Select quick-action button 6 px left to the shelf's inner boundary.
- Place subsequent 28 px buttons at an exact 28 px stride, with no gaps or overlapping paint/input rectangles.
- Restore ten actions per page inside the unchanged shelf and verify both sides of shared button edges, the complete row and paging.

## 0.2.0 — 2026-10-10

- Add a dedicated Combat focus button, exact server level/message and a readiness status. Disable attempts while focusing, pending, at maximum level, stunned, prone/open, or outside combat; initial engagement is explicitly marked as an estimate and corrected by server feedback.
- Separate distance/footing into Position and Shield bash/server-granted weapon moves into Special moves, with explanatory native hover hints and correct unavailable/stun input handling.
- Add No target above the portrait, using the native combat command and the existing double-click clear behavior.
- Close Select immediately when the same selected creature becomes the displayed combat target, clearing the portrait latch while preserving unrelated objects and corpse selections.
- Fit Fighting HUD health/progress to Chamomilo frame cells with full-well glass, remove the inactive progress row and repeated target name, and widen the target card into existing space. Retain the complete 656 × 258 footprint.
- Align Select actions with the information text and add 3 px gaps; nine buttons per page retain access to every action without enlarging the shelf.
- Replace system SansSerif HUD text with shared Alegreya Sans body fonts and Alegreya Sans SC titles/buttons, preserving native text placement and compact geometry.
- Validate native target transfer, No target/Focus transport, server-granted special input, focus state, action paging and all three gauge painters before packaging.

## 0.1.6 — 2026-10-10

- Increase the Healthbar's exterior contour from 3 to 5 px for better visibility, retaining its 323 × 108 body, 6 px portrait contour and 3 px shared dividers.
- Keep values, glass, captions and the Sleep button inside the wider contour without enlarging the window.
- Align Select bar quick actions with their shelf, sample icons without their old atlas contour and preserve icon size while pressing.
- Keep hotkey captions anchored to their buttons and check real atlas rendering plus click bounds in the native probe.

## 0.1.5 — 2026-10-10

- Target the current mount or vehicle for Disembark instead of a surface tile at level zero, avoiding the wrong floor/layer target indoors and in caves.
- Double portrait frame thickness from 3 to 6 px without enlarging any panel or portrait viewport.
- Fit Healthbar and Select bar values to the exact Chamomilo UI grid cells, removing the extra legacy inset.
- Apply the canonical glass layer across each complete gauge well, including its unfilled portion, with explicit blended rendering and fixed component opacity.
- Add native dispatch and gauge-rendering regression checks before packaging.

## 0.1.4 — 2026-10-09

- Migrate Healthbar, Select bar and Fighting HUD to the reviewed Chamomilo UI 0.3.4 artwork, tiled materials and shared frames.
- Retain the existing panel, portrait, gauge, button and hit-area dimensions; compact HUD controls use explicit scaling rather than growing to standard window defaults.
- Fit Alegreya Sans SC captions in both weights, sharing size and ink baseline across Sleep bonus states; hover uses real Bold with warm yellow text.
- Add compact button release activation, disabled-state protection and drag cancellation, with stable render alpha.
- Remove the previous private updater copy and embed reference module 1.0.2 from the canonical updater project, including its SDK, SHA-256 lock and exact JAR/ZIP entry verification.
- Verify native compact button input, fixed-alpha rendering and a production-artwork layout preview before packaging.

## 0.1.3

- Replace the previous updater with the current shared registry, native Mod updates menu and saved startup preference.
- Reuse the original button artwork with complete top/bottom borders and stable horizontal stretching.
- Verify native layout, input, HUD replacement and shared updater packaging before distribution.

## 0.1.1 — 2026-10-04

- Added a right-aligned Sleep bonus `Activate`/`Deactivate` toggle to the
  Healthbar and a live countdown until activation is available again.
- Preserved deactivation during the server activation cooldown and synchronized
  the control with player state and server messages.
- Removed obsolete standalone mod-loader and hook-installation paths from the
  three internal panel adapters; the release now has one entry point and one
  hook owner.
- Added the shared Chamomilo update coordinator and in-game update window.
- Added release-content checks, a complete public README, and a clean unified
  install archive.

## 0.1.0

- Created one High-res HUD distribution containing Healthbar, Select bar, and
  Fighting HUD.
- Replaced three overlapping hook installers with `UnifiedHookInstaller` and a
  fail-open runtime dispatcher.
- Combined the three resource packs and load the result once.
- Added a public core API for lifecycle, actions, messages, input, and hidden
  Examine coordination, ready for Keybinder, Waypointer, and Third Person.
- Tagged HUD/internal action origins so technical actions are distinguishable
  from user input.
- Serialized unrelated hidden Examine probes and coalesced same-target probes.
- Unified portrait model/profile/render classes, coordinated all HUD portrait
  jobs, and deduplicated compatible Select bar/Fighting HUD portraits.
- Unified action-progress state and creature attitude cache.
