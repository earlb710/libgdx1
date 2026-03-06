package eb.framework1.character;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The daily schedule of a non-player character — an ordered list of
 * {@link NpcScheduleEntry} objects covering a full 24-hour day.
 *
 * <p>Entries are sorted ascending by {@link NpcScheduleEntry#startHour}.
 * Use {@link #getEntryForHour(int)} to determine where an NPC is at a
 * particular time of day.
 *
 * <p>Instances are immutable once constructed.
 *
 * <h3>Example</h3>
 * <pre>
 *   NpcCharacter npc = npcGenerator.generateClient(CaseType.THEFT, cityMap);
 *   NpcSchedule schedule = npc.getSchedule();
 *
 *   NpcScheduleEntry at14 = schedule.getEntryForHour(14);  // 2 PM
 *   System.out.println(at14.activityType);   // e.g. "WORK"
 *   System.out.println(at14.locationName);   // e.g. "City Mall"
 *   int x = at14.cellX;                      // map X coordinate (or -1)
 * </pre>
 */
public final class NpcSchedule {

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /** Schedule entries sorted by start hour (ascending). */
    private final List<NpcScheduleEntry> entries;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a schedule from the given list of entries.
     * The list is defensively copied and sorted by
     * {@link NpcScheduleEntry#startHour}.  {@code null} or empty lists are
     * accepted and produce an empty schedule.
     *
     * @param entries entries to include; may be {@code null} or empty
     */
    public NpcSchedule(List<NpcScheduleEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            this.entries = Collections.emptyList();
        } else {
            List<NpcScheduleEntry> copy = new ArrayList<>(entries);
            Collections.sort(copy, new Comparator<NpcScheduleEntry>() {
                @Override
                public int compare(NpcScheduleEntry a, NpcScheduleEntry b) {
                    return Integer.compare(a.startHour, b.startHour);
                }
            });
            this.entries = Collections.unmodifiableList(copy);
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns an unmodifiable view of all schedule entries, sorted ascending
     * by {@link NpcScheduleEntry#startHour}.
     */
    public List<NpcScheduleEntry> getEntries() {
        return entries;
    }

    /**
     * Returns the schedule entry whose time window covers {@code hour}, or
     * {@code null} if no entry covers that hour.
     *
     * <p>When multiple entries cover the same hour (overlapping windows), the
     * one with the latest {@link NpcScheduleEntry#startHour} is returned.
     *
     * @param hour hour of the day (0–23)
     * @return the entry in effect at {@code hour}, or {@code null}
     */
    public NpcScheduleEntry getEntryForHour(int hour) {
        NpcScheduleEntry best = null;
        for (NpcScheduleEntry entry : entries) {
            if (entry.coversHour(hour)) {
                best = entry; // last matching entry wins (latest start)
            }
        }
        return best;
    }

    /**
     * Returns the number of entries in this schedule.
     */
    public int size() {
        return entries.size();
    }

    @Override
    public String toString() {
        return "NpcSchedule{entries=" + entries + "}";
    }
}
