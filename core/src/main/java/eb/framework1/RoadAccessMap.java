package eb.framework1;

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
}
