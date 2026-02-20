package eb.framework1;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Improvement discovery and hidden value.
 */
public class ImprovementTest {

    @Test
    public void testImprovementNotDiscoveredByDefault() {
        Improvement imp = new Improvement("Solar Panels", 3, 5);
        assertFalse("Improvement should not be discovered by default", imp.isDiscovered());
    }

    @Test
    public void testDiscoverImprovement() {
        Improvement imp = new Improvement("Solar Panels", 3, 5);
        assertFalse(imp.isDiscovered());
        imp.discover();
        assertTrue("Improvement should be discovered after calling discover()", imp.isDiscovered());
    }

    @Test
    public void testHiddenValueStored() {
        Improvement imp = new Improvement("Solar Panels", 3, 7);
        assertEquals("Hidden value should be 7", 7, imp.getHiddenValue());
    }

    @Test
    public void testHiddenValueZero() {
        Improvement imp = new Improvement("Garden", 1, 0);
        assertEquals("Hidden value should be 0", 0, imp.getHiddenValue());
        assertFalse("Even with hiddenValue 0, improvement starts undiscovered", imp.isDiscovered());
    }

    @Test
    public void testHiddenValueTen() {
        Improvement imp = new Improvement("Secret Vault", 5, 10);
        assertEquals("Hidden value should be 10", 10, imp.getHiddenValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHiddenValueBelowZero() {
        new Improvement("Invalid", 1, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHiddenValueAboveTen() {
        new Improvement("Invalid", 1, 11);
    }

    @Test
    public void testNameAndLevelPreserved() {
        Improvement imp = new Improvement("HVAC System", 4, 3);
        assertEquals("HVAC System", imp.getName());
        assertEquals(4, imp.getLevel());
    }

    @Test
    public void testToStringIncludesHiddenAndDiscovered() {
        Improvement imp = new Improvement("Elevator", 2, 6);
        String str = imp.toString();
        assertTrue("toString should include hiddenValue", str.contains("hiddenValue=6"));
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
        // Verify via CityMap that all generated improvements have hiddenValue 0-10
        CityMap map = new CityMap(12345L);
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                Cell cell = map.getCell(x, y);
                if (cell.hasBuilding()) {
                    for (Improvement imp : cell.getBuilding().getImprovements()) {
                        assertTrue("Hidden value should be >= 0 at (" + x + "," + y + ")",
                                   imp.getHiddenValue() >= 0);
                        assertTrue("Hidden value should be <= 10 at (" + x + "," + y + ")",
                                   imp.getHiddenValue() <= 10);
                    }
                }
            }
        }
    }
}
