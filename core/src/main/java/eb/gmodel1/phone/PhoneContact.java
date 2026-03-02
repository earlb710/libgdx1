package eb.gmodel1.phone;

import eb.gmodel1.investigation.*;
import eb.gmodel1.popup.*;


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
     * Creates a new phone contact with no call rating.
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
     * Creates a new phone contact with a call rating.
     *
     * @param name     display name (non-null; treated as empty string if {@code null})
     * @param caseId   ID of the source case (non-null; treated as empty string if {@code null})
     * @param caseOpen {@code true} if the source case is still open
     * @param phoned   {@code true} if this contact has already been called
     * @param rating   rating of the call, or {@code null} if not yet called
     */
    public PhoneContact(String name, String caseId, boolean caseOpen, boolean phoned,
                        PhoneMessageRating rating) {
        this.name     = name   != null ? name   : "";
        this.caseId   = caseId != null ? caseId : "";
        this.caseOpen = caseOpen;
        this.phoned   = phoned;
        this.rating   = rating;
    }
}
