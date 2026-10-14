package org.highreshud.client.craft;

import java.lang.reflect.Field;
import java.util.*;
import com.wurmonline.shared.util.MaterialUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class CraftCatalogIndexTest {
    @SuppressWarnings("unchecked") private static List<CraftCatalog.Template> templates() throws Exception {
        Field field=CraftCatalog.class.getDeclaredField("TEMPLATES");field.setAccessible(true);
        return new ArrayList<>(((Map<Integer,CraftCatalog.Template>)field.get(null)).values());
    }
    @Test public void indexPreservesFullCatalogueNameIconAndMaterialPreference() throws Exception {
        List<CraftCatalog.Template> templates=templates();Map<Integer,String[]> names=new HashMap<>();
        for(CraftCatalog.Template template:templates)names.put(template.id,new String[]{CraftCatalog.clean(template.name),CraftCatalog.clean(template.plural)});
        for(CraftCatalog.Template template:templates)for(String name:new String[]{template.name,template.plural,template.name+", "+MaterialUtilities.getMaterialString((byte)template.material)}) {
            assertSame(name,referenceFind(name,template.icon,templates,names),CraftCatalog.find(name,template.icon));
            assertSame(name,referenceFind(name,-1,templates,names),CraftCatalog.find(name,-1));
        }
    }
    private static CraftCatalog.Template referenceFind(String name,int icon,List<CraftCatalog.Template> templates,Map<Integer,String[]> names) {
        String key=CraftCatalog.clean(name);CraftCatalog.Template best=null;
        for(CraftCatalog.Template template:templates) {
            String[] normalized=names.get(template.id);
            if(!key.equals(normalized[0])&&!key.equals(normalized[1])&&!key.equals("small "+normalized[0])&&!key.equals("large "+normalized[0]))continue;
            if(key.startsWith("small ")&&!template.model.contains(".small."))continue;
            if(key.startsWith("large ")&&!template.model.contains(".large."))continue;
            if(template.icon==icon) {
                String raw=name.toLowerCase(Locale.ROOT),material=MaterialUtilities.getMaterialString((byte)template.material);
                if(raw.startsWith(material+" ")||raw.endsWith(", "+material)||raw.endsWith(","+material))return template;
                if(best==null||best.icon!=icon)best=template;
            } else if(best==null)best=template;
        }
        return best;
    }
    @Test(timeout=5000) public void unknownServerRecipesDoNotScanEveryStandardTemplate() {
        CraftCatalog.get(850);
        for(int n=0;n<20_000;n++)assertNull(CraftCatalog.find("custom server recipe "+n,60));
    }
}
