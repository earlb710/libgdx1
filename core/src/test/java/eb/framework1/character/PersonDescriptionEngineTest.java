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

    // =========================================================================
    // NpcCharacter.Builder — height and weight fields
    // =========================================================================

    @Test
    public void builder_heightCmDefaultsZero() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("x").fullName("X").gender("M").build();
        assertEquals(0, npc.getHeightCm());
    }

    @Test
    public void builder_weightKgDefaultsZero() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("x").fullName("X").gender("M").build();
        assertEquals(0, npc.getWeightKg());
    }

    @Test
    public void builder_setsHeightCm() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("x").fullName("X").gender("M").heightCm(182).build();
        assertEquals(182, npc.getHeightCm());
    }

    @Test
    public void builder_setsWeightKg() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("x").fullName("X").gender("F").weightKg(65).build();
        assertEquals(65, npc.getWeightKg());
    }

    // =========================================================================
    // PersonDescriptionEngine.heightDesc
    // =========================================================================

    @Test
    public void heightDesc_short() {
        assertEquals("short", PersonDescriptionEngine.heightDesc(155));
    }

    @Test
    public void heightDesc_belowAverage() {
        assertEquals("below average height", PersonDescriptionEngine.heightDesc(165));
    }

    @Test
    public void heightDesc_averageHeight() {
        assertEquals("average height", PersonDescriptionEngine.heightDesc(175));
    }

    @Test
    public void heightDesc_tall() {
        assertEquals("tall", PersonDescriptionEngine.heightDesc(185));
    }

    @Test
    public void heightDesc_veryTall() {
        assertEquals("very tall", PersonDescriptionEngine.heightDesc(195));
    }

    // =========================================================================
    // PersonDescriptionEngine.buildDesc
    // =========================================================================

    @Test
    public void buildDesc_slim() {
        // BMI ≈ 17.5 → slim
        assertEquals("slim", PersonDescriptionEngine.buildDesc(180, 57));
    }

    @Test
    public void buildDesc_average() {
        // BMI ≈ 22 → average
        assertEquals("average", PersonDescriptionEngine.buildDesc(175, 67));
    }

    @Test
    public void buildDesc_stocky() {
        // BMI ≈ 27 → stocky
        assertEquals("stocky", PersonDescriptionEngine.buildDesc(175, 83));
    }

    @Test
    public void buildDesc_heavy() {
        // BMI ≈ 32 → heavy
        assertEquals("heavy", PersonDescriptionEngine.buildDesc(175, 98));
    }

    @Test
    public void buildDesc_zeroHeight_returnsAverage() {
        // Guard: height == 0 should not divide by zero
        assertEquals("average", PersonDescriptionEngine.buildDesc(0, 70));
    }

    // =========================================================================
    // PersonDescriptionEngine.describe — height/weight sentences
    // =========================================================================

    @Test
    public void describe_withHeightAndWeight_containsHeightWord() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("h1").fullName("Tall Person").gender("M")
                .age(35).wealthyLevel(5).heightCm(190).weightKg(85).build();
        String desc = PersonDescriptionEngine.describe(npc);
        assertTrue("Description should mention 'tall'", desc.contains("tall"));
    }

    @Test
    public void describe_withHeightAndWeight_containsBuildWord() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("h2").fullName("Average Build").gender("F")
                .age(28).wealthyLevel(5).heightCm(165).weightKg(60).build();
        String desc = PersonDescriptionEngine.describe(npc);
        // height 165, weight 60 → BMI ≈ 22 = average build
        assertTrue("Description should mention 'build'", desc.contains("build"));
    }

    @Test
    public void describe_heightOnlyNoWeight_stillIncludesHeight() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("h3").fullName("Height Only").gender("M")
                .age(40).wealthyLevel(5).heightCm(175).build();
        String desc = PersonDescriptionEngine.describe(npc);
        assertTrue("Description should mention height", desc.contains("average height"));
    }

    @Test
    public void describe_noHeightNoWeight_omitsHeightSentence() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("h4").fullName("No Body").gender("F")
                .age(50).wealthyLevel(5).build();
        String desc = PersonDescriptionEngine.describe(npc);
        assertFalse("Height sentence should be absent", desc.contains(" They are "));
    }

    // =========================================================================
    // CharacterGenerator — height and weight assigned
    // =========================================================================

    @Test
    public void characterGenerator_setsPositiveHeight() {
        eb.framework1.investigation.CaseType ct = eb.framework1.investigation.CaseType.FRAUD;
        NpcCharacter npc = makeGenerator(10).generateClient(ct);
        assertTrue("heightCm should be > 0, was " + npc.getHeightCm(),
                   npc.getHeightCm() > 0);
    }

    @Test
    public void characterGenerator_setsPositiveWeight() {
        eb.framework1.investigation.CaseType ct = eb.framework1.investigation.CaseType.THEFT;
        NpcCharacter npc = makeGenerator(20).generateSuspect(ct);
        assertTrue("weightKg should be > 0, was " + npc.getWeightKg(),
                   npc.getWeightKg() > 0);
    }

    @Test
    public void characterGenerator_heightInMaleRange() {
        eb.framework1.investigation.CaseType ct = eb.framework1.investigation.CaseType.MURDER;
        // Run several seeds to cover male generation
        for (long seed = 100; seed <= 130; seed++) {
            NpcCharacter npc = makeGenerator(seed).generateSuspect(ct);
            int h = npc.getHeightCm();
            assertTrue("height out of expected range: " + h,
                       h >= 150 && h <= 200); // generous range covering both genders
        }
    }

    @Test
    public void characterGenerator_weightReasonableForHeight() {
        eb.framework1.investigation.CaseType ct = eb.framework1.investigation.CaseType.FRAUD;
        for (long seed = 1; seed <= 25; seed++) {
            NpcCharacter npc = makeGenerator(seed).generateVictim(ct);
            int h = npc.getHeightCm();
            int w = npc.getWeightKg();
            if (h > 0 && w > 0) {
                float hm  = h / 100f;
                float bmi = w / (hm * hm);
                assertTrue("BMI out of plausible range [14,40]: " + bmi + " (h=" + h + ",w=" + w + ")",
                           bmi >= 14f && bmi <= 40f);
            }
        }
    }

    // =========================================================================
    // PersonDescriptionEngine.describe — carried items sentence
    // =========================================================================

    @Test
    public void describe_noCarriedItems_omitsItemsSentence() {
        NpcCharacter npc = makeNpc("M", 30, "straight", "brown", 5, "");
        String desc = PersonDescriptionEngine.describe(npc);
        assertFalse("No items → no carrying sentence",
                desc.contains("carrying"));
    }

    @Test
    public void describe_oneCarriedItem_mentionsItem() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("cop1").fullName("Officer Jones").gender("M")
                .age(35).wealthyLevel(5)
                .addCarriedItem(EquipItem.PISTOL)
                .build();
        String desc = PersonDescriptionEngine.describe(npc);
        assertTrue("Should mention the item", desc.contains("pistol"));
        assertTrue("Should say 'carrying'", desc.contains("carrying"));
    }

    @Test
    public void describe_twoCarriedItems_mentionsBoth() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("x1").fullName("Guard One").gender("F")
                .age(28).wealthyLevel(5)
                .addCarriedItem(EquipItem.PISTOL)
                .addCarriedItem(EquipItem.BINOCULARS)
                .build();
        String desc = PersonDescriptionEngine.describe(npc);
        assertTrue("Should mention pistol", desc.contains("pistol"));
        assertTrue("Should mention binoculars", desc.contains("binoculars"));
        assertTrue("Should use 'and' connector", desc.contains(" and "));
    }

    @Test
    public void builder_carriedItemsDefaultEmpty() {
        NpcCharacter npc = makeNpc("M", 40, "", "", 5, "");
        assertNotNull(npc.getCarriedItems());
        assertTrue(npc.getCarriedItems().isEmpty());
    }

    // =========================================================================
    // PersonDescriptionEngine — glasses / vision trait
    // =========================================================================

    @Test
    public void describe_npcWithGlasses_saysWearGlasses() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("g1").fullName("Four Eyes").gender("F")
                .age(30).wealthyLevel(5)
                .addCarriedItem(EquipItem.GLASSES)
                .build();
        String desc = PersonDescriptionEngine.describe(npc);
        assertTrue("Should say 'wear glasses'", desc.contains("wear glasses"));
        assertFalse("Should not say 'carrying a glasses'", desc.contains("carrying a glasses"));
    }

    @Test
    public void describe_npcWithGlassesAndPistol_bothMentioned() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("g2").fullName("Armed Nerd").gender("M")
                .age(40).wealthyLevel(5)
                .addCarriedItem(EquipItem.GLASSES)
                .addCarriedItem(EquipItem.PISTOL)
                .build();
        String desc = PersonDescriptionEngine.describe(npc);
        assertTrue("Should mention glasses as 'wear glasses'", desc.contains("wear glasses"));
        assertTrue("Should mention pistol as carrying", desc.contains("carrying"));
        assertTrue("Should mention pistol", desc.contains("pistol"));
    }

    @Test
    public void describe_npcWithNoGlasses_doesNotSayWearGlasses() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("g3").fullName("Normal Eyes").gender("F")
                .age(25).wealthyLevel(5)
                .build();
        String desc = PersonDescriptionEngine.describe(npc);
        assertFalse("No glasses → should not say wear glasses", desc.contains("wear glasses"));
    }
}
