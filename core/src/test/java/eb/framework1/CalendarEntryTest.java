package eb.framework1;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link CalendarEntry}, focused on the {@code contactName} field and
 * the meet-button logic tied to the 2-hour appointment window.
 */
public class CalendarEntryTest {

    // =========================================================================
    // contactName field — constructors
    // =========================================================================

    @Test
    public void shortConstructor_contactNameDefaultsToEmpty() {
        CalendarEntry e = new CalendarEntry("2050-01-02 10:00", "Some Event", "Your Office");
        assertEquals("", e.contactName);
    }

    @Test
    public void rewardConstructor_contactNameDefaultsToEmpty() {
        CalendarEntry e = new CalendarEntry("2050-01-02 10:00", "Some Event", "Your Office",
                200, null);
        assertEquals("", e.contactName);
    }

    @Test
    public void cellConstructor_contactNameDefaultsToEmpty() {
        CalendarEntry e = new CalendarEntry("2050-01-02 10:00", "Some Event", "Downtown Cafe",
                200, null, 5, 7);
        assertEquals("", e.contactName);
    }

    @Test
    public void fullConstructor_contactNameStoredCorrectly() {
        CalendarEntry e = new CalendarEntry("2050-01-02 10:00", "Meeting: Alice Smith",
                "Downtown Cafe", 200, null, 5, 7, "Alice Smith");
        assertEquals("Alice Smith", e.contactName);
    }

    @Test
    public void fullConstructor_nullContactNameBecomesEmpty() {
        CalendarEntry e = new CalendarEntry("2050-01-02 10:00", "NYPD: Crime Scene",
                "Crime Scene (TBD)", 500, null, -1, -1, null);
        assertEquals("", e.contactName);
    }

    // =========================================================================
    // contactName field — other fields still correct
    // =========================================================================

    @Test
    public void fullConstructor_allFieldsStoredCorrectly() {
        CalendarEntry e = new CalendarEntry("2050-03-15 14:30", "Meeting: Bob Jones",
                "Your Office", 300, "Evidence Kit", 2, 4, "Bob Jones");
        assertEquals("2050-03-15 14:30", e.dateTime);
        assertEquals("Meeting: Bob Jones", e.title);
        assertEquals("Your Office", e.location);
        assertEquals(300, e.rewardMoney);
        assertEquals("Evidence Kit", e.rewardItemName);
        assertEquals(2, e.locationCellX);
        assertEquals(4, e.locationCellY);
        assertEquals("Bob Jones", e.contactName);
    }

    @Test
    public void contactName_emptyString_shortConstructorOtherFieldsUnchanged() {
        CalendarEntry e = new CalendarEntry("2050-01-02 08:00", "Title", "Location");
        assertEquals("2050-01-02 08:00", e.dateTime);
        assertEquals("Title", e.title);
        assertEquals("Location", e.location);
        assertEquals(0, e.rewardMoney);
        assertNull(e.rewardItemName);
        assertEquals(-1, e.locationCellX);
        assertEquals(-1, e.locationCellY);
    }

    // =========================================================================
    // Meet-button label logic (mirrors InfoPanelRenderer inline logic)
    // =========================================================================

    /**
     * Replicates the label-selection logic from InfoPanelRenderer so we can
     * test it without requiring a LibGDX runtime.
     */
    private static String resolveButtonLabel(CalendarEntry entry) {
        return entry.contactName.isEmpty()
                ? "Appointment: " + entry.title
                : "Meet " + entry.contactName;
    }

    @Test
    public void buttonLabel_withContactName_showsMeet() {
        CalendarEntry e = new CalendarEntry("2050-01-02 10:00", "Meeting: Alice Smith",
                "Downtown Cafe", 200, null, 5, 7, "Alice Smith");
        assertEquals("Meet Alice Smith", resolveButtonLabel(e));
    }

    @Test
    public void buttonLabel_withoutContactName_showsAppointment() {
        CalendarEntry e = new CalendarEntry("2050-01-02 10:00", "NYPD: Crime Scene",
                "Crime Scene (TBD)", 500, null, -1, -1, "");
        assertEquals("Appointment: NYPD: Crime Scene", resolveButtonLabel(e));
    }

    @Test
    public void buttonLabel_nullContactNameNormalizedToEmpty_showsAppointment() {
        CalendarEntry e = new CalendarEntry("2050-01-02 10:00", "Consulting Request",
                "Crime Scene (TBD)", 500, null, -1, -1, null);
        assertTrue("contactName should be empty when null supplied",
                e.contactName.isEmpty());
        assertEquals("Appointment: Consulting Request", resolveButtonLabel(e));
    }

    // =========================================================================
    // 2-hour window — dateTimeToMinutes mirror (package-access helper)
    // =========================================================================

    /**
     * Mirrors {@code InfoPanelRenderer.dateTimeToMinutes} exactly, so that the
     * 2-hour threshold can be tested without a LibGDX runtime.
     */
    static long dateTimeToMinutes(String dt) {
        final int[] MONTH_DAYS = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        try {
            String[] halves = dt.split(" ");
            String[] d = halves[0].split("-");
            String[] t = halves[1].split(":");
            int year  = Integer.parseInt(d[0]);
            int month = Integer.parseInt(d[1]);
            int day   = Integer.parseInt(d[2]);
            int hour  = Integer.parseInt(t[0]);
            int min   = Integer.parseInt(t[1]);
            long totalDays = (long)(year - 2050) * 365L;
            for (int m = 1; m < month; m++) totalDays += MONTH_DAYS[m - 1];
            totalDays += day;
            return totalDays * 24L * 60L + hour * 60L + min;
        } catch (Exception e) {
            return Long.MAX_VALUE / 2;
        }
    }

    /** Returns true when {@code apptDT} is within 2 hours (120 minutes) of {@code nowDT}. */
    private static boolean isWithinTwoHours(String nowDT, String apptDT) {
        long diff = dateTimeToMinutes(apptDT) - dateTimeToMinutes(nowDT);
        return diff >= 0 && diff <= 120;
    }

    @Test
    public void within2Hours_exactly0MinutesAhead_isTrue() {
        assertTrue(isWithinTwoHours("2050-01-02 10:00", "2050-01-02 10:00"));
    }

    @Test
    public void within2Hours_exactly120MinutesAhead_isTrue() {
        assertTrue(isWithinTwoHours("2050-01-02 10:00", "2050-01-02 12:00"));
    }

    @Test
    public void within2Hours_121MinutesAhead_isFalse() {
        assertFalse(isWithinTwoHours("2050-01-02 10:00", "2050-01-02 12:01"));
    }

    @Test
    public void within2Hours_60MinutesAhead_isTrue() {
        assertTrue(isWithinTwoHours("2050-01-02 10:00", "2050-01-02 11:00"));
    }

    @Test
    public void within2Hours_pastAppointment_isFalse() {
        assertFalse(isWithinTwoHours("2050-01-02 10:00", "2050-01-02 09:59"));
    }

    @Test
    public void within2Hours_180MinutesAhead_nowOutsideWindow_isFalse() {
        // Previously the button showed at 3 h (180 min); now it must NOT show.
        assertFalse("3-hour window should no longer trigger the button",
                isWithinTwoHours("2050-01-02 10:00", "2050-01-02 13:00"));
    }

    @Test
    public void within2Hours_crossingMidnight_handledCorrectly() {
        // 2050-01-02 23:00 + 120 min = 2050-01-03 01:00
        assertTrue(isWithinTwoHours("2050-01-02 23:00", "2050-01-03 01:00"));
    }

    @Test
    public void within2Hours_malformedNow_isFalse() {
        assertFalse(isWithinTwoHours("bad-input", "2050-01-02 10:00"));
    }

    @Test
    public void within2Hours_malformedAppt_isFalse() {
        assertFalse(isWithinTwoHours("2050-01-02 10:00", "bad-input"));
    }
}
