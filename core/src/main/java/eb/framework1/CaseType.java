package eb.framework1;

/**
 * Broad categories of detective case that a client can bring to the player.
 *
 * <p>Each type carries a display name (shown in the UI) and a short description
 * of the work involved.  The {@link CaseGenerator} uses the type to produce
 * contextually appropriate case descriptions, objectives, and hidden leads.
 */
public enum CaseType {

    MISSING_PERSON(
            "Missing Person",
            "Locate a person who has disappeared and determine what happened to them"),

    INFIDELITY(
            "Infidelity",
            "Gather evidence of a partner's unfaithful behaviour"),

    THEFT(
            "Theft",
            "Identify who stole property and, if possible, recover it"),

    FRAUD(
            "Fraud",
            "Uncover deliberate financial deception or identity misrepresentation"),

    BLACKMAIL(
            "Blackmail",
            "Identify the source of a blackmail threat and neutralise it"),

    MURDER(
            "Murder",
            "Reinvestigate a suspicious death the authorities closed too quickly"),

    STALKING(
            "Stalking",
            "Identify and document a stalker threatening the client or their family"),

    CORPORATE_ESPIONAGE(
            "Corporate Espionage",
            "Uncover an internal information leak damaging a business");

    private final String displayName;
    private final String description;

    CaseType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /** Short label shown in menus and case-file headers. */
    public String getDisplayName() { return displayName; }

    /** One-sentence explanation of what this type of case involves. */
    public String getDescription() { return description; }
}
