package eb.gmodel1.character;

import eb.gmodel1.generator.*;
import eb.gmodel1.investigation.*;


import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Procedurally generates {@link NpcCharacter} objects for use as clients,
 * victims, and suspects in detective cases.
 *
 * <p>Each generated NPC receives:
 * <ul>
 *   <li>A randomly chosen name from the supplied {@link PersonNameGenerator}.</li>
 *   <li>A randomly chosen gender ({@code "M"} or {@code "F"}).</li>
 *   <li>An age drawn from a range appropriate to the role and case type.</li>
 *   <li>A sprite key consistent with the chosen gender.</li>
 *   <li>All eleven investigative {@link CharacterAttribute}s randomly assigned
 *       values in the range 1–10 (the same attributes used by the player
 *       character, but generated automatically rather than via the point-buy
 *       screen).</li>
 *   <li>Three personality traits — cooperativeness, honesty, and nervousness —
 *       also randomly assigned values in the range 1–10.</li>
 * </ul>
 *
 * <p>The eleven investigative attributes are:
 * {@link CharacterAttribute#INTELLIGENCE}, {@link CharacterAttribute#PERCEPTION},
 * {@link CharacterAttribute#MEMORY}, {@link CharacterAttribute#INTUITION},
 * {@link CharacterAttribute#AGILITY}, {@link CharacterAttribute#STAMINA},
 * {@link CharacterAttribute#STRENGTH}, {@link CharacterAttribute#CHARISMA},
 * {@link CharacterAttribute#INTIMIDATION}, {@link CharacterAttribute#EMPATHY},
 * {@link CharacterAttribute#STEALTH}.
 *
 * <p>This class has <strong>no libGDX dependency</strong> and can be constructed
 * and tested with plain JUnit.  A {@link Random} instance is accepted so that
 * tests can produce deterministic output.
 *
 * <h3>Example</h3>
 * <pre>
 *   CharacterGenerator gen = new CharacterGenerator(nameGenerator, new Random(42));
 *
 *   NpcCharacter client  = gen.generateClient(CaseType.FRAUD);
 *   NpcCharacter victim  = gen.generateVictim(CaseType.MURDER);
 *   NpcCharacter suspect = gen.generateSuspect(CaseType.MURDER);
 *
 *   int intel = client.getAttribute(CharacterAttribute.INTELLIGENCE);  // 1–10
 * </pre>
 */
public class CharacterGenerator {

    /**
     * The eleven investigative attributes that every NPC shares with the player
     * character.  Body-measurement attributes and the derived
     * {@link CharacterAttribute#DETECTIVE_LEVEL} are excluded.
     */
    static final CharacterAttribute[] INVESTIGATIVE_ATTRIBUTES = {
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
    };

    private static final int ATTR_MIN = 1;
    private static final int ATTR_MAX = 10;

    // Sprite key pools indexed by gender
    private static final String[] MALE_SPRITES   = { "man1", "man2" };
    private static final String[] FEMALE_SPRITES = { "woman1", "woman2" };

    private final PersonNameGenerator nameGen;
    private final Random              random;

    // Counter used to produce unique NPC ids within a single generator instance.
    private int npcCounter = 0;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a generator with the given name source and a default {@link Random}.
     *
     * @param nameGen name generator used to produce NPC names; must not be
     *                {@code null}
     */
    public CharacterGenerator(PersonNameGenerator nameGen) {
        this(nameGen, new Random());
    }

    /**
     * Creates a generator with an explicit {@link Random} instance.
     * Pass a seeded {@code Random} to obtain reproducible output in tests.
     *
     * @param nameGen name generator; must not be {@code null}
     * @param random  random-number source; {@code null} is replaced by a
     *                default {@code new Random()}
     */
    public CharacterGenerator(PersonNameGenerator nameGen, Random random) {
        if (nameGen == null) {
            throw new IllegalArgumentException("nameGen must not be null");
        }
        this.nameGen = nameGen;
        this.random  = random != null ? random : new Random();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Generates an NPC to act as the <em>client</em> who hired the detective.
     *
     * <p>Age range: 25–70.  All eleven investigative attributes and all three
     * personality traits are randomly assigned values in the range 1–10.
     *
     * @param caseType the type of case being investigated; must not be
     *                 {@code null}
     * @return a fully populated {@link NpcCharacter} with the CLIENT role
     * @throws IllegalArgumentException if {@code caseType} is {@code null}
     */
    public NpcCharacter generateClient(CaseType caseType) {
        if (caseType == null) throw new IllegalArgumentException("caseType must not be null");
        String gender = randomGender();
        String id     = "char-client-" + (++npcCounter);
        return buildBase(id, gender, 25, 70)
                .occupation(clientOccupation(caseType))
                .build();
    }

    /**
     * Generates an NPC to act as the <em>victim</em> in the case.
     *
     * <p>Age range: 18–80.  All eleven investigative attributes and all three
     * personality traits are randomly assigned values in the range 1–10.
     *
     * @param caseType the type of case being investigated; must not be
     *                 {@code null}
     * @return a fully populated {@link NpcCharacter} with the VICTIM role
     * @throws IllegalArgumentException if {@code caseType} is {@code null}
     */
    public NpcCharacter generateVictim(CaseType caseType) {
        if (caseType == null) throw new IllegalArgumentException("caseType must not be null");
        String gender = randomGender();
        String id     = "char-victim-" + (++npcCounter);
        return buildBase(id, gender, 18, 80)
                .occupation(victimOccupation(caseType))
                .build();
    }

    /**
     * Generates an NPC to act as the <em>suspect</em> in the case.
     *
     * <p>Age range: 20–65.  All eleven investigative attributes and all three
     * personality traits are randomly assigned values in the range 1–10.
     *
     * @param caseType the type of case being investigated; must not be
     *                 {@code null}
     * @return a fully populated {@link NpcCharacter} with the SUSPECT role
     * @throws IllegalArgumentException if {@code caseType} is {@code null}
     */
    public NpcCharacter generateSuspect(CaseType caseType) {
        return generateSuspect(caseType, PersonalityProfile.DEFAULT);
    }

    /**
     * Generates an NPC to act as the <em>suspect</em> in the case with an
     * explicit {@link PersonalityProfile}.
     *
     * <p>The profile constrains the randomly generated cooperativeness, honesty,
     * and nervousness trait values.  For example, {@link PersonalityProfile#PSYCHOPATH}
     * produces a suspect who is very dishonest (honesty 1–2) and very confident
     * (nervousness 1–2) while appearing superficially cooperative (5–9).
     *
     * <p>Age range: 20–65.
     *
     * @param caseType the type of case being investigated; must not be
     *                 {@code null}
     * @param profile  the personality archetype to apply; {@code null} is treated
     *                 as {@link PersonalityProfile#DEFAULT}
     * @return a fully populated {@link NpcCharacter} with the SUSPECT role
     * @throws IllegalArgumentException if {@code caseType} is {@code null}
     */
    public NpcCharacter generateSuspect(CaseType caseType, PersonalityProfile profile) {
        if (caseType == null) throw new IllegalArgumentException("caseType must not be null");
        PersonalityProfile effectiveProfile = profile != null ? profile : PersonalityProfile.DEFAULT;
        String gender = randomGender();
        String id     = "char-suspect-" + (++npcCounter);
        return buildBase(id, gender, 20, 65, effectiveProfile)
                .occupation(suspectOccupation(caseType))
                .build();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a {@link NpcCharacter.Builder} pre-filled with fields common to
     * all roles: id, name, gender, age, sprite key, personality traits, and
     * the full set of investigative attributes.
     *
     * <p>The {@code profile}'s trait ranges constrain cooperativeness, honesty,
     * and nervousness values.
     */
    private NpcCharacter.Builder buildBase(String id, String gender,
                                           int minAge, int maxAge,
                                           PersonalityProfile profile) {
        String name      = nameGen.generateFull(gender);
        int    age       = minAge + random.nextInt(maxAge - minAge + 1);
        String spriteKey = pickSprite(gender);

        NpcCharacter.Builder b = new NpcCharacter.Builder()
                .id(id)
                .fullName(name)
                .gender(gender)
                .age(age)
                .spriteKey(spriteKey)
                .personalityProfile(profile)
                .cooperativeness(randomInRange(profile.getMinCooperativeness(),
                                               profile.getMaxCooperativeness()))
                .honesty(randomInRange(profile.getMinHonesty(),
                                       profile.getMaxHonesty()))
                .nervousness(randomInRange(profile.getMinNervousness(),
                                           profile.getMaxNervousness()));

        // Add all eleven investigative attributes, each randomly 1–10.
        for (CharacterAttribute attr : INVESTIGATIVE_ATTRIBUTES) {
            b.attribute(attr, randomAttr());
        }

        return b;
    }

    /** Convenience overload using {@link PersonalityProfile#DEFAULT}. */
    private NpcCharacter.Builder buildBase(String id, String gender,
                                           int minAge, int maxAge) {
        return buildBase(id, gender, minAge, maxAge, PersonalityProfile.DEFAULT);
    }

    /** Returns {@code "M"} or {@code "F"} at random. */
    private String randomGender() {
        return random.nextBoolean() ? "M" : "F";
    }

    /** Returns a random integer in the range 1–10 (inclusive). */
    private int randomAttr() {
        return ATTR_MIN + random.nextInt(ATTR_MAX - ATTR_MIN + 1);
    }

    /** Returns a random integer in the range {@code min}–{@code max} (inclusive). */
    private int randomInRange(int min, int max) {
        if (min == max) return min;
        return min + random.nextInt(max - min + 1);
    }

    /** Picks a sprite key consistent with the given gender. */
    private String pickSprite(String gender) {
        String[] pool = "F".equals(gender) ? FEMALE_SPRITES : MALE_SPRITES;
        return pool[random.nextInt(pool.length)];
    }

    // -------------------------------------------------------------------------
    // Occupation pools — small inline pools, one per role and case type.
    // These can be externalised to an occupations_en.json asset in future.
    // -------------------------------------------------------------------------

    private String clientOccupation(CaseType caseType) {
        switch (caseType) {
            case MISSING_PERSON:  return pick("Parent", "Sibling", "Partner", "Friend");
            case INFIDELITY:      return pick("Spouse", "Partner", "Fiancé");
            case THEFT:           return pick("Business Owner", "Homeowner", "Retailer");
            case FRAUD:           return pick("Investor", "Business Owner", "Accountant");
            case BLACKMAIL:       return pick("Executive", "Politician", "Public Figure");
            case MURDER:          return pick("Relative", "Colleague", "Friend");
            case STALKING:        return pick("Professional", "Teacher", "Nurse");
            case CORPORATE_ESPIONAGE: return pick("Executive", "Manager", "Director");
            default:              return "Citizen";
        }
    }

    private String victimOccupation(CaseType caseType) {
        switch (caseType) {
            case MISSING_PERSON:  return pick("Student", "Freelancer", "Retail Worker");
            case INFIDELITY:      return pick("Spouse", "Partner");
            case THEFT:           return "Property Owner";
            case FRAUD:           return pick("Investor", "Retiree", "Small Business Owner");
            case BLACKMAIL:       return pick("Executive", "Politician", "Entertainer");
            case MURDER:          return pick("Business Owner", "Accountant", "Journalist");
            case STALKING:        return pick("Teacher", "Nurse", "Social Worker");
            case CORPORATE_ESPIONAGE: return pick("Engineer", "Researcher", "Product Manager");
            default:              return "Citizen";
        }
    }

    private String suspectOccupation(CaseType caseType) {
        switch (caseType) {
            case MISSING_PERSON:  return pick("Acquaintance", "Landlord", "Ex-partner");
            case INFIDELITY:      return pick("Colleague", "Personal Trainer", "Neighbour");
            case THEFT:           return pick("Labourer", "Driver", "Unemployed");
            case FRAUD:           return pick("Accountant", "Financial Adviser", "Company Director");
            case BLACKMAIL:       return pick("Former Employee", "Journalist", "Acquaintance");
            case MURDER:          return pick("Associate", "Colleague", "Family Member");
            case STALKING:        return pick("Ex-partner", "Acquaintance", "Obsessive Fan");
            case CORPORATE_ESPIONAGE: return pick("Engineer", "Sales Manager", "Contractor");
            default:              return "Person of Interest";
        }
    }

    /** Returns one element chosen at random from the given strings. */
    private String pick(String... options) {
        return options[random.nextInt(options.length)];
    }
}
