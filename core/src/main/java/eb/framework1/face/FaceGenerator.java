package eb.framework1.face;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Generates random {@link FaceConfig} objects, ported from the
 * <a href="https://github.com/zengm-games/facesjs">facesjs</a> JavaScript
 * {@code generate()} function.
 *
 * <p>Call {@link #generate()} for a random male face, or use the builder-style
 * {@link Options} to specify gender, race, and optional overrides.
 *
 * <h3>Example</h3>
 * <pre>
 *   FaceGenerator gen = new FaceGenerator(new Random(42));
 *   FaceConfig face = gen.generate();                          // random male
 *   FaceConfig f2   = gen.generate(new FaceGenerator.Options().gender("female").race("asian"));
 * </pre>
 *
 * <p>The generator has <strong>no libGDX dependency</strong> and can be used in
 * plain-Java unit tests.
 */
public final class FaceGenerator {

    // -------------------------------------------------------------------------
    // SVG feature ID catalogs (from svgs-index.js)
    // -------------------------------------------------------------------------

    /** Available IDs per feature, parallel to {@link #SVG_GENDERS}. */
    private static final String[][] SVG_IDS = {
        // accessories
        { "eye-black","hat","hat2","hat3","headband-high","headband","none" },
        // body
        { "body","body2","body3","body4","body5" },
        // ear
        { "ear1","ear2","ear3" },
        // eye
        { "eye1","eye10","eye11","eye12","eye13","eye14","eye15","eye16","eye17","eye18","eye19",
          "eye2","eye3","eye4","eye5","eye6","eye7","eye8","eye9",
          "female1","female10","female11","female12","female13","female14","female15","female16",
          "female2","female3","female4","female5","female6","female7","female8","female9" },
        // eyeLine
        { "line1","line2","line3","line4","line5","line6","none" },
        // eyebrow
        { "eyebrow1","eyebrow10","eyebrow11","eyebrow12","eyebrow13","eyebrow14","eyebrow15",
          "eyebrow16","eyebrow17","eyebrow18","eyebrow19","eyebrow2","eyebrow20","eyebrow3",
          "eyebrow4","eyebrow5","eyebrow6","eyebrow7","eyebrow8","eyebrow9",
          "female1","female10","female2","female3","female4","female5","female6","female7",
          "female8","female9" },
        // facialHair
        { "beard-point","beard1","beard2","beard3","beard4","beard5","beard6","chin-strap",
          "chin-strapStache","fullgoatee","fullgoatee2","fullgoatee3","fullgoatee4","fullgoatee5",
          "fullgoatee6","goatee-thin-stache","goatee-thin","goatee1-stache","goatee1","goatee10",
          "goatee11","goatee12","goatee15","goatee16","goatee17","goatee18","goatee19","goatee2",
          "goatee3","goatee4-stache","goatee4","goatee5","goatee6","goatee7","goatee8","goatee9",
          "harley1-sb-1","harley1-sb-2","harley1","harley2-sb-1","harley2-sb-2","harley2",
          "harly3-sb-1","harly3-sb-2","harly3","honest-abe-stache","honest-abe","logan",
          "loganGoatee2","loganGoatee2Stache","loganGoatee3","loganGoatee3soul",
          "loganGoatee3soulStache","loganSoul","mustache-thin","mustache1","mustache1SB1",
          "mustache1SB2","mutton","muttonGoatee1","muttonGoatee1Stache","muttonGoatee2",
          "muttonGoatee2Stache","muttonGoatee5","muttonGoatee5Stache","muttonSoul","muttonStache",
          "muttonStacheSoul","neckbeard","neckbeard2","neckbeard2SB1","neckbeard2SB2",
          "neckbeardSB1","neckbeardSB2","none","sideburns1","sideburns2","sideburns3",
          "soul-stache","soul","wilt-sideburns-long","wilt-sideburns-short","wilt" },
        // glasses
        { "facemask","glasses1-primary","glasses1-secondary","glasses2-black",
          "glasses2-primary","glasses2-secondary","none" },
        // hair
        { "afro","afro2","bald","blowoutFade","cornrows","crop-fade","crop-fade2","crop",
          "curly","curly2","curly3","curlyFade1","curlyFade2","dreads","emo","faux-hawk",
          "fauxhawk-fade",
          "female1","female10","female11","female12","female2","female3","female4","female5",
          "female6","female7","female8","female9",
          "hair","high","juice","longHair","messy-short","messy","middle-part","parted",
          "shaggy1","shaggy2","short-bald","short-fade","short","short2","short3",
          "shortBangs","spike","spike2","spike3","spike4","tall-fade" },
        // hairBg
        { "female1","female2","female3","female4","female5","longHair","none","shaggy" },
        // head
        { "female1","female2","female3",
          "head1","head10","head11","head12","head13","head14","head15","head16","head17","head18",
          "head2","head3","head4","head5","head6","head7","head8","head9" },
        // jersey
        { "baseball","baseball2","baseball3","baseball4","football","football2","football3",
          "football4","football5","hockey","hockey2","hockey3","hockey4",
          "jersey","jersey2","jersey3","jersey4","jersey5" },
        // miscLine
        { "blush","chin1","chin2","forehead1","forehead2","forehead3","forehead4","forehead5",
          "freckles1","freckles2","none" },
        // mouth
        { "angry","closed","mouth","mouth2","mouth3","mouth4","mouth5","mouth6","mouth7",
          "mouth8","side","smile-closed","smile","smile2","smile3","smile4","straight" },
        // nose
        { "honker","nose1","nose10","nose11","nose12","nose13","nose14","nose2","nose3",
          "nose4","nose5","nose6","nose7","nose8","nose9","pinocchio","small" },
        // smileLine
        { "line1","line2","line3","line4","none" }
    };

    /** Gender tag per feature slot — parallel to {@link #SVG_IDS}. "B"=both, "M"=male, "F"=female. */
    private static final char[][] SVG_GENDERS = {
        // accessories
        { 'B','B','B','B','B','B','B' },
        // body
        { 'B','B','M','B','M' },
        // ear
        { 'B','B','M' },
        // eye (19 male, 16 female)
        { 'M','M','M','M','M','M','M','M','M','M','M','M','M','M','M','M','M','M','M',
          'F','F','F','F','F','F','F','F','F','F','F','F','F','F','F','F' },
        // eyeLine
        { 'M','M','M','M','M','M','B' },
        // eyebrow (20 both, 10 female)
        { 'B','B','B','B','B','B','B','B','B','B','B','B','B','B','B','B','B','B','B','B',
          'F','F','F','F','F','F','F','F','F','F' },
        // facialHair (82 male, 1 both = none)
        { 'M','M','M','M','M','M','M','M','M','M','M','M','M','M','M','M','M','M','M','M',
          'M','M','M','M','M','M','M','M','M','M','M','M','M','M','M','M','M','M','M','M',
          'M','M','M','M','M','M','M','M','M','M','M','M','M','M','M','M','M','M','M','M',
          'M','M','M','M','M','M','M','M','M','M','M','M','M','M','B','M','M','M','M','M',
          'M','M','M' },
        // glasses
        { 'B','B','B','B','B','B','B' },
        // hair (17 male, 13 female, rest mixed)
        { 'M','B','M','M','B','M','M','M','B','B','B','M','M','M','B','M','M',
          'F','F','F','F','F','F','F','F','F','F','F','F',
          'M','M','M','B','M','M','M','M','B','B','M','M','M','M','B','M','M','M','M','M','M' },
        // hairBg
        { 'F','F','F','F','F','B','B','B' },
        // head (3 female, rest male/both)
        { 'F','F','F',
          'M','M','M','M','B','B','M','B','M','M','B','M','M','M','B','M','M','M' },
        // jersey
        { 'B','B','B','B','B','B','B','B','B','B','B','B','B','B','B','B','B','B' },
        // miscLine
        { 'F','B','B','M','M','M','M','M','B','B','M' },
        // mouth
        { 'B','B','B','B','B','B','B','B','B','B','B','B','B','B','B','B','B' },
        // nose
        { 'B','B','B','B','B','B','B','B','B','B','B','B','B','B','B','B','B' },
        // smileLine
        { 'B','B','B','B','B' }
    };

    // Feature index constants
    private static final int F_ACCESSORIES = 0;
    private static final int F_BODY        = 1;
    private static final int F_EAR         = 2;
    private static final int F_EYE         = 3;
    private static final int F_EYELINE     = 4;
    private static final int F_EYEBROW     = 5;
    private static final int F_FACIALHAIR  = 6;
    private static final int F_GLASSES     = 7;
    private static final int F_HAIR        = 8;
    private static final int F_HAIRBG      = 9;
    private static final int F_HEAD        = 10;
    @SuppressWarnings("unused")
    private static final int F_JERSEY      = 11;
    private static final int F_MISCLINE    = 12;
    @SuppressWarnings("unused")
    private static final int F_MOUTH       = 13;
    @SuppressWarnings("unused")
    private static final int F_NOSE        = 14;
    private static final int F_SMILELINE   = 15;

    /**
     * Mouth IDs used when no rule pool is available. Emotion-specific entries
     * ("angry", "closed", "side") are excluded so that default/fallback faces
     * never show emotion expressions.
     */
    private static final String[] F_MOUTH_NORMAL = {
        "mouth","mouth2","mouth3","mouth4","mouth5","mouth6","mouth7","mouth8",
        "smile-closed","smile","smile2","smile3","smile4","straight"
    };

    // -------------------------------------------------------------------------
    // Skin and hair colour palettes (from generate.ts)
    // -------------------------------------------------------------------------

    private static final String[] SKIN_WHITE  = { "#f2d6cb", "#ddb7a0" };
    private static final String[] HAIR_WHITE  = { "#272421","#3D2314","#5A3825","#CC9966",
                                                   "#2C1608","#B55239","#e9c67b","#D7BF91" };
    private static final String[] SKIN_ASIAN  = { "#fedac7","#f0c5a3","#eab687" };
    private static final String[] HAIR_ASIAN  = { "#272421","#0f0902" };
    private static final String[] SKIN_BROWN  = { "#bb876f","#aa816f","#a67358" };
    private static final String[] HAIR_BROWN  = { "#272421","#1c1008" };
    private static final String[] SKIN_BLACK  = { "#ad6453","#74453d","#5c3937" };
    private static final String[] HAIR_BLACK  = { "#272421" };

    private static final String[] RACES = { "white", "black", "brown", "asian" };
    private static final String[] DEFAULT_TEAM_COLORS = { "#89bfd3", "#7a1319", "#07364f" };

    // Number ranges keyed by feature × gender, from generate.ts numberRanges
    // Order: [femaleMin, femaleMax, maleMin, maleMax]
    private static final double[] RANGE_BODY_SIZE       = { 0.8, 0.9, 0.95, 1.05 };
    private static final double[] RANGE_FATNESS         = { 0.0, 0.4, 0.0,  1.0  };
    private static final double[] RANGE_EAR_SIZE        = { 0.5, 1.0, 0.5,  1.5  };
    private static final int[]    RANGE_EYE_ANGLE       = { -10, 15, -10,   15   };
    private static final int[]    RANGE_EYEBROW_ANGLE   = { -15, 20, -15,   20   };
    private static final double[] RANGE_HEAD_SHAVE      = { 0.0, 0.0, 0.0, 0.2  };
    private static final double[] RANGE_NOSE_SIZE       = { 0.5, 1.0, 0.5, 1.25 };
    private static final double[] RANGE_SMILELINE_SIZE  = { 0.25,2.25,0.25,2.25 };

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final Random rng;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /** Creates a generator backed by a default {@link Random} instance. */
    public FaceGenerator() {
        this(new Random());
    }

    /**
     * Creates a generator backed by the supplied {@link Random}.
     * Pass a seeded {@code Random} for reproducible output in tests.
     *
     * @param rng random source; {@code null} uses {@code new Random()}
     */
    public FaceGenerator(Random rng) {
        this.rng = rng != null ? rng : new Random();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Generates a random male face with a random race.
     *
     * @return a new {@link FaceConfig}
     */
    public FaceConfig generate() {
        return generate(new Options());
    }

    /**
     * Generates a random face according to the supplied {@link Options}.
     *
     * @param options generation options (gender, race); must not be {@code null}
     * @return a new {@link FaceConfig}
     */
    public FaceConfig generate(Options options) {
        if (options == null) options = new Options();

        boolean isMale = !"female".equalsIgnoreCase(options.gender);
        String  race   = options.race != null ? options.race : randChoice(RACES);

        String[] skinPalette = skinPalette(race);
        String[] hairPalette = hairPalette(race);

        String skinColor = randChoice(skinPalette);
        String hairColor = randChoice(hairPalette);

        // hairBg: male 10% chance, female 90% chance
        double hairBgChance = isMale ? 0.1 : 0.9;
        String hairBgId     = rng.nextDouble() < hairBgChance
                              ? randGenderedId(F_HAIRBG, isMale)
                              : "none";

        // head shave: male only, 25% chance
        double shaveVal = 0.0;
        if (isMale && rng.nextDouble() < 0.25) {
            shaveVal = randUniform(RANGE_HEAD_SHAVE[2], RANGE_HEAD_SHAVE[3]);
        }
        String shaveColor = "rgba(0,0,0," + round2(shaveVal) + ")";

        // eyeLine: 75% chance
        String eyeLineId = rng.nextDouble() < 0.75
                           ? randGenderedId(F_EYELINE, isMale)
                           : "none";

        // smileLine: male 75%, female 10%
        double smileChance = isMale ? 0.75 : 0.1;
        String smileId     = rng.nextDouble() < smileChance
                             ? randGenderedId(F_SMILELINE, isMale)
                             : "none";

        // miscLine: only from rules – default to none so rules control which
        // IDs are eligible (random fallback would allow any catalogue ID,
        // e.g. blush appearing on females with no rule using it).
        String miscLineId = "none";

        // facialHair: male 50% chance (female always none)
        String facialHairId = (!isMale || rng.nextDouble() < 0.5)
                              ? "none"
                              : randGenderedId(F_FACIALHAIR, true);

        // glasses: only from rules – default to none
        String glassesId = "none";

        // accessories: only from rules – default to none
        String accessoriesId = "none";

        // hair: if wearing certain hats, override specific hair styles
        String hairId = randGenderedId(F_HAIR, isMale);
        if ("hat".equals(accessoriesId) || "hat2".equals(accessoriesId)
                || "hat3".equals(accessoriesId)) {
            hairId = applyHatHairOverride(hairId);
        }

        FaceConfig.Builder b = new FaceConfig.Builder()
            .fatness(randUniform(isMale ? RANGE_FATNESS[2] : RANGE_FATNESS[0],
                                 isMale ? RANGE_FATNESS[3] : RANGE_FATNESS[1]))
            .teamColors(DEFAULT_TEAM_COLORS.clone())
            .hairBg(new FaceConfig.SimpleFeature(hairBgId))
            .body(new FaceConfig.BodyFeature(
                    randGenderedId(F_BODY, isMale), skinColor,
                    randUniform(isMale ? RANGE_BODY_SIZE[2] : RANGE_BODY_SIZE[0],
                                isMale ? RANGE_BODY_SIZE[3] : RANGE_BODY_SIZE[1])))
            .jersey(new FaceConfig.SimpleFeature("none"))
            .ear(new FaceConfig.EarFeature(
                    randGenderedId(F_EAR, isMale),
                    randUniform(isMale ? RANGE_EAR_SIZE[2] : RANGE_EAR_SIZE[0],
                                isMale ? RANGE_EAR_SIZE[3] : RANGE_EAR_SIZE[1])))
            .head(new FaceConfig.HeadFeature(randGenderedId(F_HEAD, isMale), shaveColor))
            .eyeLine(new FaceConfig.SimpleFeature(eyeLineId))
            .smileLine(new FaceConfig.SmileLineFeature(
                    smileId,
                    randUniform(isMale ? RANGE_SMILELINE_SIZE[2] : RANGE_SMILELINE_SIZE[0],
                                isMale ? RANGE_SMILELINE_SIZE[3] : RANGE_SMILELINE_SIZE[1])))
            .miscLine(new FaceConfig.SimpleFeature(miscLineId))
            .facialHair(new FaceConfig.SimpleFeature(facialHairId))
            .eye(new FaceConfig.EyeFeature(
                    randGenderedId(F_EYE, isMale),
                    randInt(isMale ? RANGE_EYE_ANGLE[2] : RANGE_EYE_ANGLE[0],
                            isMale ? RANGE_EYE_ANGLE[3] : RANGE_EYE_ANGLE[1])))
            .eyebrow(new FaceConfig.EyebrowFeature(
                    randGenderedId(F_EYEBROW, isMale),
                    randInt(isMale ? RANGE_EYEBROW_ANGLE[2] : RANGE_EYEBROW_ANGLE[0],
                            isMale ? RANGE_EYEBROW_ANGLE[3] : RANGE_EYEBROW_ANGLE[1])))
            .hair(new FaceConfig.HairFeature(hairId, hairColor, false))
            .mouth(new FaceConfig.MouthFeature(
                    randChoice(F_MOUTH_NORMAL), rng.nextBoolean()))
            .nose(new FaceConfig.NoseFeature(
                    randGenderedId(F_NOSE, isMale),
                    rng.nextBoolean(),
                    randUniform(isMale ? RANGE_NOSE_SIZE[2] : RANGE_NOSE_SIZE[0],
                                isMale ? RANGE_NOSE_SIZE[3] : RANGE_NOSE_SIZE[1])))
            .glasses(new FaceConfig.SimpleFeature(glassesId))
            .accessories(new FaceConfig.SimpleFeature(accessoriesId));

        return b.build();
    }

    /**
     * Generates a face using eligible part IDs from the supplied pool, with
     * standard random numeric parameters (angles, sizes, colours).
     *
     * <p>For each feature type present in {@code pool}, a random ID is chosen
     * from the eligible list.  For feature types absent from the pool, the
     * standard {@link #generate(Options)} random selection applies as a
     * fallback.
     *
     * @param options generation options (gender, race); {@code null} → defaults
     * @param pool    eligible ID map returned by {@link #defaultCharacterFace};
     *                {@code null} or empty → falls back to {@link #generate(Options)}
     * @return a new {@link FaceConfig} respecting the pool constraints
     */
    public FaceConfig generate(Options options, Map<String, List<String>> pool) {
        if (pool == null || pool.isEmpty()) return generate(options);
        if (options == null) options = new Options();

        boolean isMale = !"female".equalsIgnoreCase(options.gender);
        String  race   = options.race != null ? options.race : randChoice(RACES);

        String[] skinPalette = skinPalette(race);
        String[] hairPalette = hairPalette(race);
        String skinColor = randChoice(skinPalette);
        String hairColor = randChoice(hairPalette);

        // hairBg
        double hairBgChance = isMale ? 0.1 : 0.9;
        String hairBgId = pickFromPool(pool, "hairBg",
                rng.nextDouble() < hairBgChance ? randGenderedId(F_HAIRBG, isMale) : "none");

        // head shave: use caller-supplied colour when provided; otherwise male-only random
        String shaveColor;
        if (options.shaveColor != null) {
            shaveColor = options.shaveColor;
        } else {
            double shaveVal = 0.0;
            if (isMale && rng.nextDouble() < 0.25) {
                shaveVal = randUniform(RANGE_HEAD_SHAVE[2], RANGE_HEAD_SHAVE[3]);
            }
            shaveColor = "rgba(0,0,0," + round2(shaveVal) + ")";
        }

        // eyeLine (75%)
        String eyeLineId = pickFromPool(pool, "eyeLine",
                rng.nextDouble() < 0.75 ? randGenderedId(F_EYELINE, isMale) : "none");

        // smileLine (male 75%, female 10%)
        double smileChance = isMale ? 0.75 : 0.1;
        String smileId = pickFromPool(pool, "smileLine",
                rng.nextDouble() < smileChance ? randGenderedId(F_SMILELINE, isMale) : "none");

        // miscLine: only use pool entry; default to none so rules control which
        // miscLine IDs are eligible (random fallback would allow any catalogue ID).
        String miscLineId = pickFromPool(pool, "miscLine", "none");

        // facialHair: only use pool entry; default to none so beard/facial-hair
        // rules control this. The Male and Female rules do not include facial
        // hair, so characters generated with those rules will never have a beard.
        String facialHairId = pickFromPool(pool, "facialHair", "none");

        // glasses: only use pool entry; default to none so glasses-rules control this
        String glassesId = pickFromPool(pool, "glasses", "none");

        // accessories: only use pool entry; default to none so clothes-rules control this
        String accessoriesId = pickFromPool(pool, "accessories", "none");

        // hair (with hat override)
        String hairId = pickFromPool(pool, "hair", randGenderedId(F_HAIR, isMale));
        if ("hat".equals(accessoriesId) || "hat2".equals(accessoriesId)
                || "hat3".equals(accessoriesId)) {
            hairId = applyHatHairOverride(hairId);
        }

        FaceConfig.Builder b = new FaceConfig.Builder()
            .fatness(randUniform(isMale ? RANGE_FATNESS[2] : RANGE_FATNESS[0],
                                 isMale ? RANGE_FATNESS[3] : RANGE_FATNESS[1]))
            .teamColors(DEFAULT_TEAM_COLORS.clone())
            .hairBg(new FaceConfig.SimpleFeature(hairBgId))
            .body(new FaceConfig.BodyFeature(
                    pickFromPool(pool, "body", randGenderedId(F_BODY, isMale)), skinColor,
                    randUniform(isMale ? RANGE_BODY_SIZE[2] : RANGE_BODY_SIZE[0],
                                isMale ? RANGE_BODY_SIZE[3] : RANGE_BODY_SIZE[1])))
            .jersey(new FaceConfig.SimpleFeature(
                    pickFromPool(pool, "jersey", "none")))
            .ear(new FaceConfig.EarFeature(
                    pickFromPool(pool, "ear", randGenderedId(F_EAR, isMale)),
                    randUniform(isMale ? RANGE_EAR_SIZE[2] : RANGE_EAR_SIZE[0],
                                isMale ? RANGE_EAR_SIZE[3] : RANGE_EAR_SIZE[1])))
            .head(new FaceConfig.HeadFeature(
                    pickFromPool(pool, "head", randGenderedId(F_HEAD, isMale)), shaveColor))
            .eyeLine(new FaceConfig.SimpleFeature(eyeLineId))
            .smileLine(new FaceConfig.SmileLineFeature(
                    smileId,
                    randUniform(isMale ? RANGE_SMILELINE_SIZE[2] : RANGE_SMILELINE_SIZE[0],
                                isMale ? RANGE_SMILELINE_SIZE[3] : RANGE_SMILELINE_SIZE[1])))
            .miscLine(new FaceConfig.SimpleFeature(miscLineId))
            .facialHair(new FaceConfig.SimpleFeature(facialHairId))
            .eye(new FaceConfig.EyeFeature(
                    pickFromPool(pool, "eye", randGenderedId(F_EYE, isMale)),
                    randInt(isMale ? RANGE_EYE_ANGLE[2] : RANGE_EYE_ANGLE[0],
                            isMale ? RANGE_EYE_ANGLE[3] : RANGE_EYE_ANGLE[1])))
            .eyebrow(new FaceConfig.EyebrowFeature(
                    pickFromPool(pool, "eyebrow", randGenderedId(F_EYEBROW, isMale)),
                    randInt(isMale ? RANGE_EYEBROW_ANGLE[2] : RANGE_EYEBROW_ANGLE[0],
                            isMale ? RANGE_EYEBROW_ANGLE[3] : RANGE_EYEBROW_ANGLE[1])))
            .hair(new FaceConfig.HairFeature(hairId, hairColor, false))
            .mouth(new FaceConfig.MouthFeature(
                    pickFromPool(pool, "mouth", "none"), rng.nextBoolean()))
            .nose(new FaceConfig.NoseFeature(
                    pickFromPool(pool, "nose", randGenderedId(F_NOSE, isMale)),
                    rng.nextBoolean(),
                    randUniform(isMale ? RANGE_NOSE_SIZE[2] : RANGE_NOSE_SIZE[0],
                                isMale ? RANGE_NOSE_SIZE[3] : RANGE_NOSE_SIZE[1])))
            .glasses(new FaceConfig.SimpleFeature(glassesId))
            .accessories(new FaceConfig.SimpleFeature(accessoriesId));

        return b.build();
    }

    /**
     * Picks a random ID from the pool entry for {@code feature} using the
     * generator's own RNG.  Returns {@code fallback} when the pool has no
     * entry for the feature.
     */
    private String pickFromPool(Map<String, List<String>> pool, String feature, String fallback) {
        List<String> ids = pool.get(feature);
        if (ids != null && !ids.isEmpty()) {
            return ids.get(rng.nextInt(ids.size()));
        }
        return fallback;
    }

    // -------------------------------------------------------------------------
    // Rule-based character face computation
    // -------------------------------------------------------------------------

    /**
     * Rule names that {@link #defaultCharacterFace} is permitted to fire.
     *
     * <p>The core gender rules ("Male", "Female") set the base eligible parts.
     * The extra-feature rules ("faceExtraFemale", "faceExtraMale") add
     * gender-specific miscLine variety (e.g. blush, freckles, chin lines).
     * Beard, age, clothes and emotion rules are handled by dedicated generators.
     */
    private static final Set<String> ALLOWED_RULE_NAMES = new HashSet<>(
            Arrays.asList("Male", "Female", "faceExtraFemale", "faceExtraMale"));

    /**
     * Computes a map of eligible SVG part IDs per feature type for a character
     * using the supplied face rules.
     *
     * <h3>Algorithm</h3>
     * <ol>
     *   <li>Rules are assumed to arrive already sorted ascending by priority
     *       (lowest priority first).  If not sorted, callers should sort before
     *       passing to this method.  Rules with equal priority are processed in
     *       list order.</li>
     *   <li>For each rule, its conditions are evaluated (gender, emotion, minAge,
     *       percentage).  Rules with a non-empty emotion other than {@code "normal"}
     *       are skipped because this method produces the default (normal) face.
     *       The {@code percentage} roll is seeded with
     *       {@code seed ^ (ruleIndex * 6364136223846793005L)} so that each
     *       character always rolls the same result for each rule.</li>
     *   <li>When a rule fires:
     *       <ul>
     *         <li><strong>include</strong> entries are "unique per type": the
     *             eligible set for each feature type in {@code include} is
     *             <em>replaced</em> with the entries from this rule.</li>
     *         <li><strong>additional</strong> entries are "not unique per type":
     *             entries are <em>added</em> to the current eligible set for
     *             each feature type without replacing existing entries.</li>
     *         <li><strong>exclude</strong> entries are removed from every
     *             feature type's final eligible set.</li>
     *       </ul>
     *   </li>
     * </ol>
     *
     * <h3>Return value</h3>
     * The returned map contains only feature types for which at least one
     * eligible ID survived after exclusions.  The value lists preserve the
     * insertion order of eligible IDs but are unmodifiable.
     *
     * <h3>Usage</h3>
     * <pre>
     *   List&lt;FaceRule&gt; rules = FaceRuleLoader.fromJson(json);
     *   Map&lt;String, List&lt;String&gt;&gt; pool =
     *       FaceGenerator.defaultCharacterFace(npcId, "female", 35, rules);
     *   // pool.get("eye") → ["eye.female1", "eye.female2", ...]
     * </pre>
     *
     * @param seed    per-character seed for deterministic percentage rolls
     * @param gender  {@code "male"} or {@code "female"} (case-insensitive);
     *                {@code null} treated as {@code "male"}
     * @param age     character age; rules with {@code minAge > age} are skipped
     * @param rules   face rules already sorted ascending by priority; must not
     *                be {@code null}
     * @return unmodifiable map of feature type → ordered list of eligible IDs
     */
    public static Map<String, List<String>> defaultCharacterFace(
            long seed, String gender, int age, List<FaceRule> rules) {

        if (rules == null) throw new IllegalArgumentException("rules must not be null");

        final String normGender = (gender != null) ? gender.toLowerCase() : "male";

        // include: unique per type – only the last (highest-priority) fired rule's
        //   include list for a given type is kept.
        Map<String, List<String>> includedByType = new HashMap<>();

        // additional: accumulates across all fired rules for each type.
        Map<String, Set<String>> additionalByType = new HashMap<>();

        // exclude: global set of IDs removed from the final eligible set.
        Set<String> globalExclude = new LinkedHashSet<>();

        for (int i = 0; i < rules.size(); i++) {
            FaceRule rule = rules.get(i);

            // ── Condition checks ─────────────────────────────────────────────

            // Name whitelist: only the core character rules fire here.
            // Clothes and emotion rules are handled by separate generators.
            if (!ALLOWED_RULE_NAMES.contains(rule.name)) {
                System.err.printf(Locale.US,
                        "[FaceGenerator] rule skipped (not in whitelist): \"%s\"%n",
                        rule.name);
                continue;
            }

            // Gender filter
            if (!rule.gender.isEmpty()
                    && !rule.gender.equalsIgnoreCase(normGender)) {
                continue;
            }

            // Emotion filter – this method produces the "normal" (default) face.
            // Rules scoped to a specific non-normal emotion are skipped.
            if (!rule.emotion.isEmpty() && !rule.emotion.equalsIgnoreCase("normal")) {
                continue;
            }

            // Age filter
            if (rule.minAge > 0 && age < rule.minAge) {
                continue;
            }

            // Percentage roll – seeded per (character × rule) for determinism
            if (rule.percentage < 100) {
                long rollSeed = seed ^ (long) i * 6364136223846793005L;
                Random rollRng = new Random(rollSeed);
                if (rollRng.nextInt(100) >= rule.percentage) {
                    continue;
                }
            }

            // ── Debug: log which rule fired ───────────────────────────────────
            System.err.printf(Locale.US,
                    "[FaceGenerator] rule fired: \"%s\" (priority=%d, gender=\"%s\", minAge=%d)"
                    + " include=%d additional=%d exclude=%d%n",
                    rule.name, rule.priority, rule.gender, rule.minAge,
                    rule.include.size(), rule.additional.size(), rule.exclude.size());

            // ── Apply include (unique per type) ───────────────────────────────
            // Group include entries by feature type and replace any previous list
            Map<String, List<String>> ruleIncludes = new HashMap<>();
            for (String entry : rule.include) {
                int dot = entry.indexOf('.');
                if (dot <= 0) continue;
                String featureType = entry.substring(0, dot);
                String id          = entry.substring(dot + 1);
                ruleIncludes.computeIfAbsent(featureType, k -> new ArrayList<>()).add(id);
            }
            for (Map.Entry<String, List<String>> e : ruleIncludes.entrySet()) {
                includedByType.put(e.getKey(), new ArrayList<>(e.getValue()));
            }

            // ── Apply additional (accumulates) ────────────────────────────────
            for (String entry : rule.additional) {
                int dot = entry.indexOf('.');
                if (dot <= 0) continue;
                String featureType = entry.substring(0, dot);
                String id          = entry.substring(dot + 1);
                additionalByType.computeIfAbsent(featureType, k -> new LinkedHashSet<>()).add(id);
            }

            // ── Collect exclude entries ────────────────────────────────────────
            for (String entry : rule.exclude) {
                int dot = entry.indexOf('.');
                if (dot <= 0) continue;
                // store the full "feature.id" form so we can match against the maps
                globalExclude.add(entry);
            }
        }

        // ── Build final eligible map ──────────────────────────────────────────
        // Gather all feature types from both include and additional maps
        Set<String> allTypes = new LinkedHashSet<>();
        allTypes.addAll(includedByType.keySet());
        allTypes.addAll(additionalByType.keySet());

        Map<String, List<String>> result = new HashMap<>();

        for (String featureType : allTypes) {
            // Start with include IDs for this type (if any)
            Set<String> eligible = new LinkedHashSet<>();
            List<String> inc = includedByType.get(featureType);
            if (inc != null) eligible.addAll(inc);

            // Add additional IDs
            Set<String> add = additionalByType.get(featureType);
            if (add != null) eligible.addAll(add);

            // Remove excluded
            eligible.removeIf(id -> globalExclude.contains(featureType + "." + id));

            if (!eligible.isEmpty()) {
                result.put(featureType, Collections.unmodifiableList(
                        new ArrayList<>(eligible)));
            }
        }

        // ── Debug: log final eligible pool per feature type ──────────────────
        for (Map.Entry<String, List<String>> e : result.entrySet()) {
            System.err.printf(Locale.US, "[FaceGenerator] pool: %s → %s%n",
                    e.getKey(), e.getValue());
        }

        return Collections.unmodifiableMap(result);
    }

    // -------------------------------------------------------------------------
    // Options
    // -------------------------------------------------------------------------

    /**
     * Generation options for {@link #generate(Options)}.
     *
     * <p>All fields have sensible defaults ({@code "male"} gender, random race).
     */
    public static final class Options {
        /** {@code "male"} or {@code "female"} (default: {@code "male"}). */
        public String gender = "male";
        /** {@code "white"}, {@code "black"}, {@code "brown"}, or {@code "asian"};
         *  {@code null} picks at random. */
        public String race   = null;
        /**
         * Optional shave-shadow colour for the head SVG, e.g.
         * {@code "rgba(0,0,0,0.06)"}.  When non-null this value overrides the
         * built-in random shave calculation in
         * {@link FaceGenerator#generate(Options, java.util.Map)}.
         * {@code null} means "use the default random logic".
         */
        public String shaveColor = null;

        public Options() {}

        /** Sets the gender and returns {@code this} for chaining. */
        public Options gender(String g) { this.gender = g; return this; }
        /** Sets the race and returns {@code this} for chaining. */
        public Options race(String r)   { this.race = r;   return this; }
        /** Sets the shave-shadow colour override and returns {@code this} for chaining. */
        public Options shaveColor(String c) { this.shaveColor = c; return this; }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Returns a random element from the array. */
    private <T> T randChoice(T[] arr) {
        return arr[rng.nextInt(arr.length)];
    }

    /** Returns a random int in [a, b] inclusive. */
    private int randInt(int a, int b) {
        return a + rng.nextInt(b - a + 1);
    }

    /** Returns a random double in [a, b). */
    private double randUniform(double a, double b) {
        return a + rng.nextDouble() * (b - a);
    }

    /** Rounds to two decimal places. */
    private static double round2(double x) {
        return Math.round(x * 100.0) / 100.0;
    }

    /**
     * Returns a random SVG feature ID that is compatible with the given gender.
     * Compatible means the gender tag is {@code 'B'} (both), or exactly matches
     * ({@code 'M'} for male, {@code 'F'} for female).
     */
    private String randGenderedId(int featureIndex, boolean isMale) {
        String[] ids    = SVG_IDS[featureIndex];
        char[]   gends  = SVG_GENDERS[featureIndex];
        char     target = isMale ? 'M' : 'F';

        List<String> valid = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            char g = gends[i];
            if (g == 'B' || g == target) {
                valid.add(ids[i]);
            }
        }
        if (valid.isEmpty()) return ids[0]; // fallback
        return valid.get(rng.nextInt(valid.size()));
    }

    /** Returns the skin colour palette for the given race. */
    private static String[] skinPalette(String race) {
        switch (race) {
            case "asian": return SKIN_ASIAN;
            case "brown": return SKIN_BROWN;
            case "black": return SKIN_BLACK;
            default:      return SKIN_WHITE;
        }
    }

    /** Returns the hair colour palette for the given race. */
    private static String[] hairPalette(String race) {
        switch (race) {
            case "asian": return HAIR_ASIAN;
            case "brown": return HAIR_BROWN;
            case "black": return HAIR_BLACK;
            default:      return HAIR_WHITE;
        }
    }

    /**
     * When an NPC is wearing a hat, certain long/voluminous hair styles are
     * replaced with shorter alternatives (mirrors the JS hat-override logic).
     */
    private static String applyHatHairOverride(String hairId) {
        switch (hairId) {
            case "afro": case "afro2": case "curly": case "curly2": case "curly3":
            case "faux-hawk": case "hair": case "high": case "juice":
            case "messy-short": case "messy": case "middle-part": case "parted":
            case "shaggy1": case "shaggy2": case "short3": case "spike":
            case "spike2": case "spike3": case "spike4":
                return "short";
            case "blowoutFade": case "curlyFade1": case "curlyFade2": case "dreads":
            case "fauxhawk-fade": case "tall-fade":
                return "short-fade";
            default:
                return hairId;
        }
    }
}
