package eb.framework1.character;

/**
 * A single time-slot entry in an NPC's daily schedule.
 *
 * <p>An entry covers the hours [{@link #startHour}, {@link #endHour}) on a
 * 24-hour clock.  The NPC is considered to be at {@link #locationName} /
 * ({@link #cellX}, {@link #cellY}) for the duration of that window.
 *
 * <p>Instances are immutable; create them directly with one of the constructors.
 *
 * <h3>Activity type constants</h3>
 * Use {@link #SLEEP}, {@link #HOME}, {@link #WORK}, {@link #LEISURE}, and
 * {@link #SHOPPING} as values for {@link #activityType}.
 */
public final class NpcScheduleEntry {

    // -------------------------------------------------------------------------
    // Activity type constants
    // -------------------------------------------------------------------------

    /** Sleeping at home (typically 22:00–06:00). */
    public static final String SLEEP    = "SLEEP";
    /** Morning/evening home routine (awake but not working or out). */
    public static final String HOME     = "HOME";
    /** At the NPC's designated workplace. */
    public static final String WORK     = "WORK";
    /** Visiting a leisure venue (gym, cinema, bowling alley, etc.). */
    public static final String LEISURE  = "LEISURE";
    /** Grocery or retail shopping. */
    public static final String SHOPPING = "SHOPPING";

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /** Start of this time slot (inclusive), 0–23. */
    public final int    startHour;

    /**
     * End of this time slot (exclusive), 1–24.
     * Use {@code 24} to represent the end of the day (midnight).
     */
    public final int    endHour;

    /**
     * Type of activity.  Should be one of {@link #SLEEP}, {@link #HOME},
     * {@link #WORK}, {@link #LEISURE}, or {@link #SHOPPING}.
     */
    public final String activityType;

    /**
     * Human-readable display name of the location (building display name or
     * address string).  May be empty if the location is unknown.
     */
    public final String locationName;

    /**
     * City-map cell X-coordinate of the location, or {@code -1} if unknown.
     */
    public final int    cellX;

    /**
     * City-map cell Y-coordinate of the location, or {@code -1} if unknown.
     */
    public final int    cellY;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Creates a fully specified schedule entry with map coordinates.
     *
     * @param startHour    start hour, 0–23
     * @param endHour      end hour (exclusive), 1–24
     * @param activityType activity type (e.g. {@link #WORK}); {@code null} defaults to {@link #HOME}
     * @param locationName display name of the location; {@code null} becomes {@code ""}
     * @param cellX        map cell X coordinate, or {@code -1} if unknown
     * @param cellY        map cell Y coordinate, or {@code -1} if unknown
     */
    public NpcScheduleEntry(int startHour, int endHour, String activityType,
                            String locationName, int cellX, int cellY) {
        this.startHour    = startHour;
        this.endHour      = endHour;
        this.activityType = activityType != null ? activityType : HOME;
        this.locationName = locationName != null ? locationName : "";
        this.cellX        = cellX;
        this.cellY        = cellY;
    }

    /**
     * Creates a schedule entry with unknown map coordinates.
     * {@link #cellX} and {@link #cellY} will both be {@code -1}.
     *
     * @param startHour    start hour, 0–23
     * @param endHour      end hour (exclusive), 1–24
     * @param activityType activity type (e.g. {@link #WORK})
     * @param locationName display name of the location; {@code null} becomes {@code ""}
     */
    public NpcScheduleEntry(int startHour, int endHour, String activityType,
                            String locationName) {
        this(startHour, endHour, activityType, locationName, -1, -1);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the given hour falls within this entry's time
     * window [{@link #startHour}, {@link #endHour}).
     *
     * @param hour a 24-hour value (0–23)
     * @return {@code true} if this entry is in effect at {@code hour}
     */
    public boolean coversHour(int hour) {
        return hour >= startHour && hour < endHour;
    }

    /**
     * Returns {@code true} if this entry has valid (non-negative) map coordinates.
     */
    public boolean hasKnownCell() {
        return cellX >= 0 && cellY >= 0;
    }

    @Override
    public String toString() {
        return "NpcScheduleEntry{" + startHour + "-" + endHour
                + " " + activityType
                + " @ " + locationName
                + " (" + cellX + "," + cellY + ")}";
    }
}
