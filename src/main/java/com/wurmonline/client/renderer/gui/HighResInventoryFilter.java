package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.options.Options;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.HudTextFonts;
import java.util.*;
import org.chamomilo.wurm.ui.v1.*;
import org.highreshud.client.i18n.HudMessages;

/** A transient projection of native nodes; IDs, selection and open states remain native. */
final class HighResInventoryFilter {
    private static final int FIELD_HEIGHT=22;
    final WurmTreeList<?> table;
    final WButton name;
    final FilterField field;
    final HighResInventoryOpacity opacity;
    private String query="";
    HighResInventoryFilter(WurmTreeList<?> table,WButton name,HighResNativeWindow skin) {
        this.table=table;this.name=name;
        field=new FilterField();
        opacity=new HighResInventoryOpacity(skin);
    }
    private void changed(String value) {
        HighResWindowIntegration.inventoryDragCleared();
        query=value.trim().toLowerCase(Locale.ROOT);
        table.recalcLines();refresh();
        FlexComponent body=(FlexComponent)HighResWindowAccess.get(table,"treeList");
        for(WurmComponent parent=body.parent;parent!=null;parent=parent.parent)if(parent instanceof WurmScrollPanel) {
            WurmScrollPanel scroll=(WurmScrollPanel)parent;scroll.yo=0;scroll.isAtBottom=false;scroll.layout();break;
        }
    }
    void reset() {field.setText("");changed("");}
    static int captionGap() {return HighResWindowTypography.heading().getWidth("nnn");}
    void layout() {
        int left=name.x+HighResWindowTypography.heading().getWidth(HudMessages.tr("Name"))+6+captionGap();
        field.place(left,name.y+(name.height-FIELD_HEIGHT)/2);
        opacity.setLocation(field.x+field.width+6,field.y,HighResInventoryOpacity.widthFor(field.width),FIELD_HEIGHT);
    }
    void paint(Queue queue) {layout();field.render(queue,1f);}
    FlexComponent at(int x,int y) {layout();return opacity.contains(x,y)?opacity:field.contains(x,y)?field.getComponentAt(x,y):null;}
    void paintOpacity(Queue queue) {layout();opacity.render(queue,1f);}
    void refresh() {
        layout();
        if(query.isEmpty() || HighResWindowIntegration.inventoryGap(table))return;
        if((Boolean)HighResWindowAccess.get(table,"shouldRecalcLines")) {
            table.recalcLines();HighResWindowAccess.set(table,"shouldRecalcLines",false);
        }
        List<WTreeListNode<?>> filtered=new ArrayList<>();
        collect((WTreeListNode<?>)HighResWindowAccess.get(table,"rootNode"),filtered);
        if(table.lines.equals(filtered))return;
        rows().clear();rows().addAll(filtered);
        FlexComponent body=(FlexComponent)HighResWindowAccess.get(table,"treeList");
        int height=((Number)HighResWindowAccess.get(table,"lineHeight")).intValue();
        body.setSize(body.width,filtered.size()*height);
    }
    @SuppressWarnings("unchecked") private List<WTreeListNode<?>> rows() {return (List<WTreeListNode<?>>)(List<?>)table.lines;}
    private boolean collect(WTreeListNode<?> parent,List<WTreeListNode<?>> result) {
        boolean any=false;
        for(WTreeListNode<?> node:parent.children) {
            if(node.item==null)continue;
            String text=node.item.getName();
            if("body".equals(text) && (Boolean)HighResWindowAccess.get(table,"isPlayerOneInventory")
                    && !Options.showBodyInInventory.value())continue;
            List<WTreeListNode<?>> children=new ArrayList<>();
            boolean descendant=collect(node,children);
            if(descendant || text!=null && text.toLowerCase(Locale.ROOT).contains(query)) {
                result.add(node);result.addAll(children);any=true;
            }
        }
        return any;
    }
    /** Native editing with the explicitly requested frameless background. */
    final class FilterField extends FlexComponent implements InputFieldListener {
        final WurmInputField input;
        private final ChamomiloUiV1Canvas canvas=new ChamomiloUiV1Canvas(this);
        // Match the supplied Chamomilo text-field reference.
        private final UiColor background=UiColor.rgb(0x241d14);
        FilterField() {
            super("highres.inventory.filter",0,0,1,FIELD_HEIGHT);
            input=new WurmInputField("",this,1,128);input.prompt="";input.parent=this;
            input.text=HudTextFonts.body(14,false);input.textBold=HudTextFonts.body(14,true);
            input.setLineHeight(input.text.getHeight()+1);input.setMaxLines(1);
            input.setBackgroundColor(background.red,background.green,background.blue);
            input.setPenColor(UiColor.TEXT.red,UiColor.TEXT.green,UiColor.TEXT.blue);
        }
        void place(int left,int top) {
            int fiveLetters=Math.max(input.text.getWidth("WWWWW"),input.text.getWidth("ШШШШШ"));
            // Native textArea reserves 8px; allow that plus the outer 2px insets.
            setLocation(left,top,fiveLetters+12,FIELD_HEIGHT);
            input.setLocation(x+2,y+(height-input.text.getHeight()-2)/2,width-4,input.text.getHeight()+2);
        }
        void setText(String value) {input.setTextMoveToEnd(value);}
        String value() {return input.getText();}
        @Override public FlexComponent getComponentAt(int x,int y) {return contains(x,y)?input:null;}
        @Override protected void renderComponent(Queue queue,float ignoredAlpha) {
            canvas.begin(queue);canvas.fill(background,1f,x,y,width,height);input.render(queue,1f);
        }
        @Override public void handleInput(String value) {setText(value);changed(value);}
        @Override public void handleInputChanged(WurmInputField input,String value) {changed(value);}
        @Override public void handleEscape(WurmInputField input) { }
    }
}
