package com.wurmonline.client.renderer.gui;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import org.highreshud.client.HighResWindowsSettings;

/** Explicit one-shot recipe export; disabled unless an external path is configured. */
final class HighResCatalogExport {
    private static final Logger LOG=Logger.getLogger("HighResHud.Catalog");
    private static HeadsUpDisplay seen;
    private static boolean done;
    private HighResCatalogExport(){}
    static void tick(HeadsUpDisplay hud) {
        String destination=HighResWindowsSettings.catalogExport;
        if(destination.isEmpty())return;
        if(seen!=hud){seen=hud;done=false;}
        if(done)return;
        CreationListWindow window=hud.getCreationListWindow();if(window==null)return;
        @SuppressWarnings("unchecked") List<CreationListItem> recipes=(List<CreationListItem>)HighResWindowAccess.get(window,"createItemList");
        int expected=((Number)HighResWindowAccess.get(window,"numberofItemsToRebuild")).intValue();
        if(expected<=0||recipes.size()<expected||((Number)HighResWindowAccess.get(window,"itemCount")).intValue()!=0)return;
        done=true;
        try {
            Path output=Paths.get(destination).toAbsolutePath().normalize();
            Path gameDirectory=Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
            if(output.startsWith(gameDirectory))throw new IOException("Recipe export must be outside the game directory");
            StringBuilder text=new StringBuilder("# wurm-catalog-capture-v1\n# capturedUtc="+java.time.Instant.now()+"\n");
            text.append("# serverName64=").append(encode(hud.getWorld().getServerName())).append('\n');
            Object connection=hud.getWorld().getServerConnection();
            byte[] address=(byte[])HighResWindowAccess.get(connection,"serverIp");
            if(address!=null)text.append("# serverIp=").append(java.net.InetAddress.getByAddress(address).getHostAddress()).append('\n');
            text.append("# recipes=").append(recipes.size()).append("\n# expected=").append(expected).append("\n# receiving=0\n");
            for(CreationListItem recipe:recipes)append(text,recipe,0);
            String snapshot=text.toString();int count=recipes.size();
            Thread writer=new Thread(()->{
                try{Files.createDirectories(output.getParent());Files.write(output,snapshot.getBytes(StandardCharsets.UTF_8));LOG.info("Exported "+count+" server recipes to "+output);}
                catch(IOException failure){LOG.log(java.util.logging.Level.WARNING,"Recipe export failed",failure);}
            },"highres.catalog.export");writer.setDaemon(true);writer.start();
        }catch(IOException|RuntimeException failure){LOG.log(java.util.logging.Level.WARNING,"Recipe export failed",failure);}
    }
    static String encode(String value){return Base64.getEncoder().encodeToString((value==null?"":value).getBytes(StandardCharsets.UTF_8));}
    static void append(StringBuilder out,CreationListItem item,int depth) {
        out.append("I\t").append(depth).append('\t').append(encode(item.getName())).append('\t').append(encode(item.getSkill())).append('\t')
                .append(HighResWindowAccess.get(item,"numberOfitems")).append('\t').append(HighResWindowAccess.get(item,"iconId")).append('\t')
                .append(HighResWindowAccess.get(item,"categoryId")).append('\t').append(HighResWindowAccess.get(item,"isCategory")).append('\n');
        for(CreationListItem child:item.getChilds())append(out,child,depth+1);
    }
}
