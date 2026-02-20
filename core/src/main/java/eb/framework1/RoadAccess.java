package eb.framework1;

/**
 * Represents road access for a single cell in the city map grid.
 * Each cell has four directional access flags: North, South, East, West.
 * Each direction stores a {@link RoadType} indicating the type of connection.
 */
public class RoadAccess {
    private RoadType north;
    private RoadType south;
    private RoadType east;
    private RoadType west;

    /**
     * Creates a RoadAccess with boolean flags (backward-compatible).
     * true maps to ROAD, false maps to NONE.
     */
    public RoadAccess(boolean north, boolean south, boolean east, boolean west) {
        this.north = north ? RoadType.ROAD : RoadType.NONE;
        this.south = south ? RoadType.ROAD : RoadType.NONE;
        this.east = east ? RoadType.ROAD : RoadType.NONE;
        this.west = west ? RoadType.ROAD : RoadType.NONE;
    }

    public boolean hasNorth() {
        return north != RoadType.NONE;
    }

    public boolean hasSouth() {
        return south != RoadType.NONE;
    }

    public boolean hasEast() {
        return east != RoadType.NONE;
    }

    public boolean hasWest() {
        return west != RoadType.NONE;
    }

    public RoadType getNorthType() {
        return north;
    }

    public RoadType getSouthType() {
        return south;
    }

    public RoadType getEastType() {
        return east;
    }

    public RoadType getWestType() {
        return west;
    }

    void setNorth(boolean north) {
        this.north = north ? RoadType.ROAD : RoadType.NONE;
    }

    void setSouth(boolean south) {
        this.south = south ? RoadType.ROAD : RoadType.NONE;
    }

    void setEast(boolean east) {
        this.east = east ? RoadType.ROAD : RoadType.NONE;
    }

    void setWest(boolean west) {
        this.west = west ? RoadType.ROAD : RoadType.NONE;
    }

    void setNorthType(RoadType type) {
        this.north = type;
    }

    void setSouthType(RoadType type) {
        this.south = type;
    }

    void setEastType(RoadType type) {
        this.east = type;
    }

    void setWestType(RoadType type) {
        this.west = type;
    }

    @Override
    public String toString() {
        return "RoadAccess{N=" + north + ", S=" + south + ", E=" + east + ", W=" + west + "}";
    }
}
