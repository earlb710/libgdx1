package eb.framework1.character;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

/**
 * Tests that {@link Profile#advanceGameTime(int)} correctly updates the game
 * date/time and that the resulting {@link Profile#getCurrentHour()} value
 * matches the hour used by {@link NpcCharacter#getCurrentCellX(int)} and
 * {@link NpcCharacter#getCurrentCellY(int)} for NPC map-position look-ups.
 *
 * <p>This validates the contract relied upon by {@code MainScreen.render()}:
 * after advancing time, setting {@code state.currentHour = profile.getCurrentHour()}
 * causes NPC stick figures to be drawn at the correct schedule-derived cell.
 */
public class ProfileTimeTest {

    // =========================================================================
    // Profile.getCurrentHour() reflects the hour embedded in gameDateTime
    // =========================================================================

    @Test
    public void getCurrentHour_initialDateTime_returnsCorrectHour() {
        Profile p = new Profile("Test", "M", "Normal");
        // Default gameDateTime is "2050-01-02 16:00"
        assertEquals(16, p.getCurrentHour());
    }

    @Test
    public void getCurrentHour_afterAdvance60Minutes_incrementsBy1() {
        Profile p = new Profile("Test", "M", "Normal");
        int before = p.getCurrentHour();
        p.advanceGameTime(60);
        assertEquals(before + 1, p.getCurrentHour());
    }

    @Test
    public void getCurrentHour_afterAdvance120Minutes_incrementsBy2() {
        Profile p = new Profile("Test", "M", "Normal");
        int before = p.getCurrentHour();
        p.advanceGameTime(120);
        assertEquals(before + 2, p.getCurrentHour());
    }

    @Test
    public void getCurrentHour_zeroMinutes_doesNotChange() {
        Profile p = new Profile("Test", "M", "Normal");
        int before = p.getCurrentHour();
        p.advanceGameTime(0);
        assertEquals(before, p.getCurrentHour());
    }

    @Test
    public void getCurrentHour_negativeMinutes_doesNotChange() {
        Profile p = new Profile("Test", "M", "Normal");
        int before = p.getCurrentHour();
        p.advanceGameTime(-30);
        assertEquals(before, p.getCurrentHour());
    }

    @Test
    public void getCurrentHour_midnightRollover_wrapsToZero() {
        Profile p = new Profile("Test", "M", "Normal");
        // Advance from 16:00 to 24:00 (= 00:00 next day)
        p.advanceGameTime(480); // +8h → 00:00
        assertEquals(0, p.getCurrentHour());
    }

    @Test
    public void getCurrentHour_multipleAdvances_cumulative() {
        Profile p = new Profile("Test", "M", "Normal");
        p.advanceGameTime(30);
        p.advanceGameTime(30);
        // 16:00 + 60 min = 17:00
        assertEquals(17, p.getCurrentHour());
    }

    // =========================================================================
    // NPC map-position tracks the in-game hour
    // =========================================================================

    /**
     * Verifies that after advancing game time the NPC's cell coordinates
     * returned by {@link NpcCharacter#getCurrentCellX(int)} /
     * {@link NpcCharacter#getCurrentCellY(int)} reflect the new hour —
     * mirroring what {@code MainScreen.render()} does when it sets
     * {@code state.currentHour = profile.getCurrentHour()}.
     */
    @Test
    public void npcCell_changesWhenHourChanges() {
        // Build an NPC with a schedule: 0–8 at home (5,5), 8–16 at work (10,3)
        List<NpcScheduleEntry> entries = Arrays.asList(
                new NpcScheduleEntry(0,  8,  NpcScheduleEntry.HOME, "Home", 5, 5),
                new NpcScheduleEntry(8,  24, NpcScheduleEntry.WORK, "Office", 10, 3)
        );
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("n1").fullName("Alice Smith").gender("F")
                .schedule(new NpcSchedule(entries))
                .build();

        // Simulate: profile starts at 16:00, NPC should be at work
        Profile p = new Profile("Test", "M", "Normal");
        int hour = p.getCurrentHour(); // 16
        assertEquals("NPC should be at work during hour 16",
                10, npc.getCurrentCellX(hour));
        assertEquals("NPC should be at work during hour 16",
                3, npc.getCurrentCellY(hour));

        // Advance past midnight (to hour 1) — NPC should be back home
        p.advanceGameTime(9 * 60); // +9 h → 01:00
        int newHour = p.getCurrentHour();
        assertEquals(1, newHour);
        assertEquals("NPC should be at home at hour 1",
                5, npc.getCurrentCellX(newHour));
        assertEquals("NPC should be at home at hour 1",
                5, npc.getCurrentCellY(newHour));
    }

    @Test
    public void npcCell_unchangedWhenTimeAdvancesWithinSameHourBlock() {
        List<NpcScheduleEntry> entries = Arrays.asList(
                new NpcScheduleEntry(0, 24, NpcScheduleEntry.HOME, "Home", 7, 2)
        );
        NpcCharacter npc = new NpcCharacter.Builder()
                .id("n2").fullName("Bob Jones").gender("M")
                .schedule(new NpcSchedule(entries))
                .build();

        Profile p = new Profile("Test", "M", "Normal");
        // Advance only 30 minutes — still hour 16
        p.advanceGameTime(30);
        int hour = p.getCurrentHour(); // still 16
        assertEquals(7, npc.getCurrentCellX(hour));
        assertEquals(2, npc.getCurrentCellY(hour));
    }
}
