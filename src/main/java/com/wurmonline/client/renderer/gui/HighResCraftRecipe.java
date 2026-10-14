package com.wurmonline.client.renderer.gui;

import java.util.*;
import org.highreshud.client.craft.CraftCatalog;
import com.wurmonline.client.game.inventory.InventoryMetaItem;

final class HighResCraftRecipe {
    final CreationListItem nativeItem;
    final String name;
    final CraftCatalog.Template result;
    final List<Requirement> requirements=new ArrayList<>();
    final CraftCatalog.Entry entry;
    final List<HighResCraftRecipe> variants=new ArrayList<>();
    private final String searchText;
    private final String nameSearchText,componentSearchText;
    private final String caption;
    String displayName(){return caption;}
    HighResCraftRecipe(CreationListItem item) {
        nativeItem=item;name=item.getName();result=CraftCatalog.find(name,icon(item));
        caption=result!=null&&result.metal()?CraftCatalog.clean(name):name;
        List<CreationListItem> children=item.getChilds();
        CraftCatalog.Template source=children.size()>0?CraftCatalog.find(children.get(0).getName(),icon(children.get(0))):null;
        CraftCatalog.Template target=children.size()>1?CraftCatalog.find(children.get(1).getName(),icon(children.get(1))):null;
        entry=CraftCatalog.entry(result,source,target);
        for(int i=0;i<children.size();i++) {
            CreationListItem child=children.get(i);CraftCatalog.Template template=i==0?source:i==1?target:CraftCatalog.find(child.getName(),icon(child));
            int count=i<2?1:Math.max(1,((Number)HighResWindowAccess.get(child,"numberOfitems")).intValue());
            boolean tool=i<2&&entry!=null&&!entry.destroyBoth&&!(i==0?entry.consumeSource:entry.consumeTarget);
            if(i<2&&entry==null&&template!=null&&(template.flags.contains(38)||template.flags.contains(15)))tool=true;
            // Advanced entries mark both starters as undepleted; resource starters are still consumed.
            if(entry!=null&&entry.advanced&&template!=null&&!template.flags.contains(38)&&!template.flags.contains(15))tool=false;
            float unit=i<2?CraftCatalog.startKilograms(entry,template,result,i==0):template==null?Float.NaN:template.grams/1000f;
            if(tool)unit=0f;
            if(!tool&&unit==0&&template!=null)unit=template.grams/1000f;
            requirements.add(new Requirement(child.getName(),icon(child),template,count,unit,i<2,tool));
        }
        nameSearchText=name.toLowerCase(Locale.ROOT);
        StringBuilder components=new StringBuilder();for(Requirement r:requirements)components.append(' ').append(r.name.toLowerCase(Locale.ROOT));
        componentSearchText=components.toString();searchText=nameSearchText+componentSearchText;
    }
    HighResCraftRecipe(CreationListItem item,CraftCatalog.Template result,CraftCatalog.Entry entry,List<Requirement> requirements,String caption) {
        nativeItem=item;name=item.getName();this.result=result;this.entry=entry;this.caption=caption;this.requirements.addAll(requirements);
        nameSearchText=name.toLowerCase(Locale.ROOT);StringBuilder components=new StringBuilder();for(Requirement requirement:requirements)components.append(' ').append(requirement.name.toLowerCase(Locale.ROOT));componentSearchText=components.toString();searchText=nameSearchText+componentSearchText;
    }
    static int icon(CreationListItem item){return ((Number)HighResWindowAccess.get(item,"iconId")).intValue();}
    static final class Requirement {
        final String name;final int icon;final CraftCatalog.Template template;final int materialId;
        int count;final float unitKg;final boolean start,tool;
        Requirement(String name,int icon,CraftCatalog.Template template,int count,float unit,boolean start,boolean tool){this(name,icon,template,count,unit,start,tool,namedMaterial(name,template));}
        Requirement(String name,int icon,CraftCatalog.Template template,int count,float unit,boolean start,boolean tool,int materialId){this.name=name;this.icon=icon;this.template=template;this.count=count;unitKg=unit;this.start=start;this.tool=tool;this.materialId=materialId;}
        static int namedMaterial(String name,CraftCatalog.Template template){String raw=name.toLowerCase(Locale.ROOT);for(int id=1;id<128;id++){String mat=com.wurmonline.shared.util.MaterialUtilities.getMaterialString((byte)id);if(raw.startsWith(mat+" ")||raw.endsWith(", "+mat)||raw.endsWith(","+mat))return id;}return template==null?0:template.material;}
        float totalKg(){return (tool&&template!=null?template.grams/1000f:unitKg)*count;}
        boolean location(){String key=CraftCatalog.clean(name);return template==null&&(key.equals("tile border")||key.equals("tile")||key.equals("tile corner")||key.equals("floor")||key.equals("wall")||key.equals("bridge part"));}
        boolean matches(InventoryMetaItem item){return item.getType()==icon&&CraftCatalog.clean(item.getBaseName()).equals(CraftCatalog.clean(name))||CraftCatalog.clean(item.getBaseName()).equals(CraftCatalog.clean(name));}
        boolean glowing(HighResCraftRecipe recipe) {
            if(tool||template==null||!template.metal()||recipe==null||recipe.entry==null)return false;
            if(!start)return false; // Continue uses the server's remaining component protocol, not a new smelting action.
            return recipe.entry.skill==10041||recipe.requirements.size()>1&&recipe.requirements.get(0).template!=null&&recipe.requirements.get(1).template!=null&&recipe.requirements.get(0).template.metal()&&recipe.requirements.get(1).template.metal();
        }
    }
    boolean matchesQuery(String query) {
        return matchesWords(query.toLowerCase(Locale.ROOT).trim().split("\\s+"));
    }
    boolean matchesName(String query){return nameSearchText.contains(query);}
    boolean matchesMaterial(String query){return componentSearchText.contains(query);}
    boolean matchesWords(String[] words) {
        for(String word:words)if(!searchText.contains(word))return false;
        return true;
    }
}
