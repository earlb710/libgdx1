package eb.framework1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a building in a cell with improvements.
 */
public class Building {
    private static final int IMPROVEMENT_COUNT = 4;
    
    private final String name;
    private final List<Improvement> improvements;
    private final BuildingDefinition definition;
    private final int floors;

    /**
     * Creates a building with just a name and improvements (legacy constructor).
     */
    public Building(String name, List<Improvement> improvements) {
        this(name, improvements, null, 1);
    }

    /**
     * Creates a building with a definition, floors, and improvements.
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

    @Override
    public String toString() {
        return "Building{name='" + name + "', floors=" + floors + 
               ", category=" + (definition != null ? definition.getCategory() : "unknown") +
               ", improvements=" + improvements + "}";
    }
}
