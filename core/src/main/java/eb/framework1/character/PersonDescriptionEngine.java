package eb.framework1.character;

import java.util.List;
/**
 * Generates a text description of an {@link NpcCharacter}'s observable appearance
 * as seen from a distance (the "examine from afar" feature).
 *
 * <p>The description is built from the appearance attributes on
 * {@link NpcCharacter}: hair type and colour, apparent wealth level,
 * height, weight/build, and favourite colour.  Only externally visible
 * traits are included — this class intentionally omits private information
 * such as personality or investigative attributes.
 *
 * <p>This class has <strong>no libGDX dependency</strong> and can be unit-tested
 * with plain JUnit.
 *
 * <h3>Example</h3>
 * <pre>
 *   String desc = PersonDescriptionEngine.describe(npc);
 *   // → "A middle-aged man with wavy brown hair.
 *   //    They are tall and of average build.
 *   //    They appear comfortably dressed.
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
        String ageTerm    = ageTerm(npc.getAge());
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

        // ── Sentence 1b (optional): beard ────────────────────────────────────
        String beardStyle = npc.getBeardStyle();
        if (!beardStyle.isEmpty()) {
            sb.append(" They have ").append(beardStylePhrase(beardStyle)).append('.');
        }

        // ── Sentence 2 (optional): height and build ───────────────────────────
        int h = npc.getHeightCm();
        int w = npc.getWeightKg();
        if (h > 0) {
            String heightWord = heightDesc(h);
            if (w > 0) {
                String buildWord = buildDesc(h, w);
                sb.append(" They are ").append(heightWord)
                  .append(" and of ").append(buildWord).append(" build.");
            } else {
                sb.append(" They are ").append(heightWord).append('.');
            }
        }

        // ── Sentence 3: apparent wealth ───────────────────────────────────────
        int wealth = npc.getWealthyLevel();
        sb.append(' ').append(wealthDesc(wealth)).append('.');

        // ── Sentence 4 (optional): favourite colour ───────────────────────────
        String fc = npc.getFavColor();
        if (fc != null && !fc.isEmpty()) {
            sb.append(" They seem to favour the colour ").append(fc).append('.');
        }

        // ── Sentence 5 (optional): glasses / sun glasses (worn, not "carried") ─
        // ── Sentence 6 (optional): other visible carried items ─────────────────
        List<EquipItem> items = npc.getCarriedItems();
        boolean hasGlasses    = false;
        boolean hasSunGlasses = false;
        java.util.List<EquipItem> otherItems = new java.util.ArrayList<>();
        for (EquipItem item : items) {
            if (item == EquipItem.GLASSES)     hasGlasses    = true;
            else if (item == EquipItem.SUN_GLASSES) hasSunGlasses = true;
            else otherItems.add(item);
        }
        if (hasGlasses && hasSunGlasses) {
            sb.append(" They wear glasses and sun glasses.");
        } else if (hasSunGlasses) {
            sb.append(" They wear sun glasses.");
        } else if (hasGlasses) {
            sb.append(" They wear glasses.");
        }
        if (!otherItems.isEmpty()) {
            if (otherItems.size() == 1) {
                sb.append(" They appear to be carrying ")
                  .append(article(otherItems.get(0).getName())).append(' ')
                  .append(otherItems.get(0).getName().toLowerCase()).append('.');
            } else {
                sb.append(" They appear to be carrying ");
                for (int i = 0; i < otherItems.size(); i++) {
                    if (i > 0) sb.append(i == otherItems.size() - 1 ? " and " : ", ");
                    String name = otherItems.get(i).getName();
                    sb.append(article(name)).append(' ').append(name.toLowerCase());
                }
                sb.append('.');
            }
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

    /**
     * Returns the indefinite article ({@code "a"} or {@code "an"}) appropriate
     * for the given item name.  Matches on the first letter of the name,
     * case-insensitively.
     */
    static String article(String name) {
        if (name == null || name.isEmpty()) return "a";
        char first = Character.toLowerCase(name.charAt(0));
        return (first == 'a' || first == 'e' || first == 'i'
                || first == 'o' || first == 'u') ? "an" : "a";
    }

    /**
     * Maps a height in cm to a qualitative descriptor.
     * Thresholds are gender-agnostic; the framing ("tall", "short", etc.)
     * works for both males and females across typical adult ranges.
     */
    static String heightDesc(int heightCm) {
        if (heightCm < 160) return "short";
        if (heightCm < 170) return "below average height";
        if (heightCm < 180) return "average height";
        if (heightCm < 190) return "tall";
        return "very tall";
    }

    /**
     * Derives a qualitative build description from height and weight using BMI.
     *
     * <p>BMI = weight(kg) / height(m)²
     * <ul>
     *   <li>&lt; 18.5 → slim</li>
     *   <li>18.5–24.9 → average</li>
     *   <li>25–29.9 → stocky</li>
     *   <li>≥ 30    → heavy</li>
     * </ul>
     */
    static String buildDesc(int heightCm, int weightKg) {
        if (heightCm <= 0) return "average";
        float heightM = heightCm / 100f;
        float bmi     = weightKg / (heightM * heightM);
        if (bmi < 18.5f) return "slim";
        if (bmi < 25f)   return "average";
        if (bmi < 30f)   return "stocky";
        return "heavy";
    }

    /** Maps a 1–10 wealth level to a clothing/appearance description sentence. */
    static String wealthDesc(int level) {
        if (level <= 2)  return "Their clothing looks worn and threadbare";
        if (level <= 4)  return "They dress in a modest, practical style";
        if (level <= 6)  return "They appear comfortably dressed";
        if (level <= 8)  return "Their attire looks polished and expensive";
        return "They are dressed in an ostentatiously wealthy manner";
    }

    /**
     * Returns the natural-language phrase used in a beard sentence for the given
     * {@code beardStyle} value.
     *
     * <ul>
     *   <li>{@code "short beard"} → {@code "a short beard"}</li>
     *   <li>{@code "long beard"}  → {@code "a long beard"}</li>
     *   <li>{@code "stubble"}     → {@code "stubble"}</li>
     *   <li>anything else        → the raw style string</li>
     * </ul>
     */
    static String beardStylePhrase(String beardStyle) {
        switch (beardStyle) {
            case "short beard": return "a short beard";
            case "long beard":  return "a long beard";
            case "stubble":     return "stubble";
            default:            return beardStyle;
        }
    }
}
