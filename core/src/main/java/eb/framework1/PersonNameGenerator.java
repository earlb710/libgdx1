package eb.framework1;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates random person names from the {@code person_names.json} and
 * {@code person_surnames.json} data files.
 *
 * <p>This class is intentionally free of libGDX dependencies so that it can be
 * constructed and tested with plain JUnit without a running libGDX application.
 * Data is injected via the constructor; loading from JSON is the responsibility
 * of {@link GameDataManager}.
 *
 * <h3>Gender filter</h3>
 * <ul>
 *   <li>{@code "M"} – male names and gender-neutral names</li>
 *   <li>{@code "F"} – female names and gender-neutral names</li>
 *   <li>{@code "B"} or any other value – all names (no filter)</li>
 *   <li>{@code null} – all names (no filter)</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>
 *   PersonNameGenerator gen = gameDataManager.getPersonNameGenerator();
 *
 *   // First name only (male)
 *   String firstName = gen.generate("M");
 *
 *   // First name + explicit surname
 *   String full = gen.generate("F", "Smith");
 *
 *   // First name + random surname (any gender)
 *   String randomFull = gen.generateFull(null);
 * </pre>
 */
public class PersonNameGenerator {

    /** Immutable data record for a single first-name entry. */
    public static final class NameEntry {
        /** The first name, e.g. {@code "Alice"}. */
        public final String name;
        /**
         * Gender tag: {@code "M"} = male, {@code "F"} = female,
         * {@code "B"} = both / gender-neutral.
         */
        public final String gender;

        public NameEntry(String name, String gender) {
            this.name   = name   != null ? name   : "";
            this.gender = gender != null ? gender.toUpperCase() : "B";
        }
    }

    private final List<NameEntry> allFirstNames;
    private final List<String>    allSurnames;
    private final Random          random;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a generator with the supplied name data and a default {@link Random}.
     *
     * @param firstNames list of {@link NameEntry} objects loaded from
     *                   {@code person_names.json}; {@code null} is treated as an
     *                   empty list
     * @param surnames   flat list of surname strings loaded from
     *                   {@code person_surnames.json}; {@code null} is treated as
     *                   an empty list
     */
    public PersonNameGenerator(List<NameEntry> firstNames, List<String> surnames) {
        this(firstNames, surnames, new Random());
    }

    /**
     * Creates a generator with an explicit {@link Random} instance.
     * Useful for seeded/reproducible tests.
     *
     * @param firstNames list of {@link NameEntry} objects
     * @param surnames   flat list of surname strings
     * @param random     random-number source to use
     */
    public PersonNameGenerator(List<NameEntry> firstNames, List<String> surnames,
                                Random random) {
        this.allFirstNames = firstNames != null ? firstNames : new ArrayList<NameEntry>();
        this.allSurnames   = surnames   != null ? surnames   : new ArrayList<String>();
        this.random        = random     != null ? random     : new Random();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns a random first name matching the given gender.
     *
     * <p>If no names match the gender filter (or the list is empty), returns
     * {@code "Unknown"}.
     *
     * @param gender {@code "M"}, {@code "F"}, {@code "B"}, or {@code null}
     *               (null / unknown → all names eligible)
     * @return a first name string, never {@code null}
     */
    public String generate(String gender) {
        List<NameEntry> pool = filterByGender(gender);
        if (pool.isEmpty()) return "Unknown";
        return pool.get(random.nextInt(pool.size())).name;
    }

    /**
     * Returns a full name composed of a random first name plus the given surname.
     *
     * <p>If {@code surname} is {@code null} or blank the result is the first name
     * only (no trailing space).
     *
     * @param gender  gender filter for the first name (see class javadoc)
     * @param surname optional surname; pass {@code null} to omit it
     * @return full name string, e.g. {@code "Alice Smith"}
     */
    public String generate(String gender, String surname) {
        String firstName = generate(gender);
        if (surname == null || surname.trim().isEmpty()) {
            return firstName;
        }
        return firstName + " " + surname.trim();
    }

    /**
     * Returns a full name composed of a random first name plus a random surname
     * chosen from the loaded surname list.
     *
     * <p>If the surname list is empty the result contains the first name only.
     *
     * @param gender gender filter for the first name (see class javadoc)
     * @return full name string, e.g. {@code "James Foster"}
     */
    public String generateFull(String gender) {
        String firstName = generate(gender);
        if (allSurnames.isEmpty()) return firstName;
        String surname = allSurnames.get(random.nextInt(allSurnames.size()));
        return firstName + " " + surname;
    }

    /**
     * Returns a random surname from the loaded surname list, or {@code ""}
     * if the list is empty.
     */
    public String randomSurname() {
        if (allSurnames.isEmpty()) return "";
        return allSurnames.get(random.nextInt(allSurnames.size()));
    }

    /**
     * Returns the number of first names loaded for the given gender filter
     * (same filter semantics as {@link #generate(String)}).
     */
    public int firstNameCount(String gender) {
        return filterByGender(gender).size();
    }

    /** Returns the total number of surnames loaded. */
    public int surnameCount() {
        return allSurnames.size();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the subset of {@link #allFirstNames} that matches the gender filter.
     *
     * <ul>
     *   <li>"M" → names tagged "M" or "B"</li>
     *   <li>"F" → names tagged "F" or "B"</li>
     *   <li>anything else (including null) → all names</li>
     * </ul>
     */
    private List<NameEntry> filterByGender(String gender) {
        if (gender == null) return allFirstNames;
        String g = gender.toUpperCase();
        if (!g.equals("M") && !g.equals("F")) return allFirstNames;

        List<NameEntry> pool = new ArrayList<NameEntry>();
        for (NameEntry e : allFirstNames) {
            if (e.gender.equals(g) || e.gender.equals("B")) {
                pool.add(e);
            }
        }
        return pool;
    }
}
