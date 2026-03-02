package eb.framework1.city;

import eb.framework1.character.*;
import eb.framework1.popup.*;


import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for {@link BuildingServices} and {@link BuildingService}.
 */
public class BuildingServicesTest {

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

    private Profile basicProfile() {
        return new Profile("TestChar", "male", "normal");
    }

    // -------------------------------------------------------------------------
    // Service lookup
    // -------------------------------------------------------------------------

    @Test
    public void testBudgetHotelHasTalkToReception() {
        Building b = buildingWithDef("hotel_budget", "hospitality");
        List<BuildingService> svcs = BuildingServices.getServices(b);
        assertEquals(1, svcs.size());
        assertEquals(BuildingServices.SVC_HOTEL_RECEPTION, svcs.get(0).id);
        assertEquals(0, svcs.get(0).cost);   // cost handled by popup/handleHotelCheckIn
    }

    @Test
    public void testAllHotelsTalkToReception() {
        for (String id : new String[]{"hotel_budget", "hotel_business", "hotel_luxury"}) {
            Building b = buildingWithDef(id, "hospitality");
            List<BuildingService> svcs = BuildingServices.getServices(b);
            assertEquals("Hotel " + id + " should have exactly one service", 1, svcs.size());
            assertEquals("Hotel " + id + " service should be reception",
                    BuildingServices.SVC_HOTEL_RECEPTION, svcs.get(0).id);
        }
    }

    @Test
    public void testHotelBonusesAreTiered() {
        Building budget  = buildingWithDef("hotel_budget",   "hospitality");
        Building business = buildingWithDef("hotel_business", "hospitality");
        Building luxury  = buildingWithDef("hotel_luxury",   "hospitality");
        assertTrue("Business bonus > budget bonus",
                BuildingServices.getHotelStaminaBonus(business)
                        > BuildingServices.getHotelStaminaBonus(budget));
        assertTrue("Luxury bonus > business bonus",
                BuildingServices.getHotelStaminaBonus(luxury)
                        > BuildingServices.getHotelStaminaBonus(business));
    }

    @Test
    public void testHotelNightlyCostsAreTiered() {
        Building budget  = buildingWithDef("hotel_budget",   "hospitality");
        Building business = buildingWithDef("hotel_business", "hospitality");
        Building luxury  = buildingWithDef("hotel_luxury",   "hospitality");
        assertTrue("Business nightly > budget nightly",
                BuildingServices.getHotelNightlyCost(business)
                        > BuildingServices.getHotelNightlyCost(budget));
        assertTrue("Luxury nightly > business nightly",
                BuildingServices.getHotelNightlyCost(luxury)
                        > BuildingServices.getHotelNightlyCost(business));
    }

    @Test
    public void testHotelRoomTypeLabels() {
        assertEquals("Budget Room",     BuildingServices.getHotelRoomType(buildingWithDef("hotel_budget",   "hospitality")));
        assertEquals("Comfortable Room",BuildingServices.getHotelRoomType(buildingWithDef("hotel_business", "hospitality")));
        assertEquals("Luxury Suite",    BuildingServices.getHotelRoomType(buildingWithDef("hotel_luxury",   "hospitality")));
        assertEquals("Standard Room",   BuildingServices.getHotelRoomType(null));
    }

    @Test
    public void testNonHotelBuildingHasZeroBonus() {
        Building gym = buildingWithDef("gym_fitness_center", "commercial");
        assertEquals(0, BuildingServices.getHotelStaminaBonus(gym));
        assertEquals(0, BuildingServices.getHotelNightlyCost(gym));
    }

    @Test
    public void testFitnessCenterHasTalkToInstructor() {
        Building b = buildingWithDef("gym_fitness_center", "commercial");
        List<BuildingService> svcs = BuildingServices.getServices(b);
        assertEquals(1, svcs.size());
        assertEquals(BuildingServices.SVC_GYM_INSTRUCTOR, svcs.get(0).id);
        assertEquals(0, svcs.get(0).cost);   // cost handled by handleGymTraining
    }

    @Test
    public void testPtCostsMoreThanSelf() {
        assertTrue("PT cost > self cost",
                BuildingServices.GYM_COST_PT > BuildingServices.GYM_COST_SELF);
    }

    @Test
    public void testPtHasHigherChanceThanSelf() {
        assertTrue("PT chance > self chance",
                BuildingServices.GYM_CHANCE_PT > BuildingServices.GYM_CHANCE_SELF);
    }

    @Test
    public void testPtIsFasterThanSelf() {
        assertTrue("PT time < self-guided time (more focused)",
                BuildingServices.GYM_TIME_PT < BuildingServices.GYM_TIME_SELF);
    }

    @Test
    public void testGymOptionConstantsAreDistinct() {
        // Ensure the four option IDs are different integers
        int[] opts = {
            BuildingServices.GYM_OPT_STRENGTH_SELF,
            BuildingServices.GYM_OPT_STRENGTH_PT,
            BuildingServices.GYM_OPT_STAMINA_SELF,
            BuildingServices.GYM_OPT_STAMINA_PT
        };
        for (int i = 0; i < opts.length; i++) {
            for (int j = i + 1; j < opts.length; j++) {
                assertNotEquals("GYM_OPT values must be distinct", opts[i], opts[j]);
            }
        }
    }

    @Test
    public void testGymChanceLinesContainAttributes() {
        String strengthSelf = GymInstructorPopup.chanceLine(BuildingServices.GYM_OPT_STRENGTH_SELF);
        String strengthPt   = GymInstructorPopup.chanceLine(BuildingServices.GYM_OPT_STRENGTH_PT);
        String staminaSelf  = GymInstructorPopup.chanceLine(BuildingServices.GYM_OPT_STAMINA_SELF);
        String staminaPt    = GymInstructorPopup.chanceLine(BuildingServices.GYM_OPT_STAMINA_PT);
        assertTrue(strengthSelf.contains("Strength"));
        assertTrue(strengthPt.contains("Strength"));
        assertTrue(staminaSelf.contains("Stamina"));
        assertTrue(staminaPt.contains("Stamina"));
        // PT has higher chance in description
        int pctSelf = Integer.parseInt(strengthSelf.split("%")[0].trim());
        int pctPt   = Integer.parseInt(strengthPt.split("%")[0].trim());
        assertTrue("PT chance% > self chance% in description", pctPt > pctSelf);
    }

    @Test
    public void testLibraryStudyIsFree() {
        Building b = buildingWithDef("library", "public_services");
        List<BuildingService> svcs = BuildingServices.getServices(b);
        assertEquals(1, svcs.size());
        assertEquals(BuildingServices.SVC_LIBRARY_STUDY, svcs.get(0).id);
        assertEquals(0, svcs.get(0).cost);
    }

    @Test
    public void testReligiousBuildingAttendServiceFree() {
        for (String rid : new String[]{"church", "mosque", "synagogue"}) {
            Building b = buildingWithDef(rid, "religious");
            List<BuildingService> svcs = BuildingServices.getServices(b);
            assertEquals(1, svcs.size());
            assertEquals(BuildingServices.SVC_ATTEND_SERVICE, svcs.get(0).id);
            assertEquals("Service at " + rid + " should be free", 0, svcs.get(0).cost);
        }
    }

    @Test
    public void testNoBuildingReturnsEmpty() {
        assertTrue(BuildingServices.getServices(null).isEmpty());
    }

    @Test
    public void testBuildingWithNoDefinitionReturnsEmpty() {
        List<Improvement> imps = new ArrayList<>();
        for (int i = 0; i < 4; i++) imps.add(new Improvement("Imp" + i, 1, 0));
        Building b = new Building("Mystery", imps);
        assertTrue(BuildingServices.getServices(b).isEmpty());
    }

    @Test
    public void testParkingLotHasNoServices() {
        Building b = buildingWithDef("parking_lot_small", "infrastructure");
        assertTrue(BuildingServices.getServices(b).isEmpty());
    }

    @Test
    public void testConvenienceStoreHasBuySupplies() {
        Building b = buildingWithDef("convenience_store", "commercial");
        List<BuildingService> svcs = BuildingServices.getServices(b);
        assertEquals(1, svcs.size());
        assertEquals(BuildingServices.SVC_BUY_SUPPLIES, svcs.get(0).id);
    }

    @Test
    public void testSupermarketHasBuySupplies() {
        Building b = buildingWithDef("supermarket", "commercial");
        List<BuildingService> svcs = BuildingServices.getServices(b);
        assertEquals(1, svcs.size());
        assertEquals(BuildingServices.SVC_BUY_SUPPLIES, svcs.get(0).id);
    }

    @Test
    public void testWarehouseStoreHasBuySupplies() {
        Building b = buildingWithDef("warehouse_store", "commercial");
        List<BuildingService> svcs = BuildingServices.getServices(b);
        assertEquals(1, svcs.size());
        assertEquals(BuildingServices.SVC_BUY_SUPPLIES, svcs.get(0).id);
    }

    @Test
    public void testAllSupplyBuildingsHaveBuySuppliesService() {
        String[] supplyIds = {
            "convenience_store", "supermarket", "warehouse_store",
            "small_retail_store", "strip_mall", "shopping_center", "regional_mall"
        };
        for (String id : supplyIds) {
            Building b = buildingWithDef(id, "commercial");
            List<BuildingService> svcs = BuildingServices.getServices(b);
            assertFalse("Supply building '" + id + "' should have at least one service", svcs.isEmpty());
            assertEquals("Supply building '" + id + "' service should be buy_supplies",
                    BuildingServices.SVC_BUY_SUPPLIES, svcs.get(0).id);
        }
    }

    @Test
    public void testLaundromatHasLaundryService() {
        Building b = buildingWithDef("laundromat", "commercial");
        List<BuildingService> svcs = BuildingServices.getServices(b);
        assertEquals(1, svcs.size());
        assertEquals(BuildingServices.SVC_LAUNDRY, svcs.get(0).id);
    }

    // -------------------------------------------------------------------------
    // menuLabel
    // -------------------------------------------------------------------------

    @Test
    public void testMenuLabelIncludesCostAndTime() {
        BuildingService svc = new BuildingService("test", "Work Out", "desc", 15, 90);
        String label = svc.menuLabel();
        assertTrue("Label should include '$15'", label.contains("$15"));
        assertTrue("Label should include '1h 30 min'", label.contains("1h 30 min"));
    }

    @Test
    public void testMenuLabelFreeService() {
        BuildingService svc = new BuildingService("test", "Study", "desc", 0, 90);
        String label = svc.menuLabel();
        assertFalse("Free service label should not contain '$'", label.contains("$"));
        assertTrue("Label should include '1h 30 min'", label.contains("1h 30 min"));
    }

    @Test
    public void testMenuLabelHoursOnly() {
        BuildingService svc = new BuildingService("test", "Rent a Room", "desc", 50, 480);
        String label = svc.menuLabel();
        assertTrue("Label should include '8h'", label.contains("8h"));
        assertFalse("Exact-hour label should not contain 'min'", label.contains("min"));
    }

    // -------------------------------------------------------------------------
    // Gym tracking
    // -------------------------------------------------------------------------

    @Test
    public void testGymUsesTodayZeroOnFirstVisit() {
        Profile p = basicProfile();
        assertEquals(0, BuildingServices.gymUsesToday(p, 20500102));
    }

    @Test
    public void testRecordGymUseIncrementsCount() {
        Profile p = basicProfile();
        BuildingServices.recordGymUse(p, 20500102);
        assertEquals(1, BuildingServices.gymUsesToday(p, 20500102));
        BuildingServices.recordGymUse(p, 20500102);
        assertEquals(2, BuildingServices.gymUsesToday(p, 20500102));
    }

    @Test
    public void testGymCountResetsOnNewDay() {
        Profile p = basicProfile();
        BuildingServices.recordGymUse(p, 20500102);
        BuildingServices.recordGymUse(p, 20500102);
        assertEquals(2, BuildingServices.gymUsesToday(p, 20500102));
        // New day: count should reset to 0 before recording
        assertEquals(0, BuildingServices.gymUsesToday(p, 20500103));
        BuildingServices.recordGymUse(p, 20500103);
        assertEquals(1, BuildingServices.gymUsesToday(p, 20500103));
    }

    // -------------------------------------------------------------------------
    // gameDateInt
    // -------------------------------------------------------------------------

    @Test
    public void testGameDateInt() {
        assertEquals(20500102, BuildingServices.gameDateInt("2050-01-02 13:20"));
        assertEquals(20501231, BuildingServices.gameDateInt("2050-12-31 00:00"));
    }

    @Test
    public void testGameDateIntInvalidReturnsZero() {
        assertEquals(0, BuildingServices.gameDateInt(null));
        assertEquals(0, BuildingServices.gameDateInt(""));
        assertEquals(0, BuildingServices.gameDateInt("not a date"));
    }
}
