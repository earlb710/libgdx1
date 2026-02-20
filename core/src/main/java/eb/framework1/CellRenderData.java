package eb.framework1;

import com.badlogic.gdx.graphics.Color;
import java.util.Collections;
import java.util.List;

/**
 * Pre-computed rendering data for a single map cell.
 *
 * <p>Build an instance for every cell after map generation via
 * {@link RoadAccessMap#buildRenderData} and reuse it each frame.  The fill
 * rectangle covers the whole cell; border segments are present only for the
 * sides on which the cell has road access.
 *
 * <p>All coordinates are in world units, matching the convention used by
 * LibGDX's {@code ShapeRenderer#rect} / {@code SpriteBatch} (y increases
 * upward).
 */
public class CellRenderData {

    /** World-space left edge of the cell. */
    private final float x;
    /** World-space bottom edge of the cell. */
    private final float y;
    /** Width of the cell in world units. */
    private final float width;
    /** Height of the cell in world units. */
    private final float height;
    /** Fill color derived from the cell's terrain type. */
    private final Color fillColor;
    /** Color used for all active border segments. */
    private final Color borderColor;
    /**
     * Pre-computed border segments.  Each element is a four-element array
     * {@code {x, y, width, height}} in world units.  Only sides that have
     * road access are included; sides without road access are omitted.
     */
    private final List<float[]> borderSegments;

    CellRenderData(float x, float y, float width, float height,
                   Color fillColor, Color borderColor,
                   List<float[]> borderSegments) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.fillColor = fillColor.cpy();
        this.borderColor = borderColor.cpy();
        this.borderSegments = Collections.unmodifiableList(borderSegments);
    }

    /** Returns the world-space left edge of the cell. */
    public float getX() { return x; }
    /** Returns the world-space bottom edge of the cell. */
    public float getY() { return y; }
    /** Returns the cell width in world units. */
    public float getWidth() { return width; }
    /** Returns the cell height in world units. */
    public float getHeight() { return height; }
    /** Returns the fill color for the cell interior. */
    public Color getFillColor() { return fillColor; }
    /** Returns the color used for all border segments. */
    public Color getBorderColor() { return borderColor; }
    /**
     * Returns the pre-computed border segments as an unmodifiable list.
     * Each element is {@code float[]{x, y, width, height}}.
     * Only sides with road access are present.
     */
    public List<float[]> getBorderSegments() { return borderSegments; }
}
