package eb.framework1.character;

/**
 * Hidden personality traits / opinions that NPCs hold.  Each trait is stored
 * as an integer in the range <strong>−3 to +3</strong>:
 *
 * <ul>
 *   <li>{@code -3} — strongly dislikes / opposed</li>
 *   <li>{@code -2} — dislikes</li>
 *   <li>{@code -1} — slightly dislikes</li>
 *   <li>{@code  0} — neutral / indifferent</li>
 *   <li>{@code +1} — slightly likes</li>
 *   <li>{@code +2} — likes</li>
 *   <li>{@code +3} — strongly likes / passionate</li>
 * </ul>
 *
 * <p>Traits are <em>hidden</em> from the player by default and can only be
 * discovered through interviews (the {@code PERSONALITY}
 * {@link eb.framework1.investigation.InterviewTopic} topic).  Once discovered,
 * they contribute indirectly to the case — for example, knowing that a suspect
 * loves hiking narrows down locations they might visit, or knowing they
 * dislike the victim provides circumstantial evidence of motive.
 *
 * <h3>Categories</h3>
 * Traits are grouped into three broad categories for generation purposes:
 * <ul>
 *   <li><strong>Hobbies &amp; Interests</strong> — SPORTS, HIKING, COOKING,
 *       READING, MUSIC, ART, GAMBLING, TRAVEL</li>
 *   <li><strong>Social Behaviour</strong> — FLIRTING, SOCIALIZING, GOSSIP,
 *       SOLITUDE</li>
 *   <li><strong>Attitudes</strong> — RISK_TAKING, AUTHORITY, WEALTH,
 *       ANIMALS</li>
 * </ul>
 *
 * @see NpcCharacter#getPersonalityTraits()
 * @see NpcCharacter#getTraitValue(PersonalityTrait)
 */
public enum PersonalityTrait {

    // ---- Hobbies & Interests ----

    /** Interest in sports and athletic activities. */
    SPORTS("Sports", "interest in sports and athletic activities",
            Category.HOBBIES),

    /** Interest in hiking and outdoor activities. */
    HIKING("Hiking", "interest in hiking and the outdoors",
            Category.HOBBIES),

    /** Interest in cooking and food. */
    COOKING("Cooking", "interest in cooking and food",
            Category.HOBBIES),

    /** Interest in reading and literature. */
    READING("Reading", "interest in reading and literature",
            Category.HOBBIES),

    /** Interest in music (listening or playing). */
    MUSIC("Music", "interest in music",
            Category.HOBBIES),

    /** Interest in art, galleries, and creative expression. */
    ART("Art", "interest in art and creative expression",
            Category.HOBBIES),

    /** Interest in gambling and games of chance. */
    GAMBLING("Gambling", "interest in gambling and risk games",
            Category.HOBBIES),

    /** Interest in travelling and seeing new places. */
    TRAVEL("Travel", "interest in travelling",
            Category.HOBBIES),

    // ---- Social Behaviour ----

    /** Tendency to flirt or engage in romantic advances. */
    FLIRTING("Flirting", "tendency to flirt",
            Category.SOCIAL),

    /** Enjoyment of social gatherings and meeting new people. */
    SOCIALIZING("Socializing", "enjoyment of social gatherings",
            Category.SOCIAL),

    /** Tendency to gossip about others. */
    GOSSIP("Gossip", "tendency to gossip about others",
            Category.SOCIAL),

    /** Preference for being alone. */
    SOLITUDE("Solitude", "preference for being alone",
            Category.SOCIAL),

    // ---- Attitudes ----

    /** Willingness to take risks. */
    RISK_TAKING("Risk-Taking", "willingness to take risks",
            Category.ATTITUDES),

    /** Attitude toward authority figures and rules. */
    AUTHORITY("Authority", "respect for authority and rules",
            Category.ATTITUDES),

    /** Attitude toward money and material possessions. */
    WEALTH("Wealth", "interest in money and material possessions",
            Category.ATTITUDES),

    /** Attitude toward animals and pets. */
    ANIMALS("Animals", "fondness for animals",
            Category.ATTITUDES);

    // -------------------------------------------------------------------------

    /** Broad grouping of personality traits for generation purposes. */
    public enum Category {
        /** Hobbies and interests (sports, hiking, cooking, etc.). */
        HOBBIES("Hobbies & Interests"),
        /** Social behaviour traits (flirting, socializing, gossip). */
        SOCIAL("Social Behaviour"),
        /** General attitudes (risk-taking, authority, wealth). */
        ATTITUDES("Attitudes");

        private final String displayName;
        Category(String displayName) { this.displayName = displayName; }
        /** Human-readable label for this category. */
        public String getDisplayName() { return displayName; }
    }

    // -------------------------------------------------------------------------

    /** Minimum trait value (strongly dislikes / opposed). */
    public static final int MIN_VALUE = -3;
    /** Maximum trait value (strongly likes / passionate). */
    public static final int MAX_VALUE = 3;

    private final String displayName;
    private final String description;
    private final Category category;

    PersonalityTrait(String displayName, String description, Category category) {
        this.displayName = displayName;
        this.description = description;
        this.category    = category;
    }

    /** Short label shown in the UI (e.g. {@code "Sports"}). */
    public String getDisplayName() { return displayName; }

    /** One-phrase description of what this trait measures. */
    public String getDescription() { return description; }

    /** The broad category this trait belongs to. */
    public Category getCategory() { return category; }

    /**
     * Returns a human-readable label for the given value, e.g.
     * {@code "strongly likes"}, {@code "neutral"}, {@code "dislikes"}.
     *
     * @param value the trait value (−3 to +3)
     * @return descriptive label; values outside −3..+3 are clamped
     */
    public static String labelForValue(int value) {
        if (value <= -3) return "strongly dislikes";
        if (value == -2) return "dislikes";
        if (value == -1) return "slightly dislikes";
        if (value ==  0) return "neutral";
        if (value ==  1) return "slightly likes";
        if (value ==  2) return "likes";
        return "strongly likes";  // >= 3
    }

    /**
     * Convenience: returns a complete phrase like "strongly likes sports".
     *
     * @param value the trait value (−3 to +3)
     * @return e.g. {@code "strongly likes sports"} or {@code "dislikes cooking"}
     */
    public String describe(int value) {
        return labelForValue(value) + " " + displayName.toLowerCase();
    }

    /**
     * Clamps the given value to the valid range [{@value #MIN_VALUE},
     * {@value #MAX_VALUE}].
     *
     * @param value the raw value
     * @return the clamped value
     */
    public static int clamp(int value) {
        return Math.max(MIN_VALUE, Math.min(MAX_VALUE, value));
    }
}
