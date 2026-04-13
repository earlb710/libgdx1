package eb.framework1.character;

import eb.framework1.face.FaceConfig;
import eb.framework1.investigation.*;


import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A non-player character (NPC) generated for use as a client, victim, or suspect
 * in a detective case.
 *
 * <p>Every NPC carries:
 * <ul>
 *   <li><strong>Identity</strong> — name, gender, age, occupation.</li>
 *   <li><strong>Appearance</strong> — sprite key matching the existing character-sprite
 *       set and a one-sentence physical description.</li>
 *   <li><strong>Location</strong> — home and workplace addresses (building names
 *       resolvable on the city map) plus a short list of frequently-visited places.</li>
 *   <li><strong>Contact</strong> — phone number and email.</li>
 *   <li><strong>Personality</strong> — cooperativeness, honesty and nervousness
 *       (1–10 each), used to weight interview and surveillance outcomes.</li>
 *   <li><strong>Character attributes</strong> — the same eleven investigative
 *       attributes as the player character ({@link CharacterAttribute}):
 *       Intelligence, Perception, Memory, Intuition, Agility, Stamina, Strength,
 *       Charisma, Intimidation, Empathy, and Stealth.  Each value is in the
 *       range 1–10.  Generated automatically by {@link CharacterGenerator};
 *       NPCs do not go through the player's point-buy screen.</li>
 *   <li><strong>Items</strong> — items the NPC visibly carries, such as a pistol
 *       for a police officer.  Assigned by {@link NpcGenerator} based on
 *       occupation; empty for most NPCs.</li>
 * </ul>
 *
 * <p>Instances are built through {@link Builder}.  All fields are immutable once
 * constructed.
 *
 * <h3>Example</h3>
 * <pre>
 *   CharacterGenerator gen = new CharacterGenerator(nameGenerator, new Random(42));
 *   NpcCharacter client  = gen.generateClient(CaseType.FRAUD);
 *   NpcCharacter victim  = gen.generateVictim(CaseType.MURDER);
 *   NpcCharacter suspect = gen.generateSuspect(CaseType.MURDER);
 *
 *   int intel = client.getAttribute(CharacterAttribute.INTELLIGENCE); // 1–10
 * </pre>
 */
public final class NpcCharacter {

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final String id;
    private final String fullName;
    private final String gender;
    private final int    age;
    private final String occupation;

    private final String spriteKey;
    private final String physicalDescription;

    private final String       homeAddress;
    private final String       workplaceAddress;
    private final List<String> frequentLocations;

    private final String phoneNumber;
    private final String email;

    private final int cooperativeness;
    private final int honesty;
    private final int nervousness;
    private final PersonalityProfile personalityProfile;

    /**
     * The eleven investigative attributes, keyed by {@link CharacterAttribute}.
     * Only attributes where {@link CharacterAttribute#isBodyMeasurement()} and
     * {@link CharacterAttribute#isDerivedAttribute()} are both {@code false} are
     * stored here (i.e. the same 11 attributes as the player's point-buy system).
     */
    private final Map<CharacterAttribute, Integer> attributes;

    /**
     * Skills inferred from the NPC's occupation.  Populated by {@link NpcGenerator};
     * empty list when constructed via {@link CharacterGenerator} directly.
     */
    private final List<NpcSkill> skills;

    /**
     * Daily schedule that describes where the NPC is at each hour of the day.
     * {@code null} when constructed via {@link CharacterGenerator} directly.
     */
    private final NpcSchedule schedule;

    /**
     * Date of birth in {@code "YYYY-MM-DD"} format (using the in-game year).
     * Empty string when not explicitly set.
     */
    private final String birthdate;

    /**
     * Whether this NPC can be tracked / shown on the city map.
     * In debug / developer mode all NPCs are treated as tracked regardless of
     * this flag.
     */
    private final boolean tracked;

    /**
     * {@code true} if this NPC is deceased (e.g. a murder victim).
     * A dead NPC will not appear on the city map or respond to interviews.
     */
    private final boolean dead;

    /**
     * Estimated date and time of death as milliseconds since the Unix epoch
     * (in-game time).  {@code 0L} when the NPC is alive or the death
     * date/time is unknown (e.g. body missing in a possible-murder case).
     *
     * <p>When non-zero this is the <em>best-estimate</em> time of death as
     * determined by a coroner or detective.  The actual time may differ by up
     * to ±{@link #deathTimeVarianceMinutes} minutes.
     *
     * <p>Use {@link #getDeathDateTimeFormatted()} to obtain a human-readable
     * {@code "YYYY-MM-DD HH:mm"} string for display.
     */
    private final long deathDateTime;

    /**
     * Error variance on the estimated time of death, in minutes.
     * <ul>
     *   <li>{@code 0} — the time of death is known precisely</li>
     *   <li>{@code > 0} — the true time of death falls within
     *       ±{@code deathTimeVarianceMinutes} of {@link #deathDateTime}</li>
     *   <li>{@code -1} — the time of death is completely unknown (e.g. body
     *       is missing; "possible murder")</li>
     * </ul>
     * Only meaningful when {@link #dead} is {@code true}.
     */
    private final int deathTimeVarianceMinutes;

    // -------------------------------------------------------------------------
    // Appearance attributes
    // -------------------------------------------------------------------------

    /** Hair type, e.g. {@code "straight"}, {@code "wavy"}, {@code "curly"}, {@code "bald"}. */
    private final String hairType;

    /** Hair colour, e.g. {@code "black"}, {@code "brown"}, {@code "blonde"}, {@code "red"},
     *  {@code "gray"}, {@code "white"}. */
    private final String hairColor;

    /**
     * Beard/facial-hair style for male characters.
     * Examples: {@code "short beard"}, {@code "long beard"}, {@code "stubble"}.
     * Empty string for female characters or clean-shaven men.
     */
    private final String beardStyle;

    /**
     * Apparent wealth level on a 1–10 scale.
     * 1 = visibly destitute; 10 = ostentatiously wealthy.
     */
    private final int wealthyLevel;

    /**
     * The NPC's favourite colour (optional; empty string means none observable).
     * Examples: {@code "red"}, {@code "blue"}, {@code "green"}.
     */
    private final String favColor;

    /**
     * Height in centimetres (e.g. 175).
     * 0 = not set (description engine will omit the height sentence).
     */
    private final int heightCm;

    /**
     * Body weight in kilograms (e.g. 75).
     * 0 = not set (description engine will omit the build sentence).
     */
    private final int weightKg;

    /**
     * Items this NPC is currently carrying (e.g. a pistol for a police officer).
     * The list is empty for NPCs that carry nothing visible.
     */
    private final List<EquipItem> carriedItems;

    /**
     * Vision impairment, if any.  {@link VisionTrait#NONE} means no impairment.
     * Characters with an impaired trait typically also carry {@link EquipItem#GLASSES}.
     */
    private final VisionTrait visionTrait;

    /**
     * Vector-face configuration generated by {@link eb.framework1.face.FaceGenerator}.
     * May be {@code null} when the NPC was created without face generation (e.g.
     * in older save files or test fixtures that pre-date this feature).
     */
    private final FaceConfig faceConfig;

    /**
     * Skin-tone category code for this NPC (e.g. {@code "fair_light"}).
     * May be {@code null} for NPCs created before skin-tone assignment was added.
     */
    private final String skinToneCode;

    /**
     * Hidden personality traits / opinions, each in the range −3 to +3.
     * Only a subset of the available {@link PersonalityTrait} values are
     * assigned to any one NPC (typically 3–5).  Traits not present in the map
     * are considered neutral (0).
     *
     * <p>Traits are hidden from the player and can only be discovered through
     * interviews.  They contribute indirectly to the case — for example,
     * knowing a suspect loves hiking narrows possible locations.
     */
    private final Map<PersonalityTrait, Integer> personalityTraits;

    /**
     * Relationships this NPC has formed with characters they have met.
     * The list is mutable so that relationship entries can be added during
     * gameplay without rebuilding the NPC object.
     */
    private final List<Relationship> relationships = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    private NpcCharacter(Builder b) {
        this.id                   = b.id;
        this.fullName             = b.fullName;
        this.gender               = b.gender;
        this.age                  = b.age;
        this.occupation           = b.occupation;
        this.spriteKey            = b.spriteKey;
        this.physicalDescription  = b.physicalDescription;
        this.homeAddress          = b.homeAddress;
        this.workplaceAddress     = b.workplaceAddress;
        this.frequentLocations    = Collections.unmodifiableList(
                new ArrayList<>(b.frequentLocations));
        this.phoneNumber          = b.phoneNumber;
        this.email                = b.email;
        this.cooperativeness      = b.cooperativeness;
        this.honesty              = b.honesty;
        this.nervousness          = b.nervousness;
        this.personalityProfile   = b.personalityProfile;
        this.attributes           = Collections.unmodifiableMap(
                new EnumMap<>(b.attributes));
        this.skills               = Collections.unmodifiableList(
                new ArrayList<>(b.skills));
        this.schedule             = b.schedule;
        this.birthdate            = b.birthdate != null ? b.birthdate : "";
        this.tracked              = b.tracked;
        this.dead                 = b.dead;
        this.deathDateTime        = b.deathDateTime;
        this.deathTimeVarianceMinutes = b.deathTimeVarianceMinutes;
        this.hairType             = b.hairType  != null ? b.hairType  : "";
        this.hairColor            = b.hairColor != null ? b.hairColor : "";
        this.beardStyle           = b.beardStyle != null ? b.beardStyle : "";
        this.wealthyLevel         = b.wealthyLevel;
        this.favColor             = b.favColor  != null ? b.favColor  : "";
        this.heightCm             = b.heightCm;
        this.weightKg             = b.weightKg;
        this.carriedItems         = Collections.unmodifiableList(
                new ArrayList<>(b.carriedItems));
        this.visionTrait          = b.visionTrait;
        this.faceConfig           = b.faceConfig;
        this.skinToneCode         = b.skinToneCode;
        this.personalityTraits    = Collections.unmodifiableMap(
                new EnumMap<>(b.personalityTraits));
    }

    // -------------------------------------------------------------------------
    // Accessors — identity
    // -------------------------------------------------------------------------

    /** Unique identifier for this character within its case. */
    public String getId() { return id; }

    /** Full name (first name + surname), e.g. {@code "Alice Smith"}. */
    public String getFullName() { return fullName; }

    /** Gender tag: {@code "M"} or {@code "F"}. */
    public String getGender() { return gender; }

    /** Age in years. */
    public int getAge() { return age; }

    /** Job title or occupation string. */
    public String getOccupation() { return occupation; }

    // -------------------------------------------------------------------------
    // Accessors — appearance
    // -------------------------------------------------------------------------

    /**
     * Key used to look up the character's sprite (e.g. {@code "man1"},
     * {@code "woman2"}).
     */
    public String getSpriteKey() { return spriteKey; }

    /**
     * One-sentence description of physical appearance, used in interview and
     * surveillance flavour text.
     */
    public String getPhysicalDescription() { return physicalDescription; }

    // -------------------------------------------------------------------------
    // Accessors — location
    // -------------------------------------------------------------------------

    /** Home address (building name resolvable on the city map). */
    public String getHomeAddress() { return homeAddress; }

    /** Workplace address (building name resolvable on the city map). */
    public String getWorkplaceAddress() { return workplaceAddress; }

    /**
     * Up to three additional places the character visits regularly (bars,
     * gyms, etc.).  Returns an unmodifiable list.
     */
    public List<String> getFrequentLocations() { return frequentLocations; }

    // -------------------------------------------------------------------------
    // Accessors — contact
    // -------------------------------------------------------------------------

    /** Formatted phone number string. */
    public String getPhoneNumber() { return phoneNumber; }

    /** Email address (may be empty if not applicable). */
    public String getEmail() { return email; }

    // -------------------------------------------------------------------------
    // Accessors — personality
    // -------------------------------------------------------------------------

    /**
     * How willingly this NPC provides information when interviewed (1–10).
     * A higher value makes {@link DiscoveryMethod#INTERVIEW} lead checks easier.
     */
    public int getCooperativeness() { return cooperativeness; }

    /**
     * How truthful this NPC is when interviewed (1–10).
     * A lower value means extra verification actions are required to confirm
     * what they say.
     */
    public int getHonesty() { return honesty; }

    /**
     * How visibly anxious this NPC is (1–10).
     * Used as an in-game signal of possible involvement, especially during
     * interview and surveillance activities.
     */
    public int getNervousness() { return nervousness; }

    /**
     * The personality archetype of this NPC.
     * Determines the ranges from which honesty, nervousness, and cooperativeness
     * were drawn.  Never {@code null}; defaults to {@link PersonalityProfile#DEFAULT}.
     */
    public PersonalityProfile getPersonalityProfile() { return personalityProfile; }

    // -------------------------------------------------------------------------
    // Accessors — personality traits (hidden opinions, −3 to +3)
    // -------------------------------------------------------------------------

    /**
     * Returns the value of a single hidden personality trait (−3 to +3).
     *
     * <p>Traits are hidden from the player and discovered through interviews.
     * A value of 0 (neutral) is returned for traits not explicitly assigned.
     *
     * @param trait the trait to look up; must not be {@code null}
     * @return the trait value, or {@code 0} if not present
     */
    public int getTraitValue(PersonalityTrait trait) {
        if (trait == null) return 0;
        return personalityTraits.getOrDefault(trait, 0);
    }

    /**
     * Returns an unmodifiable copy of all assigned personality traits.
     * Only traits that were explicitly set (non-neutral) are included.
     *
     * @return map of trait → value (−3 to +3); never {@code null}
     */
    public Map<PersonalityTrait, Integer> getPersonalityTraits() {
        return personalityTraits;
    }

    /**
     * Returns {@code true} if this NPC has any personality traits assigned.
     */
    public boolean hasPersonalityTraits() {
        return !personalityTraits.isEmpty();
    }

    /**
     * Returns a human-readable summary of this NPC's personality traits,
     * e.g. {@code "likes Sports, strongly dislikes Gambling, neutral Cooking"}.
     * Returns an empty string if no traits are assigned.
     */
    public String getTraitsSummary() {
        if (personalityTraits.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<PersonalityTrait, Integer> e : personalityTraits.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.getKey().describe(e.getValue()));
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Accessors — character attributes
    // -------------------------------------------------------------------------

    /**
     * Returns the value (1–10) of a single investigative attribute.
     *
     * <p>The eleven investigative attributes are the same ones used by the
     * player character:
     * {@link CharacterAttribute#INTELLIGENCE}, {@link CharacterAttribute#PERCEPTION},
     * {@link CharacterAttribute#MEMORY}, {@link CharacterAttribute#INTUITION},
     * {@link CharacterAttribute#AGILITY}, {@link CharacterAttribute#STAMINA},
     * {@link CharacterAttribute#STRENGTH}, {@link CharacterAttribute#CHARISMA},
     * {@link CharacterAttribute#INTIMIDATION}, {@link CharacterAttribute#EMPATHY},
     * {@link CharacterAttribute#STEALTH}.
     *
     * <p>Body-measurement attributes ({@link CharacterAttribute#isBodyMeasurement()})
     * and the derived {@link CharacterAttribute#DETECTIVE_LEVEL} are not stored;
     * querying them returns 0.
     *
     * @param attribute the attribute to look up; must not be {@code null}
     * @return the attribute value, or {@code 0} if not present
     */
    public int getAttribute(CharacterAttribute attribute) {
        if (attribute == null) return 0;
        return attributes.getOrDefault(attribute, 0);
    }

    /**
     * Returns an unmodifiable copy of all stored attribute values.
     * Only the eleven investigative attributes are present (no body measurements
     * or derived attributes).
     */
    public Map<CharacterAttribute, Integer> getAttributes() {
        return attributes;
    }

    // -------------------------------------------------------------------------
    // Accessors — skills and schedule
    // -------------------------------------------------------------------------

    /**
     * Returns the skills inferred from this NPC's occupation by
     * {@link NpcGenerator}.  Returns an empty list for NPCs created directly
     * via {@link CharacterGenerator} (without the full generation pipeline).
     *
     * @return unmodifiable list of skills; never {@code null}
     */
    public List<NpcSkill> getSkills() {
        return skills;
    }

    /**
     * Returns the NPC's daily schedule as built by {@link NpcGenerator}, or
     * {@code null} if the NPC was not enriched by the generator.
     *
     * @return the {@link NpcSchedule}, or {@code null}
     */
    public NpcSchedule getSchedule() {
        return schedule;
    }

    // -------------------------------------------------------------------------
    // Accessors — birthdate and tracking
    // -------------------------------------------------------------------------

    /**
     * Returns the NPC's date of birth in {@code "YYYY-MM-DD"} format (using
     * the in-game calendar year).  Returns an empty string if the birthdate
     * was not explicitly set.
     */
    public String getBirthdate() {
        return birthdate;
    }

    /**
     * Returns {@code true} if this NPC can be tracked / shown on the city map
     * in normal game mode.  In developer / debug mode all NPCs are treated as
     * tracked regardless of this flag.
     */
    public boolean isTracked() {
        return tracked;
    }

    /**
     * Returns {@code true} if this NPC is deceased (e.g. a murder victim).
     * A dead NPC does not appear on the city map and cannot be interviewed.
     */
    public boolean isDead() {
        return dead;
    }

    /**
     * Returns the estimated date and time of death as milliseconds since the
     * Unix epoch, or {@code 0L} if this NPC is alive or the death date/time
     * is unknown.
     */
    public long getDeathDateTime() {
        return deathDateTime;
    }

    /**
     * Returns the estimated date and time of death formatted as
     * {@code "YYYY-MM-DD HH:mm"} for display purposes, or an empty string
     * when this NPC is alive or the time of death is unknown ({@code 0L}).
     */
    public String getDeathDateTimeFormatted() {
        if (deathDateTime == 0L) return "";
        java.util.Calendar cal = java.util.Calendar.getInstance(
                java.util.TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(deathDateTime);
        return String.format("%04d-%02d-%02d %02d:%02d",
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1,
                cal.get(java.util.Calendar.DAY_OF_MONTH),
                cal.get(java.util.Calendar.HOUR_OF_DAY),
                cal.get(java.util.Calendar.MINUTE));
    }

    /**
     * Returns the error variance on the estimated time of death, in minutes.
     * <ul>
     *   <li>{@code 0} — time of death is known precisely</li>
     *   <li>{@code > 0} — true time of death is within
     *       ±this many minutes of {@link #getDeathDateTime()}</li>
     *   <li>{@code -1} — time of death is completely unknown (body missing)</li>
     * </ul>
     * Only meaningful when {@link #isDead()} is {@code true}.
     */
    public int getDeathTimeVarianceMinutes() {
        return deathTimeVarianceMinutes;
    }

    // -------------------------------------------------------------------------
    // Accessors — appearance attributes
    // -------------------------------------------------------------------------

    /**
     * Hair type string, e.g. {@code "straight"}, {@code "wavy"}, {@code "curly"},
     * {@code "bald"}.  Empty string when not set.
     */
    public String getHairType() { return hairType; }

    /**
     * Hair colour string, e.g. {@code "black"}, {@code "brown"}, {@code "blonde"},
     * {@code "red"}, {@code "gray"}, {@code "white"}.  Empty string when not set.
     */
    public String getHairColor() { return hairColor; }

    /**
     * Beard/facial-hair style for male characters, e.g. {@code "short beard"},
     * {@code "long beard"}, {@code "stubble"}.  Empty string for females or
     * clean-shaven men.
     */
    public String getBeardStyle() { return beardStyle; }

    /**
     * Apparent wealth level on a 1–10 scale.
     * 1 = visibly destitute; 10 = ostentatiously wealthy.
     */
    public int getWealthyLevel() { return wealthyLevel; }

    /**
     * Favourite colour (optional), e.g. {@code "red"}, {@code "blue"}.
     * Empty string when none is observable.
     */
    public String getFavColor() { return favColor; }

    /**
     * Height in centimetres, e.g. {@code 175}.
     * Returns {@code 0} when not set.
     */
    public int getHeightCm() { return heightCm; }

    /**
     * Body weight in kilograms, e.g. {@code 75}.
     * Returns {@code 0} when not set.
     */
    public int getWeightKg() { return weightKg; }

    /**
     * Items this NPC is currently carrying (e.g. a pistol for a police officer).
     * Returns an unmodifiable list; never {@code null}.
     */
    public List<EquipItem> getCarriedItems() { return carriedItems; }

    /**
     * Returns the vision trait for this NPC.
     * Never {@code null}; defaults to {@link VisionTrait#NONE}.
     */
    public VisionTrait getVisionTrait() { return visionTrait; }

    /**
     * Returns the vector-face configuration for this NPC, or {@code null} if
     * no face was generated (e.g. for NPCs created before this feature existed).
     *
     * <p>Pass the returned config to {@link eb.framework1.face.FaceSvgBuilder}
     * to produce an SVG image of the face.
     */
    public FaceConfig getFaceConfig() { return faceConfig; }

    /**
     * Returns the skin-tone category code assigned to this NPC (e.g.
     * {@code "fair_light"}), or {@code null} if no skin tone was assigned.
     */
    public String getSkinToneCode() { return skinToneCode; }

    // -------------------------------------------------------------------------
    // Convenience — current map position (derived from schedule)
    // -------------------------------------------------------------------------

    /**
     * Returns the city-map cell X-coordinate for this NPC at the given hour,
     * derived from the NPC's {@link NpcSchedule}.
     *
     * <p>Returns {@code -1} when no schedule is assigned, when no entry covers
     * {@code hour}, or when the matching entry has no known cell coordinate.
     *
     * @param hour hour of the day (0–23)
     * @return cell X coordinate, or {@code -1} if unknown
     */
    public int getCurrentCellX(int hour) {
        if (schedule == null) return -1;
        NpcScheduleEntry entry = schedule.getEntryForHour(hour);
        return (entry != null) ? entry.cellX : -1;
    }

    /**
     * Returns the city-map cell Y-coordinate for this NPC at the given hour,
     * derived from the NPC's {@link NpcSchedule}.
     *
     * <p>Returns {@code -1} when no schedule is assigned, when no entry covers
     * {@code hour}, or when the matching entry has no known cell coordinate.
     *
     * @param hour hour of the day (0–23)
     * @return cell Y coordinate, or {@code -1} if unknown
     */
    public int getCurrentCellY(int hour) {
        if (schedule == null) return -1;
        NpcScheduleEntry entry = schedule.getEntryForHour(hour);
        return (entry != null) ? entry.cellY : -1;
    }

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "NpcCharacter{id='" + id + "', name='" + fullName
                + "', gender='" + gender + "', age=" + age
                + ", occupation='" + occupation + "'}";
    }

    // -------------------------------------------------------------------------
    // Relationships
    // -------------------------------------------------------------------------

    /**
     * Adds a relationship entry, replacing any existing entry for the same
     * {@link Relationship#getTargetId() targetId}.
     *
     * @param relationship must not be {@code null}
     */
    public void addOrUpdateRelationship(Relationship relationship) {
        if (relationship == null) throw new IllegalArgumentException("relationship must not be null");
        for (int i = 0; i < relationships.size(); i++) {
            if (relationships.get(i).getTargetId().equals(relationship.getTargetId())) {
                relationships.set(i, relationship);
                return;
            }
        }
        relationships.add(relationship);
    }

    /**
     * Returns the relationship entry for the character with the given ID, or
     * {@code null} if no such entry exists.
     *
     * @param targetId the identifier to look up
     */
    public Relationship getRelationship(String targetId) {
        if (targetId == null) return null;
        for (Relationship r : relationships) {
            if (r.getTargetId().equals(targetId)) return r;
        }
        return null;
    }

    /**
     * Returns an unmodifiable view of all relationship entries held by this
     * NPC.
     */
    public List<Relationship> getRelationships() {
        return Collections.unmodifiableList(relationships);
    }

    // =========================================================================
    // Builder
    // =========================================================================

    /**
     * Fluent builder for {@link NpcCharacter}.
     *
     * <p>Only {@link #id(String)}, {@link #fullName(String)}, and
     * {@link #gender(String)} are mandatory.  All other fields default to
     * empty strings / zero / empty lists.
     */
    public static final class Builder {

        // mandatory
        private String id;
        private String fullName;
        private String gender;

        // optional — sensible defaults
        private int    age               = 0;
        private String occupation        = "";
        private String spriteKey         = "";
        private String physicalDescription = "";
        private String homeAddress       = "";
        private String workplaceAddress  = "";
        private final List<String> frequentLocations = new ArrayList<>();
        private String phoneNumber       = "";
        private String email             = "";
        private int    cooperativeness   = 5;
        private int    honesty           = 5;
        private int    nervousness       = 5;
        private PersonalityProfile personalityProfile = PersonalityProfile.DEFAULT;
        private final Map<CharacterAttribute, Integer> attributes =
                new EnumMap<>(CharacterAttribute.class);
        private final List<NpcSkill> skills = new ArrayList<>();
        private NpcSchedule schedule = null;
        private String      birthdate = "";
        private boolean     tracked   = false;
        private boolean     dead      = false;
        private long        deathDateTime = 0L;
        private int         deathTimeVarianceMinutes = 0;

        // Appearance attributes
        private String hairType    = "";
        private String hairColor   = "";
        private String beardStyle  = "";
        private int    wealthyLevel = 5;
        private String favColor    = "";
        private int    heightCm    = 0;
        private int    weightKg    = 0;

        // Carried items
        private final List<EquipItem> carriedItems = new ArrayList<>();

        // Vision trait
        private VisionTrait visionTrait = VisionTrait.NONE;

        // Face configuration
        private FaceConfig faceConfig   = null;
        private String     skinToneCode = null;

        // Hidden personality traits (−3 to +3)
        private final Map<PersonalityTrait, Integer> personalityTraits =
                new EnumMap<>(PersonalityTrait.class);

        /**
         * Sets the mandatory unique identifier.
         *
         * @param id must not be {@code null} or blank
         */
        public Builder id(String id) {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("NpcCharacter id must not be blank");
            }
            this.id = id.trim();
            return this;
        }

        /**
         * Sets the mandatory full name.
         *
         * @param fullName must not be {@code null} or blank
         */
        public Builder fullName(String fullName) {
            if (fullName == null || fullName.trim().isEmpty()) {
                throw new IllegalArgumentException("NpcCharacter fullName must not be blank");
            }
            this.fullName = fullName.trim();
            return this;
        }

        /**
         * Sets the mandatory gender tag ({@code "M"} or {@code "F"}).
         *
         * @param gender must not be {@code null} or blank
         */
        public Builder gender(String gender) {
            if (gender == null || gender.trim().isEmpty()) {
                throw new IllegalArgumentException("NpcCharacter gender must not be blank");
            }
            this.gender = gender.trim().toUpperCase();
            return this;
        }

        /** Sets the age in years. */
        public Builder age(int age) { this.age = age; return this; }

        /** Sets the occupation / job title. */
        public Builder occupation(String occupation) {
            this.occupation = occupation != null ? occupation : "";
            return this;
        }

        /** Sets the sprite key used to look up the character's visual asset. */
        public Builder spriteKey(String spriteKey) {
            this.spriteKey = spriteKey != null ? spriteKey : "";
            return this;
        }

        /** Sets the one-sentence physical description. */
        public Builder physicalDescription(String desc) {
            this.physicalDescription = desc != null ? desc : "";
            return this;
        }

        /** Sets the home address (building name on the city map). */
        public Builder homeAddress(String homeAddress) {
            this.homeAddress = homeAddress != null ? homeAddress : "";
            return this;
        }

        /** Sets the workplace address (building name on the city map). */
        public Builder workplaceAddress(String workplaceAddress) {
            this.workplaceAddress = workplaceAddress != null ? workplaceAddress : "";
            return this;
        }

        /** Adds a frequently-visited location. May be called multiple times. */
        public Builder addFrequentLocation(String location) {
            if (location != null && !location.trim().isEmpty()) {
                this.frequentLocations.add(location.trim());
            }
            return this;
        }

        /** Sets the formatted phone number. */
        public Builder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber != null ? phoneNumber : "";
            return this;
        }

        /** Sets the email address. */
        public Builder email(String email) {
            this.email = email != null ? email : "";
            return this;
        }

        /**
         * Sets the cooperativeness personality trait (1–10).
         *
         * @throws IllegalArgumentException if value is outside 1–10
         */
        public Builder cooperativeness(int value) {
            this.cooperativeness = checkRange("cooperativeness", value);
            return this;
        }

        /**
         * Sets the honesty personality trait (1–10).
         *
         * @throws IllegalArgumentException if value is outside 1–10
         */
        public Builder honesty(int value) {
            this.honesty = checkRange("honesty", value);
            return this;
        }

        /**
         * Sets the nervousness personality trait (1–10).
         *
         * @throws IllegalArgumentException if value is outside 1–10
         */
        public Builder nervousness(int value) {
            this.nervousness = checkRange("nervousness", value);
            return this;
        }

        /**
         * Sets the personality profile for this NPC.
         * The profile records which archetype was used to generate trait values
         * so that game systems can query it without re-examining individual
         * trait numbers.
         *
         * @param profile the profile to associate; {@code null} is replaced by
         *                {@link PersonalityProfile#DEFAULT}
         */
        public Builder personalityProfile(PersonalityProfile profile) {
            this.personalityProfile = profile != null ? profile : PersonalityProfile.DEFAULT;
            return this;
        }

        /**
         * Sets a single investigative attribute value (1–10).
         *
         * <p>Body-measurement attributes ({@link CharacterAttribute#isBodyMeasurement()})
         * and {@link CharacterAttribute#DETECTIVE_LEVEL} are silently ignored
         * because they are not part of the NPC's investigative-attribute set.
         *
         * @param attribute the attribute to set; must not be {@code null}
         * @param value     must be in the range 1–10
         * @throws IllegalArgumentException if {@code attribute} is {@code null}
         *         or {@code value} is outside 1–10
         */
        public Builder attribute(CharacterAttribute attribute, int value) {
            if (attribute == null) {
                throw new IllegalArgumentException("attribute must not be null");
            }
            if (attribute.isBodyMeasurement() || attribute.isDerivedAttribute()) {
                return this; // silently ignore non-investigative attributes
            }
            this.attributes.put(attribute, checkRange(attribute.name(), value));
            return this;
        }

        /**
         * Replaces the entire attribute map with the given values.
         * Entries that are body measurements or derived attributes are ignored.
         *
         * @param attrs map of attribute → value; {@code null} is treated as empty
         */
        public Builder attributes(Map<CharacterAttribute, Integer> attrs) {
            this.attributes.clear();
            if (attrs != null) {
                for (Map.Entry<CharacterAttribute, Integer> e : attrs.entrySet()) {
                    attribute(e.getKey(), e.getValue());
                }
            }
            return this;
        }

        /**
         * Replaces the entire list of frequently-visited locations.
         * Entries that are {@code null} or blank are silently ignored.
         *
         * @param locations list of location strings; {@code null} clears the list
         */
        public Builder frequentLocations(List<String> locations) {
            this.frequentLocations.clear();
            if (locations != null) {
                for (String loc : locations) {
                    addFrequentLocation(loc);
                }
            }
            return this;
        }

        /**
         * Adds a skill to the NPC's skill list.  {@code null} is silently ignored.
         *
         * @param skill the skill to add
         */
        public Builder addSkill(NpcSkill skill) {
            if (skill != null) this.skills.add(skill);
            return this;
        }

        /**
         * Replaces the entire skill list.  {@code null} values in the list are
         * silently ignored.
         *
         * @param skills the new skill list; {@code null} clears the list
         */
        public Builder skills(List<NpcSkill> skills) {
            this.skills.clear();
            if (skills != null) {
                for (NpcSkill s : skills) {
                    if (s != null) this.skills.add(s);
                }
            }
            return this;
        }

        /**
         * Sets the NPC's daily schedule.  {@code null} is permitted and means
         * the NPC has no schedule.
         *
         * @param schedule the schedule, or {@code null}
         */
        public Builder schedule(NpcSchedule schedule) {
            this.schedule = schedule;
            return this;
        }

        /**
         * Sets the NPC's date of birth as a {@code "YYYY-MM-DD"} string.
         * {@code null} or blank is stored as an empty string.
         *
         * @param birthdate date of birth string, or {@code null}
         */
        public Builder birthdate(String birthdate) {
            this.birthdate = (birthdate != null) ? birthdate.trim() : "";
            return this;
        }

        /**
         * Sets whether this NPC can be tracked / shown on the city map in
         * normal game mode.
         *
         * @param tracked {@code true} to enable tracking
         */
        public Builder tracked(boolean tracked) {
            this.tracked = tracked;
            return this;
        }

        /**
         * Marks this NPC as dead (e.g. a murder victim).
         *
         * @param dead {@code true} if the NPC is deceased
         */
        public Builder dead(boolean dead) {
            this.dead = dead;
            return this;
        }

        /**
         * Sets the estimated date and time of death as milliseconds since the
         * Unix epoch (in-game time).  Pass {@code 0L} for unknown or alive.
         *
         * @param deathDateTime epoch millis, or {@code 0L}
         */
        public Builder deathDateTime(long deathDateTime) {
            this.deathDateTime = deathDateTime;
            return this;
        }

        /**
         * Sets the error variance on the estimated time of death, in minutes.
         * <ul>
         *   <li>{@code 0} — known precisely</li>
         *   <li>{@code > 0} — ± this many minutes</li>
         *   <li>{@code -1} — completely unknown (body missing)</li>
         * </ul>
         *
         * @param minutes the variance in minutes
         */
        public Builder deathTimeVarianceMinutes(int minutes) {
            this.deathTimeVarianceMinutes = minutes;
            return this;
        }

        /** Sets the hair type (e.g. {@code "straight"}, {@code "curly"}, {@code "bald"}). */
        public Builder hairType(String hairType) {
            this.hairType = hairType != null ? hairType : "";
            return this;
        }

        /** Sets the hair colour (e.g. {@code "black"}, {@code "brown"}, {@code "blonde"}). */
        public Builder hairColor(String hairColor) {
            this.hairColor = hairColor != null ? hairColor : "";
            return this;
        }

        /**
         * Sets the beard/facial-hair style (e.g. {@code "short beard"}, {@code "stubble"}).
         * Pass an empty string or {@code null} for clean-shaven / female characters.
         */
        public Builder beardStyle(String beardStyle) {
            this.beardStyle = beardStyle != null ? beardStyle : "";
            return this;
        }

        /**
         * Sets the apparent wealth level (1–10).
         * Values outside this range are clamped.
         */
        public Builder wealthyLevel(int wealthyLevel) {
            this.wealthyLevel = Math.max(1, Math.min(10, wealthyLevel));
            return this;
        }

        /** Sets the favourite colour (e.g. {@code "red"}, {@code "blue"}; empty = none). */
        public Builder favColor(String favColor) {
            this.favColor = favColor != null ? favColor : "";
            return this;
        }

        /**
         * Sets the height in centimetres (e.g. {@code 175}).
         * Values ≤ 0 are stored as-is (means "not set").
         */
        public Builder heightCm(int heightCm) {
            this.heightCm = heightCm;
            return this;
        }

        /**
         * Sets the body weight in kilograms (e.g. {@code 75}).
         * Values ≤ 0 are stored as-is (means "not set").
         */
        public Builder weightKg(int weightKg) {
            this.weightKg = weightKg;
            return this;
        }

        /**
         * Adds a single item to the NPC's carried-items list.
         * {@code null} is silently ignored.
         *
         * @param item the item to carry
         */
        public Builder addCarriedItem(EquipItem item) {
            if (item != null) this.carriedItems.add(item);
            return this;
        }

        /**
         * Replaces the entire carried-items list.
         * {@code null} values in the list are silently ignored.
         *
         * @param items the new list; {@code null} clears the list
         */
        public Builder carriedItems(List<EquipItem> items) {
            this.carriedItems.clear();
            if (items != null) {
                for (EquipItem item : items) {
                    if (item != null) this.carriedItems.add(item);
                }
            }
            return this;
        }

        /**
         * Sets the vision trait.  {@code null} is treated as
         * {@link VisionTrait#NONE}.
         */
        public Builder visionTrait(VisionTrait visionTrait) {
            this.visionTrait = visionTrait != null ? visionTrait : VisionTrait.NONE;
            return this;
        }

        /**
         * Sets the vector-face configuration for this NPC.
         * {@code null} means no face has been generated (treated as absent).
         *
         * @param faceConfig the generated face, or {@code null}
         */
        public Builder faceConfig(FaceConfig faceConfig) {
            this.faceConfig = faceConfig;
            return this;
        }

        /**
         * Sets the skin-tone category code for this NPC.
         *
         * @param skinToneCode code matching a {@link SkinToneDefinition}, or {@code null}
         */
        public Builder skinToneCode(String skinToneCode) {
            this.skinToneCode = skinToneCode;
            return this;
        }

        /**
         * Sets a single hidden personality trait value (−3 to +3).
         * Values outside this range are clamped.
         *
         * @param trait the trait to set; must not be {@code null}
         * @param value the opinion value (−3 = strongly dislikes … +3 = strongly likes)
         * @throws IllegalArgumentException if {@code trait} is {@code null}
         */
        public Builder personalityTrait(PersonalityTrait trait, int value) {
            if (trait == null) {
                throw new IllegalArgumentException("trait must not be null");
            }
            this.personalityTraits.put(trait, PersonalityTrait.clamp(value));
            return this;
        }

        /**
         * Replaces the entire personality-trait map.
         * Entries with {@code null} keys are ignored; values are clamped to −3..+3.
         *
         * @param traits map of trait → value; {@code null} clears all traits
         */
        public Builder personalityTraits(Map<PersonalityTrait, Integer> traits) {
            this.personalityTraits.clear();
            if (traits != null) {
                for (Map.Entry<PersonalityTrait, Integer> e : traits.entrySet()) {
                    if (e.getKey() != null) {
                        personalityTrait(e.getKey(), e.getValue());
                    }
                }
            }
            return this;
        }

        /**
         * Builds the {@link NpcCharacter}.
         *
         * @throws IllegalStateException if mandatory fields ({@code id},
         *         {@code fullName}, {@code gender}) have not been set
         */
        public NpcCharacter build() {
            if (id == null)       throw new IllegalStateException("id is required");
            if (fullName == null) throw new IllegalStateException("fullName is required");
            if (gender == null)   throw new IllegalStateException("gender is required");
            return new NpcCharacter(this);
        }

        private static int checkRange(String field, int value) {
            if (value < 1 || value > 10) {
                throw new IllegalArgumentException(
                        field + " must be between 1 and 10 but was " + value);
            }
            return value;
        }
    }
}
