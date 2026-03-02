package eb.gmodel1.city;

import eb.gmodel1.save.*;
import eb.gmodel1.ui.*;


import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for CellRenderData and CityMap render data pre-computation.
 */
public class CellRenderDataTest {

    @Test
    public void testCellRenderDataCreation() {
        CellRenderData rd = new CellRenderData(3, 5, 1, 1, 0.5f, 0.6f, 0.7f, 1.0f,
                                                RoadType.ROAD, RoadType.NONE, RoadType.PATHWAY, RoadType.NONE,
                                                "icons/test.png");
        assertEquals(3, rd.getX());
        assertEquals(5, rd.getY());
        assertEquals(1, rd.getWidth());
        assertEquals(1, rd.getHeight());
        assertEquals(0.5f, rd.getR(), 0.001f);
        assertEquals(0.6f, rd.getG(), 0.001f);
        assertEquals(0.7f, rd.getB(), 0.001f);
        assertEquals(1.0f, rd.getA(), 0.001f);
        assertTrue("Should have north border", rd.hasBorderNorth());
        assertFalse("Should not have south border", rd.hasBorderSouth());
        assertTrue("Should have east border", rd.hasBorderEast());
        assertFalse("Should not have west border", rd.hasBorderWest());
        assertEquals("North should be ROAD", RoadType.ROAD, rd.getBorderTypeNorth());
        assertEquals("South should be NONE", RoadType.NONE, rd.getBorderTypeSouth());
        assertEquals("East should be PATHWAY", RoadType.PATHWAY, rd.getBorderTypeEast());
        assertEquals("West should be NONE", RoadType.NONE, rd.getBorderTypeWest());
    }

    @Test
    public void testCellRenderDataToString() {
        CellRenderData rd = new CellRenderData(0, 0, 1, 1, 0.4f, 0.35f, 0.3f, 1.0f,
                                                RoadType.ROAD, RoadType.ROAD, RoadType.NONE, RoadType.NONE,
                                                null);
        String str = rd.toString();
        assertNotNull(str);
        assertTrue(str.contains("x=0"));
        assertTrue(str.contains("y=0"));
        assertTrue("toString should include border info", str.contains("borders"));
    }

    @Test
    public void testRenderDataExistsForAllCells() {
        CityMap map = new CityMap(12345L);
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                CellRenderData rd = map.getCellRenderData(x, y);
                assertNotNull("Render data should exist at (" + x + "," + y + ")", rd);
                assertEquals("Render data x should match", x, rd.getX());
                assertEquals("Render data y should match", y, rd.getY());
                assertEquals("Render data width should be 1", 1, rd.getWidth());
                assertEquals("Render data height should be 1", 1, rd.getHeight());
                assertEquals("Alpha should be 1.0", 1.0f, rd.getA(), 0.001f);
            }
        }
    }

    @Test
    public void testRenderDataDeterministic() {
        CityMap map1 = new CityMap(12345L);
        CityMap map2 = new CityMap(12345L);

        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                CellRenderData rd1 = map1.getCellRenderData(x, y);
                CellRenderData rd2 = map2.getCellRenderData(x, y);
                assertEquals("Color R should match at (" + x + "," + y + ")",
                             rd1.getR(), rd2.getR(), 0.001f);
                assertEquals("Color G should match at (" + x + "," + y + ")",
                             rd1.getG(), rd2.getG(), 0.001f);
                assertEquals("Color B should match at (" + x + "," + y + ")",
                             rd1.getB(), rd2.getB(), 0.001f);
            }
        }
    }

    @Test
    public void testMountainCellsHaveCorrectColor() {
        CityMap map = new CityMap(12345L);
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                Cell cell = map.getCell(x, y);
                if (cell.getTerrainType() == TerrainType.MOUNTAIN) {
                    CellRenderData rd = map.getCellRenderData(x, y);
                    assertEquals("Mountain R", 0.4f, rd.getR(), 0.001f);
                    assertEquals("Mountain G", 0.35f, rd.getG(), 0.001f);
                    assertEquals("Mountain B", 0.3f, rd.getB(), 0.001f);
                }
            }
        }
    }

    @Test
    public void testBeachCellsHaveCorrectColor() {
        CityMap map = new CityMap(12345L);
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                Cell cell = map.getCell(x, y);
                if (cell.getTerrainType() == TerrainType.BEACH) {
                    CellRenderData rd = map.getCellRenderData(x, y);
                    assertEquals("Beach R", 0.95f, rd.getR(), 0.001f);
                    assertEquals("Beach G", 0.9f, rd.getG(), 0.001f);
                    assertEquals("Beach B", 0.6f, rd.getB(), 0.001f);
                }
            }
        }
    }

    @Test
    public void testFallbackBuildingCellsHaveGrayColor() {
        // Without GameDataManager, building cells use fallback gray color
        CityMap map = new CityMap(12345L);
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                Cell cell = map.getCell(x, y);
                if (cell.getTerrainType() == TerrainType.BUILDING) {
                    CellRenderData rd = map.getCellRenderData(x, y);
                    assertEquals("Fallback building R", 0.5f, rd.getR(), 0.001f);
                    assertEquals("Fallback building G", 0.5f, rd.getG(), 0.001f);
                    assertEquals("Fallback building B", 0.5f, rd.getB(), 0.001f);
                }
            }
        }
    }

    @Test
    public void testRenderDataColorComponentsInRange() {
        CityMap map = new CityMap(12345L);
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                CellRenderData rd = map.getCellRenderData(x, y);
                assertTrue("R should be >= 0", rd.getR() >= 0.0f);
                assertTrue("R should be <= 1", rd.getR() <= 1.0f);
                assertTrue("G should be >= 0", rd.getG() >= 0.0f);
                assertTrue("G should be <= 1", rd.getG() <= 1.0f);
                assertTrue("B should be >= 0", rd.getB() >= 0.0f);
                assertTrue("B should be <= 1", rd.getB() <= 1.0f);
                assertTrue("A should be >= 0", rd.getA() >= 0.0f);
                assertTrue("A should be <= 1", rd.getA() <= 1.0f);
            }
        }
    }

    @Test
    public void testRenderDataBoundsCheck() {
        CityMap map = new CityMap(12345L);
        try {
            map.getCellRenderData(-1, 0);
            fail("Should throw exception for negative x");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            map.getCellRenderData(0, -1);
            fail("Should throw exception for negative y");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            map.getCellRenderData(16, 0);
            fail("Should throw exception for x >= MAP_SIZE");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            map.getCellRenderData(0, 16);
            fail("Should throw exception for y >= MAP_SIZE");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testMountainCellsHaveNoBorders() {
        // Mountains have no road access, so all borders should be false
        CityMap map = new CityMap(12345L);
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                Cell cell = map.getCell(x, y);
                if (cell.getTerrainType() == TerrainType.MOUNTAIN) {
                    CellRenderData rd = map.getCellRenderData(x, y);
                    assertFalse("Mountain should have no north border at (" + x + "," + y + ")",
                                rd.hasBorderNorth());
                    assertFalse("Mountain should have no south border at (" + x + "," + y + ")",
                                rd.hasBorderSouth());
                    assertFalse("Mountain should have no east border at (" + x + "," + y + ")",
                                rd.hasBorderEast());
                    assertFalse("Mountain should have no west border at (" + x + "," + y + ")",
                                rd.hasBorderWest());
                }
            }
        }
    }

    @Test
    public void testBorderFlagsMatchRoadAccess() {
        // Border types should match the road access map
        CityMap map = new CityMap(12345L);
        RoadAccessMap roadMap = map.getRoadAccessMap();
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                CellRenderData rd = map.getCellRenderData(x, y);
                RoadAccess ra = roadMap.getAccess(x, y);
                assertEquals("North border type should match road access at (" + x + "," + y + ")",
                             ra.getNorthType(), rd.getBorderTypeNorth());
                assertEquals("South border type should match road access at (" + x + "," + y + ")",
                             ra.getSouthType(), rd.getBorderTypeSouth());
                assertEquals("East border type should match road access at (" + x + "," + y + ")",
                             ra.getEastType(), rd.getBorderTypeEast());
                assertEquals("West border type should match road access at (" + x + "," + y + ")",
                             ra.getWestType(), rd.getBorderTypeWest());
            }
        }
    }

    @Test
    public void testBuildingCellsHaveAtLeastOneBorder() {
        // Building cells should have at least one road access (connectivity preserved)
        CityMap map = new CityMap(12345L);
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                Cell cell = map.getCell(x, y);
                if (cell.getTerrainType() == TerrainType.BUILDING) {
                    CellRenderData rd = map.getCellRenderData(x, y);
                    boolean hasAnyBorder = rd.hasBorderNorth() || rd.hasBorderSouth()
                                        || rd.hasBorderEast() || rd.hasBorderWest();
                    assertTrue("Building at (" + x + "," + y + ") should have at least one border",
                               hasAnyBorder);
                }
            }
        }
    }

    @Test
    public void testPathwaysExistBetweenBuildings() {
        // After road removal, some building-to-building connections should be pathways
        CityMap map = new CityMap(12345L);
        RoadAccessMap roadMap = map.getRoadAccessMap();
        int pathwayCount = 0;
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                RoadAccess ra = roadMap.getAccess(x, y);
                if (ra.getNorthType() == RoadType.PATHWAY) pathwayCount++;
                if (ra.getEastType() == RoadType.PATHWAY) pathwayCount++;
            }
        }
        assertTrue("Should have at least one pathway after road removal", pathwayCount > 0);
    }

    @Test
    public void testPathwaysOnlyBetweenBuildings() {
        // Pathways should only exist between two building cells
        CityMap map = new CityMap(12345L);
        RoadAccessMap roadMap = map.getRoadAccessMap();
        for (int x = 0; x < CityMap.MAP_SIZE; x++) {
            for (int y = 0; y < CityMap.MAP_SIZE; y++) {
                RoadAccess ra = roadMap.getAccess(x, y);
                if (ra.getEastType() == RoadType.PATHWAY && x + 1 < CityMap.MAP_SIZE) {
                    assertEquals("Pathway east: cell at (" + x + "," + y + ") should be BUILDING",
                                 TerrainType.BUILDING, map.getCell(x, y).getTerrainType());
                    assertEquals("Pathway east: cell at (" + (x+1) + "," + y + ") should be BUILDING",
                                 TerrainType.BUILDING, map.getCell(x + 1, y).getTerrainType());
                }
                if (ra.getNorthType() == RoadType.PATHWAY && y + 1 < CityMap.MAP_SIZE) {
                    assertEquals("Pathway north: cell at (" + x + "," + y + ") should be BUILDING",
                                 TerrainType.BUILDING, map.getCell(x, y).getTerrainType());
                    assertEquals("Pathway north: cell at (" + x + "," + (y+1) + ") should be BUILDING",
                                 TerrainType.BUILDING, map.getCell(x, y + 1).getTerrainType());
                }
            }
        }
    }
}
