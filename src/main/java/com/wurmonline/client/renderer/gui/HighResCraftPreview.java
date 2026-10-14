package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.model.*;
import com.wurmonline.client.resources.textures.Texture;
import com.wurmonline.client.WurmClientBase;
import com.wurmonline.client.resources.Resources;
import com.wurmonline.client.renderer.backend.Queue;
import java.util.function.Predicate;
import org.highreshud.client.craft.CraftCatalog;
import org.highreshud.client.portrait.*;
import com.wurmonline.shared.util.MaterialUtilities;

/** Independent result model participates in the existing shared portrait frame queue. */
final class HighResCraftPreview {
    private volatile ModelResourceWrapper model;
    private PortraitRenderService renderer;
    private String mapping="";
    private long started=System.nanoTime();
    private volatile long generation;
    private static String baseMapping(CraftCatalog.Template template){
        String name=template.model;
        while(name.endsWith("."))name=name.substring(0,name.length()-1);
        // The rack's stock mapping has an explicit state rather than a generic root.
        if(name.equals("model.container.rack.crate"))name+=".empty";
        return name;
    }
    static String mappingFor(CraftCatalog.Template template,byte material){
        if(template==null)return "";
        String name=baseMapping(template);
        if(!name.isEmpty()&&(template.metal()||template.wood())){
            String suffix=MaterialUtilities.getMaterialString(material);
            if(!name.endsWith("."+suffix))name+="."+suffix;
        }
        return name;
    }
    static String geometryMappingFor(CraftCatalog.Template template,byte material,Predicate<String> exactMapping){
        String selected=mappingFor(template,material);
        if(selected.isEmpty()||exactMapping.test(selected))return selected;
        String base=baseMapping(template);
        if(exactMapping.test(base))return base;
        // Some tool parts only map their iron/steel mesh. Keep that part's geometry
        // instead of letting native prefix fallback resolve the complete tool.
        String stock=mappingFor(template,(byte)template.material);
        return exactMapping.test(stock)?stock:selected;
    }
    static PortraitProfile profileFor(String mapping,String path,ModelDataBounds bounds,float yaw){
        PortraitProfile p=PortraitProfile.defaults(mapping,path);
        p.mode=PortraitProfile.Mode.FULL_MODEL;p.anchor=PortraitProfile.Anchor.BOUNDS;p.pitch=-12f;
        p.yaw=yaw;p.yawMin=-180f;p.yawMax=180f;p.zoom=.85f;
        if(bounds!=null){
            double x=bounds.getX1()-bounds.getX0(),h=bounds.getH1()-bounds.getH0(),y=bounds.getY1()-bounds.getY0();
            double radius=Math.sqrt(x*x+h*h+y*y)*.5;
            // A bounding sphere fits every yaw/pitch, including the near face's
            // perspective enlargement; reserve 10% of the texture around it.
            double angle=Math.atan(Math.tan(Math.toRadians(p.fov*.5))*.9);
            float distance=(float)Math.max(.05,radius/Math.sin(angle)+.01);
            p.cameraDistance=distance*p.zoom/PortraitFraming.baseDistance(bounds,p.mode,1f);
        }
        return p;
    }
    static void paintTexture(Queue queue,Texture texture,FlexComponent box){
        int side=Math.max(1,Math.min(box.width-8,box.height-8));
        // Offscreen textures use OpenGL's bottom origin, like the Character doll.
        Renderer.texturedQuadAlphaBlend(queue,texture,1f,1f,1f,1f,
                box.x+(box.width-side)/2,box.y+(box.height-side)/2,side,side,0f,1f,1f,-1f);
    }
    void select(CraftCatalog.Template template,byte material) {
        String next=mappingFor(template,material);
        if(mapping.equals(next))return;mapping=next;model=null;long token=++generation;
        if(next.isEmpty())return;
        Resources resources=WurmClientBase.getResourceManager();
        String geometry=resources==null?next:geometryMappingFor(template,material,name->resources.resourceExists(name,false));
        ModelResourceWrapper source=ModelResourceLoader.getModel(geometry);
        if(source==null||source==ModelResourceLoader.getBrokenModel())return;
        source.addLoadListener(new ModelLoadListener(){
            public boolean wantNewModelCopy(){return true;}
            public void modelLoaded(ModelResourceWrapper loaded){if(token==generation)model=loaded;}
        });started=System.nanoTime();
    }
    void beginFrame() {
        if(model==null||!model.isLoaded())return;
        if(renderer==null)renderer=new PortraitRenderService(512);
        PortraitProfile p=profileFor(mapping,model.getResourceUrl().getFilePath(),model.getBounds(),
                ((System.nanoTime()-started)/1_000_000_000f*18f)%360f-180f);
        renderer.requestRender(model,p,mapping);renderer.beginRender();
    }
    Texture texture(){return renderer==null||model==null?null:renderer.getTexture();}
}
