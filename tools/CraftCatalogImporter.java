package com.wurmonline.client.renderer.gui;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Offline conversion; no running client or server connection is needed. */
public final class CraftCatalogImporter {
    private static String decode(String value){return new String(Base64.getDecoder().decode(value),StandardCharsets.UTF_8);}
    private static String encode(String value){return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));}
    private static void nativeFields(PrintWriter output,CreationListItem item){
        output.print(encode(item.getName())+"\t"+encode(item.getSkill())+"\t"+HighResWindowAccess.get(item,"numberOfitems")+"\t"+HighResWindowAccess.get(item,"iconId")+"\t"+HighResWindowAccess.get(item,"categoryId")+"\t"+HighResWindowAccess.get(item,"isCategory"));
    }
    public static void main(String[] arguments) throws Exception {
        Path input=Paths.get(arguments[0]),output=Paths.get(arguments[1]);
        List<CreationListItem> roots=new ArrayList<>(),parents=new ArrayList<>();
        List<String> metadata=new ArrayList<>();int expected=-1,receiving=-1,declared=-1;
        for(String line:Files.readAllLines(input,StandardCharsets.UTF_8)) {
            if(line.startsWith("#")){metadata.add(line);if(line.startsWith("# expected="))expected=Integer.parseInt(line.substring(11));if(line.startsWith("# receiving="))receiving=Integer.parseInt(line.substring(12));if(line.startsWith("# recipes="))declared=Integer.parseInt(line.substring(10));continue;}
            if(line.isEmpty())continue;String[] fields=line.split("\t",-1);
            if(fields.length!=8||!fields[0].equals("I"))throw new IOException("Invalid capture row");
            int depth=Integer.parseInt(fields[1]);CreationListItem item=new CreationListItem(decode(fields[2]),decode(fields[3]),Integer.parseInt(fields[4]),Short.parseShort(fields[5]),Short.parseShort(fields[6]),Boolean.parseBoolean(fields[7]));
            if(depth==0)roots.add(item);else{if(depth>parents.size())throw new IOException("Invalid hierarchy");parents.get(depth-1).addChild(item);}
            while(parents.size()>depth)parents.remove(parents.size()-1);parents.add(item);
        }
        if(receiving!=0||expected<=0||roots.size()<expected||roots.size()!=declared)throw new IOException("Incomplete server capture");
        Files.createDirectories(output.toAbsolutePath().getParent());int known=0,requirements=0;
        try(PrintWriter writer=new PrintWriter(Files.newBufferedWriter(output,StandardCharsets.UTF_8))) {
            writer.println("# sklotopolis-processed-recipes-v1");for(String line:metadata)if(!line.equals("# wurm-catalog-capture-v1"))writer.println(line);
            for(CreationListItem item:roots) {
                HighResCraftRecipe recipe=new HighResCraftRecipe(item);if(recipe.result!=null)known++;
                writer.print("R\t");nativeFields(writer,item);writer.println("\t"+(recipe.result==null?-1:recipe.result.id)+"\t"+(recipe.entry==null?-1:recipe.entry.source)+"\t"+(recipe.entry==null?-1:recipe.entry.target)+"\t"+encode(recipe.displayName()));
                for(int i=0;i<recipe.requirements.size();i++) {
                    HighResCraftRecipe.Requirement requirement=recipe.requirements.get(i);writer.print("C\t");nativeFields(writer,item.getChilds().get(i));
                    writer.println("\t"+(requirement.template==null?-1:requirement.template.id)+"\t"+requirement.count+"\t"+requirement.unitKg+"\t"+requirement.start+"\t"+requirement.tool+"\t"+requirement.materialId);requirements++;
                }
            }
            if(writer.checkError())throw new IOException("Catalog output failed");
        }
        System.out.println("Processed recipes="+roots.size()+" knownTemplate="+known+" requirements="+requirements+" output="+output);
    }
}
