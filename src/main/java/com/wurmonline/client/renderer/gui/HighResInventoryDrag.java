package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.HudTextFonts;
import com.wurmonline.client.renderer.gui.text.TextFont;
import java.util.*;
import org.chamomilo.wurm.ui.v1.*;
import org.highreshud.client.i18n.HudMessages;

/** Native item moves with a temporary insertion row and explicit destination feedback. */
final class HighResInventoryDrag {
    private HighResNativeWindow destination;
    private WurmTreeList<?> table;
    private WTreeListNode<?> gap, target;
    private int index;
    private boolean blocked;
    private String label;

    static FlexComponent bodyAt(HighResNativeWindow skin,int x,int y) {
        WurmTreeList<?> table=skin.itemTable();
        if(table==null)return null;
        FlexComponent body=(FlexComponent)HighResWindowAccess.get(table,"treeList");
        // The contents can extend beyond the viewport when scrolled.
        for(WurmComponent cursor=body.parent;cursor!=null&&cursor!=skin.window;cursor=cursor.parent)
            if(!cursor.contains(x,y))return null;
        // The empty portion of the viewport is also a valid drop target.
        return body;
    }
    @SuppressWarnings("unchecked") private static List<WTreeListNode<?>> lines(WurmTreeList<?> table) {return (List<WTreeListNode<?>>)(List<?>)table.lines;}
    void clear() {
        if(gap!=null&&table!=null) {lines(table).remove(gap);resizeBody();}
        gap=null;target=null;destination=null;table=null;label=null;blocked=false;index=0;
    }
    private void resizeBody() {
        FlexComponent body=(FlexComponent)HighResWindowAccess.get(table,"treeList");
        body.setSize(body.width,Math.max(1,table.lines.size()*lineHeight()));
    }
    private int lineHeight() {return ((Number)HighResWindowAccess.get(table,"lineHeight")).intValue();}
    void update(HeadsUpDisplay hud,Collection<HighResNativeWindow> windows,int x,int y) {
        DraggableComponent dragged=(DraggableComponent)HighResWindowAccess.get(hud,"draggedItem");
        if(dragged==null) {clear();return;}
        if(gap!=null&&table!=null&&lines(table).contains(gap)) {
            FlexComponent body=(FlexComponent)HighResWindowAccess.get(table,"treeList");
            int top=body.y+lines(table).indexOf(gap)*lineHeight();
            if(bodyAt(destination,x,y)!=null&&y>=top&&y<top+lineHeight())return;
        }
        clear();
        WurmComponent hovered=hud.getComponentAt(x,y);
        for(HighResNativeWindow skin:windows)if(!skin.window.minimized&&skin.itemTable()!=null&&bodyAt(skin,x,y)!=null) {
            boolean sameWindow=false;
            for(WurmComponent cursor=hovered;cursor!=null;cursor=cursor.parent)if(cursor==skin.window)sameWindow=true;
            if(!sameWindow)continue;
            destination=skin;table=skin.itemTable();break;
        }
        if(table==null)return;
        FlexComponent body=(FlexComponent)HighResWindowAccess.get(table,"treeList");
        int row=Math.max(0,(y-body.y)/lineHeight());
        List<WTreeListNode<?>> rows=lines(table);
        if(rows.isEmpty()) {label="Insert here";return;}
        row=Math.min(row,rows.size()-1);target=rows.get(row);
        int within=y-body.y-row*lineHeight();
        InventoryMetaItem meta=meta(target.item);
        boolean insertion=within<Math.max(3,lineHeight()/4)||within>=lineHeight()-Math.max(3,lineHeight()/4)||y>=body.y+rows.size()*lineHeight();
        index=row+(within>=lineHeight()/2?1:0);
        // The native root is an inventory indicator, never a reorder destination.
        if(insertion && index==row && target.item instanceof InventoryListComponent.InventoryTreeListItem
                && ((InventoryListComponent.InventoryTreeListItem)target.item).getOwner().rootItem==target.item) {
            blocked=true;return;
        }
        blocked=blocked(dragged,insertion?target.parentNode:target);
        label=blocked?"Cannot move here":insertion?"Insert here":container(target.item)?"Put inside":"Move here";
        if(insertion) {
            TreeListItem item;
            if(target.item instanceof InventoryListComponent.InventoryTreeListItem) {
                InventoryListComponent owner=((InventoryListComponent.InventoryTreeListItem)target.item).getOwner();
                item=new InsertionItem(owner,meta);
            } else if(target.item instanceof InventoryContainerWindow.InventoryContainerItem)
                item=((InventoryContainerWindow)destination.window).new InventoryContainerItem(meta);
            else return;
            gap=new WTreeListNode<>(null,item);gap.depth=target.depth;
            rows.add(index,gap);resizeBody();
        }
    }
    private static InventoryMetaItem meta(TreeListItem item) {
        if(item instanceof InventoryListComponent.InventoryTreeListItem)return ((InventoryListComponent.InventoryTreeListItem)item).item;
        if(item instanceof InventoryContainerWindow.InventoryContainerItem)return ((InventoryContainerWindow.InventoryContainerItem)item).getItem();
        return null;
    }
    private static boolean container(TreeListItem item) {
        if(item instanceof InventoryListComponent.InventoryTreeListItem)return ((InventoryListComponent.InventoryTreeListItem)item).isContainer;
        return item instanceof InventoryContainerWindow.InventoryContainerItem&&((InventoryContainerWindow.InventoryContainerItem)item).getIsContainer();
    }
    private static boolean blocked(DraggableComponent source,WTreeListNode<?> target) {
        Set<Long> ids=new HashSet<>();
        if(source instanceof InventoryListComponent.InventoryTreeListItem)
            for(InventoryListComponent.InventoryTreeListItem item:((InventoryListComponent.InventoryTreeListItem)source).getSelectedItems())if(item.item!=null)ids.add(item.item.getId());
        if(source instanceof InventoryContainerWindow.InventoryContainerItem)
            for(InventoryContainerWindow.InventoryContainerItem item:((InventoryContainerWindow.InventoryContainerItem)source).getSelectedItems())ids.add(item.getId());
        if(source instanceof TreeListItem) {InventoryMetaItem meta=meta((TreeListItem)source);if(meta!=null)ids.add(meta.getId());}
        if(source instanceof PaperDollItem)ids.add(((PaperDollItem)source).getId());
        for(WTreeListNode<?> cursor=target;cursor!=null;cursor=cursor.parentNode) {
            InventoryMetaItem meta=meta(cursor.item);if(meta!=null&&ids.contains(meta.getId()))return true;
        }
        return false;
    }
    void paint(HighResNativeWindow skin,Queue queue) {
        if(destination!=skin||label==null)return;
        FlexComponent body=(FlexComponent)HighResWindowAccess.get(table,"treeList");
        WurmComponent viewport=body.parent;
        int left=Math.max(body.x,viewport.x),right=Math.min(body.x+body.width,viewport.x+viewport.width);
        int row=gap==null?lines(table).indexOf(target):lines(table).indexOf(gap);
        int top=body.y+Math.max(0,row)*lineHeight();
        int bottom=Math.min(top+lineHeight(),viewport.y+viewport.height);top=Math.max(top,viewport.y);
        if(bottom<=top||right<=left)return;
        UiColor color=blocked?UiColor.rgb(0xe44b38):UiColor.rgb(0xebc56b);
        ChamomiloUiV1Canvas canvas=new ChamomiloUiV1Canvas(body);canvas.begin(queue);
        if(gap!=null)canvas.fill(UiColor.rgb(0x201b15),1f,left,top,right-left,bottom-top);
        canvas.fill(color,1f,left,top,right-left,2);canvas.fill(color,1f,left,bottom-2,right-left,2);
        if(gap==null) {canvas.fill(color,1f,left,top,2,bottom-top);canvas.fill(color,1f,right-2,top,2,bottom-top);}
        TextFont font=HudTextFonts.body(14,true);String caption=HudMessages.tr(label);
        caption=HighResWindowIntegration.ellipsis(caption,font,Math.max(0,right-left-12));
        int width=font.getWidth(caption),textX=left+(right-left-width)/2;
        canvas.fill(UiColor.rgb(0x201b15),1f,textX-4,top+1,width+8,bottom-top-2);
        font.moveTo(textX,top+(bottom-top-font.getHeight())/2+font.getAscent());font.paint(queue,caption,color.red,color.green,color.blue,1f);
    }
    boolean drop(HeadsUpDisplay hud,int x,int y,int button) {
        if(button!=0)return false;
        update(hud,HighResWindowIntegration.skins(),x,y);
        if(destination==null)return false;
        DraggableComponent source=(DraggableComponent)HighResWindowAccess.get(hud,"draggedItem");
        if(source==null)return false;
        HighResNativeWindow skin=destination;WurmTreeList<?> list=table;
        InventoryMetaItem sourceMeta=source instanceof TreeListItem?meta((TreeListItem)source):null;
        InventoryMetaItem targetMeta=target==null?null:meta(target.item);
        boolean local=sourceMeta!=null&&targetMeta!=null&&sourceMeta.getParentId()==targetMeta.getParentId()
                && (source instanceof InventoryListComponent.InventoryTreeListItem
                    ? ((InventoryListComponent.InventoryTreeListItem)source).getOwner().itemList==table
                    : source instanceof InventoryContainerWindow.InventoryContainerItem&&HighResWindowAccess.get(source,"this$0")==skin.window);
        int targetIndex=target==null?-1:lines(table).indexOf(target);
        boolean insertion=gap!=null;
        boolean after=insertion&&index>targetIndex-(targetIndex>=index?1:0);
        WTreeListNode<?> dropTarget=gap==null||local?target:target==null?null:target.parentNode;
        boolean cancel=blocked;
        clear();
        FlexComponent body=(FlexComponent)HighResWindowAccess.get(list,"treeList");
        int row=dropTarget==null?-1:lines(list).indexOf(dropTarget);
        int height=((Number)HighResWindowAccess.get(list,"lineHeight")).intValue();
        int dropY=body.y+row*height+(local&&after?height-2:2);
        try {
            if(!cancel) {
                if(local && (insertion || !container(dropTarget.item))) {
                    // Native recalcLines otherwise immediately sorts the manual move away.
                    list.setKeepAutoSorted(false);
                    HighResWindowAccess.set(list,"sortOn",-1);
                    HighResWindowAccess.set(list,"lastSortOn",-1);
                }
                if(local && insertion && container(dropTarget.item)) reorder(list,dropTarget,dropY);
                else if(skin.window instanceof InventoryContainerWindow)skin.window.itemDropped(x,dropY,source);
                else {
                    InventoryListComponent owner=skin.window instanceof InventoryWindow?((InventoryWindow)skin.window).getInventoryListComponent()
                            :(InventoryListComponent)HighResWindowAccess.get(skin.window,"component");
                    owner.itemDropped(x,dropY,source);
                }
            }
        } finally {
            HighResWindowAccess.set(hud,"draggedItem",null);
            HighResWindowAccess.set(hud,"mouseFocusComponent",null);
        }
        return true;
    }
    boolean hasGap(WurmTreeList<?> list) {return table==list && gap!=null && lines(list).contains(gap);}
    @SuppressWarnings({"rawtypes","unchecked"}) private static void reorder(WurmTreeList list,WTreeListNode target,int y) {
        List<WTreeListNode> selected=new ArrayList<>();
        for(Object item:list.getSelections()) {
            WTreeListNode node=list.getNode((TreeListItem)item);
            if(node.parentNode==target.parentNode)selected.add(node);
        }
        if(!selected.isEmpty())list.insertBefore(selected,target,y);
    }
    private static final class InsertionItem extends InventoryListComponent.InventoryTreeListItem {
        InsertionItem(InventoryListComponent owner,InventoryMetaItem item) {owner.super(item);}
        @Override void update() { }
        @Override String getName() {return "";}
        @Override String getParameter(int column) {return "";}
        @Override int getCommandTargetCount() {return 0;}
        @Override long[] getCommandTargetIds() {return new long[0];}
        @Override DraggableComponent getDraggable() {return null;}
    }
}
