package eb.framework1.engine;

/**
 * Drives time-consuming in-game actions such as sleeping.
 *
 * <h3>Concept</h3>
 * The engine is started with a game-time window ({@code startTime} –
 * {@code endTime} in 24-hour format), an animation {@code interval} count,
 * a {@code subInterval} count, and a display {@code message}.
 *
 * <ul>
 *   <li>Each <b>interval</b> appends one dot ({@code .}) to the message.
 *       Intervals are spaced {@value #INTERVAL_SEC} seconds apart.</li>
 *   <li>Each interval is divided into <b>sub-intervals</b>.  For every
 *       sub-interval tick the engine fires a callback so that the caller
 *       can advance game time and update the map (NPC movement).</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>
 *   TimeEngine engine = new TimeEngine();
 *   engine.start(22, 6, 8, 3, "Sleeping", minutes -&gt; {
 *       profile.advanceGameTime(minutes);
 *   });
 *
 *   // each frame:
 *   engine.update(delta);
 *   String text = engine.getDisplayText(); // "Sleeping..."
 * </pre>
 */
public class TimeEngine {

    /** Callback fired once per sub-interval tick. */
    public interface SubIntervalCallback {
        /**
         * Called each sub-interval with the number of game minutes to
         * advance during this tick.
         */
        void onTick(int minutesToAdvance);
    }

    /** Seconds between each dot (one interval). */
    static final float INTERVAL_SEC = 0.3f;

    // -- configuration (set by start()) --
    private String message;
    private int    intervals;
    private int    subIntervals;
    private int    totalGameMinutes;
    private int    minutesPerSubTick;
    private int    remainderMinutes;
    private SubIntervalCallback callback;

    // -- runtime state --
    private boolean running;
    private boolean completed;
    private float   timer;
    private int     completedSubTicks;
    private int     totalSubTicks;

    /**
     * Starts (or restarts) the time engine.
     *
     * @param startTime    game start hour (0-23)
     * @param endTime      game end hour (0-23); may wrap past midnight
     * @param intervals    number of dots to display (≥ 1)
     * @param subIntervals number of sub-interval ticks per interval (≥ 1)
     * @param message      text shown during the animation (e.g.&nbsp;"Sleeping")
     * @param callback     invoked once per sub-interval tick, or {@code null}
     */
    public void start(int startTime, int endTime, int intervals,
                      int subIntervals, String message,
                      SubIntervalCallback callback) {
        this.message      = message  != null ? message : "";
        this.intervals    = Math.max(1, intervals);
        this.subIntervals = Math.max(1, subIntervals);
        this.callback     = callback;

        int hourDiff = endTime - startTime;
        if (hourDiff <= 0) hourDiff += 24;
        this.totalGameMinutes = hourDiff * 60;

        this.totalSubTicks      = this.intervals * this.subIntervals;
        this.minutesPerSubTick  = totalGameMinutes / totalSubTicks;
        this.remainderMinutes   = totalGameMinutes % totalSubTicks;

        this.timer             = 0f;
        this.completedSubTicks = 0;
        this.running           = true;
        this.completed         = false;
    }

    /**
     * Convenience helper for movement actions where the total travel time
     * in minutes is already known (e.g.&nbsp;from
     * {@code RouteResult.totalMinutes}).
     *
     * <p>Each <em>step</em> produces one animation dot.  Each step is
     * divided into {@code subIntervals} ticks, and on every tick the
     * {@code callback} receives the number of game minutes to advance.
     *
     * <h3>Example</h3>
     * <pre>
     *   // 35-minute walk, 7 steps, 2 sub-interval ticks per step
     *   engine.startMovement(35, 7, 2, "Traveling", minutes -&gt; {
     *       profile.advanceGameTime(minutes);
     *   });
     * </pre>
     *
     * @param totalMinutes total travel time in game minutes (≥ 1)
     * @param steps        number of walk steps / dots to display (≥ 1)
     * @param subIntervals number of sub-interval ticks per step (≥ 1)
     * @param message      text shown during the animation (e.g.&nbsp;"Traveling")
     * @param callback     invoked once per sub-interval tick, or {@code null}
     */
    public void startMovement(int totalMinutes, int steps, int subIntervals,
                              String message, SubIntervalCallback callback) {
        this.message      = message != null ? message : "";
        this.intervals    = Math.max(1, steps);
        this.subIntervals = Math.max(1, subIntervals);
        this.callback     = callback;

        this.totalGameMinutes = Math.max(1, totalMinutes);

        this.totalSubTicks     = this.intervals * this.subIntervals;
        this.minutesPerSubTick = this.totalGameMinutes / totalSubTicks;
        this.remainderMinutes  = this.totalGameMinutes % totalSubTicks;

        this.timer             = 0f;
        this.completedSubTicks = 0;
        this.running           = true;
        this.completed         = false;
    }

    /**
     * Advances the engine by {@code delta} seconds.
     * Call once per frame from the render loop.
     */
    public void update(float delta) {
        if (!running) return;

        timer += delta;

        float subIntervalSec = INTERVAL_SEC / subIntervals;
        int targetSubTicks = Math.min(
                (int) (timer / subIntervalSec), totalSubTicks);

        while (completedSubTicks < targetSubTicks) {
            int minutes = minutesPerSubTick;
            // Give the last tick any leftover minutes so the total is exact
            if (completedSubTicks == totalSubTicks - 1) {
                minutes += remainderMinutes;
            }
            completedSubTicks++;
            if (callback != null) {
                callback.onTick(minutes);
            }
        }

        if (completedSubTicks >= totalSubTicks) {
            running   = false;
            completed = true;
        }
    }

    // -----------------------------------------------------------------
    // Display helpers
    // -----------------------------------------------------------------

    /**
     * Returns the current display text: the message followed by one dot
     * per completed interval.
     */
    public String getDisplayText() {
        if (message == null) return "";
        int dots = getDotCount();
        StringBuilder sb = new StringBuilder(message);
        for (int i = 0; i < dots; i++) sb.append('.');
        return sb.toString();
    }

    /**
     * Returns how many dots (completed intervals) should be shown.
     */
    public int getDotCount() {
        if (!running && !completed) return 0;
        return Math.min((int) (timer / INTERVAL_SEC), intervals);
    }

    // -----------------------------------------------------------------
    // State queries
    // -----------------------------------------------------------------

    /** {@code true} while the animation is playing. */
    public boolean isRunning()   { return running; }

    /** {@code true} after the animation finishes. */
    public boolean isCompleted() { return completed; }

    /** {@code true} while running <em>or</em> completed (not yet reset). */
    public boolean isVisible()   { return running || completed; }

    /** Returns the configured message. */
    public String getMessage()   { return message; }

    /** Returns the total number of intervals (dots). */
    public int getIntervals()    { return intervals; }

    /** Returns the number of sub-intervals per interval. */
    public int getSubIntervals() { return subIntervals; }

    /** Returns how many sub-interval ticks have fired so far. */
    public int getCompletedSubTicks() { return completedSubTicks; }

    /** Returns the total game minutes spanned by this engine run. */
    public int getTotalGameMinutes()  { return totalGameMinutes; }

    // -----------------------------------------------------------------
    // Control
    // -----------------------------------------------------------------

    /** Resets the engine to idle (invisible, not running). */
    public void reset() {
        running           = false;
        completed         = false;
        timer             = 0f;
        completedSubTicks = 0;
    }

    // package-private for testing
    float getTimer() { return timer; }
}
