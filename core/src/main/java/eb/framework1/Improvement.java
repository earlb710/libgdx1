package eb.framework1;

import java.util.Collections;
import java.util.Map;

/**
 * Represents an improvement/upgrade within a building.
 * Improvements start undiscovered and must be found through investigation.
 * The hiddenValue (0-5) indicates how difficult the improvement is to discover:
 * <ul>
 *   <li>0 = no investigation needed, but the location must be visited first</li>
 *   <li>1-5 = increasing difficulty; a Perception score of 5 finds all improvements</li>
 * </ul>
 *
 * Each improvement may also affect character attributes (from -3 to +3),
 * determined by the improvement's name via {@link ImprovementEffects}.
 */
public class Improvement {
    private final String name;
    private final int level;
    private final int hiddenValue;
    private final Map<CharacterAttribute, Integer> attributeModifiers;
    private boolean discovered;

    /**
     * Creates a new Improvement with a hidden value.
     * Improvements are not discovered by default.
     * Attribute modifiers are automatically determined from the improvement name.
     *
     * @param name        The improvement name (cannot be null or empty)
     * @param level       The improvement level (must be >= 0)
     * @param hiddenValue How hidden the improvement is (0-5, where 0 = found on arrival)
     */
    public Improvement(String name, int level, int hiddenValue) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Improvement name cannot be null or empty");
        }
        if (level < 0) {
            throw new IllegalArgumentException("Improvement level cannot be negative");
        }
        if (hiddenValue < 0 || hiddenValue > 5) {
            throw new IllegalArgumentException("Hidden value must be between 0 and 5");
        }
        this.name = name.trim();
        this.level = level;
        this.hiddenValue = hiddenValue;
        this.discovered = false;
        this.attributeModifiers = ImprovementEffects.getEffects(this.name);
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

    /**
     * Gets the attribute modifiers for this improvement.
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
        return "Improvement{name='" + name + "', level=" + level +
               ", hiddenValue=" + hiddenValue + ", discovered=" + discovered +
               ", modifiers=" + attributeModifiers + "}";
    }
}
