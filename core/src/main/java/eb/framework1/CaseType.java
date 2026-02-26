package eb.framework1;

/**
 * Broad categories of detective case that a client can bring to the player.
 *
 * <p>Each type carries a display name (shown in the UI), a short description
 * of the work involved, and a difficulty level from 1 (easiest) to 10 (hardest).
 * The {@link CaseGenerator} uses the type to produce contextually appropriate
 * case descriptions, objectives, and hidden leads.
 */
public enum CaseType {

    MISSING_PERSON(
            "Missing Person",
            "Locate a person who has disappeared and determine what happened to them",
            5),

    INFIDELITY(
            "Infidelity",
            "Gather evidence of a partner's unfaithful behaviour",
            3),

    THEFT(
            "Theft",
            "Identify who stole property and, if possible, recover it",
            3),

    FRAUD(
            "Fraud",
            "Uncover deliberate financial deception or identity misrepresentation",
            7),

    BLACKMAIL(
            "Blackmail",
            "Identify the source of a blackmail threat and neutralise it",
            6),

    MURDER(
            "Murder",
            "Reinvestigate a suspicious death the authorities closed too quickly",
            9),

    STALKING(
            "Stalking",
            "Identify and document a stalker threatening the client or their family",
            5),

    CORPORATE_ESPIONAGE(
            "Corporate Espionage",
            "Uncover an internal information leak damaging a business",
            8);

    private final String displayName;
    private final String description;
    private final int difficultyLevel;

    CaseType(String displayName, String description, int difficultyLevel) {
        this.displayName = displayName;
        this.description = description;
        this.difficultyLevel = difficultyLevel;
    }

    /** Short label shown in menus and case-file headers. */
    public String getDisplayName() { return displayName; }

    /** One-sentence explanation of what this type of case involves. */
    public String getDescription() { return description; }

    /** Difficulty rating from 1 (easiest) to 10 (hardest). */
    public int getDifficultyLevel() { return difficultyLevel; }
}
