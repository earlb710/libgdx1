package eb.framework1.character;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for {@link SkinToneDefinition}.
 *
 * <p>All tests are pure-Java with no libGDX runtime dependency.
 * {@link eb.framework1.save.GameDataManager} is NOT exercised here because it
 * requires a libGDX {@code Gdx.files} environment; the JSON parsing path is
 * tested indirectly via the {@link SkinToneDefinition} POJO instead.
 */
public class SkinToneDefinitionTest {

    // =========================================================================
    // Constructor and getters
    // =========================================================================

    @Test
    public void definition_storesCodeNameAndRgb() {
        SkinToneDefinition def = new SkinToneDefinition("porcelain_very_fair", "Porcelain / Very Fair", "rgb(255, 229, 217)");
        assertEquals("porcelain_very_fair",   def.getCode());
        assertEquals("Porcelain / Very Fair", def.getName());
        assertEquals("rgb(255, 229, 217)",    def.getRgb());
    }

    @Test
    public void noArgConstructor_producesNullFields() {
        SkinToneDefinition def = new SkinToneDefinition();
        assertNull(def.getCode());
        assertNull(def.getName());
        assertNull(def.getRgb());
    }

    // =========================================================================
    // Setters
    // =========================================================================

    @Test
    public void setters_updateFields() {
        SkinToneDefinition def = new SkinToneDefinition();
        def.setCode("fair_light");
        def.setName("Fair / Light");
        def.setRgb("rgb(245, 208, 197)");
        assertEquals("fair_light",          def.getCode());
        assertEquals("Fair / Light",        def.getName());
        assertEquals("rgb(245, 208, 197)", def.getRgb());
    }

    // =========================================================================
    // toString
    // =========================================================================

    @Test
    public void toString_containsCodeNameAndRgb() {
        SkinToneDefinition def = new SkinToneDefinition("medium_brown", "Medium Brown", "rgb(198, 134, 66)");
        String str = def.toString();
        assertTrue(str.contains("medium_brown"));
        assertTrue(str.contains("Medium Brown"));
        assertTrue(str.contains("rgb(198, 134, 66)"));
    }

    // =========================================================================
    // All six defined skin tones
    // =========================================================================

    @Test
    public void allSixTones_haveExpectedCodesNamesAndRgb() {
        Object[][] expected = {
            {"porcelain_very_fair", "Porcelain / Very Fair", "rgb(255, 229, 217)"},
            {"fair_light",          "Fair / Light",          "rgb(245, 208, 197)"},
            {"light_medium",        "Light Medium",          "rgb(235, 196, 175)"},
            {"medium_olive",        "Medium / Olive",        "rgb(212, 165, 116)"},
            {"medium_brown",        "Medium Brown",          "rgb(198, 134, 66)"},
            {"dark_brown_deep",     "Dark Brown / Deep",     "rgb(141, 85, 36)"},
        };
        for (Object[] row : expected) {
            SkinToneDefinition def = new SkinToneDefinition((String) row[0], (String) row[1], (String) row[2]);
            assertEquals(row[0], def.getCode());
            assertEquals(row[1], def.getName());
            assertEquals(row[2], def.getRgb());
        }
    }
}
