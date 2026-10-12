package org.highreshud.client;

import java.util.Properties;
import org.chamomilo.wurm.update.ModSetting;
import org.junit.Test;
import static org.junit.Assert.*;

public class InventoryOpacitySettingsTest {
    @Test public void liveDefaultChangeDoesNotApplyPendingStartupOrFontSettings() {
        int previous=HighResWindowsSettings.defaultInventoryOpacity,font=HighResWindowsSettings.fontSize;
        boolean inventory=HighResWindowsSettings.inventory;
        try {
            HighResWindowsSettings.fontSize=24;HighResWindowsSettings.inventory=true;
            Properties props=new Properties();props.setProperty("inventory.defaultOpacity","45");
            props.setProperty("fontSize","12");props.setProperty("inventory.enabledByDefault","false");
            HighResWindowsSettings.configureOpacity(props);
            assertEquals(45,HighResWindowsSettings.defaultInventoryOpacity);assertEquals(24,HighResWindowsSettings.fontSize);assertTrue(HighResWindowsSettings.inventory);
        } finally {HighResWindowsSettings.defaultInventoryOpacity=previous;HighResWindowsSettings.fontSize=font;HighResWindowsSettings.inventory=inventory;}
    }
    @Test public void registryRejectsValuesOutsideTheRequestedRange() {
        ModSetting spec=ModSettingsSchema.specs().stream().filter(value -> value.key.equals("inventory.defaultOpacity")).findFirst().get();
        assertEquals("Default opacity of inventory windows",spec.label);
        assertEquals("10",spec.validate("10"));assertEquals("100",spec.validate("100"));
        for(String invalid:new String[]{"0","5","9","101","50.5","unknown"}) {
            try {spec.validate(invalid);fail("Invalid opacity accepted: "+invalid);}catch(IllegalArgumentException expected) { }
        }
        assertTrue(spec.live);
    }
    @Test public void legacyAndHandEditedConfigurationsHaveSafeDefaults() {
        int previous=HighResWindowsSettings.defaultInventoryOpacity;
        try {
            Properties props=new Properties();HighResWindowsSettings.configure(props);assertEquals(100,HighResWindowsSettings.defaultInventoryOpacity);
            props.setProperty("inventory.defaultOpacity","5");HighResWindowsSettings.configure(props);assertEquals(10,HighResWindowsSettings.defaultInventoryOpacity);
            props.setProperty("inventory.defaultOpacity","150");HighResWindowsSettings.configure(props);assertEquals(100,HighResWindowsSettings.defaultInventoryOpacity);
            props.setProperty("inventory.defaultOpacity","35");HighResWindowsSettings.configure(props);assertEquals(35,HighResWindowsSettings.defaultInventoryOpacity);
        } finally {HighResWindowsSettings.defaultInventoryOpacity=previous;}
    }
}
