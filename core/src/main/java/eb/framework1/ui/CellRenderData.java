package eb.framework1.ui;

import eb.framework1.city.*;


/**
 * Stores pre-computed rendering data for a map cell.
 * Contains the rectangle (x, y, width, height) in grid coordinates,
 * the pre-computed color, and per-side border types based on road access.
 *
 * This data is computed once after map generation so that the rendering loop
 * can draw cells directly without repeating terrain/building/category lookups,
 * brightness calculations, and road access lookups each frame.
 *
 * Border types indicate the kind of road on each side:
 * <ul>
 *   <li>ROAD - full border gap (standard road width)</li>
 *   <li>PATHWAY - narrow border gap (1/4 of road width)</li>
 *   <li>NONE - no border (building extends to cell edge)</li>
 * </ul>
 */
public class CellRenderData {
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final float r;
    private final float g;
    private final float b;
    private final float a;
    private final RoadType borderNorth;
    private final RoadType borderSouth;
    private final RoadType borderEast;
    private final RoadType borderWest;
    private final String iconPath;

    /**
     * Creates a new CellRenderData with the given grid rectangle, color, border types, and icon path.
     *
     * @param x            The x coordinate in the grid
     * @param y            The y coordinate in the grid
     * @param width        The width in grid cells (typically 1)
     * @param height       The height in grid cells (typically 1)
     * @param r            Red color component (0.0-1.0)
     * @param g            Green color component (0.0-1.0)
     * @param b            Blue color component (0.0-1.0)
     * @param a            Alpha color component (0.0-1.0)
     * @param borderNorth  The road type on the north side
     * @param borderSouth  The road type on the south side
     * @param borderEast   The road type on the east side
     * @param borderWest   The road type on the west side
     * @param iconPath     The path to the building icon (null if no icon)
     */
    public CellRenderData(int x, int y, int width, int height, float r, float g, float b, float a,
                          RoadType borderNorth, RoadType borderSouth, RoadType borderEast, RoadType borderWest,
                          String iconPath) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.borderNorth = borderNorth;
        this.borderSouth = borderSouth;
        this.borderEast = borderEast;
        this.borderWest = borderWest;
        this.iconPath = iconPath;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float getR() {
        return r;
    }

    public float getG() {
        return g;
    }

    public float getB() {
        return b;
    }

    public float getA() {
        return a;
    }

    public boolean hasBorderNorth() {
        return borderNorth != RoadType.NONE;
    }

    public boolean hasBorderSouth() {
        return borderSouth != RoadType.NONE;
    }

    public boolean hasBorderEast() {
        return borderEast != RoadType.NONE;
    }

    public boolean hasBorderWest() {
        return borderWest != RoadType.NONE;
    }

    public RoadType getBorderTypeNorth() {
        return borderNorth;
    }

    public RoadType getBorderTypeSouth() {
        return borderSouth;
    }

    public RoadType getBorderTypeEast() {
        return borderEast;
    }

    public RoadType getBorderTypeWest() {
        return borderWest;
    }

    /**
     * Gets the icon path for this cell's building, or null if no icon.
     */
    public String getIconPath() {
        return iconPath;
    }

    @Override
    public String toString() {
        return "CellRenderData{x=" + x + ", y=" + y +
               ", width=" + width + ", height=" + height +
               ", color=(" + r + "," + g + "," + b + "," + a + ")" +
               ", borders={N=" + borderNorth + ",S=" + borderSouth +
               ",E=" + borderEast + ",W=" + borderWest + "}" +
               ", icon=" + iconPath + "}";
    }
}
