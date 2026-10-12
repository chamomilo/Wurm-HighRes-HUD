package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.game.inventory.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;
import org.chamomilo.wurm.ui.v1.*;
import org.highreshud.client.i18n.HudMessages;
import org.highreshud.client.SkillDetailSettings;

/** Real native inventory model, managers, tabs, editor and window layout, with GPU drawing substituted. */
public final class HighResWindowsProbe {
    public static long closedId = Long.MIN_VALUE;
    public static int minimumTicksOption = -1;
    public static InventoryMetaWindowView playerInventory;
    public static com.wurmonline.client.comm.SimpleServerConnectionClass connection;
    public static long movedTarget = Long.MIN_VALUE;
    public static long[] movedIds;
    private static long dragNow;
    public static int modelWidth, modelHeight;
    public static int actionCount,actionId;
    public static long[] actionTargets;
    private static final class ContainerView implements InventoryMetaWindowView {
        final long id; final String name; final InventoryMetaItem root; int added, removed;
        ContainerView(long id, String name) throws Exception { this.id = id; this.name = name; root = item(id, -1, name, 0); }
        public long getWindowId() { return id; }
        public String getWindowName() { return name; }
        public InventoryMetaItem getRootItem() { return root; }
        public InventoryMetaItem getItem(long itemId) { return itemId == id ? root : null; }
        public boolean contains(long itemId) { return itemId == id; }
        public void addItemListener(InventoryMetaListener listener) { added++; }
        public void removeItemListener(InventoryMetaListener listener) { removed++; }
    }
    public static void main(String[] args) throws Exception {
        if (System.getProperty("olderSharedUiJar") != null) {
            boolean absent=false;
            try { UiButtonLayout.class.getMethod("compactGroup",String[].class,int[].class,UiScale.class,boolean.class); }
            catch(NoSuchMethodException expected) { absent=true; }
            check(absent,"Older SDK fixture must actually load the older shared class without Compact APIs");
        }
        HudButtonProbe.canvas = new HudButtonProbe.PreviewCanvas();
        WurmComponent.SCREEN_WIDTH = 1920; WurmComponent.SCREEN_HEIGHT = 1080;
        verify(new File(args[0]));
    }
    private static sun.misc.Unsafe unsafe() throws Exception {
        java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        return (sun.misc.Unsafe) field.get(null);
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void release(HeadsUpDisplay hud, WurmComponent button, int x, int y) {
        HighResWindowAccess.set(hud, "mouseFocusComponent", button);
        hud.mouseReleased(x, y, 0);
    }
    private static void sort(HeadsUpDisplay hud, WurmTreeList<?> table, int column) {
        WurmArrayPanel<?> row = (WurmArrayPanel<?>) HighResWindowAccess.get(table, "buttonRow");
        List<WButton> headings = new ArrayList<>();
        for (Object button : row.components) headings.add((WButton) button);
        WurmBorderPanel band = (WurmBorderPanel) row.parent;
        for (FlexComponent child : (FlexComponent[]) HighResWindowAccess.get(band, "components")) if (child instanceof WButton) headings.add((WButton) child);
        for (WButton button : headings) if (((Number) HighResWindowAccess.get(button, "index")).intValue() == column) {
            int x=button.x+button.width/2,y=button.y+button.height/2;
            button.leftPressed(x,y,0);release(hud,button,x,y);return;
        }
        throw new AssertionError("Missing native heading " + column);
    }
    public static com.wurmonline.client.options.MultiOption guiSkin() throws Exception {
        com.wurmonline.client.options.MultiOption skin = (com.wurmonline.client.options.MultiOption) unsafe().allocateInstance(com.wurmonline.client.options.MultiOption.class);
        HighResWindowAccess.set(skin, "options", new String[]{"Default"}); return skin;
    }
    private static InventoryMetaItem item(long id, long parent, String name, float quality) throws Exception {
        InventoryMetaItem item = (InventoryMetaItem) unsafe().allocateInstance(InventoryMetaItem.class);
        HighResWindowAccess.set(item, "id", id); HighResWindowAccess.set(item, "parentId", parent);
        HighResWindowAccess.set(item, "baseName", name); HighResWindowAccess.set(item, "displayName", name);
        HighResWindowAccess.set(item, "customName", ""); HighResWindowAccess.set(item, "hoverText", "");
        HighResWindowAccess.set(item, "groupName", name); HighResWindowAccess.set(item, "quality", quality);
        HighResWindowAccess.set(item, "children", new ArrayList<InventoryMetaItem>());
        return item;
    }
    @SuppressWarnings("unchecked")
    public static void verify(File output) throws Exception {
        HeadsUpDisplay hud = (HeadsUpDisplay) unsafe().allocateInstance(HeadsUpDisplay.class);
        HighResWindowAccess.set(hud, "components", new ArrayList<WurmComponent>());
        HighResWindowAccess.set(hud, "popups", new ArrayList<WurmPopup>());
        HighResWindowAccess.set(hud, "dropdownPopups", new ArrayList<WurmDropdownPopup>());
        Map<InventoryMetaWindowView, ItemListWindow> containers = new HashMap<>();
        HighResWindowAccess.set(hud, "inventoryWindows", containers);
        Map<Long, InventoryContainerWindow> inventoryContainers = new HashMap<>();
        HighResWindowAccess.set(hud, "inventoryContainerWindows", inventoryContainers);
        HighResWindowAccess.set(hud, "dynamicComponents", new HashMap<Short, WurmComponent>());
        HighResWindowAccess.set(hud, "cachedInventoryWindowsPositions", new HashMap<Long, com.wurmonline.client.util.WindowState>());
        HighResWindowAccess.set(hud, "width", 1920); HighResWindowAccess.set(hud, "height", 1080);
        HighResWindowAccess.set(hud, "visible", true);
        HudButtonProbe.world=(com.wurmonline.client.game.World)unsafe().allocateInstance(com.wurmonline.client.game.World.class);
        HighResWindowAccess.set(hud,"world",HudButtonProbe.world);
        connection=(com.wurmonline.client.comm.SimpleServerConnectionClass)unsafe().allocateInstance(com.wurmonline.client.comm.SimpleServerConnectionClass.class);
        WurmComponent.hud = hud;
        HighResWindowAccess.set(hud, "hudSettings", new HudSettings());
        final InventoryMetaItem root = item(1, -1, "Inventory", 0);
        final Map<Long, InventoryMetaItem> items = new HashMap<>(); items.put(1L, root);
        Set<InventoryMetaListener> playerListeners = Collections.newSetFromMap(new IdentityHashMap<InventoryMetaListener, Boolean>());
        InventoryMetaWindowView view = new InventoryMetaWindowView() {
            public long getWindowId() { return -1; }
            public String getWindowName() { return "Inventory"; }
            public InventoryMetaItem getRootItem() { return root; }
            public InventoryMetaItem getItem(long id) { return items.get(id); }
            public boolean contains(long id) { return items.containsKey(id); }
            public void addItemListener(InventoryMetaListener listener) { playerListeners.add(listener); }
            public void removeItemListener(InventoryMetaListener listener) { playerListeners.remove(listener); }
        };
        playerInventory = view;
        InventoryWindow inventory = new InventoryWindow(view);
        InventoryMetaItem large = item(2, 1, "Iron lump", 80), small = item(3, 1, "Carving knife", 9);
        items.put(2L, large); items.put(3L, small);
        inventory.getInventoryListComponent().addInventoryItem(large);
        inventory.getInventoryListComponent().addInventoryItem(small);
        final String[] submitted = {null};
        WurmInputField input = new WurmInputField("Chat input", new InputFieldListener() {
            public void handleInput(String text) { submitted[0] = text; }
            public void handleInputChanged(WurmInputField field, String text) { }
            public void handleEscape(WurmInputField field) { }
        });
        input.setMaxInput(200); input.defaultFocusable = true;
        com.wurmonline.client.renderer.gui.text.TextFont editorFont = input.text;
        HighResWindowAccess.set(hud, "chatInput", input);
        // Use the real HUD chat subclass so hasInputField/getInputField and focus routing are exercised.
        java.lang.reflect.Constructor<?> chatConstructor = Class.forName(HeadsUpDisplay.class.getName() + "$2")
                .getDeclaredConstructor(HeadsUpDisplay.class, String.class, FlexComponent.class);
        chatConstructor.setAccessible(true);
        WurmTabbedWindow chat = (WurmTabbedWindow) chatConstructor.newInstance(hud, "Local chat window", input);
        WurmProgressBar progress = new WurmProgressBar("Action progress");
        WurmArrayPanel<FlexComponent> status = new WurmArrayPanel<>(WurmArrayPanel.DIR_VERTICAL, 32, 38);
        status.addComponent(new WurmLabel("Standing still")); status.addComponent(progress);
        WurmTabbedWindow events = new WurmTabbedWindow("Event chat window", status);
        ChatPanelComponent chatManager = new ChatPanelComponent(chat, ":Local", false);
        ChatPanelComponent eventManager = new ChatPanelComponent(events, ":Event", false);
        for (String name : new String[]{":Trade", ":Village", ":Alliance", "CA HELP", "Long custom server channel", ":Global", ":Recruit", "Infos"})
            chatManager.getTab(name, true).open();
        for (String name : new String[]{":Combat", ":Skills"}) eventManager.getTab(name, false).open();
        FriendsTab friends = new FriendsTab(eventManager, "Friends", false);
        eventManager.addTab("Friends", friends); friends.open();
        HighResWindowAccess.set(hud, "inventoryWindow", inventory);
        HighResWindowAccess.set(hud, "chatWindow", chat); HighResWindowAccess.set(hud, "eventWindow", events);
        hud.getComponents().add(inventory); hud.getComponents().add(chat); hud.getComponents().add(events);
        inventory.setLocation(20, 30, 540, 340); chat.setLocation(590, 30, 560, 280); events.setLocation(590, 340, 560, 220);
        Object inventoryList = inventory.getInventoryListComponent();
        int[] nativeColumnWidths = inventory.getInventoryListComponent().itemList.columnWidths.clone();
        ContainerView backpack = new ContainerView(10, "backpack");
        ItemListWindow existingBackpack = new ItemListWindow(backpack);
        existingBackpack.setLocation(20, 400, 340, 140); containers.put(backpack, existingBackpack);
        hud.getComponents().add(existingBackpack);
        InventoryMetaItem startupQuiverItem = item(200, 1, "quiver", 50); items.put(200L, startupQuiverItem);
        InventoryContainerWindow startupQuiver = new InventoryContainerWindow(startupQuiverItem, null);
        inventoryContainers.put(200L, startupQuiver); hud.getComponents().add(startupQuiver);
        Object nativeHistory = HighResWindowAccess.get(chatManager.getActiveTab(), "textList");
        HighResWindowIntegration.hudReady(hud);
        HighResWindowAccess.set(staticField(HighResWindowIntegration.class,"INVENTORY_DRAG"),"clock",
                (java.util.function.LongSupplier)() -> dragNow);
        check(HighResWindowIntegration.owns(startupQuiver) && startupQuiver.getComponent() == HighResWindowAccess.get(startupQuiver, "list"),
                "Player quiver already open at HUD readiness receives the shared presentation");
        startupQuiver.closePressed();
        check(!HighResWindowIntegration.owns(startupQuiver) && !playerListeners.contains(startupQuiver),
                "Startup player container keeps its original close/listener lifecycle");
        check(inventory.getInventoryListComponent() == inventoryList, "Native inventory model/handlers retained");
        assertDataFonts(inventory.getComponent()); assertDataFonts(chat.getComponent()); assertDataFonts(events.getComponent());
        check(input.text == editorFont, "Native editor retains caret/selection/clipboard font metrics");
        check(Arrays.equals(nativeColumnWidths, inventory.getInventoryListComponent().itemList.columnWidths), "Native inventory column widths retained");
        check(HighResWindowAccess.get(chatManager.getActiveTab(), "textList") == nativeHistory, "Native chat history retained");
        HudSettings settings = (HudSettings) HighResWindowAccess.get(hud, "hudSettings");
        check(settings.getMenuSet().size() == 2, "Chat and events registered in HUD exactly once");
        HighResWindowIntegration.hudReady(hud);
        check(settings.getMenuSet().size() == 2, "Repeated HUD readiness does not duplicate registrations");
        input.setTextMoveToEnd("/say retained command");
        HighResWindowIntegration.beginFrame(hud);
        check(input.getText().equals("/say retained command"), "Editor text and original input callback retained");
        check(chat.hasInputField() && chat.getInputField() == input, "Original HUD editor and chat focus API retained");
        check(input.parent.height == 24 && input.parent.width == chat.getComponent().width
                && input.y >= chat.y + chat.height - 32 && input.width > chat.width - 30,
                "Chat editor reserves a visible full-width bottom row: input=" + input.x + "," + input.y + "," + input.width + "," + input.height
                        + " row=" + input.parent.width + "," + input.parent.height + " body=" + chat.getComponent().width + " bottom=" + (chat.y + chat.height));
        check(hud.getComponentAt(input.x + 8, input.y + 5) == input, "Native HUD hit testing reaches bottom chat editor");
        assertHeadings(inventory.getInventoryListComponent().itemList);
        inventory.setItem(small, null);
        HudMessages.useLanguage("ru");
        check(((WurmBorderPanel) inventory.getComponent()).getComponentAt(inventory.x + 20, inventory.y + inventory.height - 10) != HighResWindowAccess.get(inventory, "itemLabel"),
                "Old activation footer is removed from the body");
        input.keyTyped('\n');
        check("/say retained command".equals(submitted[0]), "Native Enter submits original command");
        AbstractTab trade = chatManager.getTab(":Trade");
        chatManager.addText(":Trade", "A message from the server", 1, 1, 1, false);
        check((Boolean) HighResWindowAccess.get(trade, "hasNewMessages"), "Inactive channel marks unread");
        java.lang.reflect.Field windowMap = HighResWindowIntegration.class.getDeclaredField("WINDOWS"); windowMap.setAccessible(true);
        Map<WurmComponent, HighResNativeWindow> skins = (Map<WurmComponent, HighResNativeWindow>) windowMap.get(null);
        HighResChatWindow skin = (HighResChatWindow) skins.get(chat);
        HighResNativeWindow inventorySkin = skins.get(inventory);
        check(inventorySkin.header.close.preset() == UiButtonPreset.COMPACT && inventorySkin.header.maximize.preset() == UiButtonPreset.COMPACT
                && inventorySkin.header.collapse.preset() == UiButtonPreset.COMPACT, "Item window actions use the explicit Compact preset");
        HighResNativeWindow eventsSkin = skins.get(events);
        check(!skin.hasTitle() && !eventsSkin.hasTitle() && skin.header.close == null && eventsSkin.header.close == null,
                "Chat/Events have channel buttons only, without title or three window controls");
        check(eventsSkin.header.height == HighResChannelStrip.HEIGHT && events.getComponent().y == events.y + HighResChannelStrip.HEIGHT,
                "Fitting Events tabs sit immediately above native messages without an empty title/scrollbar band");
        check(skin.channels().rowCount() > 1 && skin.header.height == skin.channels().rowCount() * HighResChannelStrip.HEIGHT,
                "Overflow tabs reserve visible wrapped rows without a horizontal scrollbar");
        check(inventorySkin.caption().equals("Инвентарь · Активирован: Carving knife"), "Activation follows Inventory in the translated title");
        HudMessages.useLanguage("en"); skin.tick();
        assertNoHorizontal(chat.getComponent());
        check(progress.parent.height == 18 && progress.parent.getClass().getSimpleName().equals("ProgressFrame"), "Native Events progress has a compact branded presentation");
        progress.setProgress(.5f, "Working", false);
        check(((Number) HighResWindowAccess.get(progress, "progress")).floatValue() == .5f, "Original action progress updates retained");
        check(skins.get(existingBackpack) instanceof HighResContainerWindow
                        && existingBackpack.width == HighResWindowIntegration.minimumWidth(existingBackpack,340),
                "Container already open at HUD readiness accommodates filter and opacity controls");
        List<WWindow> previews = new ArrayList<>(); previews.add(existingBackpack);
        for (String name : new String[]{"vehicle hold", "quiver"}) {
            ContainerView container = new ContainerView(20 + previews.size(), name);
            hud.addInventoryWindow(container);
            ItemListWindow nativeWindow = containers.get(container);
            HighResContainerWindow adapter = (HighResContainerWindow) skins.get(nativeWindow);
            check(adapter != null && adapter.itemTable() == ((InventoryListComponent) HighResWindowAccess.get(nativeWindow, "component")).itemList,
                    name + " keeps the native item model after dynamic opening");
            check(nativeWindow.width >= 320 && adapter.caption().equals(name), "Native container title and usable header width retained: " + name);
            assertHeadings(adapter.itemTable());
            nativeWindow.setLocation(20, 400 + previews.size() * 155, 340, 140);
            int[] columns = adapter.itemTable().columnWidths.clone();
            adapter.close();
            check(closedId == container.id && !containers.containsKey(container) && !skins.containsKey(nativeWindow)
                    && container.removed == 1, "Native server close and listener/adapter cleanup: " + name);
            hud.addInventoryWindow(container);
            ItemListWindow reopened = containers.get(container);
            check(reopened.x == 20 && reopened.y == 400 + previews.size() * 155 && reopened.width >= 340
                    && Arrays.equals(columns, ((HighResContainerWindow) skins.get(reopened)).itemTable().columnWidths),
                    "Reopened container retains saved position, size and columns: " + name);
            previews.add(reopened);
        }
        verifyInventoryContainers(hud, inventoryContainers, items, playerListeners, skins, output);
        verifyDataTypography(output);
        skin.tick();
        HighResChannelStrip strip = skin.channels();
        verifyResizeAndRows(hud, skin, inventorySkin, eventsSkin, previews);
        List<HighResChannelStrip.Channel> proxies = (List<HighResChannelStrip.Channel>) HighResWindowAccess.get(strip, "channels");
        HighResChannelStrip.Channel tradeButton = proxies.get(1);
        check(tradeButton.nativeButton.tab == trade, "Native channel order retained");
        check(tradeButton.height == HighResChannelStrip.HEIGHT && tradeButton.width >= 48, "Native pointer rectangle matches painted tab geometry");
        check(tradeButton.captionLayout.preset == UiButtonPreset.COMPACT && tradeButton.captionLayout.fontPixels == 14,
                "Channel captions use the compact nominal role near native text size");
        int px = tradeButton.x + tradeButton.width / 2, py = tradeButton.y + 10;
        tradeButton.leftPressed(px, py, 0);
        check(chatManager.getActiveTab() != trade, "Tab switches only on completed release");
        release(hud, tradeButton, px, py); trade.gameTick(); skin.tick();
        check(chatManager.getActiveTab() == trade && !(Boolean) HighResWindowAccess.get(trade, "hasNewMessages"), "Native selection acknowledges unread");
        HighResChannelStrip.Channel localButton = proxies.get(0);
        int lx = localButton.x + localButton.width / 2, ly = localButton.y + 10;
        localButton.leftPressed(lx, ly, 0); localButton.mouseDragged(lx, ly - 70); localButton.mouseDragged(lx, ly); release(hud,localButton,lx,ly);
        check(chatManager.getActiveTab() == trade, "Dragging out then back cancels selection");
        localButton.leftPressed(lx, ly, 0); release(hud,localButton,localButton.x + localButton.width - 1, localButton.y + 2);
        check(chatManager.getActiveTab() == trade, "Release inside a covered sloped rectangle cancels selection");
        localButton.nativeButton.setEnabled(false); localButton.leftPressed(lx, ly, 0); release(hud,localButton,lx,ly);
        check(chatManager.getActiveTab() == trade, "Native disabled channel cannot activate");
        localButton.nativeButton.setEnabled(true);
        hud.getComponents().remove(chat);
        chatManager.addText(":Trade", "Hidden active channel message", 1, 1, 1, false); trade.gameTick();
        check((Boolean) HighResWindowAccess.get(trade, "hasNewMessages"), "Hidden selected channel retains unread");
        hud.getComponents().add(chat); trade.gameTick();
        check(!(Boolean) HighResWindowAccess.get(trade, "hasNewMessages"), "Reopened visible channel acknowledges unread");
        WurmTreeList<?> table = inventory.getInventoryListComponent().itemList;
        sort(hud,table,0);
        check(((Number) HighResWindowAccess.get(table, "sortOrder")).intValue() == 1, "First quality sort is ascending");
        sort(hud,table,0);
        check(((Number) HighResWindowAccess.get(table, "sortOrder")).intValue() == -1, "Repeated sort descends");
        sort(hud,table,-2);
        check(((Number) HighResWindowAccess.get(table, "sortOrder")).intValue() == 1, "New name column resets ascending");
        check(table.getLastSortOn() == -2, "Name heading sorts using native column identity");
        sort(hud,table,-2);
        check(((Number) HighResWindowAccess.get(table, "sortOrder")).intValue() == -1, "Name sort can still descend while open");
        hud.getComponents().remove(inventory);
        java.lang.reflect.Method show = HeadsUpDisplay.class.getDeclaredMethod("addComponent", WurmComponent.class);
        show.setAccessible(true); show.invoke(hud, inventory);
        check(table.getLastSortOn() == -2 && ((Number) HighResWindowAccess.get(table, "sortOrder")).intValue() == 1,
                "Reopening Inventory resets native name sorting to ascending");
        friends.select(); HighResWindowIntegration.beginFrame(hud);
        WurmTreeList<?> friendsTable = (WurmTreeList<?>) HighResWindowAccess.get(friends, "listComponent");
        assertHeadings(friendsTable); assertNoHorizontal(events.getComponent());
        sort(hud, friendsTable, 0); sort(hud, friendsTable, 0);
        check(((Number) HighResWindowAccess.get(friendsTable, "sortOrder")).intValue() == -1, "Friends native status sorting remains usable");
        eventManager.getTab(":Event").select(); HighResWindowIntegration.beginFrame(hud);
        friends.select(); HighResWindowIntegration.beginFrame(hud);
        check(friendsTable.getLastSortOn() == -2 && ((Number) HighResWindowAccess.get(friendsTable, "sortOrder")).intValue() == 1,
                "Opening Friends resets native name sorting to ascending");
        eventManager.getTab(":Event").select(); HighResWindowIntegration.beginFrame(hud);
        int originalHeight = inventory.height;
        inventorySkin.collapse(); check(inventory.minimized && inventory.height == HighResNativeWindow.TITLE_HEIGHT + HighResNativeWindow.FRAME, "Collapse keeps usable item-window title controls");
        check(hud.getComponentAt(inventory.x + 15, inventory.y + 12) == inventorySkin.header, "Collapsed header still routes input through native HUD");
        inventorySkin.collapse(); check(!inventory.minimized && inventory.height == originalHeight, "Restore retains window height");
        inventorySkin.maximize(); check(inventory.width == hud.getWidth() - 20, "Maximize follows HUD bounds");
        inventorySkin.maximize(); check(inventory.width == 540 && inventory.height == originalHeight, "Maximize restore retains geometry");
        for (String language : HudMessages.languageCodes()) {
            HudMessages.useLanguage(language); skin.tick();
            check(chatManager.getActiveTab() == trade && inventory.getInventoryListComponent() == inventoryList,
                    "Language switch preserves native state: " + language);
            List<HighResChannelStrip.Channel> translated = (List<HighResChannelStrip.Channel>) HighResWindowAccess.get(strip, "channels");
            for (HighResChannelStrip.Channel channel : translated)
                if (channel.source.equals(":Recruit") || channel.source.equals("Infos")) {
                    String key = channel.source.equals(":Recruit") ? "Recruit" : "Infos";
                    check(channel.getLabel().equals(HudMessages.tr(key)) && channel.getHoverString().equals(HudMessages.tr(key.equals("Recruit") ? "Recruitment chat" : "Information channel"))
                            && channel.captionLayout.fontPixels == 14 && channel.height == 21,
                            "Recruit/Infos captions and full hints translate without changing Compact text size: " + language);
                }
            check(HighResChannelStrip.caption("INFO").equals(HudMessages.tr("Info"))
                    && HighResChannelStrip.caption(":RECRUIT").equals(HudMessages.tr("Recruit"))
                    && HighResChannelStrip.caption("FRIENDS").equals(HudMessages.tr("Friends"))
                    && HighResContainerWindow.containerCaption("BACKPACK").equals(HudMessages.tr("backpack"))
                    && HighResContainerWindow.containerCaption("quiver").equals(HudMessages.tr("quiver"))
                    && HighResContainerWindow.containerCaption("vehicle hold").equals(HudMessages.tr("vehicle hold"))
                    && HighResContainerWindow.containerCaption("Raritet [Raritet]").equals("Raritet [Raritet]"),
                    "Uppercase and native-prefixed recruitment/information aliases translate: " + language);
        }
        HudMessages.useLanguage("en"); chatManager.getTab(":Local").select(); skin.tick();
        verifyHoverHints(hud, inventory.getInventoryListComponent().itemList, friendsTable, skins, strip);
        verifySkills(hud, eventManager, (HighResChatWindow) eventsSkin, output);
        verifyInventoryDrag(hud, inventorySkin);
        verifyCrossWindowDrop(hud,inventorySkin,skins.get(previews.get(0)));
        verifyCrateTransfers(hud, containers, skins);
        verifyAmphoraRoundTrip(hud, inventorySkin, items, containers, skins);
        verifyFontTransferIntegration(hud, inventorySkin, items, containers, skins);
        verifyInventoryOpacity(hud,inventorySkin,skins.get(previews.get(0)),output);
        if(System.getProperty("olderSharedUiJar")==null)renderSettingsPreview(hud,output);
        verifyTimedInventoryDrag(hud,containers,skins);
        verifyCharacter(hud,view,skins,output);
        input.setTextMoveToEnd("/say Chat input"); input.setKbFocus(true);
        BufferedImage reference = null;
        for (float alpha : new float[]{0, .2f, .65f, 1}) {
            BufferedImage image = new BufferedImage(1180, 900, BufferedImage.TYPE_INT_ARGB);
            HudButtonProbe.previewGraphics(image);
            HighResWindowIntegration.renderWindow(inventory,null); HighResWindowIntegration.renderWindow(chat,null); HighResWindowIntegration.renderWindow(events,null);
            for (WWindow container : previews) HighResWindowIntegration.renderWindow(container,null);
            HudButtonProbe.finishPreview();
            if (reference != null) check(Arrays.equals(reference.getRGB(0, 0, 1180, 900, null, 0, 1180), image.getRGB(0, 0, 1180, 900, null, 0, 1180)),
                    "All window pixels ignore incoming HUD alpha");
            reference = image;
        }
        HudButtonProbe.previewGraphics(reference);
        // The native body GPU painter is omitted in the fixture; clearly label its sample rows.
        sampleText("Inventory / Chat / Events / Backpack / Vehicle hold / Quiver — compact controls", 20, 878, 16);
        sampleText("Offline layout fixture; game content rendering requires a live Wurm check", 20, 896, 12);
        for (int row = 0; row < 5; row++) sampleText(new String[]{"Carving knife", "Iron lump", "Leather", "Rope", "Hammer"}[row], 38, 148 + row * 26, 14);
        sampleText("[12:34] Player: Hello from Local", 610, 100, 14);
        sampleText("[12:35] Player: Native chat history and commands", 610, 124, 14);
        sampleText("[12:35] You examine the carving knife.", 610, 399, 14);
        sampleText("[12:35] Your skill improves.", 610, 423, 14);
        for (int i = 0; i < previews.size(); i++) sampleText(new String[]{"Leather and tools", "Cargo and supplies", "Arrows"}[i], 38, 480 + i * 155, 14);
        HudButtonProbe.finishPreview(); ImageIO.write(reference, "png", output);
        check(settings.getMenuSet().size() == 2, "Repaint/resize/locale never duplicate HUD registrations");
        containers.clear(); hud.getComponents().removeAll(previews); HighResWindowIntegration.reconnected(hud);
        for (WWindow container : previews) check(!skins.containsKey(container), "Reconnect releases stale container adapters");
        System.out.println("HUD_WINDOWS_OK: both native container types, backpack/quiver add/toggle/listeners/reopen, full HUD resize press/drag/release, multirow tab reflow, Compact preset, editor Enter, HUD registration, tab release/drag, hidden unread, sorting, Skills menu/filter/preferences/dropdown, typography, collapse/maximize, four languages, fixed-alpha pixels; preview=" + output);
    }
    @SuppressWarnings("unchecked")
    private static void verifyInventoryDrag(HeadsUpDisplay hud, HighResNativeWindow skin) throws Exception {
        verifyInventoryFilter(hud,skin);
        WurmTreeList<?> table = skin.itemTable();
        FlexComponent panel = (FlexComponent) HighResWindowAccess.get(table, "treeList");
        int rowHeight = ((Number) HighResWindowAccess.get(table, "lineHeight")).intValue();
        int px = panel.x + 100, py = panel.y + (table.lines.size()>1?rowHeight:0) + rowHeight / 2;
        HighResWindowAccess.set(hud, "draggedItem", null);
        check(hud.mousePressed(px, py, 0, 0), "HUD accepts inventory item press");
        check(HighResWindowAccess.get(hud, "mouseFocusComponent") == panel, "Inventory press reaches native tree body");
        hud.mouseDragged(px + 12, py);
        check(HighResWindowAccess.get(hud, "draggedItem") != null, "Inventory row starts real HUD drag after movement threshold");
        HighResWindowAccess.set(hud, "draggedItem", null);
        hud.mouseReleased(px + 12, py, 0);
        if(table.lines.size()<3)return;
        int count=table.lines.size();
        TreeListItem source=table.lines.get(1).item,target=table.lines.get(2).item;
        InventoryMetaItem sourceMeta=(InventoryMetaItem)HighResWindowAccess.get(source,"item");
        InventoryMetaItem targetMeta=(InventoryMetaItem)HighResWindowAccess.get(target,"item");
        long sourceId=sourceMeta.getId();
        Object drag=staticField(HighResWindowIntegration.class,"INVENTORY_DRAG");
        TreeListItem firstRow=table.lines.get(0).item;
        if(firstRow instanceof InventoryListComponent.InventoryTreeListItem
                && ((InventoryListComponent.InventoryTreeListItem)firstRow).getOwner().rootItem==firstRow) {
            hud.mousePressed(px,py,0,0);hud.mouseDragged(px+12,py);hud.mouseDragged(px+12,panel.y+1);
            check(table.lines.size()==count && HighResWindowAccess.get(drag,"label")==null,
                    "The root inventory indicator has no insertion target above it");
            movedTarget=Long.MIN_VALUE;hud.mouseReleased(px+12,panel.y+1,0);
            check(movedTarget==Long.MIN_VALUE && table.lines.get(0).item==firstRow,
                    "Dropping above the root neither moves items nor changes the first row");
        }
        hud.mousePressed(px,py,0,0);hud.mouseDragged(px+12,py);
        int gapY=panel.y+rowHeight*3-1;
        hud.mouseDragged(px+12,gapY);
        check(table.lines.size()==count+1,"Insert here is an actual temporary row between inventory entries");
        movedTarget=Long.MIN_VALUE;hud.mouseReleased(px+12,gapY+rowHeight/2,0);
        check(table.lines.size()==count&&HighResWindowAccess.get(hud,"draggedItem")==null,"Release removes the gap and ends the drag");
        check(movedTarget==Long.MIN_VALUE&&table.lines.get(2).item==source,"Insert here reorders siblings through the native client handler without a server move: "+skin.window.getClass().getSimpleName());
        boolean container=(Boolean)HighResWindowAccess.get(target,"isContainer");
        HighResWindowAccess.set(target,"isContainer",true);
        for(int n=0;n<table.lines.size();n++)if(table.lines.get(n).item==source)py=panel.y+n*rowHeight+rowHeight/2;
        hud.mousePressed(px,py,0,0);hud.mouseDragged(px+12,py);
        int targetY=0;for(int n=0;n<table.lines.size();n++)if(table.lines.get(n).item==target)targetY=panel.y+n*rowHeight+rowHeight/2;
        hud.mouseDragged(px+12,targetY);
        check("Put inside".equals(HighResWindowAccess.get(drag,"label")),"Container row announces Put inside");
        movedTarget=Long.MIN_VALUE;hud.mouseReleased(px+12,targetY,0);
        check(movedTarget==targetMeta.getId(),"Put inside uses the same target ID announced by the indicator");
        HighResWindowAccess.set(target,"isContainer",container);
        hud.mousePressed(px,py,0,0);hud.mouseDragged(px+12,py);
        movedTarget=Long.MIN_VALUE;hud.mouseReleased(px+12,py,0);
        check(movedTarget==Long.MIN_VALUE,"Self-drop is cancelled before a server request");
    }
    @SuppressWarnings("unchecked") private static void verifyInventoryFilter(HeadsUpDisplay hud,HighResNativeWindow skin) throws Exception {
        Map<WurmTreeList<?>,HighResInventoryFilter> filters=(Map<WurmTreeList<?>,HighResInventoryFilter>)staticField(HighResWindowIntegration.class,"FILTERS");
        HighResInventoryFilter filter=filters.get(skin.itemTable());
        check(filter!=null,"Every native item window has its own header filter");
        HighResWindowIntegration.resized(skin.window);
        List<?> original=new ArrayList<>(filter.table.lines);
        if(original.isEmpty())return;
        TreeListItem match=filter.table.lines.get(filter.table.lines.size()-1).item;
        String letters=match.getName().toUpperCase(Locale.ROOT);
        filter.field.setText(letters);filter.field.handleInputChanged(null,letters);
        check(filter.table.lines.stream().anyMatch(node -> node.item==match),"Filter is case insensitive and retains original native items");
        filter.field.setText("unlikely filter xyz123");filter.field.handleInputChanged(null,filter.field.value());
        check(filter.table.lines.isEmpty(),"A nonmatching filter shows an empty list");
        filter.layout();FlexComponent input=filter.at(filter.field.x+8,filter.field.y+8);
        check(input instanceof WurmInputField && hud.getComponentAt(filter.field.x+8,filter.field.y+8)==input,"Unlabelled header field reaches the native text editor");
        filter.field.setText("");filter.field.handleInputChanged(null,"");
        hud.mousePressed(filter.field.x+8,filter.field.y+8,0,0);hud.mouseReleased(filter.field.x+8,filter.field.y+8,0);
        check(((WurmInputField)input).hasKbFocus,"Clicking the frameless field grants native keyboard focus");
        for(char letter:"abcdefg".toCharArray())((WurmInputField)input).keyTyped(letter);
        check(filter.field.value().equals("abcdefg")&&filter.table.lines.isEmpty(),"Native typing updates the filter and permits queries longer than five visible letters");
        com.wurmonline.client.renderer.PickData pick=new com.wurmonline.client.renderer.PickData(hud);
        check(hud.pick(filter.field.x+8,filter.field.y+8,pick)&&pick.getText().contains(HudMessages.tr("filter area")),"Filter editor exposes its explanation through HUD hover");
        hud.hideComponent(skin.window);
        java.lang.reflect.Method show=HeadsUpDisplay.class.getDeclaredMethod("addComponent",WurmComponent.class);show.setAccessible(true);show.invoke(hud,skin.window);
        check(filter.field.value().isEmpty() && filter.table.lines.size()==original.size(),"Closing and reopening clears unsaved filter text and restores contents");
        check(!(Boolean)HighResWindowAccess.get(filter.table,"keepAutoSorted"),"Opening sorts once, without continuous automatic sorting");
        boolean material=filter.table.hasImpColumn;
        if(!material)filter.table.toggleImpColumn(true);
        HighResWindowIntegration.beginFrame(hud);HighResWindowIntegration.resized(skin.window);
        WButton materialButton=(WButton)HighResWindowAccess.get(filter.table,"impButton");
        for(String language:HudMessages.languageCodes()) {
            HudMessages.useLanguage(language);filter.layout();
            check(materialButton.width==16 && HighResWindowTypography.heading().getWidth("m")<=materialButton.width-2,
                    "Material keeps its native single-letter column width");
            com.wurmonline.client.renderer.PickData materialHint=new com.wurmonline.client.renderer.PickData(hud);
            check(hud.pick(materialButton.x+8,materialButton.y+8,materialHint)
                    && materialHint.getText().contains(HudMessages.tr("Item material type")),
                    "The compact material heading retains its full localized hover description");
            check(filter.opacity.x+filter.opacity.width<=filter.name.x+filter.name.width-18,
                    "Filter reserves the native Name sort indicator at narrow widths: "+language+" name="+filter.name.width+" field="+filter.field.width
                    +" window="+skin.window.width+" table="+filter.table.width+" common="+filter.table.commonWidth+" row="+((WurmComponent)HighResWindowAccess.get(filter.table,"buttonRow")).width+" band="+filter.name.parent.width);
        }
        HudMessages.useLanguage("en");if(!material)filter.table.toggleImpColumn(false);HighResWindowIntegration.beginFrame(hud);
    }
    @SuppressWarnings("unchecked") private static void verifyCrossWindowDrop(HeadsUpDisplay hud,HighResNativeWindow source,HighResNativeWindow destination) throws Exception {
        Map<WurmTreeList<?>,HighResInventoryFilter> filters=(Map<WurmTreeList<?>,HighResInventoryFilter>)staticField(HighResWindowIntegration.class,"FILTERS");
        HighResInventoryFilter filter=filters.get(destination.itemTable());
        filter.field.setText("no matching items xyz");filter.field.handleInputChanged(null,filter.field.value());
        FlexComponent from=(FlexComponent)HighResWindowAccess.get(source.itemTable(),"treeList");
        int height=((Number)HighResWindowAccess.get(source.itemTable(),"lineHeight")).intValue();
        int x=from.x+70,y=from.y+height+height/2;
        hud.mousePressed(x,y,0,0);hud.mouseDragged(x+12,y);
        DraggableComponent dragged=(DraggableComponent)HighResWindowAccess.get(hud,"draggedItem");
        check(dragged!=null,"Cross-window transfer starts from a native item");
        FlexComponent to=(FlexComponent)HighResWindowAccess.get(destination.itemTable(),"treeList");
        x=to.parent.x+70;y=to.parent.y+40;
        hud.mouseDragged(x,y);
        Object feedback=staticField(HighResWindowIntegration.class,"INVENTORY_DRAG");
        String targetDescription="destination="+HighResWindowAccess.get(feedback,"destination")+" hovered="+hud.getComponentAt(x,y)+" pointer="+x+","+y+" root="+destination.window.x+","+destination.window.y+","+destination.window.width+","+destination.window.height;
        movedTarget=Long.MIN_VALUE;movedIds=null;hud.mouseReleased(x,y,0);
        check(movedTarget!=Long.MIN_VALUE&&movedIds!=null&&movedIds.length>0,
                "An empty filtered destination accepts a native server move with source item IDs: "+targetDescription);
        check(HighResWindowAccess.get(hud,"draggedItem")==null,"Cross-window release clears drag focus");filter.reset();
    }
    private static void verifyCrateTransfers(HeadsUpDisplay hud, Map<InventoryMetaWindowView, ItemListWindow> containers,
                                              Map<WurmComponent, HighResNativeWindow> skins) throws Exception {
        ContainerView rack = new ContainerView(8100, "crate rack"), wagon = new ContainerView(8200, "wagon");
        HighResWindowAccess.set(rack.root, "typeBits", (short)8);
        HighResWindowAccess.set(wagon.root, "typeBits", (short)8);
        hud.addInventoryWindow(rack); hud.addInventoryWindow(wagon);
        ItemListWindow rackWindow = containers.get(rack), wagonWindow = containers.get(wagon);
        rackWindow.setLocation(1180, 30, 420, 260); wagonWindow.setLocation(1180, 340, 420, 260);
        HighResNativeWindow rackSkin = skins.get(rackWindow), wagonSkin = skins.get(wagonWindow);
        InventoryListComponent rackList = (InventoryListComponent)HighResWindowAccess.get(rackWindow, "component");
        InventoryListComponent wagonList = (InventoryListComponent)HighResWindowAccess.get(wagonWindow, "component");
        InventoryMetaItem crate = item(8101, rack.id, "large crate", 50);
        InventoryMetaItem otherCrate = item(8102, rack.id, "small crate", 50);
        HighResWindowAccess.set(crate, "typeBits", (short)8);
        HighResWindowAccess.set(otherCrate, "typeBits", (short)8);
        rackList.addInventoryItem(crate); rackList.addInventoryItem(otherCrate);
        rackSkin.tick(); wagonSkin.tick();
        check(wagonSkin.itemTable().lines.isEmpty(), "Empty wagon fixture has no displayed items or filtered rows");
        // External roots are hidden: a native drop outside the rows means the
        // rack/wagon itself, never its first crate or the player's inventory.
        FlexComponent wagonBody = (FlexComponent)HighResWindowAccess.get(wagonSkin.itemTable(), "treeList");
        transferCrate(hud, rackSkin, crate.getId(), wagonSkin, wagonBody.parent.y + 40, wagon.id,
                "Crate rack to genuinely empty wagon");
        InventoryMetaItem wagonCrate = item(8201, wagon.id, "large crate", 50);
        HighResWindowAccess.set(wagonCrate, "typeBits", (short)8);
        wagonList.addInventoryItem(wagonCrate); wagonSkin.tick();
        FlexComponent rackBody = (FlexComponent)HighResWindowAccess.get(rackSkin.itemTable(), "treeList");
        int height = ((Number)HighResWindowAccess.get(rackSkin.itemTable(), "lineHeight")).intValue();
        movedTarget = Long.MIN_VALUE;
        rackList.itemDropped(rackBody.x + 100, rackBody.y + rackSkin.itemTable().lines.size() * height + height,
                (DraggableComponent)wagonSkin.itemTable().lines.get(0).item);
        check(movedTarget == rack.id, "Native reference drop resolves the hidden rack root");
        transferCrate(hud, wagonSkin, wagonCrate.getId(), rackSkin, rackBody.y + 1, rack.id,
                "Wagon to rack before its first crate");
        rackBody = (FlexComponent)HighResWindowAccess.get(rackSkin.itemTable(), "treeList");
        transferCrate(hud, wagonSkin, wagonCrate.getId(), rackSkin,
                rackBody.y + rackSkin.itemTable().lines.size() * height + 30, rack.id,
                "Wagon to rack in blank viewport space");
        wagonBody = (FlexComponent)HighResWindowAccess.get(wagonSkin.itemTable(), "treeList");
        transferCrate(hud, rackSkin, crate.getId(), wagonSkin, wagonBody.y + 1, wagon.id,
                "Rack to wagon before an existing crate");
        Map<WurmTreeList<?>,HighResInventoryFilter> filters =
                (Map<WurmTreeList<?>,HighResInventoryFilter>)staticField(HighResWindowIntegration.class,"FILTERS");
        HighResInventoryFilter filter = filters.get(rackSkin.itemTable());
        filter.field.setText("small"); filter.field.handleInputChanged(null, "small");
        rackBody = (FlexComponent)HighResWindowAccess.get(rackSkin.itemTable(), "treeList");
        transferCrate(hud, wagonSkin, wagonCrate.getId(), rackSkin, rackBody.y + 1, rack.id,
                "Filtered rack retains its hidden root instead of the first matching crate");
        filter.reset();
        // Contents may be unloaded even though the first row is a container.
        // A drop over that row must still mean Put inside that exact item.
        rackBody = (FlexComponent)HighResWindowAccess.get(rackSkin.itemTable(), "treeList");
        transferCrate(hud, wagonSkin, wagonCrate.getId(), rackSkin, rackBody.y + height / 2, crate.getId(),
                "Explicit Put inside retains the native container target");
        ContainerView crateContents = new ContainerView(crate.getId(), "large crate");
        HighResWindowAccess.set(crateContents.root, "typeBits", (short)8);
        hud.addInventoryWindow(crateContents);
        ItemListWindow crateWindow = containers.get(crateContents);
        crateWindow.setLocation(1610, 30, 310, 260);
        HighResNativeWindow crateSkin = skins.get(crateWindow); crateSkin.tick();
        FlexComponent crateBody = (FlexComponent)HighResWindowAccess.get(crateSkin.itemTable(), "treeList");
        transferCrate(hud, rackSkin, crate.getId(), crateSkin, crateBody.parent.y + 40, Long.MIN_VALUE,
                "Empty container cannot accept a drop of itself");
        crateWindow.closePressed(); rackWindow.closePressed(); wagonWindow.closePressed();
        System.out.println("CRATE_TRANSFERS_OK: empty and populated wagon, rack row edges and blank space, filtered root, explicit container target, empty self-drop");
    }
    private static void verifyAmphoraRoundTrip(HeadsUpDisplay hud, HighResNativeWindow inventorySkin,
            Map<Long, InventoryMetaItem> playerItems, Map<InventoryMetaWindowView, ItemListWindow> containers,
            Map<WurmComponent, HighResNativeWindow> skins) throws Exception {
        InventoryListComponent inventory = ((InventoryWindow)inventorySkin.window).getInventoryListComponent();
        long inventoryId = playerInventory.getRootItem().getId();
        ContainerView wagon = new ContainerView(9100, "wagon");
        HighResWindowAccess.set(wagon.root, "typeBits", (short)8);
        hud.addInventoryWindow(wagon);
        ItemListWindow window = containers.get(wagon); window.setLocation(1180, 340, 420, 260);
        HighResNativeWindow wagonSkin = skins.get(window);
        InventoryListComponent wagonList = (InventoryListComponent)HighResWindowAccess.get(window, "component");
        InventoryMetaItem amphora = item(9101, inventoryId, "amphora", 50);
        InventoryMetaItem backpack = item(9102, inventoryId, "Z test backpack", 50);
        InventoryMetaItem contents = item(9103, backpack.getId(), "thread", 50);
        HighResWindowAccess.set(amphora, "typeBits", (short)8);
        HighResWindowAccess.set(backpack, "typeBits", (short)8);
        for (InventoryMetaItem value : new InventoryMetaItem[]{amphora, backpack, contents}) {
            playerItems.put(value.getId(), value); inventory.addInventoryItem(value);
        }
        for (WTreeListNode<?> node : inventory.itemList.lines)
            if (node.item instanceof InventoryListComponent.InventoryTreeListItem
                    && ((InventoryListComponent.InventoryTreeListItem)node.item).item.getId() == backpack.getId()) node.setOpen(true);
        inventory.itemList.recalcLines(); inventorySkin.tick(); wagonSkin.tick();
        FlexComponent wagonBody = (FlexComponent)HighResWindowAccess.get(wagonSkin.itemTable(), "treeList");
        transferCrate(hud, inventorySkin, amphora.getId(), wagonSkin, wagonBody.parent.y + 40, wagon.id,
                "Amphora from player inventory to wagon");
        // Apply the native listener/model changes received after the server move.
        inventory.removeInventoryItem(amphora); playerItems.remove(amphora.getId());
        HighResWindowAccess.set(amphora, "parentId", wagon.id); wagonList.addInventoryItem(amphora);
        inventory.itemList.recalcLines(); wagonSkin.tick(); inventorySkin.tick();
        check(((InventoryListComponent.InventoryTreeListItem)inventory.itemList.lines.get(inventory.itemList.lines.size()-1).item)
                .item.getId() == contents.getId(), "Last player row is inside an expanded backpack after amphora removal");
        FlexComponent body = (FlexComponent)HighResWindowAccess.get(inventory.itemList, "treeList");
        int height = ((Number)HighResWindowAccess.get(inventory.itemList, "lineHeight")).intValue();
        int blankY = body.y + inventory.itemList.lines.size() * height + 25;
        movedTarget = Long.MIN_VALUE;
        inventory.itemDropped(body.x + 100, blankY, (DraggableComponent)wagonSkin.itemTable().lines.get(0).item);
        check(movedTarget == inventoryId, "Native blank inventory drop selects player inventory, not the last row's backpack");
        transferCrate(hud, wagonSkin, amphora.getId(), inventorySkin, blankY, inventoryId,
                "Amphora returns from wagon into blank player inventory after native model update");
        body = (FlexComponent)HighResWindowAccess.get(inventory.itemList, "treeList");
        int lastRowBottom = body.y + inventory.itemList.lines.size() * height;
        transferCrate(hud, wagonSkin, amphora.getId(), inventorySkin, lastRowBottom + height/2, inventoryId,
                "Incoming amphora leaves the last nested row's appended gap for blank player inventory", lastRowBottom - 1);
        body = (FlexComponent)HighResWindowAccess.get(inventory.itemList, "treeList");
        int backpackRow = -1;
        for(int n=0;n<inventory.itemList.lines.size();n++)
            if(((InventoryListComponent.InventoryTreeListItem)inventory.itemList.lines.get(n).item).item.getId()==backpack.getId()) backpackRow=n;
        transferCrate(hud, wagonSkin, amphora.getId(), inventorySkin, body.y + backpackRow*height + height/2, backpack.getId(),
                "Explicit backpack row drop still uses the backpack and native capacity rules");
        wagonList.removeInventoryItem(amphora); HighResWindowAccess.set(amphora, "parentId", inventoryId);
        playerItems.put(amphora.getId(), amphora); inventory.addInventoryItem(amphora);
        for (InventoryMetaItem value : new InventoryMetaItem[]{contents, backpack, amphora}) {
            inventory.removeInventoryItem(value); playerItems.remove(value.getId());
        }
        inventory.itemList.recalcLines(); inventorySkin.tick(); window.closePressed();
        System.out.println("AMPHORA_ROUND_TRIP_OK: player to wagon, native listener updates, wagon to blank player inventory beside expanded backpack");
    }
    @SuppressWarnings("unchecked")
    private static void verifyInventoryOpacity(HeadsUpDisplay hud,HighResNativeWindow skin,HighResNativeWindow other,File output) throws Exception {
        com.wurmonline.client.renderer.backend.Queue batch=InventoryOpacityPreview.begin();
        for(int n=0;n<3;n++) {
            com.wurmonline.client.renderer.backend.Primitive primitive=(com.wurmonline.client.renderer.backend.Primitive)
                    unsafe().allocateInstance(com.wurmonline.client.renderer.backend.Primitive.class);
            primitive.a=n==1?.4f:1f;primitive.blendmode=n==1?com.wurmonline.client.renderer.backend.Primitive.BlendMode.ALPHABLEND:
                    com.wurmonline.client.renderer.backend.Primitive.BlendMode.OPAQUE;
            primitive.texture=new com.wurmonline.client.resources.textures.Texture[1];batch.getQueue()[n]=primitive;
        }
        HighResWindowAccess.set(batch,"queueCount",3);HighResInventoryOpacity.apply(batch,1,3,50);
        check(batch.getQueue()[0].a==1f&&batch.getQueue()[0].blendmode==com.wurmonline.client.renderer.backend.Primitive.BlendMode.OPAQUE,
                "Other windows' queued primitives remain unchanged");
        check(Math.abs(batch.getQueue()[1].a-.2f)<.0001f&&batch.getQueue()[2].a==.5f
                        &&batch.getQueue()[2].blendmode==com.wurmonline.client.renderer.backend.Primitive.BlendMode.ALPHABLEND,
                "Existing texture masks multiply opacity and opaque native primitives enable alpha blending");
        InventoryOpacityPreview.finish();
        Map<WurmTreeList<?>,HighResInventoryFilter> filters=(Map<WurmTreeList<?>,HighResInventoryFilter>)staticField(HighResWindowIntegration.class,"FILTERS");
        HighResInventoryFilter filter=filters.get(skin.itemTable());filter.layout();
        HighResInventoryOpacity slider=filter.opacity;
        check(slider.height==filter.field.height&&Math.abs(slider.width-filter.field.width/3*1.3f)<=.5f&&slider.x==filter.field.x+filter.field.width+6,
                "Opacity field is 30 percent wider than its previous one-third width, immediately right of Filter");
        int y=slider.y+slider.height/2;
        check(hud.getComponentAt(slider.thumbX()+1,y)==slider,"Opacity thumb receives native HUD hit testing");
        int otherValue=other.opacityPercent();
        hud.mousePressed(slider.thumbX()+1,y,0,0);hud.mouseDragged(slider.x-100,y);hud.mouseReleased(slider.x-100,y,0);
        check(skin.opacityPercent()==10&&other.opacityPercent()==otherValue,"Left drag clamps at 10 and only changes its own window");
        check(HighResWindowAccess.get(hud,"draggedItem")==null,"Opacity gesture does not start an item drag");
        hud.mousePressed(slider.thumbX()+1,y,0,0);hud.mouseDragged(slider.x+slider.width+100,y);hud.mouseReleased(slider.x+slider.width+100,y,0);
        check(skin.opacityPercent()==100,"Right drag reaches full opacity");
        int oldDefault=org.highreshud.client.HighResWindowsSettings.defaultInventoryOpacity;
        org.highreshud.client.HighResWindowsSettings.defaultInventoryOpacity=35;
        check(skin.opacityPercent()==100,"Changing the default keeps already-open windows independent");
        hud.hideComponent(skin.window);
        java.lang.reflect.Method show=HeadsUpDisplay.class.getDeclaredMethod("addComponent",WurmComponent.class);show.setAccessible(true);show.invoke(hud,skin.window);
        check(skin.opacityPercent()==35,"Reopened inventory initializes from the current default");
        ContainerView fresh=new ContainerView(9300,"Opacity test wagon");hud.addInventoryWindow(fresh);
        Map<InventoryMetaWindowView,ItemListWindow> nativeWindows=(Map<InventoryMetaWindowView,ItemListWindow>)HighResWindowAccess.get(hud,"inventoryWindows");
        WWindow freshWindow=nativeWindows.get(fresh);
        check(((Map<WurmComponent,HighResNativeWindow>)staticField(HighResWindowIntegration.class,"WINDOWS")).get(freshWindow).opacityPercent()==35,
                "New server container uses the configured default opacity");
        freshWindow.closePressed();
        for(String language:HudMessages.languageCodes()) {
            HudMessages.useLanguage(language);filter.layout();
            com.wurmonline.client.renderer.PickData pick=new com.wurmonline.client.renderer.PickData(hud);
            check(hud.pick(slider.x+5,slider.y+5,pick)&&pick.getText().contains(HudMessages.tr("Inventory window opacity: {0}%",35)),
                    "Opacity hover changes language without changing the percentage");
        }
        HudMessages.useLanguage("en");org.highreshud.client.HighResWindowsSettings.defaultInventoryOpacity=oldDefault;
        int ox=skin.window.x,oy=skin.window.y,ow=skin.window.width,oh=skin.window.height;
        BufferedImage image=new BufferedImage(1620,390,BufferedImage.TYPE_INT_ARGB);HudButtonProbe.previewGraphics(image);
        HudButtonProbe.canvas.fill(UiColor.rgb(0x777d78),1f,0,0,image.getWidth(),image.getHeight());
        com.wurmonline.client.renderer.backend.Queue queue=InventoryOpacityPreview.begin();
        for(int n=0;n<3;n++) {
            skin.window.setLocation(20+n*535,35,515,320);skin.opacityPercent(new int[]{10,50,100}[n]);
            int first=queue.getQueueCount();HighResWindowIntegration.renderWindow(skin.window,queue);
            int end=queue.getQueueCount(),sliderPrimitives=3;
            for(int p=first;p<end-sliderPrimitives;p++)check(queue.getQueue()[p].a<=skin.opacityPercent()/100f+.0001f,
                    "Window frames, fields, glyphs and text respect intentional per-window alpha");
            for(int p=end-sliderPrimitives;p<end;p++)check(queue.getQueue()[p].a==1f,"Recovery slider remains fully visible");
        }
        InventoryOpacityPreview.finish();HudButtonProbe.finishPreview();
        ImageIO.write(image,"png",new File(output.getParentFile(),"hud-inventory-opacity-preview.png"));
        skin.window.setLocation(ox,oy,ow,oh);skin.opacityPercent(100);HudMessages.useLanguage("en");
        System.out.println("INVENTORY_OPACITY_OK: 30 percent wider field, 10-100 native mouse drag, per-window isolation, reopen default, four languages, whole-window native queue alpha, visible recovery slider");
    }
    @SuppressWarnings("unchecked") private static void renderSettingsPreview(HeadsUpDisplay hud,File output) throws Exception {
        java.lang.reflect.Method method=Class.forName("org.highreshud.client.ModSettingsSchema").getDeclaredMethod("specs");method.setAccessible(true);
        final List<org.chamomilo.wurm.update.ModSetting> specs=(List<org.chamomilo.wurm.update.ModSetting>)method.invoke(null);
        final Properties values=new Properties();for(org.chamomilo.wurm.update.ModSetting spec:specs)values.setProperty(spec.key,spec.defaultValue);
        org.chamomilo.wurm.update.SharedModSettings.Provider provider=new org.chamomilo.wurm.update.SharedModSettings.Provider() {
            public List<org.chamomilo.wurm.update.ModSetting> settings() {return specs;}
            public String value(String key) {return values.getProperty(key);}
            public void change(String key,String value) {throw new AssertionError("Documentation preview must not modify settings");}
        };
        org.chamomilo.wurm.update.UpdatePreferences previous=HudButtonProbe.updaterPreferences;
        try {
            HudButtonProbe.updaterPreferences=new org.chamomilo.wurm.update.UpdatePreferences(new File(output.getParentFile(),"settings-preview-preferences.properties").toPath());
            HudButtonProbe.updaterPreferences.setForcedLanguage("en");
            ChamomiloUpdateSettingsWindow window=new ChamomiloUpdateSettingsWindow(hud,"highres-hud","High-res HUD",provider);
            window.setLocation(10,10,780,620);
            check(((List<?>)HighResWindowAccess.get(window,"rows")).size()==specs.size(),"Latest updater Settings consumes the complete product schema");
            BufferedImage image=new BufferedImage(800,640,BufferedImage.TYPE_INT_ARGB);HudButtonProbe.previewGraphics(image);
            HudButtonProbe.canvas.fill(UiColor.rgb(0x17140f),1f,0,0,image.getWidth(),image.getHeight());
            window.render(null,1f);HudButtonProbe.finishPreview();ImageIO.write(image,"png",new File(output.getParentFile(),"hud-settings-preview.png"));
        } finally {HudButtonProbe.updaterPreferences=previous;}
    }
    private static void verifyFontTransferIntegration(HeadsUpDisplay hud, HighResNativeWindow inventorySkin,
            Map<Long, InventoryMetaItem> playerItems, Map<InventoryMetaWindowView, ItemListWindow> containers,
            Map<WurmComponent, HighResNativeWindow> skins) throws Exception {
        int original = org.highreshud.client.HighResWindowsSettings.fontSize;
        int rowHeight = ((Number)HighResWindowAccess.get(inventorySkin.itemTable(), "lineHeight")).intValue();
        com.wurmonline.client.renderer.gui.text.TextFont heading = HighResWindowTypography.heading();
        try {
            org.highreshud.client.HighResWindowsSettings.fontSize = 24;
            HighResWindowIntegration.refreshTypography();
            check(HighResDataTypography.pixels(14) == 24
                            && ((Number)HighResWindowAccess.get(inventorySkin.itemTable(), "lineHeight")).intValue() > rowHeight,
                    "Live 0.7 font setting enlarges native item rows before transfers");
            check(heading.getHeight() == HighResWindowTypography.heading().getHeight()
                            && heading.getWidth("Heading Заголовок") == HighResWindowTypography.heading().getWidth("Heading Заголовок"),
                    "Item font changes retain the existing heading role");
            verifyCrateTransfers(hud, containers, skins);
            verifyAmphoraRoundTrip(hud, inventorySkin, playerItems, containers, skins);
        } finally {
            org.highreshud.client.HighResWindowsSettings.fontSize = original;
            HighResWindowIntegration.refreshTypography();
        }
        System.out.println("FONT_TRANSFER_INTEGRATION_OK: live 24px item rows, fixed headings, crate transfers and native amphora round trip");
    }
    private static void transferCrate(HeadsUpDisplay hud, HighResNativeWindow source, long sourceId,
                                      HighResNativeWindow destination, int dropY, long expectedTarget, String scenario) {
        transferCrate(hud,source,sourceId,destination,dropY,expectedTarget,scenario,Integer.MIN_VALUE);
    }
    private static void transferCrate(HeadsUpDisplay hud, HighResNativeWindow source, long sourceId,
                                      HighResNativeWindow destination, int dropY, long expectedTarget, String scenario,int previewY) {
        WurmTreeList<?> table = source.itemTable();
        FlexComponent body = (FlexComponent)HighResWindowAccess.get(table, "treeList");
        int height = ((Number)HighResWindowAccess.get(table, "lineHeight")).intValue(), row = -1;
        for (int n = 0; n < table.lines.size(); n++)
            if (((InventoryMetaItem)HighResWindowAccess.get(table.lines.get(n).item, "item")).getId() == sourceId) row = n;
        check(row >= 0, scenario + ": source crate is visible");
        int x = body.x + 100, y = body.y + row * height + height / 2;
        check(hud.mousePressed(x, y, 0, 0), scenario + ": native item press");
        hud.mouseDragged(x + 12, y);
        check(HighResWindowAccess.get(hud, "draggedItem") != null, scenario + ": native drag starts");
        FlexComponent targetBody = (FlexComponent)HighResWindowAccess.get(destination.itemTable(), "treeList");
        x = targetBody.x + 100;
        if(previewY!=Integer.MIN_VALUE) {
            hud.mouseDragged(x,previewY);dragNow+=1_000_000_000L;
            HighResWindowIntegration.inventoryDragFrame(hud,x,previewY);
        }
        hud.mouseDragged(x, dropY);
        dragNow+=1_000_000_000L;HighResWindowIntegration.inventoryDragFrame(hud,x,dropY);
        movedTarget = Long.MIN_VALUE; movedIds = null;
        hud.mouseReleased(x, dropY, 0);
        check(movedTarget == expectedTarget && (expectedTarget == Long.MIN_VALUE ? movedIds == null : Arrays.equals(movedIds, new long[]{sourceId})),
                scenario + ": expected native target " + expectedTarget + ", actual " + movedTarget + ", ids=" + Arrays.toString(movedIds));
        check(HighResWindowAccess.get(hud, "draggedItem") == null, scenario + ": drag ends");
    }

    private static InventoryListComponent itemList(HighResNativeWindow skin) {
        return (InventoryListComponent)HighResWindowAccess.get(skin.window,"component");
    }
    private static WTreeListNode<?> itemNode(WTreeListNode<?> node,long id) {
        if(node.item instanceof InventoryContainerWindow.InventoryContainerItem
                &&((InventoryContainerWindow.InventoryContainerItem)node.item).getId()==id)return node;
        if(node.item instanceof InventoryListComponent.InventoryTreeListItem&&!node.item.getClass().getSimpleName().equals("GroupTreeListItem")
                &&((InventoryListComponent.InventoryTreeListItem)node.item).item.getId()==id)return node;
        for(WTreeListNode<?> child:node.children) {WTreeListNode<?> match=itemNode(child,id);if(match!=null)return match;}
        return null;
    }
    private static WTreeListNode<?> itemNode(HighResNativeWindow skin,long id) {
        return itemNode((WTreeListNode<?>)HighResWindowAccess.get(skin.itemTable(),"rootNode"),id);
    }
    private static int rowY(HighResNativeWindow skin,WTreeListNode<?> node,int offset) {
        return ((FlexComponent)HighResWindowAccess.get(skin.itemTable(),"treeList")).y
                +skin.itemTable().lines.indexOf(node)*((Number)HighResWindowAccess.get(skin.itemTable(),"lineHeight")).intValue()+offset;
    }
    private static void startItemDrag(HeadsUpDisplay hud,HighResNativeWindow skin,long id) {
        FlexComponent body=(FlexComponent)HighResWindowAccess.get(skin.itemTable(),"treeList");
        int height=((Number)HighResWindowAccess.get(skin.itemTable(),"lineHeight")).intValue();
        int x=body.x+100,y=rowY(skin,itemNode(skin,id),height/2);
        check(hud.mousePressed(x,y,0,0),"Timed drag starts at native row");hud.mouseDragged(x+12,y);
        check(HighResWindowAccess.get(hud,"draggedItem")!=null,"Timed drag crosses native movement threshold");
    }
    private static void received(HeadsUpDisplay hud,HighResNativeWindow from,HighResNativeWindow to,InventoryMetaItem item,long parent) {
        itemList(from).removeInventoryItem(item);HighResWindowAccess.set(item,"parentId",parent);itemList(to).addInventoryItem(item);
        from.itemTable().recalcLines();to.itemTable().recalcLines();HighResWindowIntegration.beginFrame(hud);
    }
    private static void verifyTimedInventoryDrag(HeadsUpDisplay hud,Map<InventoryMetaWindowView,ItemListWindow> containers,
            Map<WurmComponent,HighResNativeWindow> skins) throws Exception {
        ContainerView from=new ContainerView(9400,"timed source"),to=new ContainerView(9500,"timed wagon");
        HighResWindowAccess.set(from.root,"typeBits",(short)8);HighResWindowAccess.set(to.root,"typeBits",(short)8);
        hud.addInventoryWindow(from);hud.addInventoryWindow(to);
        ItemListWindow sourceWindow=containers.get(from),targetWindow=containers.get(to);
        sourceWindow.setLocation(1180,30,420,260);targetWindow.setLocation(1180,340,420,300);
        HighResNativeWindow source=skins.get(sourceWindow),destination=skins.get(targetWindow);
        InventoryMetaItem incoming=item(9401,from.id,"A incoming",50),middle=item(9501,to.id,"Middle item",50),last=item(9502,to.id,"Z last",50);
        InventoryMetaItem bag=item(9503,to.id,"bag",50),secondBag=item(9504,to.id,"bag",40),inside=item(9505,bag.getId(),"thread",50);
        HighResWindowAccess.set(bag,"typeBits",(short)8);HighResWindowAccess.set(secondBag,"typeBits",(short)8);
        itemList(source).addInventoryItem(incoming);
        for(InventoryMetaItem value:new InventoryMetaItem[]{middle,last,bag,secondBag,inside})itemList(destination).addInventoryItem(value);
        source.tick();destination.tick();destination.itemTable().recalcLines();
        HighResInventoryDrag drag=(HighResInventoryDrag)staticField(HighResWindowIntegration.class,"INVENTORY_DRAG");
        WTreeListNode<?> bagNode=itemNode(destination,bag.getId()),group=bagNode.parentNode;
        check(group.item!=null&&group.item.getClass().getSimpleName().equals("GroupTreeListItem"),"Fixture contains real native grouped containers");
        group.setOpen(false);destination.itemTable().recalcLines();
        FlexComponent body=(FlexComponent)HighResWindowAccess.get(destination.itemTable(),"treeList");int x=body.x+100;
        int height=((Number)HighResWindowAccess.get(destination.itemTable(),"lineHeight")).intValue();
        int y=rowY(destination,group,height/2),count=destination.itemTable().lines.size();
        startItemDrag(hud,source,incoming.getId());hud.mouseDragged(x,y);
        dragNow+=999_000_000L;HighResWindowIntegration.inventoryDragFrame(hud,x,y);
        check(!group.isOpen()&&!drag.hasGap(destination.itemTable())&&destination.itemTable().lines.size()==count,
                "At 999ms a grouped container stays closed and the destination has no position preview");
        movedTarget=Long.MIN_VALUE;hud.mouseReleased(x,y,0);
        check(movedTarget==to.id,"Fast drop over a container group selects the wagon root");
        received(hud,source,destination,incoming,to.id);
        WTreeListNode<?> arrived=itemNode(destination,incoming.getId());
        check(arrived.parentNode.children.get(arrived.parentNode.children.size()-1)==arrived,"Quick incoming item is appended despite alphabetical name sorting");
        destination.itemTable().recalcLines();check(destination.itemTable().lines.get(destination.itemTable().lines.size()-1)==arrived,"Append order survives native recalc");
        received(hud,destination,source,incoming,from.id);
        startItemDrag(hud,source,incoming.getId());y=rowY(destination,itemNode(destination,middle.getId()),1);hud.mouseDragged(x,y);
        dragNow+=999_000_000L;HighResWindowIntegration.inventoryDragFrame(hud,x,y);
        check(!drag.hasGap(destination.itemTable()),"No row insertion during the first 999ms");
        dragNow+=1_000_000L;HighResWindowIntegration.inventoryDragFrame(hud,x,y);
        check(drag.hasGap(destination.itemTable()),"At exactly 1000ms a stationary mouse activates the insertion row");
        movedTarget=Long.MIN_VALUE;hud.mouseReleased(x,y,0);check(movedTarget==to.id,"Precise gap retains the physical wagon destination");
        received(hud,source,destination,incoming,to.id);
        arrived=itemNode(destination,incoming.getId());WTreeListNode<?> anchor=itemNode(destination,middle.getId());
        check(arrived.parentNode==anchor.parentNode&&arrived.parentNode.children.indexOf(arrived)+1==arrived.parentNode.children.indexOf(anchor),
                "Server arrival is inserted immediately before the chosen row");
        received(hud,destination,source,incoming,from.id);
        startItemDrag(hud,source,incoming.getId());y=rowY(destination,anchor,height-1);hud.mouseDragged(x,y);
        dragNow+=1_000_000_000L;HighResWindowIntegration.inventoryDragFrame(hud,x,y);hud.mouseReleased(x,y,0);
        received(hud,source,destination,incoming,to.id);arrived=itemNode(destination,incoming.getId());
        check(anchor.parentNode.children.indexOf(anchor)+1==anchor.parentNode.children.indexOf(arrived),"After-row position survives the asynchronous server arrival");
        received(hud,destination,source,incoming,from.id);
        group.setOpen(false);destination.itemTable().recalcLines();y=rowY(destination,group,height/2);
        startItemDrag(hud,source,incoming.getId());hud.mouseDragged(x,y);dragNow+=999_000_000L;
        HighResWindowIntegration.inventoryDragFrame(hud,x,y);hud.mouseDragged(-20,-20);hud.mouseDragged(x,y);
        dragNow+=1_000_000L;HighResWindowIntegration.inventoryDragFrame(hud,x,y);
        check(!group.isOpen()&&!drag.hasGap(destination.itemTable()),"Leaving and re-entering resets both hover timers");
        dragNow+=999_000_000L;HighResWindowIntegration.inventoryDragFrame(hud,x,y);
        check(group.isOpen()&&destination.itemTable().lines.contains(bagNode),"One uninterrupted second opens a real group without a click");
        y=rowY(destination,bagNode,height/2);hud.mouseDragged(x,y);movedTarget=Long.MIN_VALUE;hud.mouseReleased(x,y,0);
        check(movedTarget==bag.getId(),"Expanded group lets the player select a specific nested container");
        received(hud,source,destination,incoming,bag.getId());
        arrived=itemNode(destination,incoming.getId());check(arrived.parentNode==bagNode,"Native model puts the received item in the selected bag");
        received(hud,destination,source,incoming,from.id);
        InventoryMetaItem thirdBag=item(9403,from.id,"bag",30);HighResWindowAccess.set(thirdBag,"typeBits",(short)8);
        itemList(source).addInventoryItem(thirdBag);source.itemTable().recalcLines();
        group.setOpen(true);destination.itemTable().recalcLines();
        WTreeListNode<?> secondBagNode=itemNode(destination,secondBag.getId());
        startItemDrag(hud,source,thirdBag.getId());y=rowY(destination,secondBagNode,1);hud.mouseDragged(x,y);
        dragNow+=1_000_000_000L;HighResWindowIntegration.inventoryDragFrame(hud,x,y);movedTarget=Long.MIN_VALUE;hud.mouseReleased(x,y,0);
        check(movedTarget==to.id,"Position inside a virtual group selects its physical inventory, not the first hidden bag");
        received(hud,source,destination,thirdBag,to.id);WTreeListNode<?> thirdBagNode=itemNode(destination,thirdBag.getId());
        check(thirdBagNode.parentNode==group&&group.children.indexOf(thirdBagNode)+1==group.children.indexOf(secondBagNode),
                "Received grouped container retains the chosen position inside the expanded native group");
        received(hud,destination,source,thirdBag,from.id);bagNode.setOpen(true);destination.itemTable().recalcLines();
        startItemDrag(hud,source,thirdBag.getId());y=rowY(destination,bagNode,height-1);hud.mouseDragged(x,y);
        dragNow+=1_000_000_000L;HighResWindowIntegration.inventoryDragFrame(hud,x,y);
        WTreeListNode<?> previewGap=(WTreeListNode<?>)HighResWindowAccess.get(drag,"gap");
        check(destination.itemTable().lines.indexOf(previewGap)>destination.itemTable().lines.indexOf(itemNode(destination,inside.getId())),
                "After an expanded bag, the preview follows its children instead of suggesting a false nested position");
        y=rowY(destination,previewGap,height/2);hud.mouseReleased(x,y,0);received(hud,source,destination,thirdBag,to.id);
        thirdBagNode=itemNode(destination,thirdBag.getId());
        check(group.children.indexOf(bagNode)+1==group.children.indexOf(thirdBagNode)
                        &&destination.itemTable().lines.indexOf(thirdBagNode)>destination.itemTable().lines.indexOf(itemNode(destination,inside.getId())),
                "After-server position matches the preview following the expanded bag's subtree");
        group.setOpen(false);destination.itemTable().recalcLines();
        startItemDrag(hud,destination,middle.getId());y=rowY(destination,group,1);hud.mouseDragged(x,y);
        dragNow+=999_000_000L;HighResWindowIntegration.inventoryDragFrame(hud,x,y);check(!group.isOpen(),"Same-window group respects the hover delay");
        dragNow+=1_000_000L;HighResWindowIntegration.inventoryDragFrame(hud,x,y);check(group.isOpen(),"Same-window drag also expands a group at one second");
        hud.resetDrag();
        InventoryMetaItem secondIncoming=item(9402,from.id,"F incoming",50);itemList(source).addInventoryItem(secondIncoming);source.itemTable().recalcLines();
        startItemDrag(hud,source,incoming.getId());itemNode(source,secondIncoming.getId()).setSelected(true);
        y=rowY(destination,itemNode(destination,last.getId()),1);hud.mouseDragged(x,y);
        dragNow+=1_000_000_000L;HighResWindowIntegration.inventoryDragFrame(hud,x,y);hud.mouseReleased(x,y,0);
        check(Arrays.equals(movedIds,new long[]{incoming.getId(),secondIncoming.getId()}),"Multi-selection preserves native move IDs");
        received(hud,source,destination,secondIncoming,to.id);received(hud,source,destination,incoming,to.id);
        WTreeListNode<?> firstArrival=itemNode(destination,incoming.getId()),secondArrival=itemNode(destination,secondIncoming.getId()),lastNode=itemNode(destination,last.getId());
        check(lastNode.parentNode.children.indexOf(firstArrival)+1==lastNode.parentNode.children.indexOf(secondArrival)
                        &&lastNode.parentNode.children.indexOf(secondArrival)+1==lastNode.parentNode.children.indexOf(lastNode),
                "Multiple arrivals in reverse server order stay together at the chosen position");
        received(hud,destination,source,incoming,from.id);
        startItemDrag(hud,source,incoming.getId());y=rowY(destination,anchor,1);hud.mouseDragged(x,y);
        dragNow+=1_000_000_000L;HighResWindowIntegration.inventoryDragFrame(hud,x,y);hud.mouseReleased(x,y,0);
        List<?> unchanged=new ArrayList<>(anchor.parentNode.children);HighResWindowIntegration.beginFrame(hud);
        check(unchanged.equals(anchor.parentNode.children),"Unconfirmed server move does not reorder unrelated target rows");
        dragNow+=30_000_000_000L;HighResWindowIntegration.beginFrame(hud);
        check(((List<?>)HighResWindowAccess.get(drag,"pending")).isEmpty(),"Unconfirmed positions expire after thirty seconds");
        InventoryMetaItem quiverRoot=item(9600,1,"quiver",50);
        InventoryContainerWindow quiver=new InventoryContainerWindow(quiverRoot,null);
        Map<Long,InventoryContainerWindow> playerContainers=(Map<Long,InventoryContainerWindow>)HighResWindowAccess.get(hud,"inventoryContainerWindows");
        playerContainers.put(quiverRoot.getId(),quiver);hud.getComponents().add(quiver);quiver.setLocation(1610,340,300,300);
        HighResWindowIntegration.beginFrame(hud);HighResNativeWindow quiverSkin=skins.get(quiver);
        InventoryMetaItem quiverLast=item(9601,quiverRoot.getId(),"Z quiver item",50);quiver.addInventoryItem(quiverLast);quiverSkin.itemTable().recalcLines();
        startItemDrag(hud,source,incoming.getId());FlexComponent quiverBody=(FlexComponent)HighResWindowAccess.get(quiverSkin.itemTable(),"treeList");
        int qx=quiverBody.x+100,qy=quiverBody.parent.y+40;hud.mouseDragged(qx,qy);hud.mouseReleased(qx,qy,0);
        check(movedTarget==quiverRoot.getId(),"Quick player-container transfer uses its native root ID");
        itemList(source).removeInventoryItem(incoming);HighResWindowAccess.set(incoming,"parentId",quiverRoot.getId());quiver.addInventoryItem(incoming);
        quiverSkin.itemTable().recalcLines();HighResWindowIntegration.beginFrame(hud);
        check(quiverSkin.itemTable().lines.get(quiverSkin.itemTable().lines.size()-1)==itemNode(quiverSkin,incoming.getId()),"Quick transfer also appends in the distinct native player-container window");
        startItemDrag(hud,quiverSkin,incoming.getId());y=rowY(destination,anchor,1);hud.mouseDragged(x,y);hud.mouseReleased(x,y,0);
        check(movedTarget==to.id&&Arrays.equals(movedIds,new long[]{incoming.getId()}),"Player-container source keeps the correct native item IDs");
        quiver.removeInventoryItem(incoming);HighResWindowAccess.set(incoming,"parentId",to.id);itemList(destination).addInventoryItem(incoming);
        destination.itemTable().recalcLines();HighResWindowIntegration.beginFrame(hud);
        check(destination.itemTable().lines.get(destination.itemTable().lines.size()-1)==itemNode(destination,incoming.getId()),"Player-container source appends after the native server arrival");
        received(hud,destination,source,incoming,from.id);quiver.closePressed();
        startItemDrag(hud,source,incoming.getId());hud.mouseDragged(x,y);hud.mouseReleased(x,y,0);
        sourceWindow.closePressed();targetWindow.closePressed();
        check(((List<?>)HighResWindowAccess.get(drag,"pending")).isEmpty(),"Closing the target removes pending arrival positions");
        System.out.println("TIMED_INVENTORY_DRAG_OK: 999/1000ms, quick root append, before/after server-arrival positions, hover reset, native groups and group positions, nested container, same-window expansion, multi-selection, rejection timeout, close cleanup");
    }
    private static void verifyCharacter(HeadsUpDisplay hud,InventoryMetaWindowView view,Map<WurmComponent,HighResNativeWindow> skins,File output) throws Exception {
        com.wurmonline.client.renderer.PlayerBodyRenderable body=(com.wurmonline.client.renderer.PlayerBodyRenderable)unsafe().allocateInstance(com.wurmonline.client.renderer.PlayerBodyRenderable.class);
        PaperDollInventory character=new PaperDollInventory(body,view);
        HighResWindowAccess.set(hud,"paperdollInventory",character);
        HighResWindowAccess.set(hud,"paperDollRendererInventory",unsafe().allocateInstance(PaperDollRenderer.class));
        Map<Long,PaperDollSlot> slots=(Map<Long,PaperDollSlot>)HighResWindowAccess.get(character,"frameList");
        for(Object definition:(List<?>)HighResWindowAccess.get(character,"slotList")) {
            byte id=(byte)HighResCharacterWindow.number(definition,"id");
            slots.put(500L+id,new PaperDollSlot(id,item(500L+id,-1,"body part "+id,0),null,null,0,0,1,1,0,0,44,44,null,null,16,16));
        }
        int cornerId=600;
        for(String field:new String[]{"bodyItem","equippedWeightItem","workingHandItem"})
            HighResWindowAccess.set(character,field,new PaperDollSlot((byte)-1,item(cornerId++,-1,field,0),null,null,0,0,1,1,0,0,34,58,null,null,0,0));
        InventoryMetaItem woundMeta=item(900,503,"Bruise",0);HighResWindowAccess.set(woundMeta,"damage",35f);
        PaperDollItem wound=new PaperDollItem(null,(byte)3,woundMeta,32,32);slots.get(503L).addWound(wound);
        HighResWindowAccess.set(character,"inventoryItem",item(700,-1,"Inventory",0));
        character.setPosition(1220,20);hud.getComponents().add(character);
        HighResCharacterWindow skin=new HighResCharacterWindow(character,hud);skins.put(character,skin);skin.install();
        check(skin.modelView(),"Character model is the default view");
        InventoryMetaItem originalTool=(InventoryMetaItem)HighResWindowAccess.get(hud,"activeToolItem");
        check(character.getCommandTargetsFrom(skin.px(14),skin.py(430))[0]==600L,"Body corner retains native Manage/body command targets");
        check(character.getCommandTargetsFrom(skin.px(290),skin.py(430))[0]==602L,"Hand corner retains the native hand tool target");
        HighResWindowAccess.set(hud,"activeToolItem",character.getHandItem());
        check(skin.cornerActive("workingHandItem"),"Hand artwork reflects the selected hand tool");
        HighResWindowAccess.set(hud,"activeToolItem",originalTool);
        check(!skin.cornerActive("workingHandItem"),"Hand artwork returns to its unselected state for another tool");
        int ux=skin.unequip.x+40,uy=skin.unequip.y+12;
        int before=actionCount;
        hud.mousePressed(ux,uy,0,0);hud.mouseReleased(ux,uy,0);
        check(actionCount==before+1&&actionId==585&&Arrays.equals(actionTargets,new long[]{600}),
                "Unequip all dispatches one native armor action to the body target");
        hud.mousePressed(ux,uy,0,0);hud.mouseDragged(ux,uy-40);hud.mouseReleased(ux,uy-40,0);
        check(actionCount==before+1,"Unequip all cancels when dragged outside the button");
        int cx=skin.px(160),cy=skin.py(220);
        check(hud.mousePressed(cx,cy,0,0),"Model drag reaches native character input");hud.mouseDragged(cx+30,cy);hud.mouseReleased(cx+30,cy,0);
        check(((Number)HighResWindowAccess.get(character,"rotation")).floatValue()==30f,"Dragging rotates the native model");
        BufferedImage image=new BufferedImage(1680,690,BufferedImage.TYPE_INT_ARGB);HudButtonProbe.previewGraphics(image);
        HighResWindowIntegration.renderWindow(character,null);HudButtonProbe.finishPreview();
        check(modelWidth==512&&modelHeight==742,"Model offscreen target has twice the destination pixel density");
        PortraitPoseController pose=(PortraitPoseController)HighResWindowAccess.get(skin,"pose");
        long poseStart=System.nanoTime()-PortraitPoseController.TURN_DURATION;
        HighResWindowAccess.set(pose,"nextTurn",poseStart);skin.updatePose(poseStart);
        float targetRotation=((Number)HighResWindowAccess.get(pose,"rotationTarget")).floatValue();
        check(targetRotation>=-90f&&targetRotation<=90f,"Character uses the Healthbar automatic turn range");
        skin.updatePose(poseStart+PortraitPoseController.TURN_DURATION/2);
        check(((Number)HighResWindowAccess.get(character,"rotation")).floatValue()==pose.rotation(),
                "Character applies the interpolated automatic pose to the native model");
        skin.updatePose(poseStart+PortraitPoseController.TURN_DURATION);
        check(((Number)HighResWindowAccess.get(character,"rotation")).floatValue()==targetRotation,
                "Character completes the Healthbar three-second automatic turn");
        image=new BufferedImage(1680,690,BufferedImage.TYPE_INT_ARGB);HudButtonProbe.previewGraphics(image);
        HighResWindowIntegration.renderWindow(character,null);HudButtonProbe.finishPreview();
        check(modelWidth==512&&modelHeight==742,"Automatic Character pose retains the full-body render scale");
        hud.mousePressed(cx,cy,0,0);hud.mouseDragged(cx+20,cy);hud.mouseReleased(cx+20,cy,0);
        float manualRotation=((Number)HighResWindowAccess.get(character,"rotation")).floatValue();
        skin.updatePose(System.nanoTime());
        check(((Number)HighResWindowAccess.get(character,"rotation")).floatValue()==manualRotation,
                "Manual drag cancels the automatic transition and retains its angle");
        skin.zones.leftPressed(skin.zones.x+50,skin.zones.y+12,0);skin.zones.leftReleased(skin.zones.x+50,skin.zones.y+12);
        check(!skin.modelView(),"Completed view switch selects body zones");
        check(skin.hit(skin.px(150),skin.py(180),true)==3,"Torso hit follows the visible high-res geometry");
        check(skin.hit(skin.px(25),skin.py(145),false)==3,"Equipment hit follows high-res slot geometry");
        int wx=skin.px(150),wy=skin.py(180);
        hud.mousePressed(wx,wy,0,0);hud.mouseReleased(wx,wy,0);
        check(slots.get(503L).isWoundButtonOpen(),"Body-zone click opens native wound list");
        skin.tick();check(skin.woundAt(wound.getXPosition()+80,wound.getYPosition()+4)==wound,"Full wound row retains native selection and actions");
        List<PaperDollItem> woundList=(List<PaperDollItem>)HighResWindowAccess.get(slots.get(503L),"woundList");
        for(int i=1;i<10;i++)slots.get(503L).addWound(new PaperDollItem(null,(byte)3,item(900+i,503,"Bruise "+i,0),32,32));
        skin.tick();check(woundList.get(8).getXPosition()<0,"Long wound lists stay within the Character window");
        skin.nextWounds.leftPressed(skin.nextWounds.x+8,skin.nextWounds.y+8,0);skin.nextWounds.leftReleased(skin.nextWounds.x+8,skin.nextWounds.y+8);
        check(wound.getXPosition()<0&&skin.woundAt(woundList.get(8).getXPosition()+60,woundList.get(8).getYPosition()+5)==woundList.get(8),
                "Wound paging keeps the selected native wound identity");
        woundList.subList(1,woundList.size()).clear();skin.tick();
        BufferedImage reference=null;
        for(String language:HudMessages.languageCodes()) {
            HudMessages.useLanguage(language);skin.tick();
            for(float alpha:new float[]{0,.2f,1}) {
                BufferedImage shot=new BufferedImage(1680,690,BufferedImage.TYPE_INT_ARGB);HudButtonProbe.previewGraphics(shot);
                skin.drawing.render(null,alpha);HudButtonProbe.finishPreview();
                if(reference!=null)check(Arrays.equals(reference.getRGB(1220,20,410,HighResCharacterWindow.HEIGHT,null,0,410),shot.getRGB(1220,20,410,HighResCharacterWindow.HEIGHT,null,0,410)),"Character pixels ignore incoming alpha");
                reference=shot;
            }
            reference=null;
        }
        HudMessages.useLanguage("ru");skin.view(false);
        woundList.clear();HudButtonProbe.anatomyTints.clear();
        image=new BufferedImage(1680,690,BufferedImage.TYPE_INT_ARGB);HudButtonProbe.previewGraphics(image);HighResWindowIntegration.renderWindow(character,null);HudButtonProbe.finishPreview();
        check(!HudButtonProbe.anatomyTints.isEmpty(),"The anatomy view paints its detailed texture");
        for(float[] tint:HudButtonProbe.anatomyTints)check(Arrays.equals(tint,new float[]{1,1,1}),"Healthy anatomy retains its original colors");
        ImageIO.write(image.getSubimage(1220,20,410,HighResCharacterWindow.HEIGHT),"png",new File(output.getParentFile(),"hud-character-preview.png"));
        HighResWindowAccess.set(woundMeta,"damage",.1f);woundList.add(wound);HudButtonProbe.anatomyTints.clear();
        BufferedImage minor=new BufferedImage(1680,690,BufferedImage.TYPE_INT_ARGB);HudButtonProbe.previewGraphics(minor);HighResWindowIntegration.renderWindow(character,null);HudButtonProbe.finishPreview();
        boolean green=false;for(float[] tint:HudButtonProbe.anatomyTints)green|=tint[1]>tint[0]&&tint[1]>tint[2];
        check(green,"Minor wounds introduce a green anatomical zone");
        HudMessages.useLanguage("en");hud.getComponents().remove(character);skins.remove(character);
    }
    private static void verifyInventoryContainers(HeadsUpDisplay hud, Map<Long, InventoryContainerWindow> containers,
                                                  Map<Long, InventoryMetaItem> items, Set<InventoryMetaListener> listeners,
                                                  Map<WurmComponent, HighResNativeWindow> skins, File output) throws Exception {
        List<WWindow> previews = new ArrayList<>();
        for (String name : new String[]{"backpack", "quiver"}) {
            long id = 100 + previews.size();
            InventoryMetaItem root = item(id, 1, name, 50);
            InventoryMetaItem last = item(id * 10 + 1, id, "Zinc arrow", 90), first = item(id * 10 + 2, id, "Arrow", 30);
            root.getChildren().add(last); root.getChildren().add(first); items.put(id, root);
            if (name.equals("backpack")) hud.addInventoryContainerWindow(id);
            else check(hud.toggleInventoryContainerWindow(id), "Native quiver toggle opens its distinct window type");
            InventoryContainerWindow window = containers.get(id);
            HighResContainerWindow skin = (HighResContainerWindow) skins.get(window);
            check(skin != null && skin.itemTable() == HighResWindowAccess.get(window, "list") && listeners.contains(window),
                    "Player inventory container retains native list and listener: " + name);
            check(window.getComponent() == skin.itemTable(), "Native container content identity is unchanged");
            assertHeadings(skin.itemTable());
            assertDataFonts(window.getComponent());
            WTreeListNode<?> rootNode = (WTreeListNode<?>) HighResWindowAccess.get(skin.itemTable(), "rootNode");
            check(rootNode.children.get(0).item.getName().equals("Arrow"), "Existing container items sort by name ascending");
            InventoryMetaItem arriving = item(id * 10 + 3, id, "Aardvark arrow", 60);
            window.addInventoryItem(arriving);
            check(rootNode.children.get(0).item.getName().equals("Arrow"), "Later native item updates preserve the order from opening");
            int[] columns = skin.itemTable().columnWidths.clone();
            window.setLocation(20 + previews.size() * 370, 20, 340, 300);
            verifyInventoryDrag(hud,skin);
            resizeThroughHud(hud, skin);
            window.setLocation(20 + previews.size() * 370, 20, 340, 300);
            for (String language : HudMessages.languageCodes()) {
                HudMessages.useLanguage(language); skin.tick();
                check(skin.caption().equals(HudMessages.tr(name)) && Arrays.equals(columns, skin.itemTable().columnWidths),
                        "Native backpack/quiver caption translates without resizing columns: " + language);
            }
            sort(hud, skin.itemTable(), -2);
            sort(hud, skin.itemTable(), -2);
            check(((Number) HighResWindowAccess.get(skin.itemTable(), "sortOrder")).intValue() == -1,
                    "Native player container heading still toggles descending");
            skin.close();
            check(!containers.containsKey(id) && !skins.containsKey(window) && !listeners.contains(window),
                    "Native close removes container, listener and skin: " + name);
            hud.addInventoryContainerWindow(id);
            InventoryContainerWindow reopened = containers.get(id);
            assertHeadings(((HighResContainerWindow) skins.get(reopened)).itemTable());
            check(reopened.width >= 340 && reopened.height == 300 && listeners.contains(reopened),
                    "Reopened player container restores native size and listener");
            check(!hud.toggleInventoryContainerWindow(id) && !containers.containsKey(id) && !skins.containsKey(reopened)
                            && !listeners.contains(reopened), "Native toggle closes and releases the presentation adapter");
            hud.addInventoryContainerWindow(id);
            previews.add(containers.get(id));
        }
        HudMessages.useLanguage("ru");
        BufferedImage image = new BufferedImage(760, 340, BufferedImage.TYPE_INT_ARGB);
        HudButtonProbe.previewGraphics(image);
        for (WWindow window : previews) HighResWindowIntegration.renderWindow(window, null);
        HudButtonProbe.finishPreview();
        ImageIO.write(image, "png", new File(output.getParentFile(), "hud-player-containers-preview.png"));
        for (WWindow window : previews) skins.get(window).close();
        // Other mods can create a native window without going through HUD add/toggle.
        InventoryContainerWindow existing = new InventoryContainerWindow(items.get(100L), null);
        containers.put(100L, existing); hud.addDynamicComponent(existing);
        HighResWindowIntegration.beginFrame(hud);
        check(skins.get(existing) instanceof HighResContainerWindow, "Frame scan catches player containers opened by other mods");
        containers.clear(); hud.getComponents().remove(existing); HighResWindowIntegration.reconnected(hud);
        check(!skins.containsKey(existing), "Reconnect releases a removed player container adapter");
        // The fixture's disconnected inventory has no server to remove its listener.
        listeners.remove(existing); HudMessages.useLanguage("en");
    }
    private static void assertDataFonts(FlexComponent component) {
        if (component == null) return;
        if (component instanceof WurmTreeList) {
            check(component.text == com.wurmonline.client.renderer.gui.text.HudTextFonts.body(HighResDataTypography.pixels(14), false)
                    && component.textBold == com.wurmonline.client.renderer.gui.text.HudTextFonts.body(HighResDataTypography.pixels(14), true),
                    "Inventory/container/Friends share original-case Alegreya Sans data fonts");
            WurmComponent painter = (WurmComponent) HighResWindowAccess.get(component, "treeList");
            check(painter.text == component.text && painter.textBold == component.textBold
                            && ((Number) HighResWindowAccess.get(component, "lineHeight")).intValue() == component.text.getHeight() + 1,
                    "Native row painter and selection hit testing use the same shared data metrics");
        } else if (component instanceof WurmTextPanel) {
            int size = HighResDataTypography.pixels((Boolean) HighResWindowAccess.get(component, "useWrap") ? 16 : 14);
            check(component.text == com.wurmonline.client.renderer.gui.text.HudTextFonts.body(size, false)
                            && component.textBold == com.wurmonline.client.renderer.gui.text.HudTextFonts.body(size, true),
                    "Chat/Event/Friends histories and player names use the shared body role");
        }
        if (component instanceof WurmScrollPanel) assertDataFonts(((WurmScrollPanel) component).content);
        else if (component instanceof WurmBorderPanel)
            for (FlexComponent child : (FlexComponent[]) HighResWindowAccess.get(component, "components")) assertDataFonts(child);
        else if (component instanceof WurmDecorator) assertDataFonts(((WurmDecorator) component).component);
        else if (component instanceof WurmArrayPanel)
            for (Object child : ((WurmArrayPanel<?>) component).components) assertDataFonts((FlexComponent) child);
    }
    @SuppressWarnings("unchecked")
    private static void verifyDataTypography(File output) throws Exception {
        WurmTextPanel panel = new WurmTextPanel("Data typography", true);
        com.wurmonline.client.renderer.gui.text.TextFont nativeFont = panel.text;
        WurmTextPanel unrelated = new WurmTextPanel("Unrelated native window", true);
        com.wurmonline.client.renderer.gui.text.TextFont unrelatedFont = unrelated.text;
        WurmScrollPanel scroll = new WurmScrollPanel("Data typography scroll", panel);
        scroll.setSize(240, 90);
        panel.setSize(((FlexComponent) scroll.offs).width, panel.height);
        String[] samples = {"Rare carving Knife, steel · Quality 99.87", "Descrição: Árvore, aço e coração 99,87",
                "Beschreibung: Größe, Äste und Öl 99,87", "Описание: Колчан, сталь и рюкзак 99,87"};
        for (int i = 0; i < 24; i++) panel.addLine(samples[i % samples.length], .7f, .9f, .4f);
        List<com.wurmonline.shared.util.MulticolorLineSegment> segments = Arrays.asList(
                new com.wurmonline.shared.util.MulticolorLineSegment("Mind increased by ", (byte) 0),
                new com.wurmonline.shared.util.MulticolorLineSegment("0,000023 to 70,584198", (byte) 0));
        panel.addLine(segments); panel.gameTick(); scroll.layout();
        List<Object> originals = (List<Object>) HighResWindowAccess.get(panel, "orgLines");
        List<Object> saved = new ArrayList<>(originals);
        List<Object> visible = (List<Object>) HighResWindowAccess.get(panel, "lines");
        int oldHeight = panel.text.getHeight() + 1;
        scroll.yo = oldHeight * 3; scroll.isAtBottom = false; scroll.layout();
        Object topId = HighResWindowAccess.get(visible.get(3), "id");
        HighResWindowAccess.set(visible.get(3), "selected", true);
        HighResDataTypography.apply(panel); assertDataFonts(panel);
        check(originals.equals(saved) && unrelated.text == unrelatedFont, "Body font migration keeps native source history and unrelated font settings");
        check(HighResWindowAccess.get(originals.get(originals.size() - 1), "segments") == segments,
                "Multicolor messages retain their original segments and colors");
        int newHeight = panel.text.getHeight() + 1;
        check(HighResWindowAccess.get(visible.get(scroll.yo / newHeight), "id").equals(topId),
                "Font migration retains the first visible message anchor");
        boolean retainedSelection = false;
        for (Object line : visible) {
            if (HighResWindowAccess.get(line, "id").equals(topId)) retainedSelection |= (Boolean) HighResWindowAccess.get(line, "selected");
            check(((Number) HighResWindowAccess.get(line, "width")).intValue() + 4 <= panel.width,
                    "Native wrapping uses the new glyph advances: " + HighResWindowAccess.get(line, "text")
                            + " width=" + HighResWindowAccess.get(line, "width") + " available=" + (panel.width - 4));
        }
        check(retainedSelection, "Native message selection survives body-font reflow");
        for (int i = 0; i < 24; i++) check(HighResWindowAccess.get(originals.get(i), "text").equals(samples[i % samples.length]),
                "Data casing, Cyrillic, diacritics and punctuation remain intact");
        scroll.isAtBottom = true; panel.text = nativeFont; HighResDataTypography.apply(panel);
        check(scroll.yo == Math.max(0, panel.height - ((FlexComponent) scroll.offs).height) && scroll.isAtBottom,
                "Sticky-bottom message scrolling survives font changes");
        com.wurmonline.client.renderer.gui.text.TextFont heading=HighResWindowTypography.heading();
        org.highreshud.client.HighResWindowsSettings.fontSize=24;
        HighResDataTypography.apply(panel);assertDataFonts(panel);
        check(HighResDataTypography.pixels(14)==24 && HighResDataTypography.pixels(16)==24,
                "Explicit body font size controls both table data and paragraphs");
        check(HighResWindowTypography.heading().getHeight()==heading.getHeight()
                        && HighResWindowTypography.heading().getWidth("Heading Заголовок")==heading.getWidth("Heading Заголовок")
                        && unrelated.text==unrelatedFont,
                "Body font size does not change headings or unrelated native windows");
        org.highreshud.client.HighResWindowsSettings.fontSize=12;
        HighResDataTypography.apply(panel);check(HighResDataTypography.pixels(16)==12,
                "Explicit body size can be smaller than the native font preference");
        org.highreshud.client.HighResWindowsSettings.fontSize=0;
        HighResDataTypography.apply(panel);assertDataFonts(panel);
        BufferedImage image = new BufferedImage(760, 190, BufferedImage.TYPE_INT_ARGB); HudButtonProbe.previewGraphics(image);
        int row = 0;
        for (int size : new int[]{14, 16}) for (boolean bold : new boolean[]{false, true}) {
            com.wurmonline.client.renderer.gui.text.TextFont font = com.wurmonline.client.renderer.gui.text.HudTextFonts.body(size, bold);
            HudButtonProbe.ProbeFont delegate = (HudButtonProbe.ProbeFont) HighResWindowAccess.get(font, "delegate");
            check(delegate.density == UiDensity.LOW && delegate.size == size && delegate.bold == bold,
                    "Native data role uses the actual Regular/Bold body family and shared sizes");
            Object line = originals.get(row);
            java.lang.reflect.Method render = line.getClass().getDeclaredMethod("renderLine", com.wurmonline.client.renderer.backend.Queue.class,
                    com.wurmonline.client.renderer.gui.text.TextFont.class, int.class, int.class, boolean.class);
            render.setAccessible(true); render.invoke(line, null, font, 12, 35 + row * 37, false); row++;
        }
        HudButtonProbe.finishPreview(); ImageIO.write(image, "png", new File(output.getParentFile(), "hud-data-fonts-preview.png"));
    }
    private static void resizeThroughHud(HeadsUpDisplay hud, HighResNativeWindow skin) {
        WWindow window = skin.window;
        int width = window.width, height = window.height;
        int px = window.x + width - 12, py = window.y + height - 12;
        WurmComponent handle = hud.getComponentAt(px, py);
        check(handle == HighResWindowAccess.get(window, "southBar"), "Hover finds the painted resize handle: " + skin.id);
        check(hud.mousePressed(px, py, 0, 0), "HUD accepts resize press: " + skin.id);
        check(HighResWindowAccess.get(hud, "mouseFocusComponent") == handle, "Real press captures resize instead of body/editor: " + skin.id);
        hud.mouseDragged(px + 37, py + 29); hud.mouseReleased(px + 37, py + 29, 0);
        check(window.width == width + 37 && window.height == height + 29, "Native drag changes width and height: " + skin.id);
        hud.mouseDragged(px + 80, py + 80);
        check(window.width == width + 37 && window.height == height + 29, "Release ends native resizing: " + skin.id);
        window.setSize(width, height); skin.tick();
    }
    @SuppressWarnings("unchecked")
    private static void verifyResizeAndRows(HeadsUpDisplay hud, HighResChatWindow chat, HighResNativeWindow inventory,
                                          HighResNativeWindow events, List<WWindow> containers) throws Exception {
        List<HighResNativeWindow> windows = new ArrayList<>(); windows.add(chat); windows.add(inventory); windows.add(events);
        java.lang.reflect.Field field = HighResWindowIntegration.class.getDeclaredField("WINDOWS"); field.setAccessible(true);
        Map<WurmComponent, HighResNativeWindow> skins = (Map<WurmComponent, HighResNativeWindow>) field.get(null);
        for (WWindow window : containers) windows.add(skins.get(window));
        for (HighResNativeWindow skin : windows) resizeThroughHud(hud, skin);
        List<HighResChannelStrip.Channel> buttons = new ArrayList<>((List<HighResChannelStrip.Channel>) HighResWindowAccess.get(chat.channels(), "channels"));
        int width = chat.window.width, height = chat.window.height;
        for (String language : HudMessages.languageCodes()) {
            HudMessages.useLanguage(language); chat.tick();
            int previousCount = -1;
            for (int targetWidth : new int[]{1100, 540, 270, 1100}) {
                chat.window.setSize(targetWidth, height); chat.tick();
                HighResChannelStrip strip = chat.channels();
                List<HighResChannelStrip.Channel> actual = (List<HighResChannelStrip.Channel>) HighResWindowAccess.get(strip, "channels");
                check(strip.width == chat.header.width && chat.header.height == strip.rowCount() * HighResChannelStrip.HEIGHT,
                        "Width change immediately updates row geometry and header height: " + language);
                check(chat.window.getComponent().y == chat.header.y + chat.header.height, "Message body starts below the last tab row");
                int lastRow = strip.rowCount(), baseline = -1;
                for (HighResChannelStrip.Channel button : actual) {
                    int row = (button.y - strip.y) / HighResChannelStrip.HEIGHT;
                    check(row <= lastRow && button.x >= strip.x && button.x + button.width <= strip.x + strip.width,
                            "Every translated tab fits its row in native order: " + language + " " + targetWidth + " " + button.source);
                    check(button.captionLayout.fontPixels == 14 && button.height == 21, "Wrapping preserves Compact caption size");
                    if (baseline == -1) baseline = button.captionLayout.baseline(0);
                    check(button.captionLayout.baseline(0) == baseline, "All wrapped rows share one caption baseline");
                    lastRow = row;
                    int px = button.x + button.width / 2, py = button.y + 10;
                    check(hud.getComponentAt(px, py) == button && hud.mousePressed(px, py, 0, 0), "Lower-row tab is reachable through native HUD input");
                    hud.mouseReleased(px, py, 0);
                    check(((WurmTabbedWindow) chat.window).getSelectedTab() == button.nativeButton.tab, "Completed lower-row click selects the native channel");
                }
                check(lastRow == 0 && actual.get(0).y == strip.y + (strip.rowCount()-1)*HighResChannelStrip.HEIGHT,
                        "Additional rows grow upward and the first Local row remains next to the messages");
                if (previousCount >= 0 && targetWidth != 1100) check(strip.rowCount() >= previousCount, "Narrowing moves tabs into later rows");
                if (targetWidth == 270) check(strip.rowCount() >= 3, "Narrow windows use a third row and more");
                if (targetWidth == 1100) check(strip.rowCount() <= 2, "Widening moves tabs back into earlier rows");
                previousCount = strip.rowCount();
            }
        }
        HudMessages.useLanguage("en"); chat.window.setSize(270, height); chat.tick();
        int originalRows = chat.channels().rowCount();
        int originalY=chat.window.y;
        chat.window.setPosition(chat.window.x,350);
        int messageY=chat.window.getComponent().y;
        ChatPanelComponent manager = (ChatPanelComponent) HighResWindowAccess.get(chat.window, "activeManager");
        List<AbstractTab> opened = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            AbstractTab tab = manager.getTab("Additional channel " + i, true); tab.open(); opened.add(tab); chat.tick();
        }
        List<HighResChannelStrip.Channel> expanded = (List<HighResChannelStrip.Channel>) HighResWindowAccess.get(chat.channels(), "channels");
        check(expanded.size() == buttons.size() + opened.size() && chat.channels().rowCount() > originalRows,
                "New channels expand the wrapped strip into additional rows");
        check(chat.window.getComponent().y==messageY,"New rows extend the top upward without moving the message area");
        check(chat.window.height >= chat.header.height + 80 && chat.window.getComponent().y == chat.header.y + chat.header.height,
                "Additional rows retain message/editor room below the complete header");
        for (AbstractTab tab : opened) tab.close(); chat.tick();
        chat.window.setPosition(chat.window.x,originalY);
        check(chat.channels().rowCount() == originalRows, "Closing channels removes unnecessary rows");
        // Exercise immediate reflow during native dragging, before any render/frame tick.
        chat.window.setSize(540, chat.window.height); chat.tick();
        // Added rows can overlap another fixture window; native active-window
        // notifications are omitted, so explicitly bring this window to the front.
        hud.getComponents().remove(chat.window); hud.getComponents().add(chat.window);
        int px = chat.window.x + chat.window.width - 12, py = chat.window.y + chat.window.height - 12;
        check(hud.mousePressed(px, py, 0, 0), "Native drag starts for a tabbed window");
        check(HighResWindowAccess.get(hud, "mouseFocusComponent") == HighResWindowAccess.get(chat.window, "southBar"),
                "Resize press captures the intended tabbed window");
        hud.mouseDragged(px - 270, py);
        check(chat.channels().rowCount() == originalRows && chat.window.getComponent().y == chat.header.y + chat.header.height,
                "Resize drag immediately moves tabs into the rows below");
        hud.mouseReleased(px - 270, py, 0);
        chat.window.setSize(width, height); chat.tick();
        List<HighResChannelStrip.Channel> actual = (List<HighResChannelStrip.Channel>) HighResWindowAccess.get(chat.channels(), "channels");
        for (int i = 0; i < buttons.size(); i++) check(buttons.get(i).nativeButton == actual.get(i).nativeButton, "Reflow preserves native channel identities");
        actual.get(0).nativeButton.tab.select(); chat.tick();
    }
    @SuppressWarnings("unchecked")
    private static void verifySkills(HeadsUpDisplay hud, ChatPanelComponent manager, HighResChatWindow events, File output) throws Exception {
        java.nio.file.Path preference = output.toPath().resolveSibling("skills-probe.properties");
        java.nio.file.Files.deleteIfExists(preference); SkillDetailSettings.load(preference);
        try {
            events.tick();
            List<HighResChannelStrip.Channel> channels = (List<HighResChannelStrip.Channel>) HighResWindowAccess.get(events.channels(), "channels");
            HighResChannelStrip.Channel skills = null;
            for (HighResChannelStrip.Channel channel : channels) if (channel.source.equals(":Skills")) skills = channel;
            check(skills != null, "Skills native channel is present");
            skills.rightPressed(skills.x + skills.width / 2, skills.y + 10, 0);
            List<WurmPopup> popups = (List<WurmPopup>) HighResWindowAccess.get(hud, "popups");
            WurmPopup popup = popups.get(popups.size() - 1);
            HighResSkillDetailDialog.MenuItem action = null;
            for (FlexComponent child : popup.getComponents()) if (child instanceof HighResSkillDetailDialog.MenuItem) action = (HighResSkillDetailDialog.MenuItem) child;
            check(action != null && popup.getComponents().size() > 2, "Skills menu retains native commands and adds detail action");
            check(action.captionLayout.preset == UiButtonPreset.COMPACT && action.captionLayout.fontPixels == 14 && action.height == 21,
                    "Skills context action uses shared Compact caption metrics");
            int px = action.x + action.width / 2, py = action.y + 10;
            action.leftPressed(px, py, 0); action.mouseDragged(px, py - 50); action.mouseDragged(px, py); release(hud, action, px, py);
            check(staticField(HighResSkillDetailDialog.class, "dialog") == null, "Dragging out cancels detail action");
            action.leftPressed(px, py, 0); release(hud, action, px, py);
            WWindow dialog = (WWindow) staticField(HighResSkillDetailDialog.class, "dialog");
            check(dialog != null && hud.getComponents().contains(dialog) && !popups.contains(popup), "Completed action replaces native menu with detail dialog");
            Object body = staticField(HighResSkillDetailDialog.class, "body");
            ChamomiloUiV1DropDown choice = (ChamomiloUiV1DropDown) HighResWindowAccess.get(body, "choice");
            ChamomiloUiV1CompactButton apply = (ChamomiloUiV1CompactButton) HighResWindowAccess.get(body, "apply");
            ChamomiloUiV1CompactButton cancel = (ChamomiloUiV1CompactButton) HighResWindowAccess.get(body, "cancel");
            check(choice.height == 24 && choice.selectedIndex() == 0 && apply.preset() == UiButtonPreset.COMPACT, "Detail choices start at All with Compact actions");
            check(((HudButtonProbe.ProbeFont) HighResWindowTypography.heading()).bold
                    && ((HudButtonProbe.ProbeFont) HighResWindowTypography.heading()).density == UiDensity.HIGH
                    && ((HudButtonProbe.ProbeFont) HighResWindowTypography.label()).density == UiDensity.LOW,
                    "Window heading and settings caption roles use the guideline font families");
            for (String language : HudMessages.languageCodes()) {
                HudMessages.useLanguage(language); HighResSkillDetailDialog.open(hud);
                check(choice.selectedText().equals(HudMessages.tr("All")) && ((UiCompactLayout) HighResWindowAccess.get(apply, "layout")).fontPixels == 14
                        && ((UiCompactLayout) HighResWindowAccess.get(cancel, "layout")).fontPixels == 14,
                        "Detail locale keeps All and shared 14 px actions: " + language);
            }
            HudMessages.useLanguage("ru"); HighResSkillDetailDialog.open(hud);
            dialog.setLocation(20, 30, 390, 142);
            verifyDialogAlpha(dialog, choice, hud, output);
            choice.selectIndex(3); click(hud, cancel);
            check(!hud.getComponents().contains(dialog) && SkillDetailSettings.selectedIndex() == 0, "Cancel discards uncommitted detail choice");
            HighResSkillDetailDialog.open(hud); check(choice.selectedIndex() == 0, "Reopen restores saved detail level");
            choice.selectIndex(3); click(hud, apply);
            check(SkillDetailSettings.selectedIndex() == 3 && minimumTicksOption == 5 && !hud.getComponents().contains(dialog),
                    "Apply persists detail threshold and enables all upstream native ticks");
            WurmTextPanel history = (WurmTextPanel) HighResWindowAccess.get(manager.getTab(":Skills"), "textList");
            List<?> rows = (List<?>) HighResWindowAccess.get(history, "orgLines"); int before = rows.size();
            manager.addText(":Skills", "Mind increased by 0,000023 to 70,584198", 1, 1, 1, false);
            check(rows.size() == before, "Native addText hook suppresses subthreshold Skills tick");
            manager.addText(":Skills", Arrays.asList(new com.wurmonline.shared.util.MulticolorLineSegment("Mind increased by ", (byte) 0),
                    new com.wurmonline.shared.util.MulticolorLineSegment("0,000023 to 70,584198", (byte) 0)), false);
            check(rows.size() == before, "Multicolor native addText hook uses the same Skills threshold");
            manager.addText(":Skills", "Mind increased by 0,01 to 70,594198", 1, 1, 1, false);
            manager.addText(":Skills", "You gain an affinity in Mind.", 1, 1, 1, false);
            check(rows.size() == before + 2, "Boundary tick and affinity remain in native history");
            HighResSkillDetailDialog.open(hud); choice.selectIndex(7); click(hud, apply);
            before = rows.size();
            manager.addText(":Skills", "Mind increased by 0,000001 to 70,594199", 1, 1, 1, false);
            check(rows.size() == before + 1, "Finest native six-decimal tick remains visible");
            HighResSkillDetailDialog.open(hud); choice.selectIndex(0); click(hud, apply);
            before = rows.size(); manager.addText(":Skills", "Mind increased by 0,000000 to 70,594199", 1, 1, 1, false);
            check(rows.size() == before + 1, "All restores rounded-zero tick messages");
            SkillDetailSettings.load(preference); check(SkillDetailSettings.selectedIndex() == 0 && SkillDetailSettings.configured(), "Native dialog choice persists across reload");
        } finally {
            java.nio.file.Files.deleteIfExists(preference); SkillDetailSettings.load(preference); HudMessages.useLanguage("en");
        }
    }
    @SuppressWarnings("unchecked")
    private static void verifyHoverHints(HeadsUpDisplay hud, WurmTreeList<?> inventory, WurmTreeList<?> friends,
                                         Map<WurmComponent, HighResNativeWindow> skins, HighResChannelStrip strip) {
        Map<WurmComponent, WurmTreeList<?>> headings = (Map<WurmComponent, WurmTreeList<?>>) staticFieldUnchecked(HighResWindowIntegration.class, "HEADINGS");
        List<WurmTreeList<?>> tables = new ArrayList<>(); tables.add(inventory); tables.add(friends);
        for (HighResNativeWindow skin : skins.values()) if (skin.itemTable() != null && !tables.contains(skin.itemTable())) tables.add(skin.itemTable());
        for (String language : HudMessages.languageCodes()) {
            HudMessages.useLanguage(language);
            // No beginFrame/render: the native pick must resolve the new language itself.
            for (Map.Entry<WurmComponent, WurmTreeList<?>> entry : headings.entrySet()) {
                if (!(entry.getKey() instanceof WButton) || !tables.contains(entry.getValue())) continue;
                WButton button = (WButton) entry.getKey(); String caption = button.getLabel();
                int width = button.width; int[] columns = entry.getValue().columnWidths.clone();
                String expected;
                switch (caption) {
                    case "QL": expected = "Item quality"; break;
                    case "DMG": expected = "Item damage"; break;
                    case "Weight": expected = "Item weight"; break;
                    case "Volume": expected = "Item volume"; break;
                    case "Price": expected = "Item price"; break;
                    case "i": expected = "Item material type"; break;
                    case "Name": expected = entry.getValue() == friends ? "Character name" : "Item name"; break;
                    case "Status": expected = "Player online status"; break;
                    default: throw new AssertionError("Unreviewed native heading: " + caption);
                }
                com.wurmonline.client.renderer.PickData pick = new com.wurmonline.client.renderer.PickData(hud);
                check(org.highreshud.client.HighResHudRuntime.pickComponentHint(button, pick, button.x + 3, button.y + 10)
                        && pick.getText().equals(Collections.singletonList(HudMessages.tr(expected))), "Native heading full hover: " + caption + "/" + language + " table=" + entry.getValue().name + " expected=" + HudMessages.tr(expected) + " actual=" + pick.getText());
                check(button.getLabel().equals(caption) && button.width == width && Arrays.equals(columns, entry.getValue().columnWidths), "Hover leaves native labels and columns intact");
                if (entry.getValue() == inventory && caption.equals("QL")) {
                    pick.reset();
                    check(hud.pick(button.x + 3, button.y + 10, pick) && pick.getText().contains(HudMessages.tr("Item quality")), "Real HUD.pick shows expanded quality hint: " + language);
                }
            }
            for (HighResChannelStrip.Channel channel : (List<HighResChannelStrip.Channel>) HighResWindowAccess.get(strip, "channels")) {
                int px = channel.x + channel.width / 2, py = channel.y + 10;
                if (!channel.ownsPointer(px, py)) continue;
                com.wurmonline.client.renderer.PickData pick = new com.wurmonline.client.renderer.PickData(hud);
                channel.pick(pick, px, py);
                check(pick.getText().size() == 1 && !pick.getText().get(0).isEmpty(), "All channel buttons retain full hints after immediate language change");
                if (channel.source.equals("CA HELP")) check(pick.getText().get(0).equals(HudMessages.tr("Community assistant help chat")), "CA abbreviation is explained");
                if (channel.source.equals("Long custom server channel")) check(pick.getText().get(0).equals(channel.source), "Custom channel name is preserved");
            }
        }
        HudMessages.useLanguage("en"); HighResWindowIntegration.beginFrame(hud);
    }
    private static Object staticFieldUnchecked(Class<?> type, String name) {
        try { return staticField(type, name); } catch (Exception failure) { throw new AssertionError(failure); }
    }
    private static Object staticField(Class<?> type, String name) throws Exception {
        java.lang.reflect.Field field = type.getDeclaredField(name); field.setAccessible(true); return field.get(null);
    }
    private static void click(HeadsUpDisplay hud, WButton button) {
        int x = button.x + button.width / 2, y = button.y + button.height / 2;
        button.leftPressed(x, y, 0); release(hud, button, x, y);
    }
    @SuppressWarnings("unchecked")
    private static void verifyDialogAlpha(WWindow dialog, ChamomiloUiV1DropDown choice, HeadsUpDisplay hud, File output) throws Exception {
        BufferedImage previous = null;
        for (float alpha : new float[]{0, .2f, .65f, 1}) {
            BufferedImage image = new BufferedImage(430, 200, BufferedImage.TYPE_INT_ARGB); HudButtonProbe.previewGraphics(image);
            choice.render(null, alpha); HudButtonProbe.finishPreview();
            if (previous != null) check(Arrays.equals(previous.getRGB(0, 0, 430, 200, null, 0, 430), image.getRGB(0, 0, 430, 200, null, 0, 430)), "Closed detail dropdown ignores HUD alpha");
            previous = image;
        }
        BufferedImage dialogImage = new BufferedImage(430, 200, BufferedImage.TYPE_INT_ARGB); HudButtonProbe.previewGraphics(dialogImage);
        HighResWindowIntegration.renderWindow(dialog, null); HudButtonProbe.finishPreview();
        ImageIO.write(dialogImage, "png", new File(output.getParentFile(), "hud-skills-dialog-preview.png"));
        choice.leftPressed(choice.x + 20, choice.y + 12, 0);
        List<WurmDropdownPopup> popups = (List<WurmDropdownPopup>) HighResWindowAccess.get(hud, "dropdownPopups");
        WurmDropdownPopup popup = popups.get(popups.size() - 1);
        previous = null;
        for (float alpha : new float[]{0, .2f, .65f, 1}) {
            BufferedImage image = new BufferedImage(430, 400, BufferedImage.TYPE_INT_ARGB); HudButtonProbe.previewGraphics(image);
            popup.render(null, alpha); HudButtonProbe.finishPreview();
            if (previous != null) check(Arrays.equals(previous.getRGB(0, 0, 430, 400, null, 0, 430), image.getRGB(0, 0, 430, 400, null, 0, 430)), "Detached detail options ignore HUD alpha");
            previous = image;
        }
        ImageIO.write(previous, "png", new File(output.getParentFile(), "hud-skills-options-preview.png"));
        int px = popup.x + 14, py = popup.y + 3 + 3 * 24 + 12;
        FlexComponent option = popup.getComponentAt(px, py);
        check(option != null, "Detached dropdown options receive native hit testing");
        option.leftPressed(px, py, 0); release(hud, option, px, py);
        check(choice.selectedIndex() == 3 && popups.isEmpty(), "Completed native option selection changes threshold and dismisses popup");
        choice.selectIndex(0);
    }
    private static void sampleText(String caption, int x, int y, int pixels) {
        HudButtonProbe.ProbeFont font = new HudButtonProbe.ProbeFont(pixels, false, UiDensity.LOW);
        font.moveTo(x, y); font.paint(null, caption, UiColor.TEXT.red, UiColor.TEXT.green, UiColor.TEXT.blue, 1);
    }
    private static void assertHeadings(WurmTreeList<?> table) {
        WurmComponent row = (WurmComponent) HighResWindowAccess.get(table, "buttonRow");
        WurmComponent band = row.parent;
        int height=((Map<?,?>)staticFieldUnchecked(HighResWindowIntegration.class,"FILTERS")).containsKey(table)?28:21;
        check(band != null && band.parent != null && band.height == height && band.parent.height == height && band.parent.getClass().getSimpleName().equals("HeadingBand"),
                "Table headings reserve the minimal visible row: " + table.name + " row=" + row.name + " band="
                        + (band == null ? "null" : band.name + "," + band.height + "," + (band.parent == null ? "null" : band.parent.getClass().getSimpleName() + "," + band.parent.name + "," + band.parent.height)));
        check(table.getLastSortOn() == -2 && ((Number) HighResWindowAccess.get(table, "sortOrder")).intValue() == 1,
                "Table opens sorted by name ascending: " + table.name);
    }
    private static void assertNoHorizontal(FlexComponent component) {
        if (component instanceof WurmScrollPanel) {
            check(((FlexComponent[]) HighResWindowAccess.get(component, "components"))[WurmBorderPanel.SOUTH] == null,
                    "Chat/Friends has no horizontal scrollbar: " + component.name);
            assertNoHorizontal(((WurmScrollPanel) component).content);
        } else if (component instanceof WurmBorderPanel) {
            for (FlexComponent child : (FlexComponent[]) HighResWindowAccess.get(component, "components")) if (child != null) assertNoHorizontal(child);
        } else if (component instanceof WurmDecorator) assertNoHorizontal(((WurmDecorator) component).component);
        else if (component instanceof WurmArrayPanel)
            for (Object child : ((WurmArrayPanel<?>) component).components) assertNoHorizontal((FlexComponent) child);
    }
}
