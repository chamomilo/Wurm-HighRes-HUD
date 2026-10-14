package org.highreshud.client.craft;

/** Transfer rules verified in SimpleCreationEntry and AdvancedCreationEntry. */
public final class CraftRarityRules {
    private CraftRarityRules(){}
    public static double continueTransfer(CraftCatalog.Entry entry){
        return entry==null||!entry.advanced?0:1.0/Math.max(1,entry.totalSteps);
    }
    public static double startTransfer(CraftCatalog.Entry entry,CraftCatalog.Template component,
                                       boolean source,float availableKg,float consumedKg){
        if(entry==null||component==null)return 0;
        if(entry.advanced){
            // The advanced constructor depletes the source if neither flag is set.
            boolean depleted=source?entry.consumeSource||!entry.consumeTarget:entry.consumeTarget;
            if(!depleted)return 0;
            // Fishing-pole rarity is always carried into its initial rod upgrade.
            return source&&component.id==1344?1:1.0/Math.max(1,entry.totalSteps);
        }
        // Simple assembly copies weapon-head/tool-part/blade rarity only when
        // that whole component is consumed. Ordinary raw materials do not copy it.
        return consumedKg>0&&CraftReadiness.enough(consumedKg,availableKg)&&CraftCatalog.carriesPartRarity(component)?1:0;
    }
}
