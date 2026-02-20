package eb.framework1;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for RoadAccessMap.
 */
public class RoadAccessMapTest {

    @Test
    public void testMountainsHaveNoAccess() {
        CityMap map = new CityMap(12345L);
        RoadAccessMap accessMap = new RoadAccessMap(map);

        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                if (map.getCell(x, y).getTerrainType() == TerrainType.MOUNTAIN) {
                    RoadAccess ra = accessMap.getAccess(x, y);
                    assertFalse("Mountain at (" + x + "," + y + ") should not have North access", ra.hasNorth());
                    assertFalse("Mountain at (" + x + "," + y + ") should not have South access", ra.hasSouth());
                    assertFalse("Mountain at (" + x + "," + y + ") should not have East access", ra.hasEast());
                    assertFalse("Mountain at (" + x + "," + y + ") should not have West access", ra.hasWest());
                }
            }
        }
    }

    @Test
    public void testBeachOnlyAccessTowardNonBeach() {
        CityMap map = new CityMap(12345L);
        RoadAccessMap accessMap = new RoadAccessMap(map);

        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                if (map.getCell(x, y).getTerrainType() == TerrainType.BEACH) {
                    RoadAccess ra = accessMap.getAccess(x, y);

                    // Check North
                    if (y + 1 < CityMap.MAP_SIZE && map.getCell(x, y + 1).getTerrainType() == TerrainType.BEACH) {
                        assertFalse("Beach at (" + x + "," + y + ") should not have North access toward beach",
                                    ra.hasNorth());
                    }
                    // Check South
                    if (y - 1 >= 0 && map.getCell(x, y - 1).getTerrainType() == TerrainType.BEACH) {
                        assertFalse("Beach at (" + x + "," + y + ") should not have South access toward beach",
                                    ra.hasSouth());
                    }
                    // Check East
                    if (x + 1 < CityMap.MAP_SIZE && map.getCell(x + 1, y).getTerrainType() == TerrainType.BEACH) {
                        assertFalse("Beach at (" + x + "," + y + ") should not have East access toward beach",
                                    ra.hasEast());
                    }
                    // Check West
                    if (x - 1 >= 0 && map.getCell(x - 1, y).getTerrainType() == TerrainType.BEACH) {
                        assertFalse("Beach at (" + x + "," + y + ") should not have West access toward beach",
                                    ra.hasWest());
                    }

                    // Beach at map edge should not have access toward outside
                    if (y + 1 >= CityMap.MAP_SIZE) {
                        assertFalse("Beach at (" + x + "," + y + ") should not have North access at edge",
                                    ra.hasNorth());
                    }
                    if (y - 1 < 0) {
                        assertFalse("Beach at (" + x + "," + y + ") should not have South access at edge",
                                    ra.hasSouth());
                    }
                    if (x + 1 >= CityMap.MAP_SIZE) {
                        assertFalse("Beach at (" + x + "," + y + ") should not have East access at edge",
                                    ra.hasEast());
                    }
                    if (x - 1 < 0) {
                        assertFalse("Beach at (" + x + "," + y + ") should not have West access at edge",
                                    ra.hasWest());
                    }
                }
            }
        }
    }

    @Test
    public void testReciprocalConstraint() {
        CityMap map = new CityMap(12345L);
        RoadAccessMap accessMap = new RoadAccessMap(map);

        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                RoadAccess ra = accessMap.getAccess(x, y);

                // If no North access, neighbor to North must not have South access
                if (!ra.hasNorth() && y + 1 < CityMap.MAP_SIZE) {
                    assertFalse("Reciprocal: cell (" + x + "," + (y + 1) + ") should not have South access",
                                accessMap.getAccess(x, y + 1).hasSouth());
                }
                // If no South access, neighbor to South must not have North access
                if (!ra.hasSouth() && y - 1 >= 0) {
                    assertFalse("Reciprocal: cell (" + x + "," + (y - 1) + ") should not have North access",
                                accessMap.getAccess(x, y - 1).hasNorth());
                }
                // If no East access, neighbor to East must not have West access
                if (!ra.hasEast() && x + 1 < CityMap.MAP_SIZE) {
                    assertFalse("Reciprocal: cell (" + (x + 1) + "," + y + ") should not have West access",
                                accessMap.getAccess(x + 1, y).hasWest());
                }
                // If no West access, neighbor to West must not have East access
                if (!ra.hasWest() && x - 1 >= 0) {
                    assertFalse("Reciprocal: cell (" + (x - 1) + "," + y + ") should not have East access",
                                accessMap.getAccess(x - 1, y).hasEast());
                }
            }
        }
    }

    @Test
    public void testSetAccessPropagatesReciprocal() {
        CityMap map = new CityMap(12345L);
        RoadAccessMap accessMap = new RoadAccessMap(map);

        // Find a building cell not on the edge with a neighbor that has reciprocal access
        for (int x = 1; x < CityMap.MAP_SIZE - 1; x++) {
            for (int y = 1; y < CityMap.MAP_SIZE - 1; y++) {
                if (map.getCell(x, y).getTerrainType() == TerrainType.BUILDING
                    && map.getCell(x + 1, y).getTerrainType() == TerrainType.BUILDING) {
                    // Both should initially have access toward each other
                    assertTrue("Building at (" + x + "," + y + ") should have East access",
                               accessMap.getAccess(x, y).hasEast());
                    assertTrue("Building at (" + (x + 1) + "," + y + ") should have West access",
                               accessMap.getAccess(x + 1, y).hasWest());

                    // Remove East access from (x, y)
                    accessMap.setAccess(x, y, RoadAccessMap.Direction.EAST, false);

                    // Verify propagation: neighbor's West access should also be removed
                    assertFalse("After removing East access at (" + x + "," + y + "), " +
                                "cell (" + (x + 1) + "," + y + ") should lose West access",
                                accessMap.getAccess(x + 1, y).hasWest());
                    return; // Test passed with first found pair
                }
            }
        }
        fail("Could not find two adjacent building cells for test");
    }

    @Test
    public void testBuildingNextToMountainLosesAccess() {
        CityMap map = new CityMap(12345L);
        RoadAccessMap accessMap = new RoadAccessMap(map);

        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                if (map.getCell(x, y).getTerrainType() == TerrainType.MOUNTAIN) {
                    // Check all neighbors of the mountain
                    if (y + 1 < CityMap.MAP_SIZE) {
                        assertFalse("Cell north of mountain at (" + x + "," + y + ") should not have South access",
                                    accessMap.getAccess(x, y + 1).hasSouth());
                    }
                    if (y - 1 >= 0) {
                        assertFalse("Cell south of mountain at (" + x + "," + y + ") should not have North access",
                                    accessMap.getAccess(x, y - 1).hasNorth());
                    }
                    if (x + 1 < CityMap.MAP_SIZE) {
                        assertFalse("Cell east of mountain at (" + x + "," + y + ") should not have West access",
                                    accessMap.getAccess(x + 1, y).hasWest());
                    }
                    if (x - 1 >= 0) {
                        assertFalse("Cell west of mountain at (" + x + "," + y + ") should not have East access",
                                    accessMap.getAccess(x - 1, y).hasEast());
                    }
                }
            }
        }
    }

    @Test
    public void testEdgeCellsNoAccessOutside() {
        CityMap map = new CityMap(12345L);
        RoadAccessMap accessMap = new RoadAccessMap(map);

        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            assertFalse("Cell at (" + x + ",0) should not have South access",
                        accessMap.getAccess(x, 0).hasSouth());
            assertFalse("Cell at (" + x + "," + (CityMap.MAP_SIZE - 1) + ") should not have North access",
                        accessMap.getAccess(x, CityMap.MAP_SIZE - 1).hasNorth());
        }
        for (int y = 0; y < CityMap.MAP_SIZE; y++) {
            assertFalse("Cell at (0," + y + ") should not have West access",
                        accessMap.getAccess(0, y).hasWest());
            assertFalse("Cell at (" + (CityMap.MAP_SIZE - 1) + "," + y + ") should not have East access",
                        accessMap.getAccess(CityMap.MAP_SIZE - 1, y).hasEast());
        }
    }

    @Test
    public void testAccessMapSize() {
        CityMap map = new CityMap(12345L);
        RoadAccessMap accessMap = new RoadAccessMap(map);
        assertEquals("Access map size should match city map size", CityMap.MAP_SIZE, accessMap.getSize());
    }

    @Test
    public void testGetAccessOutOfBounds() {
        CityMap map = new CityMap(12345L);
        RoadAccessMap accessMap = new RoadAccessMap(map);

        try {
            accessMap.getAccess(-1, 0);
            fail("Should throw exception for negative x");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            accessMap.getAccess(0, -1);
            fail("Should throw exception for negative y");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            accessMap.getAccess(CityMap.MAP_SIZE, 0);
            fail("Should throw exception for x >= size");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testSetAccessOutOfBounds() {
        CityMap map = new CityMap(12345L);
        RoadAccessMap accessMap = new RoadAccessMap(map);

        try {
            accessMap.setAccess(-1, 0, RoadAccessMap.Direction.NORTH, false);
            fail("Should throw exception for negative x");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testDeterministicAccessMap() {
        CityMap map1 = new CityMap(12345L);
        CityMap map2 = new CityMap(12345L);
        RoadAccessMap accessMap1 = new RoadAccessMap(map1);
        RoadAccessMap accessMap2 = new RoadAccessMap(map2);

        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                RoadAccess ra1 = accessMap1.getAccess(x, y);
                RoadAccess ra2 = accessMap2.getAccess(x, y);
                assertEquals("N access should match at (" + x + "," + y + ")", ra1.hasNorth(), ra2.hasNorth());
                assertEquals("S access should match at (" + x + "," + y + ")", ra1.hasSouth(), ra2.hasSouth());
                assertEquals("E access should match at (" + x + "," + y + ")", ra1.hasEast(), ra2.hasEast());
                assertEquals("W access should match at (" + x + "," + y + ")", ra1.hasWest(), ra2.hasWest());
            }
        }
    }
}
