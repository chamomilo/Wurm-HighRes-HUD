package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.HudTextFonts;
import com.wurmonline.client.renderer.gui.text.TextFont;
import java.util.*;
import java.util.function.LongSupplier;
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
    private static final long HOVER_DELAY = 1_000_000_000L;
    private static final long ARRIVAL_TIMEOUT = 30_000_000_000L;
    private final LongSupplier clock;
    private DraggableComponent activeSource;
    private HighResNativeWindow hoverWindow;
    private WTreeListNode<?> hoverGroup;
    private long windowSince, groupSince;
    private boolean append;
    private final List<PendingOrder> pending = new ArrayList<>();

    HighResInventoryDrag() { this(System::nanoTime); }
    HighResInventoryDrag(LongSupplier clock) { this.clock=clock; }

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
        clearPreview();
        activeSource=null;hoverWindow=null;hoverGroup=null;windowSince=groupSince=0;
    }
    void reset() { clear();pending.clear(); }
    void removed(WWindow window) { clear();pending.removeIf(order -> order.skin.window==window); }
    private void clearPreview() {
        if(gap!=null&&table!=null) {lines(table).remove(gap);resizeBody();}
        gap=null;target=null;destination=null;table=null;label=null;blocked=false;index=0;append=false;
    }
    private void resizeBody() {
        FlexComponent body=(FlexComponent)HighResWindowAccess.get(table,"treeList");
        body.setSize(body.width,Math.max(1,table.lines.size()*lineHeight()));
    }
    private int lineHeight() {return ((Number)HighResWindowAccess.get(table,"lineHeight")).intValue();}
    void update(HeadsUpDisplay hud,Collection<HighResNativeWindow> windows,int x,int y) {
        DraggableComponent dragged=(DraggableComponent)HighResWindowAccess.get(hud,"draggedItem");
        if(dragged==null) {clear();return;}
        if(dragged!=activeSource) {clear();activeSource=dragged;}
        long now=clock.getAsLong();
        HighResNativeWindow next=null;
        WurmComponent hovered=hud.getComponentAt(x,y);
        for(HighResNativeWindow skin:windows)if(!skin.window.minimized&&skin.itemTable()!=null&&bodyAt(skin,x,y)!=null) {
            for(WurmComponent cursor=hovered;cursor!=null;cursor=cursor.parent)
                if(cursor==skin.window) {next=skin;break;}
            if(next!=null)break;
        }
        if(next!=hoverWindow) {hoverWindow=next;windowSince=now;hoverGroup=null;}
        if(next==null) {clearPreview();return;}
        if(gap!=null&&table!=null&&lines(table).contains(gap)) {
            FlexComponent body=(FlexComponent)HighResWindowAccess.get(table,"treeList");
            int gapRow=lines(table).indexOf(gap),top=body.y+gapRow*lineHeight();
            // An appended preview row must not capture an incoming item that
            // has moved into blank viewport space. Keep same-parent reordering.
            if(bodyAt(destination,x,y)!=null&&y>=top&&y<top+lineHeight()
                    &&next==destination&&(gapRow<lines(table).size()-1||localMove(dragged,target,destination))) {
                if(hoverGroup==target&&now-groupSince>=HOVER_DELAY) {
                    WurmTreeList<?> expandedTable=table;WTreeListNode<?> expanded=hoverGroup;
                    clearPreview();expanded.setOpen(true);expandedTable.recalcLines();hoverGroup=null;
                    HighResWindowIntegration.refreshInventoryFilter(expandedTable);
                } else return;
            }
        }
        clearPreview();destination=next;table=next.itemTable();
        FlexComponent body=(FlexComponent)HighResWindowAccess.get(table,"treeList");
        int row=Math.max(0,(y-body.y)/lineHeight());
        List<WTreeListNode<?>> rows=lines(table);
        WTreeListNode<?> node=row<rows.size()?rows.get(row):null;
        if(node==null||node.isOpen()||node.children.isEmpty()||blocked(dragged,node))hoverGroup=null;
        else {
            if(node!=hoverGroup) {hoverGroup=node;groupSince=now;}
            else if(now-groupSince>=HOVER_DELAY) {
                node.setOpen(true);table.recalcLines();hoverGroup=null;
                HighResWindowIntegration.refreshInventoryFilter(table);
            }
        }
        // Each destination window gets its own grace period, even if rows change.
        // Frame updates also advance the timers while the mouse is stationary.
        if(!sameWindow(dragged,next)&&now-windowSince<HOVER_DELAY) {
            target=(WTreeListNode<?>)HighResWindowAccess.get(table,"rootNode");
            blocked=blocked(dragged,target);append=true;return;
        }
        // Native blank-space drops target the window root, regardless of which
        // expanded container owns the last displayed child.
        if(rows.isEmpty()||y>=body.y+rows.size()*lineHeight()) {
            target=(WTreeListNode<?>)HighResWindowAccess.get(table,"rootNode");
            blocked=blocked(dragged,target);
            label=blocked?"Cannot move here":"Insert here";append=true;return;
        }
        row=Math.min(row,rows.size()-1);target=rows.get(row);
        int within=y-body.y-row*lineHeight();
        InventoryMetaItem meta=meta(target.item);
        boolean insertion=within<Math.max(3,lineHeight()/4)||within>=lineHeight()-Math.max(3,lineHeight()/4);
        index=row+(within>=lineHeight()/2?1:0);
        // A sibling placed after an expanded row follows its whole visible subtree.
        if(insertion&&index>row)while(index<rows.size()&&rows.get(index).depth>target.depth)index++;
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
        if(group(item))return false;
        if(item instanceof InventoryListComponent.InventoryTreeListItem)return ((InventoryListComponent.InventoryTreeListItem)item).isContainer;
        return item instanceof InventoryContainerWindow.InventoryContainerItem&&((InventoryContainerWindow.InventoryContainerItem)item).getIsContainer();
    }
    private static boolean sameWindow(DraggableComponent source,HighResNativeWindow skin) {
        return source instanceof InventoryListComponent.InventoryTreeListItem
                ? ((InventoryListComponent.InventoryTreeListItem)source).getOwner().itemList==skin.itemTable()
                : source instanceof InventoryContainerWindow.InventoryContainerItem&&HighResWindowAccess.get(source,"this$0")==skin.window;
    }
    private static boolean localMove(DraggableComponent source,WTreeListNode<?> target,HighResNativeWindow skin) {
        InventoryMetaItem sourceMeta=source instanceof TreeListItem?meta((TreeListItem)source):null;
        InventoryMetaItem targetMeta=target==null?null:meta(target.item);
        return sourceMeta!=null&&targetMeta!=null&&sourceMeta.getParentId()==targetMeta.getParentId()
                &&(source instanceof InventoryListComponent.InventoryTreeListItem
                    ?((InventoryListComponent.InventoryTreeListItem)source).getOwner().itemList==skin.itemTable()
                    :source instanceof InventoryContainerWindow.InventoryContainerItem&&HighResWindowAccess.get(source,"this$0")==skin.window);
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
            InventoryMetaItem meta=meta(cursor.item);if(!group(cursor.item)&&meta!=null&&ids.contains(meta.getId()))return true;
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
        boolean local=localMove(source,target,skin);
        int targetIndex=target==null?-1:lines(table).indexOf(target);
        boolean insertion=gap!=null;
        boolean after=insertion&&index>targetIndex-(targetIndex>=index?1:0);
        WTreeListNode<?> dropTarget=gap==null||local?target:target==null?null:target.parentNode;
        if(!local&&!append&&target!=null&&!container(target.item))dropTarget=target.parentNode;
        if(!local)while(dropTarget!=null&&group(dropTarget.item))dropTarget=dropTarget.parentNode;
        WTreeListNode<?> anchor=insertion||!append&&target!=null&&!container(target.item)?target:null;
        boolean placeAfter=insertion?after:target!=null&&y>=((FlexComponent)HighResWindowAccess.get(list,"treeList")).y+targetIndex*lineHeight()+lineHeight()/2;
        PendingOrder order=!local&&!blocked&&dropTarget!=null
                ? new PendingOrder(skin,source,dropTarget,anchor,placeAfter,clock.getAsLong()) : null;
        boolean cancel=blocked;
        clear();
        FlexComponent body=(FlexComponent)HighResWindowAccess.get(list,"treeList");
        int row=dropTarget==null?-1:lines(list).indexOf(dropTarget);
        int height=((Number)HighResWindowAccess.get(list,"lineHeight")).intValue();
        // An external container's root (or a filtered root) has no visible row.
        // Native hit testing uses integer division: -height+2 truncates to row 0
        // and redirects the move into the first item instead of the container.
        // A full row above the body reliably selects the native root fallback.
        int dropY=row<0?body.y-height:body.y+row*height+(local&&after?height-2:2);
        try {
            if(!cancel) {
                if(local && (insertion || !container(dropTarget.item))) {
                    // Native recalcLines otherwise immediately sorts the manual move away.
                    list.setKeepAutoSorted(false);
                    HighResWindowAccess.set(list,"sortOn",-1);
                    HighResWindowAccess.set(list,"lastSortOn",-1);
                }
                if(local && (insertion&&container(dropTarget.item)||group(dropTarget.item))) reorder(list,dropTarget,dropY);
                else if(skin.window instanceof InventoryContainerWindow)skin.window.itemDropped(x,dropY,source);
                else {
                    InventoryListComponent owner=skin.window instanceof InventoryWindow?((InventoryWindow)skin.window).getInventoryListComponent()
                            :(InventoryListComponent)HighResWindowAccess.get(skin.window,"component");
                    owner.itemDropped(x,dropY,source);
                }
                if(order!=null&&order.ids.length>0)pending.add(order);
            }
        } finally {
            HighResWindowAccess.set(hud,"draggedItem",null);
            HighResWindowAccess.set(hud,"mouseFocusComponent",null);
        }
        return true;
    }
    boolean hasGap(WurmTreeList<?> list) {return table==list && gap!=null && lines(list).contains(gap);}
    /** Position only items whose arrival in this inventory has been confirmed by the server. */
    void arrivals(Collection<HighResNativeWindow> windows) {
        long now=clock.getAsLong();
        Iterator<PendingOrder> iterator=pending.iterator();
        while(iterator.hasNext()) {
            PendingOrder order=iterator.next();
            if(!windows.contains(order.skin)||now-order.created>=ARRIVAL_TIMEOUT) {iterator.remove();continue;}
            WurmTreeList<?> list=order.skin.itemTable();
            WTreeListNode<?> root=(WTreeListNode<?>)HighResWindowAccess.get(list,"rootNode");
            List<WTreeListNode<?>> arrived=new ArrayList<>();int found=0;
            for(long id:order.ids) {
                WTreeListNode<?> node=findItem(root,id,order.parentId);
                if(node==null)continue;
                found++;
                // Groups stay a single row unless the chosen position is inside one.
                while(node.parentNode!=null&&group(node.parentNode.item)
                        &&(order.anchor==null||node.parentNode!=order.anchor.parentNode))node=node.parentNode;
                if(!arrived.contains(node))arrived.add(node);
            }
            if(!arrived.isEmpty()) {
                clearPreview();manualOrder(list);
                Set<WTreeListNode<?>> parents=Collections.newSetFromMap(new IdentityHashMap<WTreeListNode<?>,Boolean>());
                for(WTreeListNode<?> node:arrived)if(node.parentNode!=null)parents.add(node.parentNode);
                for(WTreeListNode<?> parent:parents)place(parent,arrived,order.anchor,order.after);
                list.recalcLines();HighResWindowIntegration.refreshInventoryFilter(list);
            }
            if(found==order.ids.length)iterator.remove();
        }
    }
    private static boolean group(TreeListItem item) {
        return item!=null&&item.getClass().getName().equals("com.wurmonline.client.renderer.gui.InventoryListComponent$GroupTreeListItem");
    }
    private static WTreeListNode<?> findItem(WTreeListNode<?> node,long id,long parentId) {
        InventoryMetaItem value=meta(node.item);
        if(!group(node.item)&&value!=null&&value.getId()==id&&value.getParentId()==parentId)return node;
        for(WTreeListNode<?> child:node.children) {WTreeListNode<?> found=findItem(child,id,parentId);if(found!=null)return found;}
        return null;
    }
    @SuppressWarnings({"rawtypes","unchecked"}) private static void place(WTreeListNode parent,List<WTreeListNode<?>> arrived,WTreeListNode<?> anchor,boolean after) {
        List<WTreeListNode<?>> members=new ArrayList<>();
        for(WTreeListNode<?> node:arrived)if(node.parentNode==parent)members.add(node);
        if(members.contains(anchor))return;
        parent.children.removeAll(members);
        int at=parent.children.indexOf(anchor);
        parent.children.addAll(at<0?parent.children.size():at+(after?1:0),members);
        parent.orderChanged();
    }
    private static void manualOrder(WurmTreeList<?> list) {
        list.setKeepAutoSorted(false);HighResWindowAccess.set(list,"sortOn",-1);HighResWindowAccess.set(list,"lastSortOn",-1);
    }
    private static long[] sourceIds(DraggableComponent source) {
        if(source instanceof InventoryListComponent.InventoryTreeListItem)return ((InventoryListComponent.InventoryTreeListItem)source).getOwner().getSelectedCommandTargets();
        if(source instanceof InventoryContainerWindow.InventoryContainerItem)return ((InventoryContainerWindow.InventoryContainerItem)source).getSelectedCommandTargets();
        if(source instanceof PaperDollItem)return new long[]{((PaperDollItem)source).getId()};
        return new long[0];
    }
    private static final class PendingOrder {
        final HighResNativeWindow skin;final long[] ids;final long parentId,created;final WTreeListNode<?> anchor;final boolean after;
        PendingOrder(HighResNativeWindow skin,DraggableComponent source,WTreeListNode<?> target,WTreeListNode<?> anchor,boolean after,long created) {
            this.skin=skin;this.ids=sourceIds(source);this.anchor=anchor;this.after=after;this.created=created;
            if(target.item instanceof InventoryListComponent.InventoryTreeListItem)parentId=((InventoryListComponent.InventoryTreeListItem)target.item).getDropTarget();
            else {
                InventoryMetaItem value=meta(target.item);
                if(value==null)parentId=((InventoryContainerWindow.InventoryContainerItem)HighResWindowAccess.get(skin.window,"rootItem")).getId();
                else parentId=target==HighResWindowAccess.get(skin.itemTable(),"rootNode")||container(target.item)?value.getId():value.getParentId();
            }
        }
    }
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
