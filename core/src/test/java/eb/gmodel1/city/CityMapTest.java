package eb.gmodel1.city;

import eb.gmodel1.character.*;


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

    @Test
    public void testFindFastestRouteSameCell() {
        CityMap map = new CityMap(12345L);
        CityMap.RouteResult result = map.findFastestRoute(0, 0, 0, 0);
        assertTrue("Same-cell route should be reachable", result.isReachable());
        assertEquals("Same-cell route should take 0 minutes", 0, result.totalMinutes);
        assertNotNull("Same-cell route should have a path", result.path);
        assertEquals("Same-cell path should have exactly 1 cell", 1, result.path.size());
    }

    @Test
    public void testFindFastestRouteReturnsDeterministicResult() {
        CityMap map1 = new CityMap(12345L);
        CityMap map2 = new CityMap(12345L);
        // Find any two connected building cells
        int[] start = findFirstBuildingCell(map1);
        int[] end   = findLastBuildingCell(map1);
        CityMap.RouteResult r1 = map1.findFastestRoute(start[0], start[1], end[0], end[1]);
        CityMap.RouteResult r2 = map2.findFastestRoute(start[0], start[1], end[0], end[1]);
        assertEquals("Route cost should be deterministic", r1.totalMinutes, r2.totalMinutes);
    }

    @Test
    public void testFindFastestRouteCostMultipleOfFive() {
        CityMap map = new CityMap(12345L);
        int[] start = findFirstBuildingCell(map);
        int[] end   = findLastBuildingCell(map);
        CityMap.RouteResult result = map.findFastestRoute(start[0], start[1], end[0], end[1]);
        if (result.isReachable()) {
            // All edge costs are multiples of 5 (ROAD=5, PATH=20)
            assertEquals("Travel time should be a multiple of 5",
                    0, result.totalMinutes % 5);
        }
    }

    @Test
    public void testFindFastestRoutePathContainsEndpoints() {
        CityMap map = new CityMap(12345L);
        int[] start = findFirstBuildingCell(map);
        int[] end   = findLastBuildingCell(map);
        CityMap.RouteResult result = map.findFastestRoute(start[0], start[1], end[0], end[1]);
        if (result.isReachable()) {
            int[] first = result.path.get(0);
            int[] last  = result.path.get(result.path.size() - 1);
            assertEquals("Path should start at fromX", start[0], first[0]);
            assertEquals("Path should start at fromY", start[1], first[1]);
            assertEquals("Path should end at toX", end[0], last[0]);
            assertEquals("Path should end at toY", end[1], last[1]);
        }
    }

    @Test
    public void testRouteResultFormatTime() {
        CityMap.RouteResult unreachable = new CityMap.RouteResult(null, -1);
        assertEquals("Unreachable result should say Unreachable", "Unreachable",
                unreachable.formatTime());

        CityMap.RouteResult shortRoute = new CityMap.RouteResult(null, 35);
        assertEquals("35 minutes should format as '35 min'", "35 min",
                shortRoute.formatTime());

        CityMap.RouteResult longRoute = new CityMap.RouteResult(null, 65);
        assertEquals("65 minutes should format as '1h 5min'", "1h 5min",
                longRoute.formatTime());
    }

    // --- helpers ---

    private int[] findFirstBuildingCell(CityMap map) {
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                if (map.getCell(x, y).getTerrainType() == TerrainType.BUILDING) {
                    return new int[]{x, y};
                }
            }
        }
        return new int[]{0, 0};
    }

    private int[] findLastBuildingCell(CityMap map) {
        int[] last = new int[]{0, 0};
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                if (map.getCell(x, y).getTerrainType() == TerrainType.BUILDING) {
                    last = new int[]{x, y};
                }
            }
        }
        return last;
    }

    // ===== Building state tests =====

    @Test
    public void testBuildingStateIsValidValue() {
        CityMap map = new CityMap(12345L);
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                Cell cell = map.getCell(x, y);
                if (cell.getTerrainType() == TerrainType.BUILDING && cell.hasBuilding()) {
                    String state = cell.getBuilding().getState();
                    assertTrue("Building state must be good, normal, or bad at (" + x + "," + y + ")",
                            "good".equals(state) || "normal".equals(state) || "bad".equals(state));
                }
            }
        }
    }

    @Test
    public void testComputeBuildingStateCentrePreferredGood() {
        // At the exact centre (7, 7) the probability of good is 60% — over many rolls
        // we expect noticeably more "good" than "bad".
        java.util.Random rng = new java.util.Random(99L);
        int good = 0, bad = 0;
        int trials = 1000;
        int cx = (CityMap.MAP_SIZE - 1) / 2;
        int cy = (CityMap.MAP_SIZE - 1) / 2;
        for (int i = 0; i < trials; i++) {
            String s = CityMap.computeBuildingState(rng, cx, cy);
            if ("good".equals(s)) good++;
            if ("bad".equals(s))  bad++;
        }
        assertTrue("Centre should produce more good than bad states (good=" + good + ", bad=" + bad + ")",
                good > bad);
    }

    @Test
    public void testComputeBuildingStateEdgePreferredBad() {
        // At corner (0, 0) the probability of bad is 60% — over many rolls
        // we expect noticeably more "bad" than "good".
        java.util.Random rng = new java.util.Random(99L);
        int good = 0, bad = 0;
        int trials = 1000;
        for (int i = 0; i < trials; i++) {
            String s = CityMap.computeBuildingState(rng, 0, 0);
            if ("good".equals(s)) good++;
            if ("bad".equals(s))  bad++;
        }
        assertTrue("Corner should produce more bad than good states (good=" + good + ", bad=" + bad + ")",
                bad > good);
    }

    @Test
    public void testBuildingStateDeterministicWithSeed() {
        // Same seed → same states across the whole map
        CityMap map1 = new CityMap(42L);
        CityMap map2 = new CityMap(42L);
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                Cell c1 = map1.getCell(x, y);
                Cell c2 = map2.getCell(x, y);
                if (c1.getTerrainType() == TerrainType.BUILDING && c1.hasBuilding()
                        && c2.hasBuilding()) {
                    assertEquals("Building state must be deterministic at (" + x + "," + y + ")",
                            c1.getBuilding().getState(), c2.getBuilding().getState());
                }
            }
        }
    }
}
