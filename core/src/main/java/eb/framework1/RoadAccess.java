package eb.framework1;

/** Represents road access for a single map cell in the four cardinal directions. */
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

    public void setNorth(boolean north) {
        this.north = north;
    }

    public void setSouth(boolean south) {
        this.south = south;
    }

    public void setEast(boolean east) {
        this.east = east;
    }

    public void setWest(boolean west) {
        this.west = west;
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
