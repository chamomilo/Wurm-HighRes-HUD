package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.TextFont;
import com.wurmonline.client.game.inventory.InventoryMetaWindowView;
import org.chamomilo.wurm.ui.v1.*;
import org.highreshud.client.HighResWindowsSettings;
import org.highreshud.client.i18n.HudMessages;
import org.highreshud.client.i18n.HudHoverText;
import com.wurmonline.client.renderer.PickData;
import java.util.*;

/** Called only after native HUD initialization; the HUD owns all original window registrations. */
public final class HighResWindowIntegration {
    static final int HEADING_HEIGHT = 21;
    private static HeadsUpDisplay currentHud;
    private static final Map<WurmComponent, HighResNativeWindow> WINDOWS = new IdentityHashMap<>();
    private static final Map<WurmScrollPanel, ScrollSkin> SCROLLS = new IdentityHashMap<>();
    private static final Set<WurmTreeList<?>> TABLES = Collections.newSetFromMap(new IdentityHashMap<WurmTreeList<?>, Boolean>());
    private static final Map<WurmComponent, WurmTreeList<?>> HEADINGS = new IdentityHashMap<>();
    private static final Map<WButton, String> HEADING_HINTS = new IdentityHashMap<>();
    private static final Set<WurmComponent> BANDS = Collections.newSetFromMap(new IdentityHashMap<WurmComponent, Boolean>());
    private static final HighResInventoryDrag INVENTORY_DRAG = new HighResInventoryDrag();
    private static final Map<WurmTreeList<?>, HighResInventoryFilter> FILTERS = new IdentityHashMap<>();
    private HighResWindowIntegration() { }

    /** HUD-thread font refresh reuses native rows, selection and scroll anchors. */
    public static void refreshTypography() {
        for (HighResNativeWindow skin : new ArrayList<HighResNativeWindow>(WINDOWS.values())) skin.tick();
    }

    public static void hudReady(HeadsUpDisplay hud) {
        if (hud == null || currentHud == hud) return;
        INVENTORY_DRAG.reset();
        WINDOWS.clear(); SCROLLS.clear(); TABLES.clear(); HEADINGS.clear(); HEADING_HINTS.clear(); BANDS.clear(); FILTERS.clear(); currentHud = hud;
        if (HighResWindowsSettings.inventory) attach(new HighResInventoryWindow(
                (InventoryWindow) HighResWindowAccess.get(hud, "inventoryWindow"), hud));
        if (HighResWindowsSettings.events) attach(new HighResEventsWindow(
                (WurmTabbedWindow) HighResWindowAccess.get(hud, "eventWindow"), hud));
        if (HighResWindowsSettings.chat) attach(new HighResChatWindow(
                (WurmTabbedWindow) HighResWindowAccess.get(hud, "chatWindow"), hud));
        if (HighResWindowsSettings.character && hud.getPaperDollInventory() != null)
            attach(new HighResCharacterWindow(hud.getPaperDollInventory(), hud));
        WurmTabbedWindow friends = (WurmTabbedWindow) HighResWindowAccess.get(hud, "friendWindow");
        if (HighResWindowsSettings.events && friends != null) attach(new HighResChatWindow("highres.friends", "Friends", friends, hud));
        HighResSkillDetailDialog.restoreNativeMessages();
        attachContainers(hud);
        // The same instances retain native show/hide commands and saved-position keys.
        ensureHudRegistration(hud, "Event window", (WurmComponent) HighResWindowAccess.get(hud, "eventWindow"));
        ensureHudRegistration(hud, "Chat window", (WurmComponent) HighResWindowAccess.get(hud, "chatWindow"));
    }
    private static void ensureHudRegistration(HeadsUpDisplay hud, String name, WurmComponent window) {
        HudSettings settings = (HudSettings) HighResWindowAccess.get(hud, "hudSettings");
        for (Map.Entry<WButton, WurmComponent> entry : settings.getMenuSet()) if (entry.getValue() == window) return;
        settings.registerComponent(name, window);
    }
    private static void attach(HighResNativeWindow skin) {
        WINDOWS.put(skin.window, skin); skin.install();
        if(skin.itemTable()!=null)resized(skin.window);
    }
    static void attachDialog(WWindow dialog, HeadsUpDisplay hud) {
        attach(new HighResNativeWindow("highres.skills.detail", "Skill detail level", dialog, hud) { });
    }
    @SuppressWarnings("unchecked")
    private static Map<InventoryMetaWindowView, ItemListWindow> containers(HeadsUpDisplay hud) {
        Map<InventoryMetaWindowView, ItemListWindow> windows = (Map<InventoryMetaWindowView, ItemListWindow>) HighResWindowAccess.get(hud, "inventoryWindows");
        return windows == null ? Collections.<InventoryMetaWindowView, ItemListWindow>emptyMap() : windows;
    }
    @SuppressWarnings("unchecked")
    private static Map<Long, InventoryContainerWindow> inventoryContainers(HeadsUpDisplay hud) {
        Map<Long, InventoryContainerWindow> windows = (Map<Long, InventoryContainerWindow>) HighResWindowAccess.get(hud, "inventoryContainerWindows");
        return windows == null ? Collections.<Long, InventoryContainerWindow>emptyMap() : windows;
    }
    private static void attachContainers(HeadsUpDisplay hud) {
        if (!HighResWindowsSettings.inventory) return;
        for (ItemListWindow window : containers(hud).values()) attachContainer(window, hud);
        for (InventoryContainerWindow window : inventoryContainers(hud).values()) attachContainer(window, hud);
    }
    private static void attachContainer(WWindow window, HeadsUpDisplay hud) {
        if (window != null && !WINDOWS.containsKey(window)) attach(new HighResContainerWindow(window, hud));
    }
    public static void containerOpened(HeadsUpDisplay hud, InventoryMetaWindowView view) {
        if (hud == currentHud && HighResWindowsSettings.inventory) attachContainer(containers(hud).get(view), hud);
    }
    public static void containerClosed(HeadsUpDisplay hud, InventoryMetaWindowView view) {
        if (hud == currentHud) detach(containers(hud).get(view));
    }
    public static void inventoryContainerOpened(HeadsUpDisplay hud, long id) {
        if (hud == currentHud && HighResWindowsSettings.inventory) attachContainer(inventoryContainers(hud).get(id), hud);
    }
    public static void windowRemoved(HeadsUpDisplay hud, WurmComponent window) {
        if (hud == currentHud) detach(window);
    }
    public static void reconnected(HeadsUpDisplay hud) {
        if (hud != currentHud) return;
        for (WurmComponent window : new ArrayList<>(WINDOWS.keySet()))
            if (window instanceof ItemListWindow && !containers(hud).containsValue(window)
                    || window instanceof InventoryContainerWindow && !inventoryContainers(hud).containsValue(window)) detach(window);
    }
    private static boolean descendant(WurmComponent component, WurmComponent window) {
        for (WurmComponent cursor = component; cursor != null; cursor = cursor.parent) if (cursor == window) return true;
        return false;
    }
    private static void detach(WurmComponent window) {
        if (window == null || !WINDOWS.containsKey(window)) return;
        INVENTORY_DRAG.removed((WWindow)window);
        SCROLLS.keySet().removeIf(component -> descendant(component, window));
        FILTERS.entrySet().removeIf(entry -> descendant(entry.getKey(), window));
        TABLES.removeIf(component -> descendant(component, window));
        HEADING_HINTS.keySet().removeIf(component -> descendant(component, window) || descendant(HEADINGS.get(component), window));
        HEADINGS.entrySet().removeIf(entry -> descendant(entry.getKey(), window) || descendant(entry.getValue(), window));
        BANDS.removeIf(component -> descendant(component, window));
        WINDOWS.remove(window);
    }
    public static void beginFrame(HeadsUpDisplay hud) {
        if (hud != currentHud) return;
        // Also catch windows created/reparented by another mod after the native open hook.
        attachContainers(hud);
        for (HighResNativeWindow skin : WINDOWS.values()) skin.tick();
        INVENTORY_DRAG.arrivals(WINDOWS.values());
        for (HighResInventoryFilter filter : FILTERS.values()) filter.refresh();
        for (WButton button : HEADING_HINTS.keySet()) refreshHeadingHint(button);
        for (ScrollSkin scroll : SCROLLS.values()) scroll.synchronize();
    }
    public static boolean owns(WurmComponent component) {
        for (WurmComponent cursor = component; cursor != null; cursor = cursor.parent)
            if (WINDOWS.containsKey(cursor)) return true;
        return false;
    }
    public static boolean renderWindow(WurmComponent component, Queue queue) {
        HighResNativeWindow skin = WINDOWS.get(component);
        if (skin == null) return false;
        // Use native final render for its push/pop scissor, with our own virtual
        // renderComponent rather than rewriting the already-loaded window class.
        skin.drawing.setLocation(component.x, component.y, component.width, component.height);
        skin.drawing.render(queue, 1.0f); return true;
    }
    public static boolean minimized(WurmComponent component) {
        HighResNativeWindow skin = WINDOWS.get(component); return skin != null && skin.window.minimized;
    }
    public static boolean hiddenChatManager(Object manager) {
        if (!(manager instanceof ChatPanelComponent)) return false;
        WurmTabbedWindow window = ((ChatPanelComponent) manager).parent;
        HighResNativeWindow skin = WINDOWS.get(window);
        return skin != null && !skin.owner.getComponents().contains(window);
    }
    public static FlexComponent minimizedComponent(WurmComponent component, int x, int y) {
        HighResNativeWindow skin = WINDOWS.get(component);
        return skin == null ? null : skin.header.getComponentAt(x, y);
    }
    public static WurmComponent windowComponentAt(WurmComponent component, int x, int y) {
        HighResNativeWindow skin = WINDOWS.get(component);
        if (skin == null || !component.contains(x, y)) return null;
        if (skin.window.minimized) return skin.header.getComponentAt(x, y);
        if (skin instanceof HighResCharacterWindow) return ((HighResCharacterWindow) skin).componentAt(x, y);
        HighResInventoryFilter filter=FILTERS.get(skin.itemTable());
        if(filter!=null) {FlexComponent input=filter.at(x,y);if(input!=null)return input;}
        if (skin.window.resizable && x >= component.x + component.width - 19 && y >= component.y + component.height - 19)
            return (WurmComponent) HighResWindowAccess.get(component, "southBar");
        FlexComponent body = HighResInventoryDrag.bodyAt(skin, x, y);
        if (body != null) return body;
        return null;
    }
    static Collection<HighResNativeWindow> skins() {return WINDOWS.values();}
    static void paintInventoryDrag(HighResNativeWindow skin,Queue queue) {INVENTORY_DRAG.paint(skin,queue);}
    static void paintInventoryOpacity(HighResNativeWindow skin,Queue queue) {
        HighResInventoryFilter filter=FILTERS.get(skin.itemTable());
        if(filter!=null)filter.paintOpacity(queue);
    }
    public static void inventoryDragFrame(HeadsUpDisplay hud,int x,int y) {if(hud==currentHud)INVENTORY_DRAG.update(hud,WINDOWS.values(),x,y);}
    public static boolean inventoryDrop(HeadsUpDisplay hud,int x,int y,int button) {return hud==currentHud&&INVENTORY_DRAG.drop(hud,x,y,button);}
    public static void inventoryDragCleared() {INVENTORY_DRAG.clear();}
    static boolean inventoryGap(WurmTreeList<?> table) {return INVENTORY_DRAG.hasGap(table);}
    static void refreshInventoryFilter(WurmTreeList<?> table) {
        HighResInventoryFilter filter=FILTERS.get(table);if(filter!=null)filter.refresh();
    }
    public static void windowHidden(HeadsUpDisplay hud,WurmComponent component) {
        if(hud!=currentHud)return;
        INVENTORY_DRAG.clear();
        HighResNativeWindow skin=WINDOWS.get(component);
        HighResInventoryFilter filter=skin==null?null:FILTERS.get(skin.itemTable());
        if(filter!=null)filter.reset();
    }
    public static boolean characterOwned(Object window) { return WINDOWS.get(window) instanceof HighResCharacterWindow; }
    public static int characterPosition(Object window, byte id, boolean vertical, boolean wound) {
        return ((HighResCharacterWindow) WINDOWS.get(window)).position(id, vertical, wound);
    }
    public static byte characterHit(Object window, int x, int y, boolean wound) {
        return ((HighResCharacterWindow) WINDOWS.get(window)).hit(x,y,wound);
    }
    public static boolean characterPlayerHit(Object window,int x,int y) { return ((HighResCharacterWindow) WINDOWS.get(window)).playerHit(x,y); }
    public static PaperDollItem characterWoundAt(Object window,int x,int y) { return ((HighResCharacterWindow) WINDOWS.get(window)).woundAt(x,y); }
    public static void characterPressed(Object window,int x,int y) {
        if(!characterOwned(window))return;
        HighResWindowAccess.set(window,"leftClickedAtPlayerButton",false);
        ((HighResCharacterWindow)WINDOWS.get(window)).modelPressed(x,y);
    }
    public static void characterReleased(Object window,int x,int y) {
        if(characterOwned(window))((HighResCharacterWindow)WINDOWS.get(window)).modelReleased(x,y);
    }
    public static void characterLayout(Object window) { if(characterOwned(window)) WINDOWS.get(window).tick(); }
    public static boolean characterPick(Object window,PickData pick,int x,int y) { return characterOwned(window)&&((HighResCharacterWindow) WINDOWS.get(window)).pick(pick,x,y); }
    public static void collapsedChanged(WurmComponent component) {
        HighResNativeWindow skin = WINDOWS.get(component); if (skin != null) skin.collapsedChanged();
    }
    public static int minimumWidth(WurmComponent component, int proposed) {
        HighResNativeWindow skin = WINDOWS.get(component);
        if(skin instanceof HighResCharacterWindow)return Math.max(HighResCharacterWindow.WIDTH,proposed);
        int nameWidth=0;
        if(skin!=null && skin.itemTable()!=null) {
            for(String language:HudMessages.languageCodes())
                nameWidth=Math.max(nameWidth,HighResWindowTypography.heading().getWidth(HudMessages.translate(language,"Name")));
            TextFont editor=com.wurmonline.client.renderer.gui.text.HudTextFonts.body(14,false);
            int filterWidth=Math.max(editor.getWidth("WWWWW"),editor.getWidth("ШШШШШ"))+12;
            nameWidth+=filterWidth+HighResInventoryOpacity.widthFor(filterWidth)+6+24+32+HighResInventoryFilter.captionGap();
        }
        int minimum = skin != null && skin.itemTable() != null
                ? Math.max(240, skin.itemTable().commonWidth + nameWidth
                    + (skin.itemTable().hasImpColumn ? 16 : 0)) : 260;
        return skin == null ? proposed : Math.max(minimum, proposed);
    }
    public static int minimumHeight(WurmComponent component, int proposed) {
        HighResNativeWindow skin = WINDOWS.get(component);
        if(skin instanceof HighResCharacterWindow && !skin.window.minimized)return Math.max(HighResCharacterWindow.HEIGHT,proposed);
        return skin == null ? proposed : skin.window.minimized ? HighResNativeWindow.TITLE_HEIGHT + HighResNativeWindow.FRAME
                : Math.max(skin.hasTitle() ? 140 : Math.max(140, skin.header.height + 80), proposed);
    }
    public static void resized(WurmComponent component) {
        for (WurmComponent cursor = component; cursor != null; cursor = cursor.parent) {
            if (!WINDOWS.containsKey(cursor)) continue;
            WINDOWS.get(cursor).tick();
            int width = minimumWidth(cursor, cursor.width), height = minimumHeight(cursor, cursor.height);
            if (width != cursor.width || height != cursor.height) cursor.setSize(width, height);
            return;
        }
    }
    static String ellipsis(String caption, TextFont font, int available) {
        if (font.getWidth(caption) <= available) return caption;
        if (font.getWidth("…") > available) return "";
        int end = caption.length();
        while (end > 0 && font.getWidth(caption.substring(0, end) + "…") > available) end = caption.offsetByCodePoints(end, -1);
        return caption.substring(0, end) + "…";
    }
    @SuppressWarnings("unchecked")
    static void skinContent(FlexComponent component) {
        if (component == null) return;
        HighResDataTypography.apply(component);
        if (component instanceof WurmTreeList) installInventoryHeadings((WurmTreeList<?>) component);
        if (component instanceof WurmScrollPanel && !(component instanceof ChamomiloUiV1ScrollPanel)) {
            WurmScrollPanel scroll = (WurmScrollPanel) component;
            if (!SCROLLS.containsKey(scroll)) SCROLLS.put(scroll, new ScrollSkin(scroll));
            skinContent(scroll.content); return;
        }
        if (component instanceof WurmBorderPanel) {
            for (FlexComponent child : (FlexComponent[]) HighResWindowAccess.get(component, "components")) skinContent(child);
        } else if (component instanceof WurmDecorator) skinContent(((WurmDecorator) component).component);
        else if (component instanceof WurmArrayPanel) {
            for (Object child : ((WurmArrayPanel<?>) component).components) skinContent((FlexComponent) child);
        }
        if (component instanceof WurmInputField) {
            ((WurmInputField) component).setBackgroundColor(.10f, .085f, .07f);
            if (!(component.parent instanceof InputFrame) && component.parent instanceof WurmBorderPanel) {
                WurmBorderPanel parent = (WurmBorderPanel) component.parent;
                FlexComponent[] children = (FlexComponent[]) HighResWindowAccess.get(parent, "components");
                for (int i = 0; i < children.length; i++) if (children[i] == component) { parent.setComponent(new InputFrame(component), i); break; }
            }
        }
        if (component instanceof WurmProgressBar && !(component.parent instanceof ProgressFrame)) {
            WurmComponent parent = component.parent;
            replaceChild(parent, component, new ProgressFrame(component));
        }
        if (component instanceof WurmLabel && !(component.parent instanceof LabelFrame)
                && (component.parent instanceof WurmBorderPanel || component.parent instanceof WurmArrayPanel)) {
            WurmComponent parent = component.parent;
            replaceChild(parent, component, new LabelFrame(component));
        }
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void replaceChild(WurmComponent parent, FlexComponent original, FlexComponent replacement) {
        if (parent instanceof WurmBorderPanel) {
            FlexComponent[] children = (FlexComponent[]) HighResWindowAccess.get(parent, "components");
            for (int i = 0; i < children.length; i++) if (children[i] == original) { ((WurmBorderPanel) parent).setComponent(replacement, i); break; }
        } else if (parent instanceof WurmArrayPanel) {
            List children = ((WurmArrayPanel<?>) parent).components;
            int index = children.indexOf(original);
            if (index >= 0) { children.set(index, replacement); replacement.parent = parent; ((WurmArrayPanel<?>) parent).layout(); }
        }
    }
    public static void inputFront(WurmComponent component, Queue queue) {
        if (owns(component)) UiPainter.frame(new ChamomiloUiV1Canvas(component).begin(queue), 3, 1.0f,
                component.x, component.y, component.width, component.height);
    }
    static void installInventoryHeadings(WurmTreeList<?> table) {
        if (!TABLES.add(table)) return;
        WurmArrayPanel<?> row = (WurmArrayPanel<?>) HighResWindowAccess.get(table, "buttonRow");
        if (row == null) { TABLES.remove(table); return; }
        WurmBorderPanel band = (WurmBorderPanel) row.parent;
        HighResNativeWindow itemSkin=null;
        for(HighResNativeWindow skin:WINDOWS.values())if(skin.itemTable()==table)itemSkin=skin;
        boolean items=itemSkin!=null;
        int height=items?28:HEADING_HEIGHT;
        WButton name=null;
        for(FlexComponent child:(FlexComponent[])HighResWindowAccess.get(band,"components"))
            if(child instanceof WButton && "Name".equals(((WButton)child).getLabel()))name=(WButton)child;
        if(items && name!=null)FILTERS.put(table,new HighResInventoryFilter(table,name,itemSkin));
        BANDS.add(band); HEADINGS.put(band, table);
        band.sizeFlags = 0; band.setSize(band.width, height); band.sizeFlags = FlexComponent.FIXED_HEIGHT;
        HEADINGS.put(row, table);
        for (FlexComponent child : (FlexComponent[]) HighResWindowAccess.get(band, "components"))
            if (child instanceof WButton) prepareHeading((WButton) child, table);
        for (Object child : row.components) {
            WButton button = (WButton) child;
            int index = ((Number) HighResWindowAccess.get(button, "index")).intValue();
            if (index >= 0) button.setSize(table.columnWidths[index], button.height);
            prepareHeading(button, table);
        }
        // The optional improvement column may be inserted by the native menu later.
        prepareHeading((WButton) HighResWindowAccess.get(table, "impButton"), table);
        WurmBorderPanel parent = (WurmBorderPanel) band.parent;
        if (parent == null) parent = (WurmBorderPanel) ((FlexComponent[]) HighResWindowAccess.get(table, "components"))[WurmBorderPanel.CENTER];
        FlexComponent[] children = (FlexComponent[]) HighResWindowAccess.get(parent, "components");
        boolean attached = false;
        for (int i = 0; i < children.length; i++) if (children[i] == band) { parent.setComponent(new HeadingBand(band), i); attached = true; break; }
        if (!attached) parent.setComponent(new HeadingBand(band), WurmBorderPanel.NORTH);
        row.layout(); band.layout(); table.layout();
        sortByName(table);
        for (WurmComponent cursor = table; cursor != null; cursor = cursor.parent)
            if (WINDOWS.containsKey(cursor)) {
                int minimum=minimumWidth(cursor,cursor.width);
                if(cursor.width<minimum)cursor.setSize(minimum,cursor.height);
                ((ContainerComponent) cursor).layout(); break;
            }
    }
    private static void prepareHeading(WButton button, WurmTreeList<?> table) {
        HEADINGS.put(button, table);
        if (!HEADING_HINTS.containsKey(button)) HEADING_HINTS.put(button, button.getHoverString());
        refreshHeadingHint(button);
        int flags = button.sizeFlags;
        button.sizeFlags = 0; button.setSize(button.width, FILTERS.containsKey(table)?28:HEADING_HEIGHT); button.sizeFlags = flags | FlexComponent.FIXED_HEIGHT;
    }
    private static void refreshHeadingHint(WButton button) {
        WurmArrayPanel<?> row = (WurmArrayPanel<?>) HighResWindowAccess.get(HEADINGS.get(button), "buttonRow");
        // Native component names can be null outside the client's debug mode.
        // Friends is the migrated table with an online-status column.
        boolean friends = false;
        for (Object child : row.components) if ("Status".equals(((WButton) child).getLabel())) { friends = true; break; }
        button.setHoverString(HudHoverText.column(button.getLabel(), friends, HEADING_HINTS.get(button)));
    }
    public static boolean pickHeading(WurmComponent component, PickData pick, int px, int py) {
        if (!(component instanceof WButton) || !HEADINGS.containsKey(component)) return false;
        WButton button = (WButton) component;
        refreshHeadingHint(button);
        button.pick(pick, px, py);
        return true;
    }
    public static boolean pickWindowChrome(WurmComponent component, PickData pick, int px, int py) {
        for(HighResInventoryFilter filter:FILTERS.values())if(descendant(component,filter.field)) {
            pick.addText(HudMessages.tr("filter area"));return true;
        }
        for(HighResInventoryFilter filter:FILTERS.values())if(descendant(component,filter.opacity)) {
            pick.addText(HudMessages.tr("Inventory window opacity: {0}%",filter.opacity.value()));return true;
        }
        for (HighResNativeWindow skin : WINDOWS.values()) {
            WWindow window = skin.window;
            if (window.resizable && !window.minimized && component == HighResWindowAccess.get(window, "southBar")
                    && window.contains(px, py) && px >= window.x + window.width - 19 && py >= window.y + window.height - 19) {
                pick.addText(HudMessages.tr("Resize window: drag this corner"));
                return true;
            }
        }
        for (ScrollSkin skin : SCROLLS.values()) {
            if (skin.vertical != null && descendant(component, skin.vertical)) {
                pick.addText(HudMessages.tr("Scroll the contents vertically")); return true;
            }
            if (skin.horizontal != null && descendant(component, skin.horizontal)) {
                pick.addText(HudMessages.tr("Scroll the contents horizontally")); return true;
            }
        }
        return false;
    }
    static void sortByName(WurmTreeList<?> table) {
        // Sort once on opening; later updates and manual moves keep their native order.
        table.setKeepAutoSorted(false);
        HighResWindowAccess.set(table, "lastSortOn", -1);
        HighResWindowAccess.set(table, "sortOrder", 1);
        table.setSortOn(-2);
    }
    static void sortContent(FlexComponent component) {
        if (component == null) return;
        if (component instanceof WurmTreeList) { sortByName((WurmTreeList<?>) component); return; }
        if (component instanceof WurmScrollPanel) sortContent(((WurmScrollPanel) component).content);
        else if (component instanceof WurmBorderPanel)
            for (FlexComponent child : (FlexComponent[]) HighResWindowAccess.get(component, "components")) sortContent(child);
        else if (component instanceof WurmDecorator) sortContent(((WurmDecorator) component).component);
        else if (component instanceof WurmArrayPanel)
            for (Object child : ((WurmArrayPanel<?>) component).components) sortContent((FlexComponent) child);
    }
    public static void windowShown(HeadsUpDisplay hud, WurmComponent component) {
        if (hud != currentHud) return;
        HighResNativeWindow skin = WINDOWS.get(component);
        if (skin != null) { if(skin.itemTable()!=null)skin.resetOpacity(); skin.tick(); sortContent(skin.window.getComponent()); }
    }
    public static void beforeSort(Object nativeTable, int column) {
        WurmTreeList<?> table = (WurmTreeList<?>) nativeTable;
        if(TABLES.contains(table))table.setKeepAutoSorted(false);
        if (TABLES.contains(table) && table.getLastSortOn() != column) HighResWindowAccess.set(table, "sortOrder", 1);
    }
    public static void beforeButtonRelease(WurmComponent component, int x, int y) {
        if (component instanceof HighResSkillDetailDialog.MenuItem && !component.contains(x, y))
            ((HighResSkillDetailDialog.MenuItem) component).armed = ((WButton) component).isCloseHovered = false;
        if (component instanceof HighResChannelStrip.Channel) {
            HighResChannelStrip.Channel channel = (HighResChannelStrip.Channel) component;
            if (!channel.ownsPointer(x, y)) channel.held = channel.armed = false;
        }
        WurmTreeList<?> table = HEADINGS.get(component);
        if (table != null && component instanceof WButton && component.contains(x, y)
                && ((WButton) component).isCloseHovered && ((WButton) component).isEnabled()) {
            int column = ((Number) HighResWindowAccess.get(component, "index")).intValue();
            if (column >= 0 || column == -2) beforeSort(table, column);
        }
    }
    public static boolean renderHeadingBand(WurmComponent component, Queue queue) {
        if (!HEADINGS.containsKey(component) || component instanceof WButton) return false;
        ChamomiloUiV1Canvas ui = new ChamomiloUiV1Canvas(component);
        ui.begin(queue);
        if (BANDS.contains(component)) UiPainter.background(ui, UiBackground.LEATHER, UiScale.BASE, 1.0f,
                component.x, component.y, component.width, component.height);
        if (component instanceof WurmArrayPanel) {
            for (Object child : ((WurmArrayPanel<?>) component).components) renderHeading((WurmComponent) child, queue);
        } else for (FlexComponent child : (FlexComponent[]) HighResWindowAccess.get(component, "components"))
            if (child != null && !renderHeading(child, queue)) renderHeadingBand(child, queue);
        if (BANDS.contains(component)) UiPainter.frame(ui, 3, 1.0f, component.x, component.y, component.width, component.height);
        return true;
    }
    private static final class HeadingBand extends WurmDecorator {
        final HighResInventoryFilter filter;
        HeadingBand(FlexComponent band) {
            super("highres.headings", band); pack(); sizeFlags = FIXED_HEIGHT;
            filter=FILTERS.get(HEADINGS.get(band));
            if(filter!=null) {filter.field.parent=this;filter.opacity.parent=this;}
        }
        @Override public FlexComponent getComponentAt(int x,int y) {
            FlexComponent input=filter==null?null:filter.at(x,y);
            return input==null?super.getComponentAt(x,y):input;
        }
        @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
            renderHeadingBand(component, queue);
            if(filter!=null)filter.paint(queue);
        }
    }
    private static final class InputFrame extends WurmDecorator {
        final ChamomiloUiV1Canvas ui = new ChamomiloUiV1Canvas(this);
        InputFrame(FlexComponent input) {
            super("highres.input", input);
            // Decorators start at zero size: reserve the row before attaching it to SOUTH.
            setSize(input.width, Math.max(24, input.height + 6)); sizeFlags = FIXED_HEIGHT;
        }
        @Override void performLayout() { component.setLocation(x + 4, y + 3, Math.max(1, width - 8), Math.max(1, height - 6)); }
        @Override public FlexComponent getComponentAt(int px, int py) { return contains(px, py) ? component : null; }
        @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
            UiPainter.background(ui.begin(queue), UiBackground.SOLID, 1.0f, x, y, width, height);
            component.render(queue, 1.0f);
            UiPainter.frame(ui, 3, 1.0f, x, y, width, height);
            if (((WurmInputField) component).hasKbFocus) ui.fill(UiColor.TEXT, 1.0f, x + 3, y + height - 3, Math.max(0, width - 6), 1);
        }
    }
    private static final class ProgressFrame extends WurmDecorator {
        final ChamomiloUiV1Canvas ui = new ChamomiloUiV1Canvas(this);
        ProgressFrame(FlexComponent bar) { super("highres.action-progress", bar); pack(); setSize(width, Math.max(18, height)); sizeFlags = FIXED_HEIGHT; }
        @Override public FlexComponent getComponentAt(int px, int py) { return contains(px, py) ? this : null; }
        @Override public void pick(PickData data, int px, int py) {
            if (!contains(px, py)) return;
            String title = (String) HighResWindowAccess.get(component, "title");
            if (title != null && !title.isEmpty()) data.addText(HudMessages.tr(title));
            float progress = ((Number) HighResWindowAccess.get(component, "progress")).floatValue();
            data.addText(HudMessages.tr("Action progress: {0}%", Math.round(Math.max(0f, Math.min(1f, progress)) * 100f)));
        }
        @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
            UiPainter.background(ui.begin(queue), UiBackground.LEATHER, UiScale.BASE, 1.0f, x, y, width, height);
            float progress = ((Number) HighResWindowAccess.get(component, "progress")).floatValue();
            UiColor color = (Boolean) HighResWindowAccess.get(component, "changeColor") ? UiColor.rgb(0x60884c) : UiColor.rgb(0x927338);
            ui.fill(color, 1.0f, x + 3, y + 3, Math.round(Math.max(0, width - 6) * progress), Math.max(0, height - 6));
            String title = HudMessages.tr((String) HighResWindowAccess.get(component, "title"));
            if (title == null) title = "";
            TextFont font = HighResWindowTypography.label();
            font.moveTo(x + 5, y + (height - font.getHeight()) / 2 + font.getAscent());
            font.paint(queue, ellipsis(title, font, Math.max(0, width - 10)), UiColor.TEXT.red, UiColor.TEXT.green, UiColor.TEXT.blue, 1.0f);
            UiPainter.frame(ui, 3, 1.0f, x, y, width, height);
        }
    }
    private static final class LabelFrame extends WurmDecorator {
        LabelFrame(FlexComponent label) { super("highres.status-label", label); pack(); setSize(width, Math.max(18, height)); sizeFlags = FIXED_HEIGHT; }
        @Override public FlexComponent getComponentAt(int px, int py) { return contains(px, py) ? this : null; }
        @Override public void pick(PickData data, int px, int py) {
            if (contains(px, py)) data.addText(HudMessages.tr((String) HighResWindowAccess.get(component, "label")));
        }
        @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
            TextFont font = HighResWindowTypography.label();
            String label = HudMessages.tr((String) HighResWindowAccess.get(component, "label"));
            font.moveTo(x + 4, y + (height - font.getHeight()) / 2 + font.getAscent());
            font.paint(queue, ellipsis(label, font, Math.max(0, width - 8)), UiColor.TEXT.red, UiColor.TEXT.green, UiColor.TEXT.blue, 1.0f);
        }
    }
    public static boolean renderHeading(WurmComponent component, Queue queue) {
        WurmTreeList<?> table = HEADINGS.get(component);
        if (table == null || !(component instanceof WButton)) return false;
        WButton button = (WButton) component;
        int index = ((Number) HighResWindowAccess.get(button, "index")).intValue();
        boolean sortable = index >= 0 || index == -2;
        boolean active = sortable && table.getLastSortOn() == index;
        UiColor color = !button.isEnabled() ? UiColor.MUTED : active || button.hovered ? UiColor.rgb(0xfff09a) : UiColor.TEXT;
        TextFont font = HighResWindowTypography.heading();
        boolean material="i".equalsIgnoreCase(button.getLabel());
        String full = material ? "m" : HudMessages.tr(button.getLabel());
        refreshHeadingHint(button);
        String caption = material ? full : ellipsis(full, font, Math.max(0, button.width - (sortable ? 19 : 8)));
        int pressed = button.isCloseHovered ? 1 : 0;
        // Reserve the compact sort slot above the trailing caption, preserving native widths.
        // Both slot and text baseline stay fixed in every sort state.
        int tx = material ? button.x+(button.width-font.getWidth(caption))/2
                : index >= 0 ? button.x + Math.max(4, button.width - 15 - font.getWidth(caption)) : button.x + 4;
        font.moveTo(tx + pressed, button.y + (button.height-HEADING_HEIGHT)/2 + 16 + pressed);
        font.paint(queue, caption, color.red, color.green, color.blue, 1.0f);
        if (sortable && button.width >= 18) {
            ChamomiloUiV1Canvas ui = new ChamomiloUiV1Canvas(button); ui.begin(queue);
            int gx = button.x + button.width - 11 + pressed, gy = button.y + (button.height-8)/2 + pressed;
            if (active) (((Number) HighResWindowAccess.get(table, "sortOrder")).intValue() > 0 ? UiIcon.CHEVRON_UP : UiIcon.CHEVRON_DOWN)
                    .paint(ui, color, 1.0f, gx, gy, 8);
            else {
                UiColor glyph = button.hovered && button.isEnabled() ? color : UiColor.MUTED;
                UiIcon.CHEVRON_UP.paint(ui, glyph, 1.0f, gx, gy - 2, 8);
                UiIcon.CHEVRON_DOWN.paint(ui, glyph, 1.0f, gx, gy + 2, 8);
            }
        }
        return true;
    }
    private static final class ScrollSkin {
        final WurmScrollPanel scroll;
        final ChamomiloUiV1ScrollBar horizontal, vertical;
        final boolean horizontalSuppressed;
        boolean synchronizing;
        ScrollSkin(WurmScrollPanel scroll) {
            this.scroll = scroll;
            boolean chat = false;
            for (WurmComponent ancestor = scroll; ancestor != null; ancestor = ancestor.parent)
                if (WINDOWS.get(ancestor) instanceof HighResChatWindow) { chat = true; break; }
            horizontalSuppressed = chat;
            horizontal = chat || scroll.horizontalScrollBar == null ? null : new ChamomiloUiV1ScrollBar(scroll.name + ".highres.x", UiAxis.HORIZONTAL,
                    240, value -> { scroll.xo = value; if (!synchronizing) ((ContainerComponent) scroll.offs).layout(); });
            vertical = scroll.verticalScrollBar == null ? null : new ChamomiloUiV1ScrollBar(scroll.name + ".highres.y", UiAxis.VERTICAL,
                    180, value -> { scroll.yo = value; scroll.isAtBottom = value >= Math.max(0, scroll.content.height - ((FlexComponent) scroll.offs).height);
                        if (!synchronizing) ((ContainerComponent) scroll.offs).layout(); });
            if (horizontal != null) horizontal.setScale(HighResChannelStrip.ART_SCALE);
            if (vertical != null) vertical.setScale(HighResChannelStrip.ART_SCALE);
            scroll.setComponent(horizontal, WurmBorderPanel.SOUTH);
            if (chat) scroll.xo = 0;
            if (vertical != null) scroll.setComponent(vertical, WurmBorderPanel.EAST);
            scroll.layout(); synchronize();
        }
        void synchronize() {
            synchronizing = true;
            try {
                if (horizontalSuppressed) {
                    scroll.xo = 0;
                    if (scroll.content instanceof WurmTextPanel && (Boolean) HighResWindowAccess.get(scroll.content, "useWrap")
                            && scroll.content.width != ((FlexComponent) scroll.offs).width)
                        scroll.content.setSize(((FlexComponent) scroll.offs).width, scroll.content.height);
                }
                if (horizontal != null) { horizontal.setRange(scroll.content.width, ((FlexComponent) scroll.offs).width); horizontal.setValue(scroll.xo); scroll.xo = horizontal.value(); }
                if (vertical != null) { vertical.setRange(scroll.content.height, ((FlexComponent) scroll.offs).height); vertical.setValue(scroll.yo); scroll.yo = vertical.value(); }
            } finally { synchronizing = false; }
        }
    }
    public static boolean mouseWheeled(WurmComponent component, int x, int y, int delta) {
        ScrollSkin skin = null;
        for (WurmComponent cursor = component; cursor != null && skin == null; cursor = cursor.parent) skin = SCROLLS.get(cursor);
        if (skin == null) return false;
        skin.synchronize();
        ChamomiloUiV1ScrollBar bar = skin.vertical != null ? skin.vertical : skin.horizontal;
        if (bar != null) bar.mouseWheeled(x, y, delta);
        return true;
    }
}
