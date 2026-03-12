package eb.framework1.face;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parses a {@code facerules.json} file into a list of {@link FaceRule} objects.
 *
 * <p>The expected JSON structure is:
 * <pre>
 * {
 *   "rules": [
 *     {
 *       "name":        "Female",
 *       "gender":      "female",
 *       "emotion":     "",
 *       "minWealth":   0,
 *       "minAge":      0,
 *       "clothesType": "",
 *       "percentage":  100,
 *       "priority":    0,
 *       "include":    [ "eye.female1", ... ],
 *       "additional": [ "eye.female5", ... ],
 *       "exclude":    [ "hair.bald", ... ]
 *     },
 *     ...
 *   ]
 * }
 * </pre>
 *
 * <p>The {@code "additional"} key is optional and defaults to an empty list,
 * providing backward compatibility with older files that do not contain it.
 *
 * <p>This loader has <strong>no libGDX dependency</strong> and can be used in
 * plain-Java unit tests.
 */
public final class FaceRuleLoader {

    private FaceRuleLoader() {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Parses a {@code facerules.json} string and returns a list of
     * {@link FaceRule} objects sorted ascending by priority.
     *
     * @param json raw JSON string; must not be {@code null}
     * @return unmodifiable list of face rules, sorted by priority ascending
     * @throws IllegalArgumentException if the JSON cannot be parsed
     */
    public static List<FaceRule> fromJson(String json) {
        if (json == null) throw new IllegalArgumentException("json must not be null");
        int[] pos = { 0 };
        skipWhitespace(json, pos);
        expect(json, pos, '{');
        skipWhitespace(json, pos);

        List<FaceRule> rules = new ArrayList<>();

        while (pos[0] < json.length() && json.charAt(pos[0]) != '}') {
            String key = parseString(json, pos);
            skipWhitespace(json, pos);
            expect(json, pos, ':');
            skipWhitespace(json, pos);

            if ("rules".equals(key)) {
                rules = parseRulesArray(json, pos);
            } else {
                // Skip unknown top-level values
                skipValue(json, pos);
            }

            skipWhitespace(json, pos);
            if (pos[0] < json.length() && json.charAt(pos[0]) == ',') {
                pos[0]++;
                skipWhitespace(json, pos);
            }
        }

        rules.sort((a, b) -> Integer.compare(a.priority, b.priority));
        return Collections.unmodifiableList(rules);
    }

    // -------------------------------------------------------------------------
    // Private JSON parsing
    // -------------------------------------------------------------------------

    private static List<FaceRule> parseRulesArray(String json, int[] pos) {
        List<FaceRule> list = new ArrayList<>();
        expect(json, pos, '[');
        skipWhitespace(json, pos);

        while (pos[0] < json.length() && json.charAt(pos[0]) != ']') {
            FaceRule rule = parseRule(json, pos);
            list.add(rule);
            skipWhitespace(json, pos);
            if (pos[0] < json.length() && json.charAt(pos[0]) == ',') {
                pos[0]++;
                skipWhitespace(json, pos);
            }
        }
        expect(json, pos, ']');
        return list;
    }

    private static FaceRule parseRule(String json, int[] pos) {
        FaceRule.Builder b = new FaceRule.Builder();
        expect(json, pos, '{');
        skipWhitespace(json, pos);

        while (pos[0] < json.length() && json.charAt(pos[0]) != '}') {
            String key = parseString(json, pos);
            skipWhitespace(json, pos);
            expect(json, pos, ':');
            skipWhitespace(json, pos);

            switch (key) {
                case "name":        b.name(parseString(json, pos));             break;
                case "gender":      b.gender(parseString(json, pos));           break;
                case "emotion":     b.emotion(parseString(json, pos));          break;
                case "minWealth":   b.minWealth(parseInt(json, pos));           break;
                case "minAge":      b.minAge(parseInt(json, pos));              break;
                case "clothesType": b.clothesType(parseString(json, pos));      break;
                case "percentage":  b.percentage(parseInt(json, pos));          break;
                case "priority":    b.priority(parseInt(json, pos));            break;
                case "include":     b.include(parseStringArray(json, pos));     break;
                case "additional":  b.additional(parseStringArray(json, pos));  break;
                case "exclude":     b.exclude(parseStringArray(json, pos));     break;
                default:            skipValue(json, pos);                       break;
            }

            skipWhitespace(json, pos);
            if (pos[0] < json.length() && json.charAt(pos[0]) == ',') {
                pos[0]++;
                skipWhitespace(json, pos);
            }
        }
        expect(json, pos, '}');
        return b.build();
    }

    /** Parses a JSON array of strings, e.g. {@code ["a", "b"]}. */
    private static List<String> parseStringArray(String json, int[] pos) {
        List<String> list = new ArrayList<>();
        expect(json, pos, '[');
        skipWhitespace(json, pos);

        while (pos[0] < json.length() && json.charAt(pos[0]) != ']') {
            list.add(parseString(json, pos));
            skipWhitespace(json, pos);
            if (pos[0] < json.length() && json.charAt(pos[0]) == ',') {
                pos[0]++;
                skipWhitespace(json, pos);
            }
        }
        expect(json, pos, ']');
        return list;
    }

    /** Parses a JSON integer (optionally negative). */
    private static int parseInt(String json, int[] pos) {
        skipWhitespace(json, pos);
        StringBuilder sb = new StringBuilder();
        if (pos[0] < json.length() && json.charAt(pos[0]) == '-') {
            sb.append('-');
            pos[0]++;
        }
        while (pos[0] < json.length() && Character.isDigit(json.charAt(pos[0]))) {
            sb.append(json.charAt(pos[0]++));
        }
        if (sb.length() == 0 || "-".equals(sb.toString())) {
            throw new IllegalArgumentException("Expected integer at position " + pos[0]);
        }
        return Integer.parseInt(sb.toString());
    }

    /**
     * Skips over an arbitrary JSON value (string, number, boolean, null,
     * object, or array) to allow forward-compatible parsing.
     */
    private static void skipValue(String json, int[] pos) {
        skipWhitespace(json, pos);
        if (pos[0] >= json.length()) return;
        char c = json.charAt(pos[0]);
        if (c == '"') {
            parseString(json, pos);
        } else if (c == '{') {
            skipObject(json, pos);
        } else if (c == '[') {
            skipArray(json, pos);
        } else {
            // number, boolean, null
            while (pos[0] < json.length()) {
                char ch = json.charAt(pos[0]);
                if (ch == ',' || ch == '}' || ch == ']' || Character.isWhitespace(ch)) break;
                pos[0]++;
            }
        }
    }

    private static void skipObject(String json, int[] pos) {
        expect(json, pos, '{');
        int depth = 1;
        while (pos[0] < json.length() && depth > 0) {
            char ch = json.charAt(pos[0]++);
            if (ch == '"') {
                // rewind, then parse and discard the string
                pos[0]--;
                parseString(json, pos);
            } else if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
            }
        }
    }

    private static void skipArray(String json, int[] pos) {
        expect(json, pos, '[');
        int depth = 1;
        while (pos[0] < json.length() && depth > 0) {
            char ch = json.charAt(pos[0]++);
            if (ch == '"') {
                pos[0]--;
                parseString(json, pos);
            } else if (ch == '[') {
                depth++;
            } else if (ch == ']') {
                depth--;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Shared string / whitespace helpers
    // -------------------------------------------------------------------------

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
            int contextEnd = Math.min(pos[0] + 40, json.length());
            throw new IllegalArgumentException(
                    "Expected '" + ch + "' at position " + pos[0]
                    + " but got: " + json.substring(pos[0], contextEnd));
        }
        pos[0]++;
    }
}
