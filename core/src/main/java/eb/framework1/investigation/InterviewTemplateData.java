package eb.framework1.investigation;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds all text pools used by {@link InterviewTemplateEngine}, loaded from
 * {@code assets/text/interview_templates_en.json}.
 *
 * <p>Pool keys follow the naming convention {@code npcRole.topic[.variant]}, e.g.
 * {@code client.alibi}, {@code subject.motive.deflect}.  Word sub-pools are
 * prefixed with {@code words.}, e.g. {@code words.hobby}.
 *
 * <p>{@link #parse(String)} accepts raw JSON text and uses the libGDX
 * {@code JsonReader} which does <em>not</em> require {@code Gdx} to be
 * initialised — it is a pure in-memory parser.
 */
public class InterviewTemplateData {

    private final Map<String, List<String>> pools;
    private final Map<String, String>       questions;

    private InterviewTemplateData(Map<String, List<String>> pools,
                                   Map<String, String> questions) {
        this.pools     = pools;
        this.questions = questions;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the pool for the given key, or an empty list if the key is not
     * present.
     *
     * @param key pool key, e.g. {@code "client.alibi"} or {@code "words.hobby"}
     * @return immutable view of the pool entries (may be empty, never null)
     */
    public List<String> getPool(String key) {
        List<String> pool = pools.get(key);
        return pool != null ? Collections.unmodifiableList(pool)
                            : Collections.<String>emptyList();
    }

    /**
     * Returns the question string for the given key after resolving any
     * {@code $placeholder} tokens that appear in it, or {@code ""} if the
     * key is absent.
     *
     * @param key       question key, e.g. {@code "client.motive"}
     * @param vars      placeholder→value substitutions
     * @param data      the same data instance (needed for word-pool tokens)
     * @param random    random source for word-pool tokens
     * @return resolved question string
     */
    public String getQuestion(String key, Map<String, String> vars,
                              InterviewTemplateData data, java.util.Random random) {
        String q = questions.get(key);
        if (q == null) return "";
        return TemplateResolver.resolve(q, vars, data, random);
    }

    /** Raw (unresolved) question string for the given key; {@code ""} if absent. */
    public String getRawQuestion(String key) {
        String q = questions.get(key);
        return q != null ? q : "";
    }

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    /**
     * Parses an {@link InterviewTemplateData} from raw JSON text.
     *
     * <p>Expected top-level structure:
     * <pre>
     * {
     *   "pools": {
     *     "client.alibi": ["...", ...],
     *     "words.hobby":  ["...", ...],
     *     ...
     *   },
     *   "questions": {
     *     "alibi": "...",
     *     ...
     *   }
     * }
     * </pre>
     *
     * @param json raw UTF-8 JSON text
     * @return parsed data instance
     * @throws RuntimeException if the JSON cannot be parsed
     */
    public static InterviewTemplateData parse(String json) {
        Map<String, List<String>> pools     = new HashMap<String, List<String>>();
        Map<String, String>       questions = new HashMap<String, String>();

        JsonReader reader = new JsonReader();
        JsonValue  root   = reader.parse(json);

        JsonValue poolsNode = root.get("pools");
        if (poolsNode != null) {
            for (JsonValue entry = poolsNode.child; entry != null; entry = entry.next) {
                String key = entry.name();
                List<String> list = new ArrayList<String>();
                for (JsonValue item = entry.child; item != null; item = item.next) {
                    list.add(item.asString());
                }
                pools.put(key, list);
            }
        }

        JsonValue questionsNode = root.get("questions");
        if (questionsNode != null) {
            for (JsonValue entry = questionsNode.child; entry != null; entry = entry.next) {
                questions.put(entry.name(), entry.asString());
            }
        }

        return new InterviewTemplateData(pools, questions);
    }
}
