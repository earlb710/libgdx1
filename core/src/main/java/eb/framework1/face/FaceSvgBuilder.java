package eb.framework1.face;

import java.util.ArrayList;
import java.util.List;
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

        // Compute bounding-box centre BEFORE colour substitution so that
        // placeholder strings like "$[skinColor]" cannot accidentally affect
        // coordinate parsing.
        double[] center = (info.positions != null) ? computeCenter(featureSvg)
                                                   : null;

        // Apply colour substitutions
        featureSvg = applySubstitutions(featureSvg, face);

        // Determine how many positions to draw (null means single, no translate)
        int posCount = (info.positions == null) ? 1 : info.positions.length;

        for (int i = 0; i < posCount; i++) {
            String transform = buildTransform(face, info, i, bodySize, fatness, center);
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
     *
     * <p>The correct facesjs-equivalent formula for a positioned feature is:
     * <pre>  translate(px, py) [rotate(angle)] scale(±s, s) translate(-cx, -cy)</pre>
     * This places the feature's bounding-box centre at the target position
     * (px, py), matching {@code element.getBBox()} centring in the browser.
     *
     * @param center  bounding-box centre {cx, cy} of the raw SVG template, or
     *                {@code null} when the feature has no explicit position
     */
    private String buildTransform(FaceConfig face,
                                  FeatureInfo info,
                                  int instanceIdx,
                                  double bodySize,
                                  double fatness,
                                  double[] center) {

        StringBuilder t = new StringBuilder();

        boolean hasPosition = (info.positions != null);

        // --- Translation to position ---
        if (hasPosition) {
            int px = info.positions[instanceIdx][0];
            int py = info.positions[instanceIdx][1];
            // Step 1: move the coordinate origin to the target position.
            // The bbox-centre offset is applied as the LAST step below so that
            // scale / rotate operations act about the correct centre.
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

        // --- Fatness scaling for scaleFatness features without position ---
        if (info.scaleFatness && !hasPosition) {
            double fs = fatScale(fatness);
            appendScale(t, fs, 1.0);
        }

        // --- Bbox-centre offset (mirrors getBBox() centring from facesjs) ---
        // Appending translate(-cx, -cy) ensures the feature's geometric centre
        // lands exactly on the target position (px, py) after all preceding
        // transforms.  This matches the JavaScript:
        //   translate(x - bbox.cx, y - bbox.cy)
        if (hasPosition && center != null) {
            appendTranslate(t, -center[0], -center[1]);
        }

        return t.toString().trim();
    }

    private static double fatScale(double fatness) {
        return 0.8 + 0.2 * fatness;
    }

    // -------------------------------------------------------------------------
    // SVG bounding-box computation (pure Java, no AWT — Android-compatible)
    // -------------------------------------------------------------------------

    /**
     * Pattern matching a single path {@code d=} attribute value.
     * Works for both {@code d="..."} and {@code d='...'} quoting.
     */
    private static final Pattern ATTR_D =
            Pattern.compile("\\bd=[\"']([^\"']+)[\"']");

    /**
     * Tokeniser for SVG path data: matches either a single letter command or a
     * floating-point number (optionally signed, scientific notation allowed).
     */
    private static final Pattern PATH_TOKEN = Pattern.compile(
            "[MmLlHhVvCcSsQqTtAaZz]"
            + "|[-+]?(?:[0-9]+\\.?[0-9]*|\\.[0-9]+)(?:[eE][-+]?[0-9]+)?");

    /**
     * Computes the approximate bounding-box centre of all {@code <path>}
     * elements in an SVG fragment.  Uses the control-point convex hull for
     * Bézier curves, which is a good-enough approximation for centering.
     *
     * <p>Does not use any {@code java.awt} classes, making it safe on Android.
     *
     * @param svgFragment raw SVG fragment (may contain colour placeholders)
     * @return {cx, cy} centre of the bounding box, or {0, 0} if unparseable
     */
    static double[] computeCenter(String svgFragment) {
        double minX = Double.MAX_VALUE,  maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.MAX_VALUE,  maxY = Double.NEGATIVE_INFINITY;

        Matcher dm = ATTR_D.matcher(svgFragment);
        while (dm.find()) {
            double[] box = pathBbox(dm.group(1));
            if (box != null) {
                if (box[0] < minX) minX = box[0];
                if (box[1] > maxX) maxX = box[1];
                if (box[2] < minY) minY = box[2];
                if (box[3] > maxY) maxY = box[3];
            }
        }
        if (minX == Double.MAX_VALUE) return new double[]{0, 0};
        return new double[]{(minX + maxX) / 2.0, (minY + maxY) / 2.0};
    }

    /**
     * Returns [minX, maxX, minY, maxY] for a single SVG path {@code d=} string,
     * or {@code null} if no coordinates could be parsed.
     *
     * <p>Handles absolute and relative M, L, H, V, C, S, Q, T, A, Z commands.
     * Bézier control points are included in the bbox (convex-hull approximation).
     */
    private static double[] pathBbox(String d) {
        double minX = Double.MAX_VALUE,  maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.MAX_VALUE,  maxY = Double.NEGATIVE_INFINITY;
        double curX = 0, curY = 0;
        char   cmd  = 'M';
        boolean rel  = false;
        List<Double> nums = new ArrayList<>();

        Matcher m = PATH_TOKEN.matcher(d);
        while (m.find()) {
            String tok = m.group();
            char c0 = tok.charAt(0);
            if (Character.isLetter(c0)) {
                // Process any accumulated numbers for the *previous* command
                double[] upd = applyBbox(cmd, rel, nums, curX, curY, minX, maxX, minY, maxY);
                minX = upd[0]; maxX = upd[1]; minY = upd[2]; maxY = upd[3];
                curX = upd[4]; curY = upd[5];

                cmd = Character.toUpperCase(c0);
                rel = Character.isLowerCase(c0);
                nums.clear();
            } else {
                try { nums.add(Double.parseDouble(tok)); }
                catch (NumberFormatException ignored) { /* skip */ }
            }
        }
        // Process the last command
        double[] upd = applyBbox(cmd, rel, nums, curX, curY, minX, maxX, minY, maxY);
        minX = upd[0]; maxX = upd[1]; minY = upd[2]; maxY = upd[3];

        if (minX == Double.MAX_VALUE) return null;
        return new double[]{minX, maxX, minY, maxY};
    }

    /**
     * Processes one path command's number list, updating the running
     * bounding-box and current-position values.
     *
     * @return double[6] = {minX, maxX, minY, maxY, newCurX, newCurY}
     */
    private static double[] applyBbox(char cmd, boolean rel, List<Double> nums,
                                      double cx, double cy,
                                      double minX, double maxX, double minY, double maxY) {
        int n = nums.size();
        switch (cmd) {
            case 'M': case 'L': case 'T': {
                for (int i = 0; i + 1 < n; i += 2) {
                    double x = nums.get(i)   + (rel ? cx : 0);
                    double y = nums.get(i+1) + (rel ? cy : 0);
                    if (x < minX) minX = x; if (x > maxX) maxX = x;
                    if (y < minY) minY = y; if (y > maxY) maxY = y;
                    cx = x; cy = y;
                }
                break;
            }
            case 'H': {
                for (int i = 0; i < n; i++) {
                    double x = nums.get(i) + (rel ? cx : 0);
                    if (x < minX) minX = x; if (x > maxX) maxX = x;
                    cx = x;
                }
                break;
            }
            case 'V': {
                for (int i = 0; i < n; i++) {
                    double y = nums.get(i) + (rel ? cy : 0);
                    if (y < minY) minY = y; if (y > maxY) maxY = y;
                    cy = y;
                }
                break;
            }
            case 'C': {
                for (int i = 0; i + 5 < n; i += 6) {
                    for (int j = 0; j < 6; j += 2) {
                        double x = nums.get(i+j)   + (rel ? cx : 0);
                        double y = nums.get(i+j+1) + (rel ? cy : 0);
                        if (x < minX) minX = x; if (x > maxX) maxX = x;
                        if (y < minY) minY = y; if (y > maxY) maxY = y;
                    }
                    cx = nums.get(i+4) + (rel ? cx : 0);
                    cy = nums.get(i+5) + (rel ? cy : 0);
                }
                break;
            }
            case 'S': case 'Q': {
                for (int i = 0; i + 3 < n; i += 4) {
                    for (int j = 0; j < 4; j += 2) {
                        double x = nums.get(i+j)   + (rel ? cx : 0);
                        double y = nums.get(i+j+1) + (rel ? cy : 0);
                        if (x < minX) minX = x; if (x > maxX) maxX = x;
                        if (y < minY) minY = y; if (y > maxY) maxY = y;
                    }
                    cx = nums.get(i+2) + (rel ? cx : 0);
                    cy = nums.get(i+3) + (rel ? cy : 0);
                }
                break;
            }
            case 'A': {
                // args: rx ry x-rot large-arc sweep x y (7 per arc)
                for (int i = 0; i + 6 < n; i += 7) {
                    double rx = nums.get(i);
                    double ry = nums.get(i+1);
                    double ex = nums.get(i+5) + (rel ? cx : 0);
                    double ey = nums.get(i+6) + (rel ? cy : 0);
                    // Approximate arc extents using the endpoint ± radii
                    double ax1 = ex - rx, ax2 = ex + rx;
                    double ay1 = ey - ry, ay2 = ey + ry;
                    if (ax1 < minX) minX = ax1; if (ax2 > maxX) maxX = ax2;
                    if (ay1 < minY) minY = ay1; if (ay2 > maxY) maxY = ay2;
                    cx = ex; cy = ey;
                }
                break;
            }
            case 'Z': default:
                break;
        }
        return new double[]{minX, maxX, minY, maxY, cx, cy};
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
