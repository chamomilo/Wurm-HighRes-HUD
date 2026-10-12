package org.highreshud.client;

import java.awt.Rectangle;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import org.chamomilo.wurm.ui.v1.*;
import org.highreshud.client.i18n.*;
import org.highreshud.client.ui.*;
import org.highresfightinghud.client.CombatFocusState;
import org.highresfocusbar.client.*;
import org.junit.*;
import static org.junit.Assert.*;

public class HudLocalizationTest {
    @After public void resetLanguage() { HudMessages.useLanguage("en"); }

    @Test public void russianItemColumnsUseTheRequestedAbbreviations() {
        assertEquals("Кч", HudMessages.translate("ru", "QL"));
        assertEquals("Пвр", HudMessages.translate("ru", "DMG"));
        assertEquals("Кч 90 · Пвр 1", HudMessages.translate("ru", "QL {0} · Dam {1}", 90, 1));
    }

    @Test public void allTranslationsHaveMatchingPlaceholdersAndBundledGlyphs() {
        Pattern placeholder = Pattern.compile("\\{\\d+\\}");
        for (String source : HudMessages.sources()) for (String code : HudMessages.languageCodes()) {
            String translated = HudMessages.translate(code, source);
            Set<String> originalArgs = new HashSet<>(), translatedArgs = new HashSet<>();
            for (Matcher m = placeholder.matcher(source); m.find();) originalArgs.add(m.group());
            for (Matcher m = placeholder.matcher(translated); m.find();) translatedArgs.add(m.group());
            assertEquals(code + ": " + source, originalArgs, translatedArgs);
            for (UiDensity density : UiDensity.values()) for (boolean bold : new boolean[]{false, true})
                assertEquals(code + ": missing glyph in " + translated, -1,
                        UiTypography.font(12, bold, density).canDisplayUpTo(translated));
        }
        assertEquals("Предмет {1}", HudMessages.translate("ru", "{0}", "Предмет {1}"));
    }

    @Test public void everyFixedTranslationCallHasAResource() throws Exception {
        Path root = Paths.get(System.getProperty("highResHudProjectDir"), "src/main/java");
        Pattern call = Pattern.compile("(?:tr|detail)\\(\"([^\"\\\\]+)\"");
        try (java.util.stream.Stream<Path> files = Files.walk(root)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    String source = new String(Files.readAllBytes(p), java.nio.charset.StandardCharsets.UTF_8);
                    for (Matcher m = call.matcher(source); m.find();)
                        assertTrue(p + ": untranslated " + m.group(1), HudMessages.contains(m.group(1)));
                } catch (java.io.IOException failure) { throw new RuntimeException(failure); }
            });
        }
    }

    @Test public void buttonGroupsRelocalizeExistingControlsWithoutShorteningOrResizing() {
        Object[][] groups = {{76,14,new String[]{"Activate","Deactivate"}},
                {76,16,new String[]{"Disembark"}}, {78,20,new String[]{"No target"}},
                {96,20,new String[]{"Combat focus"}}};
        for (Object[] entry : groups) {
            int width = (Integer)entry[0], height = (Integer)entry[1];
            String[] variants = (String[])entry[2];
            HudCaptionGroup group = new HudCaptionGroup(width,height,HudSkin.COMPACT,width==78||width==96?14:128,variants);
            for (String code : HudMessages.languageCodes()) {
                HudMessages.useLanguage(code); group.refreshLanguage();
                for (String caption : variants) {
                    assertEquals(code + ": shortened " + caption, HudMessages.tr(caption), group.caption(caption));
                    for (boolean bold : new boolean[]{false,true}) {
                        Rectangle ink = UiTypography.ink(group.caption(caption),group.fontPixels,bold,UiDensity.HIGH);
                        ink.translate(group.textX(caption,bold,width),group.baseline);
                        assertTrue(code + ": clipped " + caption + " " + ink,
                                ink.x >= 2 && ink.x + ink.width <= width - 1
                                && ink.y >= 1 && ink.y + ink.height <= height - 1);
                    }
                }
            }
        }
    }

    @Test public void storedDetailsAndFocusLogicSurviveLiveLanguageChanges() {
        SelectedExamineState state = new SelectedExamineState();
        // Use the same public observation path as the server; snapshots stay canonical English.
        assertTrue(state.observeFor(4L, ":Event", "It is locked with a lock of quality 50."));
        for (String code : HudMessages.languageCodes()) {
            HudMessages.useLanguage(code);
            assertEquals(HudMessages.tr("Locked"), SelectedStatusText.compose(state.get(4L), ""));
            assertEquals(HudMessages.tr("Pregnant ~{0}d", 12), HudMessages.detail("Pregnant ~12d"));
            assertEquals(HudMessages.tr("Engaging ~{0}/3", 2), HudMessages.detail("Engaging ~2/3"));
            assertEquals(HudMessages.tr("Owner ") + "Русский {1}", HudMessages.detail("Owner Русский {1}"));
            assertEquals("Custom server creature", HudMessages.detail("Custom server creature"));
            CombatFocusState focus = new CombatFocusState(); focus.combatChanged(true);
            focus.levelReceived((byte)1, "server focus message");
            String status = focus.readiness(true,false,true,false,false,System.nanoTime());
            assertTrue("Translation must not change action readiness", CombatFocusState.ready(status));
            assertEquals(HudMessages.tr("Ready to try"), HudMessages.detail(status));
            assertEquals("server focus message", focus.levelMessage());
        }
    }

    @Test public void fixedGaugeAndFooterLabelsStayWithinTheirColumns() {
        for (String code : HudMessages.languageCodes()) {
            String[] nutrients = {"Calories","Carbs","Fats","Prot."};
            for (int i = 0; i < nutrients.length; i++)
                assertTrue(code + ": nutrient label too wide: " + nutrients[i],
                        UiTypography.width(HudMessages.translate(code,nutrients[i]),12,true,UiDensity.LOW)
                        <= HudSkin.healthGauge(0,0,i+3).width - 6);
            for (String label : new String[]{"Sleep bonus: ON","Sleep bonus: OFF"})
                assertTrue(code + ": sleep label overlaps action",
                        UiTypography.width(HudMessages.translate(code,label),12,true,UiDensity.LOW) <= 128);
            assertTrue(code + ": fatigue footer overlaps divider",
                    UiTypography.width(HudMessages.translate(code,"Fatigue:"),14,false,UiDensity.LOW) + 3
                    + UiTypography.width("23:59",14,true,UiDensity.LOW) <= 109);
        }
    }

    @Test public void updaterCapabilityPersistsTheHudChoiceAcrossInstances() throws Exception {
        Path file = Files.createTempDirectory("hud-language-test").resolve("highres-hud.properties");
        HighResHudMod mod = new HighResHudMod();
        java.lang.reflect.Field preferences = HighResHudMod.class.getDeclaredField("languagePreferences");
        preferences.setAccessible(true); preferences.set(mod,new HudLanguagePreferences(file));
        assertArrayEquals(new String[]{"en","pt-BR","de","ru"},mod.getSupportedLanguages());
        mod.setLanguage("pt_br"); assertEquals("pt-BR",mod.getLanguage());
        assertEquals("pt-BR",new HudLanguagePreferences(file).language());
        try { mod.setLanguage("fr"); fail("Unsupported language must not overwrite the preference"); }
        catch (IllegalArgumentException expected) { }
        assertEquals("pt-BR",new HudLanguagePreferences(file).language());
    }
}
