package eb.framework1.character;

import eb.framework1.RandomUtils;
import eb.framework1.face.FaceConfig;
import eb.framework1.face.FaceGenerator;
import eb.framework1.face.FaceRule;
import eb.framework1.generator.*;
import eb.framework1.investigation.*;


import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
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

    /** Probability (≈ 33%) that an age-eligible male character receives a beard. */
    private static final double BEARD_PROBABILITY = 1.0 / 3.0;

    // Sprite key pools indexed by gender
    private static final String[] MALE_SPRITES   = { "man1", "man2" };
    private static final String[] FEMALE_SPRITES = { "woman1", "woman2" };

    // Appearance attribute pools
    private static final String[] HAIR_TYPES  = { "straight", "wavy", "curly", "buzzed" };
    private static final String[] HAIR_COLORS = { "black", "brown", "blonde", "red", "gray", "white" };
    private static final String[] FAV_COLORS  = { "", "", "", "red", "blue", "green",
                                                   "yellow", "purple", "orange", "black", "white" };

    /**
     * Maps hair colour names (as stored on {@link NpcCharacter}) to representative
     * hex values used in the face SVG portrait.  These colours are chosen to match
     * the shades produced by {@link eb.framework1.face.FaceGenerator}'s own palettes
     * so the text description is consistent with the rendered portrait.
     */
    private static final java.util.Map<String, String> HAIR_COLOR_HEX;
    static {
        java.util.Map<String, String> m = new java.util.HashMap<>();
        m.put("black",  "#272421");
        m.put("brown",  "#3D2314");
        m.put("blonde", "#CC9966");
        m.put("red",    "#B55239");
        m.put("gray",   "#909090");
        m.put("white",  "#E8E4E0");
        HAIR_COLOR_HEX = java.util.Collections.unmodifiableMap(m);
    }

    // Height ranges (cm) and weight ranges (kg) per gender
    /** Male height range: 160–195 cm. */
    private static final int MALE_HEIGHT_MIN   = 160;
    private static final int MALE_HEIGHT_MAX   = 195;
    /** Female height range: 150–180 cm. */
    private static final int FEMALE_HEIGHT_MIN = 150;
    private static final int FEMALE_HEIGHT_MAX = 180;
    /**
     * BMI offsets used to derive weight from height.
     * weight = (height_m)^2 × BMI; BMI is uniformly sampled in [17, 34].
     */
    private static final float BMI_MIN = 17f;
    private static final float BMI_MAX = 34f;

    private final PersonNameGenerator   nameGen;
    private final Random                random;
    private final FaceGenerator         faceGen;
    private final List<FaceRule>        faceRules;
    /** Skin-tone definitions used for weighted random assignment; may be empty. */
    private final List<SkinToneDefinition> skinTones;

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
        this(nameGen, new Random(), null, null);
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
        this(nameGen, random, null, null);
    }

    /**
     * Creates a generator with face rules for age- and gender-aware part selection.
     *
     * @param nameGen   name generator; must not be {@code null}
     * @param random    random-number source; {@code null} → {@code new Random()}
     * @param faceRules parsed face rules from {@code facerules.json};
     *                  {@code null} or empty disables rule-based face generation
     */
    public CharacterGenerator(PersonNameGenerator nameGen, Random random, List<FaceRule> faceRules) {
        this(nameGen, random, faceRules, null);
    }

    /**
     * Creates a generator with face rules and weighted skin-tone definitions.
     *
     * @param nameGen    name generator; must not be {@code null}
     * @param random     random-number source; {@code null} → {@code new Random()}
     * @param faceRules  parsed face rules; {@code null} or empty → no rules
     * @param skinTones  skin-tone definitions with percentage weights;
     *                   {@code null} or empty → no skin-tone assignment
     */
    public CharacterGenerator(PersonNameGenerator nameGen, Random random,
                               List<FaceRule> faceRules,
                               List<SkinToneDefinition> skinTones) {
        if (nameGen == null) {
            throw new IllegalArgumentException("nameGen must not be null");
        }
        this.nameGen    = nameGen;
        this.random     = random != null ? random : new Random();
        this.faceGen    = new FaceGenerator(this.random);
        this.faceRules  = (faceRules  != null) ? faceRules  : Collections.emptyList();
        this.skinTones  = (skinTones  != null) ? skinTones  : Collections.emptyList();
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
        String hairColor = pick(HAIR_COLORS);
        String hairType  = pick(HAIR_TYPES);

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
                                           profile.getMaxNervousness()))
                .hairType(hairType)
                .hairColor(hairColor)
                .wealthyLevel(ATTR_MIN + random.nextInt(ATTR_MAX - ATTR_MIN + 1))
                .favColor(pick(FAV_COLORS));

        int genHeight = randomHeight(gender);
        b.heightCm(genHeight).weightKg(randomWeight(genHeight));

        // Add all eleven investigative attributes, each randomly 1–10.
        for (CharacterAttribute attr : INVESTIGATIVE_ATTRIBUTES) {
            b.attribute(attr, randomAttr());
        }

        // Assign 3–5 random personality traits (hidden, −3 to +3 each).
        // Each NPC gets a random subset so that not every character has
        // opinions about everything — the player must interview multiple NPCs
        // to build a full personality profile.
        assignRandomPersonalityTraits(b);

        // 30% of characters have a vision impairment (farsighted or nearsighted).
        VisionTrait visionTrait = null;
        if (random.nextFloat() < 0.30f) {
            visionTrait = random.nextBoolean() ? VisionTrait.FARSIGHTED : VisionTrait.NEARSIGHTED;
            b.visionTrait(visionTrait);
        }

        // Generate a vector face that matches the NPC's gender and age.
        String normGender = "F".equals(gender) ? "female" : "male";
        FaceGenerator.Options faceOpts = new FaceGenerator.Options().gender(normGender);
        FaceConfig face;
        // beard style text ("short beard", "long beard", "stubble", or "")
        String[] beardStyleOut = {""};
        if (!faceRules.isEmpty()) {
            // Use rule-based eligible pool (age- and gender-aware)
            long faceSeed = (long) id.hashCode() * 2654435761L ^ age;
            Map<String, List<String>> pool =
                    new HashMap<>(FaceGenerator.defaultCharacterFace(faceSeed, normGender, age, faceRules));
            // When the NPC has a vision impairment they will carry glasses;
            // apply the gender-specific glasses rule so the face SVG shows them.
            if (visionTrait != null && visionTrait.isImpaired()) {
                pool = applyGlassesRule(pool, normGender, faceRules);
            }
            // For male characters apply the Bold rule to decide baldness, then
            // assign a beard/shave style based on age.
            if ("male".equals(normGender)) {
                boolean[] baldOut = {false};
                pool = applyBoldRule(pool, faceSeed, age, faceRules, baldOut);
                if (baldOut[0]) {
                    b.hairType("bald");
                }
                pool = applyBeardRule(pool, faceOpts, age, faceRules, beardStyleOut);
            }
            face = faceGen.generate(faceOpts, pool);
        } else {
            face = faceGen.generate(faceOpts);
        }

        b.beardStyle(beardStyleOut[0]);

        // Sync the portrait hair colour with the NpcCharacter hair colour name so
        // that the rendered face matches the text description.
        String hairColorHex = HAIR_COLOR_HEX.get(hairColor);
        if (hairColorHex != null) {
            face = face.withHairColor(hairColorHex);
        }

        // Assign a skin tone based on weighted random selection.
        if (!skinTones.isEmpty()) {
            SkinToneDefinition chosen = pickWeightedSkinTone();
            if (chosen != null) {
                face = face.withSkinColor(chosen.getRgb());
                b.skinToneCode(chosen.getCode());
            }
        }

        b.faceConfig(face);
        return b;
    }

    /**
     * Picks a skin tone using weighted random selection based on each
     * definition's {@code percentage} value.  Returns {@code null} if the
     * list is empty or all weights are zero.
     */
    private SkinToneDefinition pickWeightedSkinTone() {
        int total = 0;
        for (SkinToneDefinition st : skinTones) total += Math.max(0, st.getPercentage());
        if (total <= 0) return null;
        int roll = random.nextInt(total);
        int cum  = 0;
        for (SkinToneDefinition st : skinTones) {
            cum += Math.max(0, st.getPercentage());
            if (roll < cum) return st;
        }
        return skinTones.get(skinTones.size() - 1);
    }

    /** Convenience overload using {@link PersonalityProfile#DEFAULT}. */
    private NpcCharacter.Builder buildBase(String id, String gender,
                                           int minAge, int maxAge) {
        return buildBase(id, gender, minAge, maxAge, PersonalityProfile.DEFAULT);
    }

    /**
     * Augments the face-part pool with glasses SVG IDs from the
     * {@code glassesMale} or {@code glassesFemale} rule when the NPC needs
     * vision correction.  Returns a new mutable map; the input map is not
     * modified.
     *
     * @param pool       the current pool (mutable copy expected)
     * @param normGender {@code "male"} or {@code "female"}
     * @param rules      the full list of face rules
     * @return the same map with a {@code "glasses"} entry added (or unchanged
     *         if no matching rule was found)
     */
    private static Map<String, List<String>> applyGlassesRule(
            Map<String, List<String>> pool,
            String normGender,
            List<FaceRule> rules) {
        String targetName = "female".equals(normGender) ? "glassesFemale" : "glassesMale";
        for (FaceRule rule : rules) {
            if (targetName.equals(rule.name)) {
                List<String> glassesIds = new ArrayList<>();
                for (String entry : rule.include) {
                    int dot = entry.indexOf('.');
                    if (dot > 0 && "glasses".equals(entry.substring(0, dot))) {
                        glassesIds.add(entry.substring(dot + 1));
                    }
                }
                if (!glassesIds.isEmpty()) {
                    pool.put("glasses", Collections.unmodifiableList(glassesIds));
                }
                break;
            }
        }
        return pool;
    }

    /**
     * Assigns a beard/shave style to a male character based on {@code age} and
     * a random roll, encoding the result in the pool and/or {@code faceOpts}.
     *
     * <p>Approximately 1/3 of age-eligible men receive a short or long beard
     * (drawn from the {@code "Beard Short"} or {@code "Beard Long"} face rules).
     * The remaining men receive a non-beard shave style whose intensity is set
     * as a {@code shaveColor} override on {@code faceOpts}.
     *
     * <table border="1" summary="Beard eligibility by age">
     *   <tr><th>Age</th><th>Eligible styles</th></tr>
     *   <tr><td>&lt; 20</td><td>clean shaven, shaven (no beards)</td></tr>
     *   <tr><td>20–24</td><td>shaven, stubbles, short beard (1/3 chance)</td></tr>
     *   <tr><td>25+</td><td>shaven, stubbles, short beard or long beard (1/3 chance)</td></tr>
     * </table>
     *
     * @param pool         mutable pool to augment with facial-hair IDs when a beard style is chosen
     * @param faceOpts     options object whose {@code shaveColor} is set for non-beard styles
     * @param age          character age
     * @param rules        full list of face rules
     * @param beardStyleOut single-element array; set to a descriptive beard label on return.
     *                     Possible values: {@code "short beard"}, {@code "long beard"},
     *                     {@code "stubble"}, or {@code ""} (clean-shaven / shaven).
     * @return the same {@code pool} (possibly augmented with a {@code "facialHair"} entry)
     */
    private Map<String, List<String>> applyBeardRule(
            Map<String, List<String>> pool,
            FaceGenerator.Options faceOpts,
            int age,
            List<FaceRule> rules,
            String[] beardStyleOut) {
        if (age >= 20 && random.nextDouble() < BEARD_PROBABILITY) {
            // 1/3 of men aged 20+ get a beard.
            // Under 25, only short beard is available; 25+ split evenly between short and long.
            boolean longBeard = age >= 25 && random.nextBoolean();
            addFacialHairFromRule(pool, longBeard ? "Beard Long" : "Beard Short", rules);
            faceOpts.shaveColor("rgba(0,0,0,0.0)");
            beardStyleOut[0] = longBeard ? "long beard" : "short beard";
        } else {
            // Non-beard shave style.
            // Under 20: clean shaven (alpha 0) or shaven (alpha 0.06) with equal probability.
            // 20+: shaven or stubbles (alpha 0.15) with equal probability.
            final String shaveColor;
            if (age < 20) {
                shaveColor = random.nextBoolean() ? "rgba(0,0,0,0.0)" : "rgba(0,0,0,0.06)";
            } else {
                shaveColor = random.nextBoolean() ? "rgba(0,0,0,0.06)" : "rgba(0,0,0,0.15)";
            }
            faceOpts.shaveColor(shaveColor);
            // Only "stubble" is visibly noteworthy; shaven/clean-shaven are left as "".
            beardStyleOut[0] = "rgba(0,0,0,0.15)".equals(shaveColor) ? "stubble" : "";
        }
        return pool;
    }

    /**
     * Applies the {@code "Bold"} face rule to decide whether a male character
     * should be bald.  The rule's {@code minAge} and {@code percentage} conditions
     * are evaluated with a seeded RNG so the result is deterministic per character.
     *
     * <p>When the rule fires:
     * <ul>
     *   <li>The {@code "hair"} and {@code "hairBg"} entries in {@code pool} are
     *       replaced with the IDs supplied by the Bold rule's {@code include} list.</li>
     *   <li>{@code baldOut[0]} is set to {@code true}.</li>
     * </ul>
     *
     * <p>When the rule does not fire (character too young, percentage roll fails, or
     * no "Bold" rule exists) the pool is left unchanged and {@code baldOut[0]}
     * remains {@code false}.
     *
     * @param pool     mutable pool to augment when Bold fires
     * @param seed     per-character seed for deterministic percentage rolls
     * @param age      character age
     * @param rules    full list of face rules
     * @param baldOut  single-element array; set to {@code true} when Bold fires
     * @return the same {@code pool} (possibly updated with bald hair IDs)
     */
    private static Map<String, List<String>> applyBoldRule(
            Map<String, List<String>> pool,
            long seed,
            int age,
            List<FaceRule> rules,
            boolean[] baldOut) {
        for (int i = 0; i < rules.size(); i++) {
            FaceRule rule = rules.get(i);
            if (!"Bold".equals(rule.name)) continue;

            // Age gate
            if (rule.minAge > 0 && age < rule.minAge) break;

            // Percentage roll — seeded identically to defaultCharacterFace rolls
            if (rule.percentage < 100) {
                long rollSeed = seed ^ (long) i * 6364136223846793005L;
                Random rollRng = new Random(rollSeed);
                if (rollRng.nextInt(100) >= rule.percentage) break;
            }

            // Rule fires — inject hair IDs from the Bold rule's include list
            Map<String, List<String>> hairEntries = new HashMap<>();
            for (String entry : rule.include) {
                int dot = entry.indexOf('.');
                if (dot > 0) {
                    String featureType = entry.substring(0, dot);
                    String featureId   = entry.substring(dot + 1);
                    hairEntries.computeIfAbsent(featureType, k -> new ArrayList<>()).add(featureId);
                }
            }
            for (Map.Entry<String, List<String>> e : hairEntries.entrySet()) {
                pool.put(e.getKey(), Collections.unmodifiableList(e.getValue()));
            }
            baldOut[0] = true;
            break;
        }
        return pool;
    }

    /**
     * Adds {@code facialHair} IDs from the named rule's {@code include} list to
     * {@code pool}.  If the rule is not found or has no facial-hair entries the
     * pool is left unchanged.
     */
    private static void addFacialHairFromRule(
            Map<String, List<String>> pool,
            String ruleName,
            List<FaceRule> rules) {
        for (FaceRule rule : rules) {
            if (ruleName.equals(rule.name)) {
                List<String> ids = new ArrayList<>();
                for (String entry : rule.include) {
                    int dot = entry.indexOf('.');
                    if (dot > 0 && "facialHair".equals(entry.substring(0, dot))) {
                        ids.add(entry.substring(dot + 1));
                    }
                }
                if (!ids.isEmpty()) {
                    pool.put("facialHair", Collections.unmodifiableList(ids));
                }
                break;
            }
        }
    }

    /** Returns {@code "M"} or {@code "F"} at random. */
    private String randomGender() {
        return RandomUtils.randomGender(random);
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

    /**
     * Assigns 3–5 random {@link PersonalityTrait}s to the builder.
     * Each trait gets a random value from −3 to +3 (excluding 0 to ensure
     * the trait is actually notable).
     */
    private void assignRandomPersonalityTraits(NpcCharacter.Builder b) {
        PersonalityTrait[] allTraits = PersonalityTrait.values();
        int traitCount = 3 + random.nextInt(3); // 3–5 traits
        // Shuffle by doing a Fisher-Yates partial shuffle on a copy
        PersonalityTrait[] shuffled = allTraits.clone();
        for (int i = 0; i < traitCount && i < shuffled.length; i++) {
            int j = i + random.nextInt(shuffled.length - i);
            PersonalityTrait tmp = shuffled[i];
            shuffled[i] = shuffled[j];
            shuffled[j] = tmp;
        }
        for (int i = 0; i < traitCount && i < shuffled.length; i++) {
            // Generate a non-zero value: −3..−1 or +1..+3
            int value = 1 + random.nextInt(3); // 1–3
            if (random.nextBoolean()) value = -value;
            b.personalityTrait(shuffled[i], value);
        }
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
        return RandomUtils.pick(random, options);
    }

    /** Returns a random height (cm) for the given gender. */
    private int randomHeight(String gender) {
        boolean female = "F".equalsIgnoreCase(gender);
        int min = female ? FEMALE_HEIGHT_MIN : MALE_HEIGHT_MIN;
        int max = female ? FEMALE_HEIGHT_MAX : MALE_HEIGHT_MAX;
        return min + random.nextInt(max - min + 1);
    }

    /**
     * Returns a random body weight (kg) consistent with a realistic BMI range
     * for the given height.
     *
     * <p>Formula: {@code round(heightM² × bmi)} where {@code bmi} is sampled
     * uniformly in [{@link #BMI_MIN}, {@link #BMI_MAX}].
     */
    private int randomWeight(int heightCm) {
        float heightM = heightCm / 100f;
        float bmi     = BMI_MIN + random.nextFloat() * (BMI_MAX - BMI_MIN);
        return Math.round(heightM * heightM * bmi);
    }

    /**
     * Randomly distributes {@code freePoints} across the eleven investigative
     * attributes, starting each at {@link #ATTR_MIN} (1).
     *
     * <p>This mirrors the point-buy logic of {@link
     * eb.framework1.screen.CharacterAttributeScreen} but allocates the points
     * randomly instead of letting the player choose.  The result is a balanced
     * starting character whose attribute total equals
     * {@code 11 * ATTR_MIN + freePoints}.
     *
     * <p>The method has no libGDX dependency and can be called from plain JUnit
     * tests.  Pass a seeded {@link Random} for deterministic output.
     *
     * @param freePoints the number of extra points to distribute above the
     *                   minimum; non-negative
     * @param random     source of randomness; {@code null} creates a new
     *                   {@code Random}
     * @return an {@link EnumMap} mapping each investigative
     *         {@link CharacterAttribute} to its assigned value (1–10)
     */
    public static Map<CharacterAttribute, Integer> generatePlayerAttributes(
            int freePoints, Random random) {

        if (freePoints < 0) {
            throw new IllegalArgumentException("freePoints must be non-negative");
        }
        Random rng = random != null ? random : new Random();

        Map<CharacterAttribute, Integer> attrs = new EnumMap<>(CharacterAttribute.class);
        for (CharacterAttribute attr : INVESTIGATIVE_ATTRIBUTES) {
            attrs.put(attr, ATTR_MIN);
        }

        int remaining = freePoints;
        // Safety counter: prevents an infinite loop when freePoints exceeds the
        // maximum distributable total (ATTR_MAX - ATTR_MIN) * attribute count.
        // Using 2× the product of freePoints and attribute count gives enough
        // attempts even in the most adversarial random sequences while keeping
        // the bound tight.
        int maxAttempts = freePoints * INVESTIGATIVE_ATTRIBUTES.length * 2 + 1;
        int attempts = 0;
        while (remaining > 0 && attempts < maxAttempts) {
            CharacterAttribute candidate =
                    INVESTIGATIVE_ATTRIBUTES[rng.nextInt(INVESTIGATIVE_ATTRIBUTES.length)];
            if (attrs.get(candidate) < ATTR_MAX) {
                attrs.put(candidate, attrs.get(candidate) + 1);
                remaining--;
            }
            attempts++;
        }

        return attrs;
    }
}
