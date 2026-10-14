package org.highreshud.client;

import org.chamomilo.wurm.update.ModSetting;
import java.util.*;

/** Product metadata for the shared native Settings editor. */
final class ModSettingsSchema {
    private ModSettingsSchema() { }
    static void includeSkillDetail() {
        final org.chamomilo.wurm.update.SharedModSettings.Provider base = org.chamomilo.wurm.update.SharedModSettings.provider("highres-hud");
        final ModSetting detail = ModSetting.choice("Window contents","skillDetail","Skill detail level",
                "Hide skill change messages smaller than this value. All shows every change; skill values and server updates remain unchanged.","All",true,SkillDetailSettings.LEVELS);
        org.chamomilo.wurm.update.SharedModSettings.register("highres-hud",new org.chamomilo.wurm.update.SharedModSettings.Provider() {
            public List<ModSetting> settings() { List<ModSetting> result=new ArrayList<ModSetting>(base.settings());result.add(detail);return result; }
            public String value(String key) { return key.equals("skillDetail")?SkillDetailSettings.LEVELS[SkillDetailSettings.selectedIndex()]:base.value(key); }
            public void change(String key,String value)throws Exception {
                if(key.equals("skillDetail"))SkillDetailSettings.save(Arrays.asList(SkillDetailSettings.LEVELS).indexOf(detail.validate(value)));
                else base.change(key,value);
            }
        });
    }
    static List<ModSetting> specs() {
        return Arrays.asList(
            ModSetting.choice("Window contents", "fontSize", "Font size", "Font size in pixels for window CONTENTS: inventory rows, chat, events and descriptions. Titles, column headings, tabs and buttons keep their own sizes. 0 follows the native font preference; selecting a size overrides that preference for contents only.", "0", true, "0", "12", "14", "16", "18", "20", "24", "28", "32", "36", "40", "44", "48"),
            ModSetting.number("Windows", "inventory.defaultOpacity", "Default opacity of inventory windows", "Initial opacity of all high-res windows, from 10 to 100 percent. Applies when a window opens. Drag the fourth title-control square, or the slider beside Chat/Events tabs, to change that window. The control stays visible at minimum opacity and while collapsed.", 100, 10, 100, true, true),
            ModSetting.bool("Windows", "inventory.enabledByDefault", "Inventory window", "Use the HighRes skin for this native window at the next client start.", true, false),
            ModSetting.bool("Windows", "events.enabledByDefault", "Events window", "Use the HighRes skin for this native window at the next client start.", true, false),
            ModSetting.bool("Windows", "chat.enabledByDefault", "Chat window", "Use the HighRes skin for this native window at the next client start.", true, false),
            ModSetting.bool("Windows", "character.enabledByDefault", "Character window", "Use the HighRes skin for this native window at the next client start.", true, false),
            ModSetting.bool("Windows", "build.enabledByDefault", "Build menu and recipe browser", "Use high-res crafting windows with material and tool reservations at the next client start.", true, false),
            ModSetting.bool("HUD panels", "healthbar.enabledByDefault", "healthbar enabled by default", "Default visibility for a new HUD profile. Existing visibility is controlled by Wurm's HUD Settings.", true, false),
            ModSetting.bool("HUD panels", "selectbar.enabledByDefault", "selectbar enabled by default", "Default visibility for a new HUD profile. Existing visibility is controlled by Wurm's HUD Settings.", true, false),
            ModSetting.bool("HUD panels", "fightingHud.enabledByDefault", "fightingHud enabled by default", "Default visibility for a new HUD profile. Existing visibility is controlled by Wurm's HUD Settings.", true, false),
            ModSetting.number("Healthbar portrait", "healthbar.portraitRenderSize", "Healthbar portrait resolution", "Resolution of the offscreen portrait texture, allocated when the HUD is created.", 256, 128, 1024, true, false),
            ModSetting.number("Healthbar portrait", "healthbar.portraitRotation", "Portrait rotation (degrees)", "Rotation of the character portrait, applied when its renderer is created.", 45, -90, 90, false, false),
            ModSetting.number("Healthbar portrait", "healthbar.portraitFov", "Portrait field of view (degrees)", "Field of view of the healthbar portrait renderer.", 45, 45, 60, false, false),
            ModSetting.number("Healthbar portrait", "healthbar.portraitCropWidth", "Portrait crop width", "Fraction of the portrait source used for the visible crop.", 0.4, 0.25, 1, false, true),
            ModSetting.number("Healthbar portrait", "healthbar.portraitCropHeight", "Portrait crop height", "Vertical fraction of the portrait source used for the visible crop.", 0.42, 0.25, 1, false, true),
            ModSetting.number("Healthbar", "healthbar.fatigueRefreshSeconds", "Fatigue refresh interval (s)", "Interval between fatigue refresh requests.", 60, 30, 600, true, true),
            ModSetting.number("Healthbar", "healthbar.fatigueInitialDelaySeconds", "Fatigue initial delay (s)", "Delay before the first fatigue request after the tracker is refreshed.", 4, 0, 60, true, true),
            ModSetting.bool("Animation", "healthbar.animateRisingGauges", "Animate rising healthbar gauges", "Animate increasing health, stamina and other healthbar values.", true, true),
            ModSetting.number("Selected target", "selectbar.portraitRenderSize", "Selected target portrait resolution", "Resolution of the selected target portrait texture.", 512, 256, 1024, true, false),
            ModSetting.bool("Animation", "selectbar.animateGaugeShine", "Animate selected target shine", "Animate gauge highlights in the selected target panel.", true, true),
            ModSetting.number("Fighting HUD", "fightingHud.portraitRenderSize", "Fighting portrait resolution", "Resolution of the fighting target portrait texture.", 512, 256, 1024, true, false),
            ModSetting.bool("Animation", "fightingHud.animateGaugeShine", "Animate fighting gauge shine", "Animate gauge highlights in the fighting HUD.", true, true),
            ModSetting.bool("Fighting HUD", "fightingHud.collectCombatKnowledge", "Collect combat knowledge", "Collect new combat observations for the fighting HUD.", true, true),
            ModSetting.text("Data storage", "fightingHud.knowledgeDirectory", "Combat knowledge folder", "Folder used for combat observations; the store is created on startup.", "mods/highres-hud/knowledge", false),
            ModSetting.text("Resources", "portraitProfilesOverride", "Portrait profile file", "Optional portrait crop profiles read at startup.", "mods/highres-hud/portrait-profiles.properties", false),
            ModSetting.text("Resources", "resourcePack", "HUD resource pack", "Resource pack supplying portrait and HUD artwork.", "mods/highres-hud/highres-hud-resources-" + HighResHudMod.VERSION + ".jar", false)
        );
    }
}
