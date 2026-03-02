package eb.gmodel1.shop;

import eb.gmodel1.city.*;


import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ShopItem} and {@link BuildingServices#getShopItems(Building)}.
 */
public class ShopItemTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Building buildingWithDef(String id, String category) {
        BuildingDefinition def = new BuildingDefinition(
                id, "Test Building", category, 1, 5, 4, 50, 1.0, "Test.", new ArrayList<>());
        List<Improvement> imps = new ArrayList<>();
        for (int i = 0; i < 4; i++) imps.add(new Improvement("Imp" + i, 1, 0));
        return new Building("Test Building", imps, def, 3);
    }

    // -------------------------------------------------------------------------
    // ShopItem constructor tests
    // -------------------------------------------------------------------------

    @Test
    public void testShopItemConsumableConstructor() {
        ShopItem item = new ShopItem("Pain Killers", "Restores stamina.", 10, true, 2);
        assertEquals("Pain Killers", item.name);
        assertEquals("Restores stamina.", item.description);
        assertEquals(10, item.price);
        assertTrue(item.consumable);
        assertEquals(2, item.staminaGain);
    }

    @Test
    public void testShopItemNonConsumableConstructor() {
        ShopItem item = new ShopItem("Pistol", "A handgun.", 350, false);
        assertEquals("Pistol", item.name);
        assertEquals(350, item.price);
        assertFalse(item.consumable);
        assertEquals(0, item.staminaGain); // staminaGain is always 0 for non-consumables
    }

    @Test
    public void testNonConsumableIgnoresStaminaGain() {
        ShopItem item = new ShopItem("Pistol", "A handgun.", 350, false, 99);
        assertEquals(0, item.staminaGain); // stamina gain must be 0 for non-consumables
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBlankNameThrows() {
        new ShopItem("", "desc", 10, true, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullNameThrows() {
        new ShopItem(null, "desc", 10, true, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativePriceThrows() {
        new ShopItem("Item", "desc", -1, true, 1);
    }

    @Test
    public void testNullDescriptionDefaultsToEmpty() {
        ShopItem item = new ShopItem("Item", null, 5, true, 1);
        assertEquals("", item.description);
    }

    @Test
    public void testZeroPriceIsAllowed() {
        ShopItem item = new ShopItem("Freebie", "Free item.", 0, true, 1);
        assertEquals(0, item.price);
    }

    @Test
    public void testNameIsTrimmed() {
        ShopItem item = new ShopItem("  Pistol  ", "A handgun.", 350, false);
        assertEquals("Pistol", item.name);
    }

    // -------------------------------------------------------------------------
    // BuildingServices.getShopItems tests
    // -------------------------------------------------------------------------

    @Test
    public void testSecurityShopHasGearItems() {
        Building b = buildingWithDef("security_shop", "commercial");
        List<ShopItem> items = BuildingServices.getShopItems(b);
        assertFalse("Security shop should have items", items.isEmpty());
        // All gear items should be non-consumable
        for (ShopItem item : items) {
            assertFalse("Gear items should be non-consumable: " + item.name, item.consumable);
            assertTrue("Price must be > 0", item.price > 0);
        }
    }

    @Test
    public void testSecurityShopContainsPistol() {
        Building b = buildingWithDef("security_shop", "commercial");
        List<ShopItem> items = BuildingServices.getShopItems(b);
        boolean hasPistol = items.stream().anyMatch(i -> i.name.equals("Pistol"));
        assertTrue("Security shop should sell Pistol", hasPistol);
    }

    @Test
    public void testPharmacyHasMedicineItems() {
        Building b = buildingWithDef("pharmacy", "commercial");
        List<ShopItem> items = BuildingServices.getShopItems(b);
        assertFalse("Pharmacy should have items", items.isEmpty());
        // Medicine items should be consumable
        for (ShopItem item : items) {
            assertTrue("Medicine items should be consumable: " + item.name, item.consumable);
            assertTrue("Medicine should restore stamina: " + item.name, item.staminaGain > 0);
        }
    }

    @Test
    public void testGasStationHasSnackItems() {
        Building b = buildingWithDef("gas_station", "commercial");
        List<ShopItem> items = BuildingServices.getShopItems(b);
        assertFalse("Gas station should have snack items", items.isEmpty());
        for (ShopItem item : items) {
            assertTrue("Snacks should be consumable: " + item.name, item.consumable);
        }
    }

    @Test
    public void testConvenienceStoreHasSupplies() {
        Building b = buildingWithDef("convenience_store", "commercial");
        List<ShopItem> items = BuildingServices.getShopItems(b);
        assertFalse("Convenience store should have supply items", items.isEmpty());
    }

    @Test
    public void testSupermarketHasSupplies() {
        Building b = buildingWithDef("supermarket", "commercial");
        List<ShopItem> items = BuildingServices.getShopItems(b);
        assertFalse("Supermarket should have supply items", items.isEmpty());
    }

    @Test
    public void testUnknownBuildingReturnsEmpty() {
        Building b = buildingWithDef("library", "education");
        List<ShopItem> items = BuildingServices.getShopItems(b);
        assertTrue("Unknown building type should have no shop items", items.isEmpty());
    }

    @Test
    public void testNullBuildingReturnsEmpty() {
        List<ShopItem> items = BuildingServices.getShopItems(null);
        assertTrue("Null building should return empty list", items.isEmpty());
    }

    @Test
    public void testAllShopItemsHaveNonNullFields() {
        String[] shopBuildingIds = {
            "security_shop", "pharmacy", "gas_station",
            "convenience_store", "supermarket", "warehouse_store",
            "small_retail_store", "strip_mall", "shopping_center", "regional_mall"
        };
        for (String id : shopBuildingIds) {
            Building b = buildingWithDef(id, "commercial");
            List<ShopItem> items = BuildingServices.getShopItems(b);
            for (ShopItem item : items) {
                assertNotNull("Name should not be null for " + id, item.name);
                assertFalse("Name should not be empty for " + id, item.name.isEmpty());
                assertNotNull("Description should not be null for " + id, item.description);
                assertTrue("Price should be >= 0 for " + id, item.price >= 0);
            }
        }
    }

    @Test
    public void testGetShopTitleReturnsNonEmpty() {
        Building b = buildingWithDef("security_shop", "commercial");
        String title = BuildingServices.getShopTitle(b, BuildingServices.SVC_BUY_GEAR);
        assertNotNull(title);
        assertFalse(title.isEmpty());
    }

    @Test
    public void testGetShopTitleForNullBuildingReturnsShop() {
        String title = BuildingServices.getShopTitle(null, "any");
        assertEquals("Shop", title);
    }
}
