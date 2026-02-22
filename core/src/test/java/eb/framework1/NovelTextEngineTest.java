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
}
