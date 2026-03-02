package eb.framework1.city;

/**
 * Describes a paid or free service that a player can use at a building
 * (e.g. "Rent a Room" at a hotel, "Work Out" at a fitness centre).
 *
 * <p>Cost is in in-game currency; a cost of 0 means the service is free.
 * Time cost is in in-game minutes.</p>
 */
public class BuildingService {

    /** Machine-readable identifier (used to dispatch execution logic). */
    public final String id;

    /** Human-readable name shown in menus (e.g. "Work Out ($15, 90 min)"). */
    public final String name;

    /** Short description shown in the result popup. */
    final String description;

    /** Money cost in in-game currency (0 = free). */
    public final int cost;

    /** In-game minutes consumed when the service is used. */
    public final int timeCost;

    public BuildingService(String id, String name, String description, int cost, int timeCost) {
        this.id          = id;
        this.name        = name;
        this.description = description;
        this.cost        = cost;
        this.timeCost    = timeCost;
    }

    /** Returns a context-menu label like "Rent a Room ($50, 8h)". */
    public String menuLabel() {
        StringBuilder sb = new StringBuilder(name);
        boolean hasCost = cost > 0;
        boolean hasTime = timeCost > 0;
        if (hasCost || hasTime) {
            sb.append(" (");
            if (hasCost) sb.append("$").append(cost);
            if (hasCost && hasTime) sb.append(", ");
            if (hasTime) {
                int h = timeCost / 60;
                int m = timeCost % 60;
                if (h > 0 && m > 0) sb.append(h).append("h ").append(m).append(" min");
                else if (h > 0)     sb.append(h).append("h");
                else                sb.append(m).append(" min");
            }
            sb.append(")");
        }
        return sb.toString();
    }
}
