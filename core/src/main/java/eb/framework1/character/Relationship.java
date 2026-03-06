package eb.framework1.character;

/**
 * Records one character's opinion of another character.
 *
 * <p>Each {@link Profile} and {@link NpcCharacter} holds an
 * {@code ArrayList<Relationship>}, one entry per character they have met.
 * The list is populated bilaterally by
 * {@link #recordMeeting(Profile, String, String, int, NpcCharacter)} so that both
 * participants get an entry the moment they first meet.
 *
 * <h3>Opinion scale</h3>
 * The {@code opinion} field is an uncapped integer that starts at
 * {@code (targetCharisma - 5) × 10} on first meeting:
 * <ul>
 *   <li>charisma 1 → −40  (very off-putting)</li>
 *   <li>charisma 5 →   0  (neutral)</li>
 *   <li>charisma 10 → +50  (very charming)</li>
 * </ul>
 * Positive values mean the holder <em>likes</em> the target; negative values
 * mean they <em>dislike</em> them.  Game systems can adjust the opinion over
 * time via {@link #adjustOpinion(int)}.
 */
public final class Relationship {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /**
     * The neutral charisma baseline.  Characters with exactly this charisma
     * produce an initial opinion of zero in whoever they meet.
     */
    public static final int NEUTRAL_CHARISMA = 5;

    /**
     * Opinion points added (or subtracted) for each charisma point above
     * (or below) {@link #NEUTRAL_CHARISMA}.
     */
    public static final int OPINION_PER_CHARISMA_POINT = 10;

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /** Identifier of the character this entry refers to. */
    private final String targetId;

    /** Display name of the character this entry refers to. */
    private final String targetName;

    /**
     * The holder's current opinion of the target.
     * Positive = favourable, negative = unfavourable.
     * Mutated by {@link #adjustOpinion(int)}.
     */
    private int opinion;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a relationship entry with an explicitly supplied opinion.
     *
     * @param targetId   identifier of the other character; must not be {@code null}
     * @param targetName display name; must not be {@code null}
     * @param opinion    initial opinion value (any integer)
     * @throws IllegalArgumentException if {@code targetId} or {@code targetName}
     *                                  is {@code null} or blank
     */
    public Relationship(String targetId, String targetName, int opinion) {
        if (targetId == null || targetId.trim().isEmpty()) {
            throw new IllegalArgumentException("targetId must not be blank");
        }
        if (targetName == null || targetName.trim().isEmpty()) {
            throw new IllegalArgumentException("targetName must not be blank");
        }
        this.targetId   = targetId.trim();
        this.targetName = targetName.trim();
        this.opinion    = opinion;
    }

    /**
     * Creates a first-meeting relationship entry whose initial opinion is
     * derived from the target character's charisma.
     *
     * <p>Formula: {@code opinion = (targetCharisma − NEUTRAL_CHARISMA) × OPINION_PER_CHARISMA_POINT}
     *
     * @param targetId        identifier of the other character
     * @param targetName      display name of the other character
     * @param targetCharisma  charisma of the other character (1–10 expected;
     *                        values outside this range produce proportionally
     *                        extreme opinions)
     * @return a new {@link Relationship} with charisma-derived initial opinion
     */
    public static Relationship forFirstMeeting(String targetId, String targetName,
                                               int targetCharisma) {
        int initialOpinion = (targetCharisma - NEUTRAL_CHARISMA) * OPINION_PER_CHARISMA_POINT;
        return new Relationship(targetId, targetName, initialOpinion);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** Returns the identifier of the character this entry refers to. */
    public String getTargetId() { return targetId; }

    /** Returns the display name of the character this entry refers to. */
    public String getTargetName() { return targetName; }

    /** Returns the current opinion value (positive = like, negative = dislike). */
    public int getOpinion() { return opinion; }

    /** Returns {@code true} if the current opinion is strictly positive. */
    public boolean isPositive() { return opinion > 0; }

    /** Returns {@code true} if the current opinion is strictly negative. */
    public boolean isNegative() { return opinion < 0; }

    // -------------------------------------------------------------------------
    // Mutation
    // -------------------------------------------------------------------------

    /**
     * Adjusts the opinion by {@code delta} points.
     * Use positive values to improve the relationship and negative values to
     * worsen it.
     *
     * @param delta the number of opinion points to add (may be negative)
     */
    public void adjustOpinion(int delta) {
        this.opinion += delta;
    }

    // -------------------------------------------------------------------------
    // Meeting helper
    // -------------------------------------------------------------------------

    /**
     * Records a first meeting between a player and a named character
     * (typically an NPC case contact) <em>bilaterally</em>: the player's
     * {@link Profile} receives an entry about the contact, and the contact's
     * {@link NpcCharacter} — if supplied — receives a matching entry about
     * the player.
     *
     * <p>If either character already has a relationship entry for the other,
     * it is left unchanged (the meeting was not truly a first meeting).
     *
     * @param player          the player's profile; must not be {@code null}
     * @param contactId       stable identifier for the contact (e.g. the NPC's
     *                        {@link NpcCharacter#getId()}, or their name if no
     *                        ID is available)
     * @param contactName     display name of the contact
     * @param contactCharisma charisma of the contact (1–10)
     * @param contactNpc      the contact's {@link NpcCharacter}, or {@code null}
     *                        if the NPC object is not available
     */
    public static void recordMeeting(Profile player,
                                     String contactId,
                                     String contactName,
                                     int contactCharisma,
                                     NpcCharacter contactNpc) {
        if (player == null) throw new IllegalArgumentException("player must not be null");
        if (contactId == null || contactId.trim().isEmpty()) {
            throw new IllegalArgumentException("contactId must not be blank");
        }
        if (contactName == null || contactName.trim().isEmpty()) {
            throw new IllegalArgumentException("contactName must not be blank");
        }

        // Player's opinion of the contact (based on contact's charisma)
        if (player.getRelationship(contactId) == null) {
            player.addOrUpdateRelationship(
                    Relationship.forFirstMeeting(contactId, contactName, contactCharisma));
        }

        // Contact's opinion of the player (based on player's charisma)
        if (contactNpc != null && contactNpc.getRelationship(player.getCharacterId()) == null) {
            int playerCharisma = player.getAttribute(CharacterAttribute.CHARISMA.name());
            contactNpc.addOrUpdateRelationship(
                    Relationship.forFirstMeeting(
                            player.getCharacterId(),
                            player.getCharacterName(),
                            playerCharisma));
        }
    }

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "Relationship{targetId='" + targetId + '\''
                + ", targetName='" + targetName + '\''
                + ", opinion=" + opinion + '}';
    }
}
