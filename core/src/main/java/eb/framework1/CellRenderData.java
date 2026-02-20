package eb.framework1;

/**
 * Stores pre-computed rendering data for a map cell.
 * Contains the rectangle (x, y, width, height) in grid coordinates,
 * the pre-computed color, and per-side border flags based on road access.
 *
 * This data is computed once after map generation so that the rendering loop
 * can draw cells directly without repeating terrain/building/category lookups,
 * brightness calculations, and road access lookups each frame.
 *
 * Border flags indicate whether a border (gap) should be drawn on each side.
 * A border is drawn where road access exists (representing the road between cells).
 * No border is drawn where road access does not exist (building extends to edge).
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
    private final boolean borderNorth;
    private final boolean borderSouth;
    private final boolean borderEast;
    private final boolean borderWest;

    /**
     * Creates a new CellRenderData with the given grid rectangle, color, and border flags.
     *
     * @param x            The x coordinate in the grid
     * @param y            The y coordinate in the grid
     * @param width        The width in grid cells (typically 1)
     * @param height       The height in grid cells (typically 1)
     * @param r            Red color component (0.0-1.0)
     * @param g            Green color component (0.0-1.0)
     * @param b            Blue color component (0.0-1.0)
     * @param a            Alpha color component (0.0-1.0)
     * @param borderNorth  Whether to draw a border on the north side
     * @param borderSouth  Whether to draw a border on the south side
     * @param borderEast   Whether to draw a border on the east side
     * @param borderWest   Whether to draw a border on the west side
     */
    public CellRenderData(int x, int y, int width, int height, float r, float g, float b, float a,
                          boolean borderNorth, boolean borderSouth, boolean borderEast, boolean borderWest) {
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
        return borderNorth;
    }

    public boolean hasBorderSouth() {
        return borderSouth;
    }

    public boolean hasBorderEast() {
        return borderEast;
    }

    public boolean hasBorderWest() {
        return borderWest;
    }

    @Override
    public String toString() {
        return "CellRenderData{x=" + x + ", y=" + y +
               ", width=" + width + ", height=" + height +
               ", color=(" + r + "," + g + "," + b + "," + a + ")" +
               ", borders={N=" + borderNorth + ",S=" + borderSouth +
               ",E=" + borderEast + ",W=" + borderWest + "}}";
    }
}
