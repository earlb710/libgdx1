package eb.framework1;

/**
 * Represents an improvement/upgrade within a building.
 */
public class Improvement {
    private final String name;
    private final int level;

    public Improvement(String name, int level) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Improvement name cannot be null or empty");
        }
        if (level < 0) {
            throw new IllegalArgumentException("Improvement level cannot be negative");
        }
        this.name = name.trim();
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public String toString() {
        return "Improvement{name='" + name + "', level=" + level + "}";
    }
}
