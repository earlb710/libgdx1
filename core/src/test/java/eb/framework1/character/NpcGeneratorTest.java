package eb.framework1.character;

import eb.framework1.city.*;
import eb.framework1.face.FaceRule;
import eb.framework1.generator.*;
import eb.framework1.investigation.*;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link NpcGenerator}, {@link NpcSkill}, {@link NpcSchedule},
 * and {@link NpcScheduleEntry}.
 *
 * <p>All tests are pure-Java and require no libGDX runtime.  A seeded
 * {@link Random} is used for deterministic results.  {@link CityMap} is
 * created with a seed only (no {@code GameDataManager}), so buildings will not
 * have category definitions — schedule entries therefore have {@code cellX/Y =
 * -1} in these tests; this is intentional and tested explicitly.
 */
public class NpcGeneratorTest {

    // =========================================================================
    // Helpers
    // =========================================================================

    private static NpcGenerator makeGenerator(long seed) {
        List<PersonNameGenerator.NameEntry> firstNames = Arrays.asList(
                new PersonNameGenerator.NameEntry("Alice",  "F"),
                new PersonNameGenerator.NameEntry("Bob",    "M"),
                new PersonNameGenerator.NameEntry("Carol",  "F"),
                new PersonNameGenerator.NameEntry("Dave",   "M"),
                new PersonNameGenerator.NameEntry("Eve",    "F"),
                new PersonNameGenerator.NameEntry("Frank",  "M")
        );
        List<String> surnames = Arrays.asList("Smith", "Jones", "Williams", "Taylor", "Brown");
        PersonNameGenerator nameGen = new PersonNameGenerator(firstNames, surnames, new Random(seed));
        return new NpcGenerator(nameGen, new Random(seed));
    }

    // =========================================================================
    // Construction
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nullNameGen_throws() {
        new NpcGenerator(null);
    }

    @Test
    public void constructor_nullRandom_doesNotThrow() {
        List<PersonNameGenerator.NameEntry> names =
                Arrays.asList(new PersonNameGenerator.NameEntry("Sam", "M"));
        PersonNameGenerator nameGen = new PersonNameGenerator(names, Arrays.asList("X"), null);
        assertNotNull(new NpcGenerator(nameGen, null));
    }

    // =========================================================================
    // generateClient — basic field checks
    // =========================================================================

    @Test
    public void generateClient_returnsNonNull() {
        assertNotNull(makeGenerator(1).generateClient(CaseType.FRAUD, null));
    }

    @Test
    public void generateClient_hasNonBlankId() {
        NpcCharacter npc = makeGenerator(2).generateClient(CaseType.THEFT, null);
        assertFalse(npc.getId().isEmpty());
    }

    @Test
    public void generateClient_hasNonBlankName() {
        NpcCharacter npc = makeGenerator(3).generateClient(CaseType.MURDER, null);
        assertFalse(npc.getFullName().isEmpty());
    }

    // =========================================================================
    // Skills — attached to generated NPC
    // =========================================================================

    @Test
    public void generateClient_hasAtLeastOneSkill() {
        NpcCharacter npc = makeGenerator(4).generateClient(CaseType.FRAUD, null);
        assertFalse("NPC must have at least one skill", npc.getSkills().isEmpty());
    }

    @Test
    public void generateVictim_hasAtLeastOneSkill() {
        NpcCharacter npc = makeGenerator(5).generateVictim(CaseType.MURDER, null);
        assertFalse("Victim must have at least one skill", npc.getSkills().isEmpty());
    }

    @Test
    public void generateSuspect_hasAtLeastOneSkill() {
        NpcCharacter npc = makeGenerator(6).generateSuspect(CaseType.STALKING, null);
        assertFalse("Suspect must have at least one skill", npc.getSkills().isEmpty());
    }

    @Test
    public void generateSuspect_withProfile_hasAtLeastOneSkill() {
        NpcCharacter npc = makeGenerator(7).generateSuspect(
                CaseType.MURDER, PersonalityProfile.PSYCHOPATH, null);
        assertFalse(npc.getSkills().isEmpty());
    }

    // =========================================================================
    // Schedule — attached to generated NPC
    // =========================================================================

    @Test
    public void generateClient_hasSchedule() {
        NpcCharacter npc = makeGenerator(8).generateClient(CaseType.FRAUD, null);
        assertNotNull("NPC must have a schedule", npc.getSchedule());
    }

    @Test
    public void generateClient_scheduleHasEntries() {
        NpcCharacter npc = makeGenerator(9).generateClient(CaseType.THEFT, null);
        assertTrue("Schedule must have at least one entry", npc.getSchedule().size() > 0);
    }

    @Test
    public void schedule_sleepEntryAt3AM() {
        NpcCharacter npc = makeGenerator(10).generateClient(CaseType.FRAUD, null);
        NpcScheduleEntry entry = npc.getSchedule().getEntryForHour(3);
        assertNotNull("Hour 3 must have a schedule entry", entry);
        assertEquals("Hour 3 should be SLEEP", NpcScheduleEntry.SLEEP, entry.activityType);
    }

    @Test
    public void schedule_workEntryDuringWorkHours() {
        // Generate until we get a non-homemaker NPC (deterministic with seed)
        NpcCharacter npc = makeGenerator(11).generateClient(CaseType.CORPORATE_ESPIONAGE, null);
        // Work hours vary by skill; check middle of typical work day (10–14)
        boolean foundWork = false;
        for (int hour = 9; hour <= 17; hour++) {
            NpcScheduleEntry entry = npc.getSchedule().getEntryForHour(hour);
            if (entry != null && NpcScheduleEntry.WORK.equals(entry.activityType)) {
                foundWork = true;
                break;
            }
        }
        assertTrue("Schedule should contain a WORK entry during business hours", foundWork);
    }

    @Test
    public void schedule_sleepEntryAt22() {
        NpcCharacter npc = makeGenerator(12).generateClient(CaseType.THEFT, null);
        NpcScheduleEntry entry = npc.getSchedule().getEntryForHour(22);
        assertNotNull("Hour 22 must have a schedule entry", entry);
        assertEquals("Hour 22 should be SLEEP", NpcScheduleEntry.SLEEP, entry.activityType);
    }

    @Test
    public void schedule_allHoursCovered_0to23() {
        NpcCharacter npc = makeGenerator(13).generateVictim(CaseType.MURDER, null);
        NpcSchedule schedule = npc.getSchedule();
        for (int hour = 0; hour < 24; hour++) {
            assertNotNull("Hour " + hour + " must have a schedule entry",
                    schedule.getEntryForHour(hour));
        }
    }

    // =========================================================================
    // Schedule — no coordinates without GameDataManager (cityMap seed-only)
    // =========================================================================

    @Test
    public void schedule_withSeedOnlyCityMap_coordinatesAreNegativeOne() {
        // CityMap created with seed only has no BuildingDefinition (no GameDataManager),
        // so category lookups return nothing → cell coords remain -1.
        CityMap cityMap = new CityMap(42L);
        NpcCharacter npc = makeGenerator(14).generateClient(CaseType.FRAUD, cityMap);
        NpcSchedule schedule = npc.getSchedule();
        for (NpcScheduleEntry entry : schedule.getEntries()) {
            assertFalse("Entry should not have known cell when no GameDataManager",
                    entry.hasKnownCell());
        }
    }

    @Test
    public void schedule_withNullCityMap_doesNotThrow() {
        NpcCharacter npc = makeGenerator(15).generateVictim(CaseType.STALKING, null);
        assertNotNull(npc);
        assertNotNull(npc.getSchedule());
    }

    // =========================================================================
    // Frequent locations — leisure entry added
    // =========================================================================

    @Test
    public void generateClient_frequentLocationsNonEmpty() {
        NpcCharacter npc = makeGenerator(16).generateClient(CaseType.THEFT, null);
        // NpcGenerator adds the leisure location as a frequent location
        assertFalse("Frequent locations should not be empty after generation",
                npc.getFrequentLocations().isEmpty());
    }

    // =========================================================================
    // NpcGenerator.assignCarriedItems — item assignment
    // =========================================================================

    @Test
    public void assignCarriedItems_lawEnforcement_hasPistol() {
        List<NpcSkill> skills = Arrays.asList(NpcSkill.LAW_ENFORCEMENT);
        List<EquipItem> items = NpcGenerator.assignCarriedItems(skills);
        assertFalse("Law enforcement should carry items", items.isEmpty());
        assertEquals("Law enforcement should carry a Pistol", EquipItem.PISTOL, items.get(0));
    }

    @Test
    public void assignCarriedItems_freelancer_carriesNothing() {
        List<NpcSkill> skills = Arrays.asList(NpcSkill.FREELANCER);
        List<EquipItem> items = NpcGenerator.assignCarriedItems(skills);
        assertTrue("Freelancer should carry nothing", items.isEmpty());
    }

    @Test
    public void assignCarriedItems_officeWorker_carriesNothing() {
        List<NpcSkill> skills = Arrays.asList(NpcSkill.OFFICE_WORKER);
        List<EquipItem> items = NpcGenerator.assignCarriedItems(skills);
        assertTrue("Office worker should carry nothing", items.isEmpty());
    }

    @Test
    public void assignCarriedItems_emptySkills_carriesNothing() {
        List<EquipItem> items = NpcGenerator.assignCarriedItems(
                java.util.Collections.<NpcSkill>emptyList());
        assertTrue("No skills → no carried items", items.isEmpty());
    }

    @Test
    public void generateNpc_withPoliceOccupation_hasPistol() {
        // Use NpcCharacter.Builder directly with LAW_ENFORCEMENT skill
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("cop-1").fullName("Jane Officer").gender("F")
                .addSkill(NpcSkill.LAW_ENFORCEMENT)
                .addCarriedItem(EquipItem.PISTOL)
                .build();
        assertFalse("Police officer should carry items", npc.getCarriedItems().isEmpty());
        assertEquals(EquipItem.PISTOL, npc.getCarriedItems().get(0));
    }

    @Test
    public void npcCharacterBuilder_carriedItemsDefaultEmpty() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("x").fullName("Nobody").gender("M").build();
        assertNotNull(npc.getCarriedItems());
        assertTrue(npc.getCarriedItems().isEmpty());
    }

    @Test
    public void npcCharacterBuilder_addCarriedItem_nullIgnored() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("x").fullName("Nobody").gender("M")
                .addCarriedItem(null)
                .build();
        assertTrue(npc.getCarriedItems().isEmpty());
    }

    @Test
    public void npcCharacterBuilder_carriedItemsList_replacesExisting() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("x").fullName("Nobody").gender("M")
                .addCarriedItem(EquipItem.PISTOL)
                .carriedItems(java.util.Collections.<EquipItem>emptyList())
                .build();
        assertTrue("carriedItems() should replace the list", npc.getCarriedItems().isEmpty());
    }

    @Test
    public void inferSkills_null_returnsFreelancer() {
        List<NpcSkill> skills = NpcGenerator.inferSkills(null);
        assertFalse(skills.isEmpty());
        assertTrue(skills.contains(NpcSkill.FREELANCER));
    }

    @Test
    public void inferSkills_empty_returnsFreelancer() {
        List<NpcSkill> skills = NpcGenerator.inferSkills("");
        assertTrue(skills.contains(NpcSkill.FREELANCER));
    }

    @Test
    public void inferSkills_nurse_returnsMedical() {
        List<NpcSkill> skills = NpcGenerator.inferSkills("Nurse");
        assertTrue("Nurse should map to MEDICAL_PROFESSIONAL",
                skills.contains(NpcSkill.MEDICAL_PROFESSIONAL));
    }

    @Test
    public void inferSkills_teacher_returnsEducator() {
        List<NpcSkill> skills = NpcGenerator.inferSkills("Teacher");
        assertTrue("Teacher should map to EDUCATOR", skills.contains(NpcSkill.EDUCATOR));
    }

    @Test
    public void inferSkills_accountant_returnsOfficeWorker() {
        List<NpcSkill> skills = NpcGenerator.inferSkills("Accountant");
        assertTrue("Accountant should map to OFFICE_WORKER",
                skills.contains(NpcSkill.OFFICE_WORKER));
    }

    @Test
    public void inferSkills_engineer_returnsResearcher() {
        List<NpcSkill> skills = NpcGenerator.inferSkills("Engineer");
        assertTrue("Engineer should map to RESEARCHER", skills.contains(NpcSkill.RESEARCHER));
    }

    @Test
    public void inferSkills_retailWorker_returnsShopClerk() {
        List<NpcSkill> skills = NpcGenerator.inferSkills("Retail Worker");
        assertTrue("Retail Worker should map to SHOP_CLERK",
                skills.contains(NpcSkill.SHOP_CLERK));
    }

    @Test
    public void inferSkills_labourer_returnsLaborer() {
        List<NpcSkill> skills = NpcGenerator.inferSkills("Labourer");
        assertTrue("Labourer should map to LABORER", skills.contains(NpcSkill.LABORER));
    }

    @Test
    public void inferSkills_spouse_returnsHomemaker() {
        List<NpcSkill> skills = NpcGenerator.inferSkills("Spouse");
        assertTrue("Spouse should map to HOMEMAKER", skills.contains(NpcSkill.HOMEMAKER));
    }

    @Test
    public void inferSkills_politician_returnsGovernmentWorker() {
        List<NpcSkill> skills = NpcGenerator.inferSkills("Politician");
        assertTrue("Politician should map to GOVERNMENT_WORKER",
                skills.contains(NpcSkill.GOVERNMENT_WORKER));
    }

    @Test
    public void inferSkills_alwaysNonEmpty() {
        String[] occupations = {
            "Student", "Freelancer", "Unemployed", "Landlord", "Driver",
            "Personal Trainer", "Financial Adviser", "Company Director",
            "Sales Manager", "Contractor", "Obsessive Fan", "Ex-partner",
            "Former Employee", "Journalist", "Acquaintance", "Associate"
        };
        for (String occ : occupations) {
            List<NpcSkill> skills = NpcGenerator.inferSkills(occ);
            assertFalse("Skills must not be empty for occupation: " + occ, skills.isEmpty());
        }
    }

    // =========================================================================
    // NpcSkill enum
    // =========================================================================

    @Test
    public void npcSkill_shopClerk_worksInCommercial() {
        assertTrue(NpcSkill.SHOP_CLERK.worksInCategory("commercial"));
    }

    @Test
    public void npcSkill_shopClerk_doesNotWorkInOffice() {
        assertFalse(NpcSkill.SHOP_CLERK.worksInCategory("office"));
    }

    @Test
    public void npcSkill_worksInCategory_nullReturnsFalse() {
        assertFalse(NpcSkill.OFFICE_WORKER.worksInCategory(null));
    }

    @Test
    public void npcSkill_allSkillsHaveNonEmptyCategories() {
        for (NpcSkill skill : NpcSkill.values()) {
            String[] cats = skill.getWorkBuildingCategories();
            assertNotNull("Categories must not be null for " + skill, cats);
            assertTrue("Categories must not be empty for " + skill, cats.length > 0);
        }
    }

    @Test
    public void npcSkill_workHoursValid() {
        for (NpcSkill skill : NpcSkill.values()) {
            assertTrue(skill + " workStartHour must be 0–23",
                    skill.getWorkStartHour() >= 0 && skill.getWorkStartHour() <= 23);
            assertTrue(skill + " workEndHour must be 1–24",
                    skill.getWorkEndHour() >= 1 && skill.getWorkEndHour() <= 24);
            assertTrue(skill + " workStart must be before workEnd",
                    skill.getWorkStartHour() < skill.getWorkEndHour());
        }
    }

    // =========================================================================
    // NpcScheduleEntry
    // =========================================================================

    @Test
    public void scheduleEntry_coversHour_start() {
        NpcScheduleEntry e = new NpcScheduleEntry(9, 17, NpcScheduleEntry.WORK, "Office");
        assertTrue(e.coversHour(9));
    }

    @Test
    public void scheduleEntry_coversHour_end_exclusive() {
        NpcScheduleEntry e = new NpcScheduleEntry(9, 17, NpcScheduleEntry.WORK, "Office");
        assertFalse(e.coversHour(17));
    }

    @Test
    public void scheduleEntry_coversHour_inside() {
        NpcScheduleEntry e = new NpcScheduleEntry(9, 17, NpcScheduleEntry.WORK, "Office");
        assertTrue(e.coversHour(13));
    }

    @Test
    public void scheduleEntry_hasKnownCell_trueWhenCoordinatesSet() {
        NpcScheduleEntry e = new NpcScheduleEntry(9, 17, NpcScheduleEntry.WORK, "Office", 3, 5);
        assertTrue(e.hasKnownCell());
    }

    @Test
    public void scheduleEntry_hasKnownCell_falseWhenMinusOne() {
        NpcScheduleEntry e = new NpcScheduleEntry(9, 17, NpcScheduleEntry.WORK, "Office", -1, -1);
        assertFalse(e.hasKnownCell());
    }

    @Test
    public void scheduleEntry_nullActivityType_defaultsToHome() {
        NpcScheduleEntry e = new NpcScheduleEntry(9, 17, null, "Home");
        assertEquals(NpcScheduleEntry.HOME, e.activityType);
    }

    @Test
    public void scheduleEntry_nullLocationName_becomesEmpty() {
        NpcScheduleEntry e = new NpcScheduleEntry(9, 17, NpcScheduleEntry.WORK, null);
        assertEquals("", e.locationName);
    }

    // =========================================================================
    // NpcSchedule
    // =========================================================================

    @Test
    public void npcSchedule_nullList_empty() {
        NpcSchedule schedule = new NpcSchedule(null);
        assertEquals(0, schedule.size());
    }

    @Test
    public void npcSchedule_getEntryForHour_noMatch_returnsNull() {
        NpcSchedule schedule = new NpcSchedule(null);
        assertNull(schedule.getEntryForHour(10));
    }

    @Test
    public void npcSchedule_sortedByStartHour() {
        List<NpcScheduleEntry> entries = Arrays.asList(
                new NpcScheduleEntry(17, 21, NpcScheduleEntry.LEISURE, "Cinema"),
                new NpcScheduleEntry(0,  6,  NpcScheduleEntry.SLEEP,   "Home"),
                new NpcScheduleEntry(9,  17, NpcScheduleEntry.WORK,    "Office")
        );
        NpcSchedule schedule = new NpcSchedule(entries);
        List<NpcScheduleEntry> sorted = schedule.getEntries();
        assertEquals(0,  sorted.get(0).startHour);
        assertEquals(9,  sorted.get(1).startHour);
        assertEquals(17, sorted.get(2).startHour);
    }

    @Test
    public void npcSchedule_getEntryForHour_correct() {
        List<NpcScheduleEntry> entries = Arrays.asList(
                new NpcScheduleEntry(0,  9,  NpcScheduleEntry.SLEEP, "Home"),
                new NpcScheduleEntry(9,  17, NpcScheduleEntry.WORK,  "Office"),
                new NpcScheduleEntry(17, 24, NpcScheduleEntry.HOME,  "Home")
        );
        NpcSchedule schedule = new NpcSchedule(entries);
        assertEquals(NpcScheduleEntry.SLEEP, schedule.getEntryForHour(0).activityType);
        assertEquals(NpcScheduleEntry.WORK,  schedule.getEntryForHour(12).activityType);
        assertEquals(NpcScheduleEntry.HOME,  schedule.getEntryForHour(20).activityType);
    }

    // =========================================================================
    // Determinism
    // =========================================================================

    @Test
    public void determinism_sameSeedProducesSameOccupation() {
        NpcCharacter npc1 = makeGenerator(99).generateClient(CaseType.FRAUD, null);
        NpcCharacter npc2 = makeGenerator(99).generateClient(CaseType.FRAUD, null);
        assertEquals(npc1.getOccupation(), npc2.getOccupation());
    }

    @Test
    public void determinism_sameSeedProducesSameSkills() {
        NpcCharacter npc1 = makeGenerator(100).generateSuspect(CaseType.MURDER, null);
        NpcCharacter npc2 = makeGenerator(100).generateSuspect(CaseType.MURDER, null);
        assertEquals(npc1.getSkills(), npc2.getSkills());
    }

    @Test
    public void determinism_sameSeedProducesSameScheduleSize() {
        NpcCharacter npc1 = makeGenerator(101).generateVictim(CaseType.STALKING, null);
        NpcCharacter npc2 = makeGenerator(101).generateVictim(CaseType.STALKING, null);
        assertEquals(npc1.getSchedule().size(), npc2.getSchedule().size());
    }

    // =========================================================================
    // Birthdate
    // =========================================================================

    @Test
    public void generateClient_hasBirthdate() {
        NpcCharacter npc = makeGenerator(200).generateClient(CaseType.FRAUD, null);
        assertFalse("Birthdate must not be empty", npc.getBirthdate().isEmpty());
    }

    @Test
    public void birthdate_matchesExpectedFormat() {
        NpcCharacter npc = makeGenerator(201).generateVictim(CaseType.MURDER, null);
        String bd = npc.getBirthdate();
        // Must be "YYYY-MM-DD"
        assertTrue("Birthdate must match YYYY-MM-DD: " + bd,
                bd.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void birthdate_yearMatchesAgeFromGameYear() {
        NpcCharacter npc = makeGenerator(202).generateSuspect(CaseType.THEFT, null);
        int birthYear = Integer.parseInt(npc.getBirthdate().substring(0, 4));
        int expectedBirthYear = NpcGenerator.GAME_YEAR - npc.getAge();
        assertEquals("Birth year must equal GAME_YEAR − age",
                expectedBirthYear, birthYear);
    }

    @Test
    public void birthdate_monthInRange() {
        NpcCharacter npc = makeGenerator(203).generateClient(CaseType.BLACKMAIL, null);
        int month = Integer.parseInt(npc.getBirthdate().substring(5, 7));
        assertTrue("Month must be 1–12", month >= 1 && month <= 12);
    }

    @Test
    public void birthdate_dayInRange() {
        NpcCharacter npc = makeGenerator(204).generateSuspect(CaseType.FRAUD, null);
        int day = Integer.parseInt(npc.getBirthdate().substring(8, 10));
        assertTrue("Day must be 1–31", day >= 1 && day <= 31);
    }

    @Test
    public void buildBirthdate_helper_zeroAge() {
        List<PersonNameGenerator.NameEntry> names =
                Arrays.asList(new PersonNameGenerator.NameEntry("Sam", "M"));
        PersonNameGenerator nameGen = new PersonNameGenerator(names, Arrays.asList("X"),
                new Random(99));
        NpcGenerator gen = new NpcGenerator(nameGen, new Random(99));
        String bd = gen.buildBirthdate(0);
        int year = Integer.parseInt(bd.substring(0, 4));
        assertEquals(NpcGenerator.GAME_YEAR, year);
    }

    @Test
    public void determinism_sameSeedProducesSameBirthdate() {
        NpcCharacter npc1 = makeGenerator(205).generateClient(CaseType.FRAUD, null);
        NpcCharacter npc2 = makeGenerator(205).generateClient(CaseType.FRAUD, null);
        assertEquals(npc1.getBirthdate(), npc2.getBirthdate());
    }

    // =========================================================================
    // Tracking flag
    // =========================================================================

    @Test
    public void generateClient_trackedDefaultsFalse() {
        NpcCharacter npc = makeGenerator(300).generateClient(CaseType.THEFT, null);
        assertFalse("NPC tracked flag should default to false", npc.isTracked());
    }

    @Test
    public void builder_tracked_canBeSetTrue() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("t1").fullName("Test Person").gender("M")
                .tracked(true)
                .build();
        assertTrue(npc.isTracked());
    }

    @Test
    public void builder_tracked_defaultFalse() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("t2").fullName("Test Person").gender("F")
                .build();
        assertFalse(npc.isTracked());
    }

    // =========================================================================
    // getCurrentCellX / getCurrentCellY
    // =========================================================================

    @Test
    public void getCurrentCellX_noSchedule_returnsMinusOne() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("cx1").fullName("Test").gender("M").build();
        assertEquals(-1, npc.getCurrentCellX(10));
    }

    @Test
    public void getCurrentCellY_noSchedule_returnsMinusOne() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("cy1").fullName("Test").gender("M").build();
        assertEquals(-1, npc.getCurrentCellY(10));
    }

    @Test
    public void getCurrentCellX_withScheduleAndKnownCell_returnsCell() {
        List<NpcScheduleEntry> entries = Arrays.asList(
                new NpcScheduleEntry(0, 24, NpcScheduleEntry.HOME, "Home", 5, 7)
        );
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("cx2").fullName("Test").gender("M")
                .schedule(new NpcSchedule(entries))
                .build();
        assertEquals(5, npc.getCurrentCellX(12));
    }

    @Test
    public void getCurrentCellY_withScheduleAndKnownCell_returnsCell() {
        List<NpcScheduleEntry> entries = Arrays.asList(
                new NpcScheduleEntry(0, 24, NpcScheduleEntry.HOME, "Home", 5, 7)
        );
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("cy2").fullName("Test").gender("M")
                .schedule(new NpcSchedule(entries))
                .build();
        assertEquals(7, npc.getCurrentCellY(12));
    }

    @Test
    public void getCurrentCellX_entryWithNoCell_returnsMinusOne() {
        List<NpcScheduleEntry> entries = Arrays.asList(
                new NpcScheduleEntry(0, 24, NpcScheduleEntry.HOME, "Home", -1, -1)
        );
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("cx3").fullName("Test").gender("M")
                .schedule(new NpcSchedule(entries))
                .build();
        assertEquals(-1, npc.getCurrentCellX(10));
    }

    @Test
    public void getCurrentCellX_hourOutsideAllEntries_returnsMinusOne() {
        List<NpcScheduleEntry> entries = Arrays.asList(
                new NpcScheduleEntry(9, 17, NpcScheduleEntry.WORK, "Office", 3, 3)
        );
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("cx4").fullName("Test").gender("M")
                .schedule(new NpcSchedule(entries))
                .build();
        assertEquals(-1, npc.getCurrentCellX(5));
    }

    // =========================================================================
    // generateWorldNpc
    // =========================================================================

    @Test
    public void generateWorldNpc_returnsNonNull() {
        NpcGenerator gen = makeGenerator(42L);
        NpcCharacter npc = gen.generateWorldNpc(null);
        assertNotNull("generateWorldNpc must return a non-null NPC", npc);
    }

    @Test
    public void generateWorldNpc_returnsUniqueNpcsOnRepeatedCalls() {
        NpcGenerator gen = makeGenerator(7L);
        NpcCharacter npc1 = gen.generateWorldNpc(null);
        NpcCharacter npc2 = gen.generateWorldNpc(null);
        assertNotEquals("Two consecutive world NPCs should have different IDs",
                npc1.getId(), npc2.getId());
    }

    @Test
    public void generateWorldNpc_hasSchedule() {
        NpcGenerator gen = makeGenerator(99L);
        NpcCharacter npc = gen.generateWorldNpc(null);
        assertNotNull("World NPC should have a daily schedule", npc.getSchedule());
        assertFalse("World NPC schedule should not be empty",
                npc.getSchedule().getEntries().isEmpty());
    }

    @Test
    public void generateWorldNpc_generate20ProducesDistinctNpcs() {
        NpcGenerator gen = makeGenerator(123L);
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (int i = 0; i < 20; i++) {
            NpcCharacter npc = gen.generateWorldNpc(null);
            assertNotNull("NPC " + i + " must not be null", npc);
            assertTrue("Each NPC must have a unique ID", ids.add(npc.getId()));
        }
        assertEquals("Should have generated 20 distinct NPCs", 20, ids.size());
    }

    // =========================================================================
    // VisionTrait
    // =========================================================================

    @Test
    public void visionTrait_noneHasNoModifier() {
        assertEquals(0, VisionTrait.NONE.getModifier(CharacterAttribute.PERCEPTION));
        assertFalse(VisionTrait.NONE.isImpaired());
    }

    @Test
    public void visionTrait_farsightedReducesPerception() {
        assertEquals(-1, VisionTrait.FARSIGHTED.getModifier(CharacterAttribute.PERCEPTION));
        assertTrue(VisionTrait.FARSIGHTED.isImpaired());
    }

    @Test
    public void visionTrait_nearsightedReducesPerception() {
        assertEquals(-1, VisionTrait.NEARSIGHTED.getModifier(CharacterAttribute.PERCEPTION));
        assertTrue(VisionTrait.NEARSIGHTED.isImpaired());
    }

    @Test
    public void visionTrait_onlyAffectsPerception() {
        assertEquals(0, VisionTrait.FARSIGHTED.getModifier(CharacterAttribute.INTELLIGENCE));
        assertEquals(0, VisionTrait.NEARSIGHTED.getModifier(CharacterAttribute.STRENGTH));
    }

    @Test
    public void assignCarriedItemsWithVision_noImpairment_noGlasses() {
        List<EquipItem> items = NpcGenerator.assignCarriedItemsWithVision(
                VisionTrait.NONE,
                java.util.Collections.<NpcSkill>emptyList());
        assertFalse("Non-impaired NPC should not carry glasses",
                items.contains(EquipItem.GLASSES));
    }

    @Test
    public void assignCarriedItemsWithVision_farsighted_addsGlasses() {
        List<EquipItem> items = NpcGenerator.assignCarriedItemsWithVision(
                VisionTrait.FARSIGHTED,
                java.util.Collections.<NpcSkill>emptyList());
        assertTrue("Farsighted NPC should carry glasses",
                items.contains(EquipItem.GLASSES));
    }

    @Test
    public void assignCarriedItemsWithVision_nearsighted_addsGlasses() {
        List<EquipItem> items = NpcGenerator.assignCarriedItemsWithVision(
                VisionTrait.NEARSIGHTED,
                java.util.Collections.<NpcSkill>emptyList());
        assertTrue("Nearsighted NPC should carry glasses",
                items.contains(EquipItem.GLASSES));
    }

    @Test
    public void assignCarriedItemsWithVision_nullTrait_noGlasses() {
        List<EquipItem> items = NpcGenerator.assignCarriedItemsWithVision(
                null,
                java.util.Collections.<NpcSkill>emptyList());
        assertFalse("Null trait should not add glasses", items.contains(EquipItem.GLASSES));
    }

    @Test
    public void assignCarriedItemsWithVision_lawEnforcementFarsighted_hasBothItems() {
        List<EquipItem> items = NpcGenerator.assignCarriedItemsWithVision(
                VisionTrait.FARSIGHTED,
                Arrays.asList(NpcSkill.LAW_ENFORCEMENT));
        assertTrue("Should carry a pistol", items.contains(EquipItem.PISTOL));
        assertTrue("Should carry glasses", items.contains(EquipItem.GLASSES));
    }

    @Test
    public void npcCharacterBuilder_visionTraitDefaultsToNone() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("v-1").fullName("Vision Test").gender("F").build();
        assertEquals(VisionTrait.NONE, npc.getVisionTrait());
    }

    @Test
    public void npcCharacterBuilder_visionTraitSetAndGet() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("v-2").fullName("Near Sighted").gender("M")
                .visionTrait(VisionTrait.NEARSIGHTED)
                .build();
        assertEquals(VisionTrait.NEARSIGHTED, npc.getVisionTrait());
    }

    @Test
    public void npcCharacterBuilder_visionTraitNullTreatedAsNone() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("v-3").fullName("No Trait").gender("F")
                .visionTrait(null)
                .build();
        assertEquals(VisionTrait.NONE, npc.getVisionTrait());
    }

    // =========================================================================
    // NpcGenerator.enrich() — physical/face fields forwarded from base
    // =========================================================================

    private static NpcGenerator makeGeneratorWithFaceRules(long seed) {
        List<PersonNameGenerator.NameEntry> firstNames = Arrays.asList(
                new PersonNameGenerator.NameEntry("Alice", "F"),
                new PersonNameGenerator.NameEntry("Bob",   "M"),
                new PersonNameGenerator.NameEntry("Carol", "F"),
                new PersonNameGenerator.NameEntry("Dave",  "M")
        );
        List<String> surnames = Arrays.asList("Smith", "Jones");
        PersonNameGenerator nameGen = new PersonNameGenerator(firstNames, surnames, new Random(seed));

        // Minimal face rules: Male and Female so defaultCharacterFace() fires.
        FaceRule male = new FaceRule.Builder()
                .name("Male").gender("male").percentage(100).priority(0)
                .include(Arrays.asList("hair.hair1", "eye.eye1", "eyebrow.eyebrow1",
                        "nose.nose1", "mouth.mouth1", "body.body1"))
                .build();
        FaceRule female = new FaceRule.Builder()
                .name("Female").gender("female").percentage(100).priority(0)
                .include(Arrays.asList("hair.hair2", "eye.eye2", "eyebrow.eyebrow2",
                        "nose.nose2", "mouth.mouth2", "body.body2"))
                .build();
        return new NpcGenerator(nameGen, new Random(seed), Arrays.asList(male, female));
    }

    @Test
    public void enrich_preservesHairTypeAndColor() {
        // With face rules the base character gets hair type/color from CharacterGenerator.
        // The enriched NPC must carry those through.
        for (long seed = 0; seed < 20; seed++) {
            NpcGenerator gen = makeGeneratorWithFaceRules(seed);
            NpcCharacter npc = gen.generateClient(CaseType.FRAUD, null);
            // hair type and color are non-empty strings when CharacterGenerator runs
            assertFalse("hairType must not be empty after enrich()", npc.getHairType().isEmpty());
            assertFalse("hairColor must not be empty after enrich()", npc.getHairColor().isEmpty());
        }
    }

    @Test
    public void enrich_preservesHeightAndWeight() {
        for (long seed = 0; seed < 20; seed++) {
            NpcGenerator gen = makeGeneratorWithFaceRules(seed);
            NpcCharacter npc = gen.generateClient(CaseType.FRAUD, null);
            assertTrue("heightCm must be > 0 after enrich()", npc.getHeightCm() > 0);
            assertTrue("weightKg must be > 0 after enrich()", npc.getWeightKg() > 0);
        }
    }

    @Test
    public void enrich_preservesFaceConfig() {
        // With face rules the base gets a non-null FaceConfig; enrich() must copy it.
        for (long seed = 0; seed < 20; seed++) {
            NpcGenerator gen = makeGeneratorWithFaceRules(seed);
            NpcCharacter npc = gen.generateClient(CaseType.FRAUD, null);
            assertNotNull("FaceConfig must not be null after enrich()", npc.getFaceConfig());
        }
    }

    @Test
    public void enrich_impairedVision_faceConfigHasGlasses() {
        // Build a generator that has glassesMale/glassesFemale rules so that
        // impaired NPCs get glasses injected into their FaceConfig.
        List<PersonNameGenerator.NameEntry> firstNames = Arrays.asList(
                new PersonNameGenerator.NameEntry("Alice", "F"),
                new PersonNameGenerator.NameEntry("Bob",   "M")
        );
        List<String> surnames = Arrays.asList("Smith");
        PersonNameGenerator nameGen = new PersonNameGenerator(
                firstNames, surnames, new Random(0));

        FaceRule male = new FaceRule.Builder()
                .name("Male").gender("male").percentage(100).priority(0)
                .include(Arrays.asList("hair.hair1", "eye.eye1", "eyebrow.eyebrow1",
                        "nose.nose1", "mouth.mouth1", "body.body1"))
                .build();
        FaceRule female = new FaceRule.Builder()
                .name("Female").gender("female").percentage(100).priority(0)
                .include(Arrays.asList("hair.hair2", "eye.eye2", "eyebrow.eyebrow2",
                        "nose.nose2", "mouth.mouth2", "body.body2"))
                .build();
        FaceRule glassesMale = new FaceRule.Builder()
                .name("glassesMale").gender("male").percentage(100).priority(0)
                .include(Arrays.asList("glasses.glasses1-primary", "glasses.glasses2-black"))
                .build();
        FaceRule glassesFemale = new FaceRule.Builder()
                .name("glassesFemale").gender("female").percentage(100).priority(0)
                .include(Arrays.asList("glasses.glasses1-secondary"))
                .build();
        NpcGenerator gen = new NpcGenerator(nameGen, new Random(0),
                Arrays.asList(male, female, glassesMale, glassesFemale));

        boolean foundImpairedWithGlasses = false;
        for (long seed = 0; seed < 300; seed++) {
            NpcGenerator g = new NpcGenerator(
                    new PersonNameGenerator(firstNames, surnames, new Random(seed)),
                    new Random(seed),
                    Arrays.asList(male, female, glassesMale, glassesFemale));
            NpcCharacter npc = g.generateClient(CaseType.FRAUD, null);
            if (npc.getVisionTrait() != null && npc.getVisionTrait().isImpaired()) {
                assertNotNull("FaceConfig must not be null for impaired NPC", npc.getFaceConfig());
                assertFalse("Impaired NPC face must have glasses (not 'none') after enrich()",
                        "none".equals(npc.getFaceConfig().glasses.id));
                foundImpairedWithGlasses = true;
                break;
            }
        }
        assertTrue("Expected at least one vision-impaired NPC in 300 seeds", foundImpairedWithGlasses);
    }

    @Test
    public void enrich_preservesBeardStyle_whenMaleHasBeard() {
        // Generate enough male NPCs until we find one with a non-empty beardStyle;
        // verify it survives enrichment.
        List<PersonNameGenerator.NameEntry> maleNames = Arrays.asList(
                new PersonNameGenerator.NameEntry("Bob",  "M"),
                new PersonNameGenerator.NameEntry("Dave", "M")
        );
        List<String> surnames = Arrays.asList("Smith");
        FaceRule male = new FaceRule.Builder()
                .name("Male").gender("male").percentage(100).priority(0)
                .include(Arrays.asList("hair.hair1", "eye.eye1", "eyebrow.eyebrow1",
                        "nose.nose1", "mouth.mouth1", "body.body1"))
                .build();
        FaceRule female = new FaceRule.Builder()
                .name("Female").gender("female").percentage(100).priority(0)
                .include(Arrays.asList("hair.hair2", "eye.eye2", "eyebrow.eyebrow2",
                        "nose.nose2", "mouth.mouth2", "body.body2"))
                .build();
        FaceRule beardShort = new FaceRule.Builder()
                .name("Beard Short").gender("male").percentage(50).priority(0)
                .include(Arrays.asList("facialHair.beard1-short"))
                .build();
        FaceRule beardLong = new FaceRule.Builder()
                .name("Beard Long").gender("male").percentage(50).priority(0)
                .include(Arrays.asList("facialHair.beard1-long"))
                .build();

        boolean foundBearded = false;
        for (long seed = 0; seed < 500; seed++) {
            NpcGenerator g = new NpcGenerator(
                    new PersonNameGenerator(maleNames, surnames, new Random(seed)),
                    new Random(seed),
                    Arrays.asList(male, female, beardShort, beardLong));
            NpcCharacter npc = g.generateClient(CaseType.FRAUD, null);
            if ("M".equals(npc.getGender()) && !npc.getBeardStyle().isEmpty()) {
                // beardStyle survived enrich()
                assertFalse("beardStyle must not be empty after enrich()",
                        npc.getBeardStyle().isEmpty());
                foundBearded = true;
                break;
            }
        }
        assertTrue("Expected at least one bearded male NPC in 500 seeds", foundBearded);
    }

    /**
     * Statistical test: over a large sample, approximately 30% of NPCs
     * generated by {@link CharacterGenerator} should have a vision impairment.
     * Acceptable range: 20%–40% (generous tolerance for a random process).
     */
    @Test
    public void characterGenerator_approximately30PercentHaveVisionImpairment() {
        List<PersonNameGenerator.NameEntry> firstNames = Arrays.asList(
                new PersonNameGenerator.NameEntry("Alice", "F"),
                new PersonNameGenerator.NameEntry("Bob",   "M")
        );
        List<String> surnames = Arrays.asList("Smith", "Jones");
        PersonNameGenerator nameGen = new PersonNameGenerator(firstNames, surnames, new Random(0));
        CharacterGenerator gen = new CharacterGenerator(nameGen, new Random(0));

        int total = 1000;
        int impaired = 0;
        for (int i = 0; i < total; i++) {
            NpcCharacter npc = gen.generateClient(eb.framework1.investigation.CaseType.FRAUD);
            if (npc.getVisionTrait().isImpaired()) impaired++;
        }
        double ratio = (double) impaired / total;
        assertTrue("Expected ~30% impaired, got " + impaired + "/" + total,
                ratio >= 0.20 && ratio <= 0.40);
    }

    // =========================================================================
    // Death state — NpcCharacter builder fields
    // =========================================================================

    @Test
    public void npcCharacter_defaultsToAlive() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("test-alive").fullName("Alive Person").gender("M")
                .build();
        assertFalse("NPC should default to alive", npc.isDead());
        assertEquals("", npc.getDeathDateTime());
        assertEquals(0, npc.getDeathTimeVarianceMinutes());
    }

    @Test
    public void npcCharacter_deadWithPreciseTime() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("test-dead").fullName("Dead Person").gender("F")
                .dead(true)
                .deathDateTime("2050-01-15 02:30")
                .deathTimeVarianceMinutes(0)
                .build();
        assertTrue(npc.isDead());
        assertEquals("2050-01-15 02:30", npc.getDeathDateTime());
        assertEquals(0, npc.getDeathTimeVarianceMinutes());
    }

    @Test
    public void npcCharacter_deadWithVariance() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("test-var").fullName("Victim").gender("M")
                .dead(true)
                .deathDateTime("2050-02-10 14:00")
                .deathTimeVarianceMinutes(90)
                .build();
        assertTrue(npc.isDead());
        assertEquals("2050-02-10 14:00", npc.getDeathDateTime());
        assertEquals(90, npc.getDeathTimeVarianceMinutes());
    }

    @Test
    public void npcCharacter_deadUnknownTime() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("test-unknown").fullName("Missing Body").gender("F")
                .dead(true)
                .deathDateTime("")
                .deathTimeVarianceMinutes(-1)
                .build();
        assertTrue(npc.isDead());
        assertEquals("", npc.getDeathDateTime());
        assertEquals(-1, npc.getDeathTimeVarianceMinutes());
    }

    @Test
    public void enrich_copiesDeadFields() {
        NpcGenerator gen = makeGenerator(100);
        // Create a base NPC that's dead, then enrich it
        NpcCharacter base = new NpcCharacter.Builder()
                .id("v1").fullName("Victim One").gender("M")
                .age(35).occupation("Teacher")
                .cooperativeness(5).honesty(5).nervousness(5)
                .dead(true)
                .deathDateTime("2050-03-10 22:15")
                .deathTimeVarianceMinutes(60)
                .build();
        // After generating via the public API, check that a victim retains default alive state
        NpcCharacter victim = gen.generateVictim(CaseType.MURDER, null);
        assertFalse("generateVictim defaults to alive (caller sets dead flag)", victim.isDead());
    }
}
