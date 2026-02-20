package eb.framework1;

/**
 * Represents road access for a single cell in the city map grid.
 * Each cell has four directional access flags: North, South, East, West.
 */
public class RoadAccess {
    private boolean north;
    private boolean south;
    private boolean east;
    private boolean west;

    public RoadAccess(boolean north, boolean south, boolean east, boolean west) {
        this.north = north;
        this.south = south;
        this.east = east;
        this.west = west;
    }

    public boolean hasNorth() {
        return north;
    }

    public boolean hasSouth() {
        return south;
    }

    public boolean hasEast() {
        return east;
    }

    public boolean hasWest() {
        return west;
    }

    void setNorth(boolean north) {
        this.north = north;
    }

    void setSouth(boolean south) {
        this.south = south;
    }

    void setEast(boolean east) {
        this.east = east;
    }

    void setWest(boolean west) {
        this.west = west;
    }

    @Override
    public String toString() {
        return "RoadAccess{N=" + north + ", S=" + south + ", E=" + east + ", W=" + west + "}";
    }
}
