package eb.framework1.investigation;

/**
 * Classifies story-tree action titles into one of four high-level categories
 * based on their leading keyword.
 *
 * <p>Every {@code ACTION} node in the story tree starts with one of a small set
 * of verbs.  This enum maps those verbs to the corresponding action category
 * and provides the associated attribute pools, eliminating the need to repeat
 * the keyword checks throughout the codebase.
 *
 * <h3>Categories and keywords</h3>
 * <table>
 *   <tr><th>Category</th>       <th>Keywords</th></tr>
 *   <tr><td>INTERVIEW</td>      <td>Interview, Speak, Question</td></tr>
 *   <tr><td>EVIDENCE</td>       <td>Collect, Bag, Recover</td></tr>
 *   <tr><td>DOCUMENT</td>       <td>Review, Analyse, Search</td></tr>
 *   <tr><td>PHOTOGRAPH</td>     <td>Photograph, Sketch, Map entry</td></tr>
 * </table>
 *
 * <h3>Example</h3>
 * <pre>
 *   ActionType type = ActionType.classify("Interview the witness");
 *   if (type == ActionType.INTERVIEW) { ... }
 *   String[] attrs = type.getAttributes();   // CHARISMA, EMPATHY, INTIMIDATION
 * </pre>
 */
public enum ActionType {

    /** Conversation-based actions: {@code Interview}, {@code Speak}, {@code Question}. */
    INTERVIEW(new String[]{"Interview", "Speak", "Question"},
              new String[]{"CHARISMA", "EMPATHY", "INTIMIDATION"}),

    /** Physical evidence collection: {@code Collect}, {@code Bag}, {@code Recover}. */
    EVIDENCE(new String[]{"Collect", "Bag", "Recover"},
             new String[]{"PERCEPTION", "INTELLIGENCE", "STEALTH"}),

    /** Document/records analysis: {@code Review}, {@code Analyse}, {@code Search}. */
    DOCUMENT(new String[]{"Review", "Analyse", "Search"},
             new String[]{"INTELLIGENCE", "MEMORY", "PERCEPTION"}),

    /** Visual recording: {@code Photograph}, {@code Sketch}, {@code Map entry}. */
    PHOTOGRAPH(new String[]{"Photograph", "Sketch", "Map entry"},
               new String[]{"PERCEPTION", "MEMORY", "STEALTH"});

    /** All seven player attributes — returned when no specific category matches. */
    private static final String[] ALL_ATTRIBUTES = {
        "PERCEPTION", "INTELLIGENCE", "CHARISMA",
        "INTIMIDATION", "EMPATHY", "MEMORY", "STEALTH"
    };

    private final String[] keywords;
    private final String[] attributes;

    ActionType(String[] keywords, String[] attributes) {
        this.keywords   = keywords;
        this.attributes = attributes;
    }

    /**
     * Returns the action-type category for the given action title, or
     * {@code null} if the title does not start with any recognised keyword.
     *
     * @param actionTitle the full action title (e.g.&nbsp;{@code "Interview the witness"});
     *                    may be {@code null}
     * @return the matching {@link ActionType}, or {@code null}
     */
    public static ActionType classify(String actionTitle) {
        if (actionTitle == null) return null;
        for (ActionType type : values()) {
            for (String keyword : type.keywords) {
                if (actionTitle.startsWith(keyword)) return type;
            }
        }
        return null;
    }

    /**
     * Returns the attribute pool for the given action title.
     *
     * <p>If the title does not match any category, all seven attributes are
     * returned.
     *
     * @param actionTitle the full action title; may be {@code null}
     * @return attribute names appropriate for the action
     */
    public static String[] attributesFor(String actionTitle) {
        ActionType type = classify(actionTitle);
        return type != null ? type.attributes.clone() : ALL_ATTRIBUTES.clone();
    }

    /**
     * Returns the attribute pool associated with this action category.
     * The returned array is a defensive copy.
     */
    public String[] getAttributes() {
        return attributes.clone();
    }

    /**
     * Returns the fact category string that corresponds to this action type.
     * <ul>
     *   <li>{@code EVIDENCE} and {@code PHOTOGRAPH} → {@code "EVIDENCE"}</li>
     *   <li>{@code INTERVIEW} → {@code "RELATIONSHIP"}</li>
     *   <li>{@code DOCUMENT} → {@code "ITEM"}</li>
     * </ul>
     */
    public String getFactCategory() {
        switch (this) {
            case EVIDENCE:
            case PHOTOGRAPH:
                return "EVIDENCE";
            case INTERVIEW:
                return "RELATIONSHIP";
            case DOCUMENT:
            default:
                return "ITEM";
        }
    }
}
