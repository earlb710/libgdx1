package eb.framework1;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for BuildingEffects and Building attribute modifiers.
 */
public class BuildingEffectsTest {

    private Building createTestBuilding(String name) {
        List<Improvement> imps = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            imps.add(new Improvement("Test Imp " + i, 1, 0));
        }
        return new Building(name, imps);
    }

    @Test
    public void testBuildingHasAttributeModifiers() {
        Building b = createTestBuilding("Police Station-1,1");
        assertNotNull("Building should have attribute modifiers", b.getAttributeModifiers());
        assertFalse("Police Station should have non-empty modifiers", b.getAttributeModifiers().isEmpty());
    }

    @Test
    public void testPoliceStationHasPerceptionAndIntimidation() {
        Map<CharacterAttribute, Integer> mods = BuildingEffects.getEffects("Police Station");
        assertTrue("Police Station should affect Perception", mods.containsKey(CharacterAttribute.PERCEPTION));
        assertTrue("Police Station should affect Intimidation", mods.containsKey(CharacterAttribute.INTIMIDATION));
    }

    @Test
    public void testLibraryHasIntelligenceAndMemory() {
        Map<CharacterAttribute, Integer> mods = BuildingEffects.getEffects("Public Library");
        assertTrue("Library should affect Intelligence", mods.containsKey(CharacterAttribute.INTELLIGENCE));
        assertTrue("Library should affect Memory", mods.containsKey(CharacterAttribute.MEMORY));
    }

    @Test
    public void testHospitalHasIntelligenceAndEmpathy() {
        Map<CharacterAttribute, Integer> mods = BuildingEffects.getEffects("Small Hospital");
        assertTrue("Hospital should affect Intelligence", mods.containsKey(CharacterAttribute.INTELLIGENCE));
        assertTrue("Hospital should affect Empathy", mods.containsKey(CharacterAttribute.EMPATHY));
    }

    @Test
    public void testFireStationHasStrengthAndStamina() {
        Map<CharacterAttribute, Integer> mods = BuildingEffects.getEffects("Fire Station");
        assertTrue("Fire Station should affect Strength", mods.containsKey(CharacterAttribute.STRENGTH));
        assertTrue("Fire Station should affect Stamina", mods.containsKey(CharacterAttribute.STAMINA));
    }

    @Test
    public void testModifiersClampedToRange() {
        CityMap map = new CityMap(12345L);
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                Cell cell = map.getCell(x, y);
                if (cell.hasBuilding()) {
                    Building building = cell.getBuilding();
                    for (Map.Entry<CharacterAttribute, Integer> entry : building.getAttributeModifiers().entrySet()) {
                        int val = entry.getValue();
                        assertTrue("Modifier for " + entry.getKey() + " in " + building.getName() +
                                   " should be >= -3 (was " + val + ")", val >= -3);
                        assertTrue("Modifier for " + entry.getKey() + " in " + building.getName() +
                                   " should be <= 3 (was " + val + ")", val <= 3);
                        assertTrue("Modifier should not be zero", val != 0);
                    }
                }
            }
        }
    }

    @Test
    public void testUnknownBuildingHasEmptyModifiers() {
        Map<CharacterAttribute, Integer> mods = BuildingEffects.getEffects("Zebra Enclosure");
        assertTrue("Unknown building should have empty modifiers", mods.isEmpty());
    }

    @Test
    public void testNegativeModifierExists() {
        Map<CharacterAttribute, Integer> mods = BuildingEffects.getEffects("Fast Food Restaurant");
        assertTrue("Fast Food should affect Stamina", mods.containsKey(CharacterAttribute.STAMINA));
        assertTrue("Fast Food should have negative Stamina", mods.get(CharacterAttribute.STAMINA) < 0);
    }

    @Test
    public void testNullNameReturnsEmpty() {
        Map<CharacterAttribute, Integer> mods = BuildingEffects.getEffects(null);
        assertTrue("Null name should return empty modifiers", mods.isEmpty());
    }

    @Test
    public void testEmptyNameReturnsEmpty() {
        Map<CharacterAttribute, Integer> mods = BuildingEffects.getEffects("");
        assertTrue("Empty name should return empty modifiers", mods.isEmpty());
    }

    @Test
    public void testModifiersAreUnmodifiable() {
        Building b = createTestBuilding("Police Station-1,1");
        Map<CharacterAttribute, Integer> mods = b.getAttributeModifiers();
        try {
            mods.put(CharacterAttribute.AGILITY, 99);
            fail("Modifiers map should be unmodifiable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }
}
