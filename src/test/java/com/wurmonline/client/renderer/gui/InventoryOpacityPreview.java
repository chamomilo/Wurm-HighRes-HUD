package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Primitive;
import com.wurmonline.client.renderer.backend.Queue;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import org.chamomilo.wurm.ui.v1.*;

/** GPU substitute records native queue colors, then paints after production opacity scaling. */
public final class InventoryOpacityPreview {
    private static Queue active;
    private static final List<Runnable> commands = new ArrayList<>();
    private static Object allocate(Class<?> type) throws Exception {
        java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        return ((sun.misc.Unsafe)field.get(null)).allocateInstance(type);
    }
    static Queue begin() throws Exception {
        active = (Queue)allocate(Queue.class);
        HighResWindowAccess.set(active,"queue",new Primitive[8192]);
        HighResWindowAccess.set(active,"queueCount",0); commands.clear(); return active;
    }
    static void finish() { for(Runnable command:commands)command.run(); commands.clear(); active=null; }
    static boolean captures(Queue queue) { return queue!=null&&queue==active; }
    private static void record(Queue queue,float alpha,java.util.function.Consumer<Float> paint) {
        try {
            Primitive primitive=(Primitive)allocate(Primitive.class); primitive.a=alpha;
            primitive.blendmode=Primitive.BlendMode.ALPHABLEND;
            int index=queue.getQueueCount(); queue.getQueue()[index]=primitive;
            HighResWindowAccess.set(queue,"queueCount",index+1);
            Shape clip=HudButtonProbe.graphics.getClip();
            commands.add(() -> { Shape previous=HudButtonProbe.graphics.getClip();HudButtonProbe.graphics.setClip(clip);
                paint.accept(primitive.a);HudButtonProbe.graphics.setClip(previous); });
        } catch(Exception failure) { throw new IllegalStateException(failure); }
    }
    public static void fill(Queue queue,UiColor color,float alpha,int x,int y,int w,int h) {
        if(captures(queue))record(queue,alpha,a -> HudButtonProbe.canvas.fill(color,a,x,y,w,h));
        else HudButtonProbe.canvas.fill(color,alpha,x,y,w,h);
    }
    public static boolean texture(Queue queue,UiAsset asset,float tint,float alpha,int x,int y,int w,int h,float u0,float v0,float u1,float v1) {
        if(captures(queue)) {record(queue,alpha,a -> HudButtonProbe.canvas.texture(asset,tint,a,x,y,w,h,u0,v0,u1,v1));return true;}
        return HudButtonProbe.canvas.texture(asset,tint,alpha,x,y,w,h,u0,v0,u1,v1);
    }
    static void text(Queue queue,HudButtonProbe.ProbeFont font,String value,float r,float g,float b,float alpha) {
        int x=font.x,y=font.y;Font awt=UiTypography.font(font.size,font.bold,font.density);
        record(queue,alpha,a -> {HudButtonProbe.graphics.setFont(awt);HudButtonProbe.graphics.setComposite(AlphaComposite.SrcOver);
            HudButtonProbe.graphics.setColor(new Color(r,g,b,a));HudButtonProbe.graphics.drawString(value,x,y);});
    }
    public static void rows(FlexComponent body,Queue queue) {
        if(!captures(queue))return;
        WurmTreeList<?> table=(WurmTreeList<?>)HighResWindowAccess.get(body,"this$0");
        int height=((Number)HighResWindowAccess.get(table,"lineHeight")).intValue();
        for(int n=0;n<table.lines.size();n++) {
            WTreeListNode<?> node=table.lines.get(n);
            table.text.moveTo(body.x+18+Math.max(0,node.depth)*8,body.y+n*height+table.text.getAscent());
            table.text.paint(queue,node.item.getName(),.9f,.85f,.7f,1f);
        }
    }
}
