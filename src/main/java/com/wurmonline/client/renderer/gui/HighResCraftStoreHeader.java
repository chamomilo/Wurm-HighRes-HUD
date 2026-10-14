package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.HudTextFonts;
import com.wurmonline.client.renderer.gui.text.TextFont;
import org.chamomilo.wurm.ui.v1.*;
import org.highreshud.client.i18n.HudMessages;

/** Inventory heading role, native buttons/editor and the same inline filter surface. */
final class HighResCraftStoreHeader extends FlexComponent {
    final HighResBuildWindow.Store store;
    final Heading[] headings=new Heading[4];
    final Resize[] handles=new Resize[3];
    final int[] columns={250,62,62,110};
    final Filter filter=new Filter();
    private int lastWidth;
    HighResCraftStoreHeader(String id,HighResBuildWindow.Store store){
        super(id,0,0,520,28);this.store=store;filter.parent=this;
        String[] labels={"Name","QL","DMG","Weight"};
        for(int i=0;i<4;i++){headings[i]=new Heading(labels[i],i);headings[i].parent=this;}
        for(int i=0;i<3;i++){handles[i]=new Resize(i);handles[i].parent=this;}
    }
    void layoutColumns(){
        int available=Math.max(400,width-20);
        if(lastWidth!=available){columns[0]=Math.max(166,available-columns[1]-columns[2]-columns[3]);columns[3]=available-columns[0]-columns[1]-columns[2];lastWidth=available;}
        int left=x;
        for(int i=0;i<4;i++){headings[i].setLocation(left,y,columns[i],height);left+=columns[i];if(i<3)handles[i].setLocation(left-3,y,6,height);}
        filter.place(x+6+HighResWindowTypography.heading().getWidth(HudMessages.tr("Name"))+HighResInventoryFilter.captionGap(),y+(height-22)/2,Math.max(24,columns[0]-HighResWindowTypography.heading().getWidth(HudMessages.tr("Name"))-HighResInventoryFilter.captionGap()-26));
    }
    @Override void componentResized(){layoutColumns();}
    @Override public FlexComponent getComponentAt(int px,int py){if(!contains(px,py))return null;layoutColumns();for(Resize r:handles)if(r.contains(px,py))return r;if(filter.contains(px,py))return filter.getComponentAt(px,py);for(Heading h:headings)if(h.contains(px,py))return h;return this;}
    @Override protected void renderComponent(Queue q,float ignored){layoutColumns();ChamomiloUiV1Canvas ui=new ChamomiloUiV1Canvas(this);ui.begin(q);UiPainter.background(ui,UiBackground.LEATHER,UiScale.BASE,1f,x,y,width,height);UiPainter.frame(ui,3,1f,x,y,width,height);for(Heading h:headings)h.render(q,1f);filter.render(q,1f);}
    final class Heading extends WButton {
        final int column;boolean activate;int downX,downY;
        Heading(String label,int column){super(label);this.column=column;sizeFlags=0;setButtonListener(new ButtonListener(){public void buttonPressed(WButton button){}public void buttonClicked(WButton button){if(activate)store.sort(column);activate=false;}});}
        @Override protected void leftPressed(int px,int py,int count){downX=px;downY=py;activate=contains(px,py);HighResWindowAccess.set(this,"isClosing",true);isCloseHovered=activate;}
        @Override protected void mouseDragged(int px,int py){if(!contains(px,py)||Math.abs(px-downX)+Math.abs(py-downY)>4)activate=false;super.mouseDragged(px,py);}
        @Override protected void mouseExited(){activate=false;super.mouseExited();}
        @Override public void pick(com.wurmonline.client.renderer.PickData pick,int px,int py){pick.addText(HudMessages.tr(column==0?"Item name":column==1?"Item quality":column==2?"Item damage":"Item weight"));}
        @Override protected void renderComponent(Queue q,float ignored){boolean active=store.sortColumn==column;UiColor color=active||hovered?UiColor.rgb(0xfff09a):UiColor.TEXT;TextFont font=HighResWindowTypography.heading();String value=HudMessages.tr(getLabel());int max=column==0?Math.max(0,filter.x-x-10):width-20;value=HighResWindowIntegration.ellipsis(value,font,max);int press=isCloseHovered?1:0;int tx=column==0?x+6:x+Math.max(4,width-15-font.getWidth(value));font.moveTo(tx+press,y+(height-HighResWindowIntegration.HEADING_HEIGHT)/2+16+press);font.paint(q,value,color.red,color.green,color.blue,1f);ChamomiloUiV1Canvas ui=new ChamomiloUiV1Canvas(this);ui.begin(q);int gx=x+width-11+press,gy=y+(height-8)/2+press;if(active)(store.sortOrder>0?UiIcon.CHEVRON_UP:UiIcon.CHEVRON_DOWN).paint(ui,color,1f,gx,gy,8);else{UiIcon.CHEVRON_UP.paint(ui,hovered?color:UiColor.MUTED,1f,gx,gy-2,8);UiIcon.CHEVRON_DOWN.paint(ui,hovered?color:UiColor.MUTED,1f,gx,gy+2,8);}}
    }
    final class Resize extends FlexComponent {
        final int boundary;int origin,first,second;boolean dragging;
        Resize(int boundary){super("highres.build.column.resize",0,0,6,28);this.boundary=boundary;}
        @Override protected void leftPressed(int px,int py,int count){origin=px;first=columns[boundary];second=columns[boundary+1];dragging=true;}
        @Override protected void mouseDragged(int px,int py){if(!dragging)return;int min=boundary==0?166:54,nextMin=boundary+1==3?86:54;int change=Math.max(min-first,Math.min(second-nextMin,px-origin));columns[boundary]=first+change;columns[boundary+1]=second-change;layoutColumns();}
        @Override protected void leftReleased(int px,int py){mouseDragged(px,py);dragging=false;}
    }
    final class Filter extends FlexComponent implements InputFieldListener {
        final WurmInputField input;
        Filter(){super("highres.build.inventory.filter",0,0,1,22);input=new WurmInputField("",this,1,128);input.prompt="";input.parent=this;input.text=HudTextFonts.body(14,false);input.textBold=HudTextFonts.body(14,true);input.setLineHeight(input.text.getHeight()+1);input.setMaxLines(1);input.setBackgroundColor(.141f,.114f,.078f);input.setPenColor(UiColor.TEXT.red,UiColor.TEXT.green,UiColor.TEXT.blue);}
        void place(int left,int top,int maximum){int letters=Math.max(input.text.getWidth("WWWWW"),input.text.getWidth("ШШШШШ"));setLocation(left,top,Math.min(maximum,letters+12),22);input.setLocation(x+2,y+(height-input.text.getHeight()-2)/2,Math.max(1,width-4),input.text.getHeight()+2);}
        @Override public FlexComponent getComponentAt(int px,int py){return contains(px,py)?input:null;}
        @Override protected void renderComponent(Queue q,float ignored){ChamomiloUiV1Canvas ui=new ChamomiloUiV1Canvas(this);ui.begin(q);ui.fill(UiColor.rgb(0x241d14),1f,x,y,width,height);input.render(q,1f);}
        public void handleInput(String value){input.setTextMoveToEnd(value);handleInputChanged(input,value);}
        public void handleInputChanged(WurmInputField input,String value){store.query=value.trim().toLowerCase(java.util.Locale.ROOT);store.resizeRows();(store.isTools?store.owner().panel.tools:store.owner().panel.materials).scrollTo(0,0);}
        public void handleEscape(WurmInputField input){}
        @Override public void pick(com.wurmonline.client.renderer.PickData pick,int px,int py){pick.addText(HudMessages.tr("Filter recipe or component"));}
    }
}
