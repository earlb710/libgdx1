package eb.framework1;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link NpcCharacter}, {@link NpcCharacter.Builder}, and
 * {@link CharacterGenerator}.
 *
 * <p>All tests are pure-Java and require no libGDX runtime.  Data lists are
 * constructed inline and a seeded {@link Random} is injected so results are
 * deterministic.
 */
public class CharacterGeneratorTest {

    // =========================================================================
    // Helpers
    // =========================================================================

    private static CharacterGenerator makeGenerator(long seed) {
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
        return new CharacterGenerator(nameGen, new Random(seed));
    }

    // =========================================================================
    // CharacterGenerator — construction
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void characterGenerator_nullNameGen_throws() {
        new CharacterGenerator(null);
    }

    @Test
    public void characterGenerator_nullRandom_doesNotThrow() {
        // null Random is replaced internally with a new Random()
        PersonNameGenerator nameGen = new PersonNameGenerator(
                Arrays.asList(new PersonNameGenerator.NameEntry("Sam", "M")),
                Arrays.asList("Smith"), null);
        assertNotNull(new CharacterGenerator(nameGen, null));
    }

    // =========================================================================
    // CharacterGenerator — generateClient
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void generateClient_nullCaseType_throws() {
        makeGenerator(1).generateClient(null);
    }

    @Test
    public void generateClient_returnsNonNull() {
        assertNotNull(makeGenerator(2).generateClient(CaseType.FRAUD));
    }

    @Test
    public void generateClient_hasNonBlankId() {
        NpcCharacter npc = makeGenerator(3).generateClient(CaseType.THEFT);
        assertFalse(npc.getId().isEmpty());
        assertTrue(npc.getId().startsWith("char-client-"));
    }

    @Test
    public void generateClient_hasNonBlankName() {
        NpcCharacter npc = makeGenerator(4).generateClient(CaseType.MURDER);
        assertFalse(npc.getFullName().isEmpty());
    }

    @Test
    public void generateClient_genderIsMorF() {
        Set<String> valid = new HashSet<>(Arrays.asList("M", "F"));
        for (int seed = 0; seed < 20; seed++) {
            String g = makeGenerator(seed).generateClient(CaseType.STALKING).getGender();
            assertTrue("Gender must be M or F but was: " + g, valid.contains(g));
        }
    }

    @Test
    public void generateClient_ageIsInRange() {
        for (int seed = 0; seed < 30; seed++) {
            int age = makeGenerator(seed).generateClient(CaseType.FRAUD).getAge();
            assertTrue("Client age must be >= 25 but was " + age, age >= 25);
            assertTrue("Client age must be <= 70 but was " + age, age <= 70);
        }
    }

    @Test
    public void generateClient_spriteKeyMatchesGender() {
        for (int seed = 0; seed < 30; seed++) {
            NpcCharacter npc = makeGenerator(seed).generateClient(CaseType.BLACKMAIL);
            String g = npc.getGender();
            String s = npc.getSpriteKey();
            if ("M".equals(g)) {
                assertTrue("Male sprite key should start with 'man': " + s, s.startsWith("man"));
            } else {
                assertTrue("Female sprite key should start with 'woman': " + s, s.startsWith("woman"));
            }
        }
    }

    // =========================================================================
    // CharacterGenerator — generateVictim
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void generateVictim_nullCaseType_throws() {
        makeGenerator(5).generateVictim(null);
    }

    @Test
    public void generateVictim_returnsNonNull() {
        assertNotNull(makeGenerator(6).generateVictim(CaseType.MISSING_PERSON));
    }

    @Test
    public void generateVictim_hasNonBlankIdWithVictimPrefix() {
        NpcCharacter npc = makeGenerator(7).generateVictim(CaseType.MURDER);
        assertTrue(npc.getId().startsWith("char-victim-"));
    }

    @Test
    public void generateVictim_ageIsInRange() {
        for (int seed = 0; seed < 30; seed++) {
            int age = makeGenerator(seed).generateVictim(CaseType.MURDER).getAge();
            assertTrue("Victim age must be >= 18 but was " + age, age >= 18);
            assertTrue("Victim age must be <= 80 but was " + age, age <= 80);
        }
    }

    // =========================================================================
    // CharacterGenerator — generateSuspect
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void generateSuspect_nullCaseType_throws() {
        makeGenerator(8).generateSuspect(null);
    }

    @Test
    public void generateSuspect_returnsNonNull() {
        assertNotNull(makeGenerator(9).generateSuspect(CaseType.CORPORATE_ESPIONAGE));
    }

    @Test
    public void generateSuspect_hasNonBlankIdWithSuspectPrefix() {
        NpcCharacter npc = makeGenerator(10).generateSuspect(CaseType.THEFT);
        assertTrue(npc.getId().startsWith("char-suspect-"));
    }

    @Test
    public void generateSuspect_ageIsInRange() {
        for (int seed = 0; seed < 30; seed++) {
            int age = makeGenerator(seed).generateSuspect(CaseType.FRAUD).getAge();
            assertTrue("Suspect age must be >= 20 but was " + age, age >= 20);
            assertTrue("Suspect age must be <= 65 but was " + age, age <= 65);
        }
    }

    // =========================================================================
    // CharacterGenerator — IDs are unique per generator instance
    // =========================================================================

    @Test
    public void multipleGenerations_produceUniqueIds() {
        CharacterGenerator gen = makeGenerator(11);
        Set<String> ids = new HashSet<>();
        ids.add(gen.generateClient(CaseType.FRAUD).getId());
        ids.add(gen.generateVictim(CaseType.FRAUD).getId());
        ids.add(gen.generateSuspect(CaseType.FRAUD).getId());
        assertEquals("All three IDs must be unique", 3, ids.size());
    }

    // =========================================================================
    // CharacterGenerator — attributes (core requirement)
    // =========================================================================

    @Test
    public void generateClient_hasAllElevenInvestigativeAttributes() {
        NpcCharacter npc = makeGenerator(12).generateClient(CaseType.BLACKMAIL);
        for (CharacterAttribute attr : CharacterGenerator.INVESTIGATIVE_ATTRIBUTES) {
            int v = npc.getAttribute(attr);
            assertTrue("Attribute " + attr + " must be >= 1 but was " + v, v >= 1);
            assertTrue("Attribute " + attr + " must be <= 10 but was " + v, v <= 10);
        }
    }

    @Test
    public void generateVictim_hasAllElevenInvestigativeAttributes() {
        NpcCharacter npc = makeGenerator(13).generateVictim(CaseType.MURDER);
        for (CharacterAttribute attr : CharacterGenerator.INVESTIGATIVE_ATTRIBUTES) {
            int v = npc.getAttribute(attr);
            assertTrue("Attribute " + attr + " must be >= 1 but was " + v, v >= 1);
            assertTrue("Attribute " + attr + " must be <= 10 but was " + v, v <= 10);
        }
    }

    @Test
    public void generateSuspect_hasAllElevenInvestigativeAttributes() {
        NpcCharacter npc = makeGenerator(14).generateSuspect(CaseType.CORPORATE_ESPIONAGE);
        for (CharacterAttribute attr : CharacterGenerator.INVESTIGATIVE_ATTRIBUTES) {
            int v = npc.getAttribute(attr);
            assertTrue("Attribute " + attr + " must be >= 1 but was " + v, v >= 1);
            assertTrue("Attribute " + attr + " must be <= 10 but was " + v, v <= 10);
        }
    }

    @Test
    public void generateSuspect_attributeMapHasExactlyElevenEntries() {
        NpcCharacter npc = makeGenerator(15).generateSuspect(CaseType.THEFT);
        assertEquals("NPC should have exactly 11 investigative attributes",
                CharacterGenerator.INVESTIGATIVE_ATTRIBUTES.length,
                npc.getAttributes().size());
    }

    @Test
    public void generateClient_attributeMapDoesNotContainBodyMeasurements() {
        NpcCharacter npc = makeGenerator(16).generateClient(CaseType.FRAUD);
        for (CharacterAttribute attr : npc.getAttributes().keySet()) {
            assertFalse("Body measurement should not appear in NPC attributes: " + attr,
                    attr.isBodyMeasurement());
        }
    }

    @Test
    public void generateClient_attributeMapDoesNotContainDerivedAttributes() {
        NpcCharacter npc = makeGenerator(17).generateClient(CaseType.FRAUD);
        for (CharacterAttribute attr : npc.getAttributes().keySet()) {
            assertFalse("Derived attribute should not appear in NPC attributes: " + attr,
                    attr.isDerivedAttribute());
        }
    }

    @Test
    public void generateSuspect_allCaseTypes_haveAttributes() {
        for (CaseType type : CaseType.values()) {
            NpcCharacter suspect = makeGenerator(type.ordinal() + 100L).generateSuspect(type);
            assertEquals("Suspect for case type " + type + " should have 11 attributes",
                    CharacterGenerator.INVESTIGATIVE_ATTRIBUTES.length,
                    suspect.getAttributes().size());
        }
    }

    @Test
    public void investigativeAttributes_coversAllExpectedAttributes() {
        Set<CharacterAttribute> expected = new HashSet<>(Arrays.asList(
                CharacterAttribute.INTELLIGENCE,
                CharacterAttribute.PERCEPTION,
                CharacterAttribute.MEMORY,
                CharacterAttribute.INTUITION,
                CharacterAttribute.AGILITY,
                CharacterAttribute.STAMINA,
                CharacterAttribute.STRENGTH,
                CharacterAttribute.CHARISMA,
                CharacterAttribute.INTIMIDATION,
                CharacterAttribute.EMPATHY,
                CharacterAttribute.STEALTH
        ));
        Set<CharacterAttribute> actual = new HashSet<>(
                Arrays.asList(CharacterGenerator.INVESTIGATIVE_ATTRIBUTES));
        assertEquals("INVESTIGATIVE_ATTRIBUTES must match the expected 11 attributes",
                expected, actual);
    }

    // =========================================================================
    // CharacterGenerator — personality traits
    // =========================================================================

    @Test
    public void generateClient_cooperativenessIsInRange() {
        for (int seed = 0; seed < 30; seed++) {
            int v = makeGenerator(seed).generateClient(CaseType.MISSING_PERSON).getCooperativeness();
            assertTrue("cooperativeness must be 1–10 but was " + v, v >= 1 && v <= 10);
        }
    }

    @Test
    public void generateSuspect_honestyIsInRange() {
        for (int seed = 0; seed < 30; seed++) {
            int v = makeGenerator(seed).generateSuspect(CaseType.FRAUD).getHonesty();
            assertTrue("honesty must be 1–10 but was " + v, v >= 1 && v <= 10);
        }
    }

    @Test
    public void generateVictim_nervousnessIsInRange() {
        for (int seed = 0; seed < 30; seed++) {
            int v = makeGenerator(seed).generateVictim(CaseType.STALKING).getNervousness();
            assertTrue("nervousness must be 1–10 but was " + v, v >= 1 && v <= 10);
        }
    }

    // =========================================================================
    // CharacterGenerator — occupation populated
    // =========================================================================

    @Test
    public void generateClient_occupationNotBlank() {
        for (CaseType type : CaseType.values()) {
            String occ = makeGenerator(type.ordinal()).generateClient(type).getOccupation();
            assertFalse("Client occupation must not be blank for type " + type, occ.isEmpty());
        }
    }

    @Test
    public void generateVictim_occupationNotBlank() {
        for (CaseType type : CaseType.values()) {
            String occ = makeGenerator(type.ordinal()).generateVictim(type).getOccupation();
            assertFalse("Victim occupation must not be blank for type " + type, occ.isEmpty());
        }
    }

    @Test
    public void generateSuspect_occupationNotBlank() {
        for (CaseType type : CaseType.values()) {
            String occ = makeGenerator(type.ordinal()).generateSuspect(type).getOccupation();
            assertFalse("Suspect occupation must not be blank for type " + type, occ.isEmpty());
        }
    }

    // =========================================================================
    // NpcCharacter.Builder — validation
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void builder_nullId_throws() {
        new NpcCharacter.Builder().id(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_blankId_throws() {
        new NpcCharacter.Builder().id("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_nullFullName_throws() {
        new NpcCharacter.Builder().fullName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_nullGender_throws() {
        new NpcCharacter.Builder().gender(null);
    }

    @Test
    public void builder_genderIsUppercased() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("x1").fullName("Sam Smith").gender("m").build();
        assertEquals("M", npc.getGender());
    }

    @Test(expected = IllegalStateException.class)
    public void builder_buildWithoutId_throws() {
        new NpcCharacter.Builder().fullName("Alice Smith").gender("F").build();
    }

    @Test(expected = IllegalStateException.class)
    public void builder_buildWithoutFullName_throws() {
        new NpcCharacter.Builder().id("x2").gender("F").build();
    }

    @Test(expected = IllegalStateException.class)
    public void builder_buildWithoutGender_throws() {
        new NpcCharacter.Builder().id("x3").fullName("Alice Smith").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_attributeValueTooLow_throws() {
        new NpcCharacter.Builder()
                .id("x4").fullName("A").gender("M")
                .attribute(CharacterAttribute.INTELLIGENCE, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_attributeValueTooHigh_throws() {
        new NpcCharacter.Builder()
                .id("x5").fullName("A").gender("M")
                .attribute(CharacterAttribute.CHARISMA, 11);
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_nullAttribute_throws() {
        new NpcCharacter.Builder()
                .id("x6").fullName("A").gender("M")
                .attribute(null, 5);
    }

    @Test
    public void builder_bodyMeasurementAttributeSilentlyIgnored() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("x7").fullName("A").gender("M")
                .attribute(CharacterAttribute.HEIGHT_CM, 5)  // should be ignored
                .attribute(CharacterAttribute.INTELLIGENCE, 7)
                .build();
        assertEquals(0, npc.getAttribute(CharacterAttribute.HEIGHT_CM));
        assertEquals(7, npc.getAttribute(CharacterAttribute.INTELLIGENCE));
    }

    @Test
    public void builder_derivedAttributeSilentlyIgnored() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("x8").fullName("A").gender("M")
                .attribute(CharacterAttribute.DETECTIVE_LEVEL, 8)  // should be ignored
                .build();
        assertEquals(0, npc.getAttribute(CharacterAttribute.DETECTIVE_LEVEL));
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_cooperativenessOutOfRange_throws() {
        new NpcCharacter.Builder()
                .id("x9").fullName("A").gender("M")
                .cooperativeness(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_honestyOutOfRange_throws() {
        new NpcCharacter.Builder()
                .id("x10").fullName("A").gender("M")
                .honesty(11);
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_nervousnessOutOfRange_throws() {
        new NpcCharacter.Builder()
                .id("x11").fullName("A").gender("M")
                .nervousness(0);
    }

    // =========================================================================
    // NpcCharacter — accessor defaults
    // =========================================================================

    @Test
    public void npcCharacter_minimalBuild_defaultsCorrect() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("min-1").fullName("Alice Smith").gender("F").build();

        assertEquals("min-1", npc.getId());
        assertEquals("Alice Smith", npc.getFullName());
        assertEquals("F", npc.getGender());
        assertEquals(0,  npc.getAge());
        assertEquals("", npc.getOccupation());
        assertEquals("", npc.getSpriteKey());
        assertEquals("", npc.getPhysicalDescription());
        assertEquals("", npc.getHomeAddress());
        assertEquals("", npc.getWorkplaceAddress());
        assertTrue(npc.getFrequentLocations().isEmpty());
        assertEquals("", npc.getPhoneNumber());
        assertEquals("", npc.getEmail());
        // personality defaults are 5
        assertEquals(5, npc.getCooperativeness());
        assertEquals(5, npc.getHonesty());
        assertEquals(5, npc.getNervousness());
        // default profile
        assertEquals(PersonalityProfile.DEFAULT, npc.getPersonalityProfile());
        // no attributes set
        assertEquals(0, npc.getAttribute(CharacterAttribute.INTELLIGENCE));
        assertTrue(npc.getAttributes().isEmpty());
    }

    @Test
    public void npcCharacter_frequentLocations_addedAndReadBack() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("loc-1").fullName("Bob Jones").gender("M")
                .addFrequentLocation("The Blue Moon Bar")
                .addFrequentLocation("City Gym")
                .build();
        assertEquals(2, npc.getFrequentLocations().size());
        assertTrue(npc.getFrequentLocations().contains("The Blue Moon Bar"));
        assertTrue(npc.getFrequentLocations().contains("City Gym"));
    }

    @Test
    public void npcCharacter_frequentLocations_isUnmodifiable() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("loc-2").fullName("Carol Brown").gender("F")
                .addFrequentLocation("Café Central")
                .build();
        try {
            npc.getFrequentLocations().clear();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void npcCharacter_attributes_mapIsUnmodifiable() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("attr-1").fullName("Dave Taylor").gender("M")
                .attribute(CharacterAttribute.INTELLIGENCE, 7)
                .build();
        try {
            npc.getAttributes().put(CharacterAttribute.CHARISMA, 5);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void npcCharacter_getAttribute_unknownKeyReturnsZero() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("attr-2").fullName("Eve Williams").gender("F")
                .build();
        assertEquals(0, npc.getAttribute(CharacterAttribute.PERCEPTION));
    }

    @Test
    public void npcCharacter_getAttribute_nullKeyReturnsZero() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("attr-3").fullName("Frank Smith").gender("M")
                .build();
        assertEquals(0, npc.getAttribute(null));
    }

    @Test
    public void npcCharacter_toString_containsIdAndName() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("str-1").fullName("Alice Jones").gender("F").build();
        String s = npc.toString();
        assertTrue(s.contains("str-1"));
        assertTrue(s.contains("Alice Jones"));
    }

    // =========================================================================
    // PersonalityProfile enum
    // =========================================================================

    @Test
    public void personalityProfile_defaultHasFullRange() {
        PersonalityProfile p = PersonalityProfile.DEFAULT;
        assertEquals(1,  p.getMinCooperativeness());
        assertEquals(10, p.getMaxCooperativeness());
        assertEquals(1,  p.getMinHonesty());
        assertEquals(10, p.getMaxHonesty());
        assertEquals(1,  p.getMinNervousness());
        assertEquals(10, p.getMaxNervousness());
    }

    @Test
    public void personalityProfile_psychopathHasCorrectRanges() {
        PersonalityProfile p = PersonalityProfile.PSYCHOPATH;
        // honesty: very low (dishonest)
        assertTrue("PSYCHOPATH maxHonesty must be <= 2", p.getMaxHonesty() <= 2);
        assertEquals(1, p.getMinHonesty());
        // nervousness: very low (confident)
        assertTrue("PSYCHOPATH maxNervousness must be <= 2", p.getMaxNervousness() <= 2);
        assertEquals(1, p.getMinNervousness());
        // cooperativeness: superficially charming (5–9)
        assertTrue("PSYCHOPATH minCooperativeness must be >= 5", p.getMinCooperativeness() >= 5);
        assertTrue("PSYCHOPATH maxCooperativeness must be <= 9", p.getMaxCooperativeness() <= 9);
    }

    @Test
    public void personalityProfile_allValuesHaveDisplayName() {
        for (PersonalityProfile p : PersonalityProfile.values()) {
            assertNotNull(p.getDisplayName());
            assertFalse(p.getDisplayName().isEmpty());
        }
    }

    @Test
    public void personalityProfile_allValuesHaveDescription() {
        for (PersonalityProfile p : PersonalityProfile.values()) {
            assertNotNull(p.getDescription());
            assertFalse(p.getDescription().isEmpty());
        }
    }

    // =========================================================================
    // CharacterGenerator — PSYCHOPATH profile
    // =========================================================================

    @Test
    public void generateSuspect_psychopath_honestyIsVeryLow() {
        for (int seed = 0; seed < 50; seed++) {
            NpcCharacter suspect = makeGenerator(seed)
                    .generateSuspect(CaseType.MURDER, PersonalityProfile.PSYCHOPATH);
            int h = suspect.getHonesty();
            assertTrue("PSYCHOPATH honesty must be 1–2 but was " + h, h >= 1 && h <= 2);
        }
    }

    @Test
    public void generateSuspect_psychopath_nervousnessIsVeryLow() {
        for (int seed = 0; seed < 50; seed++) {
            NpcCharacter suspect = makeGenerator(seed)
                    .generateSuspect(CaseType.FRAUD, PersonalityProfile.PSYCHOPATH);
            int n = suspect.getNervousness();
            assertTrue("PSYCHOPATH nervousness must be 1–2 but was " + n, n >= 1 && n <= 2);
        }
    }

    @Test
    public void generateSuspect_psychopath_cooperativenessIsHigherRange() {
        for (int seed = 0; seed < 50; seed++) {
            NpcCharacter suspect = makeGenerator(seed)
                    .generateSuspect(CaseType.BLACKMAIL, PersonalityProfile.PSYCHOPATH);
            int c = suspect.getCooperativeness();
            assertTrue("PSYCHOPATH cooperativeness must be 5–9 but was " + c,
                    c >= 5 && c <= 9);
        }
    }

    @Test
    public void generateSuspect_psychopath_profileStoredOnNpc() {
        NpcCharacter suspect = makeGenerator(42)
                .generateSuspect(CaseType.MURDER, PersonalityProfile.PSYCHOPATH);
        assertEquals(PersonalityProfile.PSYCHOPATH, suspect.getPersonalityProfile());
    }

    @Test
    public void generateSuspect_defaultProfile_profileStoredOnNpc() {
        NpcCharacter suspect = makeGenerator(42)
                .generateSuspect(CaseType.THEFT);
        assertEquals(PersonalityProfile.DEFAULT, suspect.getPersonalityProfile());
    }

    @Test
    public void generateSuspect_nullProfile_treatedAsDefault() {
        NpcCharacter suspect = makeGenerator(42)
                .generateSuspect(CaseType.THEFT, null);
        assertEquals(PersonalityProfile.DEFAULT, suspect.getPersonalityProfile());
    }

    @Test
    public void generateSuspect_psychopath_hasAllRequiredAttributes() {
        NpcCharacter suspect = makeGenerator(99)
                .generateSuspect(CaseType.STALKING, PersonalityProfile.PSYCHOPATH);
        assertEquals(CharacterGenerator.INVESTIGATIVE_ATTRIBUTES.length,
                suspect.getAttributes().size());
        for (CharacterAttribute attr : CharacterGenerator.INVESTIGATIVE_ATTRIBUTES) {
            int v = suspect.getAttribute(attr);
            assertTrue("Attribute " + attr + " must be 1–10 but was " + v, v >= 1 && v <= 10);
        }
    }

    @Test
    public void generateSuspect_psychopath_allCaseTypes() {
        for (CaseType type : CaseType.values()) {
            NpcCharacter suspect = makeGenerator(type.ordinal() + 1000L)
                    .generateSuspect(type, PersonalityProfile.PSYCHOPATH);
            assertEquals("Profile must be PSYCHOPATH for type " + type,
                    PersonalityProfile.PSYCHOPATH, suspect.getPersonalityProfile());
            assertTrue("honesty out of range for type " + type,
                    suspect.getHonesty() >= 1 && suspect.getHonesty() <= 2);
            assertTrue("nervousness out of range for type " + type,
                    suspect.getNervousness() >= 1 && suspect.getNervousness() <= 2);
        }
    }

    @Test
    public void builder_personalityProfile_nullTreatedAsDefault() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("pp-1").fullName("Test User").gender("M")
                .personalityProfile(null)
                .build();
        assertEquals(PersonalityProfile.DEFAULT, npc.getPersonalityProfile());
    }

    @Test
    public void builder_personalityProfile_psychopathSetAndReadBack() {
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("pp-2").fullName("Test User").gender("M")
                .personalityProfile(PersonalityProfile.PSYCHOPATH)
                .honesty(1)
                .nervousness(2)
                .cooperativeness(7)
                .build();
        assertEquals(PersonalityProfile.PSYCHOPATH, npc.getPersonalityProfile());
        assertEquals(1, npc.getHonesty());
        assertEquals(2, npc.getNervousness());
        assertEquals(7, npc.getCooperativeness());
    }
}
