package eb.framework1.face;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link FaceSvgBuilder.SvgTemplateLoader SvgTemplateLoader} that is backed by a pre-parsed
 * map of feature → id → SVG-fragment strings.
 *
 * <p>Use the static factory {@link #fromMap(Map)} to build an instance from
 * data you have already parsed (e.g. from the bundled
 * {@code assets/face/svgs.json} file).
 *
 * <p>A minimal JSON parser is included so that the loader works in pure Java
 * without requiring a third-party library.  The JSON structure expected is:
 * <pre>
 * {
 *   "body":  { "body": "&lt;path .../&gt;", "body2": "...", ... },
 *   "eye":   { "eye1": "...", ... },
 *   ...
 * }
 * </pre>
 *
 * <h3>Example</h3>
 * <pre>
 *   // In a non-libGDX context (e.g. unit tests) load the file manually:
 *   String json = new String(Files.readAllBytes(Paths.get("assets/face/svgs.json")));
 *   JsonSvgTemplateLoader loader = JsonSvgTemplateLoader.fromJson(json);
 *
 *   FaceSvgBuilder builder = new FaceSvgBuilder(loader);
 * </pre>
 */
public final class JsonSvgTemplateLoader implements FaceSvgBuilder.SvgTemplateLoader {

    /** feature name → (id → svg fragment). */
    private final Map<String, Map<String, String>> data;

    private JsonSvgTemplateLoader(Map<String, Map<String, String>> data) {
        this.data = data;
    }

    // -------------------------------------------------------------------------
    // SvgTemplateLoader
    // -------------------------------------------------------------------------

    @Override
    public String getSvgTemplate(String feature, String id) {
        Map<String, String> featureMap = data.get(feature);
        if (featureMap == null) return null;
        return featureMap.get(id);
    }

    // -------------------------------------------------------------------------
    // Factories
    // -------------------------------------------------------------------------

    /**
     * Creates a loader from an already-parsed map.
     *
     * @param map feature → id → svg fragment; must not be {@code null}
     */
    public static JsonSvgTemplateLoader fromMap(
            Map<String, Map<String, String>> map) {
        if (map == null) throw new IllegalArgumentException("map must not be null");
        return new JsonSvgTemplateLoader(Collections.unmodifiableMap(
                new HashMap<>(map)));
    }

    /**
     * Parses the JSON string produced by extracting {@code svgs.js} from the
     * facesjs npm package and returns a loader backed by that data.
     *
     * <p>The parser handles the specific structure of the facesjs SVG JSON file:
     * a single JSON object whose values are objects mapping string IDs to SVG
     * fragment strings.  SVG fragments may contain escaped characters.
     *
     * @param json raw JSON string; must not be {@code null}
     * @return a populated loader
     * @throws IllegalArgumentException if the JSON cannot be parsed
     */
    public static JsonSvgTemplateLoader fromJson(String json) {
        if (json == null) throw new IllegalArgumentException("json must not be null");
        Map<String, Map<String, String>> result = parseJson(json.trim());
        return new JsonSvgTemplateLoader(result);
    }

    // -------------------------------------------------------------------------
    // Minimal JSON parser
    // -------------------------------------------------------------------------

    /**
     * Parses a JSON object of the shape {@code { "key": { "id": "value", ... }, ... }}
     * into a nested map.  String values may contain standard JSON escape sequences.
     */
    static Map<String, Map<String, String>> parseJson(String json) {
        Map<String, Map<String, String>> outer = new HashMap<>();
        int[] pos = { 0 };
        skipWhitespace(json, pos);
        expect(json, pos, '{');
        skipWhitespace(json, pos);

        while (pos[0] < json.length() && json.charAt(pos[0]) != '}') {
            String outerKey = parseString(json, pos);
            skipWhitespace(json, pos);
            expect(json, pos, ':');
            skipWhitespace(json, pos);
            Map<String, String> inner = parseStringMap(json, pos);
            outer.put(outerKey, inner);
            skipWhitespace(json, pos);
            if (pos[0] < json.length() && json.charAt(pos[0]) == ',') {
                pos[0]++;
                skipWhitespace(json, pos);
            }
        }
        return outer;
    }

    /** Parses an inner JSON object of the shape {@code { "key": "value", ... }}. */
    private static Map<String, String> parseStringMap(String json, int[] pos) {
        Map<String, String> map = new HashMap<>();
        expect(json, pos, '{');
        skipWhitespace(json, pos);

        while (pos[0] < json.length() && json.charAt(pos[0]) != '}') {
            String key = parseString(json, pos);
            skipWhitespace(json, pos);
            expect(json, pos, ':');
            skipWhitespace(json, pos);
            String value = parseString(json, pos);
            map.put(key, value);
            skipWhitespace(json, pos);
            if (pos[0] < json.length() && json.charAt(pos[0]) == ',') {
                pos[0]++;
                skipWhitespace(json, pos);
            }
        }
        expect(json, pos, '}');
        return map;
    }

    /**
     * Parses a JSON quoted string starting at {@code pos[0]}.
     * Handles {@code \"}, {@code \\}, {@code \/}, {@code \n}, {@code \r},
     * {@code \t}, and {@code \\uXXXX} escape sequences.
     */
    static String parseString(String json, int[] pos) {
        expect(json, pos, '"');
        StringBuilder sb = new StringBuilder();
        while (pos[0] < json.length()) {
            char c = json.charAt(pos[0]++);
            if (c == '"') break;
            if (c == '\\') {
                if (pos[0] >= json.length()) break;
                char esc = json.charAt(pos[0]++);
                switch (esc) {
                    case '"':  sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    case '/':  sb.append('/');  break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    case 'b':  sb.append('\b'); break;
                    case 'f':  sb.append('\f'); break;
                    case 'u':
                        if (pos[0] + 4 <= json.length()) {
                            String hex = json.substring(pos[0], pos[0] + 4);
                            pos[0] += 4;
                            sb.append((char) Integer.parseInt(hex, 16));
                        }
                        break;
                    default:   sb.append(esc);  break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static void skipWhitespace(String json, int[] pos) {
        while (pos[0] < json.length() && Character.isWhitespace(json.charAt(pos[0]))) {
            pos[0]++;
        }
    }

    private static void expect(String json, int[] pos, char ch) {
        if (pos[0] >= json.length() || json.charAt(pos[0]) != ch) {
            int ctx = Math.min(pos[0] + 40, json.length());
            throw new IllegalArgumentException(
                    "Expected '" + ch + "' at position " + pos[0]
                    + " but got: " + json.substring(pos[0], ctx));
        }
        pos[0]++;
    }
}
