package eb.framework1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Generates random company names from the {@code company_names.json} and
 * {@code company_types.json} data files.
 *
 * <p>This class is intentionally free of libGDX dependencies so that it can be
 * constructed and tested with plain JUnit without a running libGDX application.
 * Data is injected via the constructor; loading from JSON is the responsibility
 * of {@link GameDataManager}.
 *
 * <h3>Name templates</h3>
 * <p>Each template may contain the literal token {@code $surname}, which is
 * replaced at generation time with a randomly chosen surname from the injected
 * surname list.  Templates without the token are returned verbatim.</p>
 *
 * <h3>Type IDs</h3>
 * <p>Each template is tagged with a company type ID (e.g. {@code "retail"},
 * {@code "food"}) or {@code "G"} for Generic templates that are eligible for
 * any request.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 *   CompanyNameGenerator gen = gameDataManager.getCompanyNameGenerator();
 *
 *   // Any template
 *   String name = gen.generate();
 *
 *   // Filtered by type (+ matching Generic templates)
 *   String retail = gen.generate("retail");
 *
 *   // Appropriate for a specific building
 *   String forBuilding = gen.generateForBuilding("fast_food_restaurant");
 * </pre>
 */
public class CompanyNameGenerator {

    /** Placeholder token replaced with a random surname at generation time. */
    public static final String SURNAME_TOKEN = "$surname";

    /** Type ID that makes a template eligible for any {@link #generate(String)} call. */
    public static final String TYPE_GENERIC = "G";

    // -------------------------------------------------------------------------
    // Data records
    // -------------------------------------------------------------------------

    /** Immutable record for a single name template entry. */
    public static final class NameTemplate {
        /** Raw template string, may contain {@value CompanyNameGenerator#SURNAME_TOKEN}. */
        public final String template;
        /** Company type ID this template belongs to, or {@value CompanyNameGenerator#TYPE_GENERIC}. */
        public final String typeId;

        public NameTemplate(String template, String typeId) {
            this.template = template != null ? template : "";
            this.typeId   = typeId   != null ? typeId   : TYPE_GENERIC;
        }
    }

    /** Immutable record describing one company type and the buildings it occupies. */
    public static final class CompanyType {
        /** Unique type identifier, e.g. {@code "retail"}. */
        public final String id;
        /** Human-readable name, e.g. {@code "Retail"}. */
        public final String name;
        /** Building IDs where this type of company operates. */
        public final List<String> buildings;

        public CompanyType(String id, String name, List<String> buildings) {
            this.id        = id        != null ? id        : "";
            this.name      = name      != null ? name      : "";
            this.buildings = buildings != null ? Collections.unmodifiableList(new ArrayList<>(buildings))
                                               : Collections.<String>emptyList();
        }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final List<NameTemplate>         allTemplates;
    private final Map<String, List<NameTemplate>> templatesByType;   // typeId → templates of that type only
    private final List<NameTemplate>         genericTemplates;       // type == "G"
    private final Map<String, CompanyType>   typesById;              // typeId → CompanyType
    private final Map<String, String>        buildingToType;         // buildingId → typeId
    private final List<String>               surnames;
    private final Random                     random;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a generator with the supplied data and a default {@link Random}.
     *
     * @param templates list of {@link NameTemplate} objects from
     *                  {@code company_names.json}; {@code null} treated as empty
     * @param types     list of {@link CompanyType} objects from
     *                  {@code company_types.json}; {@code null} treated as empty
     * @param surnames  flat surname list from {@code person_surnames.json};
     *                  {@code null} treated as empty
     */
    public CompanyNameGenerator(List<NameTemplate> templates,
                                List<CompanyType>  types,
                                List<String>       surnames) {
        this(templates, types, surnames, new Random());
    }

    /**
     * Creates a generator with an explicit {@link Random} (useful for
     * seeded/reproducible tests).
     */
    public CompanyNameGenerator(List<NameTemplate> templates,
                                List<CompanyType>  types,
                                List<String>       surnames,
                                Random             random) {
        this.allTemplates = templates != null ? templates : new ArrayList<NameTemplate>();
        this.surnames     = surnames  != null ? surnames  : new ArrayList<String>();
        this.random       = random    != null ? random    : new Random();

        // Index templates by type
        templatesByType  = new HashMap<>();
        genericTemplates = new ArrayList<>();
        for (NameTemplate t : this.allTemplates) {
            if (TYPE_GENERIC.equals(t.typeId)) {
                genericTemplates.add(t);
            } else {
                List<NameTemplate> bucket = templatesByType.get(t.typeId);
                if (bucket == null) {
                    bucket = new ArrayList<>();
                    templatesByType.put(t.typeId, bucket);
                }
                bucket.add(t);
            }
        }

        // Index types and build buildingId → typeId map
        typesById      = new HashMap<>();
        buildingToType = new HashMap<>();
        List<CompanyType> safeTypes = types != null ? types : new ArrayList<CompanyType>();
        for (CompanyType ct : safeTypes) {
            typesById.put(ct.id, ct);
            for (String buildingId : ct.buildings) {
                buildingToType.put(buildingId, ct.id);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns a randomly generated company name from all available templates.
     *
     * @return generated name string, never {@code null}
     */
    public String generate() {
        if (allTemplates.isEmpty()) return "Unknown Company";
        NameTemplate t = allTemplates.get(random.nextInt(allTemplates.size()));
        return resolve(t.template);
    }

    /**
     * Returns a company name appropriate for the given type.
     *
     * <p>The eligible pool is the union of templates tagged with {@code typeId}
     * and templates tagged {@code "G"} (Generic).  If {@code typeId} is
     * {@code null}, {@code "G"}, or unknown, only generic templates are used;
     * if there are none either, falls back to all templates.</p>
     *
     * @param typeId company type ID (e.g. {@code "retail"}), or {@code null}
     * @return generated name string, never {@code null}
     */
    public String generate(String typeId) {
        List<NameTemplate> pool = buildPool(typeId);
        if (pool.isEmpty()) return "Unknown Company";
        return resolve(pool.get(random.nextInt(pool.size())).template);
    }

    /**
     * Returns a company name appropriate for the given building ID.
     *
     * <p>Looks up the company type for the building using the data from
     * {@code company_types.json}, then delegates to {@link #generate(String)}.
     * If the building is not mapped to any type, a generic name is returned.</p>
     *
     * @param buildingId building definition ID (e.g. {@code "fast_food_restaurant"})
     * @return generated name string, never {@code null}
     */
    public String generateForBuilding(String buildingId) {
        String typeId = buildingId != null ? buildingToType.get(buildingId) : null;
        return generate(typeId);
    }

    /**
     * Returns the company type ID that the given building belongs to,
     * or {@code null} if the building is not mapped.
     */
    public String getTypeForBuilding(String buildingId) {
        return buildingId != null ? buildingToType.get(buildingId) : null;
    }

    /** Returns the total number of templates loaded. */
    public int templateCount() {
        return allTemplates.size();
    }

    /** Returns the number of templates for the given type (excluding generics). */
    public int templateCountForType(String typeId) {
        List<NameTemplate> bucket = templatesByType.get(typeId);
        return bucket != null ? bucket.size() : 0;
    }

    /** Returns the number of Generic ({@code "G"}) templates loaded. */
    public int genericTemplateCount() {
        return genericTemplates.size();
    }

    /** Returns the number of company types loaded (including "G"). */
    public int typeCount() {
        return typesById.size();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Builds the eligible pool for a type-filtered request: type-specific
     * templates + generic templates.  Falls back to all templates if the
     * combined pool would be empty.
     */
    private List<NameTemplate> buildPool(String typeId) {
        if (typeId == null || TYPE_GENERIC.equals(typeId)) {
            return genericTemplates.isEmpty() ? allTemplates : genericTemplates;
        }
        List<NameTemplate> typed = templatesByType.get(typeId);
        if (typed == null || typed.isEmpty()) {
            // Unknown or empty type → generics only, or all if no generics
            return genericTemplates.isEmpty() ? allTemplates : genericTemplates;
        }
        // Union: typed + generic
        List<NameTemplate> pool = new ArrayList<>(typed.size() + genericTemplates.size());
        pool.addAll(typed);
        pool.addAll(genericTemplates);
        return pool;
    }

    /**
     * Resolves a template string by replacing every occurrence of
     * {@value #SURNAME_TOKEN} with a randomly chosen surname.
     */
    private String resolve(String template) {
        if (!template.contains(SURNAME_TOKEN)) return template;
        // A template may contain $surname more than once (e.g. "$surname & $surname Law")
        String result = template;
        while (result.contains(SURNAME_TOKEN)) {
            String s = surnames.isEmpty() ? "" : surnames.get(random.nextInt(surnames.size()));
            result = result.replaceFirst("\\$surname", s);
        }
        return result.trim();
    }
}
