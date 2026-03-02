package eb.gmodel1.generator;

/**
 * Represents the time of day for contextual text descriptions in the novel text engine.
 */
public enum TimeOfDay {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT;

    /**
     * Returns the {@link TimeOfDay} period that corresponds to the given hour (0–23).
     *
     * <ul>
     *   <li>MORNING   : 06:00 – 11:59</li>
     *   <li>AFTERNOON : 12:00 – 17:59</li>
     *   <li>EVENING   : 18:00 – 21:59</li>
     *   <li>NIGHT     : 22:00 – 05:59</li>
     * </ul>
     *
     * @param hour Hour of the day, 0–23
     * @return Matching {@link TimeOfDay}
     */
    public static TimeOfDay fromHour(int hour) {
        if (hour >= 6 && hour < 12) return MORNING;
        if (hour >= 12 && hour < 18) return AFTERNOON;
        if (hour >= 18 && hour < 22) return EVENING;
        return NIGHT;
    }
}
