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
        SkinToneDefinition def = new SkinToneDefinition("porcelain_very_fair", "Porcelain / Very Fair", "#FFE5D9");
        assertEquals("porcelain_very_fair",   def.getCode());
        assertEquals("Porcelain / Very Fair", def.getName());
        assertEquals("#FFE5D9",               def.getRgb());
        assertEquals(10,                      def.getPercentage()); // default
    }

    @Test
    public void fourArgConstructor_storesPercentage() {
        SkinToneDefinition def = new SkinToneDefinition("fair_light", "Fair / Light", "#F5D0C5", 20);
        assertEquals(20, def.getPercentage());
    }

    @Test
    public void setPercentage_updatesField() {
        SkinToneDefinition def = new SkinToneDefinition();
        def.setPercentage(15);
        assertEquals(15, def.getPercentage());
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
        def.setRgb("#F5D0C5");
        assertEquals("fair_light",   def.getCode());
        assertEquals("Fair / Light", def.getName());
        assertEquals("#F5D0C5",      def.getRgb());
    }

    // =========================================================================
    // toString
    // =========================================================================

    @Test
    public void toString_containsCodeNameRgbAndPercentage() {
        SkinToneDefinition def = new SkinToneDefinition("medium_brown", "Medium Brown", "#C68642", 15);
        String str = def.toString();
        assertTrue(str.contains("medium_brown"));
        assertTrue(str.contains("Medium Brown"));
        assertTrue(str.contains("#C68642"));
        assertTrue(str.contains("15"));
    }

    // =========================================================================
    // All six defined skin tones
    // =========================================================================

    @Test
    public void allSixTones_haveExpectedCodesNamesAndRgb() {
        Object[][] expected = {
            {"porcelain_very_fair", "Porcelain / Very Fair", "#FFE5D9"},
            {"fair_light",          "Fair / Light",          "#F5D0C5"},
            {"light_medium",        "Light Medium",          "#EBC4AF"},
            {"medium_olive",        "Medium / Olive",        "#D4A574"},
            {"medium_brown",        "Medium Brown",          "#C68642"},
            {"dark_brown_deep",     "Dark Brown / Deep",     "#8D5524"},
        };
        for (Object[] row : expected) {
            SkinToneDefinition def = new SkinToneDefinition((String) row[0], (String) row[1], (String) row[2]);
            assertEquals(row[0], def.getCode());
            assertEquals(row[1], def.getName());
            assertEquals(row[2], def.getRgb());
        }
    }
}
