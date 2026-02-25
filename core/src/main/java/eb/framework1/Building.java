package eb.framework1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a building in a cell with improvements.
 * Buildings start undiscovered and are only revealed when the cell is visited by a character.
 * Each building may affect character attributes (from -3 to +3),
 * determined by the building's name via {@link BuildingEffects}.
 */
public class Building {
    private static final int IMPROVEMENT_COUNT = 4;
    
    private final String name;
    private final List<Improvement> improvements;
    private final BuildingDefinition definition;
    private final int floors;
    private final Map<CharacterAttribute, Integer> attributeModifiers;
    private boolean discovered;
    private boolean home;
    private boolean owned;
    /** Company / tenant names for this building instance.  Empty for non-company buildings. */
    private List<String> tenants;
    /**
     * Maintenance/condition state of this building: "good", "normal", or "bad".
     * Determined at map-generation time based on distance from the map centre.
     */
    private String state;

    /**
     * Creates a building with just a name and improvements (legacy constructor).
     */
    public Building(String name, List<Improvement> improvements) {
        this(name, improvements, null, 1);
    }

    /**
     * Creates a building with a definition, floors, and improvements.
     * Attribute modifiers are automatically determined from the building name.
     */
    public Building(String name, List<Improvement> improvements, BuildingDefinition definition, int floors) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Building name cannot be null or empty");
        }
        if (improvements == null || improvements.size() != IMPROVEMENT_COUNT) {
            throw new IllegalArgumentException("Building must have exactly " + IMPROVEMENT_COUNT + " improvements");
        }
        this.name = name.trim();
        this.improvements = new ArrayList<>(improvements);
        this.definition = definition;
        this.floors = floors;
        this.attributeModifiers = BuildingEffects.getEffects(this.name);
        this.discovered = false;
        this.tenants = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the display name for this building.  For company buildings this is the
     * first (or only) tenant name; for non-company buildings it falls back to the
     * building type name.
     */
    public String getDisplayName() {
        return (tenants != null && !tenants.isEmpty()) ? tenants.get(0) : name;
    }

    /**
     * Returns all tenant (company) names assigned to this building.
     * Single-company buildings have exactly one entry; multi-tenant buildings have
     * several.  Non-company buildings return an empty list.
     */
    public List<String> getTenants() {
        return (tenants != null && !tenants.isEmpty())
                ? Collections.unmodifiableList(tenants)
                : Collections.<String>emptyList();
    }

    /**
     * Assigns the tenant (company) names for this building.
     *
     * @param tenants list of company names; {@code null} is treated as empty
     */
    public void setTenants(List<String> tenants) {
        this.tenants = tenants != null ? new ArrayList<>(tenants) : new ArrayList<>();
    }

    public List<Improvement> getImprovements() {
        return Collections.unmodifiableList(improvements);
    }

    public int getImprovementCount() {
        return improvements.size();
    }

    /**
     * Returns the building definition this building was created from.
     * May be null for legacy buildings created without a definition.
     */
    public BuildingDefinition getDefinition() {
        return definition;
    }

    /**
     * Returns the number of floors for this building instance.
     */
    public int getFloors() {
        return floors;
    }

    /**
     * Returns the category ID of this building, or null if no definition.
     */
    public String getCategory() {
        return definition != null ? definition.getCategory() : null;
    }

    /**
     * Whether this building has been discovered by a visiting character.
     *
     * @return true if discovered, false otherwise
     */
    public boolean isDiscovered() {
        return discovered;
    }

    /**
     * Marks this building as discovered. Called when a character visits the cell.
     */
    public void discover() {
        this.discovered = true;
    }

    public boolean isHome() { return home; }
    public void setHome(boolean home) { this.home = home; }

    public boolean isOwned() { return owned; }
    public void setOwned(boolean owned) { this.owned = owned; }

    /**
     * Returns the maintenance/condition state of this building.
     * Values are "good", "normal", or "bad".  May be {@code null} for legacy buildings
     * created without a state assignment.
     */
    public String getState() { return state; }

    /**
     * Sets the maintenance/condition state of this building.
     *
     * @param state "good", "normal", or "bad"; {@code null} is accepted (treated as unknown)
     */
    public void setState(String state) { this.state = state; }

    /**
     * Returns true if this building allows the player to rest (hotels, residential,
     * community centres, libraries).
     */
    public boolean allowsRest() {
        if (definition == null) return false;
        String cat = definition.getCategory();
        String id  = definition.getId();
        return "hospitality".equals(cat) || "residential".equals(cat)
                || "community_center".equals(id) || "library".equals(id);
    }

    /**
     * Returns true if this building allows the player to sleep overnight
     * (hotels and hospitals).
     */
    public boolean allowsSleep() {
        if (definition == null) return false;
        String cat = definition.getCategory();
        String id  = definition.getId();
        return "hospitality".equals(cat)
                || "hospital_small".equals(id) || "hospital_large".equals(id);
    }

    /**
     * Returns true if at least one improvement in this building has not yet been discovered.
     */
    public boolean hasUndiscoveredImprovements() {
        for (Improvement imp : improvements) {
            if (!imp.isDiscovered()) return true;
        }
        return false;
    }

    /**
     * Gets the attribute modifiers for this building.
     * Each entry maps a character attribute to a modifier value from -3 to +3.
     * Only non-zero modifiers are included.
     *
     * @return An unmodifiable map of attribute modifiers
     */
    public Map<CharacterAttribute, Integer> getAttributeModifiers() {
        return attributeModifiers;
    }

    @Override
    public String toString() {
        return "Building{name='" + name + "', floors=" + floors + 
               ", category=" + (definition != null ? definition.getCategory() : "unknown") +
               ", discovered=" + discovered +
               ", improvements=" + improvements +
               ", modifiers=" + attributeModifiers + "}";
    }
}
