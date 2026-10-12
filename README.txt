HIGH-RES HUD FOR WURM UNLIMITED 0.7.5

Healthbar, Select bar, Fighting HUD, Character, Inventory, containers, Chat
and Events in one client mod. No server mod is required.
Languages: English, Brazilian Portuguese, German and Russian.
Includes Chamomilo updater 1.5.0 with Settings and Chamomilo UI 0.4.11.

ILLUSTRATED GUIDE AND RELEASES
https://github.com/chamomilo/Wurm-HighRes-HUD#readme
https://github.com/chamomilo/Wurm-HighRes-HUD/releases/latest

INSTALL OR UPDATE
Requires Wurm Unlimited and Ago's Client Mod Launcher, using Java 8.
https://github.com/ago1024/WurmClientModLauncher

1. Close the game.
2. Extract this ZIP into WurmLauncher, merging the mods folder.
3. Disable old standalone highres-healthbar, highres-selectbar and
   highres-fighting-hud mods; this package includes their panels.
4. Start the game through Client Mod Launcher.

ARCHIVE CONTENTS
README.txt
mods/highres-hud.properties
mods/highres-hud/highres-hud-0.7.5.jar
mods/highres-hud/highres-hud-resources-0.7.5.jar
mods/highres-hud/portrait-profiles.bundled.properties

Keep the bundled portrait profiles alongside the JARs. Pictures and source
code are available in the repository, outside this installable archive.

INVENTORY AND CONTAINERS
Type beside Name to filter loaded items, including nested contents. The
filter clears on closing. Click a heading to sort; item updates retain your
manual order.

Hold over a collapsed group or container for one second to expand it.
Release in another item window before one second to move to that inventory
and append at the end. After one second, Insert here selects a row position
and Put inside selects a specific container. Leaving resets the hover delay.
The chosen position survives the server response, including multi-selection
and grouped rows. Native capacity and container restrictions still apply.

The slider right of Filter controls opacity from 10 to 100 percent. Drag
its marker; green fill shows the position. Text and frame fade together;
the slider stays visible. Its field and mouse area are 30 percent wider
than in 0.7.4. Reopening restores the configured default.

SETTINGS, LANGUAGE AND UPDATES
Open Main menu > Mod updates, find High-res HUD and click Settings.
Valid edits save automatically. Hover over options for explanations.
Font size changes window contents immediately; 0 follows native preferences.
Default opacity of inventory windows accepts 10-100 percent, initially 100.
Newly opened windows use it; existing windows retain their own opacity.
Startup-only changes show a restart notice. Exit game saves valid edits and
opens Wurm's standard quit confirmation.

The Mod updates window also selects the shared language and checks releases.
Download opens the release page. Install ZIP updates manually.

CHARACTER AND HUD CONTROLS
Character: drag the model to rotate; click changes pose. Body zones shows
wounds from healthy ivory through green, yellow, orange and red. Empty slots
use muted artwork. Corners provide weight, body/Manage and hand controls.
Unequip all performs Wurm's native action.
Healthbar portrait: click for pose, wheel for zoom, right-click for options.
Select portrait: drag rotates, wheel zooms, right-click opens target actions.
Select shelf: click its page indicator or use the wheel to change pages.
Select Pin keeps selection visible after the pointer leaves.
Fighting portrait: drag rotates, wheel zooms, double-click clears the target.
Sleep bonus, Disembark, Combat focus and special moves use native actions.

CHAT AND EVENTS
Commands, history, editing and clipboard support remain native.
Extra channel rows grow above the primary row. Right-click Skills to choose
the smallest displayed skill tick.

CONFIGURATION
Settings overrides are stored in your user profile. You can also edit
mods/highres-hud.properties while the game is closed. Custom portrait
profiles belong in mods/highres-hud/portrait-profiles.properties.
Combat observations default to mods/highres-hud/knowledge.

LICENSE
GNU General Public License v3. Full text: META-INF/LICENSE.txt in the JAR.
Bundled fonts retain their own license notices.
Source and license: https://github.com/chamomilo/Wurm-HighRes-HUD
