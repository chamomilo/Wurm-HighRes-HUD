package org.highreshud.client.craft;

import org.junit.Test;
import static org.junit.Assert.*;

public class CraftRarityRulesTest {
    @Test public void continueUsesFullRecipeCountAndIndependentMaterialRoll(){
        CraftCatalog.Entry wagon=CraftCatalog.entry(CraftCatalog.get(850),CraftCatalog.get(22),CraftCatalog.get(191));
        double transfer=CraftRarityRules.continueTransfer(wagon);assertEquals(1.0/41,transfer,.000001);
        CraftRarityRoll model=new CraftRarityRoll();
        assertEquals(100.0/41,model.materialUpgrade((byte)0,(byte)1,transfer,(byte)0,0),.00001);
        assertEquals(0,model.materialUpgrade((byte)1,(byte)1,transfer,(byte)0,0),.00001);
        assertEquals(100.0/41,model.materialUpgrade((byte)1,(byte)2,transfer,(byte)0,0),.00001);
        double combined=model.afterDrumroll(1,(byte)0,(byte)1,transfer,(byte)0,0,transfer).getMinimum();
        assertEquals(100*(1-Math.pow(1-transfer,2)),combined,.00001);
        assertTrue(combined<200*transfer);
    }
    @Test public void advancedStartRespectsDepletionFlagsAndFishingPoleException(){
        CraftCatalog.Entry e=new CraftCatalog.Entry();e.advanced=true;e.totalSteps=50;
        assertEquals(.02,CraftRarityRules.startTransfer(e,CraftCatalog.get(22),true,20,2),.000001);
        assertEquals(0,CraftRarityRules.startTransfer(e,CraftCatalog.get(191),false,10,10),.000001);
        e.consumeTarget=true;
        assertEquals(0,CraftRarityRules.startTransfer(e,CraftCatalog.get(22),true,20,2),.000001);
        assertEquals(.02,CraftRarityRules.startTransfer(e,CraftCatalog.get(191),false,10,10),.000001);
        e.consumeSource=true;
        assertEquals(1,CraftRarityRules.startTransfer(e,CraftCatalog.get(1344),true,.86f,.86f),.000001);
    }
    @Test public void simpleAssemblyCopiesWholePartsButNotOrdinaryRawMaterials(){
        CraftCatalog.Entry e=new CraftCatalog.Entry();e.consumeSource=true;
        CraftCatalog.Template head=CraftCatalog.find("hammer head",0);assertTrue(CraftCatalog.carriesPartRarity(head));
        float kg=head.grams/1000f;
        assertEquals(1,CraftRarityRules.startTransfer(e,head,true,kg,kg),.000001);
        assertEquals(0,CraftRarityRules.startTransfer(e,head,true,2*kg,kg),.000001);
        assertEquals(0,CraftRarityRules.startTransfer(e,CraftCatalog.get(46),true,.4f,.4f),.000001);
        assertEquals(0,CraftRarityRules.startTransfer(e,head,true,kg,0),.000001);
    }
}
