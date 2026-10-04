# Changelog

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
