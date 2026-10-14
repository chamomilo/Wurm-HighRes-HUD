# Changelog

## 0.8.7 — 2026-10-15

- Rebuild with the complete canonical Chamomilo updater 1.5.0 and verify its
  integrated Settings editor and embedded UI 0.4.11.
- Rewrite the illustrated guide and installation README for the current Build,
  recipe browser, world reservations, opacity and minimized channel strips.
- Remove artwork prompts and development exports from the project. Keep the
  installable archive limited to four runtime files and README.txt.

## 0.8.6 — 2026-10-15

- Reserve world tools and unfinished objects through the native Add to crafting
  window callbacks. Show unfinished targets in Material store, select their
  recipe by full name, and keep native ground IDs for Build/Continue.
- Retain tool alternatives from Auto-pick; a row click selects the exact tool
  used by the next native action. Recheck the final selection before dispatch.
- Remove local reservations on double-click; right-click opens the normal
  server item context menu. Refresh world QL, damage, mass and availability.
- Clear the entire Build state when the unfinished target finishes or is removed.
- Include material-loss coefficients in starting mass and regenerate the bundled
  snapshot offline. Shaft now requires 2 kg of log instead of its 1 kg result mass.
- Recognize starting tools in server-only recipes such as Palisade. Support Tile
  Border/world location targets without applying inventory mass or QL checks.

## 0.8.5 — 2026-10-15

- Highlight the selected recipe search mode with the same green surface,
  caption and marker as ON/OFF actions, preserving hover and press behavior.

## 0.8.4 — 2026-10-15

- Correct the Build preview's inverted offscreen texture and fit the whole
  model, including depth, with a margin throughout its rotation.
- Resolve actual tool-part geometry instead of falling back to the complete
  tool; use the crate rack's empty model state.
- Pass the native item material color into the model mask renderer. Installed
  Material Colors palettes/reflection markers apply to previews; different
  materials keep distinct render keys.
- Offer all client metals for assembly recipes represented by an iron head.
  Require the selected metal on starting parts and show it in requirements;
  wrong-metal heads remain invalid. Match electrum's generic component family.
- Add a localized last-wood-component requirement for the selected final wood
  and remove it when Any material is selected.
- Restore category groups and nested component recipes in the recipe browser.
  Single-click expands/selects; double-click opens that exact recipe in Build
  and closes the browser, including a component such as knarr's stern.
- Add Search in name/material/both selectors, initially names only. Name and
  material modes match partial names; Both retains the existing combined search.

## 0.8.3 — 2026-10-14

- Restore direct access to the recipe browser from the high-res Build screen:
  add Choose recipe under the title at the upper left, with native release/drag cancellation and restore
  of a collapsed recipe window. Keep `toggle creationlist` console access.
- Move material selection below the result model.
- Reduce opacity zones by two pixels in both dimensions across all high-res
  windows, including Chat and Events. Title controls now use a 22 px square.
- Add Compact minimize/restore beside Chat and Events opacity. Collapse to the
  channel strip at the screen's lower edge; selecting a tab restores the window.
  Retain history, entered text and previous expanded bounds.

## 0.8.2 — 2026-10-14

- Embed the complete 2702-recipe Sklotopolis Novus snapshot captured on
  2026-10-14. Resolve template IDs, starting masses and requirement flags offline.
- Open the recipe browser from ready cached descriptors. Ignore the incoming
  server catalogue; automatic comparison and refresh are disabled.
- Keep unfinished-item requirements and action responses authoritative from
  the live server. Catalogue updates are an explicit export/import/rebuild step.

## 0.8.1 — 2026-10-14

- Fix the login hang caused by the hidden recipe browser resolving the full
  server catalogue on the game thread. Hidden windows defer recipe processing.
- Index packaged template names and creation entries once; preserve icon,
  material and first-entry selection rules without scanning every template.
- Cache recipe metadata and search text. Filtering/repainting reuses the cache;
  first opening resolves at most 16 recipes per refresh so frames can continue.
- Add a 2200-recipe native regression covering hidden login, bounded loading,
  repeated filters and late server additions, plus complete lookup parity checks.

## 0.8.0 — 2026-10-14

- Added high-res recipe browser with recipe/component filtering and opacity.
- Added rotating result preview, material selection, separate material/tool stores,
  inventory auto-pick, grouped drag order and click-to-select next-step items.
- Display mass, QL and heat; explain inadequate next-step materials on hover.
- Protect the last wooden component when a final wood species is selected;
  Any material clears that restriction. Refresh unfinished requirements from server.
- Collapse metal variants into one recipe with mandatory starting metal selection;
  the unfinished item keeps the metal fixed by its starting lump.
- Added recipe Wiki button, hammer/anvil artwork and starting tool icons.
- Extended independent whole-window opacity sliders to Chat and Events.
- Move opacity into a fourth frameless 24 px title-control square in all high-res
  windows with title buttons, including Character and Skills. Keep it visible
  while collapsed; Chat and Events retain their tab-strip sliders.
- Removed the character screen's permanent wound counter/rotation caption;
  the doll now explains rotation on hover.
- Distinguish server percentages, conditional rarity and QL upper bounds.
- Reuse Keybinder's Improve skill/rarity estimators for Continue, adapting recipe
  difficulty, component transfers and the conditional audible-drumroll odds.
- Model material rarity inheritance using the full recipe component count,
  simple-part assembly rules and the initial fishing-pole exception. Highlight
  eligible rare stock and the selected next-step rarity contribution in gold.
- Present material/tool stores as inventory tables with Name, QL, DMG, Weight,
  inline filters, typed sorting, resizable columns and collapsible groups.

## 0.7.5 — 2026-10-13

- Widen the inventory opacity field by 30%, including its mouse interaction
  area. Preserve the 10–100% range, green fill and individual window values.
- Embed the canonical Chamomilo updater 1.5.0 with its shared Settings editor
  and the complete High-res HUD schema. Verify native layout and packaging.
- Refresh the illustrated user guide and installation README. Keep the release
  ZIP limited to four runtime files and README.txt.

## 0.7.4 — 2026-10-13

- Expand a collapsed item group or container after one second of continuous
  drag hover, so nested containers and grouped positions become accessible.
- During the first second in another item window, release moves to that
  inventory's root and appends at the end. After one second, enable the
  insertion preview and preserve the selected position after server arrival.
- Preserve positions before/after a row and inside a group, including multiple
  selections received in a different order. Retain native transfer restrictions
  and discard unconfirmed positions after timeout or target window closure.

## 0.7.3 — 2026-10-13

- Reduce every inventory opacity slider to one third of the Filter field width,
  retaining its height, position and green fill. Adjust minimum window widths.
- Raise the minimum opacity to 10% for both per-window sliders and the default
  setting. Older configured values below 10% are clamped to the new minimum.

## 0.7.2 — 2026-10-13

- Add Default opacity of inventory windows (5–100%, initially 100%). Each
  newly opened inventory or container window uses the configured default.
- Add a matching 22 px opacity slider immediately right of Filter in every
  item window. Drag the vertical marker; green fill tracks its position.
  Fade the whole window, including native item text, controls and frame, while
  keeping the slider visible. Reopening resets the per-window adjustment.
- Reserve space for both fields at narrow widths and verify native mouse
  input, independent windows, lifecycle, blending and four-language hints.

## 0.7.1 — 2026-10-13

- Include the crate/rack/wagon destination fix and the amphora round-trip fix
  with the shared Settings editor and window-content font controls from 0.7.0.
- Verify exact item and destination IDs after live font changes, including
  empty and populated containers, filtered rows and expanded backpack contents.

## 0.7.0 — 2026-10-13

- Add the shared registry Settings editor with every user-configurable property, hover help and immediate application where supported.
- Warn immediately after a saved startup-only change and offer the standard Wurm quit confirmation after settings are saved.
- Add Font size for window contents, preserving headings, text selections and scroll anchors.

## 0.6.5 — 2026-10-13

- Fix returning items such as amphoras from a wagon into free space in the
  player inventory. Blank space now targets the inventory itself instead of
  the expanded container owning the last visible row.
- Release an incoming item from an appended insertion preview when it enters
  blank space. Preserve manual sibling reordering and explicit container drops.
- Verify a complete inventory-to-wagon-to-inventory cycle with native model
  updates, expanded backpack contents and the exact source/destination IDs.

## 0.6.4 — 2026-10-13

- Fix crate/container transfers between external item windows. Insertion beside
  a crate now resolves the hidden rack or wagon root, rather than sending the
  move to the first visible crate. Keep native move handlers and server rules.
- Check empty destinations against their hidden root to reject self-drops.
  Verify actual source IDs and destination IDs through native HUD drag/release,
  including an empty wagon, populated racks/wagons and filtered contents.

## 0.6.3 — 2026-10-12

- Rebuild with the latest canonical Chamomilo updater and verify its complete
  module in the mod JAR and installable ZIP.
- Replace the user guide with an illustrated README and a plain-text install
  guide. Include only runtime JARs, mod configuration, bundled portrait profiles
  and README.txt in the ZIP; retain license notices inside the JAR.
- Remove artwork prompts, migration reports and temporary development materials
  from the repository.

## 0.6.2 — 2026-10-12

- Match the inventory filter background to the supplied Chamomilo input
  reference (#241d14), keeping its frameless 22 px height and five-letter width.
- Use a single `m` in the native 16 px material column. Keep the full material
  description in the localized hover and remove the extra column/data padding.
- Apply detailed equipment images to every empty Character slot,
  remapping each subject to its native body part and preparing exact 44 px cells.
  Apply a muted gray tint only to empty-slot backdrops, distinguishing them from
  equipped items at full color and brightness.
  Replace corner art with distinct weight, body and hand images without plaques
  or frames, preserving native targets, weight-limit hovers and hand selection.
- Add the compact Unequip all action under the doll. Dispatch the native
  Unequip all armor command to the player's body; cancelled gestures do nothing.
- Remove the extra rotation-reset button. Keep healthy anatomy in its original
  ivory colors; green begins with minor wounds, progressing to yellow/red.

## 0.6.1 — 2026-10-12

- Move the inventory filter three caption letters farther from Name, reduce its
  height to 22 px and use the canonical Chamomilo input surface color. Retain
  five visible letters, frameless editing and clearance in narrow locales.
- Animate the Character model with the Healthbar pose controller: smooth turns
  every 60–120 seconds and on click, retaining the full-body scale. Manual drag
  cancels the transition and postpones the next automatic turn.
- Hide insertion feedback above the native inventory root and cancel drops in
  that area. Keep the root indicator first and ordinary item moves available.
- Replace the geometric body silhouette with a detailed anatomical skeleton
  based on the original illustration. Restore the native slot/corner images,
  prepared as 44 px icons and small corner thumbnails. Show total carried weight,
  preserve native weight-limit hovers and body/Manage targets, and display the
  hand's own selected/unselected state instead of another active tool's name.

## 0.6.0 — 2026-10-12

- Migrate the native Character screen to Chamomilo frames, controls and sharp
  equipment glyphs, retaining the original inventory model and equipment layout.
  Default to the central rotating 3D model with a twice-density offscreen target.
  Keep weight, body, active hand and rotation reset in the corners.
- Add Body zones with continuous green/yellow/orange/red wound severity,
  visible wound counts/damage and native wound selection, treatment details and
  context actions. View switching retains rotation and item state.
- Route Inventory and both container types directly to their clipped native
  item bodies. Add a temporary Insert here row for native sibling reordering,
  a Put inside/Move here destination marker, and cancel self/descendant drops.
- Build wrapped Chat/Events channel rows upward. Newly opened channels extend
  the window above the primary row while preserving the message area when
  there is space above it. Paint and input use the same reversed row geometry.
- Add an unlabelled native text filter beside Name in every item window, with
  a frameless contrasting background, width for five letters, a filter area
  hint, case-insensitive matching and parent context for nested
  items. Clear the text when closing; never persist it.
- Sort once on opening or heading activation. Keep later item updates and
  manual row order intact. Expand the material heading and hover description.
- Verify character rotation, model density, equipment/body/wound hits, native
  drag/reorder/drop dispatch, upward rows, four languages and constant alpha.

## 0.5.2 — 2026-10-11

- Give all migrated table headings full localized hover descriptions, including
  item quality, damage, weight, volume, price, improvement tools/materials and
  Friends name/status. Resolve hints at native HUD pick time so language changes
  work before repainting, including optional and narrow/ellipsized columns.
- Expand built-in Chat/Event channel hints and skill-detail controls. Retain
  custom channel names and full status/action titles in Events hovers.
- Add Select health, quality/damage/status and action-progress hovers; expand
  vehicle quality/damage in Healthbar. Complete Fighting target rows, distance,
  footing, attack zones and modes, and explain analysis abbreviations and weapon
  quality without changing native control input or compact panel geometry.
- Refresh and verify the complete canonical shared updater before packaging.

## 0.5.1 — 2026-10-11

- Apply the shared item-window presentation to the separate native
  InventoryContainerWindow used by player backpacks and quivers. Cover native
  add/toggle paths, already-open containers and frame scans; retain original
  item models, listeners, drag/drop, saved geometry and close behavior. Translate
  standard titles and add the same compact sortable headings and initial
  name-ascending order, including items arriving after opening.
- Fix the painted resize corner's real press route through HUD mouse handling,
  retaining the original native resize drag and release behavior for all migrated
  windows. Reflow the tab rows during resizing.
- Wrap overflowing channel tabs into second, third and further 21 px rows.
  Redistribute them when width, membership or language changes, preserving native
  order, shaped hit areas and shared Compact 14 px captions. Keep the body frame's
  upper rail below the final row and the original chat input at the bottom.
- Use Russian item-column abbreviations Кч (QL) and Пвр (DMG).
- Apply the updated Chamomilo data typography to all migrated item/Friends
  lists, player names and Chat/Event histories: shared Alegreya Sans Regular/Bold,
  14 px table/list data and 16 px messages, honoring larger native readability
  settings. Recompute native row/wrapping metrics, preserve original case,
  multicolor segments, message selection and scroll anchors. Headings remain
  Alegreya Sans SC; native editor caret/clipboard metrics remain intact.
- Verify both actual native container classes, add/toggle/close/reopen and listener
  cleanup, completed HUD resize presses/drags/releases, and translated multirow tabs.
- Refresh the complete canonical shared updater module before packaging.

## 0.5.0 — 2026-10-11

- Add Change detalization level to the Skills tab's native context menu, with
  a localized Compact dialog for All, 1, 0.1, 0.01, 0.001, 0.0001, 0.00001
  and 0.000001 (native six-decimal message precision). Persist the choice and
  filter smaller future ticks without changing skill values or existing history.
  Keep affinity/status messages. When configured, enable all upstream native
  ticks so the selected display threshold can see their full precision.
- Centralize all migrated window caption roles: shared Alegreya Sans SC for
  Compact controls, SC Bold for titles/headings, and Alegreya Sans for short
  status/settings labels. Keep native fonts for long body text and editing as
  required by the kit. Retain 14 px captions and established control dimensions.

- Translate Recruit and Info/Infos channel captions and hints in the shared HUD
  catalogue, including uppercase and native-prefixed names. Russian labels are
  Набор and Инфо; also translate Friends, backpack, quiver and vehicle hold.
  Keep the Compact 14 px role, native channel IDs and custom container names.
- Reserve a visible 24 px bottom row for the original chat editor, including
  native focus, typing, command submission, history and clipboard behavior.
  Remove horizontal scrollbars from chat, message/player panes and Friends;
  overflow channel tabs remain reachable by wheel and active-tab selection.
- Fix zero-height decorators that hid table headings and the chat editor.
  Show a minimal 21 px Chamomilo heading row in Inventory, all item containers
  and Friends, with shared 14 px Bold captions, native column widths and sorting.
- Move translated Activated and the current tool from Inventory's old footer
  into its title. Skin Events' existing action progress with a compact dark bar.
  Reduce the obsolete bottom-window band to frame thickness while keeping
  native corner resizing and saved window geometry.
- Reset item and Friends tables to native name-ascending order whenever their
  window or tab opens, including contents arriving afterward. Preserve subsequent
  user sorting until the next opening.
- Catch native containers opened through another mod's window creation path.
- Verify actual HUD chat subclass/focus hit testing, native input painting,
  visible heading/editor geometry, Friends switching and reopen sorting.
  Exercise the real Skills context menu, completed/cancelled actions, preference
  persistence, threshold boundaries, four languages and detached dropdown alpha.

## 0.4.3 — 2026-10-10

- Show Events and Chat as joined channel buttons directly above their content.
  Remove their title band, three window buttons and upper exterior frame. Show
  the channel scrollbar only when tabs overflow; retain HUD show/hide controls.
  Join tab bottoms to a continuous body-frame top rail across the window width.

- Fix startup failure when Avatar2 or Keybinder has already loaded native GUI
  base classes. Hook HUD/renderer call sites and use presentation decorators;
  never attempt to rewrite or defrost classes already defined in the shared loader.
- Keep native clipping, completed clicks, sorting, resize, collapsed input and
  item/editor identities while preserving Compact geometry and caption sizes.
- Use distinct canonical Compact entry points that also work with an older
  shared SDK loaded first. Embed updater 1.2.7 and complete Chamomilo UI 0.4.10.
- Exercise real packaged-mod preInit after native GUI preload, plus window
  interaction/rendering with both the current and older shared SDK.
- Include the hook failure cause in the log message for client formatters that
  omit exception stack traces.

## 0.4.2 — 2026-10-10

- Apply the shared High-res item interface to backpack, vehicle hold, quiver
  and other native container windows, including dynamic opening and reconnect.
  Retain their item models, server close notifications, listeners and saved geometry.
- Use the explicit SDK Compact preset for window controls and channel captions.
  Keep title/tab text at 14 px, reduce header heights and retain native column widths.
- Embed canonical updater 1.2.6 with the complete reviewed Chamomilo UI 0.4.9 SDK.
- Verify container lifecycle, Compact roles and unchanged widths in the native
  window fixture alongside input, unread, sorting and fixed-alpha checks.

## 0.4.1 — 2026-10-10

- Embed canonical updater 1.2.6 with only concrete language choices and no Off
  option. The updater remains the only selector and applies its language to
  all participating installed mods, including HUD panels and windows.
- Apply the shared language at HUD readiness even without a previous manual choice.

## 0.4.0 — 2026-10-10

- Add High-res Inventory, Events and Chat adapters to the unified HUD. Retain the exact native window instances, saved positions, hotkeys, inventory model, message histories, server commands and chat editor.
- Use Chamomilo walnut/leather frames, title controls, native resize/drag handlers and shared scrollbars. Collapse, maximize and restore retain the underlying window state.
- Add sloped overlapping channel tabs with one shared caption size/baseline, native selection/context menus and horizontal overflow scrolling. Unread gray is stable; selected channels keep unread messages while their window is hidden.
- Add a single framed Inventory heading band, shared Bold small-cap captions and sorting glyphs. Numeric columns reserve caption/indicator space; each newly selected sort column starts ascending.
- Localize window titles, standard channel names and headings in all four supported languages. Preserve custom server channel names and game text.
- Verify native inventory/editor/manager behavior, HUD registration, drag cancellation, hidden unread, sorting, window controls, locale changes and fixed-alpha pixels in an offline fixture.

## 0.3.2 — 2026-10-10

- Remove the Healthbar language submenu and its callbacks. The shared updater is the only language selector.
- Retain synchronous updater language application and next-frame repaint of already-open HUD panels and existing buttons, without recreating interfaces or restarting the client.
- Verify all four updater language choices against existing Healthbar labels, Select footer, Fighting focus/analysis and action buttons, retaining panel geometry and learned samples.

## 0.3.1 — 2026-10-10

- Fix startup failure in `HudMessages` when Wurm's shared Javassist loader hides the packaged translation catalogue. Read the resource from the installed HUD JAR as a fallback and close the archive eagerly.
- Reduce Fighting HUD from 676 × 282 to 520 × 186, saving 49.3% of its area through tighter layout. Keep a 52 px portrait, 32 px attack-zone buttons, 24 px modes, 28 px specials and readable 12/14 px body text.
- Show four compact analysis rows. Retain confidence ranges, weapon details, enemy stance and incoming damage types in the analysis hover. Preserve native commands, availability and learned observations.
- Verify configuration and all four HUD/updater catalogues through Wurm's real HookManager loader with embedded resources hidden from Java's parent classpath. Verify compact frame/text geometry and native input before packaging.
- Embed the complete current updater 1.2.4 and Chamomilo UI 0.4.8. The canonical updater receives the same packaged-translation fallback fix.

## 0.3.0 — 2026-10-10

- Localize HUD-owned labels, menus, tooltips, Examine status and combat analysis in English, Brazilian Portuguese, German and Russian.
- Expose the HUD language capability to the shared updater dropdown. Apply selections immediately to all three panels and save the HUD's user preference outside the game installation; manual upgrades and updater Off retain it.
- Add a discreet Language submenu to the Healthbar right-click menu with four autonyms and a current-choice marker. It shares the updater's language selection, so either selector redraws existing panels and the updater without a game restart.
- Remeasure existing action caption groups on language changes without resizing controls. Verify full translated captions in Regular/Bold, bundled Cyrillic/accent glyphs, native layout/input and four-language previews.
- Preserve canonical server text parsing, command IDs, creature/item names, native action text and learned combat state across language changes.
- Embed and verify the complete current updater 1.2.1 and its pinned Chamomilo UI 0.4.7 SDK.

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
