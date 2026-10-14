package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.model.ModelDataBounds;
import org.highreshud.client.craft.CraftCatalog;
import org.highreshud.client.portrait.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class HighResCraftPreviewFramingTest {
    public void allCornersFitThroughoutRotationIncludingModelDepth() {
        for(float[] size:new float[][]{{1,1,1},{8,.4f,.4f},{.4f,8,.4f},{.4f,.4f,8},{4,3,2},{.01f,.02f,.03f},{300,40,80}}) {
            ModelDataBounds bounds=new ModelDataBounds(3,5,-7,3+size[0],5+size[1],-7+size[2]);
            for(int yaw=-180;yaw<=180;yaw+=5) {
                PortraitProfile p=HighResCraftPreview.profileFor("model.container.crate.large.birchwood","crate.wom",bounds,yaw);
                double distance=PortraitFraming.baseDistance(bounds,p.mode,1)*p.cameraDistance/p.zoom;
                double tangent=Math.tan(Math.toRadians(p.fov/2)),angle=Math.toRadians(yaw),pitch=Math.toRadians(p.pitch);
                for(int ix:new int[]{-1,1})for(int ih:new int[]{-1,1})for(int iy:new int[]{-1,1}) {
                    double x=ix*size[0]/2,h=ih*size[1]/2,y=iy*size[2]/2;
                    double rx=x*Math.cos(angle)+y*Math.sin(angle),ry=-x*Math.sin(angle)+y*Math.cos(angle);
                    double rh=h*Math.cos(pitch)-ry*Math.sin(pitch),depth=distance-(h*Math.sin(pitch)+ry*Math.cos(pitch));
                    assertTrue("Corner must remain beyond the near plane",depth>.01);
                    assertTrue("Horizontal clipping at yaw "+yaw,Math.abs(rx/(depth*tangent))<=.9001);
                    assertTrue("Vertical clipping at yaw "+yaw,Math.abs(rh/(depth*tangent))<=.9001);
                }
            }
        }
    }
    @Test public void toolPartFallbackKeepsTheHeadInsteadOfTheWholeTool() {
        CraftCatalog.Template head=CraftCatalog.get(123);
        Set<String> mappings=new HashSet<>(Arrays.asList("model.tool.pickaxe","model.tool.pickaxe.blade.iron","model.tool.pickaxe.blade.steel"));
        assertEquals("model.tool.pickaxe.blade.iron",HighResCraftPreview.geometryMappingFor(head,(byte)31,mappings::contains));
        assertEquals("model.tool.pickaxe.blade.steel",HighResCraftPreview.geometryMappingFor(head,(byte)9,mappings::contains));
        assertEquals("model.tool.pickaxe.blade.bronze",HighResCraftPreview.mappingFor(head,(byte)31));
        mappings.add("model.tool.pickaxe.blade.bronze");
        assertEquals("model.tool.pickaxe.blade.bronze",HighResCraftPreview.geometryMappingFor(head,(byte)31,mappings::contains));
        assertEquals("model.container.rack.crate.empty.birchwood",HighResCraftPreview.mappingFor(CraftCatalog.get(1312),(byte)14));
    }
}
