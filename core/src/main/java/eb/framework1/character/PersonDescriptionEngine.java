package eb.framework1.character;

/**
 * Generates a text description of an {@link NpcCharacter}'s observable appearance
 * as seen from a distance (the "examine from afar" feature).
 *
 * <p>The description is built from the appearance attributes added to
 * {@link NpcCharacter}: hair type and colour, apparent wealth level, and
 * favourite colour.  Only externally visible traits are included —
 * this class intentionally omits private information such as personality or
 * investigative attributes.
 *
 * <p>This class has <strong>no libGDX dependency</strong> and can be unit-tested
 * with plain JUnit.
 *
 * <h3>Example</h3>
 * <pre>
 *   String desc = PersonDescriptionEngine.describe(npc);
 *   // → "A middle-aged man with wavy brown hair.
 *   //    They dress in a modest style.
 *   //    They seem to favour the colour blue."
 * </pre>
 */
public final class PersonDescriptionEngine {

    // No instances — utility class.
    private PersonDescriptionEngine() {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns a multi-sentence visual description of {@code npc} as seen from afar.
     *
     * @param npc the character to describe; must not be {@code null}
     * @return a non-null, non-empty description string
     */
    public static String describe(NpcCharacter npc) {
        if (npc == null) throw new IllegalArgumentException("npc must not be null");

        StringBuilder sb = new StringBuilder();

        // ── Sentence 1: physical silhouette ──────────────────────────────────
        String ageTerm  = ageTerm(npc.getAge());
        String genderNoun = "F".equalsIgnoreCase(npc.getGender()) ? "woman" : "man";
        sb.append("A ").append(ageTerm).append(' ').append(genderNoun);

        String hairType  = npc.getHairType();
        String hairColor = npc.getHairColor();
        if (!hairType.isEmpty() || !hairColor.isEmpty()) {
            sb.append(" with ");
            if ("bald".equalsIgnoreCase(hairType)) {
                sb.append("a bald head");
            } else {
                if (!hairType.isEmpty())  sb.append(hairType).append(' ');
                if (!hairColor.isEmpty()) sb.append(hairColor).append(" hair");
                else if (!hairType.isEmpty()) sb.setLength(sb.length() - 1); // remove trailing space
            }
        }
        sb.append('.');

        // ── Sentence 2: apparent wealth ───────────────────────────────────────
        int w = npc.getWealthyLevel();
        sb.append(' ').append(wealthDesc(w)).append('.');

        // ── Sentence 3 (optional): favourite colour ───────────────────────────
        String fc = npc.getFavColor();
        if (fc != null && !fc.isEmpty()) {
            sb.append(" They seem to favour the colour ").append(fc).append('.');
        }

        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Maps an age in years to a qualitative descriptor. */
    static String ageTerm(int age) {
        if (age < 18)  return "young";
        if (age < 30)  return "young adult";
        if (age < 50)  return "middle-aged";
        if (age < 65)  return "older";
        return "elderly";
    }

    /** Maps a 1–10 wealth level to a clothing/appearance description sentence. */
    static String wealthDesc(int level) {
        if (level <= 2)  return "Their clothing looks worn and threadbare";
        if (level <= 4)  return "They dress in a modest, practical style";
        if (level <= 6)  return "They appear comfortably dressed";
        if (level <= 8)  return "Their attire looks polished and expensive";
        return "They are dressed in an ostentatiously wealthy manner";
    }
}
