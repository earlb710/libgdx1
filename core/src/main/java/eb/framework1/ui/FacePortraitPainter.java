package eb.framework1.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import eb.framework1.face.FaceConfig;
import eb.framework1.face.FaceSvgBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders face portraits for NPC characters by rasterising their
 * {@link FaceConfig} to a cached libGDX {@link Texture}.
 *
 * <p>SVG path data (M, L, C, Z commands) is parsed directly from the
 * {@link FaceSvgBuilder.SvgTemplateLoader} templates, transformed with the
 * same affine-matrix logic as {@link FaceSvgBuilder}, and filled using a
 * scan-line polygon algorithm on a {@link Pixmap}.  Generated textures are
 * cached by character id to avoid redundant rasterisation.
 *
 * <p>This class requires a libGDX context (for {@link Pixmap} and
 * {@link Texture}) and must be used on the OpenGL thread.
 */
public final class FacePortraitPainter {

    /** Portrait output width in pixels. */
    public static final int PORTRAIT_W = 270;
    /** Portrait output height in pixels. */
    public static final int PORTRAIT_H = 405;

    /** Face SVG canonical dimensions (400 × 600). */
    private static final float SVG_W = 400f;
    private static final float SVG_H = 600f;

    /** Maximum bezier subdivision depth to control recursion. */
    private static final int MAX_BEZIER_DEPTH = 8;
    /** Pixel-space flatness tolerance for bezier subdivision. */
    private static final float BEZIER_TOL = 0.5f;

    /**
     * When {@code true}, logs one line per SVG part used during portrait
     * rendering. Useful for diagnosing which templates contribute to a
     * portrait. Set to {@code false} in production to avoid log noise.
     */
    static boolean DEBUG_PARTS = true;

    // -------------------------------------------------------------------------
    // Feature layout (mirrors FaceSvgBuilder.FEATURE_INFOS)
    // -------------------------------------------------------------------------

    private static final String[] FEATURE_NAMES = {
        "hairBg", "body", "jersey", "ear", "head", "eyeLine", "smileLine",
        "miscLine", "facialHair", "eye", "eyebrow", "mouth", "nose",
        "hair", "glasses", "accessories"
    };

    /** Pixel positions {[x,y], ...} for positioned features; null = no explicit position. */
    private static final int[][][] FEATURE_POSITIONS = {
        null,                            // hairBg
        null,                            // body
        null,                            // jersey
        {{55, 325}, {345, 325}},         // ear (left, right)
        null,                            // head
        null,                            // eyeLine
        {{150, 435}, {250, 435}},        // smileLine (left, right)
        null,                            // miscLine
        null,                            // facialHair
        {{140, 310}, {260, 310}},        // eye (left, right)
        {{140, 270}, {260, 270}},        // eyebrow (left, right)
        {{200, 440}},                    // mouth
        {{200, 370}},                    // nose
        null,                            // hair
        null,                            // glasses
        null,                            // accessories
    };

    /** Whether the feature scales with fatness when it has no explicit position. */
    // Note: fatness is now applied as a global x-centred transform in renderFace()
    // rather than per-feature, so this array is retained only for documentation.
    @SuppressWarnings("unused")
    private static final boolean[] FEATURE_SCALE_FATNESS = {
        true,  // hairBg
        false, // body
        false, // jersey
        true,  // ear
        true,  // head
        false, // eyeLine
        false, // smileLine
        false, // miscLine
        true,  // facialHair
        false, // eye
        false, // eyebrow
        false, // mouth
        false, // nose
        true,  // hair
        true,  // glasses
        true,  // accessories
    };

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final FaceSvgBuilder.SvgTemplateLoader loader;
    private final Map<String, Texture>             cache = new HashMap<>();

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * @param loader SVG template supplier; must not be {@code null}
     */
    public FacePortraitPainter(FaceSvgBuilder.SvgTemplateLoader loader) {
        if (loader == null) throw new IllegalArgumentException("loader must not be null");
        this.loader = loader;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns a {@link Texture} portrait for the given face config, generating
     * and caching it on first call.
     *
     * @param characterId stable identifier used as the cache key
     * @param face        face configuration to render; {@code null} → {@code null}
     * @return a cached texture, or {@code null} if {@code face} is {@code null}
     */
    public Texture getPortrait(String characterId, FaceConfig face) {
        if (face == null) return null;
        Texture t = cache.get(characterId);
        if (t == null) {
            t = buildTexture(face);
            cache.put(characterId, t);
        }
        return t;
    }

    /** Disposes all cached textures and clears the cache. */
    public void dispose() {
        for (Texture t : cache.values()) t.dispose();
        cache.clear();
    }

    // -------------------------------------------------------------------------
    // Portrait building
    // -------------------------------------------------------------------------

    private Texture buildTexture(FaceConfig face) {
        Pixmap pm = new Pixmap(PORTRAIT_W, PORTRAIT_H, Pixmap.Format.RGBA8888);
        pm.setColor(1f, 1f, 1f, 1f); // white background
        pm.fill();
        renderFace(face, pm);
        Texture tex = new Texture(pm);
        pm.dispose();
        return tex;
    }

    /** Face canonical horizontal centre in SVG space (400 px canvas). */
    private static final float SVG_CX = 200f;

    private void renderFace(FaceConfig face, Pixmap pm) {
        float sx = PORTRAIT_W / SVG_W;
        float sy = PORTRAIT_H / SVG_H;
        double bodySize = face.body.size;
        double fatness  = face.fatness;

        // Global fatness transform: scale all x coordinates around the face
        // centre (SVG_CX = 200) to keep the head and features aligned.
        // fs = 0.8 + 0.2 * fatness  (range 0.8 – 1.0)
        // x' = (x - SVG_CX) * fs + SVG_CX  = x*fs + SVG_CX*(1-fs)
        // As a 2×3 matrix: {fs, 0,  0, 1,  SVG_CX*(1-fs), 0}
        float fs = (float) (0.8 + 0.2 * fatness);
        float[] fatMat = {fs, 0f, 0f, 1f, SVG_CX * (1f - fs), 0f};

        for (int fi = 0; fi < FEATURE_NAMES.length; fi++) {
            String name = FEATURE_NAMES[fi];
            String id   = getFeatureId(face, name);
            if (id == null || "none".equals(id)) continue;

            String tmpl = loader.getSvgTemplate(name, id);
            if (tmpl == null || tmpl.isEmpty()) continue;

            // Apply head-shave placeholder
            if ("head".equals(name)) {
                tmpl = tmpl.replace("$[faceShave]", face.head.shave);
                tmpl = tmpl.replace("$[headShave]", face.head.shave);
            }

            // Substitute colour placeholders with resolved hex values
            tmpl = applyColors(tmpl, face);

            // Convert <ellipse> / <circle> to <path> so the scan-line
            // renderer can handle them (pupils, nostrils, blush ellipses etc.)
            tmpl = expandShapes(tmpl);

            int[][]  positions = FEATURE_POSITIONS[fi];
            boolean  isBody    = "body".equals(name) || "jersey".equals(name);
            int      angle     = getAngle(face, name);
            double   size      = getSize(face, name);
            boolean  flip      = getFlip(face, name);

            // Bounding-box centre for positioned features
            double[] center = (positions != null) ? FaceSvgBuilder.computeCenter(tmpl) : null;

            // CSS class → fill colour map parsed from the template's <style> block
            Map<String, Integer> cssColors = parseCssColors(tmpl);

            int posCount = (positions != null) ? positions.length : 1;
            for (int pi = 0; pi < posCount; pi++) {
                float[] featureMat = buildMatrix(
                        positions, pi, isBody, flip, size, angle,
                        bodySize, center, pi);
                // Compose: fatness transform applied after the feature transform
                float[] matrix = matMul(fatMat, featureMat);
                renderPaths(tmpl, cssColors, matrix, sx, sy, pm);
            }

            if (DEBUG_PARTS) Gdx.app.log("FacePortrait", name + ": " + id);
        }
    }

    // -------------------------------------------------------------------------
    // Affine transform matrix
    // -------------------------------------------------------------------------

    /**
     * Builds a 2×3 affine matrix for the given feature instance, replicating
     * the translate/rotate/scale logic from {@link FaceSvgBuilder}.
     *
     * <p>Storage convention: {@code float[6] = {m00, m10, m01, m11, m02, m12}}
     * where {@code x' = m00·x + m01·y + m02} and {@code y' = m10·x + m11·y + m12}.
     *
     * <p>Fatness scaling is NOT applied here; the caller composes a global
     * fatness matrix afterwards so that ALL features are scaled symmetrically
     * around {@link #SVG_CX}.
     */
    private static float[] buildMatrix(int[][] positions, int posIdx,
                                       boolean isBody, boolean flip, double size,
                                       int angle, double bodySize,
                                       double[] center, int instanceIdx) {
        float[] m = {1, 0, 0, 1, 0, 0}; // identity

        boolean hasPos    = (positions != null);
        boolean needScale = isBody || flip || instanceIdx == 1 || size != 1.0;
        boolean needRot   = angle != 0;

        // Optimised path: single combined translate when nothing else is needed
        if (hasPos && center != null && !needScale && !needRot) {
            float dx = (float) (positions[posIdx][0] - center[0]);
            float dy = (float) (positions[posIdx][1] - center[1]);
            return new float[]{1, 0, 0, 1, dx, dy};
        }

        // Step 1: translate to target position
        if (hasPos) {
            float px = positions[posIdx][0];
            float py = positions[posIdx][1];
            m = matMul(m, new float[]{1, 0, 0, 1, px, py});
        }

        // Step 2: rotation (eye / eyebrow; mirrored for right instance)
        if (needRot) {
            double sign = (instanceIdx == 0) ? 1.0 : -1.0;
            double rad  = Math.toRadians(sign * angle);
            float  cosA = (float) Math.cos(rad);
            float  sinA = (float) Math.sin(rad);
            m = matMul(m, new float[]{cosA, sinA, -sinA, cosA, 0, 0});
        }

        // Step 3: scale
        if (isBody) {
            // Center the bodySize scale at SVG_CX (x=200) so the jersey stays
            // horizontally centred regardless of the bodySize value.
            // Equivalent to: translate(200*(1-bodySize), 0) then scale(bodySize, 1)
            m = matMul(m, new float[]{1, 0, 0, 1, (float)(SVG_CX * (1 - bodySize)), 0});
            m = matMul(m, new float[]{(float) bodySize, 0, 0, 1, 0, 0});
        } else if (flip || instanceIdx == 1) {
            m = matMul(m, new float[]{-(float) size, 0, 0, (float) size, 0, 0});
        } else if (size != 1.0) {
            m = matMul(m, new float[]{(float) size, 0, 0, (float) size, 0, 0});
        }

        // Step 4: bbox-centre offset
        if (hasPos && center != null) {
            m = matMul(m, new float[]{1, 0, 0, 1, -(float) center[0], -(float) center[1]});
        }

        return m;
    }

    /** Multiplies two 2×3 affine matrices: {@code result = a × b}. */
    static float[] matMul(float[] a, float[] b) {
        return new float[]{
            a[0]*b[0] + a[2]*b[1],
            a[1]*b[0] + a[3]*b[1],
            a[0]*b[2] + a[2]*b[3],
            a[1]*b[2] + a[3]*b[3],
            a[0]*b[4] + a[2]*b[5] + a[4],
            a[1]*b[4] + a[3]*b[5] + a[5]
        };
    }

    private static float applyX(float[] m, float x, float y) { return m[0]*x + m[2]*y + m[4]; }
    private static float applyY(float[] m, float x, float y) { return m[1]*x + m[3]*y + m[5]; }

    // -------------------------------------------------------------------------
    // SVG path element rendering
    // -------------------------------------------------------------------------

    private static final Pattern PATH_ELEM_PAT =
            Pattern.compile("<path\\b([^>]*?)(?:/>|>)", Pattern.DOTALL);
    private static final Pattern ATTR_PAT =
            Pattern.compile("([\\w-]+)\\s*=\\s*\"([^\"]*)\"");

    private static void renderPaths(String tmpl, Map<String, Integer> cssColors,
                                    float[] matrix, float sx, float sy, Pixmap pm) {
        Map<String, Integer> cssStrokes      = parseCssStrokes(tmpl);
        Map<String, Float>   cssStrokeWidths = parseCssStrokeWidths(tmpl);

        Matcher pm2 = PATH_ELEM_PAT.matcher(tmpl);
        while (pm2.find()) {
            Map<String, String> attrs = extractAttrs(pm2.group(0));
            String dStr = attrs.get("d");
            if (dStr == null || dStr.isEmpty()) continue;

            List<List<float[]>> contours = flattenPathToContours(dStr, matrix, sx, sy);

            // ── Fill ──────────────────────────────────────────────────────────
            int fillColor = resolveFillColor(attrs, cssColors);
            if ((fillColor & 0xFF) != 0) {
                String fillRule = attrs.get("fill-rule");
                if ("evenodd".equals(fillRule)) {
                    List<List<float[]>> valid = new ArrayList<>();
                    for (List<float[]> c : contours) {
                        if (c.size() >= 3) valid.add(c);
                    }
                    if (!valid.isEmpty()) fillPolygonEvenOdd(pm, valid, fillColor);
                } else {
                    for (List<float[]> contour : contours) {
                        if (contour.size() >= 3) {
                            fillPolygon(pm, contour, fillColor);
                        }
                    }
                }
            }

            // ── Stroke ────────────────────────────────────────────────────────
            int strokeColor = resolveStrokeColor(attrs, cssStrokes);
            if ((strokeColor & 0xFF) != 0) {
                float swSvg = resolveStrokeWidth(attrs, cssStrokeWidths);
                // Convert stroke-width from SVG units to portrait pixels (use
                // the horizontal scale factor; clamp to 1–8 px for readability).
                float swPx = Math.max(1f, Math.min(8f, swSvg * sx));
                for (List<float[]> contour : contours) {
                    if (contour.size() >= 2) {
                        drawStroke(pm, contour, strokeColor, swPx);
                    }
                }
            }
        }
    }

    private static Map<String, String> extractAttrs(String elem) {
        Map<String, String> map = new LinkedHashMap<>();
        Matcher m = ATTR_PAT.matcher(elem);
        while (m.find()) map.put(m.group(1), m.group(2));
        return map;
    }

    private static int resolveFillColor(Map<String, String> attrs,
                                        Map<String, Integer> css) {
        String fill  = attrs.get("fill");
        String clazz = attrs.get("class");
        String style = attrs.get("style");
        // Inline fill attribute takes highest priority
        if (fill != null && !fill.isEmpty()) {
            if ("none".equals(fill)) return 0;
            return parseCssColor(fill.trim());
        }
        // Inline style attribute: parse fill: ... declarations
        if (style != null && !style.isEmpty()) {
            java.util.regex.Matcher sm = Pattern.compile(
                    "\\bfill\\s*:\\s*([^;,}]+)").matcher(style);
            if (sm.find()) {
                String c = sm.group(1).trim();
                if ("none".equals(c)) return 0;
                int color = parseCssColor(c);
                if (color != 0) return color;
            }
        }
        // CSS class fill (includes explicit fill:none stored as 0)
        if (clazz != null) {
            for (String cls : clazz.split("\\s+")) {
                Integer c = css.get(cls.trim());
                if (c != null) return c;
            }
        }
        // SVG default: black (matches browser rendering when no fill is specified)
        return 0x000000FF;
    }

    // -------------------------------------------------------------------------
    // CSS style block parsing
    // -------------------------------------------------------------------------

    private static final Pattern CSS_RULE_PAT =
            Pattern.compile("\\.([\\w-]+)\\s*\\{([^}]*)\\}", Pattern.DOTALL);
    private static final Pattern CSS_FILL_PAT =
            Pattern.compile("\\bfill\\s*:\\s*([^;},]+)");
    private static final Pattern CSS_STROKE_PAT =
            Pattern.compile("\\bstroke\\s*:\\s*([^;},]+)");
    private static final Pattern CSS_STROKE_W_PAT =
            Pattern.compile("\\bstroke-width\\s*:\\s*([\\d.]+)");

    private static Map<String, Integer> parseCssColors(String svg) {
        Map<String, Integer> map = new HashMap<>();
        Matcher rm = CSS_RULE_PAT.matcher(svg);
        while (rm.find()) {
            String cls   = rm.group(1);
            String rules = rm.group(2);
            Matcher fm   = CSS_FILL_PAT.matcher(rules);
            if (fm.find()) {
                String c = fm.group(1).trim();
                if ("none".equals(c)) {
                    map.put(cls, 0); // explicit fill:none → transparent
                } else if (!c.isEmpty()) {
                    int color = parseCssColor(c);
                    if (color != 0) map.put(cls, color);
                }
            }
        }
        return map;
    }

    /**
     * Parses the {@code <style>} block for per-class stroke colour values.
     * Keys are CSS class names (without leading {@code .}).
     * A value of 0 means {@code stroke:none}.
     */
    private static Map<String, Integer> parseCssStrokes(String svg) {
        Map<String, Integer> map = new HashMap<>();
        Matcher rm = CSS_RULE_PAT.matcher(svg);
        while (rm.find()) {
            String cls   = rm.group(1);
            String rules = rm.group(2);
            Matcher sm   = CSS_STROKE_PAT.matcher(rules);
            if (sm.find()) {
                String c = sm.group(1).trim();
                if ("none".equals(c)) {
                    map.put(cls, 0);
                } else if (!c.isEmpty()) {
                    int color = parseCssColor(c);
                    map.put(cls, color);
                }
            }
        }
        return map;
    }

    /**
     * Parses the {@code <style>} block for per-class stroke-width values.
     * Returns a map of class name → stroke-width (in SVG units, &gt; 0).
     */
    private static Map<String, Float> parseCssStrokeWidths(String svg) {
        Map<String, Float> map = new HashMap<>();
        Matcher rm = CSS_RULE_PAT.matcher(svg);
        while (rm.find()) {
            String cls   = rm.group(1);
            String rules = rm.group(2);
            Matcher wm   = CSS_STROKE_W_PAT.matcher(rules);
            if (wm.find()) {
                try {
                    map.put(cls, Float.parseFloat(wm.group(1).trim()));
                } catch (NumberFormatException ignored) {}
            }
        }
        return map;
    }

    /**
     * Resolves the effective stroke colour for a {@code <path>} element.
     *
     * <p>Priority order (matching browsers):
     * inline {@code stroke=} attribute &gt; inline {@code style=} stroke &gt;
     * CSS class stroke &gt; SVG default (transparent/none).
     *
     * @return RGBA8888 colour int, or 0 if stroke is {@code none} / absent.
     */
    private static int resolveStrokeColor(Map<String, String> attrs,
                                          Map<String, Integer> cssStrokes) {
        String stroke = attrs.get("stroke");
        String clazz  = attrs.get("class");
        String style  = attrs.get("style");
        // Inline stroke attribute
        if (stroke != null && !stroke.isEmpty()) {
            if ("none".equals(stroke)) return 0;
            int c = parseCssColor(stroke.trim());
            if (c != 0) return c;
        }
        // Inline style attribute
        if (style != null && !style.isEmpty()) {
            java.util.regex.Matcher sm = CSS_STROKE_PAT.matcher(style);
            if (sm.find()) {
                String c = sm.group(1).trim();
                if ("none".equals(c)) return 0;
                int col = parseCssColor(c);
                if (col != 0) return col;
            }
        }
        // CSS class
        if (clazz != null) {
            for (String cls : clazz.split("\\s+")) {
                Integer c = cssStrokes.get(cls.trim());
                if (c != null) return c;
            }
        }
        return 0; // SVG default: no stroke
    }

    /**
     * Resolves the effective stroke-width (in SVG units) for a {@code <path>}.
     * Falls back to 1.0 if none specified but a visible stroke color is present.
     */
    private static float resolveStrokeWidth(Map<String, String> attrs,
                                            Map<String, Float> cssStrokeWidths) {
        String sw    = attrs.get("stroke-width");
        String clazz = attrs.get("class");
        String style = attrs.get("style");
        // Inline attribute
        if (sw != null && !sw.isEmpty()) {
            try { return Float.parseFloat(sw.trim().replaceAll("[^0-9.]", "")); }
            catch (NumberFormatException ignored) {}
        }
        // Inline style attribute
        if (style != null && !style.isEmpty()) {
            java.util.regex.Matcher wm = CSS_STROKE_W_PAT.matcher(style);
            if (wm.find()) {
                try { return Float.parseFloat(wm.group(1).trim()); }
                catch (NumberFormatException ignored) {}
            }
        }
        // CSS class
        if (clazz != null) {
            for (String cls : clazz.split("\\s+")) {
                Float w = cssStrokeWidths.get(cls.trim());
                if (w != null) return w;
            }
        }
        return 1.0f;
    }

    /** Parses a CSS/SVG colour string to an RGBA8888 int (0 = transparent/unknown). */
    static int parseCssColor(String s) {
        s = s.trim();
        if (s.startsWith("#")) {
            String h = s.substring(1);
            if (h.length() == 3) {
                h = "" + h.charAt(0) + h.charAt(0)
                       + h.charAt(1) + h.charAt(1)
                       + h.charAt(2) + h.charAt(2);
            }
            if (h.length() == 6) {
                try {
                    int r = Integer.parseInt(h.substring(0, 2), 16);
                    int g = Integer.parseInt(h.substring(2, 4), 16);
                    int b = Integer.parseInt(h.substring(4, 6), 16);
                    return (r << 24) | (g << 16) | (b << 8) | 0xFF;
                } catch (NumberFormatException ignored) { /* fall through */ }
            }
        } else if ("black".equals(s)) {
            return 0x000000FF;
        } else if ("white".equals(s)) {
            return 0xFFFFFFFF;
        } else if (s.startsWith("rgba")) {
            Matcher m = Pattern.compile("rgba\\s*\\(([^)]+)\\)").matcher(s);
            if (m.find()) {
                float[] ns = parseNums(m.group(1));
                if (ns.length >= 4) {
                    int r = (int) ns[0], g = (int) ns[1], b = (int) ns[2];
                    int a = (int) (ns[3] * 255);
                    return (r << 24) | (g << 16) | (b << 8) | (a & 0xFF);
                }
            }
        } else if (s.startsWith("rgb")) {
            Matcher m = Pattern.compile("rgb\\s*\\(([^)]+)\\)").matcher(s);
            if (m.find()) {
                float[] ns = parseNums(m.group(1));
                if (ns.length >= 3) {
                    int r = (int) ns[0], g = (int) ns[1], b = (int) ns[2];
                    return (r << 24) | (g << 16) | (b << 8) | 0xFF;
                }
            }
        }
        return 0; // unknown / transparent
    }

    // -------------------------------------------------------------------------
    // SVG path flattening
    // -------------------------------------------------------------------------

    /**
     * Parses the SVG path data string {@code d}, applies the affine matrix
     * and output scale, and returns a list of closed contours (each a list of
     * screen-space vertex pairs).
     */
    private static List<List<float[]>> flattenPathToContours(
            String d, float[] matrix, float sx, float sy) {

        List<List<float[]>> contours = new ArrayList<>();
        List<float[]> current = new ArrayList<>();

        float cx = 0, cy = 0; // current point
        float mx = 0, my = 0; // start of current subpath (for Z)
        float lcpx = 0, lcpy = 0; // last cubic control point (for S command)
        boolean lastWasCubic = false;

        List<Object> tokens = tokenizePath(d);
        int i = 0;
        char cmd = 'M';
        boolean rel = false;

        while (i < tokens.size()) {
            Object tok = tokens.get(i);
            if (tok instanceof Character) {
                char c = (char) (Character) tok;
                cmd = Character.toUpperCase(c);
                rel = Character.isLowerCase(c);
                if (cmd != 'C' && cmd != 'S') lastWasCubic = false;
                i++;
                continue;
            }

            switch (cmd) {
                case 'M': {
                    float x = nextF(tokens, i) + (rel ? cx : 0); i++;
                    float y = nextF(tokens, i) + (rel ? cy : 0); i++;
                    if (!current.isEmpty()) {
                        contours.add(current);
                        current = new ArrayList<>();
                    }
                    cx = x; cy = y; mx = x; my = y;
                    current.add(pt(matrix, sx, sy, x, y));
                    lastWasCubic = false;
                    // Implicit L after M
                    cmd = rel ? 'l' : 'L';
                    rel = (cmd == 'l');
                    break;
                }
                case 'L': {
                    float x = nextF(tokens, i) + (rel ? cx : 0); i++;
                    float y = nextF(tokens, i) + (rel ? cy : 0); i++;
                    cx = x; cy = y;
                    current.add(pt(matrix, sx, sy, x, y));
                    lastWasCubic = false;
                    break;
                }
                case 'H': {
                    float x = nextF(tokens, i) + (rel ? cx : 0); i++;
                    cx = x;
                    current.add(pt(matrix, sx, sy, cx, cy));
                    lastWasCubic = false;
                    break;
                }
                case 'V': {
                    float y = nextF(tokens, i) + (rel ? cy : 0); i++;
                    cy = y;
                    current.add(pt(matrix, sx, sy, cx, cy));
                    lastWasCubic = false;
                    break;
                }
                case 'C': {
                    float x1 = nextF(tokens, i) + (rel ? cx : 0); i++;
                    float y1 = nextF(tokens, i) + (rel ? cy : 0); i++;
                    float x2 = nextF(tokens, i) + (rel ? cx : 0); i++;
                    float y2 = nextF(tokens, i) + (rel ? cy : 0); i++;
                    float x3 = nextF(tokens, i) + (rel ? cx : 0); i++;
                    float y3 = nextF(tokens, i) + (rel ? cy : 0); i++;
                    flattenCubic(current, matrix, sx, sy, cx, cy, x1, y1, x2, y2, x3, y3, 0);
                    lcpx = x2; lcpy = y2;
                    lastWasCubic = true;
                    cx = x3; cy = y3;
                    break;
                }
                case 'S': {
                    // Smooth cubic bezier: first control point is reflection of last
                    float x2 = nextF(tokens, i) + (rel ? cx : 0); i++;
                    float y2 = nextF(tokens, i) + (rel ? cy : 0); i++;
                    float x3 = nextF(tokens, i) + (rel ? cx : 0); i++;
                    float y3 = nextF(tokens, i) + (rel ? cy : 0); i++;
                    float x1 = lastWasCubic ? 2 * cx - lcpx : cx;
                    float y1 = lastWasCubic ? 2 * cy - lcpy : cy;
                    flattenCubic(current, matrix, sx, sy, cx, cy, x1, y1, x2, y2, x3, y3, 0);
                    lcpx = x2; lcpy = y2;
                    lastWasCubic = true;
                    cx = x3; cy = y3;
                    break;
                }
                case 'Q': {
                    // Quadratic bezier → convert to cubic and flatten
                    float x1 = nextF(tokens, i) + (rel ? cx : 0); i++;
                    float y1 = nextF(tokens, i) + (rel ? cy : 0); i++;
                    float x2 = nextF(tokens, i) + (rel ? cx : 0); i++;
                    float y2 = nextF(tokens, i) + (rel ? cy : 0); i++;
                    // Convert Q(P0,P1,P2) to cubic: c1 = P0 + 2/3*(P1-P0), c2 = P2 + 2/3*(P1-P2)
                    float cx1 = cx + 2f/3f*(x1 - cx);
                    float cy1 = cy + 2f/3f*(y1 - cy);
                    float cx2 = x2 + 2f/3f*(x1 - x2);
                    float cy2 = y2 + 2f/3f*(y1 - y2);
                    flattenCubic(current, matrix, sx, sy, cx, cy, cx1, cy1, cx2, cy2, x2, y2, 0);
                    lastWasCubic = false;
                    cx = x2; cy = y2;
                    break;
                }
                case 'A': {
                    // Elliptical arc: convert to cubic bezier curves
                    float arx  = nextF(tokens, i); i++;
                    float ary  = nextF(tokens, i); i++;
                    float xRot = nextF(tokens, i); i++;
                    float laF  = nextF(tokens, i); i++;
                    float sweF = nextF(tokens, i); i++;
                    float ax   = nextF(tokens, i) + (rel ? cx : 0); i++;
                    float ay   = nextF(tokens, i) + (rel ? cy : 0); i++;
                    if (arx < 0.01f || ary < 0.01f || (cx == ax && cy == ay)) {
                        cx = ax; cy = ay;
                        current.add(pt(matrix, sx, sy, cx, cy));
                    } else {
                        arcToBezier(current, matrix, sx, sy, cx, cy, arx, ary,
                                xRot, (int) laF, (int) sweF, ax, ay);
                        cx = ax; cy = ay;
                    }
                    lastWasCubic = false;
                    break;
                }
                case 'Z': {
                    current.add(pt(matrix, sx, sy, mx, my));
                    if (!current.isEmpty()) {
                        contours.add(current);
                        current = new ArrayList<>();
                    }
                    cx = mx; cy = my;
                    lastWasCubic = false;
                    break;
                }
                default:
                    i++; // skip unrecognised numeric token
                    break;
            }
        }
        if (!current.isEmpty()) contours.add(current);
        return contours;
    }

    private static float[] pt(float[] m, float sx, float sy, float x, float y) {
        return new float[]{applyX(m, x, y) * sx, applyY(m, x, y) * sy};
    }

    private static void flattenCubic(List<float[]> verts, float[] matrix,
                                     float sx, float sy,
                                     float x0, float y0,
                                     float x1, float y1,
                                     float x2, float y2,
                                     float x3, float y3,
                                     int depth) {
        if (depth >= MAX_BEZIER_DEPTH) {
            verts.add(pt(matrix, sx, sy, x3, y3));
            return;
        }
        float dx  = x3 - x0, dy = y3 - y0;
        float len2 = dx*dx + dy*dy;
        float d1, d2;
        if (len2 < 0.01f) {
            d1 = dist(x1, y1, x0, y0);
            d2 = dist(x2, y2, x0, y0);
        } else {
            float sqLen = (float) Math.sqrt(len2);
            d1 = Math.abs(dy*x1 - dx*y1 + x3*y0 - y3*x0) / sqLen;
            d2 = Math.abs(dy*x2 - dx*y2 + x3*y0 - y3*x0) / sqLen;
        }
        if (Math.max(d1, d2) < BEZIER_TOL) {
            verts.add(pt(matrix, sx, sy, x3, y3));
            return;
        }
        // De Casteljau subdivision at t = 0.5
        float m01x = (x0+x1)*.5f, m01y = (y0+y1)*.5f;
        float m12x = (x1+x2)*.5f, m12y = (y1+y2)*.5f;
        float m23x = (x2+x3)*.5f, m23y = (y2+y3)*.5f;
        float m012x = (m01x+m12x)*.5f, m012y = (m01y+m12y)*.5f;
        float m123x = (m12x+m23x)*.5f, m123y = (m12y+m23y)*.5f;
        float midX  = (m012x+m123x)*.5f, midY = (m012y+m123y)*.5f;
        flattenCubic(verts, matrix, sx, sy, x0, y0, m01x, m01y, m012x, m012y, midX, midY, depth+1);
        flattenCubic(verts, matrix, sx, sy, midX, midY, m123x, m123y, m23x, m23y, x3, y3, depth+1);
    }

    private static float dist(float x1, float y1, float x2, float y2) {
        float dx = x1-x2, dy = y1-y2;
        return (float) Math.sqrt(dx*dx + dy*dy);
    }

    // -------------------------------------------------------------------------
    // Scan-line polygon fill
    // -------------------------------------------------------------------------

    private static void fillPolygon(Pixmap pm, List<float[]> verts, int rgba) {
        int W = pm.getWidth(), H = pm.getHeight();
        float minY = Float.MAX_VALUE, maxY = Float.NEGATIVE_INFINITY;
        for (float[] v : verts) {
            if (v[1] < minY) minY = v[1];
            if (v[1] > maxY) maxY = v[1];
        }
        int y0 = Math.max(0, (int) Math.floor(minY));
        int y1 = Math.min(H - 1, (int) Math.ceil(maxY));

        int r = (rgba >> 24) & 0xFF, g = (rgba >> 16) & 0xFF,
            b = (rgba >> 8) & 0xFF,  a = rgba & 0xFF;
        pm.setColor(r / 255f, g / 255f, b / 255f, a / 255f);

        int n = verts.size();
        List<Float> xs = new ArrayList<>();

        for (int y = y0; y <= y1; y++) {
            float fy = y + 0.5f;
            xs.clear();
            for (int i = 0; i < n; i++) {
                float[] va = verts.get(i), vb = verts.get((i + 1) % n);
                float ay = va[1], by = vb[1];
                if ((ay <= fy && by > fy) || (by <= fy && ay > fy)) {
                    xs.add(va[0] + (fy - ay) / (by - ay) * (vb[0] - va[0]));
                }
            }
            if (xs.size() < 2) continue;
            Collections.sort(xs);
            for (int ci = 0; ci + 1 < xs.size(); ci += 2) {
                int x0i = Math.max(0, (int) Math.ceil(xs.get(ci)));
                int x1i = Math.min(W - 1, (int) Math.floor(xs.get(ci + 1)));
                for (int x = x0i; x <= x1i; x++) pm.drawPixel(x, y);
            }
        }
    }

    /**
     * Fills multiple sub-path contours using the SVG even-odd fill rule.
     * All contours are processed together: for each scan-line the intersections
     * from every contour are merged, sorted, and filled between alternating pairs
     * (0→1 fill, 1→2 skip, 2→3 fill, …).  This correctly creates "holes" where
     * inner contours overlap the outer contour, as required by paths with
     * {@code fill-rule="evenodd"} (e.g. beards, goatees).
     */
    private static void fillPolygonEvenOdd(Pixmap pm,
                                           List<List<float[]>> contours,
                                           int rgba) {
        int W = pm.getWidth(), H = pm.getHeight();
        float minY = Float.MAX_VALUE, maxY = Float.NEGATIVE_INFINITY;
        for (List<float[]> c : contours) {
            for (float[] v : c) {
                if (v[1] < minY) minY = v[1];
                if (v[1] > maxY) maxY = v[1];
            }
        }
        int y0 = Math.max(0, (int) Math.floor(minY));
        int y1 = Math.min(H - 1, (int) Math.ceil(maxY));

        int r = (rgba >> 24) & 0xFF, g = (rgba >> 16) & 0xFF,
            b = (rgba >> 8) & 0xFF,  a = rgba & 0xFF;
        pm.setColor(r / 255f, g / 255f, b / 255f, a / 255f);

        List<Float> xs = new ArrayList<>();
        for (int y = y0; y <= y1; y++) {
            float fy = y + 0.5f;
            xs.clear();
            for (List<float[]> contour : contours) {
                int n = contour.size();
                for (int i = 0; i < n; i++) {
                    float[] va = contour.get(i), vb = contour.get((i + 1) % n);
                    float ay = va[1], by = vb[1];
                    if ((ay <= fy && by > fy) || (by <= fy && ay > fy)) {
                        xs.add(va[0] + (fy - ay) / (by - ay) * (vb[0] - va[0]));
                    }
                }
            }
            if (xs.size() < 2) continue;
            Collections.sort(xs);
            for (int ci = 0; ci + 1 < xs.size(); ci += 2) {
                int x0i = Math.max(0, (int) Math.ceil(xs.get(ci)));
                int x1i = Math.min(W - 1, (int) Math.floor(xs.get(ci + 1)));
                for (int x = x0i; x <= x1i; x++) pm.drawPixel(x, y);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Stroke rendering (thick polyline via perpendicular rectangles)
    // -------------------------------------------------------------------------

    /**
     * Draws each segment of {@code contour} as a filled rectangle perpendicular
     * to the segment direction, producing a thick-line stroke effect.
     *
     * <p>This implements the most common SVG stroke cases (solid colour, round
     * or square line-join/cap are approximated).  {@code strokeWidthPx} is the
     * half-width in portrait-pixel space.
     */
    private static void drawStroke(Pixmap pm, List<float[]> contour,
                                   int rgba, float strokeWidthPx) {
        float half = strokeWidthPx / 2f;
        int   n    = contour.size();
        for (int i = 0; i < n - 1; i++) {
            float[] a = contour.get(i), b = contour.get(i + 1);
            float dx = b[0] - a[0], dy = b[1] - a[1];
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len < 0.01f) continue;
            float nx = -dy / len * half;
            float ny =  dx / len * half;
            // Four corners of the thick-line rectangle
            List<float[]> rect = new ArrayList<>(4);
            rect.add(new float[]{a[0] + nx, a[1] + ny});
            rect.add(new float[]{a[0] - nx, a[1] - ny});
            rect.add(new float[]{b[0] - nx, b[1] - ny});
            rect.add(new float[]{b[0] + nx, b[1] + ny});
            fillPolygon(pm, rect, rgba);
        }
    }

    // -------------------------------------------------------------------------
    // <ellipse> / <circle> → <path> expansion
    // -------------------------------------------------------------------------

    /** SVG κ constant for approximating a quarter-ellipse with a cubic bezier. */
    private static final double KAPPA = 0.5522847498;

    private static final Pattern ELLIPSE_TAG_PAT = Pattern.compile(
            "<ellipse\\b([^>]*?)/>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern CIRCLE_TAG_PAT = Pattern.compile(
            "<circle\\b([^>]*?)/>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    /**
     * Converts {@code <ellipse>} and {@code <circle>} elements to equivalent
     * {@code <path>} elements using 4-cubic-bezier approximation (κ≈0.552).
     *
     * <p>All other attributes (class, fill, stroke, style, …) are preserved
     * on the generated {@code <path>} element so that CSS fill/stroke rules
     * continue to apply.
     */
    static String expandShapes(String tmpl) {
        // --- ellipses ---
        Matcher em = ELLIPSE_TAG_PAT.matcher(tmpl);
        StringBuffer sb = new StringBuffer();
        while (em.find()) {
            String attrs = em.group(1);
            Float cx = parseFloatAttr(attrs, "cx");
            Float cy = parseFloatAttr(attrs, "cy");
            Float rx = parseFloatAttr(attrs, "rx");
            Float ry = parseFloatAttr(attrs, "ry");
            if (cx != null && cy != null && rx != null && ry != null) {
                String rest = removeShapeAttrs(attrs, "cx", "cy", "rx", "ry");
                em.appendReplacement(sb, Matcher.quoteReplacement(
                        "<path d=\"" + ellipseToPathD(cx, cy, rx, ry) + "\" " + rest + "/>"));
            } else {
                em.appendReplacement(sb, Matcher.quoteReplacement(em.group(0)));
            }
        }
        em.appendTail(sb);
        tmpl = sb.toString();

        // --- circles ---
        Matcher cm = CIRCLE_TAG_PAT.matcher(tmpl);
        sb = new StringBuffer();
        while (cm.find()) {
            String attrs = cm.group(1);
            Float cx = parseFloatAttr(attrs, "cx");
            Float cy = parseFloatAttr(attrs, "cy");
            Float r  = parseFloatAttr(attrs, "r");
            if (cx != null && cy != null && r != null) {
                String rest = removeShapeAttrs(attrs, "cx", "cy", "r");
                cm.appendReplacement(sb, Matcher.quoteReplacement(
                        "<path d=\"" + ellipseToPathD(cx, cy, r, r) + "\" " + rest + "/>"));
            } else {
                cm.appendReplacement(sb, Matcher.quoteReplacement(cm.group(0)));
            }
        }
        cm.appendTail(sb);
        return sb.toString();
    }

    /**
     * Returns an SVG path {@code d} string for an ellipse centred at
     * ({@code cx},{@code cy}) with radii {@code rx} and {@code ry}, built
     * from four cubic Bézier curves.
     */
    private static String ellipseToPathD(float cx, float cy, float rx, float ry) {
        float k  = (float) KAPPA;
        float krx = k * rx, kry = k * ry;
        return String.format(Locale.US,
                "M%.4f,%.4f " +
                "C%.4f,%.4f,%.4f,%.4f,%.4f,%.4f " +
                "C%.4f,%.4f,%.4f,%.4f,%.4f,%.4f " +
                "C%.4f,%.4f,%.4f,%.4f,%.4f,%.4f " +
                "C%.4f,%.4f,%.4f,%.4f,%.4f,%.4f Z",
                cx + rx, cy,
                cx + rx,  cy - kry,  cx + krx, cy - ry,  cx,      cy - ry,
                cx - krx, cy - ry,   cx - rx,  cy - kry, cx - rx, cy,
                cx - rx,  cy + kry,  cx - krx, cy + ry,  cx,      cy + ry,
                cx + krx, cy + ry,   cx + rx,  cy + kry, cx + rx, cy);
    }

    private static Float parseFloatAttr(String attrs, String attrName) {
        Matcher m = Pattern.compile(
                "\\b" + attrName + "\\s*=\\s*[\"']([^\"']*)[\"']").matcher(attrs);
        if (!m.find()) return null;
        try { return Float.parseFloat(m.group(1).trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private static String removeShapeAttrs(String attrs, String... names) {
        for (String n : names) {
            attrs = attrs.replaceAll("\\b" + n + "\\s*=\\s*[\"'][^\"']*[\"']", "");
        }
        return attrs.trim();
    }

    // -------------------------------------------------------------------------
    // SVG path tokeniser
    // -------------------------------------------------------------------------

    private static List<Object> tokenizePath(String d) {
        List<Object> tokens = new ArrayList<>();
        int i = 0, n = d.length();
        while (i < n) {
            char c = d.charAt(i);
            if (Character.isWhitespace(c) || c == ',') { i++; continue; }
            if (Character.isLetter(c)) {
                tokens.add(c);
                i++;
            } else {
                int start = i;
                if (c == '-' || c == '+') i++;
                // Integer part
                while (i < n && Character.isDigit(d.charAt(i))) i++;
                // Fractional part — consume at most ONE decimal point so that
                // concatenated numbers like "1.6.3" are split into "1.6" and ".3"
                // rather than being swallowed as one unparseable token.
                if (i < n && d.charAt(i) == '.') {
                    i++;
                    while (i < n && Character.isDigit(d.charAt(i))) i++;
                }
                // Exponent part
                if (i < n && (d.charAt(i) == 'e' || d.charAt(i) == 'E')) {
                    i++;
                    if (i < n && (d.charAt(i) == '+' || d.charAt(i) == '-')) i++;
                    while (i < n && Character.isDigit(d.charAt(i))) i++;
                }
                if (i > start) {
                    try { tokens.add(Float.parseFloat(d.substring(start, i))); }
                    catch (NumberFormatException ignored) { /* skip */ }
                } else {
                    i++;
                }
            }
        }
        return tokens;
    }

    private static float nextF(List<Object> tokens, int i) {
        if (i >= tokens.size()) return 0f;
        Object o = tokens.get(i);
        return (o instanceof Float) ? (float) (Float) o : 0f;
    }

    private static float[] parseNums(String s) {
        String[] parts = s.trim().split("[\\s,]+");
        float[] arr = new float[parts.length];
        int count = 0;
        for (String p : parts) {
            try { arr[count++] = Float.parseFloat(p); }
            catch (NumberFormatException ignored) { /* skip */ }
        }
        if (count < arr.length) {
            float[] trimmed = new float[count];
            System.arraycopy(arr, 0, trimmed, 0, count);
            return trimmed;
        }
        return arr;
    }

    // -------------------------------------------------------------------------
    // Colour substitution (same placeholders as FaceSvgBuilder)
    // -------------------------------------------------------------------------

    private static String applyColors(String tmpl, FaceConfig face) {
        tmpl = tmpl.replace("$[skinColor]", face.body.color);
        tmpl = tmpl.replace("$[hairColor]", face.hair.color);
        tmpl = tmpl.replace("$[primary]",
                face.teamColors.length > 0 ? face.teamColors[0] : "#89bfd3");
        tmpl = tmpl.replace("$[secondary]",
                face.teamColors.length > 1 ? face.teamColors[1] : "#7a1319");
        tmpl = tmpl.replace("$[accent]",
                face.teamColors.length > 2 ? face.teamColors[2] : "#07364f");

        // If the template uses the shp0 class but has no <style> block defining it,
        // inject a style that maps shp0 → hairColor. This handles facialHair (beard,
        // goatee, etc.) templates that use shp0 as a placeholder for the hair colour.
        if (tmpl.contains("shp0") && !tmpl.toLowerCase().contains("<style")) {
            tmpl = "<style>.shp0{fill:" + face.hair.color + ";}</style>\n" + tmpl;
        }
        return tmpl;
    }

    // -------------------------------------------------------------------------
    // Feature property helpers (mirrors FaceSvgBuilder)
    // -------------------------------------------------------------------------

    private static String getFeatureId(FaceConfig f, String name) {
        switch (name) {
            case "accessories": return f.accessories.id;
            case "body":        return f.body.id;
            case "ear":         return f.ear.id;
            case "eye":         return f.eye.id;
            case "eyebrow":     return f.eyebrow.id;
            case "eyeLine":     return f.eyeLine.id;
            case "facialHair":  return f.facialHair.id;
            case "glasses":     return f.glasses.id;
            case "hair":        return f.hair.id;
            case "hairBg":      return f.hairBg.id;
            case "head":        return f.head.id;
            case "jersey":      return f.jersey.id;
            case "miscLine":    return f.miscLine.id;
            case "mouth":       return f.mouth.id;
            case "nose":        return f.nose.id;
            case "smileLine":   return f.smileLine.id;
            default:            return null;
        }
    }

    private static int getAngle(FaceConfig f, String name) {
        switch (name) {
            case "eye":     return f.eye.angle;
            case "eyebrow": return f.eyebrow.angle;
            default:        return 0;
        }
    }

    private static double getSize(FaceConfig f, String name) {
        switch (name) {
            case "ear":       return f.ear.size;
            case "nose":      return f.nose.size;
            case "smileLine": return f.smileLine.size;
            default:          return 1.0;
        }
    }

    private static boolean getFlip(FaceConfig f, String name) {
        switch (name) {
            case "hair":  return f.hair.flip;
            case "mouth": return f.mouth.flip;
            case "nose":  return f.nose.flip;
            default:      return false;
        }
    }

    // -------------------------------------------------------------------------
    // SVG arc → cubic bezier conversion
    // -------------------------------------------------------------------------

    /**
     * Converts an SVG arc segment to one or more cubic bezier curves and
     * appends the flattened vertices to {@code verts}.
     *
     * <p>Implements the endpoint-to-center-parameterization algorithm from the
     * W3C SVG specification (appendix F.6).
     */
    private static void arcToBezier(List<float[]> verts, float[] matrix,
                                     float sx, float sy,
                                     float x1, float y1,
                                     float rx, float ry,
                                     float xRotDeg, int largeArc, int sweep,
                                     float x2, float y2) {
        double phi    = Math.toRadians(xRotDeg);
        double cosPhi = Math.cos(phi), sinPhi = Math.sin(phi);

        // Step 1 – compute (x1', y1')
        double dx  = (x1 - x2) / 2.0;
        double dy  = (y1 - y2) / 2.0;
        double x1p =  cosPhi * dx + sinPhi * dy;
        double y1p = -sinPhi * dx + cosPhi * dy;

        // Ensure radii are large enough
        double rxSq  = (double) rx * rx, rySq = (double) ry * ry;
        double x1pSq = x1p * x1p,        y1pSq = y1p * y1p;
        double lambda = x1pSq / rxSq + y1pSq / rySq;
        if (lambda > 1.0) {
            double sqL = Math.sqrt(lambda);
            rx    = (float)(rx * sqL);   ry    = (float)(ry * sqL);
            rxSq  = (double) rx * rx;    rySq  = (double) ry * ry;
        }

        // Step 2 – compute (cx', cy')
        double num = rxSq * rySq - rxSq * y1pSq - rySq * x1pSq;
        double den = rxSq * y1pSq + rySq * x1pSq;
        double sq  = (den < 1e-10) ? 0 : Math.sqrt(Math.max(0, num / den));
        if (largeArc == sweep) sq = -sq;
        double cxp =  sq * rx * y1p / ry;
        double cyp = -sq * ry * x1p / rx;

        // Step 3 – compute centre
        double midX = (x1 + x2) / 2.0, midY = (y1 + y2) / 2.0;
        double cxc  = cosPhi * cxp - sinPhi * cyp + midX;
        double cyc  = sinPhi * cxp + cosPhi * cyp + midY;

        // Step 4 – compute angles
        double ux = (x1p - cxp) / rx, uy = (y1p - cyp) / ry;
        double vx = -(x1p + cxp) / rx, vy = -(y1p + cyp) / ry;
        double theta1 = svgAngle(1, 0, ux, uy);
        double dtheta = svgAngle(ux, uy, vx, vy);
        if (sweep == 0 && dtheta > 0) dtheta -= 2 * Math.PI;
        if (sweep == 1 && dtheta < 0) dtheta += 2 * Math.PI;

        // Split into segments of at most π/2 and approximate each with a cubic
        int    nSegs = Math.max(1, (int) Math.ceil(Math.abs(dtheta) / (Math.PI / 2)));
        double dSeg  = dtheta / nSegs;
        double halfD = dSeg / 2.0;
        double tHalf = Math.tan(halfD);
        double alpha = (Math.abs(dSeg) > 1e-10)
                ? Math.sin(dSeg) * (Math.sqrt(4 + 3 * tHalf * tHalf) - 1) / 3
                : 0;

        double cosT = Math.cos(theta1), sinT = Math.sin(theta1);
        double pdx   = -rx * cosPhi * sinT - ry * sinPhi * cosT;
        double pdy   = -rx * sinPhi * sinT + ry * cosPhi * cosT;
        double px = x1, py = y1;

        for (int seg = 0; seg < nSegs; seg++) {
            double theta2 = theta1 + dSeg;
            double cosT2  = Math.cos(theta2), sinT2 = Math.sin(theta2);
            double qdx    = -rx * cosPhi * sinT2 - ry * sinPhi * cosT2;
            double qdy    = -rx * sinPhi * sinT2 + ry * cosPhi * cosT2;
            double qx     = cxc + rx * cosPhi * cosT2 - ry * sinPhi * sinT2;
            double qy     = cyc + rx * sinPhi * cosT2 + ry * cosPhi * sinT2;

            flattenCubic(verts, matrix, sx, sy,
                    (float) px,  (float) py,
                    (float)(px + alpha * pdx), (float)(py + alpha * pdy),
                    (float)(qx - alpha * qdx), (float)(qy - alpha * qdy),
                    (float) qx,  (float) qy, 0);

            theta1 = theta2;
            cosT = cosT2; sinT = sinT2;
            pdx  = qdx;   pdy  = qdy;
            px   = qx;    py   = qy;
        }
    }

    /** Signed angle (radians) between vectors (ux,uy) and (vx,vy). */
    private static double svgAngle(double ux, double uy, double vx, double vy) {
        double dot = ux * vx + uy * vy;
        double len = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
        if (len < 1e-10) return 0;
        double a = Math.acos(Math.max(-1.0, Math.min(1.0, dot / len)));
        return (ux * vy - uy * vx < 0) ? -a : a;
    }
}
