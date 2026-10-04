# Architecture

## Runtime boundary

`HighResHudMod` is the only mod-loader entry point and
`UnifiedHookInstaller` is the only owner of bytecode changes. Injected Wurm
methods call `HighResHudRuntime`; component failures are logged and fail open
to the vanilla client.

The Healthbar, Select bar, and Fighting HUD packages are internal component
adapters. They do not implement mod-loader interfaces and do not install hooks;
only `HighResHudMod` and `UnifiedHookInstaller` own those responsibilities.

## Shared core API

`HighResHudApi.core()` exposes versioned, Wurm-independent coordination
services through the shared class loader:

- lifecycle stages for HUD/session readiness;
- `ActionBus` with `USER`, `HUD`, `INTERNAL`, `KEYBINDER`, `WAYPOINTER` and
  `THIRD_PERSON` origins;
- `MessageRouter`, which always fans out before deciding whether to consume;
- priority `InputRouter` for later camera, key and map integrations;
- `ExamineBroker` for coalesced, serialized hidden queries.

Keybinder should subscribe to `ActionBus` and ignore or separately account for
`INTERNAL` actions. Waypointer and Third Person can subscribe to the lifecycle
and input contracts instead of patching the same Wurm methods again.

## Portrait pipeline

There may be four character representations visible at once:

1. the avatar rendered in the 3D world;
2. the player's Healthbar portrait;
3. the Selectbar portrait;
4. the Fighting HUD portrait.

The world avatar stays in the world renderer and is not an interchangeable HUD
texture. The other three are coordinated by `SharedPortraitCoordinator`.
Each request is keyed by subject, model resource, render size, camera/framing
profile, rotation and FOV. Compatible Selectbar/Fighting requests share one GPU
job and one texture. Different subjects or different manually selected views
remain separate passes. Healthbar currently uses Wurm's `PaperDollRenderer`, so
it participates in the same frame queue but keeps a distinct, correct pass.

The shared model portrait implementation and profile database are compiled
only once. Per-frame counters report requested, executed and saved passes.

## Updates

`HighResHudMod` participates in the shared Chamomilo protocol-1 update
coordinator. It registers as the update UI host during initialization, forwards
mod metadata through `ModListener`, and starts one release check after the first
HUD becomes ready. The update window is created on Wurm's render thread and
only opens the release URL after an explicit Download click.

## Hidden queries

Wurm Examine text has no target/request id. The compatibility gate therefore
allows only one unrelated hidden target at a time. Panels requesting the same
target join the active query and all receive its lines. A visible user Examine
cancels silent ownership so the reply cannot disappear. The public
`ExamineBroker` is the destination for the remaining component-specific retry
state in the next migration stage.
