package eb.framework1.character;

/**
 * Describes a character's innate vision impairment, if any.
 *
 * <p>Both {@link #FARSIGHTED} and {@link #NEARSIGHTED} reduce
 * {@link CharacterAttribute#PERCEPTION} by 1.  Equipping
 * {@link EquipItem#GLASSES} (which grants +1 PERCEPTION) fully compensates.
 *
 * <p>Approximately 30&nbsp;% of the world population is assigned one of the
 * impaired traits by {@link NpcGenerator} and {@link CharacterGenerator}.
 */
public enum VisionTrait {

    /** No vision impairment — no attribute modifier. */
    NONE("None", 0),

    /** Can see clearly at a distance but struggles up close — −1 Perception. */
    FARSIGHTED("Farsighted", -1),

    /** Can see clearly up close but struggles at a distance — −1 Perception. */
    NEARSIGHTED("Nearsighted", -1);

    private final String displayName;
    private final int    perceptionModifier;

    VisionTrait(String displayName, int perceptionModifier) {
        this.displayName        = displayName;
        this.perceptionModifier = perceptionModifier;
    }

    /** Human-readable label shown in the UI. */
    public String getDisplayName() { return displayName; }

    /**
     * Returns the modifier this trait applies to the given attribute.
     * Currently only {@link CharacterAttribute#PERCEPTION} is affected.
     *
     * @param attr the attribute to query
     * @return the integer modifier (0 if not applicable to this attribute)
     */
    public int getModifier(CharacterAttribute attr) {
        if (attr == CharacterAttribute.PERCEPTION) return perceptionModifier;
        return 0;
    }

    /**
     * Returns {@code true} if this trait imposes a perception penalty
     * (i.e. the character is not {@link #NONE}).
     */
    public boolean isImpaired() {
        return this != NONE;
    }
}
