package eb.framework1;

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
    // Object overrides
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "NpcCharacter{id='" + id + "', name='" + fullName
                + "', gender='" + gender + "', age=" + age
                + ", occupation='" + occupation + "'}";
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
