package eb.framework1.ui;

/**
 * Immutable data object representing one item from a file-part entry in
 * {@code text/svg_resource.json}.
 *
 * <p>The JSON is organised as a list of <em>file parts</em>, each describing one
 * SVG file ({@link #file}, {@link #path}).  Each file part contains an
 * {@code items} sub-array; this class represents one such item.
 *
 * <p>Mandatory fields per item: {@link #id} (unique lookup key),
 * {@link #pathname} (descriptive name/path string), and {@link #type}
 * (classification string whose values will be defined as the feature evolves).
 * {@link #file} and {@link #path} are inherited from the parent file-part
 * during loading.
 *
 * <p>The optional {@link #x}, {@link #y}, {@link #w}, {@link #h} fields provide
 * placement and size hints.  They default to {@code 0} when not present in JSON,
 * which callers should treat as "not specified".
 *
 * <p>Example JSON structure:
 * <pre>
 * {
 *   "file": "eye.svg",
 *   "path": "svg/icons/",
 *   "items": [
 *     { "id": "A001", "pathname": "eye", "type": "icon", "w": 64, "h": 64 }
 *   ]
 * }
 * </pre>
 */
public final class SvgResourceData {

    /** Unique lookup key for this item (e.g. {@code "A001"}). */
    public final String id;

    /** Descriptive name/path string for this item (e.g. {@code "eye"}). */
    public final String pathname;

    /** SVG filename including extension (e.g. {@code "eye.svg"}), from the parent file-part. */
    public final String file;

    /** Asset directory path, ending with {@code "/"} (e.g. {@code "svg/icons/"}), from the parent file-part. */
    public final String path;

    /**
     * Classification string whose values will be defined as the feature evolves.
     * Empty string when not specified in JSON.
     */
    public final String type;

    /** Optional x position hint; {@code 0} when not specified. */
    public final int x;

    /** Optional y position hint; {@code 0} when not specified. */
    public final int y;

    /** Optional width hint in pixels; {@code 0} when not specified. */
    public final int w;

    /** Optional height hint in pixels; {@code 0} when not specified. */
    public final int h;

    /**
     * Creates a fully populated {@code SvgResourceData}.
     *
     * <p>Null values for string fields are silently converted to empty strings,
     * consistent with the rest of the data layer.
     *
     * @param id       the unique lookup key; {@code null} is treated as {@code ""}
     * @param pathname the descriptive name/path string; {@code null} is treated as {@code ""}
     * @param file     the SVG filename; {@code null} is treated as {@code ""}
     * @param path     the asset directory path; {@code null} is treated as {@code ""}
     * @param type     the classification string; {@code null} is treated as {@code ""}
     * @param x        optional x position hint (use {@code 0} when not specified)
     * @param y        optional y position hint (use {@code 0} when not specified)
     * @param w        optional width hint (use {@code 0} when not specified)
     * @param h        optional height hint (use {@code 0} when not specified)
     */
    public SvgResourceData(String id, String pathname, String file, String path, String type,
                           int x, int y, int w, int h) {
        this.id       = id       != null ? id       : "";
        this.pathname = pathname != null ? pathname : "";
        this.file     = file     != null ? file     : "";
        this.path     = path     != null ? path     : "";
        this.type     = type     != null ? type     : "";
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    /**
     * Returns the full asset path for this resource by concatenating {@link #path}
     * and {@link #file}.
     *
     * @return the full path relative to the assets root (e.g. {@code "svg/icons/eye.svg"})
     */
    public String getFullPath() {
        return path + file;
    }

    /**
     * Returns {@code true} when {@link #w} and {@link #h} are both greater than zero,
     * indicating that explicit size hints were provided.
     */
    public boolean hasSizeHint() {
        return w > 0 && h > 0;
    }

    /**
     * Returns {@code true} when {@link #x} or {@link #y} is non-zero, indicating that
     * an explicit position hint was provided.
     */
    public boolean hasPositionHint() {
        return x != 0 || y != 0;
    }

    @Override
    public String toString() {
        return "SvgResourceData{id='" + id + "', pathname='" + pathname + "', file='" + file
                + "', path='" + path + "', type='" + type + "', x=" + x + ", y=" + y
                + ", w=" + w + ", h=" + h + '}';
    }
}
