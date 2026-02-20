package eb.framework1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a building in a cell with improvements.
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
    }

    public String getName() {
        return name;
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
               ", improvements=" + improvements +
               ", modifiers=" + attributeModifiers + "}";
    }
}
