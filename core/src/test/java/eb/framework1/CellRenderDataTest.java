package eb.framework1;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for CellRenderData and CityMap render data pre-computation.
 */
public class CellRenderDataTest {

    @Test
    public void testCellRenderDataCreation() {
        CellRenderData rd = new CellRenderData(3, 5, 1, 1, 0.5f, 0.6f, 0.7f, 1.0f);
        assertEquals(3, rd.getX());
        assertEquals(5, rd.getY());
        assertEquals(1, rd.getWidth());
        assertEquals(1, rd.getHeight());
        assertEquals(0.5f, rd.getR(), 0.001f);
        assertEquals(0.6f, rd.getG(), 0.001f);
        assertEquals(0.7f, rd.getB(), 0.001f);
        assertEquals(1.0f, rd.getA(), 0.001f);
    }

    @Test
    public void testCellRenderDataToString() {
        CellRenderData rd = new CellRenderData(0, 0, 1, 1, 0.4f, 0.35f, 0.3f, 1.0f);
        String str = rd.toString();
        assertNotNull(str);
        assertTrue(str.contains("x=0"));
        assertTrue(str.contains("y=0"));
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
}
