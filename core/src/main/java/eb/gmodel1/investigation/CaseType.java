package eb.gmodel1.investigation;

/**
 * Broad categories of detective case that a client can bring to the player.
 *
 * <p>Each type carries a display name (shown in the UI), a short description
 * of the work involved, and a difficulty range ({@link #getMinDifficulty()} to
 * {@link #getMaxDifficulty()}, both 1–10) that reflects how simple or complex
 * a specific instance of that case type can be.  For example, a
 * {@link #MISSING_PERSON} case might be a straightforward runaway (level 1) or
 * a full kidnapping investigation (level 5).
 * The {@link CaseGenerator} uses the type to produce contextually appropriate
 * case descriptions, objectives, and hidden leads.
 */
public enum CaseType {

    // minDifficulty, maxDifficulty
    MISSING_PERSON(
            "Missing Person",
            "Locate a person who has disappeared and determine what happened to them",
            1, 5),

    INFIDELITY(
            "Infidelity",
            "Gather evidence of a partner's unfaithful behaviour",
            1, 4),

    THEFT(
            "Theft",
            "Identify who stole property and, if possible, recover it",
            1, 5),

    FRAUD(
            "Fraud",
            "Uncover deliberate financial deception or identity misrepresentation",
            4, 9),

    BLACKMAIL(
            "Blackmail",
            "Identify the source of a blackmail threat and neutralise it",
            3, 7),

    MURDER(
            "Murder",
            "Reinvestigate a suspicious death the authorities closed too quickly",
            5, 10),

    STALKING(
            "Stalking",
            "Identify and document a stalker threatening the client or their family",
            2, 6),

    CORPORATE_ESPIONAGE(
            "Corporate Espionage",
            "Uncover an internal information leak damaging a business",
            5, 10);

    private final String displayName;
    private final String description;
    private final int minDifficulty;
    private final int maxDifficulty;

    CaseType(String displayName, String description, int minDifficulty, int maxDifficulty) {
        this.displayName = displayName;
        this.description = description;
        this.minDifficulty = minDifficulty;
        this.maxDifficulty = maxDifficulty;
    }

    /** Short label shown in menus and case-file headers. */
    public String getDisplayName() { return displayName; }

    /** One-sentence explanation of what this type of case involves. */
    public String getDescription() { return description; }

    /** Minimum difficulty rating for this case type (1 = easiest). */
    public int getMinDifficulty() { return minDifficulty; }

    /** Maximum difficulty rating for this case type (10 = hardest). */
    public int getMaxDifficulty() { return maxDifficulty; }
}
