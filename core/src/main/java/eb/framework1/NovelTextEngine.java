package eb.framework1;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Novel text engine: selects contextual location/action descriptions from JSON data files.
 *
 * <p>Each language has its own JSON file (e.g. {@code text/en.json}, {@code text/es.json}).
 * English is the default and is used as a fallback when a requested language has no entry for
 * the given key.</p>
 *
 * <p>For a given description key, the engine chooses text in this priority order:</p>
 * <ol>
 *   <li>The variant matching the character's highest attribute (if a variant exists for it).</li>
 *   <li>The variant matching the character's gender ("male" or "female").</li>
 *   <li>The variant matching the current {@link TimeOfDay} (if one exists).</li>
 *   <li>The default description for the key.</li>
 *   <li>An empty string if the key is not found at all.</li>
 * </ol>
 *
 * <p>Usage (production — load via LibGDX):
 * <pre>{@code
 * String json = Gdx.files.internal("text/en.json").readString("UTF-8");
 * NovelTextEngine engine = NovelTextEngine.fromJsonString(json);
 * String text = engine.getDescription("gym", TimeOfDay.EVENING, profile.getAttributes(), profile.getGender());
 * // For a specific building improvement:
 * String impText = engine.getImprovementDescription("Evidence Room", profile.getGender());
 * }</pre>
 * </p>
 */
public class NovelTextEngine {

    /** Internal representation of one description entry (one location/action key). */
    static final class DescriptionEntry {
        final String defaultText;
        final Map<TimeOfDay, String> timeVariants;
        final Map<String, String> attributeVariants;
        final Map<String, String> genderVariants;

        DescriptionEntry(String defaultText,
                         Map<TimeOfDay, String> timeVariants,
                         Map<String, String> attributeVariants,
                         Map<String, String> genderVariants) {
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
        }
    }

    private final Map<String, DescriptionEntry> entries;
    private final Map<String, DescriptionEntry> improvements;

    /**
     * Constructs the engine from pre-built maps of description entries and improvement entries.
     * Intended for direct use and unit testing.
     *
     * @param entries      Map of building description key → {@link DescriptionEntry}
     * @param improvements Map of improvement name → {@link DescriptionEntry}
     */
    NovelTextEngine(Map<String, DescriptionEntry> entries, Map<String, DescriptionEntry> improvements) {
        this.entries = entries != null
                ? Collections.unmodifiableMap(new HashMap<>(entries))
                : Collections.<String, DescriptionEntry>emptyMap();
        this.improvements = improvements != null
                ? Collections.unmodifiableMap(new HashMap<>(improvements))
                : Collections.<String, DescriptionEntry>emptyMap();
    }

    /**
     * Parses a JSON string and creates a {@link NovelTextEngine} from it.
     *
     * <p>Expected JSON schema:
     * <pre>{@code
     * {
     *   "version": "1.0",
     *   "language": "en",
     *   "descriptions": {
     *     "<key>": {
     *       "default": "...",
     *       "time": {
     *         "morning": "...",
     *         "afternoon": "...",
     *         "evening": "...",
     *         "night": "..."
     *       },
     *       "attribute": {
     *         "STRENGTH": "...",
     *         "INTELLIGENCE": "..."
     *       },
     *       "gender": {
     *         "male": "...",
     *         "female": "..."
     *       }
     *     }
     *   },
     *   "improvements": {
     *     "<Improvement Name>": {
     *       "default": "...",
     *       "gender": {
     *         "male": "...",
     *         "female": "..."
     *       }
     *     }
     *   }
     * }
     * }</pre>
     * </p>
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

        Map<String, DescriptionEntry> entries = new HashMap<>();
        Map<String, DescriptionEntry> improvements = new HashMap<>();

        JsonValue descriptionsNode = root.get("descriptions");
        if (descriptionsNode != null) {
            for (JsonValue entryNode = descriptionsNode.child; entryNode != null; entryNode = entryNode.next) {
                entries.put(entryNode.name, parseEntry(entryNode));
            }
        }

        JsonValue improvementsNode = root.get("improvements");
        if (improvementsNode != null) {
            for (JsonValue entryNode = improvementsNode.child; entryNode != null; entryNode = entryNode.next) {
                improvements.put(entryNode.name, parseEntry(entryNode));
            }
        }

        return new NovelTextEngine(entries, improvements);
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

        return new DescriptionEntry(defaultText, timeVariants, attributeVariants, genderVariants);
    }

    /**
     * Returns the most contextually appropriate description for the given key.
     *
     * <p>Selection priority:
     * <ol>
     *   <li>Attribute variant for the character's highest-valued attribute that has a variant.</li>
     *   <li>Gender variant matching {@code gender} ("male" or "female", case-insensitive).</li>
     *   <li>Time-of-day variant for {@code timeOfDay}.</li>
     *   <li>Default text for the key.</li>
     *   <li>Empty string if the key is unknown.</li>
     * </ol>
     * </p>
     *
     * @param key        Description key (e.g. {@code "gym_fitness_center"}, {@code "office_building_small"})
     * @param timeOfDay  Current in-game time of day
     * @param attributes Character attribute name → value map (attribute names match
     *                   {@link CharacterAttribute} enum names, e.g. {@code "STRENGTH"})
     * @param gender     Character gender string ("male" or "female", case-insensitive); may be null
     * @return Selected description text, never {@code null}
     */
    public String getDescription(String key, TimeOfDay timeOfDay, Map<String, Integer> attributes, String gender) {
        if (key == null) return "";
        DescriptionEntry entry = entries.get(key);
        if (entry == null) return "";

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
     * Convenience overload without gender (gender check is skipped).
     *
     * @param key        Description key
     * @param timeOfDay  Current in-game time of day
     * @param attributes Character attribute map
     * @return Selected description text, never {@code null}
     */
    public String getDescription(String key, TimeOfDay timeOfDay, Map<String, Integer> attributes) {
        return getDescription(key, timeOfDay, attributes, null);
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
        return getDescription(key, TimeOfDay.fromHour(hour), attributes, gender);
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
        return getDescription(key, TimeOfDay.fromHour(hour), attributes, null);
    }

    /**
     * Returns the description for a building improvement, optionally tailored to the character's gender.
     *
     * <p>Selection priority:
     * <ol>
     *   <li>Gender variant matching {@code gender} ("male" or "female", case-insensitive).</li>
     *   <li>Default text for the improvement.</li>
     *   <li>Empty string if the improvement name is not found.</li>
     * </ol>
     * </p>
     *
     * @param name   Improvement name exactly as it appears in {@code buildings.json}
     *               (e.g. {@code "Security Camera"}, {@code "Evidence Room"})
     * @param gender Character gender string ("male" or "female", case-insensitive); may be null
     * @return Selected description text, never {@code null}
     */
    public String getImprovementDescription(String name, String gender) {
        if (name == null) return "";
        DescriptionEntry entry = improvements.get(name);
        if (entry == null) return "";

        // 1. Check gender variant
        if (gender != null && !entry.genderVariants.isEmpty()) {
            String genderText = entry.genderVariants.get(gender.toLowerCase());
            if (genderText != null) return genderText;
        }

        // 2. Fall back to default
        return entry.defaultText;
    }

    /**
     * Convenience overload that returns the gender-neutral improvement description.
     *
     * @param name Improvement name
     * @return Selected description text, never {@code null}
     */
    public String getImprovementDescription(String name) {
        return getImprovementDescription(name, null);
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
