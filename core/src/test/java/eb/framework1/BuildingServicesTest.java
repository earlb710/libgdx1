package eb.framework1;

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
    public void testBudgetHotelHasRentRoom() {
        Building b = buildingWithDef("hotel_budget", "hospitality");
        List<BuildingService> svcs = BuildingServices.getServices(b);
        assertEquals(1, svcs.size());
        assertEquals(BuildingServices.SVC_HOTEL_ROOM, svcs.get(0).id);
        assertEquals(50, svcs.get(0).cost);
    }

    @Test
    public void testBusinessHotelCostsMore() {
        Building budget  = buildingWithDef("hotel_budget",   "hospitality");
        Building business = buildingWithDef("hotel_business", "hospitality");
        int budgetCost   = BuildingServices.getServices(budget).get(0).cost;
        int businessCost = BuildingServices.getServices(business).get(0).cost;
        assertTrue("Business hotel should cost more than budget hotel",
                businessCost > budgetCost);
    }

    @Test
    public void testLuxuryHotelCostsMost() {
        Building luxury  = buildingWithDef("hotel_luxury",   "hospitality");
        Building business = buildingWithDef("hotel_business", "hospitality");
        int luxuryCost   = BuildingServices.getServices(luxury).get(0).cost;
        int businessCost = BuildingServices.getServices(business).get(0).cost;
        assertTrue("Luxury hotel should cost more than business hotel",
                luxuryCost > businessCost);
    }

    @Test
    public void testFitnessCenterHasGymWorkout() {
        Building b = buildingWithDef("gym_fitness_center", "commercial");
        List<BuildingService> svcs = BuildingServices.getServices(b);
        assertEquals(1, svcs.size());
        assertEquals(BuildingServices.SVC_GYM_WORKOUT, svcs.get(0).id);
        assertEquals(15, svcs.get(0).cost);
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
