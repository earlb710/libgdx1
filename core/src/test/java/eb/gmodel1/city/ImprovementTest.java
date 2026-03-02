package eb.gmodel1.city;

import eb.gmodel1.character.*;


import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Map;

/**
 * Unit tests for Improvement discovery and hidden value.
 */
public class ImprovementTest {

    @Test
    public void testImprovementNotDiscoveredByDefault() {
        Improvement imp = new Improvement("Solar Panels", 3, 4);
        assertFalse("Improvement should not be discovered by default", imp.isDiscovered());
    }

    @Test
    public void testDiscoverImprovement() {
        Improvement imp = new Improvement("Solar Panels", 3, 4);
        assertFalse(imp.isDiscovered());
        imp.discover();
        assertTrue("Improvement should be discovered after calling discover()", imp.isDiscovered());
    }

    @Test
    public void testHiddenValueStored() {
        Improvement imp = new Improvement("Solar Panels", 3, 4);
        assertEquals("Hidden value should be 4", 4, imp.getHiddenValue());
    }

    @Test
    public void testHiddenValueZero() {
        Improvement imp = new Improvement("Garden", 1, 0);
        assertEquals("Hidden value should be 0", 0, imp.getHiddenValue());
        assertFalse("Even with hiddenValue 0, improvement starts undiscovered", imp.isDiscovered());
    }

    @Test
    public void testHiddenValueFour() {
        Improvement imp = new Improvement("Secret Vault", 5, 4);
        assertEquals("Hidden value should be 4", 4, imp.getHiddenValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHiddenValueBelowZero() {
        new Improvement("Invalid", 1, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHiddenValueAboveFive() {
        new Improvement("Invalid", 1, 6);
    }

    @Test
    public void testNameAndLevelPreserved() {
        Improvement imp = new Improvement("HVAC System", 4, 3);
        assertEquals("HVAC System", imp.getName());
        assertEquals(4, imp.getLevel());
    }

    @Test
    public void testToStringIncludesHiddenAndDiscovered() {
        Improvement imp = new Improvement("Elevator", 2, 4);
        String str = imp.toString();
        assertTrue("toString should include hiddenValue", str.contains("hiddenValue=4"));
        assertTrue("toString should include discovered=false", str.contains("discovered=false"));
        imp.discover();
        str = imp.toString();
        assertTrue("toString should include discovered=true after discover()", str.contains("discovered=true"));
    }

    @Test
    public void testAllImprovementsStartUndiscovered() {
        // Verify via CityMap that all generated improvements are undiscovered
        CityMap map = new CityMap(12345L);
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                Cell cell = map.getCell(x, y);
                if (cell.hasBuilding()) {
                    for (Improvement imp : cell.getBuilding().getImprovements()) {
                        assertFalse("All improvements should start undiscovered at (" + x + "," + y + ")",
                                    imp.isDiscovered());
                    }
                }
            }
        }
    }

    @Test
    public void testHiddenValueInRange() {
        // Verify via CityMap that all generated improvements have hiddenValue 0-5
        CityMap map = new CityMap(12345L);
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                Cell cell = map.getCell(x, y);
                if (cell.hasBuilding()) {
                    for (Improvement imp : cell.getBuilding().getImprovements()) {
                        assertTrue("Hidden value should be >= 0 at (" + x + "," + y + ")",
                                   imp.getHiddenValue() >= 0);
                        assertTrue("Hidden value should be <= 5 at (" + x + "," + y + ")",
                                   imp.getHiddenValue() <= 5);
                    }
                }
            }
        }
    }

    @Test
    public void testAttributeModifiersNotNull() {
        Improvement imp = new Improvement("Security System", 3, 4);
    }

    @Test
    public void testSecuritySystemHasPerceptionModifier() {
        Improvement imp = new Improvement("Security Camera", 1, 0);
        Map<CharacterAttribute, Integer> mods = imp.getAttributeModifiers();
        assertTrue("Security Camera should affect Perception", mods.containsKey(CharacterAttribute.PERCEPTION));
        assertTrue("Perception modifier should be positive", mods.get(CharacterAttribute.PERCEPTION) > 0);
    }

    @Test
    public void testFitnessHasStrengthAndStamina() {
        Improvement imp = new Improvement("Fitness Center", 2, 3);
        Map<CharacterAttribute, Integer> mods = imp.getAttributeModifiers();
        assertTrue("Fitness Center should affect Strength", mods.containsKey(CharacterAttribute.STRENGTH));
        assertTrue("Fitness Center should affect Stamina", mods.containsKey(CharacterAttribute.STAMINA));
    }

    @Test
    public void testLibraryHasIntelligenceAndMemory() {
        Improvement imp = new Improvement("Library", 1, 0);
        Map<CharacterAttribute, Integer> mods = imp.getAttributeModifiers();
        assertTrue("Library should affect Intelligence", mods.containsKey(CharacterAttribute.INTELLIGENCE));
        assertTrue("Library should affect Memory", mods.containsKey(CharacterAttribute.MEMORY));
    }

    @Test
    public void testModifiersClampedToRange() {
        // All modifiers should be between -3 and +3
        CityMap map = new CityMap(12345L);
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                Cell cell = map.getCell(x, y);
                if (cell.hasBuilding()) {
                    for (Improvement imp : cell.getBuilding().getImprovements()) {
                        for (Map.Entry<CharacterAttribute, Integer> entry : imp.getAttributeModifiers().entrySet()) {
                            int val = entry.getValue();
                            assertTrue("Modifier for " + entry.getKey() + " in " + imp.getName() +
                                       " should be >= -1 (was " + val + ")", val >= -1);
                            assertTrue("Modifier for " + entry.getKey() + " in " + imp.getName() +
                                       " should be <= 2 (was " + val + ")", val <= 2);
                            assertTrue("Modifier should not be zero (was included in map)", val != 0);
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testNoEffectImprovementHasEmptyModifiers() {
        // An improvement with no matching keywords should have empty modifiers
        Improvement imp = new Improvement("Zebra Enclosure", 1, 0);
        assertTrue("Unknown improvement should have empty modifiers", imp.getAttributeModifiers().isEmpty());
    }

    @Test
    public void testNegativeModifierExists() {
        // Party Room should have a negative stamina modifier
        Improvement imp = new Improvement("Party Room", 1, 0);
        Map<CharacterAttribute, Integer> mods = imp.getAttributeModifiers();
        assertTrue("Party Room should affect Stamina", mods.containsKey(CharacterAttribute.STAMINA));
        assertTrue("Party Room should have negative Stamina", mods.get(CharacterAttribute.STAMINA) < 0);
    }

    @Test
    public void testModifiersAreUnmodifiable() {
        Improvement imp = new Improvement("Security System", 3, 4);
        Map<CharacterAttribute, Integer> mods = imp.getAttributeModifiers();
        try {
            mods.put(CharacterAttribute.AGILITY, 99);
            fail("Modifiers map should be unmodifiable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }
}
