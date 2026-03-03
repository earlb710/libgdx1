package eb.framework1.city;

/**
 * Represents the type of road connection between adjacent cells.
 * <ul>
 *   <li>NONE - No road connection (building extends to cell edge)</li>
 *   <li>ROAD - Full road connection (standard border gap)</li>
 *   <li>PATHWAY - Narrow pathway connection (1/4 the width of a road)</li>
 * </ul>
 */
public enum RoadType {
    NONE,
    ROAD,
    PATHWAY
}
