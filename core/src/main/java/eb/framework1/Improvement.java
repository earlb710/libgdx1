package eb.framework1;

/**
 * Represents an improvement/upgrade within a building.
 * Improvements start undiscovered and must be found through investigation.
 * The hiddenValue (0-10) indicates how difficult the improvement is to discover:
 * <ul>
 *   <li>0 = no investigation needed, but the location must be visited first</li>
 *   <li>1-10 = increasing difficulty to discover through investigation</li>
 * </ul>
 */
public class Improvement {
    private final String name;
    private final int level;
    private final int hiddenValue;
    private boolean discovered;

    /**
     * Creates a new Improvement with a hidden value.
     * Improvements are not discovered by default.
     *
     * @param name        The improvement name (cannot be null or empty)
     * @param level       The improvement level (must be >= 0)
     * @param hiddenValue How hidden the improvement is (0-10, where 0 = easiest to find)
     */
    public Improvement(String name, int level, int hiddenValue) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Improvement name cannot be null or empty");
        }
        if (level < 0) {
            throw new IllegalArgumentException("Improvement level cannot be negative");
        }
        if (hiddenValue < 0 || hiddenValue > 10) {
            throw new IllegalArgumentException("Hidden value must be between 0 and 10");
        }
        this.name = name.trim();
        this.level = level;
        this.hiddenValue = hiddenValue;
        this.discovered = false;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Gets the hidden value indicating how difficult this improvement is to discover.
     * 0 = no investigation needed (but location must be visited), 10 = hardest to find.
     *
     * @return The hidden value (0-10)
     */
    public int getHiddenValue() {
        return hiddenValue;
    }

    /**
     * Whether this improvement has been discovered.
     *
     * @return true if discovered, false otherwise
     */
    public boolean isDiscovered() {
        return discovered;
    }

    /**
     * Marks this improvement as discovered.
     */
    public void discover() {
        this.discovered = true;
    }

    @Override
    public String toString() {
        return "Improvement{name='" + name + "', level=" + level +
               ", hiddenValue=" + hiddenValue + ", discovered=" + discovered + "}";
    }
}
