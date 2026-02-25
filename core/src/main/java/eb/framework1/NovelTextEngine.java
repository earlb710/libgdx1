package eb.framework1;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Novel text engine: selects contextual location/action descriptions from JSON data files.
 *
 * <p>Each language has its own JSON file (e.g. {@code text/description_en.json}, {@code text/description_es.json}).
 * English is the default and is used as a fallback when a requested language has no entry for
 * the given key.</p>
 *
 * <p>Each building type or improvement may have <em>1–N alternate description variants</em>
 * stored as a JSON array.  A map location code (e.g. {@code "G6"}) is used to pick which
 * variant is shown, so the same location always produces the same text while different
 * locations of the same building type may produce different text.  If only one variant is
 * present the location parameter has no effect.</p>
 *
 * <p>For a chosen variant, text is then selected in this priority order:</p>
 * <ol>
 *   <li>The sub-variant matching the character's highest attribute (if one exists).</li>
 *   <li>The sub-variant matching the character's gender ("male" or "female").</li>
 *   <li>The sub-variant matching the current {@link TimeOfDay} (if one exists).</li>
 *   <li>The default text of the chosen variant.</li>
 *   <li>An empty string if the key is not found at all.</li>
 * </ol>
 *
 * <p>Usage (production — load via LibGDX):
 * <pre>{@code
 * String json = Gdx.files.internal("text/description_en.json").readString("UTF-8");
 * NovelTextEngine engine = NovelTextEngine.fromJsonString(json);
 *
 * // Single-variant (existing) format — location ignored, same as before:
 * String text = engine.getDescription("gym", TimeOfDay.EVENING, profile.getAttributes(), profile.getGender());
 *
 * // Multi-variant format — location selects which variant to display:
 * String text = engine.getDescription("gym", "G6", TimeOfDay.EVENING, profile.getAttributes(), profile.getGender());
 *
 * // Improvement with location:
 * String impText = engine.getImprovementDescription("WiFi", "H1", profile.getGender());
 * }</pre>
 * </p>
 *
 * <h3>JSON schema — multi-variant array format (new)</h3>
 * <pre>{@code
 * "descriptions": {
 *   "<key>": [
 *     {
 *       "default": "Variant 1 text ...",
 *       "time":      { "morning": "...", "evening": "..." },
 *       "attribute": { "STRENGTH": "..." },
 *       "gender":    { "male": "...", "female": "..." }
 *     },
 *     {
 *       "default": "Variant 2 text ...",
 *       ...
 *     }
 *   ]
 * }
 * }</pre>
 *
 * <h3>JSON schema — single-entry object format (original, still supported)</h3>
 * <pre>{@code
 * "descriptions": {
 *   "<key>": {
 *     "default": "...",
 *     "time":      { "morning": "...", ... },
 *     "attribute": { "STRENGTH": "..." },
 *     "gender":    { "male": "...", "female": "..." }
 *   }
 * }
 * }</pre>
 */
public class NovelTextEngine {

    /** Internal representation of one description entry (one location/action key). */
    static final class DescriptionEntry {
        final String defaultText;
        final Map<TimeOfDay, String> timeVariants;
        final Map<String, String> attributeVariants;
        final Map<String, String> genderVariants;
        final Map<String, String> stateVariants;

        DescriptionEntry(String defaultText,
                         Map<TimeOfDay, String> timeVariants,
                         Map<String, String> attributeVariants,
                         Map<String, String> genderVariants) {
            this(defaultText, timeVariants, attributeVariants, genderVariants, null);
        }

        DescriptionEntry(String defaultText,
                         Map<TimeOfDay, String> timeVariants,
                         Map<String, String> attributeVariants,
                         Map<String, String> genderVariants,
                         Map<String, String> stateVariants) {
            this.defaultText = defaultText != null ? defaultText : "";
            this.timeVariants = timeVariants != null
                    ? Collections.unmodifiableMap(new EnumMap<>(timeVariants))
                    : Collections.<TimeOfDay, String>emptyMap();
            this.attributeVariants = attributeVariants != null
                    ? Collections.unmodifiableMap(new HashMap<>(attributeVariants))
                    : Collections.<String, String>emptyMap();
            this.genderVariants = genderVariants != null
                    ? Collections.unmodifiableMap(new HashMap<>(genderVariants))
                    : Collections.<String, String>emptyMap();
            this.stateVariants = stateVariants != null
                    ? Collections.unmodifiableMap(new HashMap<>(stateVariants))
                    : Collections.<String, String>emptyMap();
        }
    }

    // Each key maps to a list of 1..N alternate variants.
    private final Map<String, List<DescriptionEntry>> entries;
    private final Map<String, List<DescriptionEntry>> improvements;

    /**
     * Creates an engine from pre-built maps of single description entries and improvement
     * entries.  Each entry is wrapped in a one-element list; intended for unit testing.
     *
     * @param entries      Map of building description key → {@link DescriptionEntry}
     * @param improvements Map of improvement name → {@link DescriptionEntry}
     * @return New {@link NovelTextEngine} populated from the maps
     */
    static NovelTextEngine fromSingleEntries(Map<String, DescriptionEntry> entries,
                                             Map<String, DescriptionEntry> improvements) {
        Map<String, List<DescriptionEntry>> wrappedEntries      = wrapInLists(entries);
        Map<String, List<DescriptionEntry>> wrappedImprovements = wrapInLists(improvements);
        return new NovelTextEngine(wrappedEntries, wrappedImprovements);
    }

    /**
     * Constructs the engine from pre-built maps of variant lists.
     * Intended for unit testing or programmatic construction.
     *
     * @param entries      Map of building description key → list of {@link DescriptionEntry}
     * @param improvements Map of improvement name → list of {@link DescriptionEntry}
     */
    NovelTextEngine(Map<String, List<DescriptionEntry>> entries,
                    Map<String, List<DescriptionEntry>> improvements) {
        this.entries = entries != null
                ? Collections.unmodifiableMap(new HashMap<>(entries))
                : Collections.<String, List<DescriptionEntry>>emptyMap();
        this.improvements = improvements != null
                ? Collections.unmodifiableMap(new HashMap<>(improvements))
                : Collections.<String, List<DescriptionEntry>>emptyMap();
    }

    /**
     * Parses a JSON string and creates a {@link NovelTextEngine} from it.
     *
     * <p>Both the original single-object format and the new multi-variant array format are
     * accepted for every entry in {@code "descriptions"} and {@code "improvements"}.</p>
     *
     * @param json JSON content string
     * @return New {@link NovelTextEngine} populated from the JSON
     * @throws IllegalArgumentException if {@code json} is null or blank
     */
    public static NovelTextEngine fromJsonString(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON content cannot be null or empty");
        }
        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(json);

        Map<String, List<DescriptionEntry>> entries = new HashMap<>();
        Map<String, List<DescriptionEntry>> improvements = new HashMap<>();

        JsonValue descriptionsNode = root.get("descriptions");
        if (descriptionsNode != null) {
            for (JsonValue entryNode = descriptionsNode.child; entryNode != null; entryNode = entryNode.next) {
                entries.put(entryNode.name, parseVariants(entryNode));
            }
        }

        JsonValue improvementsNode = root.get("improvements");
        if (improvementsNode != null) {
            for (JsonValue entryNode = improvementsNode.child; entryNode != null; entryNode = entryNode.next) {
                improvements.put(entryNode.name, parseVariants(entryNode));
            }
        }

        return new NovelTextEngine(entries, improvements);
    }

    /**
     * Parses a description/improvement node that is either a JSON array (multi-variant) or a
     * JSON object (single-variant, legacy format), returning a non-empty list of entries.
     */
    private static List<DescriptionEntry> parseVariants(JsonValue node) {
        if (node.isArray()) {
            List<DescriptionEntry> list = new ArrayList<>();
            for (JsonValue child = node.child; child != null; child = child.next) {
                list.add(parseEntry(child));
            }
            if (list.isEmpty()) {
                list.add(new DescriptionEntry("", null, null, null));
            }
            return list;
        }
        return Collections.singletonList(parseEntry(node));
    }

    /** Parses a single description/improvement entry node into a {@link DescriptionEntry}. */
    private static DescriptionEntry parseEntry(JsonValue entryNode) {
        String defaultText = entryNode.getString("default", "");

        // Parse time-of-day variants
        Map<TimeOfDay, String> timeVariants = new EnumMap<>(TimeOfDay.class);
        JsonValue timeNode = entryNode.get("time");
        if (timeNode != null) {
            for (JsonValue t = timeNode.child; t != null; t = t.next) {
                TimeOfDay tod = parseTimeOfDay(t.name);
                if (tod != null) {
                    timeVariants.put(tod, t.asString());
                }
            }
        }

        // Parse attribute variants
        Map<String, String> attributeVariants = new HashMap<>();
        JsonValue attrNode = entryNode.get("attribute");
        if (attrNode != null) {
            for (JsonValue a = attrNode.child; a != null; a = a.next) {
                attributeVariants.put(a.name.toUpperCase(), a.asString());
            }
        }

        // Parse gender variants (keys stored lower-cased; unrecognised gender values at
        // lookup time gracefully fall through to the time-of-day / default fallback)
        Map<String, String> genderVariants = new HashMap<>();
        JsonValue genderNode = entryNode.get("gender");
        if (genderNode != null) {
            for (JsonValue g = genderNode.child; g != null; g = g.next) {
                genderVariants.put(g.name.toLowerCase(), g.asString());
            }
        }

        // Parse state variants
        Map<String, String> stateVariants = new HashMap<>();
        JsonValue stateNode = entryNode.get("state");
        if (stateNode != null) {
            for (JsonValue s = stateNode.child; s != null; s = s.next) {
                stateVariants.put(s.name.toLowerCase(), s.asString());
            }
        }

        return new DescriptionEntry(defaultText, timeVariants, attributeVariants, genderVariants, stateVariants);
    }

    // -------------------------------------------------------------------------
    // Public API — getDescription
    // -------------------------------------------------------------------------

    /**
     * Returns the most contextually appropriate description for the given key, choosing among
     * available variants using the supplied map-location code.
     *
     * <p>The {@code location} string (e.g. {@code "G6"}, {@code "H1"}) is hashed to pick a
     * variant index deterministically, so the same location always returns the same variant for
     * a given building type.  If the entry has only one variant the location is ignored.</p>
     *
     * <p>Within the selected variant, text is resolved in this priority order:
     * <ol>
     *   <li>Attribute sub-variant for the character's highest-valued attribute that has one.</li>
     *   <li>Gender sub-variant matching {@code gender}.</li>
     *   <li>Time-of-day sub-variant for {@code timeOfDay}.</li>
     *   <li>Default text of the chosen variant.</li>
     *   <li>Empty string if the key is unknown.</li>
     * </ol>
     * </p>
     *
     * @param key        Description key (e.g. {@code "gym_fitness_center"})
     * @param location   Map location code (e.g. {@code "G6"}); may be null (uses first variant)
     * @param timeOfDay  Current in-game time of day
     * @param attributes Character attribute name → value map
     * @param gender     Character gender string ("male" or "female"); may be null
     * @return Selected description text, never {@code null}
     */
    public String getDescription(String key, String location,
                                 TimeOfDay timeOfDay, Map<String, Integer> attributes,
                                 String gender) {
        if (key == null) return "";
        List<DescriptionEntry> variants = entries.get(key);
        if (variants == null || variants.isEmpty()) return "";
        DescriptionEntry entry = variants.get(selectVariantIndex(location, variants.size()));
        return resolveEntry(entry, timeOfDay, attributes, gender);
    }

    /**
     * Returns the most contextually appropriate description for the given key.
     * When the entry has multiple variants the first variant (index 0) is used.
     * Use {@link #getDescription(String, String, TimeOfDay, Map, String)} to select by location.
     *
     * @param key        Description key
     * @param timeOfDay  Current in-game time of day
     * @param attributes Character attribute name → value map
     * @param gender     Character gender string ("male" or "female"); may be null
     * @return Selected description text, never {@code null}
     */
    public String getDescription(String key, TimeOfDay timeOfDay,
                                 Map<String, Integer> attributes, String gender) {
        return getDescription(key, null, timeOfDay, attributes, gender);
    }

    /**
     * Convenience overload without gender (gender check is skipped).
     *
     * @param key        Description key
     * @param timeOfDay  Current in-game time of day
     * @param attributes Character attribute map
     * @return Selected description text, never {@code null}
     */
    public String getDescription(String key, TimeOfDay timeOfDay, Map<String, Integer> attributes) {
        return getDescription(key, null, timeOfDay, attributes, null);
    }

    /**
     * Convenience overload that derives {@link TimeOfDay} from an hour (0–23) and includes
     * a location code and gender.
     *
     * @param key        Description key
     * @param location   Map location code (e.g. {@code "G6"}); may be null
     * @param hour       In-game hour (0–23)
     * @param attributes Character attribute map
     * @param gender     Character gender string ("male" or "female"); may be null
     * @return Selected description text, never {@code null}
     */
    public String getDescription(String key, String location, int hour,
                                 Map<String, Integer> attributes, String gender) {
        return getDescription(key, location, TimeOfDay.fromHour(hour), attributes, gender);
    }

    /**
     * Convenience overload that derives {@link TimeOfDay} from an hour (0–23) and includes gender.
     *
     * @param key        Description key
     * @param hour       In-game hour (0–23)
     * @param attributes Character attribute map
     * @param gender     Character gender string ("male" or "female"); may be null
     * @return Selected description text, never {@code null}
     */
    public String getDescription(String key, int hour, Map<String, Integer> attributes, String gender) {
        return getDescription(key, null, TimeOfDay.fromHour(hour), attributes, gender);
    }

    /**
     * Convenience overload that derives {@link TimeOfDay} from an hour (0–23), without gender.
     *
     * @param key        Description key
     * @param hour       In-game hour (0–23)
     * @param attributes Character attribute map
     * @return Selected description text, never {@code null}
     */
    public String getDescription(String key, int hour, Map<String, Integer> attributes) {
        return getDescription(key, null, TimeOfDay.fromHour(hour), attributes, null);
    }

    // -------------------------------------------------------------------------
    // Public API — getImprovementDescription
    // -------------------------------------------------------------------------

    /**
     * Returns the description for a building improvement, choosing among available variants using
     * the supplied map-location code and tailoring the result to the character's gender.
     *
     * <p>Within the selected variant, text is resolved in this priority order:
     * <ol>
     *   <li>Gender sub-variant matching {@code gender}.</li>
     *   <li>Default text of the chosen variant.</li>
     *   <li>Empty string if the improvement name is not found.</li>
     * </ol>
     * </p>
     *
     * @param name     Improvement name exactly as it appears in {@code buildings.json}
     * @param location Map location code (e.g. {@code "G6"}); may be null (uses first variant)
     * @param gender   Character gender string ("male" or "female"); may be null
     * @return Selected description text, never {@code null}
     */
    public String getImprovementDescription(String name, String location, String gender) {
        if (name == null) return "";
        List<DescriptionEntry> variants = improvements.get(name);
        if (variants == null || variants.isEmpty()) return "";
        DescriptionEntry entry = variants.get(selectVariantIndex(location, variants.size()));

        // 1. Check gender variant
        if (gender != null && !entry.genderVariants.isEmpty()) {
            String genderText = entry.genderVariants.get(gender.toLowerCase());
            if (genderText != null) return genderText;
        }

        // 2. Fall back to default
        return entry.defaultText;
    }

    /**
     * Returns the description for a building improvement tailored to the character's gender.
     * When the entry has multiple variants the first variant (index 0) is used.
     * Use {@link #getImprovementDescription(String, String, String)} to select by location.
     *
     * @param name   Improvement name
     * @param gender Character gender string ("male" or "female"); may be null
     * @return Selected description text, never {@code null}
     */
    public String getImprovementDescription(String name, String gender) {
        return getImprovementDescription(name, null, gender);
    }

    /**
     * Convenience overload that returns the gender-neutral improvement description.
     *
     * @param name Improvement name
     * @return Selected description text, never {@code null}
     */
    public String getImprovementDescription(String name) {
        return getImprovementDescription(name, null, null);
    }

    // -------------------------------------------------------------------------
    // Public API — getStateDescription
    // -------------------------------------------------------------------------

    /**
     * Returns the state-based description for the given key, choosing among available
     * variants using the supplied map-location code.
     *
     * <p>The {@code state} parameter describes the maintenance/condition level of the
     * building (e.g. {@code "good"}, {@code "normal"}, {@code "bad"}).  If no state
     * variant is defined for the key, the default text of the chosen variant is returned.</p>
     *
     * @param key      Description key (e.g. {@code "gym_fitness_center"})
     * @param location Map location code (e.g. {@code "G6"}); may be null (uses first variant)
     * @param state    Building condition state ("good", "normal", or "bad"); may be null
     * @return Selected description text, never {@code null}
     */
    public String getStateDescription(String key, String location, String state) {
        if (key == null) return "";
        List<DescriptionEntry> variants = entries.get(key);
        if (variants == null || variants.isEmpty()) return "";
        DescriptionEntry entry = variants.get(selectVariantIndex(location, variants.size()));
        if (state != null && !entry.stateVariants.isEmpty()) {
            String stateText = entry.stateVariants.get(state.toLowerCase());
            if (stateText != null) return stateText;
        }
        return entry.defaultText;
    }

    /**
     * Returns the state-based description for the given key.
     * When the entry has multiple variants the first variant (index 0) is used.
     * Use {@link #getStateDescription(String, String, String)} to select by location.
     *
     * @param key   Description key (e.g. {@code "gym_fitness_center"})
     * @param state Building condition state ("good", "normal", or "bad"); may be null
     * @return Selected description text, never {@code null}
     */
    public String getStateDescription(String key, String state) {
        return getStateDescription(key, null, state);
    }

    // -------------------------------------------------------------------------
    // Public API — variant count queries and recommendations
    // -------------------------------------------------------------------------

    /**
     * Returns the number of description variants currently loaded for the given key.
     *
     * @param key Description key (e.g. {@code "gym_fitness_center"})
     * @return Number of variants, or {@code 0} if the key is unknown
     */
    public int getVariantCount(String key) {
        if (key == null) return 0;
        List<DescriptionEntry> variants = entries.get(key);
        return variants != null ? variants.size() : 0;
    }

    /**
     * Calculates the recommended number of description variants needed so that,
     * given {@code expectedInstances} copies of a building on the map, there is
     * approximately a 50% probability that at least two of them share the
     * same description (the <em>birthday problem</em>).
     *
     * <p>The formula used is {@code k = ceil(n * (n - 1) / (2 * ln(2)))}, where
     * {@code n} is the expected number of instances, {@code k} is the number
     * of distinct variants required for a ~50% collision probability, and
     * {@code ln} denotes the natural logarithm ({@link Math#log(double)}).</p>
     *
     * @param expectedInstances Average number of times this building type
     *                          appears on the city map (must be &ge; 0)
     * @return Recommended number of description variants (&ge; 1)
     */
    public static int recommendedVariantCount(int expectedInstances) {
        if (expectedInstances <= 1) return 1;
        double n = expectedInstances;
        double k = n * (n - 1) / (2.0 * Math.log(2));
        return Math.max(1, (int) Math.ceil(k));
    }

    /**
     * Calculates the recommended number of description variants for every building
     * type, based on each building's percentage share and the total number of
     * building cells on the map.
     *
     * <p>For each building the expected instance count is
     * {@code totalBuildingCells * (percentage / totalPercentage)}, and the
     * recommended variant count follows from the birthday-problem formula
     * (see {@link #recommendedVariantCount(int)}).</p>
     *
     * @param buildings          List of building definitions (with percentage data)
     * @param totalBuildingCells Total number of building cells on the map
     * @return Map of building definition ID → recommended variant count
     */
    public static Map<String, Integer> recommendedVariantCounts(
            List<BuildingDefinition> buildings, int totalBuildingCells) {
        Map<String, Integer> result = new HashMap<>();
        if (buildings == null || buildings.isEmpty() || totalBuildingCells <= 0) {
            return result;
        }
        double totalPercentage = 0;
        for (BuildingDefinition b : buildings) {
            totalPercentage += b.getPercentage();
        }
        if (totalPercentage <= 0) {
            for (BuildingDefinition b : buildings) {
                result.put(b.getId(), 1);
            }
            return result;
        }
        for (BuildingDefinition b : buildings) {
            double normalizedPct = b.getPercentage() / totalPercentage;
            int expectedInstances = (int) Math.round(normalizedPct * totalBuildingCells);
            result.put(b.getId(), recommendedVariantCount(expectedInstances));
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Selects the variant index for a given location code and total variant count.
     *
     * <p>The same {@code location} always produces the same index for a given
     * {@code variantCount}, ensuring deterministic behaviour across game sessions.
     * Returns {@code 0} when {@code location} is null/empty or {@code variantCount} is 1.</p>
     *
     * @param location     Map location code (e.g. {@code "G6"}); may be null
     * @param variantCount Total number of available variants (≥ 1)
     * @return A value in {@code [0, variantCount)}
     */
    static int selectVariantIndex(String location, int variantCount) {
        if (location == null || location.isEmpty() || variantCount <= 1) return 0;
        int h = 0;
        for (int i = 0; i < location.length(); i++) {
            h = h * 31 + location.charAt(i);
        }
        return (h & Integer.MAX_VALUE) % variantCount;
    }

    /** Applies the attribute → gender → time → default resolution to a single entry. */
    private String resolveEntry(DescriptionEntry entry, TimeOfDay timeOfDay,
                                Map<String, Integer> attributes, String gender) {
        // 1. Check attribute variants — use the highest-valued attribute that has a variant
        if (attributes != null && !attributes.isEmpty() && !entry.attributeVariants.isEmpty()) {
            String attrText = bestAttributeVariant(attributes, entry.attributeVariants);
            if (attrText != null) return attrText;
        }

        // 2. Check gender variant
        if (gender != null && !entry.genderVariants.isEmpty()) {
            String genderText = entry.genderVariants.get(gender.toLowerCase());
            if (genderText != null) return genderText;
        }

        // 3. Check time-of-day variant
        if (timeOfDay != null && !entry.timeVariants.isEmpty()) {
            String timeText = entry.timeVariants.get(timeOfDay);
            if (timeText != null) return timeText;
        }

        // 4. Fall back to default
        return entry.defaultText;
    }

    /**
     * Returns the description text for the highest-valued attribute that has a variant,
     * or {@code null} if no attribute in the map has a matching variant.
     */
    private static String bestAttributeVariant(Map<String, Integer> attributes,
                                                Map<String, String> variants) {
        String bestText = null;
        int bestVal = Integer.MIN_VALUE;
        for (Map.Entry<String, Integer> e : attributes.entrySet()) {
            if (e.getValue() == null) continue;
            String key = e.getKey().toUpperCase();
            String text = variants.get(key);
            if (text != null && e.getValue() > bestVal) {
                bestVal = e.getValue();
                bestText = text;
            }
        }
        return bestText;
    }

    /** Wraps each value in a single-element list, producing a map of lists. */
    private static <K> Map<K, List<DescriptionEntry>> wrapInLists(Map<K, DescriptionEntry> map) {
        Map<K, List<DescriptionEntry>> result = new HashMap<>();
        if (map != null) {
            for (Map.Entry<K, DescriptionEntry> e : map.entrySet()) {
                result.put(e.getKey(), Collections.singletonList(e.getValue()));
            }
        }
        return result;
    }

    /** Converts a lower-case time-of-day string from JSON to a {@link TimeOfDay} constant. */
    private static TimeOfDay parseTimeOfDay(String name) {
        if (name == null) return null;
        switch (name.toLowerCase()) {
            case "morning":   return TimeOfDay.MORNING;
            case "afternoon": return TimeOfDay.AFTERNOON;
            case "evening":   return TimeOfDay.EVENING;
            case "night":     return TimeOfDay.NIGHT;
            default:          return null;
        }
    }
}
