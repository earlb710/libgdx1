package eb.framework1.investigation;

/**
 * The category of question that can be asked during an NPC interview.
 *
 * <p>Each topic targets a different aspect of the investigation and helps
 * the player build a profile of suspects, verify alibis, and establish
 * relationships between characters.
 *
 * <p>Responses generated for each topic are influenced by the NPC's
 * {@link eb.framework1.character.NpcCharacter#getCooperativeness()
 * cooperativeness} and
 * {@link eb.framework1.character.NpcCharacter#getHonesty() honesty} traits.
 */
public enum InterviewTopic {

    /**
     * "Where were you on the night of [date]?"
     *
     * <p>The NPC gives their account of their whereabouts at the time of the
     * crime.  Low-honesty NPCs may fabricate alibis; high-honesty NPCs give
     * verifiable accounts.
     */
    ALIBI(
            "Alibi",
            "Ask the character where they were at the time of the crime"),

    /**
     * "Where was [other person] at the time?"
     *
     * <p>The NPC states where they believe another character was during the
     * critical time window.  Useful for cross-referencing alibis.
     */
    WHEREABOUTS(
            "Whereabouts",
            "Ask what they know about another character's location during the crime"),

    /**
     * "What do you think of [other person]?"
     *
     * <p>The NPC shares their opinion of another character.  This reveals
     * personality traits like jealousy, rivalry, or trust, helping the player
     * build a profile of suspects and their motivations.
     */
    OPINION(
            "Opinion",
            "Ask what they think of another character — reveals traits like jealousy or rivalry"),

    /**
     * "How do you know [other person]?"
     *
     * <p>The NPC describes their relationship with another character.  This
     * can expose hidden connections, past conflicts, or secret alliances.
     */
    RELATIONSHIP(
            "Relationship",
            "Ask about their relationship with another character"),

    /**
     * "When did you last see [victim/subject]?"
     *
     * <p>The NPC provides the date and circumstances of their last contact
     * with the victim or subject.  Discrepancies between different NPCs'
     * accounts may reveal deception or hidden information.
     */
    LAST_CONTACT(
            "Last Contact",
            "Ask when they last saw the victim or subject"),

    /**
     * "Did you notice anything unusual recently?"
     *
     * <p>An open-ended question that can surface unexpected observations.
     * The NPC may describe suspicious behaviour, strange visitors, or
     * changes in routine that provide investigative leads.
     */
    OBSERVATION(
            "Observation",
            "Ask if they noticed anything unusual around the time of the crime"),

    /**
     * "Can you think of anyone who would want to harm [victim]?"
     *
     * <p>The NPC identifies potential motives or suspects.  Depending on
     * their relationship with the subject, they may point blame, deflect
     * suspicion, or reveal grudges and jealousy among the cast.
     */
    MOTIVE(
            "Motive",
            "Ask whether they know of anyone with a reason to harm the victim");

    private final String displayName;
    private final String description;

    InterviewTopic(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /** Short label shown in the interview UI (e.g. {@code "Alibi"}). */
    public String getDisplayName() { return displayName; }

    /** One-sentence description of what this question type covers. */
    public String getDescription() { return description; }
}
