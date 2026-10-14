package com.wurmonline.client.renderer.gui;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.highreshud.client.HudPackagedResources;
import org.highreshud.client.craft.CraftCatalog;

/** Manually updated Sklotopolis snapshot, already resolved by the offline importer. */
final class HighResBundledRecipes {
    final List<CreationListItem> items;
    final Map<CreationListItem,HighResCraftRecipe> recipes;
    private HighResBundledRecipes(List<CreationListItem> items,Map<CreationListItem,HighResCraftRecipe> recipes){this.items=Collections.unmodifiableList(items);this.recipes=Collections.unmodifiableMap(recipes);}
    private static final class Holder {static final HighResBundledRecipes INSTANCE=load();}
    static HighResBundledRecipes instance(){return Holder.INSTANCE;}
    private static HighResBundledRecipes load() {
        try(InputStream input=HudPackagedResources.open("org/highreshud/client/craft/sklotopolis-recipes.tsv")){if(input==null)throw new FileNotFoundException("sklotopolis-recipes.tsv");return read(input);}
        catch(IOException|RuntimeException failure){throw new IllegalStateException("Invalid bundled recipe snapshot",failure);}
    }
    static HighResBundledRecipes read(InputStream input) throws IOException {
        List<CreationListItem> items=new ArrayList<>();Map<CreationListItem,HighResCraftRecipe> recipes=new IdentityHashMap<>();
        CreationListItem root=null;String caption=null;CraftCatalog.Template result=null;CraftCatalog.Entry entry=null;
        List<HighResCraftRecipe.Requirement> requirements=new ArrayList<>();
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8))) {
            if(!"# sklotopolis-processed-recipes-v1".equals(reader.readLine()))throw new IOException("Unsupported recipe snapshot format");
            for(String line;(line=reader.readLine())!=null;){if(line.isEmpty()||line.startsWith("#"))continue;String[] columns=line.split("\t",-1);
                if(columns[0].equals("R")&&columns.length==11) {
                    if(root!=null)recipes.put(root,new HighResCraftRecipe(root,result,entry,requirements,caption));
                    root=item(columns);items.add(root);requirements=new ArrayList<>();result=CraftCatalog.get(Integer.parseInt(columns[7]));
                    entry=CraftCatalog.entry(result,CraftCatalog.get(Integer.parseInt(columns[8])),CraftCatalog.get(Integer.parseInt(columns[9])));caption=decode(columns[10]);
                } else if(columns[0].equals("C")&&columns.length==13&&root!=null) {
                    CreationListItem child=item(columns);root.addChild(child);
                    requirements.add(new HighResCraftRecipe.Requirement(child.getName(),HighResCraftRecipe.icon(child),CraftCatalog.get(Integer.parseInt(columns[7])),Integer.parseInt(columns[8]),Float.parseFloat(columns[9]),Boolean.parseBoolean(columns[10]),Boolean.parseBoolean(columns[11]),Integer.parseInt(columns[12])));
                } else throw new IOException("Invalid recipe snapshot row");
            }
            if(root!=null)recipes.put(root,new HighResCraftRecipe(root,result,entry,requirements,caption));
        }
        return new HighResBundledRecipes(items,recipes);
    }
    private static CreationListItem item(String[] fields){return new CreationListItem(decode(fields[1]),decode(fields[2]),Integer.parseInt(fields[3]),Short.parseShort(fields[4]),Short.parseShort(fields[5]),Boolean.parseBoolean(fields[6]));}
    private static String decode(String value){return new String(Base64.getDecoder().decode(value),StandardCharsets.UTF_8);}
}
