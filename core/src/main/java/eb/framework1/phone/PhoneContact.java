package eb.framework1.phone;

import eb.framework1.investigation.*;
import eb.framework1.popup.*;


/**
 * Represents a single entry in the player's phone contact list.
 *
 * <p>Contacts are derived from open {@link CaseFile}s: each open case
 * contributes its {@link CaseFile#getClientName() client name} and
 * {@link CaseFile#getSubjectName() subject name}.  Contacts from open cases
 * are flagged with {@link #caseOpen} and displayed with a star (★) in the
 * {@link PhonePopup}.
 *
 * <p>When the associated case is closed or marked cold the contact no longer
 * appears in the phone list.
 *
 * <h3>Phone number discovery</h3>
 * <p>Each NPC has a phone number that must be <em>discovered</em> through
 * investigation (interviews, leads, or story-tree progression) before the
 * player can call them.  Until discovered, the number is hidden and the
 * contact row shows "???" instead of the number.  The client's number is
 * always known from the start.
 *
 * <h3>Location-based appointments</h3>
 * <p>Once the player discovers an NPC's phone number, they can call to
 * arrange a meeting at a specific {@link NpcLocation}.  Each NPC has a
 * {@link #defaultLocation} where they are normally found and may agree to
 * meet at a different location depending on the conversation tone.
 */
public class PhoneContact {

    /** Display name shown in the phone contact list. */
    public final String  name;
    /** ID of the {@link CaseFile} this contact belongs to. */
    public final String  caseId;
    /** {@code true} when the associated case is still {@link CaseFile.Status#OPEN}. */
    public final boolean caseOpen;
    /** {@code true} once the player has tapped this contact to place a call. */
    public boolean phoned;
    /**
     * Rating of the phone message placed to this contact.
     * {@code null} when the contact has not yet been called.
     * Set to {@link PhoneMessageRating#NEUTRAL} on the first call, then
     * the player can cycle through the values by tapping again.
     */
    public PhoneMessageRating rating;

    /**
     * The NPC's phone number (e.g. "555-0142").  Every NPC has a number
     * at generation time, but the player cannot use it until
     * {@link #phoneDiscovered} is {@code true}.
     */
    public final String phoneNumber;

    /**
     * Whether the player has discovered this contact's phone number.
     * Initially {@code false} for most NPCs; always {@code true} for the
     * client (the person who hires the detective).  Discovering a phone
     * number is a game-play milestone that unlocks the ability to call
     * the NPC and arrange location-based meetings.
     */
    public boolean phoneDiscovered;

    /**
     * The NPC's default location — where they can usually be found
     * without an appointment.  {@code null} if no location has been set.
     */
    public final String defaultLocation;

    /**
     * Creates a new phone contact with no call rating and no phone number.
     * Backwards-compatible constructor for legacy code.
     *
     * @param name     display name (non-null; treated as empty string if {@code null})
     * @param caseId   ID of the source case (non-null; treated as empty string if {@code null})
     * @param caseOpen {@code true} if the source case is still open
     * @param phoned   {@code true} if this contact has already been called
     */
    public PhoneContact(String name, String caseId, boolean caseOpen, boolean phoned) {
        this(name, caseId, caseOpen, phoned, null);
    }

    /**
     * Creates a new phone contact with a call rating but no phone number.
     * Backwards-compatible constructor for legacy code.
     *
     * @param name     display name (non-null; treated as empty string if {@code null})
     * @param caseId   ID of the source case (non-null; treated as empty string if {@code null})
     * @param caseOpen {@code true} if the source case is still open
     * @param phoned   {@code true} if this contact has already been called
     * @param rating   rating of the call, or {@code null} if not yet called
     */
    public PhoneContact(String name, String caseId, boolean caseOpen, boolean phoned,
                        PhoneMessageRating rating) {
        this(name, caseId, caseOpen, phoned, rating, "", false, "");
    }

    /**
     * Full constructor including phone number, discovery status, and default
     * location.
     *
     * @param name            display name
     * @param caseId          ID of the source case
     * @param caseOpen        {@code true} if the source case is still open
     * @param phoned          {@code true} if this contact has already been called
     * @param rating          rating of the call, or {@code null} if not yet called
     * @param phoneNumber     the NPC's phone number (e.g. "555-0142")
     * @param phoneDiscovered {@code true} if the player knows this number
     * @param defaultLocation the NPC's usual location name, or empty string
     */
    public PhoneContact(String name, String caseId, boolean caseOpen, boolean phoned,
                        PhoneMessageRating rating, String phoneNumber,
                        boolean phoneDiscovered, String defaultLocation) {
        this.name            = name            != null ? name            : "";
        this.caseId          = caseId          != null ? caseId          : "";
        this.caseOpen        = caseOpen;
        this.phoned          = phoned;
        this.rating          = rating;
        this.phoneNumber     = phoneNumber     != null ? phoneNumber     : "";
        this.phoneDiscovered = phoneDiscovered;
        this.defaultLocation = defaultLocation != null ? defaultLocation : "";
    }
}
