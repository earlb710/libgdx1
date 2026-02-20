package eb.framework1;

/** Represents road access for a single map cell in the four cardinal directions. */
public class RoadAccess {
    private final boolean north;
    private final boolean south;
    private final boolean east;
    private final boolean west;

    public RoadAccess(boolean north, boolean south, boolean east, boolean west) {
        this.north = north;
        this.south = south;
        this.east = east;
        this.west = west;
    }

    public boolean hasNorthAccess() {
        return north;
    }

    public boolean hasSouthAccess() {
        return south;
    }

    public boolean hasEastAccess() {
        return east;
    }

    public boolean hasWestAccess() {
        return west;
    }

    /** Returns true if there is access in at least one direction. */
    public boolean hasAnyAccess() {
        return north || south || east || west;
    }

    @Override
    public String toString() {
        return "RoadAccess{N=" + north + ", S=" + south + ", E=" + east + ", W=" + west + "}";
    }
}
