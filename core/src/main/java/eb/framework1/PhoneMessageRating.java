package eb.framework1;

/**
 * Rating assigned to a phone message (call) placed to a {@link PhoneContact}.
 *
 * <p>After the player phones a contact the interaction can be rated as one of
 * three tones.  The default rating for a freshly-placed call is
 * {@link #NEUTRAL}.  Tapping the contact row again in the {@link PhonePopup}
 * cycles through the three values in declaration order.
 */
public enum PhoneMessageRating {
    FRIENDLY,
    NEUTRAL,
    UNFRIENDLY;

    /**
     * Returns the next rating in the cycle: FRIENDLY → NEUTRAL → UNFRIENDLY → FRIENDLY.
     */
    public PhoneMessageRating next() {
        PhoneMessageRating[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
