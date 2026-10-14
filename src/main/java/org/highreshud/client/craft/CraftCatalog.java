package org.highreshud.client.craft;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import org.highreshud.client.HudPackagedResources;

/** Factual standard-client metadata; incoming server recipes remain the authority. */
public final class CraftCatalog {
    public static final class Template {
        public int id,icon,grams,material;
        public float difficulty=Float.NaN;
        public String name,plural,model;
        public final Set<Integer> flags=new HashSet<>();
        public boolean metal(){return flags.contains(22);}
        public boolean wood(){return flags.contains(21);}
        public boolean combine(){return flags.contains(146);}
    }
    public static final class Entry {
        public int result,source,target,sourceGrams,targetGrams,sourceMaterial,targetMaterial,skill;
        public boolean consumeSource,consumeTarget,advanced,destroyBoth;
        public float percentageLost;
        public String category="";
        public int totalSteps=1;
    }
    private static final Map<Integer,Template> TEMPLATES=new LinkedHashMap<>();
    private static final List<Entry> ENTRIES=new ArrayList<>();
    private static final Map<String,List<Template>> BY_NAME=new HashMap<>();
    private static final Map<Integer,List<Entry>> BY_RESULT=new HashMap<>();
    private static final Pattern PARENTHESES=Pattern.compile("\\s*\\([^)]*\\)");
    private static final Pattern MATERIAL_SUFFIX=Pattern.compile(",.*$");
    private static final Pattern STATE_PREFIX=Pattern.compile("^(unfinished |incomplete |planned |rare |supreme |fantastic )+");
    private static final Pattern MATERIAL_PREFIX=Pattern.compile("^(birchwood|pinewood|oakenwood|cedarwood|willowwood|maplewood|applewood|cherrywood|olivewood|lemonwood|beechwood|walnutwood|chestnutwood|firwood|lindenwood|orangewood|iron|steel|copper|tin|zinc|lead|gold|silver|brass|bronze|adamantine|glimmersteel|seryll|electrum|birch|pine|oak|cedar|willow|maple|beech|walnut|chestnut|fir|linden|wooden|stone|cotton|wool|leather) ");
    static {
        try {
            for(String line:lines("templates.tsv")) {
                String[] p=line.split("\t",-1);if(p.length<8)continue;
                Template t=new Template();t.id=Integer.parseInt(p[0]);t.name=p[1];t.plural=p[2];
                t.icon=Integer.parseInt(p[3]);t.grams=Integer.parseInt(p[4]);t.material=Integer.parseInt(p[5]);t.model=p[6];
                if(p.length>8)t.difficulty=Float.parseFloat(p[8]);
                for(String f:p[7].split(","))if(!f.isEmpty())t.flags.add(Integer.parseInt(f));TEMPLATES.put(t.id,t);
            }
            for(String line:lines("entries.tsv")) {
                String[] p=line.split("\t",-1);if(p.length<12)continue;
                Entry e=new Entry();e.result=Integer.parseInt(p[0]);e.source=Integer.parseInt(p[1]);e.target=Integer.parseInt(p[2]);
                e.consumeSource=p[3].equals("1");e.consumeTarget=p[4].equals("1");
                e.sourceGrams=Integer.parseInt(p[5]);e.targetGrams=Integer.parseInt(p[6]);e.sourceMaterial=Integer.parseInt(p[7]);e.targetMaterial=Integer.parseInt(p[8]);e.skill=Integer.parseInt(p[9]);e.advanced=p[10].equals("1");
                e.percentageLost=p.length>14?Float.parseFloat(p[14]):0f;
                e.destroyBoth=p.length>12&&p[12].equals("1");
                if(p.length>13)e.category=p[13];
                e.totalSteps=(e.consumeSource&&e.consumeTarget||e.advanced&&e.destroyBoth)?2:1;
                for(String r:p[11].split(";"))if(!r.isEmpty()){String[] v=r.split(",");e.totalSteps+=Integer.parseInt(v[2]);}
                ENTRIES.add(e);
            }
            for(Template t:TEMPLATES.values()) {
                String name=clean(t.name);
                for(String key:Arrays.asList(name,clean(t.plural),"small "+name,"large "+name)) {
                    List<Template> candidates=BY_NAME.computeIfAbsent(key,ignored->new ArrayList<>());
                    if(!candidates.contains(t))candidates.add(t);
                }
            }
            for(Entry e:ENTRIES)BY_RESULT.computeIfAbsent(e.result,ignored->new ArrayList<>()).add(e);
        }catch(IOException|RuntimeException failure){throw new ExceptionInInitializerError(failure);}
    }
    private static List<String> lines(String file)throws IOException {
        List<String> out=new ArrayList<>();
        try(InputStream input=HudPackagedResources.open("org/highreshud/client/craft/"+file)) {
            if(input==null)throw new FileNotFoundException(file);
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8))) {
                for(String s;(s=reader.readLine())!=null;)out.add(s);
            }
        }return out;
    }
    public static Template get(int id){return TEMPLATES.get(id);}
    public static boolean carriesPartRarity(Template component){
        if(component==null)return false;
        // CreationMatrix prefers an advanced entry, then the first simple entry.
        Entry first=null;for(Entry e:BY_RESULT.getOrDefault(component.id,Collections.emptyList())){if(first==null)first=e;if(e.advanced){first=e;break;}}
        if(first!=null)return first.category.equals("WEAPON_HEADS")||first.category.equals("TOOL_PARTS")||first.category.equals("BLADES");
        return false;
    }
    public static String clean(String name) {
        if(name==null)return "";
        String s=PARENTHESES.matcher(name.toLowerCase(Locale.ROOT).trim()).replaceAll("");
        s=MATERIAL_SUFFIX.matcher(s).replaceFirst("");s=STATE_PREFIX.matcher(s).replaceFirst("");
        return MATERIAL_PREFIX.matcher(s).replaceFirst("").trim();
    }
    public static Template find(String name,int icon) {
        String key=clean(name);Template best=null;
        for(Template t:BY_NAME.getOrDefault(key,Collections.emptyList())) {
                if(key.startsWith("small ")&&!t.model.contains(".small."))continue;
                if(key.startsWith("large ")&&!t.model.contains(".large."))continue;
                if(t.icon==icon) {
                    String raw=name.toLowerCase(Locale.ROOT);String mat=com.wurmonline.shared.util.MaterialUtilities.getMaterialString((byte)t.material);
                    if(raw.startsWith(mat+" ")||raw.endsWith(", "+mat)||raw.endsWith(","+mat))return t;
                    if(best==null||best.icon!=icon)best=t;
                    continue;
                }
                if(best==null)best=t;
        }return best;
    }
    public static Entry entry(Template result,Template source,Template target) {
        if(result==null||source==null||target==null)return null;
        List<Entry> candidates=BY_RESULT.getOrDefault(result.id,Collections.emptyList());
        for(Entry e:candidates)if(e.source==source.id&&e.target==target.id)return e;
        // The vanilla catalogue expands metal lump templates into material variants.
        for(Entry e:candidates)if(sameFamily(get(e.source),source)&&sameFamily(get(e.target),target))return e;
        return null;
    }
    private static boolean sameFamily(Template a,Template b){return a!=null&&b!=null&&clean(a.name).equals(clean(b.name))&&(a.icon==b.icon||a.metal()&&b.metal()&&clean(a.name).equals("lump"));}
    public static float startKilograms(Entry e,Template ingredient,Template result,boolean source) {
        if(ingredient==null)return Float.NaN;
        if(e==null)return ingredient.grams/1000f;
        int custom=source?e.sourceGrams:e.targetGrams;if(custom>=0)return custom/1000f;
        boolean consumed=e.destroyBoth||(source?e.consumeSource:e.consumeTarget);
        if(!consumed)return 0f;
        if(!e.advanced&&ingredient.combine()&&!e.destroyBoth&&!(e.consumeSource&&e.consumeTarget)&&result!=null)
            return (result.grams+(int)(result.grams*Math.max(0f,e.percentageLost)/100f))/1000f;
        return ingredient.grams/1000f;
    }
}
