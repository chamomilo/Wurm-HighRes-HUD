# Shared updater and buttons

The updater and its UI live entirely in the canonical `C:/projects/updater`
reference. High-res HUD 0.2.3 embeds module 1.1.0, including the reviewed
Chamomilo UI 0.4.4 SDK. Product code retains only updater lifecycle calls and
metadata; the consumer build launcher cleanly rebuilds, synchronizes and verifies
the complete module before packaging.

Updater artwork, grouped fonts and native input are maintained by the reference.
Runtime textures repeat in both axes and preserve fixed corners; ordinary
rendering uses component alpha 1.0f. Private updater artwork, implementations
and tests from the old consumer copy have been removed.

Read `C:/projects/updater/docs/INTEGRATION.md` for integration and
`C:/projects/interface items/docs/BUTTONS-TYPOGRAPHY.md` / `BUTTON-GROUPS.md`
for the shared button contract. Consumer compact groups and geometry are recorded
in [UI-MIGRATION.md](UI-MIGRATION.md) and `button-groups.json`.
