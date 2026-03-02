package eb.gmodel1.city;

/**
 * Enum representing different terrain types in the city map.
 */
public enum TerrainType {
    MOUNTAIN("Mountain", false),
    BEACH("Beach", true),
    BUILDING("Building", true);

    private final String displayName;
    private final boolean accessible;

    TerrainType(String displayName, boolean accessible) {
        this.displayName = displayName;
        this.accessible = accessible;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAccessible() {
        return accessible;
    }
}
