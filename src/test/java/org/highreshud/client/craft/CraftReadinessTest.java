package org.highreshud.client.craft;
import org.junit.Test;
import static org.junit.Assert.*;

public class CraftReadinessTest {
    @Test public void nextStepChecksMassAndGlowingIndependently(){
        assertFalse(CraftReadiness.usable(.099f,.1f,5,true));
        assertFalse(CraftReadiness.usable(.1f,.1f,4,true));
        assertTrue(CraftReadiness.usable(.1f,.1f,5,true));
        assertTrue(CraftReadiness.usable(.1f,.1f,0,false));
        assertFalse(CraftReadiness.enough(10f,Float.NaN));
    }
    @Test public void successfulQualityBoundDependsOnSelectedMaterial(){
        assertEquals(31f,CraftReadiness.nextQualityUpperBound(30f,20f,20,false),.0001f);
        assertEquals(34f,CraftReadiness.nextQualityUpperBound(30f,80f,20,false),.0001f);
        assertEquals(100f,CraftReadiness.nextQualityUpperBound(99f,80f,20,false),.0001f);
        assertEquals(40f,CraftReadiness.effectiveQuality(80f,50f),.0001f);
        assertEquals(32f,CraftReadiness.nextQualityUpperBound(30f,CraftReadiness.effectiveQuality(80f,50f),20,false),.0001f);
    }
    @Test public void standardMassesAndResultModelsComeFromPinnedTemplates(){
        assertEquals(2000,CraftCatalog.get(22).grams);
        assertEquals(15000,CraftCatalog.get(132).grams);
        assertEquals("model.transports.medium.wagon.",CraftCatalog.get(850).model);
        assertEquals(46,CraftCatalog.find("iron lump",633).id);
        assertEquals(185,CraftCatalog.find("large anvil",791).id);
        assertEquals(64,CraftCatalog.find("small anvil",791).id);
        CraftCatalog.Entry plank=CraftCatalog.entry(CraftCatalog.get(22),CraftCatalog.get(24),CraftCatalog.get(9));
        assertNotNull(plank);assertEquals(4f,CraftCatalog.startKilograms(plank,CraftCatalog.get(9),CraftCatalog.get(22),false),.0001f);
        CraftCatalog.Entry shaft=CraftCatalog.entry(CraftCatalog.get(23),CraftCatalog.get(8),CraftCatalog.get(9));
        assertEquals(100f,shaft.percentageLost,0f);
        assertEquals(2f,CraftCatalog.startKilograms(shaft,CraftCatalog.get(9),CraftCatalog.get(23),false),.0001f);
        assertEquals(0f,CraftCatalog.startKilograms(shaft,CraftCatalog.get(8),CraftCatalog.get(23),true),.0001f);
        CraftCatalog.Entry wagon=CraftCatalog.entry(CraftCatalog.get(850),CraftCatalog.get(22),CraftCatalog.get(191));
        assertNotNull(wagon);assertTrue(wagon.destroyBoth);assertEquals(41,wagon.totalSteps);
        assertEquals(2f,CraftCatalog.startKilograms(wagon,CraftCatalog.get(22),CraftCatalog.get(850),true),.0001f);
        CraftCatalog.Entry copperHead=CraftCatalog.entry(CraftCatalog.find("hammer head",0),CraftCatalog.get(64),CraftCatalog.get(47));
        assertNotNull(copperHead);assertFalse(copperHead.consumeSource);assertTrue(copperHead.consumeTarget);
    }
}
