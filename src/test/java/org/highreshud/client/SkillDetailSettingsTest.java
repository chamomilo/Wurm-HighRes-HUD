package org.highreshud.client;

import java.nio.file.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class SkillDetailSettingsTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private Path preference;
    @Before public void loadEmpty() throws Exception {
        preference = temporary.getRoot().toPath().resolve("skills.properties");
        SkillDetailSettings.load(preference);
    }
    @After public void reset() { SkillDetailSettings.load(preference.resolveSibling("unused.properties")); }
    @Test public void allIncludesEvenRoundedZeroAndEachThresholdIncludesItsBoundary() {
        assertFalse(SkillDetailSettings.hidden(":Skills", "Mind increased by 0,000000 to 70"));
        for (int index = 1; index < SkillDetailSettings.LEVELS.length; index++) {
            SkillDetailSettings.save(index);
            String minimum = SkillDetailSettings.LEVELS[index];
            assertFalse(minimum, SkillDetailSettings.hidden(":Skills", "Mind increased by " + minimum + " to 70"));
            assertFalse(minimum, SkillDetailSettings.hidden("skills", "Mind decreased by -" + minimum + " to 70"));
            assertTrue(minimum, SkillDetailSettings.hidden(":Skills", "Mind increased by 0,000000 to 70"));
            if (index + 1 < SkillDetailSettings.LEVELS.length)
                assertTrue(minimum, SkillDetailSettings.hidden(":Skills", "Mind increased by " + SkillDetailSettings.LEVELS[index + 1] + " to 70"));
        }
    }
    @Test public void nativeCommaAndDotTicksFilterWithoutHidingOtherMessages() {
        SkillDetailSettings.save(3);
        assertTrue(SkillDetailSettings.hidden(":Skills", "[23:49:18] Mind increased by 0,000023 to 70,584198"));
        assertTrue(SkillDetailSettings.hidden(":Skills", "Mind decreased by 2.3e-5 to 70.584198"));
        assertFalse(SkillDetailSettings.hidden(":Skills", "Mind increased by 0.01 to 70.59"));
        assertFalse(SkillDetailSettings.hidden(":Skills", "You gain an affinity in Mind."));
        assertFalse(SkillDetailSettings.hidden(":Event", "Mind increased by 0.000023 to 70.584198"));
        assertFalse(SkillDetailSettings.hidden(":Skills", "Unknown server message"));
    }
    @Test public void choicePersistsAndInvalidPreferencesFallBackToAll() throws Exception {
        assertFalse(SkillDetailSettings.configured());
        SkillDetailSettings.save(7);
        SkillDetailSettings.load(preference);
        assertTrue(SkillDetailSettings.configured()); assertEquals(7, SkillDetailSettings.selectedIndex());
        Files.write(preference, "level=99\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        SkillDetailSettings.load(preference);
        assertEquals(0, SkillDetailSettings.selectedIndex()); assertFalse(SkillDetailSettings.configured());
        try { SkillDetailSettings.save(-1); fail("Invalid choice accepted"); } catch (IllegalArgumentException expected) { }
    }
}
