package eb.framework1.character;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link PersonDescriptionEngine} and the new appearance
 * attributes on {@link NpcCharacter} / {@link CharacterGenerator}.
 */
public class PersonDescriptionEngineTest {

    // =========================================================================
    // Helpers
    // =========================================================================

    private static NpcCharacter makeNpc(String gender, int age,
                                        String hairType, String hairColor,
                                        int wealthyLevel, String favColor) {
        return new NpcCharacter.Builder()
                .id("test-1")
                .fullName("Test Person")
                .gender(gender)
                .age(age)
                .hairType(hairType)
                .hairColor(hairColor)
                .wealthyLevel(wealthyLevel)
                .favColor(favColor)
                .build();
    }

    private static CharacterGenerator makeGenerator(long seed) {
        List<PersonNameGenerator.NameEntry> firstNames = Arrays.asList(
                new PersonNameGenerator.NameEntry("Alice", "F"),
                new PersonNameGenerator.NameEntry("Bob",   "M")
        );
        List<String> surnames = Arrays.asList("Smith", "Jones");
        PersonNameGenerator nameGen = new PersonNameGenerator(firstNames, surnames, new Random(seed));
        return new CharacterGenerator(nameGen, new Random(seed));
    }

    // =========================================================================
    // PersonDescriptionEngine.describe — null guard
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void describe_null_throws() {
        PersonDescriptionEngine.describe(null);
    }

    // =========================================================================
    // PersonDescriptionEngine.describe — basic content
    // =========================================================================

    @Test
    public void describe_containsGenderWord() {
        NpcCharacter male = makeNpc("M", 30, "straight", "brown", 5, "");
        assertTrue(PersonDescriptionEngine.describe(male).contains("man"));

        NpcCharacter female = makeNpc("F", 30, "wavy", "black", 5, "");
        assertTrue(PersonDescriptionEngine.describe(female).contains("woman"));
    }

    @Test
    public void describe_baldDescription() {
        NpcCharacter npc = makeNpc("M", 50, "bald", "", 5, "");
        String desc = PersonDescriptionEngine.describe(npc);
        assertTrue("Expected bald description", desc.contains("bald"));
        assertFalse("Should not say 'bald hair'", desc.contains("bald hair"));
    }

    @Test
    public void describe_hairColorInDescription() {
        NpcCharacter npc = makeNpc("F", 25, "curly", "blonde", 4, "");
        String desc = PersonDescriptionEngine.describe(npc);
        assertTrue(desc.contains("blonde"));
        assertTrue(desc.contains("curly"));
    }

    @Test
    public void describe_hairTypeOnlyNoTrailingSpace() {
        // When hairType is set but hairColor is empty, no trailing space should appear
        NpcCharacter npc = makeNpc("M", 35, "wavy", "", 5, "");
        String desc = PersonDescriptionEngine.describe(npc);
        assertTrue("Should contain hair type", desc.contains("wavy"));
        assertFalse("Should not have trailing space before period", desc.contains(" ."));
    }

    @Test
    public void describe_favColorIncluded() {
        NpcCharacter npc = makeNpc("M", 40, "wavy", "gray", 6, "blue");
        String desc = PersonDescriptionEngine.describe(npc);
        assertTrue(desc.contains("blue"));
    }

    @Test
    public void describe_noFavColor_omitsSentence() {
        NpcCharacter npc = makeNpc("F", 35, "straight", "brown", 7, "");
        String desc = PersonDescriptionEngine.describe(npc);
        assertFalse("Colour sentence should be absent", desc.contains("favour the colour"));
    }

    @Test
    public void describe_wealthLow_mentionsWornClothing() {
        NpcCharacter npc = makeNpc("M", 28, "buzzed", "black", 1, "");
        String desc = PersonDescriptionEngine.describe(npc);
        assertTrue(desc.toLowerCase().contains("worn"));
    }

    @Test
    public void describe_wealthHigh_mentionsExpensive() {
        NpcCharacter npc = makeNpc("F", 45, "straight", "white", 9, "");
        String desc = PersonDescriptionEngine.describe(npc);
        assertTrue(desc.toLowerCase().contains("wealthy") || desc.toLowerCase().contains("expensive")
                || desc.toLowerCase().contains("ostentatiously"));
    }

    @Test
    public void describe_returnsNonEmpty() {
        NpcCharacter npc = makeNpc("M", 60, "", "", 5, "");
        String desc = PersonDescriptionEngine.describe(npc);
        assertNotNull(desc);
        assertFalse(desc.isEmpty());
    }

    // =========================================================================
    // PersonDescriptionEngine.ageTerm
    // =========================================================================

    @Test
    public void ageTerm_child() {
        assertEquals("young", PersonDescriptionEngine.ageTerm(10));
    }

    @Test
    public void ageTerm_youngAdult() {
        assertEquals("young adult", PersonDescriptionEngine.ageTerm(20));
    }

    @Test
    public void ageTerm_middleAged() {
        assertEquals("middle-aged", PersonDescriptionEngine.ageTerm(40));
    }

    @Test
    public void ageTerm_older() {
        assertEquals("older", PersonDescriptionEngine.ageTerm(55));
    }

    @Test
    public void ageTerm_elderly() {
        assertEquals("elderly", PersonDescriptionEngine.ageTerm(70));
    }

    // =========================================================================
    // NpcCharacter.Builder — appearance fields
    // =========================================================================

    @Test
    public void builder_hairTypeDefaultsEmpty() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("x").fullName("X").gender("M").build();
        assertEquals("", npc.getHairType());
    }

    @Test
    public void builder_hairColorDefaultsEmpty() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("x").fullName("X").gender("M").build();
        assertEquals("", npc.getHairColor());
    }

    @Test
    public void builder_wealthyLevelDefaultFive() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("x").fullName("X").gender("M").build();
        assertEquals(5, npc.getWealthyLevel());
    }

    @Test
    public void builder_favColorDefaultsEmpty() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("x").fullName("X").gender("M").build();
        assertEquals("", npc.getFavColor());
    }

    @Test
    public void builder_setsHairType() {
        NpcCharacter npc = makeNpc("M", 30, "curly", "brown", 5, "");
        assertEquals("curly", npc.getHairType());
    }

    @Test
    public void builder_setsHairColor() {
        NpcCharacter npc = makeNpc("F", 25, "straight", "blonde", 3, "");
        assertEquals("blonde", npc.getHairColor());
    }

    @Test
    public void builder_setsWealthyLevel() {
        NpcCharacter npc = makeNpc("M", 40, "", "", 8, "");
        assertEquals(8, npc.getWealthyLevel());
    }

    @Test
    public void builder_clampWealthyLevelMin() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("x").fullName("X").gender("M").wealthyLevel(0).build();
        assertEquals(1, npc.getWealthyLevel());
    }

    @Test
    public void builder_clampWealthyLevelMax() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("x").fullName("X").gender("M").wealthyLevel(11).build();
        assertEquals(10, npc.getWealthyLevel());
    }

    @Test
    public void builder_setsFavColor() {
        NpcCharacter npc = makeNpc("F", 30, "", "", 5, "green");
        assertEquals("green", npc.getFavColor());
    }

    @Test
    public void builder_nullHairType_stored_asEmpty() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("x").fullName("X").gender("M").hairType(null).build();
        assertEquals("", npc.getHairType());
    }

    // =========================================================================
    // CharacterGenerator — appearance attributes assigned
    // =========================================================================

    @Test
    public void characterGenerator_setsNonEmptyHairType() {
        eb.framework1.investigation.CaseType ct = eb.framework1.investigation.CaseType.FRAUD;
        NpcCharacter npc = makeGenerator(42).generateClient(ct);
        assertNotNull(npc.getHairType());
        assertFalse(npc.getHairType().isEmpty()
                || npc.getHairType().equals("bald")); // "bald" is valid but let's not hardcode
        // Actually just check it is non-null; "bald" is a valid hair type
        assertNotNull(npc.getHairType());
    }

    @Test
    public void characterGenerator_setsNonEmptyHairColor() {
        eb.framework1.investigation.CaseType ct = eb.framework1.investigation.CaseType.THEFT;
        NpcCharacter npc = makeGenerator(99).generateClient(ct);
        assertNotNull(npc.getHairColor());
        assertFalse(npc.getHairColor().isEmpty());
    }

    @Test
    public void characterGenerator_wealthyLevelInRange() {
        eb.framework1.investigation.CaseType ct = eb.framework1.investigation.CaseType.MURDER;
        for (long seed = 1; seed <= 20; seed++) {
            NpcCharacter npc = makeGenerator(seed).generateSuspect(ct);
            int w = npc.getWealthyLevel();
            assertTrue("wealthyLevel out of range: " + w, w >= 1 && w <= 10);
        }
    }

    @Test
    public void characterGenerator_favColorNullSafe() {
        eb.framework1.investigation.CaseType ct = eb.framework1.investigation.CaseType.FRAUD;
        NpcCharacter npc = makeGenerator(7).generateVictim(ct);
        // favColor may be empty string (= none), but must never be null
        assertNotNull(npc.getFavColor());
    }
}
