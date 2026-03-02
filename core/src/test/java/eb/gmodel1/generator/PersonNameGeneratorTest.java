package eb.gmodel1.generator;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link PersonNameGenerator}.
 *
 * All tests are pure-Java and require no libGDX runtime: data lists are
 * constructed inline and a seeded {@link Random} is injected so results
 * are deterministic.
 */
public class PersonNameGeneratorTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static PersonNameGenerator.NameEntry entry(String name, String gender) {
        return new PersonNameGenerator.NameEntry(name, gender);
    }

    /** Small mixed-gender list covering M, F, and B entries. */
    private static List<PersonNameGenerator.NameEntry> sampleFirstNames() {
        return Arrays.asList(
            entry("Alice",   "F"),
            entry("Bob",     "M"),
            entry("Charlie", "M"),
            entry("Diana",   "F"),
            entry("Alex",    "B"),
            entry("Jordan",  "B")
        );
    }

    private static List<String> sampleSurnames() {
        return Arrays.asList("Smith", "Jones", "Brown", "Taylor");
    }

    /** Generator with a fixed seed for reproducible picks. */
    private PersonNameGenerator gen(long seed) {
        return new PersonNameGenerator(sampleFirstNames(), sampleSurnames(), new Random(seed));
    }

    // -------------------------------------------------------------------------
    // Construction / counts
    // -------------------------------------------------------------------------

    @Test
    public void testSurnameCount() {
        PersonNameGenerator g = new PersonNameGenerator(sampleFirstNames(), sampleSurnames());
        assertEquals(4, g.surnameCount());
    }

    @Test
    public void testFirstNameCountAll() {
        PersonNameGenerator g = new PersonNameGenerator(sampleFirstNames(), sampleSurnames());
        assertEquals(6, g.firstNameCount(null));
    }

    @Test
    public void testFirstNameCountMale() {
        // M names: Bob, Charlie + B names: Alex, Jordan = 4
        assertEquals(4, new PersonNameGenerator(sampleFirstNames(), sampleSurnames())
                .firstNameCount("M"));
    }

    @Test
    public void testFirstNameCountFemale() {
        // F names: Alice, Diana + B names: Alex, Jordan = 4
        assertEquals(4, new PersonNameGenerator(sampleFirstNames(), sampleSurnames())
                .firstNameCount("F"));
    }

    @Test
    public void testFirstNameCountBoth() {
        // "B" gender arg → all names (no filter)
        assertEquals(6, new PersonNameGenerator(sampleFirstNames(), sampleSurnames())
                .firstNameCount("B"));
    }

    // -------------------------------------------------------------------------
    // generate(gender) – first name only
    // -------------------------------------------------------------------------

    @Test
    public void testGenerateReturnsNonNull() {
        assertNotNull(gen(1).generate("M"));
    }

    @Test
    public void testGenerateMaleOnlyReturnsEligibleName() {
        Set<String> malePlusBoth = new HashSet<>(Arrays.asList("Bob", "Charlie", "Alex", "Jordan"));
        for (int i = 0; i < 50; i++) {
            String name = new PersonNameGenerator(sampleFirstNames(), sampleSurnames(),
                    new Random(i)).generate("M");
            assertTrue("Expected male-eligible name, got: " + name, malePlusBoth.contains(name));
        }
    }

    @Test
    public void testGenerateFemaleOnlyReturnsEligibleName() {
        Set<String> femalePlusBoth = new HashSet<>(Arrays.asList("Alice", "Diana", "Alex", "Jordan"));
        for (int i = 0; i < 50; i++) {
            String name = new PersonNameGenerator(sampleFirstNames(), sampleSurnames(),
                    new Random(i)).generate("F");
            assertTrue("Expected female-eligible name, got: " + name, femalePlusBoth.contains(name));
        }
    }

    @Test
    public void testGenerateNullGenderReturnsAnyName() {
        Set<String> all = new HashSet<>(Arrays.asList("Alice","Bob","Charlie","Diana","Alex","Jordan"));
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            seen.add(new PersonNameGenerator(sampleFirstNames(), sampleSurnames(),
                    new Random(i)).generate(null));
        }
        // With 200 seeds we should see at least 4 distinct names (statistical sanity)
        assertTrue("Should see multiple names across seeds", seen.size() >= 4);
        for (String n : seen) assertTrue("Name not in list: " + n, all.contains(n));
    }

    @Test
    public void testGenerateUnknownGenderTreatedAsAll() {
        // "X" is not M or F → all names eligible
        assertEquals(6, new PersonNameGenerator(sampleFirstNames(), sampleSurnames())
                .firstNameCount("X"));
    }

    @Test
    public void testGenerateEmptyListReturnsUnknown() {
        PersonNameGenerator g = new PersonNameGenerator(
                new ArrayList<PersonNameGenerator.NameEntry>(), sampleSurnames());
        assertEquals("Unknown", g.generate("M"));
    }

    // -------------------------------------------------------------------------
    // generate(gender, surname) – first name + explicit surname
    // -------------------------------------------------------------------------

    @Test
    public void testGenerateWithSurnameReturnsCombined() {
        String result = gen(1).generate("M", "Williams");
        assertTrue("Should end with Williams", result.endsWith("Williams"));
        assertTrue("Should have a space", result.contains(" "));
    }

    @Test
    public void testGenerateWithNullSurnameReturnsFirstNameOnly() {
        String result = gen(1).generate("F", null);
        assertFalse("Should not contain space", result.contains(" "));
        assertFalse("Should not be empty", result.isEmpty());
    }

    @Test
    public void testGenerateWithBlankSurnameReturnsFirstNameOnly() {
        String result = gen(1).generate("M", "   ");
        assertFalse("Should not contain space", result.contains(" "));
    }

    @Test
    public void testGenerateWithSurnameTrimsWhitespace() {
        String result = gen(1).generate("M", "  Clark  ");
        assertTrue("Should end with 'Clark'", result.endsWith("Clark"));
        assertFalse("Should not have leading/trailing spaces around surname",
                result.contains("  "));
    }

    // -------------------------------------------------------------------------
    // generateFull(gender) – first name + random surname
    // -------------------------------------------------------------------------

    @Test
    public void testGenerateFullContainsSurname() {
        Set<String> validSurnames = new HashSet<>(Arrays.asList("Smith","Jones","Brown","Taylor"));
        for (int i = 0; i < 50; i++) {
            String full = new PersonNameGenerator(sampleFirstNames(), sampleSurnames(),
                    new Random(i)).generateFull("M");
            String[] parts = full.split(" ");
            assertEquals("Should have exactly two words", 2, parts.length);
            assertTrue("Surname part should be from list: " + parts[1],
                    validSurnames.contains(parts[1]));
        }
    }

    @Test
    public void testGenerateFullEmptySurnamesReturnsFirstNameOnly() {
        PersonNameGenerator g = new PersonNameGenerator(
                sampleFirstNames(), new ArrayList<String>());
        String result = g.generateFull("F");
        assertFalse("Should not contain space when no surnames", result.contains(" "));
    }

    // -------------------------------------------------------------------------
    // randomSurname
    // -------------------------------------------------------------------------

    @Test
    public void testRandomSurnameFromList() {
        Set<String> valid = new HashSet<>(Arrays.asList("Smith","Jones","Brown","Taylor"));
        for (int i = 0; i < 20; i++) {
            String s = new PersonNameGenerator(sampleFirstNames(), sampleSurnames(),
                    new Random(i)).randomSurname();
            assertTrue("Surname not in list: " + s, valid.contains(s));
        }
    }

    @Test
    public void testRandomSurnameEmptyListReturnsEmpty() {
        PersonNameGenerator g = new PersonNameGenerator(
                sampleFirstNames(), new ArrayList<String>());
        assertEquals("", g.randomSurname());
    }

    // -------------------------------------------------------------------------
    // Null-safety
    // -------------------------------------------------------------------------

    @Test
    public void testNullListsDoNotThrow() {
        PersonNameGenerator g = new PersonNameGenerator(null, null);
        assertEquals("Unknown", g.generate("M"));
        assertEquals("Unknown", g.generateFull("F"));
        assertEquals("", g.randomSurname());
    }

    @Test
    public void testNameEntryNullsNormalised() {
        PersonNameGenerator.NameEntry e = new PersonNameGenerator.NameEntry(null, null);
        assertEquals("", e.name);
        assertEquals("B", e.gender);
    }

    @Test
    public void testNameEntryGenderUppercased() {
        PersonNameGenerator.NameEntry e = new PersonNameGenerator.NameEntry("Sam", "m");
        assertEquals("M", e.gender);
    }
}
