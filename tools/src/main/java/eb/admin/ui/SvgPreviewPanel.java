package eb.admin.ui;

import javax.swing.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/**
 * A Swing panel that renders an SVG fragment from {@code svgs.json} using Java2D.
 *
 * <p>The nominal SVG coordinate space assumed by facesjs is 400&times;600.
 * The full canvas is always drawn as a light-blue rectangle so that the caller
 * can see how the fragment is positioned within the complete canvas – which is
 * especially important for {@code head} and {@code hair} features where position
 * determines where other facial features are placed.
 *
 * <p>Template variables such as {@code $[skinColor]} are replaced with
 * sensible default preview colours before rendering.
 */
public class SvgPreviewPanel extends JPanel {

    /** Nominal SVG canvas dimensions used by facesjs (400&times;600). */
    static final int SVG_W = 400;
    static final int SVG_H = 600;

    /** Canvas fill – pale blue so it is distinct from the panel background. */
    private static final Color CANVAS_FILL   = new Color(240, 246, 255);
    /** Canvas border – muted steel blue. */
    private static final Color CANVAS_BORDER = new Color(170, 195, 225);

    /** Default hex colours for each {@code $[varName]} template token. */
    private static final Map<String, String> TEMPLATE_DEFAULTS = new LinkedHashMap<>();
    static {
        TEMPLATE_DEFAULTS.put("skinColor", "#FFCBA4");
        TEMPLATE_DEFAULTS.put("secondary", "#5588FF");
        TEMPLATE_DEFAULTS.put("primary",   "#3366CC");
        TEMPLATE_DEFAULTS.put("hairColor", "#3B1F0E");
        TEMPLATE_DEFAULTS.put("accent",    "#FF6600");
        TEMPLATE_DEFAULTS.put("faceShave", "#B08060");
        TEMPLATE_DEFAULTS.put("headShave", "#A07050");
    }

    /**
     * Features whose SVG paths are designed to fill the full 400×600 canvas and
     * must <em>not</em> be centred – they rely on absolute canvas positioning.
     */
    private static final Set<String> NO_CENTER_FEATURES = new HashSet<>(
            Arrays.asList("head", "hair", "hairBg", "body", "jersey", "accessories"));

    private String fragment    = "";
    private String featureName = "";

    /** Ordered fragments for composite face rendering; {@code null} = single-fragment mode. */
    private java.util.List<String> compositeFragments = null;

    /**
     * CSS class name → style-string map, rebuilt each paint from any {@code <style>}
     * elements found in the fragment.  Used by {@link #applyPresentationAttrs}.
     */
    private Map<String, String> currentCssStyles = Collections.emptyMap();

    /** Pattern matching a single CSS class rule: {@code .name { ... }}. */
    private static final Pattern CSS_RULE = Pattern.compile(
            "\\.([\\w-]+)\\s*\\{([^}]*)\\}", Pattern.DOTALL);

    public SvgPreviewPanel() {
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
        setPreferredSize(new Dimension(200, 200));
        setMinimumSize(new Dimension(80, 80));
    }

    /** Sets the SVG fragment to render and triggers a repaint. */
    public void setSvgFragment(String f) {
        this.fragment = (f != null) ? f : "";
        repaint();
    }

    /**
     * Sets the feature name for the current fragment.
     * Features that are not {@code head}, {@code hair}, {@code hairBg},
     * {@code body}, {@code jersey}, or {@code accessories} are automatically
     * translated to the centre of the 400×600 canvas so they are visible in
     * the preview.
     *
     * @param name feature name (e.g. {@code "eye"}, {@code "glasses"}), or empty
     */
    public void setFeatureName(String name) {
        this.featureName = (name != null) ? name : "";
        repaint();
    }

    /**
     * Sets an ordered list of SVG fragments for composite face rendering.
     * Fragments are drawn back-to-front on the shared 400×600 canvas without
     * automatic centering.  Pass {@code null} to revert to single-fragment mode.
     */
    public void setCompositeFragments(java.util.List<String> fragments) {
        this.compositeFragments = (fragments != null)
                ? new ArrayList<>(fragments) : null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_NORMALIZE);

            // Scale to fit the panel content area (4-px inset on each side)
            int pw = Math.max(1, getWidth()  - 8);
            int ph = Math.max(1, getHeight() - 8);
            double scale = Math.min((double) pw / SVG_W, (double) ph / SVG_H);
            int ox = 4 + (int) ((pw - SVG_W * scale) / 2.0);
            int oy = 4 + (int) ((ph - SVG_H * scale) / 2.0);
            g2.translate(ox, oy);
            g2.scale(scale, scale);

            // Draw the 400×600 canvas so positional context is visible
            g2.setColor(CANVAS_FILL);
            g2.fillRect(0, 0, SVG_W, SVG_H);
            g2.setColor(CANVAS_BORDER);
            float borderPx = Math.max(0.5f, (float) (1.0 / scale));
            g2.setStroke(new BasicStroke(borderPx));
            g2.drawRect(0, 0, SVG_W, SVG_H);

            if (compositeFragments != null) {
                // Composite mode: render all fragments in order without centering
                for (String frag : compositeFragments) {
                    if (frag == null || frag.isEmpty()) continue;
                    String processed = applyTemplateVars(frag);
                    Document doc = parseXml(
                            "<svg xmlns=\"http://www.w3.org/2000/svg\">" + processed + "</svg>");
                    if (doc != null) {
                        currentCssStyles = parseCssClassStyles(doc);
                        renderElement(g2, doc.getDocumentElement(), new SvgState());
                    }
                }
            } else if (!fragment.isEmpty()) {
                String processed = applyTemplateVars(fragment);
                Document doc = parseXml(
                        "<svg xmlns=\"http://www.w3.org/2000/svg\">" + processed + "</svg>");
                if (doc != null) {
                    currentCssStyles = parseCssClassStyles(doc);
                    Graphics2D g2f = (Graphics2D) g2.create();
                    try {
                        // Centre features that are not canvas-wide (head/hair/hairBg/body/jersey/accessories).
                        // Also skip centering if the fragment already contains canvas-space coordinates
                        // (coordinates > 150), which indicates the paths are pre-positioned on the face canvas.
                        boolean needsCentre = !featureName.isEmpty()
                                && !NO_CENTER_FEATURES.contains(featureName)
                                && !hasCanvasCoordinates(processed);
                        if (needsCentre) {
                            g2f.translate(SVG_W / 2.0, SVG_H / 2.0);
                        }
                        renderElement(g2f, doc.getDocumentElement(), new SvgState());
                    } finally {
                        g2f.dispose();
                    }
                }
            }
        } finally {
            g2.dispose();
        }
    }

    // ── Template variable substitution ────────────────────────────────────────

    private static String applyTemplateVars(String s) {
        for (Map.Entry<String, String> e : TEMPLATE_DEFAULTS.entrySet()) {
            s = s.replace("$[" + e.getKey() + "]", e.getValue());
        }
        return s;
    }

    // ── CSS class style parsing ───────────────────────────────────────────────

    /**
     * Builds a map of CSS class name → style-string from all {@code <style>} elements
     * found in the document.  Each style-string is in the same "property: value; ..."
     * format that {@link #applyStyleString} already understands.
     */
    private static Map<String, String> parseCssClassStyles(Document doc) {
        NodeList styleNodes = doc.getElementsByTagName("style");
        if (styleNodes.getLength() == 0) return Collections.emptyMap();
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < styleNodes.getLength(); i++) {
            String css = styleNodes.item(i).getTextContent();
            if (css == null || css.isEmpty()) continue;
            Matcher m = CSS_RULE.matcher(css);
            while (m.find()) {
                result.put(m.group(1), m.group(2).trim());
            }
        }
        return result;
    }

    /**
     * Returns {@code true} if the SVG fragment string contains path coordinates
     * clearly in canvas-space (absolute values &gt; 150), which means the feature
     * is already positioned on the 400×600 face canvas and must not be
     * re-centred by the preview panel.
     */
    /** Package-private so {@link SvgEditorPanel} can reuse the same heuristic. */
    static boolean hasCanvasCoordinates(String fragment) {
        // Scan path d= attribute values for large coordinate magnitudes.
        Matcher pathMatcher = Pattern.compile("\\bd=\"([^\"]+)\"").matcher(fragment);
        Pattern num = Pattern.compile("[-]?(\\d+)(?:\\.\\d+)?");
        while (pathMatcher.find()) {
            Matcher nm = num.matcher(pathMatcher.group(1));
            while (nm.find()) {
                if (Integer.parseInt(nm.group(1)) > 150) return true;
            }
        }
        return false;
    }

    /**
     * Computes the bounding box of all shapes in an SVG fragment by parsing
     * every {@code <path>}, {@code <circle>}, {@code <ellipse>}, and {@code <rect>}
     * element at their <em>natural</em> (pre-transform) coordinates.
     *
     * <p>This mirrors what a browser's {@code getBBox()} returns: the element's
     * bounding box in its own local coordinate system, <em>without</em> any
     * {@code transform} attribute applied.  Use the returned center to compute
     * the correct facesjs-style {@code translate(px-cx, py-cy)} offset.
     *
     * @param fragment raw SVG fragment string (no enclosing {@code <svg>} element needed)
     * @return unified bounding box, or {@code null} if no shapes could be parsed
     */
    static Rectangle2D computeFragmentBounds(String fragment) {
        if (fragment == null || fragment.isEmpty()) return null;
        Document doc = parseXml(
                "<svg xmlns=\"http://www.w3.org/2000/svg\">" + fragment + "</svg>");
        if (doc == null) return null;
        Rectangle2D bounds = collectBounds(doc.getDocumentElement());
        return (bounds == null || bounds.isEmpty()) ? null : bounds;
    }

    /** Recursively collects the union of all shape bounds within an element tree. */
    private static Rectangle2D collectBounds(Element el) {
        String tag = el.getLocalName();
        if (tag == null) tag = el.getNodeName();

        Rectangle2D result = null;

        switch (tag.toLowerCase()) {
            case "path": {
                String d = el.getAttribute("d");
                if (!d.isEmpty()) {
                    Shape s = parseSvgPath(d);
                    if (s != null) result = union(result, s.getBounds2D());
                }
                break;
            }
            case "circle": {
                double cx = attrD(el, "cx", 0), cy = attrD(el, "cy", 0),
                        r = attrD(el, "r", 0);
                if (r > 0) result = union(result,
                        new Rectangle2D.Double(cx - r, cy - r, r * 2, r * 2));
                break;
            }
            case "ellipse": {
                double cx = attrD(el, "cx", 0), cy = attrD(el, "cy", 0),
                        rx = attrD(el, "rx", 0), ry = attrD(el, "ry", 0);
                if (rx > 0 && ry > 0) result = union(result,
                        new Rectangle2D.Double(cx - rx, cy - ry, rx * 2, ry * 2));
                break;
            }
            case "rect": {
                double x = attrD(el, "x", 0), y = attrD(el, "y", 0),
                        w = attrD(el, "width", 0), h = attrD(el, "height", 0);
                if (w > 0 && h > 0) result = union(result,
                        new Rectangle2D.Double(x, y, w, h));
                break;
            }
            default:
                break;
        }

        // Recurse into children (handles <g> and unknown wrappers)
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n instanceof Element) {
                Rectangle2D childBounds = collectBounds((Element) n);
                result = union(result, childBounds);
            }
        }
        return result;
    }

    private static Rectangle2D union(Rectangle2D a, Rectangle2D b) {
        if (b == null) return a;
        if (a == null) return b;
        Rectangle2D r = new Rectangle2D.Double();
        Rectangle2D.union(a, b, r);
        return r;
    }

    // ── XML parsing ───────────────────────────────────────────────────────────

    private static Document parseXml(String xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // Harden against XXE
            dbf.setFeature("http://xml.org/sax/features/external-general-entities",  false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbf.setExpandEntityReferences(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setErrorHandler(null); // suppress SAX errors to stdout
            return db.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }

    // ── SVG rendering state ───────────────────────────────────────────────────

    /** Inheritable presentation state (fill, stroke) propagated down the tree. */
    private static final class SvgState {
        Color   fill        = Color.BLACK;
        Color   stroke      = null;
        float   strokeWidth = 1f;
        boolean fillNone    = false;
        boolean strokeNone  = true;

        SvgState copy() {
            SvgState s   = new SvgState();
            s.fill        = fill;
            s.stroke      = stroke;
            s.strokeWidth = strokeWidth;
            s.fillNone    = fillNone;
            s.strokeNone  = strokeNone;
            return s;
        }
    }

    // ── Element dispatcher ────────────────────────────────────────────────────

    private void renderElement(Graphics2D g2, Element el, SvgState parentState) {
        String tag = el.getLocalName();
        if (tag == null) tag = el.getNodeName();

        // Skip non-visual meta elements
        if ("defs".equalsIgnoreCase(tag) || "linearGradient".equalsIgnoreCase(tag)
                || "stop".equalsIgnoreCase(tag)) return;

        SvgState st = parentState.copy();
        applyPresentationAttrs(el, st);

        // Apply any SVG transform attribute by wrapping rendering in a transformed g2 clone.
        String transformAttr = el.getAttribute("transform");
        if (!transformAttr.isEmpty()) {
            AffineTransform tx = parseTransform(transformAttr);
            if (tx != null && !tx.isIdentity()) {
                Graphics2D g2t = (Graphics2D) g2.create();
                try {
                    g2t.transform(tx);
                    renderElementInternal(g2t, tag, el, st);
                } finally {
                    g2t.dispose();
                }
                return;
            }
        }
        renderElementInternal(g2, tag, el, st);
    }

    private void renderElementInternal(Graphics2D g2, String tag, Element el, SvgState st) {
        switch (tag.toLowerCase()) {
            case "svg":
            case "g":
                renderChildren(g2, el, st);
                break;
            case "path": {
                String d = el.getAttribute("d");
                if (!d.isEmpty()) {
                    Shape shape = parseSvgPath(d);
                    if (shape != null) drawShape(g2, shape, st);
                }
                break;
            }
            case "circle":
                renderCircle(g2, el, st);
                break;
            case "ellipse":
                renderEllipse(g2, el, st);
                break;
            case "rect":
                renderRect(g2, el, st);
                break;
            case "polygon":
                renderPolygon(g2, el, st);
                break;
            default:
                break;
        }
    }

    /**
     * Parses an SVG {@code transform} attribute string and returns the
     * equivalent {@link AffineTransform}.
     *
     * <p>Supports {@code translate}, {@code scale}, {@code rotate}, and
     * {@code matrix} functions, including compound transforms such as
     * {@code "translate(10 20) scale(-1 1) translate(-30 0)"}.
     * Transforms are applied in left-to-right order (SVG convention).
     */
    static AffineTransform parseTransform(String transformStr) {
        if (transformStr == null || transformStr.trim().isEmpty()) return null;
        AffineTransform result = new AffineTransform();
        Pattern funcPat = Pattern.compile(
                "(translate|scale|rotate|matrix|skewX|skewY)\\s*\\(([^)]+)\\)",
                Pattern.CASE_INSENSITIVE);
        Matcher m = funcPat.matcher(transformStr);
        while (m.find()) {
            String func = m.group(1).toLowerCase();
            double[] v = parseTxArgs(m.group(2));
            AffineTransform ti = new AffineTransform();
            switch (func) {
                case "translate":
                    ti.translate(v.length > 0 ? v[0] : 0, v.length > 1 ? v[1] : 0);
                    break;
                case "scale":
                    ti.scale(v.length > 0 ? v[0] : 1,
                             v.length > 1 ? v[1] : (v.length > 0 ? v[0] : 1));
                    break;
                case "rotate":
                    double rad = v.length > 0 ? Math.toRadians(v[0]) : 0;
                    if (v.length >= 3) {
                        ti.rotate(rad, v[1], v[2]);
                    } else {
                        ti.rotate(rad);
                    }
                    break;
                case "matrix":
                    if (v.length == 6) {
                        ti = new AffineTransform(v[0], v[1], v[2], v[3], v[4], v[5]);
                    }
                    break;
                default:
                    break;
            }
            result.concatenate(ti);
        }
        return result;
    }

    private static double[] parseTxArgs(String args) {
        String[] parts = args.trim().split("[,\\s]+");
        double[] vals = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try { vals[i] = Double.parseDouble(parts[i]); } catch (NumberFormatException e) { vals[i] = 0; }
        }
        return vals;
    }

    private void renderChildren(Graphics2D g2, Element el, SvgState st) {
        NodeList nl = el.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n instanceof Element) renderElement(g2, (Element) n, st);
        }
    }

    // ── Shape drawing ─────────────────────────────────────────────────────────

    private static void drawShape(Graphics2D g2, Shape shape, SvgState st) {
        if (!st.fillNone) {
            g2.setColor(st.fill != null ? st.fill : Color.BLACK);
            g2.fill(shape);
        }
        if (!st.strokeNone && st.stroke != null) {
            g2.setColor(st.stroke);
            g2.setStroke(new BasicStroke(st.strokeWidth,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(shape);
        }
    }

    private static void renderCircle(Graphics2D g2, Element el, SvgState st) {
        double cx = attrD(el, "cx", 0);
        double cy = attrD(el, "cy", 0);
        double r  = attrD(el, "r",  0);
        if (r <= 0) return;
        drawShape(g2, new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2), st);
    }

    private static void renderEllipse(Graphics2D g2, Element el, SvgState st) {
        double cx = attrD(el, "cx", 0);
        double cy = attrD(el, "cy", 0);
        double rx = attrD(el, "rx", 0);
        double ry = attrD(el, "ry", 0);
        if (rx <= 0 || ry <= 0) return;
        drawShape(g2, new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2), st);
    }

    private static void renderRect(Graphics2D g2, Element el, SvgState st) {
        double x  = attrD(el, "x",      0);
        double y  = attrD(el, "y",      0);
        double w  = attrD(el, "width",  0);
        double h  = attrD(el, "height", 0);
        if (w <= 0 || h <= 0) return;
        drawShape(g2, new Rectangle2D.Double(x, y, w, h), st);
    }

    private static void renderPolygon(Graphics2D g2, Element el, SvgState st) {
        String pts = el.getAttribute("points").trim();
        if (pts.isEmpty()) return;
        String[] coords = pts.split("[,\\s]+");
        if (coords.length < 4) return;
        GeneralPath poly = new GeneralPath(GeneralPath.WIND_NON_ZERO);
        for (int i = 0; i + 1 < coords.length; i += 2) {
            double x = parseD(coords[i], 0);
            double y = parseD(coords[i + 1], 0);
            if (i == 0) poly.moveTo(x, y);
            else        poly.lineTo(x, y);
        }
        poly.closePath();
        drawShape(g2, poly, st);
    }

    // ── Attribute parsing ─────────────────────────────────────────────────────

    /**
     * Applies all presentation attributes from {@code el} to {@code st}, including
     * CSS class-based styles resolved via {@link #currentCssStyles}.
     *
     * <p>Priority (lowest → highest): CSS class styles → element attributes → inline style.
     */
    private void applyPresentationAttrs(Element el, SvgState st) {
        // 1. CSS class-based styles (lowest priority)
        String cls = el.getAttribute("class");
        if (!cls.isEmpty() && !currentCssStyles.isEmpty()) {
            for (String c : cls.trim().split("\\s+")) {
                String classStyle = currentCssStyles.get(c);
                if (classStyle != null) applyStyleString(classStyle, st);
            }
        }
        // 2. Presentation attributes override class styles
        applyFill(el.getAttribute("fill"), st);
        applyStroke(el.getAttribute("stroke"), el.getAttribute("stroke-width"), st);
        // 3. Inline style declarations override element attributes
        applyStyleString(el.getAttribute("style"), st);
        // 4. Opacity modifies alpha of both colours
        String op = el.getAttribute("opacity");
        if (!op.isEmpty()) {
            float alpha = (float) parseD(op, 1.0);
            if (!st.fillNone  && st.fill   != null) st.fill   = withAlpha(st.fill,   alpha);
            if (!st.strokeNone && st.stroke != null) st.stroke = withAlpha(st.stroke, alpha);
        }
    }

    private static void applyFill(String fill, SvgState st) {
        if (fill.isEmpty()) return;
        if ("none".equalsIgnoreCase(fill)) {
            st.fillNone = true;
        } else {
            Color c = parseColor(fill);
            if (c != null) { st.fill = c; st.fillNone = false; }
        }
    }

    private static void applyStroke(String stroke, String sw, SvgState st) {
        if (!stroke.isEmpty()) {
            if ("none".equalsIgnoreCase(stroke)) {
                st.strokeNone = true;
                st.stroke = null;
            } else {
                Color c = parseColor(stroke);
                if (c != null) { st.stroke = c; st.strokeNone = false; }
            }
        }
        if (!sw.isEmpty()) {
            st.strokeWidth = (float) parseD(sw, 1.0);
        }
    }

    private static void applyStyleString(String style, SvgState st) {
        if (style.isEmpty()) return;
        // Collect opacity modifiers separately; they must be applied AFTER fill/stroke
        // so they operate on the correct colour (e.g. "opacity:0.05; fill:#501414").
        Float opacity = null;
        Float fillOpacity = null;
        Float strokeOpacity = null;
        for (String decl : style.split(";")) {
            int colon = decl.indexOf(':');
            if (colon < 0) continue;
            String prop  = decl.substring(0, colon).trim().toLowerCase();
            String value = decl.substring(colon + 1).trim();
            switch (prop) {
                case "fill":           applyFill(value, st);           break;
                case "stroke":         applyStroke(value, "", st);     break;
                case "stroke-width":   applyStroke("", value, st);     break;
                case "opacity":        opacity       = (float) parseD(value, 1.0); break;
                case "fill-opacity":   fillOpacity   = (float) parseD(value, 1.0); break;
                case "stroke-opacity": strokeOpacity = (float) parseD(value, 1.0); break;
                default: break;
            }
        }
        // Apply opacity modifiers after fill/stroke colours are resolved
        if (opacity != null) {
            if (!st.fillNone   && st.fill   != null) st.fill   = withAlpha(st.fill,   opacity);
            if (!st.strokeNone && st.stroke != null) st.stroke = withAlpha(st.stroke, opacity);
        }
        if (fillOpacity   != null && !st.fillNone   && st.fill   != null) st.fill   = withAlpha(st.fill,   fillOpacity);
        if (strokeOpacity != null && !st.strokeNone && st.stroke != null) st.stroke = withAlpha(st.stroke, strokeOpacity);
    }

    // ── Colour parsing ────────────────────────────────────────────────────────

    private static Color parseColor(String s) {
        if (s == null || s.isEmpty() || "none".equalsIgnoreCase(s)
                || "transparent".equalsIgnoreCase(s)) return null;
        try {
            if (s.startsWith("#")) {
                String h = s.substring(1);
                if (h.length() == 3) {
                    int r = Integer.parseInt("" + h.charAt(0) + h.charAt(0), 16);
                    int g = Integer.parseInt("" + h.charAt(1) + h.charAt(1), 16);
                    int b = Integer.parseInt("" + h.charAt(2) + h.charAt(2), 16);
                    return new Color(r, g, b);
                }
                if (h.length() == 6) return new Color(Integer.parseInt(h, 16));
            }
            if (s.startsWith("rgba(") && s.endsWith(")")) {
                String[] p = s.substring(5, s.length() - 1).split(",");
                if (p.length == 4) {
                    return new Color(
                            clamp(parseD(p[0].trim(), 0)),
                            clamp(parseD(p[1].trim(), 0)),
                            clamp(parseD(p[2].trim(), 0)),
                            Math.min(255, Math.max(0, (int)(parseD(p[3].trim(), 1.0) * 255))));
                }
            }
            if (s.startsWith("rgb(") && s.endsWith(")")) {
                String[] p = s.substring(4, s.length() - 1).split(",");
                if (p.length == 3) {
                    return new Color(clamp(parseD(p[0].trim(), 0)),
                            clamp(parseD(p[1].trim(), 0)),
                            clamp(parseD(p[2].trim(), 0)));
                }
            }
            switch (s.toLowerCase()) {
                case "black":   return Color.BLACK;
                case "white":   return Color.WHITE;
                case "red":     return Color.RED;
                case "green":   return Color.GREEN;
                case "blue":    return Color.BLUE;
                case "gray":
                case "grey":    return Color.GRAY;
                case "yellow":  return Color.YELLOW;
                case "orange":  return Color.ORANGE;
                case "brown":   return new Color(0x8B, 0x45, 0x13);
                case "pink":    return Color.PINK;
                default:        return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static int clamp(double v) {
        return Math.min(255, Math.max(0, (int) v));
    }

    private static Color withAlpha(Color c, float alpha) {
        int a = Math.min(255, Math.max(0, (int)(alpha * c.getAlpha())));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    // ── Attribute helpers ─────────────────────────────────────────────────────

    private static double attrD(Element el, String name, double def) {
        String v = el.getAttribute(name);
        return v.isEmpty() ? def : parseD(v, def);
    }

    private static double parseD(String s, double def) {
        if (s == null || s.isEmpty()) return def;
        try { return Double.parseDouble(s.trim()); }
        catch (NumberFormatException e) { return def; }
    }

    // ── SVG path parser ───────────────────────────────────────────────────────

    /**
     * Parses an SVG path {@code d} attribute into a Java2D {@link Path2D.Double}.
     * Supports M/m, L/l, H/h, V/v, C/c, S/s, Q/q, A/a, Z/z.
     */
    static Path2D parseSvgPath(String d) {
        if (d == null || d.isEmpty()) return null;
        Path2D   path    = new Path2D.Double();
        PathTok  tok     = new PathTok(d);
        double   cx = 0, cy = 0;    // current point
        double   lx = 0, ly = 0;    // last control point (for S/s)
        char     lastCmd = 0;

        while (tok.hasNext()) {
            char cmd = tok.nextCmd();
            if (cmd == 0) break;
            boolean rel = Character.isLowerCase(cmd);

            switch (Character.toUpperCase(cmd)) {
                case 'M': {
                    boolean first = true;
                    while (tok.hasNum()) {
                        double x = tok.num(); double y = tok.num();
                        if (rel) { x += cx; y += cy; }
                        cx = x; cy = y;
                        if (first) { path.moveTo(cx, cy); first = false; }
                        else         path.lineTo(cx, cy);
                    }
                    break;
                }
                case 'L': {
                    while (tok.hasNum()) {
                        double x = tok.num(); double y = tok.num();
                        if (rel) { x += cx; y += cy; }
                        cx = x; cy = y;
                        path.lineTo(cx, cy);
                    }
                    break;
                }
                case 'H': {
                    while (tok.hasNum()) {
                        double x = tok.num();
                        if (rel) x += cx;
                        cx = x;
                        path.lineTo(cx, cy);
                    }
                    break;
                }
                case 'V': {
                    while (tok.hasNum()) {
                        double y = tok.num();
                        if (rel) y += cy;
                        cy = y;
                        path.lineTo(cx, cy);
                    }
                    break;
                }
                case 'C': {
                    while (tok.hasNum()) {
                        double x1 = tok.num(), y1 = tok.num();
                        double x2 = tok.num(), y2 = tok.num();
                        double x  = tok.num(), y  = tok.num();
                        if (rel) { x1+=cx; y1+=cy; x2+=cx; y2+=cy; x+=cx; y+=cy; }
                        path.curveTo(x1, y1, x2, y2, x, y);
                        lx = x2; ly = y2; cx = x; cy = y;
                    }
                    break;
                }
                case 'S': {
                    while (tok.hasNum()) {
                        char up = Character.toUpperCase(lastCmd);
                        double cx2 = (up == 'C' || up == 'S') ? 2*cx - lx : cx;
                        double cy2 = (up == 'C' || up == 'S') ? 2*cy - ly : cy;
                        double x2 = tok.num(), y2 = tok.num();
                        double x  = tok.num(), y  = tok.num();
                        if (rel) { x2+=cx; y2+=cy; x+=cx; y+=cy; }
                        path.curveTo(cx2, cy2, x2, y2, x, y);
                        lx = x2; ly = y2; cx = x; cy = y;
                    }
                    break;
                }
                case 'Q': {
                    while (tok.hasNum()) {
                        double x1 = tok.num(), y1 = tok.num();
                        double x  = tok.num(), y  = tok.num();
                        if (rel) { x1+=cx; y1+=cy; x+=cx; y+=cy; }
                        path.quadTo(x1, y1, x, y);
                        lx = x1; ly = y1; cx = x; cy = y;
                    }
                    break;
                }
                case 'A': {
                    while (tok.hasNum()) {
                        double rx   = Math.abs(tok.num());
                        double ry   = Math.abs(tok.num());
                        double xRot = tok.num();
                        double fA   = tok.num();
                        double fS   = tok.num();
                        double x    = tok.num();
                        double y    = tok.num();
                        if (rel) { x += cx; y += cy; }
                        arcToBezier(path, rx, ry, xRot, fA != 0, fS != 0, cx, cy, x, y);
                        cx = x; cy = y;
                    }
                    break;
                }
                case 'Z':
                    path.closePath();
                    break;
                default:
                    break;
            }
            lastCmd = cmd;
        }
        return path;
    }

    // ── SVG arc → cubic bezier (W3C SVG spec Appendix F.6) ───────────────────

    private static void arcToBezier(Path2D path,
                                     double rx, double ry, double xRotDeg,
                                     boolean largeArc, boolean sweep,
                                     double x1, double y1, double x2, double y2) {
        if (x1 == x2 && y1 == y2) return;
        if (rx == 0 || ry == 0) { path.lineTo(x2, y2); return; }

        double phi    = Math.toRadians(xRotDeg);
        double cosPhi = Math.cos(phi);
        double sinPhi = Math.sin(phi);

        double dx = (x1 - x2) / 2.0;
        double dy = (y1 - y2) / 2.0;
        double x1p =  cosPhi * dx + sinPhi * dy;
        double y1p = -sinPhi * dx + cosPhi * dy;

        double x1pSq = x1p * x1p;
        double y1pSq = y1p * y1p;
        double rxSq  = rx * rx;
        double rySq  = ry * ry;

        // Ensure radii are large enough
        double lambda = x1pSq / rxSq + y1pSq / rySq;
        if (lambda > 1) {
            double sqL = Math.sqrt(lambda);
            rx *= sqL; ry *= sqL;
            rxSq = rx * rx; rySq = ry * ry;
        }

        double num = Math.max(0.0, rxSq * rySq - rxSq * y1pSq - rySq * x1pSq);
        double den = rxSq * y1pSq + rySq * x1pSq;
        double sq  = (den == 0) ? 0 : Math.sqrt(num / den);
        if (largeArc == sweep) sq = -sq;

        double cxp   = sq * rx * y1p / ry;
        double cyp   = -sq * ry * x1p / rx;
        double midX  = (x1 + x2) / 2.0;
        double midY  = (y1 + y2) / 2.0;
        double centerX = cosPhi * cxp - sinPhi * cyp + midX;
        double centerY = sinPhi * cxp + cosPhi * cyp + midY;

        double ux = (x1p - cxp) / rx;
        double uy = (y1p - cyp) / ry;
        double vx = (-x1p - cxp) / rx;
        double vy = (-y1p - cyp) / ry;

        double theta1 = svgAngle(1, 0, ux, uy);
        double dTheta = svgAngle(ux, uy, vx, vy);
        if (!sweep && dTheta > 0) dTheta -= 2 * Math.PI;
        if ( sweep && dTheta < 0) dTheta += 2 * Math.PI;

        int nSegs = Math.max(1, (int) Math.ceil(Math.abs(dTheta) / (Math.PI / 2)));
        double dT     = dTheta / nSegs;
        double cosRx  = Math.cos(theta1);
        double sinRx  = Math.sin(theta1);

        for (int i = 0; i < nSegs; i++) {
            double alpha = 4.0 / 3.0 * Math.tan(dT / 4.0);
            double cosE  = Math.cos(theta1 + dT);
            double sinE  = Math.sin(theta1 + dT);

            double bx1 = centerX + cosPhi*(rx*(cosRx - alpha*sinRx)) - sinPhi*(ry*(sinRx + alpha*cosRx));
            double by1 = centerY + sinPhi*(rx*(cosRx - alpha*sinRx)) + cosPhi*(ry*(sinRx + alpha*cosRx));
            double bx2 = centerX + cosPhi*(rx*(cosE + alpha*sinE))   - sinPhi*(ry*(sinE - alpha*cosE));
            double by2 = centerY + sinPhi*(rx*(cosE + alpha*sinE))   + cosPhi*(ry*(sinE - alpha*cosE));
            double bx  = centerX + cosPhi*(rx*cosE) - sinPhi*(ry*sinE);
            double by  = centerY + sinPhi*(rx*cosE) + cosPhi*(ry*sinE);

            path.curveTo(bx1, by1, bx2, by2, bx, by);
            theta1 += dT;
            cosRx = cosE;
            sinRx = sinE;
        }
    }

    private static double svgAngle(double ux, double uy, double vx, double vy) {
        double dot = ux * vx + uy * vy;
        double len = Math.sqrt((ux*ux + uy*uy) * (vx*vx + vy*vy));
        if (len == 0) return 0;
        double ang = Math.acos(Math.min(1.0, Math.max(-1.0, dot / len)));
        return (ux * vy - uy * vx < 0) ? -ang : ang;
    }

    // ── Path tokenizer ────────────────────────────────────────────────────────

    /**
     * Tokenizes an SVG path {@code d} attribute into command letters and numbers.
     * Numbers may be separated by whitespace, commas, or sign characters.
     */
    private static final class PathTok {
        private final String s;
        private int pos;

        PathTok(String s) { this.s = s.trim(); this.pos = 0; }

        boolean hasNext() {
            skipWs();
            return pos < s.length();
        }

        /** Reads the next command letter; returns 0 if no letter is next. */
        char nextCmd() {
            skipWs();
            if (pos >= s.length()) return 0;
            char c = s.charAt(pos);
            if (Character.isLetter(c)) { pos++; return c; }
            return 0;
        }

        /** Returns true if the next non-separator token looks like a number. */
        boolean hasNum() {
            skipSep();
            if (pos >= s.length()) return false;
            char c = s.charAt(pos);
            return Character.isDigit(c) || c == '.' || c == '-' || c == '+';
        }

        /** Reads and returns the next floating-point number. */
        double num() {
            skipSep();
            int start = pos;
            if (pos < s.length() && (s.charAt(pos) == '-' || s.charAt(pos) == '+')) pos++;
            // Per SVG spec: a second '.' always starts a new number token, so we stop
            // as soon as we see a second decimal point (e.g. "1.6.3" → "1.6" then "0.3").
            boolean seenDot = false;
            while (pos < s.length()) {
                char ch = s.charAt(pos);
                if (Character.isDigit(ch)) {
                    pos++;
                } else if (ch == '.' && !seenDot) {
                    seenDot = true;
                    pos++;
                } else {
                    break;
                }
            }
            if (pos < s.length() && (s.charAt(pos) == 'e' || s.charAt(pos) == 'E')) {
                pos++;
                if (pos < s.length() && (s.charAt(pos) == '-' || s.charAt(pos) == '+')) pos++;
                while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
            }
            if (pos == start) { pos++; return 0; } // guard against infinite loop
            try { return Double.parseDouble(s.substring(start, pos)); }
            catch (NumberFormatException e) { return 0; }
        }

        private void skipWs() {
            while (pos < s.length() && isWs(s.charAt(pos))) pos++;
        }

        private void skipSep() {
            while (pos < s.length() && (isWs(s.charAt(pos)) || s.charAt(pos) == ',')) pos++;
        }

        private static boolean isWs(char c) {
            return c == ' ' || c == '\t' || c == '\n' || c == '\r';
        }
    }
}
