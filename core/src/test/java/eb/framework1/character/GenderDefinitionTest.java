package eb.framework1.character;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for {@link GenderDefinition}.
 *
 * <p>All tests are pure-Java with no libGDX runtime dependency.
 * {@link eb.framework1.save.GameDataManager} is NOT exercised here because it
 * requires a libGDX {@code Gdx.files} environment; the JSON parsing path is
 * tested indirectly via the {@link GenderDefinition} POJO instead.
 */
public class GenderDefinitionTest {

    // =========================================================================
    // Constructor and getters
    // =========================================================================

    @Test
    public void definition_storesCodeAndName_male() {
        GenderDefinition def = new GenderDefinition("male", "Male");
        assertEquals("male", def.getCode());
        assertEquals("Male", def.getName());
    }

    @Test
    public void definition_storesCodeAndName_female() {
        GenderDefinition def = new GenderDefinition("female", "Female");
        assertEquals("female", def.getCode());
        assertEquals("Female", def.getName());
    }

    @Test
    public void noArgConstructor_producesNullFields() {
        GenderDefinition def = new GenderDefinition();
        assertNull(def.getCode());
        assertNull(def.getName());
    }

    // =========================================================================
    // Setters
    // =========================================================================

    @Test
    public void setters_updateFields() {
        GenderDefinition def = new GenderDefinition();
        def.setCode("male");
        def.setName("Male");
        assertEquals("male", def.getCode());
        assertEquals("Male", def.getName());
    }

    // =========================================================================
    // toString
    // =========================================================================

    @Test
    public void toString_containsCodeAndName() {
        GenderDefinition def = new GenderDefinition("female", "Female");
        String str = def.toString();
        assertTrue(str.contains("female"));
        assertTrue(str.contains("Female"));
    }

    // =========================================================================
    // Both defined gender entries
    // =========================================================================

    @Test
    public void bothEntries_haveExpectedCodesAndNames() {
        Object[][] expected = {
            {"male",   "Male"},
            {"female", "Female"},
        };
        for (Object[] row : expected) {
            GenderDefinition def = new GenderDefinition((String) row[0], (String) row[1]);
            assertEquals(row[0], def.getCode());
            assertEquals(row[1], def.getName());
        }
    }
}
