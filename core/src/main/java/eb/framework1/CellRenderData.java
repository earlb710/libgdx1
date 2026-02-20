package eb.framework1;

/**
 * Stores pre-computed rendering data for a map cell.
 * Contains the rectangle (x, y, width, height) in grid coordinates
 * and the pre-computed color for efficient redrawing without recalculation.
 *
 * This data is computed once after map generation so that the rendering loop
 * can draw cells directly without repeating terrain/building/category lookups
 * and brightness calculations each frame.
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

    /**
     * Creates a new CellRenderData with the given grid rectangle and color.
     *
     * @param x      The x coordinate in the grid
     * @param y      The y coordinate in the grid
     * @param width  The width in grid cells (typically 1)
     * @param height The height in grid cells (typically 1)
     * @param r      Red color component (0.0-1.0)
     * @param g      Green color component (0.0-1.0)
     * @param b      Blue color component (0.0-1.0)
     * @param a      Alpha color component (0.0-1.0)
     */
    public CellRenderData(int x, int y, int width, int height, float r, float g, float b, float a) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
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

    @Override
    public String toString() {
        return "CellRenderData{x=" + x + ", y=" + y +
               ", width=" + width + ", height=" + height +
               ", color=(" + r + "," + g + "," + b + "," + a + ")}";
    }
}
