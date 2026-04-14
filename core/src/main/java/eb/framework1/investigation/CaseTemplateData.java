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
     * An initial contact within a {@link CaseSeed}: a person the
     * investigator should approach early in the case (name, role, and
     * a brief reason why they are relevant).  All strings may contain
     * {@code $placeholder} tokens.
     */
    public static final class SeedContact {
        private final String name;
        private final String role;
        private final String reason;

        public SeedContact(String name, String role, String reason) {
            this.name   = name;
            this.role   = role;
            this.reason = reason;
        }

        /** Display name of the contact (may contain $placeholders). */
        public String getName()   { return name; }
        /** Role or relationship to the case (e.g. "Client's neighbour"). */
        public String getRole()   { return role; }
        /** Why this person should be contacted (e.g. "last person to see $subject alive"). */
        public String getReason() { return reason; }
    }

    /**
     * A coherent bundle of initial known facts, investigation leads,
     * and initial contacts for one case instance.  All strings may contain
     * {@code $placeholder} tokens which are resolved at generation time.
     */
    public static final class CaseSeed {
        private final List<String>      facts;
        private final List<SeedLead>    leads;
        private final List<SeedContact> contacts;

        public CaseSeed(List<String> facts, List<SeedLead> leads, List<SeedContact> contacts) {
            this.facts    = Collections.unmodifiableList(facts);
            this.leads    = Collections.unmodifiableList(leads);
            this.contacts = Collections.unmodifiableList(contacts);
        }

        /** Initial known facts (added as clues on the case file). */
        public List<String>      getFacts()    { return facts; }
        /** Initial investigation leads (added as case leads). */
        public List<SeedLead>    getLeads()    { return leads; }
        /** Initial contacts the investigator should approach. */
        public List<SeedContact> getContacts() { return contacts; }
    }

    /**
     * Describes the NPC character requirements for a case type: the base
     * roles, how many extra suspects are added at each complexity level,
     * the labels for those extra suspects, and a human-readable summary.
     */
    public static final class CaseDescription {
        private final List<String> roles;
        private final String extraSuspectsComplexity2;
        private final String extraSuspectsComplexity3;
        private final List<String> suspectLabels;
        private final String summary;

        public CaseDescription(List<String> roles,
                               String extraSuspectsComplexity2,
                               String extraSuspectsComplexity3,
                               List<String> suspectLabels,
                               String summary) {
            this.roles = Collections.unmodifiableList(roles);
            this.extraSuspectsComplexity2 = extraSuspectsComplexity2;
            this.extraSuspectsComplexity3 = extraSuspectsComplexity3;
            this.suspectLabels = Collections.unmodifiableList(suspectLabels);
            this.summary = summary;
        }

        /** Base NPC roles for this case type. */
        public List<String> getRoles() { return roles; }
        /** Human-readable count of extra suspects at complexity 2 (e.g. "1-2"). */
        public String getExtraSuspectsComplexity2() { return extraSuspectsComplexity2; }
        /** Human-readable count of extra suspects at complexity 3 (e.g. "2-3"). */
        public String getExtraSuspectsComplexity3() { return extraSuspectsComplexity3; }
        /** Labels for additional suspect roles. */
        public List<String> getSuspectLabels() { return suspectLabels; }
        /** Human-readable summary of NPC requirements for this case type. */
        public String getSummary() { return summary; }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final Map<String, List<String>> descriptions;
    private final Map<String, List<String>> objectives;
    private final Map<String, List<CaseSeed>> caseSeeds;
    private final Map<String, CaseDescription> caseDescriptions;

    private CaseTemplateData(Map<String, List<String>> descriptions,
                              Map<String, List<String>> objectives,
                              Map<String, List<CaseSeed>> caseSeeds,
                              Map<String, CaseDescription> caseDescriptions) {
        this.descriptions     = descriptions;
        this.objectives       = objectives;
        this.caseSeeds        = caseSeeds;
        this.caseDescriptions = caseDescriptions;
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

    /**
     * Returns the {@link CaseDescription} for the given case-type name,
     * or {@code null} if none is defined.
     *
     * @param caseTypeName uppercase case-type name, e.g. {@code "MURDER"}
     * @return the case description, or {@code null}
     */
    public CaseDescription getCaseDescription(String caseTypeName) {
        if (caseDescriptions == null || caseTypeName == null) return null;
        return caseDescriptions.get(caseTypeName);
    }

    /**
     * Returns an unmodifiable view of all case-description entries.
     * Keys are uppercase case-type names (e.g. {@code "MURDER"}).
     */
    public Map<String, CaseDescription> getCaseDescriptions() {
        if (caseDescriptions == null) return Collections.emptyMap();
        return Collections.unmodifiableMap(caseDescriptions);
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
     *   },
     *   "caseDescriptions": {
     *     "MURDER": {
     *       "roles": ["Client", "Subject (Suspect)", "Victim", ...],
     *       "extraSuspectsComplexity2": "1-2",
     *       "extraSuspectsComplexity3": "2-3",
     *       "suspectLabels": ["Suspect — Neighbour", ...],
     *       "summary": "A murder case requires ..."
     *     },
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
        Map<String, CaseDescription> caseDescs = new HashMap<String, CaseDescription>();

        JsonReader reader = new JsonReader();
        JsonValue  root   = reader.parse(json);

        parseSection(root, "descriptions", descriptions);
        parseSection(root, "objectives",   objectives);
        parseSeedsSection(root, seeds);
        parseCaseDescriptionsSection(root, caseDescs);

        return new CaseTemplateData(descriptions, objectives, seeds, caseDescs);
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
                // Parse contacts array
                List<SeedContact> contacts = new ArrayList<SeedContact>();
                JsonValue contactsArr = seedVal.get("contacts");
                if (contactsArr != null) {
                    for (JsonValue c = contactsArr.child; c != null; c = c.next) {
                        String cName   = c.has("name")   ? c.getString("name")   : "";
                        String cRole   = c.has("role")   ? c.getString("role")   : "";
                        String cReason = c.has("reason") ? c.getString("reason") : "";
                        contacts.add(new SeedContact(cName, cRole, cReason));
                    }
                }
                seedList.add(new CaseSeed(facts, leads, contacts));
            }
            target.put(key, seedList);
        }
    }

    /**
     * Parses the {@code "caseDescriptions"} section of the JSON.  Each
     * top-level key is a case-type name (e.g. {@code "MURDER"}) mapping to
     * an object with {@code roles}, {@code extraSuspectsComplexity2/3},
     * {@code suspectLabels}, and {@code summary}.
     */
    private static void parseCaseDescriptionsSection(JsonValue root,
                                                      Map<String, CaseDescription> target) {
        JsonValue section = root.get("caseDescriptions");
        if (section == null) return;
        for (JsonValue entry = section.child; entry != null; entry = entry.next) {
            String key = entry.name();

            List<String> roles = new ArrayList<String>();
            JsonValue rolesArr = entry.get("roles");
            if (rolesArr != null) {
                for (JsonValue r = rolesArr.child; r != null; r = r.next) {
                    roles.add(r.asString());
                }
            }

            String extra2 = entry.has("extraSuspectsComplexity2")
                    ? entry.getString("extraSuspectsComplexity2") : "0";
            String extra3 = entry.has("extraSuspectsComplexity3")
                    ? entry.getString("extraSuspectsComplexity3") : "0";

            List<String> suspectLabels = new ArrayList<String>();
            JsonValue slArr = entry.get("suspectLabels");
            if (slArr != null) {
                for (JsonValue s = slArr.child; s != null; s = s.next) {
                    suspectLabels.add(s.asString());
                }
            }

            String summary = entry.has("summary") ? entry.getString("summary") : "";

            target.put(key, new CaseDescription(roles, extra2, extra3, suspectLabels, summary));
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
