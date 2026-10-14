# High-res Build menu

Open the crafting screen with its existing key (default B), then click Choose recipe
under the title at the upper left. Material selection sits below the result model.
The button opens the existing recipe window
and restores it when collapsed. The native console command `toggle creationlist`
also toggles this window; the default console key is F1.

The browser uses the bundled Sklotopolis Novus snapshot: 2702 recipes, captured
on 2026-10-14 at 02:11 Moscow time, server 176.9.149.249. Their names, skills,
icons, categories, starting components and additional quantities came from the
live client's complete server catalogue. The offline importer resolves standard
template IDs, masses and flags before packaging. Runtime reads the prepared
descriptors once; filtering uses cached recipe/component text.

Automatic comparison and refresh from the incoming server catalogue are disabled.
Updates are manual. The browser is ready immediately, including while the
server catalogue is missing or different. Its editor and scrollbars use the
shared native SDK. Live unfinished requirements still update after actions.
Search in name is initially selected and matches a partial result name only.
Search in material matches component names, while Search in both preserves the
combined search. These three 32 px HIGH buttons form one typography group.
Category groups support manual browsing. Single-click a group, recipe or craftable
component to expand it; a component recipe can expand its own requirements.
Double-click a recipe/component to load that exact recipe into Build and close
the browser (for example knarr -> stern). Cyclic dependencies stop expanding.
The native category-label dictionary is read once when the browser opens;
recipe data and dependency indexes continue to use the bundled snapshot.
Both crafting windows use the fourth frameless 22 px title square for opacity, immediately
left of Minimize, Maximize and Close. The whole window fades; the square stays
visible and usable while collapsed. Hover shows the percentage.

For a manual catalogue update, temporarily set `craft.catalogExport` to an
absolute file path outside the game directory, restart and enter the server.
The mod exports the complete native catalogue once, without modifying inventory
or sending extra game commands. Clear that setting after capture. Compile
`tools/CraftCatalogImporter.java` against the mod JAR and the pinned client libs,
then run its main class with the raw capture path and
`src/main/resources/org/highreshud/client/craft/sklotopolis-recipes.tsv` as output.
Rebuild and install the ZIP manually. Do not substitute standard recipes for a
missing server capture. The packaged snapshot has 7773 component records.
The original capture is retained at
`assets/catalogues/sklotopolis-novus-2026-10-14.tsv` for reproducible manual import.
Its SHA-256 is
`c36893d65c12759e398ba29cfc5197226476b4712dc8ed8744e90ec5181c43c1`.

Click a recipe to open its rotating result model. Materials and tools are local
reservations of existing inventory or world IDs. Add a world anvil/tool or an
unfinished ship with the native Add to crafting window action. Tools enter Tool
store; unfinished targets enter Material store and select their own recipe using
the full name. World targets retain the native ground slot for dispatch and receive
server quality, damage, weight, replacement and removal updates.
Server-only structure recipes retain starting tool roles, including Palisade's
hammer. Select a Tile Border with Add to crafting window; it is a world target,
so it has a selected/missing status and no inventory mass/QL requirement.
Dropping or clearing reservations does not move or destroy inventory contents.
Auto-pick fills as much as is available and retains usable tool alternatives;
one usable next step is sufficient. Drag rows to change order, click a row to
select the next step, double-click to remove a reservation, and right-click to
open the item's normal context menu. The first tool is initially selected; scroll
and click another to use its exact ID. Build rechecks the final selection, mass,
heat, damage, material restrictions and available native creation action before
dispatching. A missing or changed-layer tracked world object blocks its use.
Finishing or removing the active unfinished target clears the recipe, requirements,
preview and stores, rather than returning to stale Start/Continue requirements.

Starting mass includes the recipe's material-loss coefficient where the server
uses result mass plus waste. A shaft weighs 1 kg but its log recipe has 100% loss,
so it needs 2 kg of log. These masses are resolved offline into the bundled snapshot;
opening the browser does not recalculate or compare the server catalogue.

Both stores use the inventory presentation: Name with an inline native editor,
QL, DMG and Weight, shared Bold column headings and sort chevrons, draggable
column boundaries, and collapsible type/material groups. Header clicks sort
the displayed groups and their children by typed values. Manual drag restores
the use order and updates the reservations; selection remains tied to item IDs.
Numeric group values are mass-weighted QL/DMG and summed kilograms. Each child
retains its own mass, quality, damage, rarity, heat state and validation hover.
The heading groups are build.material.column-headings and
build.tool.column-headings, reusing the inventory's compact heading font.

Every reserved item displays mass, QL and glowing state. Green identifies the
next action's exact IDs; red takes precedence for insufficient individual mass,
required heat or a conflicting final-material choice. Hover explains the reason.
The requirements show total stock and required mass; a shortage for the whole
recipe does not block a usable individual step.

The optional wood choice restricts only the last remaining wooden component.
The requirements explain it as Last wooden item should be of birchwood material
using the selected species; this line follows material/language changes.
Earlier components can have other species. Nonmatching final wood cannot be
dispatched. Choose Any material to remove this restriction. Metal variants are
one browser row; a metal must be selected before starting a metallic result.
The starting lump or metal head/part fixes the metal, which cannot be changed on
an unfinished item. The selector offers all client metals, including assembly
recipes recorded only with an iron head. Auto-pick and readiness require the
selected metal on the starting head/part; wrong-metal components are red.
The Wiki button
opens the recipe's Wurmpedia topic after a completed click.
Their remaining quantities come from server replies and are requested again
after action results; they are never decremented optimistically.

The rotating result preview fits a bounding sphere including model depth with
a 10% texture margin, so large/long objects remain inside it throughout rotation.
Its offscreen texture uses the same vertical orientation as the Character doll.
Geometry fallback preserves tool heads and the empty crate rack. The native
ItemColorsXml lookup supplies masked material color, so installed Material Colors
palettes and reflection markers apply without recoloring non-material regions.

The Build/Continue button sends one native action using one source/target pair.
Its foreground uses a remembered starting tool, falling back to the
hammer/anvil. Remembered tool icons live in .chamomilo/craft-tools.properties.

The standard template metadata records weights, flags and model mappings for
1424 templates and 955 creation entries from the pinned Wurm build. No server
bytecode is packaged. Server modifications can differ; unknown weights and
models are reported explicitly. Final-material and readiness checks remain
subordinate to server validation.

Initial creation success is the server-supplied percentage for the chosen pair.
Continue uses the Keybinder 1.0.0 ImproveSuccessChanceEstimator model, ported
to CraftSkillRoll: skill, parent skill, selected component QL/damage, Epic and
deterministic normal-CDF lookup. Build checks the result template difficulty,
where Improve checks target QL. Improve's priest/repairer action bonuses do not
apply to the standard Continue check. Unavailable skills show an unavailable
estimate; hidden alcohol, village, rune and special server bonuses can differ.

CraftRarityRoll preserves Keybinder's ImproveRarityChanceEstimator distribution
and adapts its transfer stage: a simple creation transfers action rarity directly,
an advanced creation/continuation uses 1 / total component count. An audible
drumroll means a nonzero action rarity, so its distribution is normalized by the
chance of a nonzero rarity roll. Rare/Supreme mean at least that tier, conditional
on the drumroll and a successful step. Ranges cover the unknown paying-account
state. Already-held rarity and eligible consumed rare components are included.
These values are not the probability of obtaining a drumroll.
Material-only inheritance, complete-recipe denominators, simple-part assembly
and the fishing-pole exception are documented in [CRAFT-RARITY.md](CRAFT-RARITY.md).
Eligible rare stock highlights the affected chance rows; bright gold means the
green next-step selection contributes that independent material transfer.

Next QL remains a successful-step upper bound based on selected component QL
after its damage adjustment
and the standard advanced-creation divisor; actual skill power can lower it.
Keybinder's Improve model estimates roll success and rarity, not an exact QL
gain. All three values refresh with selection, item/skill updates and results.

Model provenance: Keybinder 1.0.0, ImproveSuccessChanceEstimator.java and
ImproveRarityChanceEstimator.java. Their ported names avoid a runtime Keybinder
dependency and shared-classloader collisions. Original SHA-256 values:
6a633e006970d4d41b5c6c60a350cd36faa7f26a52c27a29d67ead6062ff337b and
72cc7838d3ae5dfefad46e04bd34014e75d80265e706db1e68d74393955c9a27.
The original estimator regression cases are retained, with creation-specific
tests for drumroll normalization, component transfers and live native inputs.

Typography groups: build.utilities (Auto-pick, Clear stores, Stop), HIGH,
32 px, shared 18 px ceiling; build.primary (Build, Continue), HIGH, 48 px,
shared 26 px ceiling with a 52 px icon reservation; build.wiki is a separate
singleton at 32 px. Body text uses Alegreya Sans at 14 px; result preview
uses the existing shared portrait renderer at 512 px.

Live GPU model rendering, custom-server recipes, temperature updates and full
network action sequences still require an in-game check after manual installation.
