package eb.framework1.face;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link FaceRuleLoader}, {@link FaceRule}, and
 * {@link FaceGenerator#defaultCharacterFace}.
 *
 * <p>All tests are pure-Java with no libGDX dependency.
 */
public class DefaultCharacterFaceTest {

    // =========================================================================
    // FaceRule builder
    // =========================================================================

    @Test
    public void faceRule_defaultBuilder_hasEmptyLists() {
        FaceRule r = new FaceRule.Builder().build();
        assertTrue(r.include.isEmpty());
        assertTrue(r.additional.isEmpty());
        assertTrue(r.exclude.isEmpty());
    }

    @Test
    public void faceRule_percentageClampedTo1_100() {
        FaceRule low  = new FaceRule.Builder().percentage(0).build();
        FaceRule high = new FaceRule.Builder().percentage(200).build();
        assertEquals(1,   low.percentage);
        assertEquals(100, high.percentage);
    }

    @Test
    public void faceRule_nullListsDefaultToEmpty() {
        FaceRule r = new FaceRule.Builder()
                .include(null).additional(null).exclude(null)
                .build();
        assertNotNull(r.include);
        assertNotNull(r.additional);
        assertNotNull(r.exclude);
    }

    // =========================================================================
    // FaceRuleLoader
    // =========================================================================

    private static final String SIMPLE_JSON = "{\n"
            + "  \"rules\": [\n"
            + "    {\n"
            + "      \"name\": \"Female\",\n"
            + "      \"gender\": \"female\",\n"
            + "      \"emotion\": \"\",\n"
            + "      \"minWealth\": 0,\n"
            + "      \"minAge\": 0,\n"
            + "      \"clothesType\": \"\",\n"
            + "      \"percentage\": 100,\n"
            + "      \"priority\": 5,\n"
            + "      \"include\": [\"eye.female1\", \"eye.female2\", \"hair.curly\"],\n"
            + "      \"additional\": [\"eye.female3\"],\n"
            + "      \"exclude\": [\"hair.bald\"]\n"
            + "    },\n"
            + "    {\n"
            + "      \"name\": \"Male\",\n"
            + "      \"gender\": \"male\",\n"
            + "      \"emotion\": \"\",\n"
            + "      \"minWealth\": 0,\n"
            + "      \"minAge\": 0,\n"
            + "      \"clothesType\": \"\",\n"
            + "      \"percentage\": 100,\n"
            + "      \"priority\": 3,\n"
            + "      \"include\": [\"eye.eye1\", \"eye.eye2\"],\n"
            + "      \"additional\": [],\n"
            + "      \"exclude\": []\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    @Test
    public void faceRuleLoader_parsesRulesCount() {
        List<FaceRule> rules = FaceRuleLoader.fromJson(SIMPLE_JSON);
        assertEquals(2, rules.size());
    }

    @Test
    public void faceRuleLoader_sortsByPriorityAscending() {
        List<FaceRule> rules = FaceRuleLoader.fromJson(SIMPLE_JSON);
        // priority 3 (Male) should come before priority 5 (Female)
        assertEquals("Male",   rules.get(0).name);
        assertEquals("Female", rules.get(1).name);
    }

    @Test
    public void faceRuleLoader_parsesFields() {
        List<FaceRule> rules = FaceRuleLoader.fromJson(SIMPLE_JSON);
        FaceRule female = rules.get(1); // priority 5
        assertEquals("Female",  female.name);
        assertEquals("female",  female.gender);
        assertEquals(100,       female.percentage);
        assertEquals(5,         female.priority);
        assertEquals(3,         female.include.size());
        assertEquals(1,         female.additional.size());
        assertEquals("eye.female3", female.additional.get(0));
        assertEquals(1,         female.exclude.size());
        assertEquals("hair.bald", female.exclude.get(0));
    }

    @Test
    public void faceRuleLoader_missingAdditional_defaultsToEmpty() {
        String json = "{\n"
                + "  \"rules\": [{\n"
                + "    \"name\": \"Minimal\",\n"
                + "    \"gender\": \"\",\n"
                + "    \"emotion\": \"\",\n"
                + "    \"minWealth\": 0,\n"
                + "    \"minAge\": 0,\n"
                + "    \"clothesType\": \"\",\n"
                + "    \"percentage\": 100,\n"
                + "    \"priority\": 0,\n"
                + "    \"include\": [\"eye.eye1\"],\n"
                + "    \"exclude\": []\n"
                + "  }]\n"
                + "}";
        List<FaceRule> rules = FaceRuleLoader.fromJson(json);
        assertEquals(1, rules.size());
        assertTrue("additional should default to empty", rules.get(0).additional.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void faceRuleLoader_nullJson_throws() {
        FaceRuleLoader.fromJson(null);
    }

    // =========================================================================
    // defaultCharacterFace — basic behaviour
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void defaultCharacterFace_nullRules_throws() {
        FaceGenerator.defaultCharacterFace(42L, "male", 30, null);
    }

    @Test
    public void defaultCharacterFace_emptyRules_returnsEmptyMap() {
        Map<String, List<String>> result =
                FaceGenerator.defaultCharacterFace(1L, "male", 25, Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void defaultCharacterFace_genderFilter_maleSeesMaleRule() {
        List<FaceRule> rules = FaceRuleLoader.fromJson(SIMPLE_JSON);
        Map<String, List<String>> pool =
                FaceGenerator.defaultCharacterFace(1L, "male", 30, rules);

        // Male rule (priority 3): include eye.eye1, eye.eye2
        assertTrue("male should have eye type", pool.containsKey("eye"));
        assertTrue(pool.get("eye").contains("eye1"));
        assertTrue(pool.get("eye").contains("eye2"));
        // Female eye IDs must not appear
        assertFalse("female eye ids should not appear",
                pool.get("eye").contains("female1"));
    }

    @Test
    public void defaultCharacterFace_genderFilter_femaleSeesFemaleRule() {
        List<FaceRule> rules = FaceRuleLoader.fromJson(SIMPLE_JSON);
        Map<String, List<String>> pool =
                FaceGenerator.defaultCharacterFace(1L, "female", 30, rules);

        // Female rule (priority 5): include eye.female1, eye.female2 + additional eye.female3
        assertTrue(pool.containsKey("eye"));
        List<String> eyePool = pool.get("eye");
        assertTrue(eyePool.contains("female1"));
        assertTrue(eyePool.contains("female2"));
        assertTrue("additional eye.female3 should be included", eyePool.contains("female3"));

        // hair.curly should be present; hair.bald should be excluded
        assertTrue(pool.containsKey("hair"));
        assertTrue(pool.get("hair").contains("curly"));
        assertFalse("bald should be excluded", pool.get("hair").contains("bald"));
    }

    @Test
    public void defaultCharacterFace_caseInsensitiveGender() {
        List<FaceRule> rules = FaceRuleLoader.fromJson(SIMPLE_JSON);
        Map<String, List<String>> upper =
                FaceGenerator.defaultCharacterFace(1L, "FEMALE", 30, rules);
        Map<String, List<String>> lower =
                FaceGenerator.defaultCharacterFace(1L, "female", 30, rules);
        assertEquals(upper.keySet(), lower.keySet());
    }

    @Test
    public void defaultCharacterFace_minAgeFilter_youngCharacterSkipsOldRule() {
        String json = "{\n"
                + "  \"rules\": [{\n"
                + "    \"name\": \"Old\",\n"
                + "    \"gender\": \"\",\n"
                + "    \"emotion\": \"\",\n"
                + "    \"minWealth\": 0,\n"
                + "    \"minAge\": 60,\n"
                + "    \"clothesType\": \"\",\n"
                + "    \"percentage\": 100,\n"
                + "    \"priority\": 0,\n"
                + "    \"include\": [\"hair.bald\"],\n"
                + "    \"additional\": [],\n"
                + "    \"exclude\": []\n"
                + "  }]\n"
                + "}";
        List<FaceRule> rules = FaceRuleLoader.fromJson(json);

        Map<String, List<String>> youngPool =
                FaceGenerator.defaultCharacterFace(1L, "male", 30, rules);
        // Rule requires minAge=60, character is 30 → rule skipped → no hair type
        assertFalse(youngPool.containsKey("hair"));

        Map<String, List<String>> elderPool =
                FaceGenerator.defaultCharacterFace(1L, "male", 65, rules);
        assertTrue(elderPool.containsKey("hair"));
        assertTrue(elderPool.get("hair").contains("bald"));
    }

    // =========================================================================
    // defaultCharacterFace — priority and merging
    // =========================================================================

    @Test
    public void defaultCharacterFace_higherPriorityIncludeReplacesLower() {
        // Priority 1 rule includes eye.eye1; Priority 2 rule (higher) includes eye.eye2.
        // The higher-priority include should REPLACE the lower one.
        String json = "{\n"
                + "  \"rules\": [\n"
                + "    {\n"
                + "      \"name\": \"Male\", \"gender\": \"\", \"emotion\": \"\",\n"
                + "      \"minWealth\": 0, \"minAge\": 0, \"clothesType\": \"\",\n"
                + "      \"percentage\": 100, \"priority\": 1,\n"
                + "      \"include\": [\"eye.eye1\"], \"additional\": [], \"exclude\": []\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"Female\", \"gender\": \"\", \"emotion\": \"\",\n"
                + "      \"minWealth\": 0, \"minAge\": 0, \"clothesType\": \"\",\n"
                + "      \"percentage\": 100, \"priority\": 2,\n"
                + "      \"include\": [\"eye.eye2\"], \"additional\": [], \"exclude\": []\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        List<FaceRule> rules = FaceRuleLoader.fromJson(json);
        Map<String, List<String>> pool =
                FaceGenerator.defaultCharacterFace(1L, "male", 30, rules);

        // Higher-priority include replaces lower-priority include for same type
        List<String> eyePool = pool.get("eye");
        assertNotNull(eyePool);
        assertFalse("eye1 from lower-priority include should be replaced",
                eyePool.contains("eye1"));
        assertTrue("eye2 from higher-priority include should be present",
                eyePool.contains("eye2"));
    }

    @Test
    public void defaultCharacterFace_additionalAccumulatesAcrossRules() {
        String json = "{\n"
                + "  \"rules\": [\n"
                + "    {\n"
                + "      \"name\": \"Male\", \"gender\": \"\", \"emotion\": \"\",\n"
                + "      \"minWealth\": 0, \"minAge\": 0, \"clothesType\": \"\",\n"
                + "      \"percentage\": 100, \"priority\": 1,\n"
                + "      \"include\": [], \"additional\": [\"eye.eye1\"], \"exclude\": []\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"Female\", \"gender\": \"\", \"emotion\": \"\",\n"
                + "      \"minWealth\": 0, \"minAge\": 0, \"clothesType\": \"\",\n"
                + "      \"percentage\": 100, \"priority\": 2,\n"
                + "      \"include\": [], \"additional\": [\"eye.eye2\"], \"exclude\": []\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        List<FaceRule> rules = FaceRuleLoader.fromJson(json);
        Map<String, List<String>> pool =
                FaceGenerator.defaultCharacterFace(1L, "male", 30, rules);

        List<String> eyePool = pool.get("eye");
        assertNotNull(eyePool);
        assertTrue("eye1 from rule A additional should accumulate", eyePool.contains("eye1"));
        assertTrue("eye2 from rule B additional should accumulate", eyePool.contains("eye2"));
    }

    @Test
    public void defaultCharacterFace_excludeRemovesFromBothIncludeAndAdditional() {
        String json = "{\n"
                + "  \"rules\": [\n"
                + "    {\n"
                + "      \"name\": \"Male\", \"gender\": \"\", \"emotion\": \"\",\n"
                + "      \"minWealth\": 0, \"minAge\": 0, \"clothesType\": \"\",\n"
                + "      \"percentage\": 100, \"priority\": 1,\n"
                + "      \"include\": [\"hair.short\", \"hair.bald\"],\n"
                + "      \"additional\": [\"hair.curly\"],\n"
                + "      \"exclude\": []\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"Female\", \"gender\": \"\", \"emotion\": \"\",\n"
                + "      \"minWealth\": 0, \"minAge\": 0, \"clothesType\": \"\",\n"
                + "      \"percentage\": 100, \"priority\": 2,\n"
                + "      \"include\": [], \"additional\": [],\n"
                + "      \"exclude\": [\"hair.bald\", \"hair.curly\"]\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        List<FaceRule> rules = FaceRuleLoader.fromJson(json);
        Map<String, List<String>> pool =
                FaceGenerator.defaultCharacterFace(1L, "male", 30, rules);

        List<String> hairPool = pool.get("hair");
        assertNotNull(hairPool);
        assertTrue("short should remain",  hairPool.contains("short"));
        assertFalse("bald should be excluded",  hairPool.contains("bald"));
        assertFalse("curly should be excluded", hairPool.contains("curly"));
    }

    @Test
    public void defaultCharacterFace_deterministic_sameSeedSameResult() {
        List<FaceRule> rules = FaceRuleLoader.fromJson(SIMPLE_JSON);
        Map<String, List<String>> pool1 =
                FaceGenerator.defaultCharacterFace(99L, "female", 40, rules);
        Map<String, List<String>> pool2 =
                FaceGenerator.defaultCharacterFace(99L, "female", 40, rules);
        assertEquals(pool1, pool2);
    }

    @Test
    public void defaultCharacterFace_percentageSkipRule_variesBySeed() {
        // A rule with percentage=50 should fire for some seeds and not others.
        String json = "{\n"
                + "  \"rules\": [{\n"
                + "    \"name\": \"Male\", \"gender\": \"\", \"emotion\": \"\",\n"
                + "    \"minWealth\": 0, \"minAge\": 0, \"clothesType\": \"\",\n"
                + "    \"percentage\": 50, \"priority\": 0,\n"
                + "    \"include\": [\"eye.eye99\"], \"additional\": [], \"exclude\": []\n"
                + "  }]\n"
                + "}";
        List<FaceRule> rules = FaceRuleLoader.fromJson(json);

        int fired = 0;
        for (long seed = 0; seed < 100; seed++) {
            Map<String, List<String>> pool =
                    FaceGenerator.defaultCharacterFace(seed, "male", 30, rules);
            if (pool.containsKey("eye")) fired++;
        }
        // With 50% probability over 100 seeds we expect between 20 and 80 fires
        assertTrue("expected rule to fire roughly half the time, got " + fired,
                fired > 20 && fired < 80);
    }

    @Test
    public void defaultCharacterFace_returnedMapIsUnmodifiable() {
        List<FaceRule> rules = FaceRuleLoader.fromJson(SIMPLE_JSON);
        Map<String, List<String>> pool =
                FaceGenerator.defaultCharacterFace(1L, "female", 30, rules);
        try {
            pool.put("newType", new ArrayList<>());
            fail("map should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
        if (!pool.isEmpty()) {
            String key = pool.keySet().iterator().next();
            try {
                pool.get(key).add("hack");
                fail("value list should be unmodifiable");
            } catch (UnsupportedOperationException expected) {
                // expected
            }
        }
    }

    @Test
    public void defaultCharacterFace_allExcluded_typeNotInResult() {
        String json = "{\n"
                + "  \"rules\": [{\n"
                + "    \"name\": \"Male\", \"gender\": \"\", \"emotion\": \"\",\n"
                + "    \"minWealth\": 0, \"minAge\": 0, \"clothesType\": \"\",\n"
                + "    \"percentage\": 100, \"priority\": 0,\n"
                + "    \"include\": [\"eye.eye1\"],\n"
                + "    \"additional\": [],\n"
                + "    \"exclude\": [\"eye.eye1\"]\n"
                + "  }]\n"
                + "}";
        List<FaceRule> rules = FaceRuleLoader.fromJson(json);
        Map<String, List<String>> pool =
                FaceGenerator.defaultCharacterFace(1L, "male", 30, rules);
        assertFalse("eye type should not appear when all IDs are excluded",
                pool.containsKey("eye"));
    }

    // =========================================================================
    // defaultCharacterFace — emotion filter
    // =========================================================================

    @Test
    public void defaultCharacterFace_emotionFilter_normalRuleFires() {
        // A rule with emotion="normal" should fire for defaultCharacterFace
        String json = "{\n"
                + "  \"rules\": [{\n"
                + "    \"name\": \"Male\", \"gender\": \"\", \"emotion\": \"normal\",\n"
                + "    \"minWealth\": 0, \"minAge\": 0, \"clothesType\": \"\",\n"
                + "    \"percentage\": 100, \"priority\": 0,\n"
                + "    \"include\": [\"mouth.smile\"], \"additional\": [], \"exclude\": []\n"
                + "  }]\n"
                + "}";
        List<FaceRule> rules = FaceRuleLoader.fromJson(json);
        Map<String, List<String>> pool =
                FaceGenerator.defaultCharacterFace(1L, "male", 30, rules);
        assertTrue("normal emotion rule should fire", pool.containsKey("mouth"));
        assertTrue(pool.get("mouth").contains("smile"));
    }

    @Test
    public void defaultCharacterFace_emotionFilter_emptyEmotionRuleFires() {
        // A rule with emotion="" (any) should also fire for defaultCharacterFace
        String json = "{\n"
                + "  \"rules\": [{\n"
                + "    \"name\": \"Male\", \"gender\": \"\", \"emotion\": \"\",\n"
                + "    \"minWealth\": 0, \"minAge\": 0, \"clothesType\": \"\",\n"
                + "    \"percentage\": 100, \"priority\": 0,\n"
                + "    \"include\": [\"mouth.closed\"], \"additional\": [], \"exclude\": []\n"
                + "  }]\n"
                + "}";
        List<FaceRule> rules = FaceRuleLoader.fromJson(json);
        Map<String, List<String>> pool =
                FaceGenerator.defaultCharacterFace(1L, "male", 30, rules);
        assertTrue("empty emotion rule should fire", pool.containsKey("mouth"));
        assertTrue(pool.get("mouth").contains("closed"));
    }

    @Test
    public void defaultCharacterFace_emotionFilter_happyRuleSkipped() {
        // A rule with emotion="happy" must NOT fire for defaultCharacterFace
        String json = "{\n"
                + "  \"rules\": [{\n"
                + "    \"name\": \"Male\", \"gender\": \"\", \"emotion\": \"happy\",\n"
                + "    \"minWealth\": 0, \"minAge\": 0, \"clothesType\": \"\",\n"
                + "    \"percentage\": 100, \"priority\": 0,\n"
                + "    \"include\": [\"mouth.smile\"], \"additional\": [], \"exclude\": []\n"
                + "  }]\n"
                + "}";
        List<FaceRule> rules = FaceRuleLoader.fromJson(json);
        Map<String, List<String>> pool =
                FaceGenerator.defaultCharacterFace(1L, "male", 30, rules);
        assertFalse("happy emotion rule should not fire for default face",
                pool.containsKey("mouth"));
    }

    @Test
    public void defaultCharacterFace_emotionFilter_angryRuleSkipped() {
        String json = "{\n"
                + "  \"rules\": [{\n"
                + "    \"name\": \"Male\", \"gender\": \"\", \"emotion\": \"angry\",\n"
                + "    \"minWealth\": 0, \"minAge\": 0, \"clothesType\": \"\",\n"
                + "    \"percentage\": 100, \"priority\": 0,\n"
                + "    \"include\": [\"mouth.angry\"], \"additional\": [], \"exclude\": []\n"
                + "  }]\n"
                + "}";
        List<FaceRule> rules = FaceRuleLoader.fromJson(json);
        Map<String, List<String>> pool =
                FaceGenerator.defaultCharacterFace(1L, "male", 30, rules);
        assertFalse("angry emotion rule should not fire for default face",
                pool.containsKey("mouth"));
    }

    @Test
    public void defaultCharacterFace_emotionFilter_caseInsensitiveNormal() {
        // "Normal" and "NORMAL" should both be treated as the normal emotion
        String json = "{\n"
                + "  \"rules\": [{\n"
                + "    \"name\": \"Male\", \"gender\": \"\", \"emotion\": \"NORMAL\",\n"
                + "    \"minWealth\": 0, \"minAge\": 0, \"clothesType\": \"\",\n"
                + "    \"percentage\": 100, \"priority\": 0,\n"
                + "    \"include\": [\"eye.eye1\"], \"additional\": [], \"exclude\": []\n"
                + "  }]\n"
                + "}";
        List<FaceRule> rules = FaceRuleLoader.fromJson(json);
        Map<String, List<String>> pool =
                FaceGenerator.defaultCharacterFace(1L, "male", 30, rules);
        assertTrue("NORMAL (uppercase) emotion rule should fire", pool.containsKey("eye"));
    }

    // =========================================================================
    // FaceGenerator.generate(Options, pool) — pool-based generation
    // =========================================================================

    @Test
    public void generateWithPool_picksIdFromPool() {
        // Given a pool with a single eye ID, generate() must use it
        Map<String, List<String>> pool = new java.util.HashMap<>();
        pool.put("eye", Collections.singletonList("eye5"));

        FaceGenerator gen = new FaceGenerator(new java.util.Random(42));
        FaceConfig face = gen.generate(new FaceGenerator.Options().gender("male"), pool);
        assertEquals("eye5", face.eye.id);
    }

    @Test
    public void generateWithPool_nullPoolFallsBackToRandom() {
        // Null pool → should still produce a valid face
        FaceGenerator gen = new FaceGenerator(new java.util.Random(42));
        FaceConfig face = gen.generate(new FaceGenerator.Options().gender("female"), null);
        assertNotNull(face);
        assertNotNull(face.eye.id);
        assertFalse(face.eye.id.isEmpty());
    }

    @Test
    public void generateWithPool_emptyPoolFallsBackToRandom() {
        // Empty pool → standard random generation
        FaceGenerator gen = new FaceGenerator(new java.util.Random(42));
        FaceConfig face1 = gen.generate(new FaceGenerator.Options().gender("male"),
                Collections.<String, List<String>>emptyMap());
        assertNotNull(face1);
    }

    @Test
    public void generateWithPool_missingFeatureUsesRandom() {
        // Pool has "eye" but no "mouth" → mouth defaults to "none" (pool-only constraint).
        Map<String, List<String>> pool = new java.util.HashMap<>();
        pool.put("eye", Collections.singletonList("eye3"));

        FaceGenerator gen = new FaceGenerator(new java.util.Random(99));
        FaceConfig face = gen.generate(new FaceGenerator.Options().gender("male"), pool);
        assertEquals("eye3", face.eye.id);
        assertNotNull("mouth id should not be null", face.mouth.id);
        assertFalse(face.mouth.id.isEmpty());
    }

    @Test
    public void generateWithPool_jerseyAndAccessoriesDefaultToNone() {
        // When pool contains no "jersey", "accessories", or "glasses" entries, all must be "none".
        // This ensures clothes/headbands/glasses are never rendered without an explicit rule.
        Map<String, List<String>> pool = new java.util.HashMap<>();
        pool.put("eye", Collections.singletonList("eye1"));

        // Run many seeds to make sure the fallback is always "none"
        for (int seed = 0; seed < 50; seed++) {
            FaceGenerator gen = new FaceGenerator(new java.util.Random(seed));
            FaceConfig face = gen.generate(new FaceGenerator.Options().gender("male"), pool);
            assertEquals("jersey must be 'none' when not in pool", "none", face.jersey.id);
            assertEquals("accessories must be 'none' when not in pool", "none", face.accessories.id);
            assertEquals("glasses must be 'none' when not in pool", "none", face.glasses.id);
        }
    }

    @Test
    public void generateWithPool_jerseyFromPoolIsUsed() {
        // When pool explicitly provides "jersey", that ID is used.
        Map<String, List<String>> pool = new java.util.HashMap<>();
        pool.put("jersey", Collections.singletonList("jersey3"));

        FaceGenerator gen = new FaceGenerator(new java.util.Random(42));
        FaceConfig face = gen.generate(new FaceGenerator.Options().gender("male"), pool);
        assertEquals("jersey3", face.jersey.id);
    }

    @Test
    public void generateWithPool_glassesFromPoolIsUsed() {
        // When pool explicitly provides "glasses", that ID is used.
        Map<String, List<String>> pool = new java.util.HashMap<>();
        pool.put("glasses", Collections.singletonList("glasses1"));

        FaceGenerator gen = new FaceGenerator(new java.util.Random(42));
        FaceConfig face = gen.generate(new FaceGenerator.Options().gender("male"), pool);
        assertEquals("glasses1", face.glasses.id);
    }

    // =========================================================================
    // FaceGenerator.generate(Options, pool) — mouth and miscLine pool-only
    // =========================================================================

    @Test
    public void generateWithPool_mouthNotInPool_defaultsToNone() {
        // When the pool has no "mouth" entry, mouth must be "none" (not a random
        // pick from the full catalogue).  This ensures only rules can supply mouths.
        Map<String, List<String>> pool = new java.util.HashMap<>();
        pool.put("eye", Collections.singletonList("eye1"));

        for (int seed = 0; seed < 50; seed++) {
            FaceGenerator gen = new FaceGenerator(new java.util.Random(seed));
            FaceConfig face = gen.generate(new FaceGenerator.Options().gender("male"), pool);
            assertEquals("mouth must be 'none' when not in pool (seed=" + seed + ")",
                    "none", face.mouth.id);
        }
    }

    @Test
    public void generateWithPool_mouthFromPoolIsUsed() {
        // When pool explicitly provides "mouth", only those IDs should appear.
        Map<String, List<String>> pool = new java.util.HashMap<>();
        pool.put("mouth", Arrays.asList("mouth2", "mouth3", "mouth4"));

        for (int seed = 0; seed < 30; seed++) {
            FaceGenerator gen = new FaceGenerator(new java.util.Random(seed));
            FaceConfig face = gen.generate(new FaceGenerator.Options().gender("male"), pool);
            assertTrue("mouth id must come from pool, was: " + face.mouth.id,
                    face.mouth.id.equals("mouth2")
                    || face.mouth.id.equals("mouth3")
                    || face.mouth.id.equals("mouth4"));
        }
    }

    @Test
    public void generateWithPool_miscLineNotInPool_defaultsToNone() {
        // When the pool has no "miscLine" entry, miscLine must be "none" (not a
        // random pick from the full catalogue).
        Map<String, List<String>> pool = new java.util.HashMap<>();
        pool.put("eye", Collections.singletonList("eye1"));

        for (int seed = 0; seed < 50; seed++) {
            FaceGenerator gen = new FaceGenerator(new java.util.Random(seed));
            FaceConfig face = gen.generate(new FaceGenerator.Options().gender("female"), pool);
            assertEquals("miscLine must be 'none' when not in pool (seed=" + seed + ")",
                    "none", face.miscLine.id);
        }
    }

    @Test
    public void generateWithPool_miscLineFromPoolIsUsed() {
        // When pool explicitly provides "miscLine", only those IDs should appear.
        Map<String, List<String>> pool = new java.util.HashMap<>();
        pool.put("miscLine", Arrays.asList("freckles1", "freckles2"));

        for (int seed = 0; seed < 30; seed++) {
            FaceGenerator gen = new FaceGenerator(new java.util.Random(seed));
            FaceConfig face = gen.generate(new FaceGenerator.Options().gender("female"), pool);
            assertTrue("miscLine id must come from pool, was: " + face.miscLine.id,
                    face.miscLine.id.equals("freckles1")
                    || face.miscLine.id.equals("freckles2"));
        }
    }
}
