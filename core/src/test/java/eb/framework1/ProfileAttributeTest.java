package eb.framework1;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests for the detective-level computation on {@link Profile} and the
 * {@link CharacterAttribute#DETECTIVE_LEVEL} derived-attribute marker.
 */
public class ProfileAttributeTest {

    // -------------------------------------------------------------------------
    // CharacterAttribute.DETECTIVE_LEVEL enum entry
    // -------------------------------------------------------------------------

    @Test
    public void detectiveLevelAttribute_exists() {
        assertNotNull(CharacterAttribute.DETECTIVE_LEVEL);
    }

    @Test
    public void detectiveLevelAttribute_isDerived() {
        assertTrue(CharacterAttribute.DETECTIVE_LEVEL.isDerivedAttribute());
    }

    @Test
    public void detectiveLevelAttribute_isNotBodyMeasurement() {
        assertFalse(CharacterAttribute.DETECTIVE_LEVEL.isBodyMeasurement());
    }

    @Test
    public void detectiveLevelAttribute_hasDisplayName() {
        assertFalse(CharacterAttribute.DETECTIVE_LEVEL.getDisplayName().isEmpty());
    }

    @Test
    public void detectiveLevelAttribute_hasDescription() {
        assertFalse(CharacterAttribute.DETECTIVE_LEVEL.getDescription().isEmpty());
    }

    @Test
    public void detectiveLevelAttribute_categoryIsDerived() {
        assertEquals("Derived", CharacterAttribute.DETECTIVE_LEVEL.getCategory());
    }

    @Test
    public void noOtherAttributeIsDerived() {
        // Only DETECTIVE_LEVEL should be marked as a derived attribute.
        for (CharacterAttribute attr : CharacterAttribute.values()) {
            if (attr == CharacterAttribute.DETECTIVE_LEVEL) continue;
            assertFalse("Only DETECTIVE_LEVEL should be derived, got: " + attr,
                    attr.isDerivedAttribute());
        }
    }

    @Test
    public void noOtherAttributeIsBothBodyMeasurementAndDerived() {
        for (CharacterAttribute attr : CharacterAttribute.values()) {
            assertFalse("Attribute cannot be both body measurement and derived: " + attr,
                    attr.isBodyMeasurement() && attr.isDerivedAttribute());
        }
    }

    // -------------------------------------------------------------------------
    // Profile.getDetectiveLevel() — boundary values
    // -------------------------------------------------------------------------

    @Test
    public void detectiveLevel_allAttributesAtMin_returns1() {
        Profile p = allAttrsAt(1);
        assertEquals(1, p.getDetectiveLevel());
    }

    @Test
    public void detectiveLevel_allAttributesAtMax_returns10() {
        Profile p = allAttrsAt(10);
        assertEquals(10, p.getDetectiveLevel());
    }

    @Test
    public void detectiveLevel_allAttributesAt5_returns5() {
        // sum = 11 × 5 = 55;  level = 1 + (55 - 11) / 11 = 1 + 4 = 5
        Profile p = allAttrsAt(5);
        assertEquals(5, p.getDetectiveLevel());
    }

    @Test
    public void detectiveLevel_allAttributesAt2_returns2() {
        // sum = 11 × 2 = 22;  level = 1 + (22 - 11) / 11 = 1 + 1 = 2
        Profile p = allAttrsAt(2);
        assertEquals(2, p.getDetectiveLevel());
    }

    @Test
    public void detectiveLevel_allAttributesAt9_returns9() {
        // sum = 11 × 9 = 99;  level = 1 + (99 - 11) / 11 = 1 + 8 = 9
        Profile p = allAttrsAt(9);
        assertEquals(9, p.getDetectiveLevel());
    }

    @Test
    public void detectiveLevel_noAttributesStored_returns1() {
        // A fresh profile with no attributes stored defaults every value to 0,
        // which is below minimum. The method must still return at least 1.
        Profile p = new Profile("Test", "Male", "Normal");
        int level = p.getDetectiveLevel();
        assertTrue("Level must be >= 1 even with no attributes stored", level >= 1);
        assertTrue("Level must be <= 10", level <= 10);
    }

    @Test
    public void detectiveLevel_alwaysInRange1to10() {
        // Try a variety of partial attribute sets.
        for (int v = 1; v <= 10; v++) {
            Profile p = allAttrsAt(v);
            int level = p.getDetectiveLevel();
            assertTrue("Level must be >= 1 for all-" + v, level >= 1);
            assertTrue("Level must be <= 10 for all-" + v, level <= 10);
        }
    }

    @Test
    public void detectiveLevel_bodyMeasurementsDoNotInfluenceLevel() {
        // Two profiles: same investigative attrs, different body measurements.
        Profile p1 = allAttrsAt(5);
        Profile p2 = allAttrsAt(5);
        p2.setAttribute(CharacterAttribute.HEIGHT_CM.name(), 200);
        p2.setAttribute(CharacterAttribute.WEIGHT_KG.name(), 120);
        p2.setAttribute(CharacterAttribute.MUSCLE_KG.name(), 50);
        p2.setAttribute(CharacterAttribute.FAT_KG.name(), 30);
        assertEquals("Body measurements must not affect detective level",
                p1.getDetectiveLevel(), p2.getDetectiveLevel());
    }

    @Test
    public void detectiveLevel_sumDrivenByAllElevenAttributes() {
        // Start at all-1, raise one attribute at a time, verify level only changes
        // once the integer division boundary is crossed.
        Profile base = allAttrsAt(1);               // level 1, sum = 11
        assertEquals(1, base.getDetectiveLevel());

        // Raise INTELLIGENCE by 11 → sum = 22 → level 2
        Profile raised = allAttrsAt(1);
        raised.setAttribute(CharacterAttribute.INTELLIGENCE.name(), 12); // 12 - 1 = 11 extra
        assertEquals(2, raised.getDetectiveLevel());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a Profile with all 11 investigative attributes set to {@code value}.
     */
    private static Profile allAttrsAt(int value) {
        Map<String, Integer> attrs = new HashMap<>();
        for (CharacterAttribute attr : CharacterAttribute.values()) {
            if (attr.isBodyMeasurement() || attr.isDerivedAttribute()) continue;
            attrs.put(attr.name(), value);
        }
        return new Profile("Detective", "Male", "Normal", attrs);
    }
}
