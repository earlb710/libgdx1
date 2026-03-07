package eb.framework1.engine;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for {@link TimeEngine}.
 *
 * <p>Validates interval/sub-interval timing, callback invocation, display
 * text generation, midnight wrapping, and state-machine transitions.
 */
public class TimeEngineTest {

    // =====================================================================
    // start() initialisation
    // =====================================================================

    @Test
    public void start_setsRunningState() {
        TimeEngine engine = new TimeEngine();
        engine.start(22, 6, 8, 3, "Sleeping", null);
        assertTrue(engine.isRunning());
        assertFalse(engine.isCompleted());
        assertTrue(engine.isVisible());
    }

    @Test
    public void start_computesTotalGameMinutes_midnightWrap() {
        TimeEngine engine = new TimeEngine();
        // 22:00 → 06:00 = 8 hours = 480 minutes
        engine.start(22, 6, 8, 3, "Sleeping", null);
        assertEquals(480, engine.getTotalGameMinutes());
    }

    @Test
    public void start_computesTotalGameMinutes_sameDay() {
        TimeEngine engine = new TimeEngine();
        // 08:00 → 17:00 = 9 hours = 540 minutes
        engine.start(8, 17, 9, 2, "Working", null);
        assertEquals(540, engine.getTotalGameMinutes());
    }

    @Test
    public void start_computesTotalGameMinutes_fullDay() {
        TimeEngine engine = new TimeEngine();
        // 0:00 → 0:00 next day = 24 hours = 1440 minutes
        engine.start(0, 0, 4, 2, "Waiting", null);
        assertEquals(1440, engine.getTotalGameMinutes());
    }

    @Test
    public void start_clampsIntervalsToAtLeast1() {
        TimeEngine engine = new TimeEngine();
        engine.start(0, 1, 0, 1, "X", null);
        assertEquals(1, engine.getIntervals());
    }

    @Test
    public void start_clampsSubIntervalsToAtLeast1() {
        TimeEngine engine = new TimeEngine();
        engine.start(0, 1, 2, 0, "X", null);
        assertEquals(1, engine.getSubIntervals());
    }

    @Test
    public void start_nullMessage_treatedAsEmpty() {
        TimeEngine engine = new TimeEngine();
        engine.start(0, 1, 1, 1, null, null);
        assertEquals("", engine.getMessage());
    }

    // =====================================================================
    // update() — dot progression
    // =====================================================================

    @Test
    public void dotCount_zeroBefore300ms() {
        TimeEngine engine = new TimeEngine();
        engine.start(0, 1, 3, 1, "Test", null);
        // 200ms < 300ms → 0 dots
        engine.update(0.2f);
        assertEquals(0, engine.getDotCount());
    }

    @Test
    public void dotCount_oneAfter300ms() {
        TimeEngine engine = new TimeEngine();
        engine.start(0, 1, 3, 1, "Test", null);
        engine.update(0.31f);
        assertEquals(1, engine.getDotCount());
    }

    @Test
    public void dotCount_twoAfter600ms() {
        TimeEngine engine = new TimeEngine();
        engine.start(0, 1, 3, 1, "Test", null);
        engine.update(0.61f);
        assertEquals(2, engine.getDotCount());
    }

    @Test
    public void dotCount_neverExceedsIntervals() {
        TimeEngine engine = new TimeEngine();
        engine.start(0, 1, 3, 1, "Test", null);
        engine.update(10f); // way past all intervals
        assertEquals(3, engine.getDotCount());
    }

    // =====================================================================
    // update() — sub-interval callbacks
    // =====================================================================

    @Test
    public void subIntervalCallbacks_fireCorrectNumberOfTimes() {
        final int[] tickCount = {0};
        TimeEngine engine = new TimeEngine();
        // 2 intervals × 3 sub-intervals = 6 total ticks
        engine.start(0, 2, 2, 3, "Test", minutes -> tickCount[0]++);
        engine.update(10f); // complete all
        assertEquals(6, tickCount[0]);
    }

    @Test
    public void subIntervalCallbacks_minutesSumToTotal() {
        final int[] totalMinutes = {0};
        TimeEngine engine = new TimeEngine();
        // 22 → 6 = 480 minutes, 8 intervals × 3 sub = 24 ticks
        engine.start(22, 6, 8, 3, "Sleeping", minutes -> totalMinutes[0] += minutes);
        engine.update(10f);
        assertEquals(480, totalMinutes[0]);
    }

    @Test
    public void subIntervalCallbacks_minutesSumToTotal_withRemainder() {
        final int[] totalMinutes = {0};
        TimeEngine engine = new TimeEngine();
        // 0 → 5 = 300 minutes, 4 intervals × 3 sub = 12 ticks
        // 300 / 12 = 25 per tick, remainder 0 — exact
        engine.start(0, 5, 4, 3, "Test", minutes -> totalMinutes[0] += minutes);
        engine.update(10f);
        assertEquals(300, totalMinutes[0]);
    }

    @Test
    public void subIntervalCallbacks_minutesSumToTotal_nonDivisible() {
        final int[] totalMinutes = {0};
        TimeEngine engine = new TimeEngine();
        // 0 → 7 = 420 minutes, 4 intervals × 2 sub = 8 ticks
        // 420 / 8 = 52 per tick, remainder 4 (added to last tick)
        engine.start(0, 7, 4, 2, "Test", minutes -> totalMinutes[0] += minutes);
        engine.update(10f);
        assertEquals(420, totalMinutes[0]);
    }

    @Test
    public void subIntervalCallbacks_fireIncrementally() {
        final int[] tickCount = {0};
        TimeEngine engine = new TimeEngine();
        // 4 intervals × 2 sub = 8 total ticks
        // subIntervalSec = 0.3 / 2 = 0.15
        engine.start(0, 4, 4, 2, "Test", minutes -> tickCount[0]++);

        engine.update(0.1f);  // < 0.15 → no ticks
        assertEquals(0, tickCount[0]);

        engine.update(0.06f); // total 0.16 → 1 tick
        assertEquals(1, tickCount[0]);

        engine.update(0.14f); // total 0.30 → 2 ticks
        assertEquals(2, tickCount[0]);
    }

    @Test
    public void subIntervalCallbacks_nullCallbackDoesNotThrow() {
        TimeEngine engine = new TimeEngine();
        engine.start(0, 1, 2, 2, "Test", null);
        engine.update(10f); // should not throw
        assertTrue(engine.isCompleted());
    }

    // =====================================================================
    // Completion
    // =====================================================================

    @Test
    public void completion_afterAllSubTicks() {
        TimeEngine engine = new TimeEngine();
        engine.start(0, 1, 2, 1, "Test", null);
        engine.update(10f);
        assertFalse(engine.isRunning());
        assertTrue(engine.isCompleted());
        assertTrue(engine.isVisible());
    }

    @Test
    public void completion_notReachedMidAnimation() {
        TimeEngine engine = new TimeEngine();
        engine.start(0, 1, 4, 1, "Test", null);
        engine.update(0.5f); // only ~1.6 of 4 intervals done
        assertTrue(engine.isRunning());
        assertFalse(engine.isCompleted());
    }

    @Test
    public void completedSubTicks_tracksProgress() {
        TimeEngine engine = new TimeEngine();
        // 3 intervals × 2 sub = 6 total
        // subIntervalSec = 0.3 / 2 = 0.15
        engine.start(0, 3, 3, 2, "Test", null);

        engine.update(0.16f); // 1 sub-tick
        assertEquals(1, engine.getCompletedSubTicks());

        engine.update(0.15f); // total 0.31 → 2 sub-ticks
        assertEquals(2, engine.getCompletedSubTicks());
    }

    // =====================================================================
    // getDisplayText()
    // =====================================================================

    @Test
    public void getDisplayText_beforeStart() {
        TimeEngine engine = new TimeEngine();
        assertEquals("", engine.getDisplayText());
    }

    @Test
    public void getDisplayText_noDots() {
        TimeEngine engine = new TimeEngine();
        engine.start(0, 1, 3, 1, "Sleeping", null);
        engine.update(0.1f); // < 300ms → 0 dots
        assertEquals("Sleeping", engine.getDisplayText());
    }

    @Test
    public void getDisplayText_oneDot() {
        TimeEngine engine = new TimeEngine();
        engine.start(0, 1, 3, 1, "Sleeping", null);
        engine.update(0.31f);
        assertEquals("Sleeping.", engine.getDisplayText());
    }

    @Test
    public void getDisplayText_allDots() {
        TimeEngine engine = new TimeEngine();
        engine.start(0, 1, 3, 1, "Sleeping", null);
        engine.update(10f);
        assertEquals("Sleeping...", engine.getDisplayText());
    }

    // =====================================================================
    // reset()
    // =====================================================================

    @Test
    public void reset_clearsState() {
        TimeEngine engine = new TimeEngine();
        engine.start(0, 1, 2, 1, "Test", null);
        engine.update(10f); // complete
        assertTrue(engine.isCompleted());

        engine.reset();
        assertFalse(engine.isRunning());
        assertFalse(engine.isCompleted());
        assertFalse(engine.isVisible());
        assertEquals(0, engine.getCompletedSubTicks());
    }

    @Test
    public void reset_allowsRestart() {
        final int[] tickCount = {0};
        TimeEngine engine = new TimeEngine();
        engine.start(0, 1, 2, 1, "A", minutes -> tickCount[0]++);
        engine.update(10f);
        assertEquals(2, tickCount[0]);

        engine.reset();
        tickCount[0] = 0;
        engine.start(0, 2, 3, 1, "B", minutes -> tickCount[0]++);
        engine.update(10f);
        assertEquals(3, tickCount[0]);
        assertEquals("B...", engine.getDisplayText());
    }

    // =====================================================================
    // update() is no-op when not running
    // =====================================================================

    @Test
    public void update_noOpWhenIdle() {
        TimeEngine engine = new TimeEngine();
        engine.update(1f); // should not throw
        assertFalse(engine.isRunning());
        assertFalse(engine.isCompleted());
    }

    @Test
    public void update_noOpAfterCompletion() {
        final int[] tickCount = {0};
        TimeEngine engine = new TimeEngine();
        engine.start(0, 1, 1, 1, "X", minutes -> tickCount[0]++);
        engine.update(10f);
        assertEquals(1, tickCount[0]);

        // further updates should not fire more callbacks
        engine.update(10f);
        assertEquals(1, tickCount[0]);
    }

    // =====================================================================
    // startMovement() helper
    // =====================================================================

    @Test
    public void startMovement_setsRunningState() {
        TimeEngine engine = new TimeEngine();
        engine.startMovement(35, 7, 2, "Traveling", null);
        assertTrue(engine.isRunning());
        assertFalse(engine.isCompleted());
        assertTrue(engine.isVisible());
    }

    @Test
    public void startMovement_storesTotalGameMinutes() {
        TimeEngine engine = new TimeEngine();
        engine.startMovement(35, 7, 2, "Traveling", null);
        assertEquals(35, engine.getTotalGameMinutes());
    }

    @Test
    public void startMovement_minutesSumToTotal() {
        final int[] totalMinutes = {0};
        TimeEngine engine = new TimeEngine();
        // 35 min, 7 steps × 2 sub = 14 ticks → 2 min/tick, remainder 7
        engine.startMovement(35, 7, 2, "Traveling", minutes -> totalMinutes[0] += minutes);
        engine.update(10f);
        assertEquals(35, totalMinutes[0]);
    }

    @Test
    public void startMovement_minutesSumToTotal_exactlyDivisible() {
        final int[] totalMinutes = {0};
        TimeEngine engine = new TimeEngine();
        // 60 min, 6 steps × 2 sub = 12 ticks → 5 min/tick, remainder 0
        engine.startMovement(60, 6, 2, "Walking", minutes -> totalMinutes[0] += minutes);
        engine.update(10f);
        assertEquals(60, totalMinutes[0]);
    }

    @Test
    public void startMovement_firesCorrectNumberOfCallbacks() {
        final int[] tickCount = {0};
        TimeEngine engine = new TimeEngine();
        // 5 steps × 3 sub = 15 total ticks
        engine.startMovement(100, 5, 3, "Moving", minutes -> tickCount[0]++);
        engine.update(10f);
        assertEquals(15, tickCount[0]);
    }

    @Test
    public void startMovement_dotProgression() {
        TimeEngine engine = new TimeEngine();
        engine.startMovement(20, 3, 1, "Walking", null);

        engine.update(0.1f); // < 300ms → 0 dots
        assertEquals("Walking", engine.getDisplayText());

        engine.update(0.21f); // total 0.31 → 1 dot
        assertEquals("Walking.", engine.getDisplayText());

        engine.update(10f); // all done
        assertEquals("Walking...", engine.getDisplayText());
    }

    @Test
    public void startMovement_clampsMinutesToAtLeast1() {
        TimeEngine engine = new TimeEngine();
        engine.startMovement(0, 2, 1, "X", null);
        assertEquals(1, engine.getTotalGameMinutes());
    }

    @Test
    public void startMovement_clampsStepsToAtLeast1() {
        TimeEngine engine = new TimeEngine();
        engine.startMovement(10, 0, 1, "X", null);
        assertEquals(1, engine.getIntervals());
    }

    @Test
    public void startMovement_clampsSubIntervalsToAtLeast1() {
        TimeEngine engine = new TimeEngine();
        engine.startMovement(10, 2, 0, "X", null);
        assertEquals(1, engine.getSubIntervals());
    }

    @Test
    public void startMovement_nullMessage_treatedAsEmpty() {
        TimeEngine engine = new TimeEngine();
        engine.startMovement(10, 2, 1, null, null);
        assertEquals("", engine.getMessage());
    }

    @Test
    public void startMovement_completesAndStops() {
        TimeEngine engine = new TimeEngine();
        engine.startMovement(10, 2, 1, "Walk", null);
        engine.update(10f);
        assertFalse(engine.isRunning());
        assertTrue(engine.isCompleted());
    }
}
