package eb.framework1;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for {@link NovelTextEngine} and {@link TimeOfDay}.
 */
public class NovelTextEngineTest {

    private static final String SIMPLE_JSON =
        "{" +
        "  \"version\": \"1.0\"," +
        "  \"language\": \"en\"," +
        "  \"descriptions\": {" +
        "    \"gym\": {" +
        "      \"default\": \"You enter the gym.\"," +
        "      \"time\": {" +
        "        \"morning\": \"Early risers fill the gym.\"," +
        "        \"evening\": \"The after-work crowd is here.\"" +
        "      }," +
        "      \"attribute\": {" +
        "        \"STRENGTH\": \"You feel right at home among the weights.\"," +
        "        \"INTELLIGENCE\": \"You analyse everyone's form critically.\"" +
        "      }" +
        "    }," +
        "    \"office\": {" +
        "      \"default\": \"You step into the office.\"," +
        "      \"time\": {" +
        "        \"morning\": \"The office hums with early arrivals.\"," +
        "        \"night\": \"The office is dark and empty.\"" +
        "      }" +
        "    }" +
        "  }" +
        "}";

    private NovelTextEngine engine;

    @Before
    public void setUp() {
        engine = NovelTextEngine.fromJsonString(SIMPLE_JSON);
    }

    // ===== TimeOfDay tests =====

    @Test
    public void testFromHourMorning() {
        assertEquals(TimeOfDay.MORNING, TimeOfDay.fromHour(6));
        assertEquals(TimeOfDay.MORNING, TimeOfDay.fromHour(9));
        assertEquals(TimeOfDay.MORNING, TimeOfDay.fromHour(11));
    }

    @Test
    public void testFromHourAfternoon() {
        assertEquals(TimeOfDay.AFTERNOON, TimeOfDay.fromHour(12));
        assertEquals(TimeOfDay.AFTERNOON, TimeOfDay.fromHour(15));
        assertEquals(TimeOfDay.AFTERNOON, TimeOfDay.fromHour(17));
    }

    @Test
    public void testFromHourEvening() {
        assertEquals(TimeOfDay.EVENING, TimeOfDay.fromHour(18));
        assertEquals(TimeOfDay.EVENING, TimeOfDay.fromHour(20));
        assertEquals(TimeOfDay.EVENING, TimeOfDay.fromHour(21));
    }

    @Test
    public void testFromHourNight() {
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromHour(22));
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromHour(0));
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromHour(5));
    }

    // ===== NovelTextEngine – basic loading =====

    @Test
    public void testFromJsonStringNotNull() {
        assertNotNull("Engine should not be null", engine);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromJsonStringNullThrows() {
        NovelTextEngine.fromJsonString(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromJsonStringEmptyThrows() {
        NovelTextEngine.fromJsonString("   ");
    }

    // ===== getDescription – default fallback =====

    @Test
    public void testDefaultDescriptionReturned() {
        String desc = engine.getDescription("gym", TimeOfDay.AFTERNOON, Collections.<String, Integer>emptyMap());
        assertEquals("You enter the gym.", desc);
    }

    @Test
    public void testUnknownKeyReturnsEmpty() {
        String desc = engine.getDescription("unknown_place", TimeOfDay.MORNING, Collections.<String, Integer>emptyMap());
        assertEquals("", desc);
    }

    @Test
    public void testNullKeyReturnsEmpty() {
        String desc = engine.getDescription(null, TimeOfDay.MORNING, Collections.<String, Integer>emptyMap());
        assertEquals("", desc);
    }

    // ===== getDescription – time-of-day selection =====

    @Test
    public void testTimeVariantMorning() {
        String desc = engine.getDescription("gym", TimeOfDay.MORNING, Collections.<String, Integer>emptyMap());
        assertEquals("Early risers fill the gym.", desc);
    }

    @Test
    public void testTimeVariantEvening() {
        String desc = engine.getDescription("gym", TimeOfDay.EVENING, Collections.<String, Integer>emptyMap());
        assertEquals("The after-work crowd is here.", desc);
    }

    @Test
    public void testMissingTimeVariantFallsBackToDefault() {
        // gym has no afternoon variant — should return default
        String desc = engine.getDescription("gym", TimeOfDay.AFTERNOON, Collections.<String, Integer>emptyMap());
        assertEquals("You enter the gym.", desc);
    }

    @Test
    public void testNullTimeOfDayFallsBackToDefault() {
        String desc = engine.getDescription("gym", (TimeOfDay) null, Collections.<String, Integer>emptyMap());
        assertEquals("You enter the gym.", desc);
    }

    // ===== getDescription – attribute-based selection =====

    @Test
    public void testAttributeVariantStrength() {
        Map<String, Integer> attrs = new HashMap<String, Integer>();
        attrs.put("STRENGTH", 8);
        attrs.put("INTELLIGENCE", 3);
        String desc = engine.getDescription("gym", TimeOfDay.MORNING, attrs);
        assertEquals("You feel right at home among the weights.", desc);
    }

    @Test
    public void testAttributeVariantIntelligence() {
        Map<String, Integer> attrs = new HashMap<String, Integer>();
        attrs.put("INTELLIGENCE", 9);
        attrs.put("STRENGTH", 4);
        String desc = engine.getDescription("gym", TimeOfDay.MORNING, attrs);
        assertEquals("You analyse everyone's form critically.", desc);
    }

    @Test
    public void testAttributeTakesPriorityOverTime() {
        // STRENGTH is highest and has a variant — should beat the morning time variant
        Map<String, Integer> attrs = new HashMap<String, Integer>();
        attrs.put("STRENGTH", 10);
        String desc = engine.getDescription("gym", TimeOfDay.MORNING, attrs);
        assertEquals("You feel right at home among the weights.", desc);
    }

    @Test
    public void testHighestAttributeWithNoVariantFallsToTime() {
        // gym has no CHARISMA variant; highest attr is CHARISMA; morning time variant exists → use time
        Map<String, Integer> attrs = new HashMap<String, Integer>();
        attrs.put("CHARISMA", 10);
        String desc = engine.getDescription("gym", TimeOfDay.MORNING, attrs);
        assertEquals("Early risers fill the gym.", desc);
    }

    @Test
    public void testNullAttributeMapFallsToTime() {
        String desc = engine.getDescription("gym", TimeOfDay.MORNING, null);
        assertEquals("Early risers fill the gym.", desc);
    }

    @Test
    public void testEmptyAttributeMapFallsToTime() {
        String desc = engine.getDescription("gym", TimeOfDay.MORNING, Collections.<String, Integer>emptyMap());
        assertEquals("Early risers fill the gym.", desc);
    }

    // ===== getDescription – entry with no attribute variants =====

    @Test
    public void testEntryWithNoAttributeVariantsUsesTime() {
        // "office" has no attribute variants
        Map<String, Integer> attrs = new HashMap<String, Integer>();
        attrs.put("STRENGTH", 10);
        String desc = engine.getDescription("office", TimeOfDay.MORNING, attrs);
        assertEquals("The office hums with early arrivals.", desc);
    }

    @Test
    public void testOfficeNightVariant() {
        String desc = engine.getDescription("office", TimeOfDay.NIGHT, Collections.<String, Integer>emptyMap());
        assertEquals("The office is dark and empty.", desc);
    }

    @Test
    public void testOfficeAfternoonFallsBackToDefault() {
        // office has no afternoon variant
        String desc = engine.getDescription("office", TimeOfDay.AFTERNOON, Collections.<String, Integer>emptyMap());
        assertEquals("You step into the office.", desc);
    }

    // ===== getDescription – hour-based convenience overload =====

    @Test
    public void testGetDescriptionByHour() {
        String desc = engine.getDescription("gym", 20, Collections.<String, Integer>emptyMap());
        assertEquals("The after-work crowd is here.", desc);
    }

    @Test
    public void testGetDescriptionByHourNight() {
        String desc = engine.getDescription("office", 23, Collections.<String, Integer>emptyMap());
        assertEquals("The office is dark and empty.", desc);
    }

    // ===== Attribute with multiple equal top values =====

    @Test
    public void testTiedAttributesPicksOneWithVariant() {
        // STRENGTH=5, INTELLIGENCE=5, CHARISMA=5 — any with a variant should be returned
        Map<String, Integer> attrs = new HashMap<String, Integer>();
        attrs.put("STRENGTH", 5);
        attrs.put("INTELLIGENCE", 5);
        attrs.put("CHARISMA", 5);
        String desc = engine.getDescription("gym", TimeOfDay.AFTERNOON, attrs);
        // Both STRENGTH and INTELLIGENCE have variants; either is acceptable
        boolean valid = desc.equals("You feel right at home among the weights.")
                     || desc.equals("You analyse everyone's form critically.");
        assertTrue("Should return a valid attribute variant when values are tied", valid);
    }

    // ===== Building-ID-specific entries =====

    private static final String BUILDING_JSON =
        "{" +
        "  \"version\": \"1.1\"," +
        "  \"language\": \"en\"," +
        "  \"descriptions\": {" +
        "    \"gym_fitness_center\": {" +
        "      \"default\": \"You enter the fitness center.\"," +
        "      \"time\": {" +
        "        \"morning\": \"Early risers power through their routines.\"," +
        "        \"evening\": \"The after-work crowd fills every station.\"" +
        "      }," +
        "      \"attribute\": {" +
        "        \"STRENGTH\": \"You feel right at home — this is your element.\"," +
        "        \"STAMINA\": \"Your eyes go straight to the cardio section.\"," +
        "        \"AGILITY\": \"You size up the open training floor immediately.\"" +
        "      }" +
        "    }," +
        "    \"police_station\": {" +
        "      \"default\": \"You enter the police station.\"," +
        "      \"time\": {" +
        "        \"morning\": \"Morning briefings fill the station.\"," +
        "        \"night\": \"Night-duty officers keep watch.\"" +
        "      }," +
        "      \"attribute\": {" +
        "        \"PERCEPTION\": \"You spot tension on the case board immediately.\"," +
        "        \"INTIMIDATION\": \"Heads turn as you walk in.\"" +
        "      }" +
        "    }," +
        "    \"fire_station\": {" +
        "      \"default\": \"You step into the fire station.\"," +
        "      \"time\": {" +
        "        \"morning\": \"The morning shift checks equipment.\"," +
        "        \"evening\": \"The evening shift shares a meal.\"" +
        "      }," +
        "      \"attribute\": {" +
        "        \"STRENGTH\": \"The crew recognises your build immediately.\"," +
        "        \"STAMINA\": \"The physical demands match your output.\"" +
        "      }" +
        "    }," +
        "    \"library\": {" +
        "      \"default\": \"You walk into the library.\"," +
        "      \"time\": {" +
        "        \"morning\": \"Early patrons absorbed in reading.\"," +
        "        \"afternoon\": \"Students fill the reading tables.\"" +
        "      }," +
        "      \"attribute\": {" +
        "        \"INTELLIGENCE\": \"You know exactly which section you need.\"," +
        "        \"MEMORY\": \"Titles and call numbers leap out at you.\"" +
        "      }" +
        "    }," +
        "    \"hospital_small\": {" +
        "      \"default\": \"You enter the hospital.\"," +
        "      \"time\": {" +
        "        \"morning\": \"Morning rounds begin.\"," +
        "        \"night\": \"Night staff maintain quiet vigilance.\"" +
        "      }," +
        "      \"attribute\": {" +
        "        \"INTELLIGENCE\": \"Medical charts don't escape your notice.\"," +
        "        \"EMPATHY\": \"Every person carries a weight you feel.\"" +
        "      }" +
        "    }" +
        "  }" +
        "}";

    private NovelTextEngine buildingEngine;

    @org.junit.Before
    public void setUpBuildingEngine() {
        buildingEngine = NovelTextEngine.fromJsonString(BUILDING_JSON);
    }

    @Test
    public void testGymFitnessCenterHasDefaultText() {
        String desc = buildingEngine.getDescription("gym_fitness_center", TimeOfDay.AFTERNOON,
                Collections.<String, Integer>emptyMap());
        assertEquals("gym_fitness_center default should be returned with no attribute match",
                "You enter the fitness center.", desc);
    }

    @Test
    public void testGymFitnessCenterStrengthAttribute() {
        Map<String, Integer> attrs = new HashMap<String, Integer>();
        attrs.put("STRENGTH", 9);
        attrs.put("CHARISMA", 2);
        String desc = buildingEngine.getDescription("gym_fitness_center", TimeOfDay.AFTERNOON, attrs);
        assertEquals("STRENGTH should trigger gym fitness center strength variant",
                "You feel right at home — this is your element.", desc);
    }

    @Test
    public void testGymFitnessCenterStaminaAttribute() {
        Map<String, Integer> attrs = new HashMap<String, Integer>();
        attrs.put("STAMINA", 8);
        attrs.put("INTELLIGENCE", 3);
        String desc = buildingEngine.getDescription("gym_fitness_center", TimeOfDay.MORNING, attrs);
        assertEquals("STAMINA should trigger gym fitness center stamina variant",
                "Your eyes go straight to the cardio section.", desc);
    }

    @Test
    public void testGymFitnessCenterAgilityAttribute() {
        Map<String, Integer> attrs = new HashMap<String, Integer>();
        attrs.put("AGILITY", 7);
        String desc = buildingEngine.getDescription("gym_fitness_center", TimeOfDay.EVENING, attrs);
        assertEquals("AGILITY should trigger gym fitness center agility variant",
                "You size up the open training floor immediately.", desc);
    }

    @Test
    public void testGymFitnessCenterTimeEvening() {
        String desc = buildingEngine.getDescription("gym_fitness_center", TimeOfDay.EVENING,
                Collections.<String, Integer>emptyMap());
        assertEquals("Evening time variant should be returned",
                "The after-work crowd fills every station.", desc);
    }

    @Test
    public void testPoliceStationPerceptionAttribute() {
        Map<String, Integer> attrs = new HashMap<String, Integer>();
        attrs.put("PERCEPTION", 10);
        String desc = buildingEngine.getDescription("police_station", TimeOfDay.MORNING, attrs);
        assertEquals("PERCEPTION should trigger police station perception variant",
                "You spot tension on the case board immediately.", desc);
    }

    @Test
    public void testPoliceStationIntimidationAttribute() {
        Map<String, Integer> attrs = new HashMap<String, Integer>();
        attrs.put("INTIMIDATION", 8);
        attrs.put("CHARISMA", 2);
        String desc = buildingEngine.getDescription("police_station", TimeOfDay.AFTERNOON, attrs);
        assertEquals("INTIMIDATION should trigger police station intimidation variant",
                "Heads turn as you walk in.", desc);
    }

    @Test
    public void testPoliceStationNightTimeVariant() {
        String desc = buildingEngine.getDescription("police_station", TimeOfDay.NIGHT,
                Collections.<String, Integer>emptyMap());
        assertEquals("Night time variant should be returned for police station",
                "Night-duty officers keep watch.", desc);
    }

    @Test
    public void testFireStationStrengthAttribute() {
        Map<String, Integer> attrs = new HashMap<String, Integer>();
        attrs.put("STRENGTH", 9);
        String desc = buildingEngine.getDescription("fire_station", TimeOfDay.MORNING, attrs);
        assertEquals("STRENGTH should trigger fire station strength variant",
                "The crew recognises your build immediately.", desc);
    }

    @Test
    public void testFireStationStaminaAttribute() {
        Map<String, Integer> attrs = new HashMap<String, Integer>();
        attrs.put("STAMINA", 8);
        String desc = buildingEngine.getDescription("fire_station", TimeOfDay.AFTERNOON, attrs);
        assertEquals("STAMINA should trigger fire station stamina variant",
                "The physical demands match your output.", desc);
    }

    @Test
    public void testLibraryIntelligenceAttribute() {
        Map<String, Integer> attrs = new HashMap<String, Integer>();
        attrs.put("INTELLIGENCE", 9);
        String desc = buildingEngine.getDescription("library", TimeOfDay.AFTERNOON, attrs);
        assertEquals("INTELLIGENCE should trigger library intelligence variant",
                "You know exactly which section you need.", desc);
    }

    @Test
    public void testLibraryMemoryAttribute() {
        Map<String, Integer> attrs = new HashMap<String, Integer>();
        attrs.put("MEMORY", 8);
        attrs.put("CHARISMA", 3);
        String desc = buildingEngine.getDescription("library", TimeOfDay.MORNING, attrs);
        assertEquals("MEMORY should trigger library memory variant",
                "Titles and call numbers leap out at you.", desc);
    }

    @Test
    public void testHospitalSmallIntelligenceAttribute() {
        Map<String, Integer> attrs = new HashMap<String, Integer>();
        attrs.put("INTELLIGENCE", 9);
        String desc = buildingEngine.getDescription("hospital_small", TimeOfDay.AFTERNOON, attrs);
        assertEquals("INTELLIGENCE should trigger hospital intelligence variant",
                "Medical charts don't escape your notice.", desc);
    }

    @Test
    public void testHospitalSmallEmpathyAttribute() {
        Map<String, Integer> attrs = new HashMap<String, Integer>();
        attrs.put("EMPATHY", 8);
        attrs.put("STRENGTH", 2);
        String desc = buildingEngine.getDescription("hospital_small", TimeOfDay.MORNING, attrs);
        assertEquals("EMPATHY should trigger hospital empathy variant",
                "Every person carries a weight you feel.", desc);
    }

    @Test
    public void testHospitalSmallNightVariant() {
        String desc = buildingEngine.getDescription("hospital_small", TimeOfDay.NIGHT,
                Collections.<String, Integer>emptyMap());
        assertEquals("Night time variant should be returned for hospital",
                "Night staff maintain quiet vigilance.", desc);
    }

    @Test
    public void testBuildingIdDefaultReturnedWhenNoMatch() {
        // No attribute variant exists for STEALTH at the police_station entry in BUILDING_JSON
        Map<String, Integer> attrs = new HashMap<String, Integer>();
        attrs.put("STEALTH", 10);
        // Should fall through to time variant
        String desc = buildingEngine.getDescription("police_station", TimeOfDay.MORNING, attrs);
        assertEquals("Should fall through to time variant when top attribute has no entry",
                "Morning briefings fill the station.", desc);
    }

    // ===== Gender variant tests =====

    private static final String GENDER_JSON =
        "{" +
        "  \"version\": \"1.2\"," +
        "  \"language\": \"en\"," +
        "  \"descriptions\": {" +
        "    \"gym_fitness_center\": {" +
        "      \"default\": \"You enter the fitness center.\"," +
        "      \"time\": {" +
        "        \"morning\": \"Early risers power through their routines.\"," +
        "        \"evening\": \"The after-work crowd fills every station.\"" +
        "      }," +
        "      \"attribute\": {" +
        "        \"STRENGTH\": \"You feel right at home — this is your element.\"" +
        "      }," +
        "      \"gender\": {" +
        "        \"male\": \"Two regulars offer you a spot within minutes of your warm-up.\"," +
        "        \"female\": \"You move to the free weights. Three people stop to watch. You ignore them.\"" +
        "      }" +
        "    }," +
        "    \"police_station\": {" +
        "      \"default\": \"You enter the police station.\"," +
        "      \"time\": {" +
        "        \"morning\": \"Morning briefings fill the station.\"" +
        "      }," +
        "      \"gender\": {" +
        "        \"male\": \"Officers treat you with guarded professional neutrality.\"," +
        "        \"female\": \"Some officers register surprise. You note which ones and start there.\"" +
        "      }" +
        "    }," +
        "    \"library\": {" +
        "      \"default\": \"You walk into the library.\"," +
        "      \"gender\": {" +
        "        \"male\": \"The librarian leaves you to your research without comment.\"," +
        "        \"female\": \"The librarian offers to pull additional references. You accept.\"" +
        "      }" +
        "    }" +
        "  }" +
        "}";

    private NovelTextEngine genderEngine;

    @org.junit.Before
    public void setUpGenderEngine() {
        genderEngine = NovelTextEngine.fromJsonString(GENDER_JSON);
    }

    @Test
    public void testMaleGenderVariantSelected() {
        String desc = genderEngine.getDescription("gym_fitness_center", TimeOfDay.AFTERNOON,
                Collections.<String, Integer>emptyMap(), "male");
        assertEquals("Male gender variant should be selected",
                "Two regulars offer you a spot within minutes of your warm-up.", desc);
    }

    @Test
    public void testFemaleGenderVariantSelected() {
        String desc = genderEngine.getDescription("gym_fitness_center", TimeOfDay.AFTERNOON,
                Collections.<String, Integer>emptyMap(), "female");
        assertEquals("Female gender variant should be selected",
                "You move to the free weights. Three people stop to watch. You ignore them.", desc);
    }

    @Test
    public void testGenderCaseInsensitive() {
        String descUpper = genderEngine.getDescription("police_station", TimeOfDay.AFTERNOON,
                Collections.<String, Integer>emptyMap(), "Male");
        String descLower = genderEngine.getDescription("police_station", TimeOfDay.AFTERNOON,
                Collections.<String, Integer>emptyMap(), "male");
        assertEquals("Gender matching should be case-insensitive", descUpper, descLower);
    }

    @Test
    public void testAttributeTakesPriorityOverGender() {
        // STRENGTH has a variant — should beat the gender variant
        Map<String, Integer> attrs = new HashMap<String, Integer>();
        attrs.put("STRENGTH", 9);
        String desc = genderEngine.getDescription("gym_fitness_center", TimeOfDay.MORNING, attrs, "female");
        assertEquals("Attribute variant should take priority over gender variant",
                "You feel right at home — this is your element.", desc);
    }

    @Test
    public void testGenderTakesPriorityOverTime() {
        // police_station has no attribute variants but has gender and time variants
        String desc = genderEngine.getDescription("police_station", TimeOfDay.MORNING,
                Collections.<String, Integer>emptyMap(), "female");
        assertEquals("Gender variant should take priority over time variant",
                "Some officers register surprise. You note which ones and start there.", desc);
    }

    @Test
    public void testNullGenderFallsToTimeVariant() {
        String desc = genderEngine.getDescription("police_station", TimeOfDay.MORNING,
                Collections.<String, Integer>emptyMap(), null);
        assertEquals("Null gender should fall through to time variant",
                "Morning briefings fill the station.", desc);
    }

    @Test
    public void testUnknownGenderFallsToTimeVariant() {
        String desc = genderEngine.getDescription("police_station", TimeOfDay.MORNING,
                Collections.<String, Integer>emptyMap(), "nonbinary");
        assertEquals("Unknown gender should fall through to time variant",
                "Morning briefings fill the station.", desc);
    }

    @Test
    public void testGenderWithNoTimeVariantFallsToDefault() {
        // library entry in GENDER_JSON has no time variants
        String desc = genderEngine.getDescription("library", TimeOfDay.MORNING,
                Collections.<String, Integer>emptyMap(), "unknown_gender");
        assertEquals("Should fall back to default when gender and time both miss",
                "You walk into the library.", desc);
    }

    @Test
    public void testMaleGenderLibrary() {
        String desc = genderEngine.getDescription("library", TimeOfDay.MORNING,
                Collections.<String, Integer>emptyMap(), "male");
        assertEquals("Male gender variant should be selected for library",
                "The librarian leaves you to your research without comment.", desc);
    }

    @Test
    public void testFemaleGenderLibrary() {
        String desc = genderEngine.getDescription("library", TimeOfDay.MORNING,
                Collections.<String, Integer>emptyMap(), "female");
        assertEquals("Female gender variant should be selected for library",
                "The librarian offers to pull additional references. You accept.", desc);
    }

    @Test
    public void testThreeArgOverloadStillWorksAfterGenderAddition() {
        // Ensure backwards-compatible 3-arg overload still works (skips gender check)
        String desc = genderEngine.getDescription("police_station", TimeOfDay.MORNING,
                Collections.<String, Integer>emptyMap());
        assertEquals("3-arg overload should skip gender and fall through to time variant",
                "Morning briefings fill the station.", desc);
    }

    @Test
    public void testHourOverloadWithGender() {
        // Hour 15 → AFTERNOON; police_station has no afternoon time variant, but has gender
        String desc = genderEngine.getDescription("police_station", 15,
                Collections.<String, Integer>emptyMap(), "male");
        assertEquals("Hour-based overload should respect gender selection",
                "Officers treat you with guarded professional neutrality.", desc);
    }
}
