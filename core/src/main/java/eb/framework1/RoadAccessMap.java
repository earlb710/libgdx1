package eb.framework1;

/**
 * Computes a road access map for a 2-D terrain grid.
 *
 * <p>Rules:
 * <ul>
 *   <li>Mountains have no road access in any direction.</li>
 *   <li>Beach cells only have access on sides that border a non-beach terrain
 *       (e.g. PLAINS, FOREST, WATER). A beach-to-beach border is not accessible.</li>
 *   <li>All other terrain types have full access in every direction.</li>
 * </ul>
 *
 * <p>Grid coordinates: {@code terrain[row][col]} where row 0 is the northernmost row.
 * Increasing row → south, increasing col → east.
 */
public class RoadAccessMap {

    private final RoadAccess[][] accessMap;
    private final int rows;
    private final int cols;

    /**
     * Builds the road access map from the provided terrain grid.
     *
     * @param terrain 2-D array of {@link TerrainType} values (terrain[row][col]).
     *                Must be non-null, non-empty, and rectangular.
     */
    public RoadAccessMap(TerrainType[][] terrain) {
        if (terrain == null || terrain.length == 0 || terrain[0] == null || terrain[0].length == 0) {
            throw new IllegalArgumentException("Terrain grid must be non-null and non-empty");
        }
        this.rows = terrain.length;
        this.cols = terrain[0].length;
        this.accessMap = new RoadAccess[rows][cols];
        build(terrain);
    }

    private void build(TerrainType[][] terrain) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                accessMap[r][c] = computeAccess(terrain, r, c);
            }
        }
    }

    private RoadAccess computeAccess(TerrainType[][] terrain, int r, int c) {
        TerrainType current = terrain[r][c];

        // Mountains have no access in any direction.
        if (current == TerrainType.MOUNTAIN) {
            return new RoadAccess(false, false, false, false);
        }

        // For beach cells, access is only allowed toward a non-beach neighbor.
        if (current == TerrainType.BEACH) {
            boolean n = hasNonBeachNeighbor(terrain, r, c, -1, 0);
            boolean s = hasNonBeachNeighbor(terrain, r, c, 1, 0);
            boolean e = hasNonBeachNeighbor(terrain, r, c, 0, 1);
            boolean w = hasNonBeachNeighbor(terrain, r, c, 0, -1);
            return new RoadAccess(n, s, e, w);
        }

        // All other terrain types have full access.
        return new RoadAccess(true, true, true, true);
    }

    /**
     * Returns true when the neighbor in direction (dr, dc) exists on the grid
     * and is not a beach (and not a mountain, which would block entry from the other side,
     * but the access flag here represents the beach cell's own outward access).
     */
    private boolean hasNonBeachNeighbor(TerrainType[][] terrain, int r, int c, int dr, int dc) {
        int nr = r + dr;
        int nc = c + dc;
        if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) {
            return false; // No neighbor at map edge – no access.
        }
        TerrainType neighbor = terrain[nr][nc];
        return neighbor != TerrainType.BEACH;
    }

    /**
     * Returns the {@link RoadAccess} for the cell at the given row and column.
     *
     * @param row row index (0 = northernmost)
     * @param col column index (0 = westernmost)
     * @return road access information for the cell
     */
    public RoadAccess getAccess(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            throw new IndexOutOfBoundsException(
                "Cell (" + row + ", " + col + ") is outside the grid (" + rows + "x" + cols + ")");
        }
        return accessMap[row][col];
    }

    /** Returns the number of rows in the grid. */
    public int getRows() {
        return rows;
    }

    /** Returns the number of columns in the grid. */
    public int getCols() {
        return cols;
    }
}
