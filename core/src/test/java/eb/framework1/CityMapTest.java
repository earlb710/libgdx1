package eb.framework1;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for CityMap generation.
 */
public class CityMapTest {
    
    @Test
    public void testMapSize() {
        CityMap map = new CityMap(12345L);
        assertEquals("Map size should be 16", 16, map.getSize());
    }
    
    @Test
    public void testDeterministicGeneration() {
        // Same seed should produce same map
        CityMap map1 = new CityMap(12345L);
        CityMap map2 = new CityMap(12345L);
        
        assertEquals("Same seed should produce same beach side", 
                     map1.getBeachSide(), map2.getBeachSide());
        
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                Cell c1 = map1.getCell(x, y);
                Cell c2 = map2.getCell(x, y);
                assertEquals("Cell terrain should match at (" + x + "," + y + ")",
                             c1.getTerrainType(), c2.getTerrainType());
            }
        }
    }
    
    @Test
    public void testBeachSide() {
        CityMap map = new CityMap(12345L);
        CityMap.Side beachSide = map.getBeachSide();
        assertNotNull("Beach side should not be null", beachSide);
        
        int beachCount = map.countTerrain(TerrainType.BEACH);
        assertEquals("Beach should occupy one full side (16 cells)", 16, beachCount);
    }
    
    @Test
    public void testMountainPercentage() {
        CityMap map = new CityMap(12345L);
        int mountainCount = map.countTerrain(TerrainType.MOUNTAIN);
        
        // 10% of non-beach cells (256 - 16 = 240 available cells)
        // 10% of 240 = 24 mountains (approximately)
        int availableCells = CityMap.MAP_SIZE * CityMap.MAP_SIZE - CityMap.MAP_SIZE;
        int expectedMountains = (int) Math.round(availableCells * 0.10);
        
        assertEquals("Mountain count should be approximately 10% of non-beach cells",
                     expectedMountains, mountainCount);
    }
    
    @Test
    public void testMountainsNotAccessible() {
        CityMap map = new CityMap(12345L);
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                Cell cell = map.getCell(x, y);
                if (cell.getTerrainType() == TerrainType.MOUNTAIN) {
                    assertFalse("Mountain cells should not be accessible", 
                                cell.isAccessible());
                }
            }
        }
    }
    
    @Test
    public void testBuildingsHaveFourImprovements() {
        CityMap map = new CityMap(12345L);
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                Cell cell = map.getCell(x, y);
                if (cell.getTerrainType() == TerrainType.BUILDING) {
                    assertTrue("Building cells should have a building", cell.hasBuilding());
                    Building building = cell.getBuilding();
                    assertNotNull("Building should not be null", building);
                    assertEquals("Each building should have 4 improvements", 
                                 4, building.getImprovements().size());
                }
            }
        }
    }
    
    @Test
    public void testNonBuildingCellsHaveNoBuilding() {
        CityMap map = new CityMap(12345L);
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                Cell cell = map.getCell(x, y);
                if (cell.getTerrainType() == TerrainType.MOUNTAIN || 
                    cell.getTerrainType() == TerrainType.BEACH) {
                    assertNull("Non-building cells should not have buildings", 
                               cell.getBuilding());
                }
            }
        }
    }
    
    @Test
    public void testProfileConstructor() {
        Profile profile = new Profile("Test", "Male", "Normal");
        CityMap map = new CityMap(profile);
        
        assertEquals("Map seed should match profile seed", 
                     profile.getRandSeed(), map.getSeed());
    }
    
    @Test
    public void testCellCoordinateBounds() {
        CityMap map = new CityMap(12345L);
        
        try {
            map.getCell(-1, 0);
            fail("Should throw exception for negative x");
        } catch (IllegalArgumentException e) {
            // Expected
        }
        
        try {
            map.getCell(0, -1);
            fail("Should throw exception for negative y");
        } catch (IllegalArgumentException e) {
            // Expected
        }
        
        try {
            map.getCell(16, 0);
            fail("Should throw exception for x >= MAP_SIZE");
        } catch (IllegalArgumentException e) {
            // Expected
        }
        
        try {
            map.getCell(0, 16);
            fail("Should throw exception for y >= MAP_SIZE");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
    
    @Test
    public void testAsciiMap() {
        CityMap map = new CityMap(12345L);
        String asciiMap = map.toAsciiMap();
        
        assertNotNull("ASCII map should not be null", asciiMap);
        assertFalse("ASCII map should not be empty", asciiMap.isEmpty());
        
        // Count characters (M, B, #) and newlines
        int mCount = 0, bCount = 0, hashCount = 0, newlineCount = 0;
        for (char c : asciiMap.toCharArray()) {
            if (c == 'M') mCount++;
            else if (c == 'B') bCount++;
            else if (c == '#') hashCount++;
            else if (c == '\n') newlineCount++;
        }
        
        assertEquals("ASCII map should have correct mountain count", 
                     map.countTerrain(TerrainType.MOUNTAIN), mCount);
        assertEquals("ASCII map should have correct beach count", 
                     map.countTerrain(TerrainType.BEACH), bCount);
        assertEquals("ASCII map should have correct building count", 
                     map.countTerrain(TerrainType.BUILDING), hashCount);
        assertEquals("ASCII map should have 16 rows", 16, newlineCount);
    }
}
