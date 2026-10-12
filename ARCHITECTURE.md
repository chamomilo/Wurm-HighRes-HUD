# Architecture

## Runtime boundary

`HighResHudMod` is the only mod-loader entry point and
`UnifiedHookInstaller` is the only owner of bytecode changes. Injected Wurm
methods call `HighResHudRuntime`; component failures are logged and fail open
to the vanilla client.

The Healthbar, Select bar, and Fighting HUD packages are internal component
adapters. They do not implement mod-loader interfaces and do not install hooks;
only `HighResHudMod` and `UnifiedHookInstaller` own those responsibilities.

`HighResWindowIntegration` attaches Inventory, Events and Chat presentation
adapters after native HUD initialization. It retains original registered window
instances rather than redirecting incoming game data to a second model. Their
native save keys, handlers, chat input and manager topology stay intact. Hooks
are scoped by component identity/ancestry to those windows and their Inventory
header. Native `ItemListWindow` server containers attach after `addInventoryWindow`;
player backpacks/quivers use `InventoryContainerWindow` and attach after the
HUD's native add/toggle methods. Readiness and frame scans cover both maps. Removal and
reconnect release all associated adapter, heading and scrollbar identities.
They retain the native close notification, listener destruction and saved
geometry. Other client windows keep their native presentation. A new HUD clears
adapter, header and scrollbar identity maps; repeated readiness is idempotent.

Window hooks target still-unloaded HUD/renderer call sites. Native GUI base
classes may already be defined by Avatar2 or Keybinder; they are never rewritten.
A FlexComponent drawing host supplies native scissor push/pop, while heading
and input decorators keep the original children and their listeners. Native HUD
release routing validates shaped tabs and prepares sort direction before the
original completed click. HUD press/move routing exposes the native SouthBar
resize corner; native drag/release retains saved window geometry. Channel strips
wrap ordered tabs into as many shared-typography rows as needed and move the body
below the last row. Compact uses distinct canonical SDK entry points to
remain usable when another mod has already loaded an older shared v1 SDK.

HighResDataTypography applies the shared Alegreya Sans data family only inside
migrated windows. Native table changeText updates row and hit-test metrics;
history wrapping recomputes advances without replacing source message identities,
colors or case, preserving selection and scroll anchors. HudTextFonts adapts
the shared baseline fonts to native lower-edge coordinates and reuses atlas caches.
The original editor keeps its caret/clipboard metrics and global settings remain
owned by the client.

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
