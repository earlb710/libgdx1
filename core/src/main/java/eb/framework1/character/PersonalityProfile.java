package eb.framework1.character;

/**
 * Describes the personality archetype of an {@link NpcCharacter}, expressed
 * as clamped ranges for the three personality traits: cooperativeness, honesty,
 * and nervousness (each normally 1–10).
 *
 * <p>When {@link CharacterGenerator} creates an NPC it uses the active profile's
 * ranges instead of the full 1–10 range to constrain the randomly generated
 * trait values.  The profile itself is stored on the resulting
 * {@link NpcCharacter} so that game systems can identify the archetype without
 * re-examining individual trait values.
 *
 * <h3>Profiles</h3>
 * <table>
 *   <caption>Personality profiles and their trait ranges</caption>
 *   <tr><th>Profile</th><th>Cooperativeness</th><th>Honesty</th><th>Nervousness</th></tr>
 *   <tr><td>{@link #DEFAULT}</td><td>1–10</td><td>1–10</td><td>1–10</td></tr>
 *   <tr><td>{@link #PSYCHOPATH}</td><td>5–9</td><td>1–2</td><td>1–2</td></tr>
 * </table>
 *
 * <p>The {@code PSYCHOPATH} profile reflects a character who is:
 * <ul>
 *   <li><strong>Very dishonest</strong> — honesty capped at 2; they lie
 *       effortlessly and statements require heavy verification.</li>
 *   <li><strong>Very confident</strong> — nervousness capped at 2; they show
 *       no visible anxiety even when guilty, giving the player few behavioural
 *       cues.</li>
 *   <li><strong>Superficially charming</strong> — cooperativeness in the
 *       5–9 range; they appear helpful and engage willingly with the detective,
 *       but their cooperation masks deception.</li>
 * </ul>
 */
public enum PersonalityProfile {

    /**
     * Standard personality profile.  All three traits are drawn uniformly
     * from the full 1–10 range.
     */
    DEFAULT(
            "Default",
            "Normal personality with no special trait skew",
            1, 10,   // cooperativeness range
            1, 10,   // honesty range
            1, 10    // nervousness range
    ),

    /**
     * Psychopath personality profile: very dishonest and very confident.
     *
     * <ul>
     *   <li>Honesty: 1–2 (compulsive liar; statements must be independently
     *       verified through other lead types)</li>
     *   <li>Nervousness: 1–2 (shows no outward anxiety even under direct
     *       questioning; hard to rattle)</li>
     *   <li>Cooperativeness: 5–9 (superficially charming and willing to
     *       engage; the helpfulness itself is a red herring)</li>
     * </ul>
     */
    PSYCHOPATH(
            "Psychopath",
            "Very dishonest and confident; superficially charming but completely unreliable",
            5, 9,    // cooperativeness range — superficially charming
            1, 2,    // honesty range — very dishonest
            1, 2     // nervousness range — very confident, no visible anxiety
    );

    // -------------------------------------------------------------------------

    private final String displayName;
    private final String description;

    private final int minCooperativeness;
    private final int maxCooperativeness;

    private final int minHonesty;
    private final int maxHonesty;

    private final int minNervousness;
    private final int maxNervousness;

    PersonalityProfile(String displayName, String description,
                       int minCooperativeness, int maxCooperativeness,
                       int minHonesty,        int maxHonesty,
                       int minNervousness,    int maxNervousness) {
        this.displayName          = displayName;
        this.description          = description;
        this.minCooperativeness   = minCooperativeness;
        this.maxCooperativeness   = maxCooperativeness;
        this.minHonesty           = minHonesty;
        this.maxHonesty           = maxHonesty;
        this.minNervousness       = minNervousness;
        this.maxNervousness       = maxNervousness;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** Human-readable name shown in UI or logs (e.g. {@code "Psychopath"}). */
    public String getDisplayName() { return displayName; }

    /** One-sentence description of the personality archetype. */
    public String getDescription() { return description; }

    /** Minimum value for the cooperativeness trait (inclusive, 1–10). */
    public int getMinCooperativeness() { return minCooperativeness; }

    /** Maximum value for the cooperativeness trait (inclusive, 1–10). */
    public int getMaxCooperativeness() { return maxCooperativeness; }

    /** Minimum value for the honesty trait (inclusive, 1–10). */
    public int getMinHonesty() { return minHonesty; }

    /** Maximum value for the honesty trait (inclusive, 1–10). */
    public int getMaxHonesty() { return maxHonesty; }

    /** Minimum value for the nervousness trait (inclusive, 1–10). */
    public int getMinNervousness() { return minNervousness; }

    /** Maximum value for the nervousness trait (inclusive, 1–10). */
    public int getMaxNervousness() { return maxNervousness; }
}
