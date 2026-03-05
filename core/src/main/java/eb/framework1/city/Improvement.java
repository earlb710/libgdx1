package eb.framework1.city;

import eb.framework1.character.*;


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
 *
 * <p>Runtime function/effective/restrict data is supplied via {@link ImprovementData}
 * (loaded from {@code text/improvements_en.json}).
 */
public class Improvement {
    private final String name;
    private final int level;
    private final int hiddenValue;
    private final int quality;
    private final Map<CharacterAttribute, Integer> attributeModifiers;
    private boolean discovered;

    // --- Optional runtime data from improvements_en.json ---
    private final String function;
    private final int effective;
    private final ImprovementData data;

    /**
     * Creates a new Improvement with a hidden value and optional runtime data.
     * Improvements are not discovered by default.
     * Attribute modifiers are automatically determined from the improvement name.
     *
     * @param name        The improvement name (cannot be null or empty)
     * @param level       The improvement level (must be >= 0)
     * @param hiddenValue How hidden the improvement is (0-5, where 0 = found on arrival)
     * @param data        Optional runtime data; may be {@code null}
     */
    public Improvement(String name, int level, int hiddenValue, ImprovementData data) {
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
        this.quality = ImprovementEffects.getQuality(this.name);
        this.attributeModifiers = ImprovementEffects.getEffects(this.name);
        this.data      = data;
        this.function  = (data != null) ? data.function  : "";
        this.effective = (data != null) ? data.effective : 0;
    }

    /**
     * Creates a new Improvement without runtime data (legacy / test constructor).
     */
    public Improvement(String name, int level, int hiddenValue) {
        this(name, level, hiddenValue, null);
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
     * Gets the quality/cost rating of this improvement on a scale of 1 (basic) to 10 (ultra-luxury).
     *
     * @return quality rating in the range [1, 10]
     */
    public int getQuality() {
        return quality;
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

    /**
     * Returns the function of this improvement (e.g. "rest", "exercise"), or an
     * empty string if this improvement has no function.
     */
    public String getFunction() {
        return function;
    }

    /**
     * Returns {@code true} if this improvement has a function.
     */
    public boolean hasFunction() {
        return !function.isEmpty();
    }

    /**
     * Returns the effectiveness rating (50–100) for this improvement's function,
     * or {@code 0} if no function is set.
     */
    public int getEffective() {
        return effective;
    }

    /**
     * Returns the {@link ImprovementData} backing this improvement, or {@code null}
     * if no data was provided (legacy construction).
     */
    public ImprovementData getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Improvement{name='" + name + "', level=" + level +
               ", hiddenValue=" + hiddenValue + ", quality=" + quality +
               ", discovered=" + discovered +
               ", modifiers=" + attributeModifiers +
               (function.isEmpty() ? "" : ", function=" + function + ", effective=" + effective) +
               "}";
    }
}

