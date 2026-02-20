package eb.framework1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

/**
 * Represents a road access map for a city grid.
 * Each cell has directional access (North, South, East, West) governed by these rules:
 * <ul>
 *   <li>Mountains have no access in any direction.</li>
 *   <li>Beach cells only have access on borders with non-beach neighbors.</li>
 *   <li>Building cells have access in all directions where a neighbor exists.</li>
 *   <li>Reciprocal constraint: if a cell has no access in a direction, the neighbor
 *       in that direction must not have access in the opposite direction.</li>
 * </ul>
 */
public class RoadAccessMap {

    static final double ROAD_REMOVAL_PERCENTAGE = 0.30;

    /**
     * Represents the four cardinal directions.
     */
    public enum Direction {
        NORTH, SOUTH, EAST, WEST
    }

    private final RoadAccess[][] access;
    private final int size;

    /**
     * Creates a road access map from the given city map.
     * Computes directional access for each cell based on terrain rules
     * and enforces reciprocal constraints.
     *
     * @param cityMap The city map to derive access from
     */
    public RoadAccessMap(CityMap cityMap) {
        this.size = cityMap.getSize();
        this.access = new RoadAccess[size][size];
        computeAccess(cityMap);
    }

    private void computeAccess(CityMap cityMap) {
        // Step 1: Initialize access based on terrain rules
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                Cell cell = cityMap.getCell(x, y);
                TerrainType terrain = cell.getTerrainType();

                if (terrain == TerrainType.MOUNTAIN) {
                    // Mountains have no access
                    access[x][y] = new RoadAccess(false, false, false, false);
                } else if (terrain == TerrainType.BEACH) {
                    // Beach only has access on borders with non-beach neighbors
                    boolean north = y + 1 < size && cityMap.getCell(x, y + 1).getTerrainType() != TerrainType.BEACH;
                    boolean south = y - 1 >= 0 && cityMap.getCell(x, y - 1).getTerrainType() != TerrainType.BEACH;
                    boolean east = x + 1 < size && cityMap.getCell(x + 1, y).getTerrainType() != TerrainType.BEACH;
                    boolean west = x - 1 >= 0 && cityMap.getCell(x - 1, y).getTerrainType() != TerrainType.BEACH;
                    access[x][y] = new RoadAccess(north, south, east, west);
                } else {
                    // Building: access in all directions where a neighbor exists
                    boolean north = y + 1 < size;
                    boolean south = y - 1 >= 0;
                    boolean east = x + 1 < size;
                    boolean west = x - 1 >= 0;
                    access[x][y] = new RoadAccess(north, south, east, west);
                }
            }
        }

        // Step 2: Enforce reciprocal constraints
        enforceReciprocal();
    }

    /**
     * Iteratively enforces the reciprocal constraint across all cells:
     * if a cell has no access in a direction, the neighbor in that direction
     * must not have access in the opposite direction.
     */
    private void enforceReciprocal() {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    RoadAccess ra = access[x][y];

                    // North neighbor at (x, y+1) must not have South access
                    if (!ra.hasNorth() && y + 1 < size && access[x][y + 1].hasSouth()) {
                        access[x][y + 1].setSouth(false);
                        changed = true;
                    }
                    // South neighbor at (x, y-1) must not have North access
                    if (!ra.hasSouth() && y - 1 >= 0 && access[x][y - 1].hasNorth()) {
                        access[x][y - 1].setNorth(false);
                        changed = true;
                    }
                    // East neighbor at (x+1, y) must not have West access
                    if (!ra.hasEast() && x + 1 < size && access[x + 1][y].hasWest()) {
                        access[x + 1][y].setWest(false);
                        changed = true;
                    }
                    // West neighbor at (x-1, y) must not have East access
                    if (!ra.hasWest() && x - 1 >= 0 && access[x - 1][y].hasEast()) {
                        access[x - 1][y].setEast(false);
                        changed = true;
                    }
                }
            }
        }
    }

    /**
     * Sets the access for a cell in the specified direction.
     * When access is removed (set to false), the reciprocal constraint is enforced:
     * the neighbor in that direction also loses access in the opposite direction.
     *
     * @param x         The x coordinate
     * @param y         The y coordinate
     * @param direction The direction to set access for
     * @param value     true to grant access, false to remove access
     * @throws IllegalArgumentException if coordinates are out of bounds
     */
    public void setAccess(int x, int y, Direction direction, boolean value) {
        if (x < 0 || x >= size || y < 0 || y >= size) {
            throw new IllegalArgumentException("Coordinates out of bounds: (" + x + ", " + y + ")");
        }

        RoadAccess ra = access[x][y];
        switch (direction) {
            case NORTH:
                ra.setNorth(value);
                if (!value && y + 1 < size) {
                    access[x][y + 1].setSouth(false);
                }
                break;
            case SOUTH:
                ra.setSouth(value);
                if (!value && y - 1 >= 0) {
                    access[x][y - 1].setNorth(false);
                }
                break;
            case EAST:
                ra.setEast(value);
                if (!value && x + 1 < size) {
                    access[x + 1][y].setWest(false);
                }
                break;
            case WEST:
                ra.setWest(value);
                if (!value && x - 1 >= 0) {
                    access[x - 1][y].setEast(false);
                }
                break;
        }
    }

    /**
     * Gets the road access for the cell at the specified coordinates.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @return The road access for the cell
     * @throws IllegalArgumentException if coordinates are out of bounds
     */
    public RoadAccess getAccess(int x, int y) {
        if (x < 0 || x >= size || y < 0 || y >= size) {
            throw new IllegalArgumentException("Coordinates out of bounds: (" + x + ", " + y + ")");
        }
        return access[x][y];
    }

    /**
     * Gets the size of the access map.
     *
     * @return The map size
     */
    public int getSize() {
        return size;
    }

    /**
     * Removes approximately 30% of existing roads using the given random seed,
     * while ensuring all connected cells remain reachable so that no cell is cut off.
     *
     * @param seed Random seed for deterministic road removal
     */
    public void removeRoads(long seed) {
        // Step 1: Record original connectivity (component IDs)
        int[][] originalComponentId = computeComponentIds();

        // Step 2: Collect all road edges
        List<int[]> edges = collectEdges();

        // Step 3: Calculate target removals and shuffle
        int targetRemovals = (int) Math.round(edges.size() * ROAD_REMOVAL_PERCENTAGE);
        Random random = new Random(seed);
        Collections.shuffle(edges, random);

        // Step 4: Remove edges while preserving connectivity
        int removed = 0;
        for (int[] edge : edges) {
            if (removed >= targetRemovals) break;

            int x = edge[0], y = edge[1], dir = edge[2];

            // Remove the edge
            if (dir == 0) { // East-West
                access[x][y].setEast(false);
                access[x + 1][y].setWest(false);
            } else { // North-South
                access[x][y].setNorth(false);
                access[x][y + 1].setSouth(false);
            }

            // Check connectivity is preserved
            if (isConnectivityPreserved(originalComponentId)) {
                removed++;
            } else {
                // Restore the edge
                if (dir == 0) {
                    access[x][y].setEast(true);
                    access[x + 1][y].setWest(true);
                } else {
                    access[x][y].setNorth(true);
                    access[x][y + 1].setSouth(true);
                }
            }
        }
    }

    /**
     * Counts the total number of road connections in the map.
     * Each road between two adjacent cells is counted once.
     *
     * @return The number of road connections
     */
    public int countRoads() {
        int count = 0;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (access[x][y].hasEast() && x + 1 < size) count++;
                if (access[x][y].hasNorth() && y + 1 < size) count++;
            }
        }
        return count;
    }

    /**
     * Collects all road edges. Each edge is represented as {x, y, direction}
     * where direction 0 = East-West edge, 1 = North-South edge.
     */
    private List<int[]> collectEdges() {
        List<int[]> edges = new ArrayList<>();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (access[x][y].hasEast() && x + 1 < size) {
                    edges.add(new int[]{x, y, 0});
                }
                if (access[x][y].hasNorth() && y + 1 < size) {
                    edges.add(new int[]{x, y, 1});
                }
            }
        }
        return edges;
    }

    /**
     * Computes connected component IDs for all cells with road access.
     * Cells with no access get component ID -1.
     */
    private int[][] computeComponentIds() {
        int[][] ids = new int[size][size];
        for (int[] row : ids) Arrays.fill(row, -1);
        int componentCount = 0;

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                RoadAccess ra = access[x][y];
                if (ids[x][y] == -1 && (ra.hasNorth() || ra.hasSouth() || ra.hasEast() || ra.hasWest())) {
                    bfsFill(x, y, ids, componentCount);
                    componentCount++;
                }
            }
        }
        return ids;
    }

    /**
     * BFS to assign a component ID to all cells reachable from (startX, startY).
     */
    private void bfsFill(int startX, int startY, int[][] ids, int componentId) {
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{startX, startY});
        ids[startX][startY] = componentId;

        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int cx = cell[0], cy = cell[1];
            RoadAccess ra = access[cx][cy];

            if (ra.hasNorth() && cy + 1 < size && ids[cx][cy + 1] == -1) {
                ids[cx][cy + 1] = componentId;
                queue.add(new int[]{cx, cy + 1});
            }
            if (ra.hasSouth() && cy - 1 >= 0 && ids[cx][cy - 1] == -1) {
                ids[cx][cy - 1] = componentId;
                queue.add(new int[]{cx, cy - 1});
            }
            if (ra.hasEast() && cx + 1 < size && ids[cx + 1][cy] == -1) {
                ids[cx + 1][cy] = componentId;
                queue.add(new int[]{cx + 1, cy});
            }
            if (ra.hasWest() && cx - 1 >= 0 && ids[cx - 1][cy] == -1) {
                ids[cx - 1][cy] = componentId;
                queue.add(new int[]{cx - 1, cy});
            }
        }
    }

    /**
     * Checks that connectivity is preserved: every cell that was in an original
     * connected component is still connected to all other cells in that component.
     */
    private boolean isConnectivityPreserved(int[][] originalComponentId) {
        int[][] currentIds = computeComponentIds();

        // For each original component, all its cells must map to the same current component
        Map<Integer, Integer> mapping = new HashMap<>();

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int origId = originalComponentId[x][y];
                if (origId >= 0) {
                    int currId = currentIds[x][y];
                    if (currId < 0) return false; // Cell lost all access

                    Integer expected = mapping.get(origId);
                    if (expected == null) {
                        mapping.put(origId, currId);
                    } else if (expected.intValue() != currId) {
                        return false; // Component was split
                    }
                }
            }
        }
        return true;
    }
}
