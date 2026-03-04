package eb.framework1.generator;

import eb.framework1.city.*;


import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
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

    // ===== Location-based variant selection =====

    private static final String MULTI_VARIANT_JSON =
        "{" +
        "  \"version\": \"1.3\"," +
        "  \"language\": \"en\"," +
        "  \"descriptions\": {" +
        "    \"gym\": [" +
        "      {" +
        "        \"default\": \"Gym variant A — iron and sweat.\"," +
        "        \"time\": {" +
        "          \"morning\": \"Gym-A morning: early crowd.\"," +
        "          \"evening\": \"Gym-A evening: after-work rush.\"" +
        "        }," +
        "        \"attribute\": {" +
        "          \"STRENGTH\": \"Gym-A strength: you feel at home.\"" +
        "        }," +
        "        \"gender\": {" +
        "          \"male\": \"Gym-A male.\"," +
        "          \"female\": \"Gym-A female.\"" +
        "        }" +
        "      }," +
        "      {" +
        "        \"default\": \"Gym variant B — rubber floors.\"," +
        "        \"time\": {" +
        "          \"morning\": \"Gym-B morning: machines taken.\"" +
        "        }," +
        "        \"attribute\": {" +
        "          \"STAMINA\": \"Gym-B stamina: cardio section.\"" +
        "        }," +
        "        \"gender\": {" +
        "          \"male\": \"Gym-B male.\"," +
        "          \"female\": \"Gym-B female.\"" +
        "        }" +
        "      }" +
        "    ]," +
        "    \"office\": {" +
        "      \"default\": \"Single-variant office.\"," +
        "      \"time\": {" +
        "        \"morning\": \"Office morning.\"" +
        "      }" +
        "    }" +
        "  }," +
        "  \"improvements\": {" +
        "    \"WiFi\": [" +
        "      {" +
        "        \"default\": \"WiFi variant A: router logs every device.\"," +
        "        \"gender\": {" +
        "          \"male\": \"WiFi-A male: access logs requested.\"," +
        "          \"female\": \"WiFi-A female: device history pulled.\"" +
        "        }" +
        "      }," +
        "      {" +
        "        \"default\": \"WiFi variant B: every connection is a timestamp.\"," +
        "        \"gender\": {" +
        "          \"male\": \"WiFi-B male: IT manager prints logs.\"," +
        "          \"female\": \"WiFi-B female: device outside footprint.\"" +
        "        }" +
        "      }" +
        "    ]," +
        "    \"Security Camera\": {" +
        "      \"default\": \"Single-variant camera.\"" +
        "    }" +
        "  }" +
        "}";

    // Known-good location constants:
    //   "A1" hashes to variant index 0 for count=2
    //   "B1" hashes to variant index 1 for count=2
    private static final String LOC_VARIANT_A = "A1";
    private static final String LOC_VARIANT_B = "B1";

    private NovelTextEngine multiVariantEngine;

    @org.junit.Before
    public void setUpMultiVariantEngine() {
        multiVariantEngine = NovelTextEngine.fromJsonString(MULTI_VARIANT_JSON);
    }

    // --- selectVariantIndex unit tests ---

    @Test
    public void testSelectVariantIndexNullLocationReturnsZero() {
        assertEquals(0, NovelTextEngine.selectVariantIndex(null, 3));
    }

    @Test
    public void testSelectVariantIndexEmptyLocationReturnsZero() {
        assertEquals(0, NovelTextEngine.selectVariantIndex("", 3));
    }

    @Test
    public void testSelectVariantIndexSingleVariantAlwaysReturnsZero() {
        assertEquals(0, NovelTextEngine.selectVariantIndex("G6", 1));
        assertEquals(0, NovelTextEngine.selectVariantIndex("H1", 1));
        assertEquals(0, NovelTextEngine.selectVariantIndex("A1", 1));
    }

    @Test
    public void testSelectVariantIndexInRange() {
        int variantCount = 5;
        for (String loc : new String[]{"A1", "B2", "C3", "G6", "H1", "P16"}) {
            int idx = NovelTextEngine.selectVariantIndex(loc, variantCount);
            assertTrue("Index must be in [0, variantCount)", idx >= 0 && idx < variantCount);
        }
    }

    @Test
    public void testSelectVariantIndexDeterministic() {
        // Same location must always produce the same index
        String loc = "G6";
        int first = NovelTextEngine.selectVariantIndex(loc, 2);
        for (int i = 0; i < 10; i++) {
            assertEquals("selectVariantIndex must be deterministic for same location",
                    first, NovelTextEngine.selectVariantIndex(loc, 2));
        }
    }

    @Test
    public void testSelectVariantIndexKnownValues() {
        // A1 → 0, B1 → 1 for count=2 (used as anchors throughout the location tests)
        assertEquals("A1 should map to variant index 0", 0,
                NovelTextEngine.selectVariantIndex(LOC_VARIANT_A, 2));
        assertEquals("B1 should map to variant index 1", 1,
                NovelTextEngine.selectVariantIndex(LOC_VARIANT_B, 2));
    }

    @Test
    public void testSelectVariantIndexDifferentLocations() {
        // A1 and B1 map to different variants for count=2
        int a1 = NovelTextEngine.selectVariantIndex(LOC_VARIANT_A, 2);
        int b1 = NovelTextEngine.selectVariantIndex(LOC_VARIANT_B, 2);
        assertNotEquals("A1 and B1 should select different variants", a1, b1);
    }

    // --- Multi-variant loading and dispatch ---

    @Test
    public void testMultiVariantEngineLoads() {
        assertNotNull(multiVariantEngine);
    }

    @Test
    public void testNullLocationUsesFirstVariant() {
        // null location → index 0 → variant A default
        String desc = multiVariantEngine.getDescription("gym", (String) null,
                TimeOfDay.AFTERNOON, Collections.<String, Integer>emptyMap(), null);
        assertEquals("Null location should use first variant",
                "Gym variant A — iron and sweat.", desc);
    }

    @Test
    public void testLocationA1AlwaysReturnsSameVariant() {
        String first = multiVariantEngine.getDescription("gym", LOC_VARIANT_A,
                TimeOfDay.AFTERNOON, Collections.<String, Integer>emptyMap(), null);
        String second = multiVariantEngine.getDescription("gym", LOC_VARIANT_A,
                TimeOfDay.AFTERNOON, Collections.<String, Integer>emptyMap(), null);
        assertEquals("Same location must always return same variant", first, second);
    }

    @Test
    public void testDifferentLocationsReturnDifferentVariants() {
        // A1 → variant A default, B1 → variant B default
        String a1 = multiVariantEngine.getDescription("gym", LOC_VARIANT_A,
                TimeOfDay.AFTERNOON, Collections.<String, Integer>emptyMap(), null);
        String b1 = multiVariantEngine.getDescription("gym", LOC_VARIANT_B,
                TimeOfDay.AFTERNOON, Collections.<String, Integer>emptyMap(), null);
        assertNotEquals("A1 and B1 should return different variant defaults", a1, b1);
    }

    @Test
    public void testLocationA1SelectsVariantADefault() {
        String desc = multiVariantEngine.getDescription("gym", LOC_VARIANT_A,
                TimeOfDay.AFTERNOON, Collections.<String, Integer>emptyMap(), null);
        assertEquals("A1 should return variant A default",
                "Gym variant A — iron and sweat.", desc);
    }

    @Test
    public void testLocationB1SelectsVariantBDefault() {
        String desc = multiVariantEngine.getDescription("gym", LOC_VARIANT_B,
                TimeOfDay.AFTERNOON, Collections.<String, Integer>emptyMap(), null);
        assertEquals("B1 should return variant B default",
                "Gym variant B — rubber floors.", desc);
    }

    @Test
    public void testVariantATimeOfDayMorning() {
        // A1 → variant A → morning time variant
        String desc = multiVariantEngine.getDescription("gym", LOC_VARIANT_A,
                TimeOfDay.MORNING, Collections.<String, Integer>emptyMap(), null);
        assertEquals("Variant A morning time variant", "Gym-A morning: early crowd.", desc);
    }

    @Test
    public void testVariantBTimeOfDayMorning() {
        // B1 → variant B → morning time variant
        String desc = multiVariantEngine.getDescription("gym", LOC_VARIANT_B,
                TimeOfDay.MORNING, Collections.<String, Integer>emptyMap(), null);
        assertEquals("Variant B morning time variant", "Gym-B morning: machines taken.", desc);
    }

    @Test
    public void testVariantAAttributeStrength() {
        Map<String, Integer> attrs = new HashMap<String, Integer>();
        attrs.put("STRENGTH", 9);
        String desc = multiVariantEngine.getDescription("gym", LOC_VARIANT_A,
                TimeOfDay.AFTERNOON, attrs, null);
        assertEquals("Variant A STRENGTH attribute", "Gym-A strength: you feel at home.", desc);
    }

    @Test
    public void testVariantBAttributeStamina() {
        Map<String, Integer> attrs = new HashMap<String, Integer>();
        attrs.put("STAMINA", 9);
        String desc = multiVariantEngine.getDescription("gym", LOC_VARIANT_B,
                TimeOfDay.AFTERNOON, attrs, null);
        assertEquals("Variant B STAMINA attribute", "Gym-B stamina: cardio section.", desc);
    }

    @Test
    public void testVariantAGenderMale() {
        String desc = multiVariantEngine.getDescription("gym", LOC_VARIANT_A,
                TimeOfDay.AFTERNOON, Collections.<String, Integer>emptyMap(), "male");
        assertEquals("Variant A male gender", "Gym-A male.", desc);
    }

    @Test
    public void testVariantBGenderFemale() {
        String desc = multiVariantEngine.getDescription("gym", LOC_VARIANT_B,
                TimeOfDay.AFTERNOON, Collections.<String, Integer>emptyMap(), "female");
        assertEquals("Variant B female gender", "Gym-B female.", desc);
    }

    @Test
    public void testSingleVariantWithLocationIgnoresLocation() {
        // "office" has only one variant — any location returns the same text
        String descA1 = multiVariantEngine.getDescription("office", LOC_VARIANT_A,
                TimeOfDay.AFTERNOON, Collections.<String, Integer>emptyMap(), null);
        String descB1 = multiVariantEngine.getDescription("office", LOC_VARIANT_B,
                TimeOfDay.AFTERNOON, Collections.<String, Integer>emptyMap(), null);
        assertEquals("Single-variant entry must be location-independent", descA1, descB1);
        assertEquals("Single-variant default text", "Single-variant office.", descA1);
    }

    @Test
    public void testSingleVariantObjectFormatWithLocationStillWorks() {
        // Existing single-object format still works when a location is provided
        String desc = multiVariantEngine.getDescription("office", LOC_VARIANT_A,
                TimeOfDay.MORNING, Collections.<String, Integer>emptyMap(), null);
        assertEquals("Single-object format + location should return time variant",
                "Office morning.", desc);
    }

    // --- Multi-variant improvements ---

    @Test
    public void testImprovementNullLocationUsesFirstVariant() {
        String desc = multiVariantEngine.getImprovementDescription("WiFi", null, null);
        assertEquals("Null location should use first WiFi variant default",
                "WiFi variant A: router logs every device.", desc);
    }

    @Test
    public void testImprovementLocationA1AlwaysReturnsSameVariant() {
        String first  = multiVariantEngine.getImprovementDescription("WiFi", LOC_VARIANT_A, null);
        String second = multiVariantEngine.getImprovementDescription("WiFi", LOC_VARIANT_A, null);
        assertEquals("Same location must always return same WiFi variant", first, second);
    }

    @Test
    public void testImprovementDifferentLocationsReturnDifferentVariants() {
        String a1 = multiVariantEngine.getImprovementDescription("WiFi", LOC_VARIANT_A, null);
        String b1 = multiVariantEngine.getImprovementDescription("WiFi", LOC_VARIANT_B, null);
        assertNotEquals("A1 and B1 should return different WiFi variant defaults", a1, b1);
    }

    @Test
    public void testImprovementVariantAGenderMale() {
        String desc = multiVariantEngine.getImprovementDescription("WiFi", LOC_VARIANT_A, "male");
        assertEquals("WiFi variant A male gender", "WiFi-A male: access logs requested.", desc);
    }

    @Test
    public void testImprovementVariantBGenderFemale() {
        String desc = multiVariantEngine.getImprovementDescription("WiFi", LOC_VARIANT_B, "female");
        assertEquals("WiFi variant B female gender", "WiFi-B female: device outside footprint.", desc);
    }

    @Test
    public void testSingleVariantImprovementWithLocationIgnoresLocation() {
        String descA1 = multiVariantEngine.getImprovementDescription("Security Camera", LOC_VARIANT_A, null);
        String descB1 = multiVariantEngine.getImprovementDescription("Security Camera", LOC_VARIANT_B, null);
        assertEquals("Single-variant improvement must be location-independent", descA1, descB1);
        assertEquals("Single-variant camera default", "Single-variant camera.", descA1);
    }

    // --- Location overload of getDescription with hour ---

    @Test
    public void testLocationHourOverload() {
        // A1 → variant A; hour 9 → MORNING → time variant
        String desc = multiVariantEngine.getDescription("gym", LOC_VARIANT_A, 9,
                Collections.<String, Integer>emptyMap(), null);
        assertEquals("Location + hour overload should resolve time variant",
                "Gym-A morning: early crowd.", desc);
    }

    // --- Backward-compat: existing no-location calls still work ---

    @Test
    public void testExistingNoLocationCallsStillWorkOnMultiVariantEntry() {
        // The original 4-arg overload (no location) must still work for multi-variant entries
        // It will always use variant at index 0
        String desc = multiVariantEngine.getDescription("gym", TimeOfDay.AFTERNOON,
                Collections.<String, Integer>emptyMap(), null);
        assertEquals("No-location call on multi-variant entry should use first variant",
                "Gym variant A — iron and sweat.", desc);
    }

    @Test
    public void testExistingNoLocationImprovementCallStillWorksOnMultiVariantEntry() {
        String desc = multiVariantEngine.getImprovementDescription("WiFi", null);
        assertEquals("No-location improvement call should use first variant default",
                "WiFi variant A: router logs every device.", desc);
    }

    // ===== getVariantCount tests =====

    @Test
    public void testGetVariantCountSingleVariant() {
        assertEquals("Single-variant entry should return 1",
                1, engine.getVariantCount("gym"));
    }

    @Test
    public void testGetVariantCountMultiVariant() {
        assertEquals("Multi-variant gym entry should return 2",
                2, multiVariantEngine.getVariantCount("gym"));
    }

    @Test
    public void testGetVariantCountSingleVariantOffice() {
        assertEquals("Single-variant office in multi-variant engine should return 1",
                1, multiVariantEngine.getVariantCount("office"));
    }

    @Test
    public void testGetVariantCountUnknownKey() {
        assertEquals("Unknown key should return 0",
                0, engine.getVariantCount("nonexistent"));
    }

    @Test
    public void testGetVariantCountNullKey() {
        assertEquals("Null key should return 0",
                0, engine.getVariantCount(null));
    }

    // ===== recommendedVariantCount tests (birthday problem) =====

    @Test
    public void testRecommendedVariantCountSingleInstance() {
        assertEquals("Single instance needs only 1 variant",
                1, NovelTextEngine.recommendedVariantCount(1));
    }

    @Test
    public void testRecommendedVariantCountZeroInstances() {
        assertEquals("Zero instances needs only 1 variant",
                1, NovelTextEngine.recommendedVariantCount(0));
    }

    @Test
    public void testRecommendedVariantCountNegativeInstances() {
        assertEquals("Negative instances needs only 1 variant",
                1, NovelTextEngine.recommendedVariantCount(-1));
    }

    @Test
    public void testRecommendedVariantCountTwoInstances() {
        // k = 2*1 / (2*ln2) = 2/1.386 ≈ 1.44 → ceil = 2
        assertEquals("Two instances should need 2 variants",
                2, NovelTextEngine.recommendedVariantCount(2));
    }

    @Test
    public void testRecommendedVariantCountThreeInstances() {
        // k = 3*2 / (2*ln2) = 6/1.386 ≈ 4.33 → ceil = 5
        assertEquals("Three instances should need 5 variants",
                5, NovelTextEngine.recommendedVariantCount(3));
    }

    @Test
    public void testRecommendedVariantCountTenInstances() {
        // k = 10*9 / (2*ln2) = 90/1.386 ≈ 64.9 → ceil = 65
        assertEquals("Ten instances should need 65 variants",
                65, NovelTextEngine.recommendedVariantCount(10));
    }

    @Test
    public void testRecommendedVariantCountMonotonicallyIncreasing() {
        int prev = NovelTextEngine.recommendedVariantCount(1);
        for (int n = 2; n <= 20; n++) {
            int curr = NovelTextEngine.recommendedVariantCount(n);
            assertTrue("recommendedVariantCount must be non-decreasing: n=" + n,
                    curr >= prev);
            prev = curr;
        }
    }

    @Test
    public void testRecommendedVariantCountAlwaysAtLeastOne() {
        for (int n = 0; n <= 50; n++) {
            assertTrue("recommendedVariantCount must be >= 1 for n=" + n,
                    NovelTextEngine.recommendedVariantCount(n) >= 1);
        }
    }

    // ===== recommendedVariantCounts (batch) tests =====

    @Test
    public void testRecommendedVariantCountsNullList() {
        Map<String, Integer> result = NovelTextEngine.recommendedVariantCounts(null, 200);
        assertTrue("Null building list should return empty map", result.isEmpty());
    }

    @Test
    public void testRecommendedVariantCountsEmptyList() {
        Map<String, Integer> result = NovelTextEngine.recommendedVariantCounts(
                Collections.<BuildingDefinition>emptyList(), 200);
        assertTrue("Empty building list should return empty map", result.isEmpty());
    }

    @Test
    public void testRecommendedVariantCountsZeroCells() {
        List<BuildingDefinition> buildings = new ArrayList<>();
        buildings.add(new BuildingDefinition("apt", "Apartment", "residential",
                1, 5, 4, 20, 10.0, "An apartment", null));
        Map<String, Integer> result = NovelTextEngine.recommendedVariantCounts(buildings, 0);
        assertTrue("Zero cells should return empty map", result.isEmpty());
    }

    @Test
    public void testRecommendedVariantCountsWithBuildings() {
        List<BuildingDefinition> buildings = new ArrayList<>();
        // 50% of 100 cells = 50 instances
        buildings.add(new BuildingDefinition("apt", "Apartment", "residential",
                1, 5, 4, 20, 50.0, "An apartment", null));
        // 50% of 100 cells = 50 instances
        buildings.add(new BuildingDefinition("shop", "Shop", "commercial",
                1, 2, 1, 10, 50.0, "A shop", null));

        Map<String, Integer> result = NovelTextEngine.recommendedVariantCounts(buildings, 100);
        assertEquals("Should have entries for both buildings", 2, result.size());
        assertTrue("Apartment should have a recommended count", result.containsKey("apt"));
        assertTrue("Shop should have a recommended count", result.containsKey("shop"));
        // Both have 50 expected instances → k = 50*49/(2*ln2) ≈ 1766
        assertTrue("High-frequency buildings should need many variants",
                result.get("apt") > 100);
        assertEquals("Both should have same recommendation since same percentage",
                result.get("apt"), result.get("shop"));
    }

    @Test
    public void testRecommendedVariantCountsLowPercentageBuilding() {
        List<BuildingDefinition> buildings = new ArrayList<>();
        // 1% out of 100% = 1% of 200 cells ≈ 2 instances
        buildings.add(new BuildingDefinition("rare", "Rare Building", "special",
                1, 1, 1, 5, 1.0, "Rare", null));
        // 99% of 200 cells ≈ 198 instances
        buildings.add(new BuildingDefinition("common", "Common Building", "common",
                1, 1, 1, 5, 99.0, "Common", null));

        Map<String, Integer> result = NovelTextEngine.recommendedVariantCounts(buildings, 200);
        assertTrue("Rare building should need fewer variants than common",
                result.get("rare") < result.get("common"));
    }

    @Test
    public void testRecommendedVariantCountsAllZeroPercentage() {
        List<BuildingDefinition> buildings = new ArrayList<>();
        buildings.add(new BuildingDefinition("a", "A", "cat", 1, 1, 1, 5, 0.0, "A", null));
        buildings.add(new BuildingDefinition("b", "B", "cat", 1, 1, 1, 5, 0.0, "B", null));

        Map<String, Integer> result = NovelTextEngine.recommendedVariantCounts(buildings, 200);
        assertEquals("All-zero percentages should default to 1 variant each", 2, result.size());
        assertEquals("Zero percentage should recommend 1 variant",
                Integer.valueOf(1), result.get("a"));
    }

    // ===== getStateDescription tests =====

    private static final String STATE_JSON =
        "{" +
        "  \"version\": \"1.0\"," +
        "  \"language\": \"en\"," +
        "  \"descriptions\": {" +
        "    \"warehouse\": {" +
        "      \"default\": \"A large industrial building.\"," +
        "      \"state\": {" +
        "        \"good\": \"Clean steel panels and a freshly painted apron.\"," +
        "        \"normal\": \"Surface rust at the lower wall joints and tyre marks on the apron.\"," +
        "        \"bad\": \"Corrosion has spread up the lower panels and the apron is cracked.\"" +
        "      }" +
        "    }," +
        "    \"shop\": {" +
        "      \"default\": \"A small retail shop.\"" +
        "    }" +
        "  }" +
        "}";

    @Test
    public void testGetStateDescriptionGood() {
        NovelTextEngine stateEngine = NovelTextEngine.fromJsonString(STATE_JSON);
        String result = stateEngine.getStateDescription("warehouse", "good");
        assertEquals("Good state should return good description",
                "Clean steel panels and a freshly painted apron.", result);
    }

    @Test
    public void testGetStateDescriptionNormal() {
        NovelTextEngine stateEngine = NovelTextEngine.fromJsonString(STATE_JSON);
        String result = stateEngine.getStateDescription("warehouse", "normal");
        assertEquals("Normal state should return normal description",
                "Surface rust at the lower wall joints and tyre marks on the apron.", result);
    }

    @Test
    public void testGetStateDescriptionBad() {
        NovelTextEngine stateEngine = NovelTextEngine.fromJsonString(STATE_JSON);
        String result = stateEngine.getStateDescription("warehouse", "bad");
        assertEquals("Bad state should return bad description",
                "Corrosion has spread up the lower panels and the apron is cracked.", result);
    }

    @Test
    public void testGetStateDescriptionFallsBackToDefault() {
        NovelTextEngine stateEngine = NovelTextEngine.fromJsonString(STATE_JSON);
        // 'shop' has no state variants — should return default
        String result = stateEngine.getStateDescription("shop", "good");
        assertEquals("Missing state variants should fall back to default",
                "A small retail shop.", result);
    }

    @Test
    public void testGetStateDescriptionUnknownStateFallsBackToDefault() {
        NovelTextEngine stateEngine = NovelTextEngine.fromJsonString(STATE_JSON);
        String result = stateEngine.getStateDescription("warehouse", "unknown");
        assertEquals("Unknown state should fall back to default",
                "A large industrial building.", result);
    }

    @Test
    public void testGetStateDescriptionNullStateFallsBackToDefault() {
        NovelTextEngine stateEngine = NovelTextEngine.fromJsonString(STATE_JSON);
        String result = stateEngine.getStateDescription("warehouse", null);
        assertEquals("Null state should fall back to default",
                "A large industrial building.", result);
    }

    @Test
    public void testGetStateDescriptionUnknownKeyReturnsEmpty() {
        NovelTextEngine stateEngine = NovelTextEngine.fromJsonString(STATE_JSON);
        String result = stateEngine.getStateDescription("nonexistent", "good");
        assertEquals("Unknown key should return empty string", "", result);
    }

    @Test
    public void testGetStateDescriptionWithLocationGood() {
        NovelTextEngine stateEngine = NovelTextEngine.fromJsonString(STATE_JSON);
        String result = stateEngine.getStateDescription("warehouse", "G6", "good");
        assertEquals("Location-based good state should return good description",
                "Clean steel panels and a freshly painted apron.", result);
    }

    @Test
    public void testGetStateDescriptionCaseInsensitive() {
        NovelTextEngine stateEngine = NovelTextEngine.fromJsonString(STATE_JSON);
        String upper = stateEngine.getStateDescription("warehouse", "GOOD");
        String lower = stateEngine.getStateDescription("warehouse", "good");
        assertEquals("State lookup should be case-insensitive", lower, upper);
    }

    // ===== your_office and hotel room description entry tests =====

    private static final String OFFICE_HOTEL_JSON =
        "{" +
        "  \"version\": \"1.0\"," +
        "  \"language\": \"en\"," +
        "  \"descriptions\": {" +
        "    \"your_office\": {" +
        "      \"default\": \"Your office. Small, purposeful, and yours.\"," +
        "      \"time\": {" +
        "        \"morning\": \"Morning light fills the room.\"," +
        "        \"night\": \"Late. The desk lamp is the only light.\"" +
        "      }," +
        "      \"state\": {" +
        "        \"good\": \"The room is clean and well-ordered.\"," +
        "        \"normal\": \"A lived-in office.\"," +
        "        \"bad\": \"The room shows its age.\"" +
        "      }" +
        "    }," +
        "    \"hotel_budget_room\": {" +
        "      \"default\": \"A budget hotel room.\"," +
        "      \"state\": {" +
        "        \"good\": \"The room is clean and fittings are intact.\"," +
        "        \"normal\": \"The room has the worn look of high turnover.\"," +
        "        \"bad\": \"The room presents a catalogue of deferred maintenance.\"" +
        "      }" +
        "    }," +
        "    \"hotel_business_room\": {" +
        "      \"default\": \"A business hotel room.\"," +
        "      \"state\": {" +
        "        \"good\": \"The room presents clean lines and recent refurbishment.\"," +
        "        \"normal\": \"The room functions well.\"," +
        "        \"bad\": \"The room shows the strain.\"" +
        "      }" +
        "    }," +
        "    \"hotel_luxury_room\": {" +
        "      \"default\": \"A luxury hotel room.\"," +
        "      \"state\": {" +
        "        \"good\": \"The room is immaculate.\"," +
        "        \"normal\": \"The room retains the quality.\"," +
        "        \"bad\": \"The room has not been maintained.\"" +
        "      }" +
        "    }" +
        "  }" +
        "}";

    @Test
    public void yourOfficeDefaultDescriptionIsLoaded() {
        NovelTextEngine e = NovelTextEngine.fromJsonString(OFFICE_HOTEL_JSON);
        String result = e.getDescription("your_office", TimeOfDay.AFTERNOON,
                java.util.Collections.<String, Integer>emptyMap(), null);
        assertEquals("Your office. Small, purposeful, and yours.", result);
    }

    @Test
    public void yourOfficeStateGoodReturnsGoodText() {
        NovelTextEngine e = NovelTextEngine.fromJsonString(OFFICE_HOTEL_JSON);
        assertEquals("The room is clean and well-ordered.",
                e.getStateDescription("your_office", "good"));
    }

    @Test
    public void yourOfficeStateNormalReturnsNormalText() {
        NovelTextEngine e = NovelTextEngine.fromJsonString(OFFICE_HOTEL_JSON);
        assertEquals("A lived-in office.",
                e.getStateDescription("your_office", "normal"));
    }

    @Test
    public void yourOfficeStateBadReturnsBadText() {
        NovelTextEngine e = NovelTextEngine.fromJsonString(OFFICE_HOTEL_JSON);
        assertEquals("The room shows its age.",
                e.getStateDescription("your_office", "bad"));
    }

    @Test
    public void yourOfficeStateNullFallsBackToDefault() {
        NovelTextEngine e = NovelTextEngine.fromJsonString(OFFICE_HOTEL_JSON);
        assertEquals("Your office. Small, purposeful, and yours.",
                e.getStateDescription("your_office", null));
    }

    @Test
    public void hotelBudgetRoomStateDescriptionsLoaded() {
        NovelTextEngine e = NovelTextEngine.fromJsonString(OFFICE_HOTEL_JSON);
        assertEquals("The room is clean and fittings are intact.",
                e.getStateDescription("hotel_budget_room", "good"));
        assertEquals("The room has the worn look of high turnover.",
                e.getStateDescription("hotel_budget_room", "normal"));
        assertEquals("The room presents a catalogue of deferred maintenance.",
                e.getStateDescription("hotel_budget_room", "bad"));
    }

    @Test
    public void hotelBusinessRoomStateDescriptionsLoaded() {
        NovelTextEngine e = NovelTextEngine.fromJsonString(OFFICE_HOTEL_JSON);
        assertEquals("The room presents clean lines and recent refurbishment.",
                e.getStateDescription("hotel_business_room", "good"));
        assertEquals("The room functions well.",
                e.getStateDescription("hotel_business_room", "normal"));
        assertEquals("The room shows the strain.",
                e.getStateDescription("hotel_business_room", "bad"));
    }

    @Test
    public void hotelLuxuryRoomStateDescriptionsLoaded() {
        NovelTextEngine e = NovelTextEngine.fromJsonString(OFFICE_HOTEL_JSON);
        assertEquals("The room is immaculate.",
                e.getStateDescription("hotel_luxury_room", "good"));
        assertEquals("The room retains the quality.",
                e.getStateDescription("hotel_luxury_room", "normal"));
        assertEquals("The room has not been maintained.",
                e.getStateDescription("hotel_luxury_room", "bad"));
    }

    @Test
    public void hotelRoomDefaultFallsBackWhenNoStateMatch() {
        NovelTextEngine e = NovelTextEngine.fromJsonString(OFFICE_HOTEL_JSON);
        assertEquals("A budget hotel room.",
                e.getStateDescription("hotel_budget_room", null));
        assertEquals("A budget hotel room.",
                e.getStateDescription("hotel_budget_room", "unknown"));
    }
}
