package eb.framework1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import com.badlogic.gdx.graphics.Color;

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

    private static final double ROAD_REMOVAL_RATIO = 0.30;

    /**
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

    /**
     * Sets the road access for the cell at {@code (row, col)} and enforces symmetry
     * with each cardinal neighbor: if this cell's access in direction D is {@code false},
     * the neighbor in direction D has its opposite access set to {@code false} as well;
     * if it is {@code true}, the neighbor's opposite access is set to {@code true}.
     *
     * <p>This method is not thread-safe. It must be called from a single thread
     * (e.g. the LibGDX rendering/logic thread).
     *
     * @param row   row index (0 = northernmost)
     * @param col   column index (0 = westernmost)
     * @param north whether this cell has road access toward the north
     * @param south whether this cell has road access toward the south
     * @param east  whether this cell has road access toward the east
     * @param west  whether this cell has road access toward the west
     */
    public void setAccess(int row, int col, boolean north, boolean south, boolean east, boolean west) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            throw new IndexOutOfBoundsException(
                "Cell (" + row + ", " + col + ") is outside the grid (" + rows + "x" + cols + ")");
        }
        RoadAccess cell = accessMap[row][col];
        cell.setNorth(north);
        cell.setSouth(south);
        cell.setEast(east);
        cell.setWest(west);

        // Propagate each direction to the corresponding neighbor's opposite direction.
        if (row - 1 >= 0) {
            accessMap[row - 1][col].setSouth(north);
        }
        if (row + 1 < rows) {
            accessMap[row + 1][col].setNorth(south);
        }
        if (col + 1 < cols) {
            accessMap[row][col + 1].setWest(east);
        }
        if (col - 1 >= 0) {
            accessMap[row][col - 1].setEast(west);
        }
    }

    /** Returns the number of rows in the grid. */
    public int getRows() {
        return rows;
    }

    /** Returns the number of columns in the grid. */
    public int getCols() {
        return cols;
    }

    /**
     * Removes approximately 30% of road edges at random using {@code randSeed}, while
     * guaranteeing that every cell that was reachable from another cell before the call
     * remains reachable afterwards (i.e. no cell is cut off).
     *
     * <p>A road edge is a bidirectional connection between two adjacent cells where both
     * cells have access toward each other. The method:
     * <ol>
     *   <li>Collects all current road edges.</li>
     *   <li>Builds a spanning forest (one BFS spanning tree per connected component) to
     *       identify the minimum set of edges needed for full connectivity.</li>
     *   <li>Shuffles the non-spanning-tree edges with {@code new Random(randSeed)}.</li>
     *   <li>Removes up to {@code floor(totalEdges * 0.30)} of those edges.</li>
     * </ol>
     *
     * <p>If the number of removable (non-tree) edges is smaller than the 30% target, as
     * many edges as possible are removed without breaking connectivity.
     *
     * <p>This method is not thread-safe. It must be called from a single thread
     * (e.g. the LibGDX rendering/logic thread).
     *
     * @param randSeed seed for the random shuffle, typically {@link eb.framework1.Profile#getRandSeed()}
     */
    public void removeRoads(long randSeed) {
        // Step 1: collect all undirected road edges.
        // Each edge is stored as {r1, c1, r2, c2} where (r2,c2) is the east or south neighbor.
        List<int[]> allEdges = collectRoadEdges();
        int totalEdges = allEdges.size();
        int targetRemove = (int) (totalEdges * ROAD_REMOVAL_RATIO);
        if (targetRemove == 0) {
            return;
        }

        // Step 2: build a spanning forest to protect the minimum connectivity edges.
        Set<String> treeEdgeKeys = buildSpanningForestEdgeKeys(allEdges);

        // Step 3: separate removable (non-tree) edges.
        List<int[]> removable = new ArrayList<>();
        for (int[] e : allEdges) {
            if (!treeEdgeKeys.contains(edgeKey(e))) {
                removable.add(e);
            }
        }

        // Step 4: shuffle and remove up to targetRemove edges.
        Collections.shuffle(removable, new Random(randSeed));
        List<int[]> toRemove = removable.subList(0, Math.min(targetRemove, removable.size()));
        for (int[] e : toRemove) {
            if (e[0] == e[2]) {
                // Horizontal edge (same row): remove east/west access.
                accessMap[e[0]][e[1]].setEast(false);
                accessMap[e[2]][e[3]].setWest(false);
            } else {
                // Vertical edge (same column): remove south/north access.
                accessMap[e[0]][e[1]].setSouth(false);
                accessMap[e[2]][e[3]].setNorth(false);
            }
        }
    }

    /**
     * Collects every undirected road edge currently present in the access map.
     * Each edge is represented as {@code {r1, c1, r2, c2}} where (r2,c2) is either the
     * eastern neighbor (same row, col+1) or the southern neighbor (row+1, same col).
     */
    private List<int[]> collectRoadEdges() {
        List<int[]> edges = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // East edge
                if (c + 1 < cols
                        && accessMap[r][c].hasEastAccess()
                        && accessMap[r][c + 1].hasWestAccess()) {
                    edges.add(new int[]{r, c, r, c + 1});
                }
                // South edge
                if (r + 1 < rows
                        && accessMap[r][c].hasSouthAccess()
                        && accessMap[r + 1][c].hasNorthAccess()) {
                    edges.add(new int[]{r, c, r + 1, c});
                }
            }
        }
        return edges;
    }

    /**
     * Builds a spanning forest over the road graph defined by {@code edges} using BFS,
     * and returns the string keys of all edges that are part of the spanning forest.
     * Spanning-forest edges are the minimum set required to keep every connected component
     * internally connected.
     */
    private Set<String> buildSpanningForestEdgeKeys(List<int[]> edges) {
        if (edges.isEmpty()) {
            return new HashSet<>();
        }

        // Build adjacency list keyed by "row,col".
        Map<String, List<int[]>> adj = new HashMap<>();
        for (int[] e : edges) {
            String k1 = nodeKey(e[0], e[1]);
            String k2 = nodeKey(e[2], e[3]);
            adj.computeIfAbsent(k1, x -> new ArrayList<>()).add(e);
            adj.computeIfAbsent(k2, x -> new ArrayList<>()).add(e);
        }

        Set<String> visited = new HashSet<>();
        Set<String> treeEdgeKeys = new HashSet<>();

        // BFS from every unvisited node to cover all components (spanning forest).
        for (String startNode : adj.keySet()) {
            if (visited.contains(startNode)) {
                continue;
            }
            Queue<String> queue = new LinkedList<>();
            queue.add(startNode);
            visited.add(startNode);
            while (!queue.isEmpty()) {
                String node = queue.poll();
                for (int[] e : adj.getOrDefault(node, Collections.emptyList())) {
                    String k1 = nodeKey(e[0], e[1]);
                    String k2 = nodeKey(e[2], e[3]);
                    String other = node.equals(k1) ? k2 : k1;
                    if (!visited.contains(other)) {
                        visited.add(other);
                        treeEdgeKeys.add(edgeKey(e));
                        queue.add(other);
                    }
                }
            }
        }
        return treeEdgeKeys;
    }

    private static String nodeKey(int r, int c) {
        return r + "," + c;
    }

    private static String edgeKey(int[] e) {
        return e[0] + "," + e[1] + "," + e[2] + "," + e[3];
    }

    // -----------------------------------------------------------------------
    // Render-data pre-computation
    // -----------------------------------------------------------------------

    /** Border color shared by all cells. */
    private static final Color BORDER_COLOR = new Color(0.2f, 0.2f, 0.2f, 1f);

    /**
     * Pre-computes a {@link CellRenderData} for every cell in the grid.
     *
     * <p>Call this once after the map (terrain + road access) is fully generated.
     * The returned grid can then be iterated each frame to draw cells without
     * re-querying road-access flags or recomputing geometry.
     *
     * <p>Coordinate convention (matches LibGDX's default):
     * <ul>
     *   <li>y increases upward.</li>
     *   <li>Row 0 (northernmost) maps to the highest world-space y value.</li>
     *   <li>Cell {@code (row, col)} has its bottom-left corner at
     *       {@code (originX + col*cellWidth, originY + (rows-1-row)*cellHeight)}.</li>
     * </ul>
     *
     * <p>Each cell receives:
     * <ul>
     *   <li>A fill rectangle covering the full cell, colored by terrain type.</li>
     *   <li>One border segment for every side that has road access; sides without
     *       road access are omitted entirely.</li>
     * </ul>
     *
     * @param terrain         terrain grid used to derive fill colors; must have the same
     *                        dimensions as the terrain supplied to the constructor
     * @param originX         world-space x of the grid's left edge
     * @param originY         world-space y of the grid's bottom edge
     * @param cellWidth       width of a single cell in world units
     * @param cellHeight      height of a single cell in world units
     * @param borderThickness thickness of a border segment in world units
     * @return a {@code rows × cols} grid of pre-computed render data
     */
    public CellRenderData[][] buildRenderData(TerrainType[][] terrain,
                                              float originX, float originY,
                                              float cellWidth, float cellHeight,
                                              float borderThickness) {
        if (terrain == null || terrain.length != rows
                || terrain[0] == null || terrain[0].length != cols) {
            throw new IllegalArgumentException(
                "Terrain grid must be non-null and match the access map dimensions ("
                + rows + "x" + cols + ")");
        }

        CellRenderData[][] renderData = new CellRenderData[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float cellX = originX + c * cellWidth;
                float cellY = originY + (rows - 1 - r) * cellHeight;
                Color fill = fillColorFor(terrain[r][c]);
                RoadAccess access = accessMap[r][c];
                List<float[]> segments = buildBorderSegments(
                        cellX, cellY, cellWidth, cellHeight, borderThickness, access);
                renderData[r][c] = new CellRenderData(
                        cellX, cellY, cellWidth, cellHeight,
                        fill, BORDER_COLOR, segments);
            }
        }
        return renderData;
    }

    /**
     * Returns the border segment rectangles for one cell.
     * A segment is only added for a side that has road access.
     */
    private static List<float[]> buildBorderSegments(float cellX, float cellY,
                                                     float cellWidth, float cellHeight,
                                                     float borderThickness,
                                                     RoadAccess access) {
        List<float[]> segments = new ArrayList<>(4);
        // North border: top edge of the cell.
        if (access.hasNorthAccess()) {
            segments.add(new float[]{cellX, cellY + cellHeight - borderThickness,
                                     cellWidth, borderThickness});
        }
        // South border: bottom edge of the cell.
        if (access.hasSouthAccess()) {
            segments.add(new float[]{cellX, cellY, cellWidth, borderThickness});
        }
        // West border: left edge of the cell.
        if (access.hasWestAccess()) {
            segments.add(new float[]{cellX, cellY, borderThickness, cellHeight});
        }
        // East border: right edge of the cell.
        if (access.hasEastAccess()) {
            segments.add(new float[]{cellX + cellWidth - borderThickness, cellY,
                                     borderThickness, cellHeight});
        }
        return segments;
    }

    /** Returns the fill color for a given terrain type. */
    private static final Color COLOR_PLAINS   = new Color(0.40f, 0.80f, 0.20f, 1f);
    private static final Color COLOR_FOREST   = new Color(0.10f, 0.50f, 0.10f, 1f);
    private static final Color COLOR_BEACH    = new Color(0.90f, 0.80f, 0.50f, 1f);
    private static final Color COLOR_MOUNTAIN = new Color(0.50f, 0.50f, 0.50f, 1f);
    private static final Color COLOR_WATER    = new Color(0.20f, 0.40f, 0.80f, 1f);

    private static Color fillColorFor(TerrainType type) {
        switch (type) {
            case PLAINS:   return COLOR_PLAINS.cpy();
            case FOREST:   return COLOR_FOREST.cpy();
            case BEACH:    return COLOR_BEACH.cpy();
            case MOUNTAIN: return COLOR_MOUNTAIN.cpy();
            case WATER:    return COLOR_WATER.cpy();
            default:       return Color.WHITE.cpy();
        }
    }
}
