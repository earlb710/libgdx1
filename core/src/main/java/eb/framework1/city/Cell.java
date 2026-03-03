package eb.framework1.city;

/**
 * Represents a single cell in the city map grid.
 */
public class Cell {
    private final int x;
    private final int y;
    private final TerrainType terrainType;
    private final Building building;

    public Cell(int x, int y, TerrainType terrainType, Building building) {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("Cell coordinates cannot be negative");
        }
        if (terrainType == null) {
            throw new IllegalArgumentException("Terrain type cannot be null");
        }
        this.x = x;
        this.y = y;
        this.terrainType = terrainType;
        this.building = building;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public TerrainType getTerrainType() {
        return terrainType;
    }

    public Building getBuilding() {
        return building;
    }

    public boolean hasBuilding() {
        return building != null;
    }

    public boolean isAccessible() {
        return terrainType.isAccessible();
    }

    @Override
    public String toString() {
        return "Cell{x=" + x + ", y=" + y + ", terrain=" + terrainType + 
               ", building=" + (building != null ? building.getName() : "none") + "}";
    }
}
