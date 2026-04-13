package eb.framework1.investigation;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Resolves {@code $placeholder} tokens in interview template strings loaded
 * from {@code assets/text/interview_templates_en.json}.
 *
 * <h3>Named placeholders</h3>
 * <ul>
 *   <li>{@code $client}       — client NPC name</li>
 *   <li>{@code $subject}      — subject / suspect NPC name</li>
 *   <li>{@code $victim}       — victim name (empty for non-murder cases)</li>
 *   <li>{@code $targetPerson} — victim for murder cases, subject or client otherwise</li>
 *   <li>{@code $pronoun}      — {@code he} or {@code she} for the current NPC's gender</li>
 *   <li>{@code $pronounCap}   — {@code He} or {@code She} (capitalised)</li>
 *   <li>{@code $otherName}    — dynamic NPC name used in cross-NPC helper methods</li>
 *   <li>{@code $phone}        — phone number string</li>
 *   <li>{@code $location}     — location name string</li>
 *   <li>{@code $associateName}— the associate NPC's own name</li>
 * </ul>
 *
 * <h3>Word-pool placeholders</h3>
 * Each occurrence is resolved <em>independently</em> (a fresh random pick
 * per token), so a template containing {@code $hobby} twice will produce two
 * different hobbies.
 * <ul>
 *   <li>{@code $hobby}        — random pick from {@code words.hobby}</li>
 *   <li>{@code $social}       — random pick from {@code words.social}</li>
 *   <li>{@code $likeDislike}  — random pick from {@code words.likeDislike}</li>
 *   <li>{@code $locationClue} — random pick from {@code words.locationClue}</li>
 * </ul>
 */
public final class TemplateResolver {

    private TemplateResolver() {}

    /**
     * Resolves all placeholders in {@code template}.
     *
     * @param template raw template string with {@code $token} tokens;
     *                 {@code null} is treated as an empty string
     * @param vars     map of placeholder name → value (e.g. {@code "client" → "Alice"});
     *                 may be {@code null}
     * @param data     pool data for word-pool tokens; may be {@code null}
     *                 (word tokens are left unresolved if data is absent)
     * @param random   random source for word-pool tokens; may be {@code null}
     * @return resolved string; never {@code null}
     */
    public static String resolve(String template,
                                 Map<String, String> vars,
                                 InterviewTemplateData data,
                                 Random random) {
        if (template == null) return "";

        String result = template;

        // Word-pool tokens — each occurrence picks independently
        if (data != null && random != null) {
            result = resolveWordPool(result, "$hobby",        "words.hobby",        data, random);
            result = resolveWordPool(result, "$social",       "words.social",       data, random);
            result = resolveWordPool(result, "$likeDislike",  "words.likeDislike",  data, random);
            result = resolveWordPool(result, "$locationClue", "words.locationClue", data, random);
        }

        // Named placeholders — fixed substitution
        if (vars != null) {
            for (Map.Entry<String, String> e : vars.entrySet()) {
                String value = e.getValue() != null ? e.getValue() : "";
                result = result.replace("$" + e.getKey(), value);
            }
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Replaces every occurrence of {@code placeholder} in {@code text} with an
     * independent random pick from the named pool.
     */
    private static String resolveWordPool(String text,
                                          String placeholder,
                                          String poolKey,
                                          InterviewTemplateData data,
                                          Random random) {
        if (!text.contains(placeholder)) return text;

        List<String> pool = data.getPool(poolKey);
        if (pool.isEmpty()) return text;

        StringBuilder sb  = new StringBuilder(text.length());
        int           idx = 0;
        while (true) {
            int pos = text.indexOf(placeholder, idx);
            if (pos < 0) {
                sb.append(text, idx, text.length());
                break;
            }
            sb.append(text, idx, pos);
            sb.append(pool.get(random.nextInt(pool.size())));
            idx = pos + placeholder.length();
        }
        return sb.toString();
    }
}
