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

    public Building(String name, List<Improvement> improvements) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Building name cannot be null or empty");
        }
        if (improvements == null || improvements.size() != IMPROVEMENT_COUNT) {
            throw new IllegalArgumentException("Building must have exactly " + IMPROVEMENT_COUNT + " improvements");
        }
        this.name = name.trim();
        this.improvements = new ArrayList<>(improvements);
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

    @Override
    public String toString() {
        return "Building{name='" + name + "', improvements=" + improvements + "}";
    }
}
