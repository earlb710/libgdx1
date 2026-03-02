package eb.gmodel1.city;

import eb.gmodel1.save.*;


import org.junit.Test;
import org.junit.Assume;
import static org.junit.Assert.*;

import java.io.File;

/**
 * Unit tests for building icon generation and mapping.
 */
public class BuildingIconTest {

    @Test
    public void testGetIconPathFormat() {
        BuildingDefinition def = new BuildingDefinition();
        def.setName("Police Station");
        assertEquals("icons/police_station.png", def.getIconPath());
    }

    @Test
    public void testGetIconPathHyphen() {
        BuildingDefinition def = new BuildingDefinition();
        def.setName("High-Rise Apartment Complex");
        assertEquals("icons/high_rise_apartment_complex.png", def.getIconPath());
    }

    @Test
    public void testGetIconPathNullName() {
        BuildingDefinition def = new BuildingDefinition();
        def.setName(null);
        assertNull("Null name should return null icon path", def.getIconPath());
    }

    @Test
    public void testGetIconPathEmptyName() {
        BuildingDefinition def = new BuildingDefinition();
        def.setName("");
        assertNull("Empty name should return null icon path", def.getIconPath());
    }

    @Test
    public void testAllBuildingIconFilesExist() {
        // Verify that every building in buildings.json has a corresponding icon file
        File assetsDir = new File("assets");
        if (!assetsDir.exists()) {
            assetsDir = new File("../../assets");
        }
        Assume.assumeTrue("Assets directory not found, skipping icon file check", assetsDir.exists());

        GameDataManager gdm = new GameDataManager();
        gdm.loadBuildings(new File(assetsDir, "buildings.json").getAbsolutePath());

        for (BuildingDefinition def : gdm.getBuildings()) {
            String iconPath = def.getIconPath();
            assertNotNull("Building '" + def.getName() + "' should have an icon path", iconPath);

            File iconFile = new File(assetsDir, iconPath);
            assertTrue("Icon file should exist for '" + def.getName() + "': " + iconFile.getAbsolutePath(),
                       iconFile.exists());
        }
    }
}
