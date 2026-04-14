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
 * <p>A third section, {@code "caseSeeds"}, provides coherent bundles of
 * initial known facts and investigation leads for each case type (keyed by
 * uppercase type name, e.g. {@code "MISSING_PERSON"}).  When a case is
 * generated the selected seed&rsquo;s facts are appended to the description
 * and added as initial clues, and its leads are appended to the objective
 * and added as {@link CaseLead}s on the {@link CaseFile}.
 *
 * <p>Each pool is a list of template strings that may contain
 * {@code $placeholder} tokens resolved by {@link TemplateResolver}.  Supported
 * placeholders:
 * <ul>
 *   <li>{@code $client}     — client name</li>
 *   <li>{@code $subject}    — subject / suspect name</li>
 *   <li>{@code $victim}     — victim name (empty for non-murder cases)</li>
 *   <li>{@code $pronounCap} — {@code He} or {@code She} (capitalised)</li>
 *   <li>{@code $pron}       — {@code he} or {@code she} (lowercase)</li>
 * </ul>
 *
 * <p>{@link #parse(String)} accepts raw JSON text and uses the libGDX
 * {@code JsonReader} which does <em>not</em> require {@code Gdx} to be
 * initialised — it is a pure in-memory parser.
 */
public class CaseTemplateData {

    // -------------------------------------------------------------------------
    // Inner value classes for seed data
    // -------------------------------------------------------------------------

    /**
     * A single lead within a {@link CaseSeed}: a hint (visible from the
     * start), a full description (revealed on discovery), and a
     * {@link DiscoveryMethod} name.
     */
    public static final class SeedLead {
        private final String hint;
        private final String description;
        private final String method;

        public SeedLead(String hint, String description, String method) {
            this.hint        = hint;
            this.description = description;
            this.method      = method;
        }

        public String getHint()        { return hint; }
        public String getDescription() { return description; }
        public String getMethod()      { return method; }
    }

    /**
     * A coherent bundle of initial known facts and investigation leads
     * for one case instance.  All strings may contain {@code $placeholder}
     * tokens which are resolved at generation time.
     */
    public static final class CaseSeed {
        private final List<String>   facts;
        private final List<SeedLead> leads;

        public CaseSeed(List<String> facts, List<SeedLead> leads) {
            this.facts = Collections.unmodifiableList(facts);
            this.leads = Collections.unmodifiableList(leads);
        }

        /** Initial known facts (added as clues on the case file). */
        public List<String>   getFacts() { return facts; }
        /** Initial investigation leads (added as case leads). */
        public List<SeedLead> getLeads() { return leads; }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final Map<String, List<String>> descriptions;
    private final Map<String, List<String>> objectives;
    private final Map<String, List<CaseSeed>> caseSeeds;

    private CaseTemplateData(Map<String, List<String>> descriptions,
                              Map<String, List<String>> objectives,
                              Map<String, List<CaseSeed>> caseSeeds) {
        this.descriptions = descriptions;
        this.objectives   = objectives;
        this.caseSeeds    = caseSeeds;
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
     * Returns a random {@link CaseSeed} for the given case type, or
     * {@code null} if no seeds are defined for that type.
     *
     * @param caseTypeName uppercase case-type name, e.g. {@code "MURDER"}
     * @param rng          random source
     * @return a seed bundle, or {@code null}
     */
    public CaseSeed pickSeed(String caseTypeName, Random rng) {
        if (caseSeeds == null || caseTypeName == null) return null;
        List<CaseSeed> pool = caseSeeds.get(caseTypeName);
        if (pool == null || pool.isEmpty()) return null;
        return pool.get(rng.nextInt(pool.size()));
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
     *     ...
     *   },
     *   "objectives": {
     *     "MISSING_PERSON.1": ["...", ...],
     *     ...
     *   },
     *   "caseSeeds": {
     *     "MISSING_PERSON": [
     *       {
     *         "facts": ["fact 1", "fact 2"],
     *         "leads": [
     *           {"hint": "...", "description": "...", "method": "INTERVIEW"}
     *         ]
     *       },
     *       ...
     *     ],
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
        Map<String, List<CaseSeed>> seeds      = new HashMap<String, List<CaseSeed>>();

        JsonReader reader = new JsonReader();
        JsonValue  root   = reader.parse(json);

        parseSection(root, "descriptions", descriptions);
        parseSection(root, "objectives",   objectives);
        parseSeedsSection(root, seeds);

        return new CaseTemplateData(descriptions, objectives, seeds);
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

    /**
     * Parses the {@code "caseSeeds"} section of the JSON.  Each top-level key
     * is a case-type name (e.g. {@code "MURDER"}) mapping to an array of seed
     * objects.
     */
    private static void parseSeedsSection(JsonValue root,
                                           Map<String, List<CaseSeed>> target) {
        JsonValue section = root.get("caseSeeds");
        if (section == null) return;
        for (JsonValue typeEntry = section.child; typeEntry != null; typeEntry = typeEntry.next) {
            String key = typeEntry.name();
            List<CaseSeed> seedList = new ArrayList<CaseSeed>();
            for (JsonValue seedVal = typeEntry.child; seedVal != null; seedVal = seedVal.next) {
                // Parse facts array
                List<String> facts = new ArrayList<String>();
                JsonValue factsArr = seedVal.get("facts");
                if (factsArr != null) {
                    for (JsonValue f = factsArr.child; f != null; f = f.next) {
                        facts.add(f.asString());
                    }
                }
                // Parse leads array
                List<SeedLead> leads = new ArrayList<SeedLead>();
                JsonValue leadsArr = seedVal.get("leads");
                if (leadsArr != null) {
                    for (JsonValue l = leadsArr.child; l != null; l = l.next) {
                        String hint = l.has("hint") ? l.getString("hint") : "";
                        String desc = l.has("description") ? l.getString("description") : "";
                        String meth = l.has("method") ? l.getString("method") : "INTERVIEW";
                        leads.add(new SeedLead(hint, desc, meth));
                    }
                }
                seedList.add(new CaseSeed(facts, leads));
            }
            target.put(key, seedList);
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
