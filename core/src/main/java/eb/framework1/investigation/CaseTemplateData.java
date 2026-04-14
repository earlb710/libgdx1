package eb.framework1.investigation;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Holds all text pools used for generating case descriptions and objectives,
 * loaded from {@code assets/text/case_templates_en.json}.
 *
 * <p>Pool keys follow the naming convention {@code CASE_TYPE.complexity}, e.g.
 * {@code MISSING_PERSON.1}, {@code MURDER.3}.  There are two top-level
 * sections: {@code "descriptions"} and {@code "objectives"}.
 *
 * <p>Each pool is a list of template strings that may contain
 * {@code $placeholder} tokens resolved by {@link TemplateResolver}.  Supported
 * placeholders:
 * <ul>
 *   <li>{@code $client}     — client name</li>
 *   <li>{@code $subject}    — subject / suspect name</li>
 *   <li>{@code $victim}     — victim name (empty for non-murder cases)</li>
 *   <li>{@code $pronoun}    — {@code He} or {@code She} (capitalised)</li>
 *   <li>{@code $pronounCap} — same as {@code $pronoun} (alias)</li>
 *   <li>{@code $pron}       — {@code he} or {@code she} (lowercase)</li>
 * </ul>
 *
 * <p>{@link #parse(String)} accepts raw JSON text and uses the libGDX
 * {@code JsonReader} which does <em>not</em> require {@code Gdx} to be
 * initialised — it is a pure in-memory parser.
 */
public class CaseTemplateData {

    private final Map<String, List<String>> descriptions;
    private final Map<String, List<String>> objectives;

    private CaseTemplateData(Map<String, List<String>> descriptions,
                              Map<String, List<String>> objectives) {
        this.descriptions = descriptions;
        this.objectives   = objectives;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Returns a random description template for the given case type and
     * complexity.  If no templates are found for the exact complexity, falls
     * back to complexity 1.  If still empty, returns {@code null}.
     *
     * @param caseTypeName uppercase case-type name, e.g. {@code "MURDER"}
     * @param complexity   1, 2, or 3
     * @param rng          random source
     * @return a raw template string, or {@code null} if no pool exists
     */
    public String pickDescription(String caseTypeName, int complexity, Random rng) {
        return pickFromSection(descriptions, caseTypeName, complexity, rng);
    }

    /**
     * Returns a random objective template for the given case type and
     * complexity.  Fallback behaviour matches {@link #pickDescription}.
     *
     * @param caseTypeName uppercase case-type name, e.g. {@code "MURDER"}
     * @param complexity   1, 2, or 3
     * @param rng          random source
     * @return a raw template string, or {@code null} if no pool exists
     */
    public String pickObjective(String caseTypeName, int complexity, Random rng) {
        return pickFromSection(objectives, caseTypeName, complexity, rng);
    }

    /**
     * Returns the description pool for the given key (e.g.
     * {@code "MURDER.2"}), or an empty list if absent.
     */
    public List<String> getDescriptionPool(String key) {
        List<String> pool = descriptions.get(key);
        return pool != null ? Collections.unmodifiableList(pool)
                            : Collections.<String>emptyList();
    }

    /**
     * Returns the objective pool for the given key (e.g.
     * {@code "STALKING.1"}), or an empty list if absent.
     */
    public List<String> getObjectivePool(String key) {
        List<String> pool = objectives.get(key);
        return pool != null ? Collections.unmodifiableList(pool)
                            : Collections.<String>emptyList();
    }

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    /**
     * Parses a {@link CaseTemplateData} from raw JSON text.
     *
     * <p>Expected top-level structure:
     * <pre>
     * {
     *   "descriptions": {
     *     "MISSING_PERSON.1": ["...", ...],
     *     "MISSING_PERSON.2": ["...", ...],
     *     ...
     *   },
     *   "objectives": {
     *     "MISSING_PERSON.1": ["...", ...],
     *     ...
     *   }
     * }
     * </pre>
     *
     * @param json raw UTF-8 JSON text
     * @return parsed data instance
     * @throws RuntimeException if the JSON cannot be parsed
     */
    public static CaseTemplateData parse(String json) {
        Map<String, List<String>> descriptions = new HashMap<String, List<String>>();
        Map<String, List<String>> objectives   = new HashMap<String, List<String>>();

        JsonReader reader = new JsonReader();
        JsonValue  root   = reader.parse(json);

        parseSection(root, "descriptions", descriptions);
        parseSection(root, "objectives",   objectives);

        return new CaseTemplateData(descriptions, objectives);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static void parseSection(JsonValue root, String sectionName,
                                     Map<String, List<String>> target) {
        JsonValue section = root.get(sectionName);
        if (section == null) return;
        for (JsonValue entry = section.child; entry != null; entry = entry.next) {
            String key = entry.name();
            List<String> list = new ArrayList<String>();
            for (JsonValue item = entry.child; item != null; item = item.next) {
                list.add(item.asString());
            }
            target.put(key, list);
        }
    }

    private static String pickFromSection(Map<String, List<String>> section,
                                          String caseTypeName, int complexity,
                                          Random rng) {
        if (section == null || caseTypeName == null) return null;
        String key = caseTypeName + "." + complexity;
        List<String> pool = section.get(key);
        if (pool == null || pool.isEmpty()) {
            // Fallback to complexity 1
            pool = section.get(caseTypeName + ".1");
        }
        if (pool == null || pool.isEmpty()) return null;
        return pool.get(rng.nextInt(pool.size()));
    }
}
