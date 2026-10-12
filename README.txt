WURM HIGH-RES HUD 0.6.3

Healthbar, Select bar, Fighting HUD, Character, Inventory, containers,
Chat and Events in one Wurm Unlimited client mod. No server mod is required.
Languages: English, Brazilian Portuguese, German and Russian.
Includes Chamomilo updater 1.4.1 and UI 0.4.11.

ILLUSTRATED GUIDE
https://github.com/chamomilo/Wurm-HighRes-HUD#readme

DOWNLOADS
https://github.com/chamomilo/Wurm-HighRes-HUD/releases/latest

REQUIREMENTS
Wurm Unlimited and Ago's Client Mod Launcher, using the client's Java 8 runtime.
https://github.com/ago1024/WurmClientModLauncher

INSTALL OR UPDATE
1. Close the client.
2. Extract this ZIP into your WurmLauncher directory, merging its mods folder.
3. Confirm these files exist:
   mods/highres-hud.properties
   mods/highres-hud/highres-hud-0.6.3.jar
   mods/highres-hud/highres-hud-resources-0.6.3.jar
   mods/highres-hud/portrait-profiles.bundled.properties
4. Disable old standalone highres-healthbar, highres-selectbar and
   highres-fighting-hud mods. This unified package includes their panels.
5. Start the game through Client Mod Launcher.

Keep the bundled portrait profiles in place. They provide portrait framing
when the shared loader hides JAR resources. Existing window positions and
user preferences are retained.

CHARACTER
The full-body 3D model is the default view. Drag to rotate; click changes its
pose. Automatic turns use the same timing as Healthbar, without zooming.
Body zones shows wound severity: healthy ivory, then green/yellow/orange/red.
Muted artwork identifies empty slots; equipped items retain full color.
Corner controls provide weight/limits, body/Manage actions and hand selection.
Unequip all performs the native Unequip all armor action.

INVENTORY AND CONTAINERS
Type beside Name to filter loaded items, including nested items. The field
clears when the window closes. The m column explains material type on hover.
Sorting runs once on opening or a heading click; manual row order is retained.
Dragging shows Insert here between rows and Put inside over a container.
Inventory remains the first row, with no drop target above it.

CHAT AND EVENTS
Commands, history, editing and clipboard support remain native.
Extra channel rows grow above the primary row. Right-click Skills to choose
the smallest displayed skill tick.

HUD CONTROLS
Healthbar portrait: click for pose, wheel for zoom, right-click for options.
Select portrait: drag to rotate, wheel to zoom, right-click for native actions.
Select shelf: click the page indicator or use the wheel to change action pages.
Fighting portrait: drag to rotate, wheel to zoom, double-click to clear target.
Sleep bonus, Disembark, Combat focus and special moves retain native actions.

LANGUAGE AND UPDATES
Open Main menu > Mod updates to select language and check releases.
The shared updater uses the newest compatible embedded Chamomilo module.
Download opens the release page; install ZIP updates manually.

CONFIGURATION
Edit mods/highres-hud.properties while the game is closed. Each panel has an
enabledByDefault setting. Optional custom portrait framing belongs in
mods/highres-hud/portrait-profiles.properties; the bundled reference is separate.
Learned combat data defaults to mods/highres-hud/knowledge.
The HUD may issue quiet /fatigue, /titles and serialized Examine requests to
populate displays. Player Examine actions take priority.

LICENSE
GNU General Public License v3. Full text is inside the runtime JAR at
META-INF/LICENSE.txt. Bundled fonts retain their license notices.
Source and license: https://github.com/chamomilo/Wurm-HighRes-HUD
