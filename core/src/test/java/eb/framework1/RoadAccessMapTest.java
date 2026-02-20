package eb.framework1;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.Queue;

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

    @Test
    public void testRemoveRoadsReducesCount() {
        CityMap map = new CityMap(12345L);
        RoadAccessMap accessMap = new RoadAccessMap(map);

        int initialRoads = accessMap.countRoads();
        accessMap.removeRoads(12345L);
        int remainingRoads = accessMap.countRoads();

        int removed = initialRoads - remainingRoads;
        int expectedRemoved = (int) Math.round(initialRoads * RoadAccessMap.ROAD_REMOVAL_PERCENTAGE);

        assertTrue("Should remove some roads (removed " + removed + " of " + initialRoads + ")",
                   removed > 0);
        assertTrue("Should remove close to target (removed " + removed + ", target " + expectedRemoved + ")",
                   removed >= expectedRemoved * 0.8);
        assertTrue("Should not remove more than target (removed " + removed + ", target " + expectedRemoved + ")",
                   removed <= expectedRemoved);
    }

    @Test
    public void testRemoveRoadsPreservesConnectivity() {
        long[] seeds = {12345L, 67890L, 11111L, 99999L};
        for (long seed : seeds) {
            CityMap map = new CityMap(seed);
            RoadAccessMap accessMap = new RoadAccessMap(map);

            // Record which cells have access before removal and their component IDs
            boolean[][] hadAccess = new boolean[CityMap.MAP_SIZE][CityMap.MAP_SIZE];
            for (int x = 0; x < CityMap.MAP_SIZE; x++) {
                for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                    RoadAccess ra = accessMap.getAccess(x, y);
                    hadAccess[x][y] = ra.hasNorth() || ra.hasSouth() || ra.hasEast() || ra.hasWest();
                }
            }

            // Compute component IDs before removal
            int[][] beforeIds = new int[CityMap.MAP_SIZE][CityMap.MAP_SIZE];
            for (int[] row : beforeIds) java.util.Arrays.fill(row, -1);
            int beforeComponents = 0;
            for (int x = 0; x < CityMap.MAP_SIZE; x++) {
                for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                    if (hadAccess[x][y] && beforeIds[x][y] == -1) {
                        bfsLabel(accessMap, x, y, beforeIds, beforeComponents);
                        beforeComponents++;
                    }
                }
            }

            accessMap.removeRoads(seed);

            // All cells that had access before should still have access
            for (int x = 0; x < CityMap.MAP_SIZE; x++) {
                for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                    if (hadAccess[x][y]) {
                        RoadAccess ra = accessMap.getAccess(x, y);
                        assertTrue("Cell (" + x + "," + y + ") should still have access (seed=" + seed + ")",
                                   ra.hasNorth() || ra.hasSouth() || ra.hasEast() || ra.hasWest());
                    }
                }
            }

            // Compute component IDs after removal
            int[][] afterIds = new int[CityMap.MAP_SIZE][CityMap.MAP_SIZE];
            for (int[] row : afterIds) java.util.Arrays.fill(row, -1);
            int afterComponents = 0;
            for (int x = 0; x < CityMap.MAP_SIZE; x++) {
                for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                    if (hadAccess[x][y] && afterIds[x][y] == -1) {
                        bfsLabel(accessMap, x, y, afterIds, afterComponents);
                        afterComponents++;
                    }
                }
            }

            // Verify no original component was split
            java.util.Map<Integer, Integer> mapping = new java.util.HashMap<>();
            for (int x = 0; x < CityMap.MAP_SIZE; x++) {
                for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                    if (hadAccess[x][y]) {
                        int origId = beforeIds[x][y];
                        int newId = afterIds[x][y];
                        assertTrue("Cell (" + x + "," + y + ") should be in a component (seed=" + seed + ")",
                                   newId >= 0);
                        Integer expected = mapping.get(origId);
                        if (expected == null) {
                            mapping.put(origId, newId);
                        } else {
                            assertEquals("Cells in same original component should remain connected (seed=" + seed + ")",
                                         expected.intValue(), newId);
                        }
                    }
                }
            }
        }
    }

    /**
     * Helper: BFS to label connected cells with a component ID.
     */
    private void bfsLabel(RoadAccessMap accessMap, int startX, int startY, int[][] ids, int componentId) {
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{startX, startY});
        ids[startX][startY] = componentId;

        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int x = cell[0], y = cell[1];
            RoadAccess ra = accessMap.getAccess(x, y);

            if (ra.hasNorth() && y + 1 < CityMap.MAP_SIZE && ids[x][y + 1] == -1) {
                ids[x][y + 1] = componentId;
                queue.add(new int[]{x, y + 1});
            }
            if (ra.hasSouth() && y - 1 >= 0 && ids[x][y - 1] == -1) {
                ids[x][y - 1] = componentId;
                queue.add(new int[]{x, y - 1});
            }
            if (ra.hasEast() && x + 1 < CityMap.MAP_SIZE && ids[x + 1][y] == -1) {
                ids[x + 1][y] = componentId;
                queue.add(new int[]{x + 1, y});
            }
            if (ra.hasWest() && x - 1 >= 0 && ids[x - 1][y] == -1) {
                ids[x - 1][y] = componentId;
                queue.add(new int[]{x - 1, y});
            }
        }
    }

    @Test
    public void testRemoveRoadsDeterministic() {
        CityMap map1 = new CityMap(12345L);
        CityMap map2 = new CityMap(12345L);
        RoadAccessMap accessMap1 = new RoadAccessMap(map1);
        RoadAccessMap accessMap2 = new RoadAccessMap(map2);

        accessMap1.removeRoads(12345L);
        accessMap2.removeRoads(12345L);

        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                RoadAccess ra1 = accessMap1.getAccess(x, y);
                RoadAccess ra2 = accessMap2.getAccess(x, y);
                assertEquals("N access should match at (" + x + "," + y + ") after removal",
                             ra1.hasNorth(), ra2.hasNorth());
                assertEquals("S access should match at (" + x + "," + y + ") after removal",
                             ra1.hasSouth(), ra2.hasSouth());
                assertEquals("E access should match at (" + x + "," + y + ") after removal",
                             ra1.hasEast(), ra2.hasEast());
                assertEquals("W access should match at (" + x + "," + y + ") after removal",
                             ra1.hasWest(), ra2.hasWest());
            }
        }
    }

    @Test
    public void testRemoveRoadsReciprocalMaintained() {
        CityMap map = new CityMap(12345L);
        RoadAccessMap accessMap = new RoadAccessMap(map);
        accessMap.removeRoads(12345L);

        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                RoadAccess ra = accessMap.getAccess(x, y);

                if (!ra.hasNorth() && y + 1 < CityMap.MAP_SIZE) {
                    assertFalse("Reciprocal N/S at (" + x + "," + (y + 1) + ")",
                                accessMap.getAccess(x, y + 1).hasSouth());
                }
                if (!ra.hasSouth() && y - 1 >= 0) {
                    assertFalse("Reciprocal S/N at (" + x + "," + (y - 1) + ")",
                                accessMap.getAccess(x, y - 1).hasNorth());
                }
                if (!ra.hasEast() && x + 1 < CityMap.MAP_SIZE) {
                    assertFalse("Reciprocal E/W at (" + (x + 1) + "," + y + ")",
                                accessMap.getAccess(x + 1, y).hasWest());
                }
                if (!ra.hasWest() && x - 1 >= 0) {
                    assertFalse("Reciprocal W/E at (" + (x - 1) + "," + y + ")",
                                accessMap.getAccess(x - 1, y).hasEast());
                }
            }
        }
    }
}
