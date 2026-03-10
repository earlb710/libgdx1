package eb.framework1.face;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts a {@link FaceConfig} into an SVG XML string, ported from the
 * <a href="https://github.com/zengm-games/facesjs">facesjs</a> JavaScript
 * {@code display()} / {@code faceToSvgString()} functions.
 *
 * <p>The builder requires a {@link SvgTemplateLoader} to supply raw SVG path
 * data for each feature ID.  The production implementation reads from the
 * bundled {@code assets/face/svgs.json} file; a stub or mock can be supplied
 * in tests.
 *
 * <h3>SVG coordinate space</h3>
 * All faces are rendered in a 400 × 600 viewBox.  The origin (0,0) is the
 * top-left corner.  Feature positions follow the same pixel coordinates as the
 * original JavaScript library:
 * <ul>
 *   <li>ears: [55,325] left, [345,325] right</li>
 *   <li>eyes: [140,310] left, [260,310] right</li>
 *   <li>eyebrows: [140,270] left, [260,270] right</li>
 *   <li>mouth: [200,440]</li>
 *   <li>nose: [200,370]</li>
 *   <li>smile lines: [150,435] left, [250,435] right</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>
 *   SvgTemplateLoader loader = JsonSvgTemplateLoader.fromAssets(fileHandle);
 *   FaceSvgBuilder builder = new FaceSvgBuilder(loader);
 *   FaceConfig face = new FaceGenerator().generate();
 *   String svg = builder.toSvgString(face);
 * </pre>
 */
public final class FaceSvgBuilder {

    // -------------------------------------------------------------------------
    // Template-loader interface
    // -------------------------------------------------------------------------

    /**
     * Supplies raw SVG path/group strings for a given feature category and ID.
     * The strings may contain the following placeholders that the builder
     * replaces before embedding:
     * <ul>
     *   <li>{@code $[skinColor]} — body skin colour hex</li>
     *   <li>{@code $[hairColor]} — hair colour hex</li>
     *   <li>{@code $[primary]}   — team primary colour</li>
     *   <li>{@code $[secondary]} — team secondary colour</li>
     *   <li>{@code $[accent]}    — team accent colour</li>
     *   <li>{@code $[faceShave]} / {@code $[headShave]} — head-shave rgba string</li>
     * </ul>
     */
    public interface SvgTemplateLoader {
        /**
         * Returns the raw SVG fragment for the given feature category and ID,
         * or {@code null} / empty string if the combination is unknown.
         *
         * @param feature feature name, e.g. {@code "eye"}, {@code "hair"}
         * @param id      variant ID, e.g. {@code "eye1"}, {@code "short"}
         */
        String getSvgTemplate(String feature, String id);
    }

    // -------------------------------------------------------------------------
    // Feature layout descriptors (mirrors featureInfos[] in display.ts)
    // -------------------------------------------------------------------------

    private static final class FeatureInfo {
        final String   name;
        /** Pixel positions [x,y] pairs, or null for "no translation". */
        final int[][]  positions;
        final boolean  scaleFatness;

        FeatureInfo(String name, int[][] positions, boolean scaleFatness) {
            this.name         = name;
            this.positions    = positions;
            this.scaleFatness = scaleFatness;
        }
    }

    /**
     * Drawing order and positioning data for every feature, exactly matching
     * the {@code featureInfos} array from {@code display.ts}.
     */
    private static final FeatureInfo[] FEATURE_INFOS = {
        new FeatureInfo("hairBg",     null,                                    true),
        new FeatureInfo("body",       null,                                    false),
        new FeatureInfo("jersey",     null,                                    false),
        new FeatureInfo("ear",        new int[][]{{55,325},{345,325}},         true),
        new FeatureInfo("head",       null,                                    true),
        new FeatureInfo("eyeLine",    null,                                    false),
        new FeatureInfo("smileLine",  new int[][]{{150,435},{250,435}},        false),
        new FeatureInfo("miscLine",   null,                                    false),
        new FeatureInfo("facialHair", null,                                    true),
        new FeatureInfo("eye",        new int[][]{{140,310},{260,310}},        false),
        new FeatureInfo("eyebrow",    new int[][]{{140,270},{260,270}},        false),
        new FeatureInfo("mouth",      new int[][]{{200,440}},                  false),
        new FeatureInfo("nose",       new int[][]{{200,370}},                  false),
        new FeatureInfo("hair",       null,                                    true),
        new FeatureInfo("glasses",    null,                                    true),
        new FeatureInfo("accessories",null,                                    true),
    };

    // Fatness scale constants from display.ts: fatScale(f) = 0.8 + 0.2*f
    // At fatness=1: 47 px side margin; fatness=0: 78 px side margin.
    private static final double FAT_MARGIN_FULL = 47.0;
    private static final double FAT_MARGIN_THIN = 78.0;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final SvgTemplateLoader loader;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * @param loader SVG template supplier; must not be {@code null}
     */
    public FaceSvgBuilder(SvgTemplateLoader loader) {
        if (loader == null) throw new IllegalArgumentException("loader must not be null");
        this.loader = loader;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Renders the face described by {@code face} as an SVG XML string.
     *
     * <p>The returned string is a complete, self-contained SVG document with
     * {@code viewBox="0 0 400 600"}.
     *
     * @param face face configuration; must not be {@code null}
     * @return SVG XML string
     */
    public String toSvgString(FaceConfig face) {
        if (face == null) throw new IllegalArgumentException("face must not be null");

        double bodySize = face.body.size;
        double fatness  = face.fatness;

        StringBuilder sb = new StringBuilder(8192);
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\"")
          .append(" version=\"1.2\" baseProfile=\"tiny\"")
          .append(" width=\"100%\" height=\"100%\"")
          .append(" viewBox=\"0 0 400 600\"")
          .append(" preserveAspectRatio=\"xMinYMin meet\">\n");

        for (FeatureInfo info : FEATURE_INFOS) {
            drawFeature(sb, face, info, bodySize, fatness);
        }

        sb.append("</svg>");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Private rendering
    // -------------------------------------------------------------------------

    private void drawFeature(StringBuilder sb,
                             FaceConfig face,
                             FeatureInfo info,
                             double bodySize,
                             double fatness) {

        // Resolve template SVG string
        String featureSvg = resolveTemplate(face, info.name);
        if (featureSvg == null || featureSvg.isEmpty()) return;

        // Apply colour substitutions
        featureSvg = applySubstitutions(featureSvg, face);

        // Determine how many positions to draw (null means single, no translate)
        int posCount = (info.positions == null) ? 1 : info.positions.length;

        for (int i = 0; i < posCount; i++) {
            String transform = buildTransform(face, info, i, bodySize, fatness);
            sb.append("<g");
            if (!transform.isEmpty()) {
                sb.append(" transform=\"").append(transform).append("\"");
            }
            sb.append(">\n");
            sb.append(featureSvg).append('\n');
            sb.append("</g>\n");
        }
    }

    /**
     * Builds the SVG {@code transform} attribute value for the i-th instance
     * of a feature, replicating the translate / scale / rotate operations from
     * {@code display.ts}.
     */
    private String buildTransform(FaceConfig face,
                                  FeatureInfo info,
                                  int instanceIdx,
                                  double bodySize,
                                  double fatness) {

        // Bounding box of the feature SVG is approximated by the viewBox centre
        // for features without a position (null) — actual rendering will still
        // look correct because transforms are cumulative.
        // For features with explicit positions we use the position directly.

        StringBuilder t = new StringBuilder();

        boolean hasPosition = (info.positions != null);

        // --- Translation to position ---
        if (hasPosition) {
            int px = info.positions[instanceIdx][0];
            int py = info.positions[instanceIdx][1];
            // Centre the feature at (px, py) — we do a simple translate here;
            // fine-grained bbox-centring would require actually parsing the SVG paths.
            appendTranslate(t, px, py);
        }

        // --- Rotation (eye, eyebrow) ---
        int angle = getAngle(face, info.name);
        if (angle != 0) {
            double sign = (instanceIdx == 0) ? 1.0 : -1.0;
            appendRotate(t, sign * angle);
        }

        // --- Scale (body/jersey, flip, size) ---
        double scale = getSize(face, info.name);
        boolean flip = getFlip(face, info.name);

        if ("body".equals(info.name) || "jersey".equals(info.name)) {
            appendScale(t, bodySize, 1.0);
        } else if (flip || instanceIdx == 1) {
            appendScale(t, -scale, scale);
        } else if (scale != 1.0) {
            appendScale(t, scale, scale);
        }

        // --- Fatness translation for scaleFatness features ---
        if (info.scaleFatness && hasPosition) {
            double distance = (FAT_MARGIN_THIN - FAT_MARGIN_FULL) * (1.0 - fatness);
            // Translate left edge by 'distance' (mirrors the "left","top" align in JS)
            appendTranslate(t, distance, 0);
        }

        // --- Fatness scaling for scaleFatness features without position ---
        if (info.scaleFatness && !hasPosition) {
            double fs = fatScale(fatness);
            appendScale(t, fs, 1.0);
        }

        return t.toString().trim();
    }

    private static double fatScale(double fatness) {
        return 0.8 + 0.2 * fatness;
    }

    // -------------------------------------------------------------------------
    // Transform helpers (emit space-separated transform list)
    // -------------------------------------------------------------------------

    private static void appendTranslate(StringBuilder t, double x, double y) {
        if (x == 0 && y == 0) return;
        if (t.length() > 0) t.append(' ');
        t.append(String.format("translate(%.2f %.2f)", x, y));
    }

    private static void appendScale(StringBuilder t, double x, double y) {
        if (x == 1.0 && y == 1.0) return;
        if (t.length() > 0) t.append(' ');
        t.append(String.format("scale(%.4f %.4f)", x, y));
    }

    private static void appendRotate(StringBuilder t, double angle) {
        if (angle == 0) return;
        if (t.length() > 0) t.append(' ');
        t.append(String.format("rotate(%.2f)", angle));
    }

    // -------------------------------------------------------------------------
    // Feature property accessors
    // -------------------------------------------------------------------------

    private static int getAngle(FaceConfig face, String name) {
        switch (name) {
            case "eye":     return face.eye.angle;
            case "eyebrow": return face.eyebrow.angle;
            default:        return 0;
        }
    }

    private static double getSize(FaceConfig face, String name) {
        switch (name) {
            case "ear":       return face.ear.size;
            case "nose":      return face.nose.size;
            case "smileLine": return face.smileLine.size;
            default:          return 1.0;
        }
    }

    private static boolean getFlip(FaceConfig face, String name) {
        switch (name) {
            case "hair":  return face.hair.flip;
            case "mouth": return face.mouth.flip;
            case "nose":  return face.nose.flip;
            default:      return false;
        }
    }

    // -------------------------------------------------------------------------
    // Template resolution
    // -------------------------------------------------------------------------

    private String resolveTemplate(FaceConfig face, String featureName) {
        String id = getFeatureId(face, featureName);
        if (id == null || "none".equals(id)) return null;

        String tmpl = loader.getSvgTemplate(featureName, id);
        if (tmpl == null || tmpl.isEmpty()) return null;

        // Apply shave placeholder substitution (head feature)
        if ("head".equals(featureName)) {
            tmpl = tmpl.replace("$[faceShave]", face.head.shave);
            tmpl = tmpl.replace("$[headShave]", face.head.shave);
        }
        return tmpl;
    }

    private static String getFeatureId(FaceConfig face, String name) {
        switch (name) {
            case "accessories": return face.accessories.id;
            case "body":        return face.body.id;
            case "ear":         return face.ear.id;
            case "eye":         return face.eye.id;
            case "eyebrow":     return face.eyebrow.id;
            case "eyeLine":     return face.eyeLine.id;
            case "facialHair":  return face.facialHair.id;
            case "glasses":     return face.glasses.id;
            case "hair":        return face.hair.id;
            case "hairBg":      return face.hairBg.id;
            case "head":        return face.head.id;
            case "jersey":      return face.jersey.id;
            case "miscLine":    return face.miscLine.id;
            case "mouth":       return face.mouth.id;
            case "nose":        return face.nose.id;
            case "smileLine":   return face.smileLine.id;
            default:            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Colour substitution
    // -------------------------------------------------------------------------

    private static final Pattern HAIR_COLOR_PATTERN = Pattern.compile("\\$\\[hairColor\\]");
    private static final Pattern PRIMARY_PATTERN    = Pattern.compile("\\$\\[primary\\]");
    private static final Pattern SECONDARY_PATTERN  = Pattern.compile("\\$\\[secondary\\]");
    private static final Pattern ACCENT_PATTERN     = Pattern.compile("\\$\\[accent\\]");

    private static String applySubstitutions(String tmpl, FaceConfig face) {
        tmpl = tmpl.replace("$[skinColor]",  face.body.color);
        tmpl = replaceAll(HAIR_COLOR_PATTERN, tmpl, face.hair.color);
        tmpl = replaceAll(PRIMARY_PATTERN,    tmpl,
                face.teamColors.length > 0 ? face.teamColors[0] : "#89bfd3");
        tmpl = replaceAll(SECONDARY_PATTERN,  tmpl,
                face.teamColors.length > 1 ? face.teamColors[1] : "#7a1319");
        tmpl = replaceAll(ACCENT_PATTERN,     tmpl,
                face.teamColors.length > 2 ? face.teamColors[2] : "#07364f");
        return tmpl;
    }

    private static String replaceAll(Pattern p, String input, String replacement) {
        Matcher m = p.matcher(input);
        return m.replaceAll(Matcher.quoteReplacement(replacement));
    }
}
