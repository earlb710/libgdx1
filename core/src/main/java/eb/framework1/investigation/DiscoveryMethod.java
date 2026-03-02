package eb.framework1.investigation;

/**
 * The technique a player must use to uncover a particular {@link CaseLead}.
 *
 * <p>Each method corresponds to a distinct in-game activity.  The
 * {@link CaseLead} carries both the discovery method and a plain-language
 * hint so the player knows what to do next.
 */
public enum DiscoveryMethod {

    INTERVIEW(
            "Interview",
            "Speak to a witness, suspect, or contact to obtain information"),

    SURVEILLANCE(
            "Surveillance",
            "Observe the subject at a location over a period of time"),

    FORENSICS(
            "Forensics",
            "Submit physical evidence to a laboratory for scientific analysis"),

    DOCUMENTS(
            "Documents",
            "Obtain and review financial records, contracts, or correspondence"),

    PHYSICAL_SEARCH(
            "Physical Search",
            "Search a building or outdoor location for hidden objects or signs"),

    BACKGROUND_CHECK(
            "Background Check",
            "Research the subject's history, associates, and prior activities");

    private final String displayName;
    private final String description;

    DiscoveryMethod(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /** Short label shown in the case-file lead panel. */
    public String getDisplayName() { return displayName; }

    /** One-sentence description of what the method involves. */
    public String getDescription() { return description; }
}
