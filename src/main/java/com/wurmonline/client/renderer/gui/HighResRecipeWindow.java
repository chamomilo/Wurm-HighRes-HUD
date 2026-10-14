package com.wurmonline.client.renderer.gui;

import java.util.*;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.PickData;
import com.wurmonline.client.renderer.gui.text.HudTextFonts;
import com.wurmonline.client.renderer.gui.text.TextFont;
import org.chamomilo.wurm.ui.v1.*;
import org.highreshud.client.i18n.HudMessages;
import org.highreshud.client.ui.HudCaptionGroup;
import org.highreshud.client.craft.CraftCatalog;

/** Recipe rows project the server's native catalogue; filtering examines every component. */
final class HighResRecipeWindow extends HighResNativeWindow {
    enum SearchScope { NAME, MATERIAL, BOTH }
    final Browser browser;
    final HighResBundledRecipes bundled;
    HighResRecipeWindow(CreationListWindow window,HeadsUpDisplay hud){
        this(window,hud,HighResBundledRecipes.instance());
    }
    HighResRecipeWindow(CreationListWindow window,HeadsUpDisplay hud,HighResBundledRecipes bundled){super("highres.recipes","Recipe browser",window,hud);this.bundled=bundled;browser=new Browser();}
    @Override void install(){window.setComponent(browser);super.install();window.setSize(Math.max(660,window.width),Math.max(420,window.height));}
    @Override void tick(){header.refresh();if(!window.minimized&&owner.getComponents().contains(window))browser.refresh();}
    @Override void close(){window.closePressed();}
    @SuppressWarnings("unchecked") List<CreationListItem> items(){return bundled!=null?bundled.items:(List<CreationListItem>)HighResWindowAccess.get(window,"createItemList");}
    final class Browser extends FlexComponent {
        final ChamomiloUiV1TextField filter;
        final Rows rows=new Rows();final ChamomiloUiV1ScrollPanel scroll;
        static final int BATCH_SIZE=16;
        String query="",appliedQuery=null;
        SearchScope scope=SearchScope.NAME;
        final String[] scopeCaptions={"Search in name","Search in material","Search in both"};
        final HighResHudActionButton[] scopeButtons=new HighResHudActionButton[3];
        WurmComponent pressedControl;
        final Set<String> expanded=new HashSet<>(),collapsedSearch=new HashSet<>();
        String selectedKey="";
        final Map<Integer,List<HighResCraftRecipe>> byTemplate=new HashMap<>();
        final Map<String,List<HighResCraftRecipe>> byName=new HashMap<>();
        final Map<Short,String> categoryNames=new HashMap<>();
        boolean categoryNamesReady;
        List<CreationListItem> snapshot=Collections.emptyList();
        final Map<CreationListItem,HighResCraftRecipe> cache=new IdentityHashMap<>();
        final Map<String,HighResCraftRecipe> unique=new LinkedHashMap<>();
        final List<HighResCraftRecipe> catalog=new ArrayList<>();
        int cursor;
        Browser(){super("highres.recipes.browser",0,0,660,380);
            filter=new ChamomiloUiV1TextField("highres.recipes.filter",300,160,this::filterChanged,s->{});filter.parent=this;
            scroll=new ChamomiloUiV1ScrollPanel("highres.recipes.scroll",rows,false,true);scroll.parent=this;autoHide(scroll);
            HudCaptionGroup searchGroup=new HudCaptionGroup(200,32,UiScale.BASE,18,scopeCaptions);
            for(int i=0;i<scopeButtons.length;i++){final int index=i;scopeButtons[i]=new HighResHudActionButton(this,200,32,searchGroup,scopeCaptions[i],()->scopeChanged(SearchScope.values()[index]));}
            if(bundled!=null){snapshot=bundled.items;for(CreationListItem item:snapshot)add(resolve(item));cursor=snapshot.size();finishCatalog();}
        }
        void filterChanged(String value){query=value;collapsedSearch.clear();refresh();scroll.scrollTo(0,0);}
        void scopeChanged(SearchScope next){scope=next;appliedQuery=null;collapsedSearch.clear();refresh();scroll.scrollTo(0,0);}
        boolean loading(){return cursor<snapshot.size();}
        HighResCraftRecipe resolve(CreationListItem item){return cache.computeIfAbsent(item,key->bundled!=null?bundled.recipes.get(key):new HighResCraftRecipe(key));}
        private void add(HighResCraftRecipe recipe){String key=(recipe.result!=null&&recipe.result.metal()?recipe.displayName():recipe.name)+":"+(recipe.result==null?-1:recipe.result.id);HighResCraftRecipe canonical=unique.get(key);if(canonical==null){canonical=recipe;canonical.variants.clear();unique.put(key,canonical);}canonical.variants.add(recipe);}
        private void finishCatalog(){
            catalog.addAll(unique.values());catalog.sort(Comparator.comparing(r->r.displayName().toLowerCase(Locale.ROOT)));
            byTemplate.clear();byName.clear();
            for(HighResCraftRecipe recipe:catalog){if(recipe.result!=null)byTemplate.computeIfAbsent(recipe.result.id,k->new ArrayList<>()).add(recipe);byName.computeIfAbsent(CraftCatalog.clean(recipe.name),k->new ArrayList<>()).add(recipe);}
        }
        HighResCraftRecipe componentRecipe(HighResCraftRecipe.Requirement component){
            List<HighResCraftRecipe> candidates=component.template==null?null:byTemplate.get(component.template.id);
            if(candidates==null||candidates.isEmpty())candidates=byName.get(CraftCatalog.clean(component.name));
            if(candidates==null||candidates.isEmpty())return null;
            for(HighResCraftRecipe recipe:candidates)if(CraftCatalog.clean(recipe.name).equals(CraftCatalog.clean(component.name)))return recipe;
            return candidates.get(0);
        }
        @SuppressWarnings("unchecked") private boolean loadCategoryNames(){
            if(categoryNamesReady||!owner.getComponents().contains(window))return false;
            Map<Short,CreationListItem> nativeNames=(Map<Short,CreationListItem>)HighResWindowAccess.get(window,"categoryMap");
            if(nativeNames==null||nativeNames.isEmpty())return false;
            for(Map.Entry<Short,CreationListItem> entry:nativeNames.entrySet())categoryNames.put(entry.getKey(),entry.getValue().getName());
            categoryNamesReady=true;return true;
        }
        String categoryName(short id,List<HighResCraftRecipe> recipes){
            String name=categoryNames.get(id);if(name!=null)return name;
            for(HighResCraftRecipe recipe:recipes)if(recipe.entry!=null&&!recipe.entry.category.isEmpty()){
                StringBuilder label=new StringBuilder();for(String word:recipe.entry.category.toLowerCase(Locale.ROOT).split("_")){if(label.length()>0)label.append(' ');label.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));}return label.toString();
            }
            return recipes.isEmpty()?HudMessages.tr("Other recipes"):recipes.get(0).nativeItem.getSkill();
        }
        boolean expanded(String key,boolean category){return expanded.contains(key)||category&&!query.trim().isEmpty()&&!collapsedSearch.contains(key);}
        void toggle(TreeRow row){
            selectedKey=row.key;
            if(row.expandable){if(expanded(row.key,row.category)){expanded.remove(row.key);if(row.category)collapsedSearch.add(row.key);}else{expanded.add(row.key);collapsedSearch.remove(row.key);}rows.rebuild();}
        }
        void refresh(){
            boolean labelsChanged=loadCategoryNames();
            List<CreationListItem> incoming=items();
            if(!incoming.equals(snapshot)) {
                snapshot=new ArrayList<>(incoming);cache.keySet().retainAll(new HashSet<>(snapshot));
                cursor=0;unique.clear();catalog.clear();rows.list.clear();rows.visible.clear();appliedQuery=null;
            }
            if(loading()) {
                int end=Math.min(snapshot.size(),cursor+BATCH_SIZE);
                while(cursor<end){CreationListItem item=snapshot.get(cursor++);
                    HighResCraftRecipe recipe=resolve(item);
                    add(recipe);
                }
                if(!loading())finishCatalog();
            }
            if(!loading()&&!query.equals(appliedQuery)) {
                appliedQuery=query;rows.list.clear();String[] words=query.toLowerCase(Locale.ROOT).trim().split("\\s+");
                String needle=query.toLowerCase(Locale.ROOT).trim();
                for(HighResCraftRecipe recipe:catalog){boolean match=false;for(HighResCraftRecipe variant:recipe.variants)if(scope==SearchScope.NAME?variant.matchesName(needle):scope==SearchScope.MATERIAL?variant.matchesMaterial(needle):variant.matchesWords(words)){match=true;break;}if(match)rows.list.add(recipe);}
                rows.rebuild();
            }
            if(labelsChanged)rows.rebuild();
            filter.setPlaceholder(HudMessages.tr(scope==SearchScope.NAME?"Filter recipe name":scope==SearchScope.MATERIAL?"Filter component name":"Filter recipe or component"));componentResized();
        }
        @Override void componentResized(){
            filter.setLocation(x+10,y+6,Math.max(180,width-20),32);
            int buttonWidth=Math.max(200,(width-32)/3);for(int i=0;i<scopeButtons.length;i++)scopeButtons[i].setLocation(x+10+i*(buttonWidth+6),y+44,buttonWidth,32);
            scroll.setLocation(x+8,y+102,Math.max(1,width-16),Math.max(1,height-110));scroll.layout();
        }
        @Override public FlexComponent getComponentAt(int px,int py){if(!contains(px,py))return null;if(filter.contains(px,py))return filter.getComponentAt(px,py);if(scroll.contains(px,py))return scroll.getComponentAt(px,py);return this;}
        @Override protected void leftPressed(int px,int py,int clicks){pressedControl=null;for(WurmComponent button:scopeButtons)if(button.contains(px,py)){pressedControl=button;button.leftPressed(px,py,clicks);break;}}
        @Override protected void mouseDragged(int px,int py){if(pressedControl!=null)pressedControl.mouseDragged(px,py);}
        @Override protected void leftReleased(int px,int py){WurmComponent button=pressedControl;pressedControl=null;if(button!=null)button.leftReleased(px,py);}
        @Override protected void mouseMoved(int px,int py){for(WurmComponent button:scopeButtons)if(button.contains(px,py))button.mouseMoved(px,py);else button.mouseExited();}
        @Override protected void mouseExited(){for(WurmComponent button:scopeButtons)button.mouseExited();pressedControl=null;}
        @Override public void pick(PickData pick,int px,int py){for(int i=0;i<scopeButtons.length;i++)if(scopeButtons[i].contains(px,py)){pick.addText(HudMessages.tr(scopeCaptions[i]));return;}}
        @Override protected void renderComponent(Queue queue,float ignored){componentResized();filter.render(queue,1f);for(int i=0;i<scopeButtons.length;i++){scopeButtons[i].active(i==scope.ordinal());scopeButtons[i].render(queue,1f);}draw(queue,HudMessages.tr("Recipe / components"),x+14,y+93,width-28,UiColor.TEXT,true);scroll.render(queue,1f);}
    }
    static final class TreeRow {
        final String key,label;final int depth;final boolean category,expandable;
        final HighResCraftRecipe recipe;final HighResCraftRecipe.Requirement component;
        TreeRow(String key,String label,int depth,boolean category,boolean expandable,HighResCraftRecipe recipe,HighResCraftRecipe.Requirement component){this.key=key;this.label=label;this.depth=depth;this.category=category;this.expandable=expandable;this.recipe=recipe;this.component=component;}
    }
    final class Rows extends FlexComponent {
        final List<HighResCraftRecipe> list=new ArrayList<>();final List<TreeRow> visible=new ArrayList<>();String pressed="";int clicks,downX,downY;boolean armed;
        Rows(){super("highres.recipes.rows",0,0,600,1);sizeFlags=FIXED_HEIGHT;}
        TreeRow at(int py){int index=(py-y)/52;return py<y||index>=visible.size()?null:visible.get(index);}
        void rebuild(){
            visible.clear();Map<Short,List<HighResCraftRecipe>> groups=new LinkedHashMap<>();for(HighResCraftRecipe recipe:list)groups.computeIfAbsent(recipe.nativeItem.getCategoryId(),k->new ArrayList<>()).add(recipe);
            List<Short> ids=new ArrayList<>(groups.keySet());ids.sort(Comparator.comparing(id->browser.categoryName(id,groups.get(id)).toLowerCase(Locale.ROOT)));
            for(short id:ids){String key="category:"+id;visible.add(new TreeRow(key,browser.categoryName(id,groups.get(id))+" ("+groups.get(id).size()+")",0,true,true,null,null));
                if(browser.expanded(key,true))for(HighResCraftRecipe recipe:groups.get(id))appendRecipe(recipe,key+"/"+recipe.name+":"+(recipe.result==null?-1:recipe.result.id),1,new HashSet<HighResCraftRecipe>());
            }
            resizeContent(this,Math.max(1,browser.scroll.width-20),Math.max(1,visible.size()*52));browser.scroll.layout();
        }
        void appendRecipe(HighResCraftRecipe recipe,String key,int depth,Set<HighResCraftRecipe> ancestors){
            visible.add(new TreeRow(key,recipe.displayName(),depth,false,!recipe.requirements.isEmpty(),recipe,null));
            if(browser.expanded(key,false))appendComponents(recipe,key,depth+1,ancestors);
        }
        void appendComponents(HighResCraftRecipe recipe,String key,int depth,Set<HighResCraftRecipe> ancestors){
            if(depth>16||ancestors.contains(recipe))return;Set<HighResCraftRecipe> path=new HashSet<>(ancestors);path.add(recipe);
            for(int i=0;i<recipe.requirements.size();i++){
                HighResCraftRecipe.Requirement component=recipe.requirements.get(i);HighResCraftRecipe nested=browser.componentRecipe(component);String childKey=key+"/component:"+i;
                boolean expandable=nested!=null&&!nested.requirements.isEmpty()&&!path.contains(nested)&&depth<16;
                visible.add(new TreeRow(childKey,component.name+" ×"+component.count,depth,false,expandable,nested,component));
                if(expandable&&browser.expanded(childKey,false))appendComponents(nested,childKey,depth+1,path);
            }
        }
        private boolean ownsPointer(int px,int py){return contains(px,py)&&browser.scroll.contains(px,py);}
        @Override protected void leftPressed(int px,int py,int count){TreeRow row=at(py);armed=row!=null&&ownsPointer(px,py);pressed=row==null?"":row.key;clicks=count;downX=px;downY=py;}
        @Override protected void mouseDragged(int px,int py){if(Math.abs(px-downX)+Math.abs(py-downY)>5)armed=false;}
        @Override protected void mouseExited(){armed=false;}
        @Override protected void leftReleased(int px,int py){TreeRow row=at(py);boolean fire=armed&&ownsPointer(px,py)&&row!=null&&row.key.equals(pressed);armed=false;pressed="";
            if(!fire)return;if(clicks>=2&&clicks%2==0){if(row.recipe!=null){HighResWindowIntegration.selectRecipe(row.recipe);HighResRecipeWindow.this.close();}}else browser.toggle(row);
        }
        @Override public void pick(PickData pick,int px,int py){TreeRow row=at(py);if(row==null)return;pick.addText(row.label);if(row.component!=null)pick.addText(HudMessages.tr(row.component.start?"Start":"Continue")+": "+HighResBuildWindow.mass(row.component.totalKg()));if(row.recipe!=null){pick.addText(row.recipe.nativeItem.getSkill());pick.addText(HudMessages.tr("Single-click to expand; double-click to build this recipe"));for(HighResCraftRecipe.Requirement req:row.recipe.requirements)pick.addText(req.name+" ×"+req.count+" · "+HighResBuildWindow.mass(req.totalKg()));}}
        @Override protected void renderComponent(Queue queue,float ignored){
            ChamomiloUiV1Canvas ui=new ChamomiloUiV1Canvas(this);ui.begin(queue);
            for(int i=0;i<visible.size();i++){int top=y+i*52;if(top+52<browser.scroll.y||top>browser.scroll.y+browser.scroll.height)continue;
                TreeRow row=visible.get(i);boolean selected=row.key.equals(browser.selectedKey);ui.fill(UiColor.rgb(selected?0x285c35:row.category?0x30291e:i%2==0?0x29231b:0x211c17),1f,x,top,width,51);
                int left=x+8+row.depth*18;if(row.expandable)(browser.expanded(row.key,row.category)?UiIcon.CHEVRON_DOWN:UiIcon.CHEVRON_RIGHT).paint(ui,UiColor.MUTED,1f,left,top+17,10);
                if(row.category){draw(queue,row.label,left+18,top+28,width-(left-x)-24,UiColor.TEXT,true);continue;}
                if(row.recipe!=null)HighResBuildWindow.icon(queue,row.recipe.nativeItem.getIcon(),left+18,top+8,28,1f);
                int textX=left+50;draw(queue,row.label,textX,top+21,width-(textX-x)-8,UiColor.TEXT,true);
                StringBuilder description=new StringBuilder();
                if(row.component!=null)description.append(HudMessages.tr(row.component.start?"Start":"Continue")).append(" · ").append(HighResBuildWindow.mass(row.component.totalKg())).append(" · ").append(HudMessages.tr(row.component.tool?"Tool store":"Material store"));
                else if(row.recipe!=null){description.append(row.recipe.nativeItem.getSkill());for(HighResCraftRecipe.Requirement req:row.recipe.requirements)description.append(" · ").append(req.name).append(" ×").append(req.count);}
                draw(queue,description.toString(),textX,top+42,width-(textX-x)-8,UiColor.MUTED,false);
            }
            if(list.isEmpty())draw(queue,HudMessages.tr(browser.loading()?"Loading recipes…":"No matching recipes"),x+12,y+25,width-24,UiColor.MUTED,false);
        }
    }
    static void autoHide(ChamomiloUiV1ScrollPanel scroll){
        // Older shared SDKs keep their normal visible scrollbars.
        try{scroll.getClass().getMethod("setAutoHideScrollBars",boolean.class).invoke(scroll,true);}
        catch(ReflectiveOperationException unsupported){}
    }
    static void resizeContent(FlexComponent content,int width,int height){int flags=content.sizeFlags;try{content.sizeFlags=0;content.setSize(width,height);}finally{content.sizeFlags=flags;}}
    static void draw(Queue q,String s,int x,int baseline,int width,UiColor color,boolean bold){TextFont f=HudTextFonts.body(14,bold);f.moveTo(x,baseline);f.paint(q,HighResWindowIntegration.ellipsis(s,f,Math.max(0,width)),color.red,color.green,color.blue,1f);}
}
