package eb.framework1.city;

import java.util.Collections;
import java.util.Map;

/**
 * Runtime data for an improvement loaded from {@code text/improvements_en.json}.
 *
 * <p>Holds the human-readable {@code name} (display name), the optional
 * {@code function} (e.g. "rest", "exercise"), the {@code effective} rating
 * (50–100, present only when function is set), an optional {@code restrict}
 * map that gates access to the improvement, and an optional human-readable
 * {@code description}.
 *
 * <p>Supported restrict keys:
 * <ul>
 *   <li>{@code "gender"} – {@code "male"} or {@code "female"}</li>
 *   <li>{@code "strength"} – minimum STRENGTH attribute value (integer)</li>
 * </ul>
 */
public final class ImprovementData {

    /** Human-readable display name of this improvement (e.g. "Security Camera"). */
    public final String name;

    /** Identifies this entry; matches the improvement's code (lower-cased). */
    public final String codeLower;

    /** The function this improvement provides (e.g. "rest", "exercise"); may be empty. */
    public final String function;

    /** Effectiveness rating 50–100; 0 when no function is set. */
    public final int effective;

    /** Human-readable description of this improvement; may be empty. */
    public final String description;

    /**
     * Restriction map.  Keys are lower-case restriction names; values are
     * the required values (Strings or Integers stored as Strings).
     * Empty map means no restrictions.
     */
    private final Map<String, String> restrict;

    public ImprovementData(String codeLower, String name, String function, int effective,
                           Map<String, String> restrict, String description) {
        this.codeLower   = codeLower   != null ? codeLower   : "";
        this.name        = name        != null ? name        : "";
        this.function    = function    != null ? function    : "";
        this.effective   = effective;
        this.description = description != null ? description : "";
        this.restrict    = restrict    != null ? Collections.unmodifiableMap(restrict)
                                               : Collections.emptyMap();
    }

    /** Returns the human-readable description, or an empty string if none. */
    public String getDescription() {
        return description;
    }

    /** Returns an unmodifiable view of the restriction map. */
    public Map<String, String> getRestrict() {
        return restrict;
    }

    /** Returns {@code true} if this improvement has a function. */
    public boolean hasFunction() {
        return !function.isEmpty();
    }

    /** Returns {@code true} if this improvement has any access restrictions. */
    public boolean hasRestrict() {
        return !restrict.isEmpty();
    }

    /**
     * Returns the required gender, or {@code null} if no gender restriction.
     * The value is lower-cased (e.g. {@code "male"}, {@code "female"}).
     */
    public String getRequiredGender() {
        return restrict.get("gender");
    }

    /**
     * Returns the minimum STRENGTH required, or {@code 0} if no strength restriction.
     */
    public int getRequiredStrength() {
        String val = restrict.get("strength");
        if (val == null) return 0;
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return 0; }
    }
}
