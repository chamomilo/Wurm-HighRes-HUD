HIGH-RES HUD FOR WURM UNLIMITED 0.8.7

Inventory, containers, Character, Chat, Events, Build and recipes, Healthbar,
Select bar and Fighting HUD in one client mod. No server mod is required.
Includes Chamomilo updater 1.5.0 with Settings and Chamomilo UI 0.4.11.
Languages: English, Brazilian Portuguese, German and Russian.

ILLUSTRATED GUIDE AND RELEASES
https://github.com/chamomilo/Wurm-HighRes-HUD#readme
https://github.com/chamomilo/Wurm-HighRes-HUD/releases/latest

INSTALL OR UPDATE
Requires Wurm Unlimited, Java 8 and Ago's Client Mod Launcher:
https://github.com/ago1024/WurmClientModLauncher

1. Close the game.
2. Extract this ZIP into WurmLauncher, merging the mods folder.
3. Disable old standalone highres-healthbar, highres-selectbar and
   highres-fighting-hud mods; their panels are included here.
4. Start the game through Client Mod Launcher.

ARCHIVE CONTENTS
README.txt
mods/highres-hud.properties
mods/highres-hud/highres-hud-0.8.7.jar
mods/highres-hud/highres-hud-resources-0.8.7.jar
mods/highres-hud/portrait-profiles.bundled.properties

Keep the bundled portrait profiles beside the JARs. Guide images, source code
and build tools are available in the repository, outside the installable ZIP.
Updates are installed manually.

SETTINGS, LANGUAGE AND UPDATES
Open Main menu > Mod updates > High-res HUD > Settings.
Valid edits save automatically; hover over options for help.
Font size changes window contents immediately; 0 follows native preferences.
Default opacity accepts 10-100 percent and applies to newly opened windows.
Window enable/disable settings take effect at the next client start.
Portrait, animation, HUD defaults and combat settings are in the same editor.
Startup-only changes show a restart notice. Exit game saves valid edits and
opens Wurm's normal quit confirmation.
Mod updates also selects the shared language and checks releases.
Download opens the release page for manual installation.

BUILD AND RECIPE BROWSER
Open crafting with B (default binding), then Choose recipe below the title.
The console command toggle creationlist also opens the browser.
Search in name is the default. Search in material matches component names;
Search in both searches across the recipe. The selected mode is green.
Single-click expands groups/components. Double-click a recipe, including a
nested component, to load it in Build and close the browser.

The prepared Sklotopolis Novus snapshot contains 2702 recipes from 2026-10-14.
Automatic catalogue comparison and refresh are disabled. Updates are manual.
Live unfinished-item requirements continue to follow the server.

Choose the result material below the rotating preview. Metallic variants
share one recipe; choose the metal before starting. Lumps and starting metal
parts must match. Wood choice is optional and protects the last remaining
wooden component; Any material clears the restriction.

Drop items into Material store and Tool store or use Auto-pick up from
inventory. Stores reserve items without moving them. In the world, use Add
to crafting window for tools, unfinished objects and supported locations such
as tile borders. World tools enter Tool store; unfinished targets enter
Material store and select their recipe.

Green marks the next-step material and tool. Click another row to change the
selection. Auto-pick retains alternative tools; scroll to choose another.
Drag rows to change order. Double-click removes a reservation; right-click
opens the item's native menu. The final selection is rechecked on Build.

Stores have inventory-style filters, Name/QL/DMG/Weight sorting, resizable
headings and collapsible groups. Requirements include mass and QL. Red means
insufficient mass, missing required glowing heat or incompatible final
material; hover explains why. Starting mass includes material loss: a 1 kg
shaft requires 2 kg of log. One usable step enables Build/Continue; filling
every requirement is unnecessary. Server responses refresh remaining needs.
Completion clears the unfinished target and the Build screen.

Wiki opens the recipe's Wurmpedia topic. Previews use native models/material
palettes, including installed Material Colors masks, when resources exist.
Initial success comes from the server; Continue uses the Keybinder Improve
model adapted for crafting. Next QL is a successful-step upper bound.
Rare/Supreme are conditional on a drumroll and a successful step. Eligible
rare stock highlights the affected chances in gold. Material rarity may also
transfer without drumroll. Hover explains assumptions and account-status
ranges. Hidden server modifiers can change estimates.

INVENTORY, CONTAINERS AND OPACITY
Filter beside Name to find loaded items and nested contents. Closing clears
the filter. Click column headings to sort.
While dragging, hold over a collapsed group for one second to expand it.
Release in another item window before one second to append at its root.
After one second, Insert here selects a row and Put inside selects a container.
Chosen order survives server responses, including multiple selected items.
Native capacity and container restrictions apply.

The frameless 22 x 22 px opacity zone sits left of the title buttons. Drag
horizontally for 10-100 percent. Green fill and hover show the value. Text and
frame fade together; the control stays visible. Each window is independent;
reopening uses the configured default.

CHAT AND EVENTS
Commands, history, text editing and clipboard support remain native.
Extra channel rows grow above the primary row. Right-click Skills to select
the smallest displayed skill tick.
Minimize beside opacity hides the text area and docks tabs at the lower
screen edge. Tabs, opacity and Restore remain. Click a tab or Restore to
return to the previous size and position.

CHARACTER AND HUD CONTROLS
Character: drag rotates the model; click changes pose. Hover explains rotation.
Body zones shows wounds from healthy ivory through green, yellow, orange and
red. Corners provide weight, body/Manage and hand controls. Unequip all uses
the native action.
Healthbar portrait: click for pose, wheel for zoom, right-click for options.
Select portrait: drag rotates, wheel zooms, right-click opens target actions.
Select shelf: click the page indicator or use the wheel to change pages.
Select Pin keeps selection visible after the pointer leaves.
Fighting portrait: drag rotates, wheel zooms, double-click clears the target.
Sleep bonus, Disembark, Combat focus and special moves use native actions.

CONFIGURATION AND LICENSE
Settings overrides are stored in your user profile. You can also edit
mods/highres-hud.properties while the game is closed. Custom portrait
profiles belong in mods/highres-hud/portrait-profiles.properties.
Combat observations default to mods/highres-hud/knowledge.
GNU General Public License v3. Full text: META-INF/LICENSE.txt in the JAR.
Bundled fonts retain their license notices.
Source and license: https://github.com/chamomilo/Wurm-HighRes-HUD
