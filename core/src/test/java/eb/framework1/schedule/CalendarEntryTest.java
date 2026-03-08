package eb.framework1.schedule;

import eb.framework1.city.*;
import eb.framework1.ui.*;


import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link CalendarEntry}, focused on the {@code contactName} field and
 * the meet-button logic tied to the 3-hour appointment window.
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
    // contactGender field
    // =========================================================================

    @Test
    public void shortConstructors_contactGenderDefaultsToM() {
        CalendarEntry e = new CalendarEntry("2050-01-02 10:00", "Some Event", "Your Office");
        assertEquals("M", e.contactGender);
    }

    @Test
    public void genderConstructor_femaleGenderStoredCorrectly() {
        CalendarEntry e = new CalendarEntry("2050-01-02 10:00", "Meeting: Alice Smith",
                "Downtown Cafe", 200, null, 5, 7, "Alice Smith", "F");
        assertEquals("F", e.contactGender);
    }

    @Test
    public void genderConstructor_maleGenderStoredCorrectly() {
        CalendarEntry e = new CalendarEntry("2050-01-02 10:00", "Meeting: Bob Jones",
                "Your Office", 300, null, 2, 4, "Bob Jones", "M");
        assertEquals("M", e.contactGender);
    }

    @Test
    public void genderConstructor_lowercaseGenderUppercased() {
        CalendarEntry e = new CalendarEntry("2050-01-02 10:00", "Meeting: Carol",
                "Your Office", 100, null, 1, 1, "Carol", "f");
        assertEquals("F", e.contactGender);
    }

    @Test
    public void genderConstructor_nullGenderDefaultsToM() {
        CalendarEntry e = new CalendarEntry("2050-01-02 10:00", "NYPD: Crime Scene",
                "Crime Scene", 500, null, -1, -1, null, null);
        assertEquals("M", e.contactGender);
    }

    @Test
    public void genderConstructor_blankGenderDefaultsToM() {
        CalendarEntry e = new CalendarEntry("2050-01-02 10:00", "Meeting: Dave",
                "Your Office", 200, null, 3, 3, "Dave", "  ");
        assertEquals("M", e.contactGender);
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
    // =========================================================================
    // 3-hour window — dateTimeToMinutes (now a shared static on CalendarEntry)
    // =========================================================================

    /** Returns true when {@code apptDT} is within 3 hours (180 minutes) of {@code nowDT}. */
    private static boolean isWithinThreeHours(String nowDT, String apptDT) {
        long diff = CalendarEntry.dateTimeToMinutes(apptDT) - CalendarEntry.dateTimeToMinutes(nowDT);
        return diff >= 0 && diff <= 180;
    }

    @Test
    public void within3Hours_exactly0MinutesAhead_isTrue() {
        assertTrue(isWithinThreeHours("2050-01-02 10:00", "2050-01-02 10:00"));
    }

    @Test
    public void within3Hours_exactly180MinutesAhead_isTrue() {
        assertTrue(isWithinThreeHours("2050-01-02 10:00", "2050-01-02 13:00"));
    }

    @Test
    public void within3Hours_181MinutesAhead_isFalse() {
        assertFalse(isWithinThreeHours("2050-01-02 10:00", "2050-01-02 13:01"));
    }

    @Test
    public void within3Hours_60MinutesAhead_isTrue() {
        assertTrue(isWithinThreeHours("2050-01-02 10:00", "2050-01-02 11:00"));
    }

    @Test
    public void within3Hours_120MinutesAhead_isTrue() {
        assertTrue(isWithinThreeHours("2050-01-02 10:00", "2050-01-02 12:00"));
    }

    @Test
    public void within3Hours_pastAppointment_isFalse() {
        assertFalse(isWithinThreeHours("2050-01-02 10:00", "2050-01-02 09:59"));
    }

    @Test
    public void within3Hours_crossingMidnight_handledCorrectly() {
        // 2050-01-02 23:00 + 180 min = 2050-01-03 02:00
        assertTrue(isWithinThreeHours("2050-01-02 23:00", "2050-01-03 02:00"));
    }

    @Test
    public void within3Hours_malformedNow_isFalse() {
        assertFalse(isWithinThreeHours("bad-input", "2050-01-02 10:00"));
    }

    @Test
    public void within3Hours_malformedAppt_isFalse() {
        assertFalse(isWithinThreeHours("2050-01-02 10:00", "bad-input"));
    }

    // =========================================================================
    // Cell-coordinate location matching (mirrors findUpcomingAppointmentAtLocation)
    // =========================================================================

    /**
     * Mirrors the location-match branch added to
     * {@code InfoPanelRenderer.findUpcomingAppointmentAtLocation}: when
     * {@code locationCellX >= 0} the entry is matched by cell coordinates, not
     * by the {@code location} string.
     *
     * @param charX          character's current map cell X
     * @param charY          character's current map cell Y
     * @param homeCellX      player home/office cell X
     * @param homeCellY      player home/office cell Y
     * @param entryLocation  {@link CalendarEntry#location} string (e.g. "Your Office")
     * @param entryLocCellX  {@link CalendarEntry#locationCellX} (-1 if unknown)
     * @param entryLocCellY  {@link CalendarEntry#locationCellY} (-1 if unknown)
     */
    private static boolean locationMatchesByCell(
            int charX, int charY,
            int homeCellX, int homeCellY,
            String entryLocation, int entryLocCellX, int entryLocCellY) {
        if ("Your Office".equalsIgnoreCase(entryLocation)) {
            return charX == homeCellX && charY == homeCellY;
        } else if (entryLocCellX >= 0 && entryLocCellY >= 0) {
            return charX == entryLocCellX && charY == entryLocCellY;
        } else {
            // name-based fallback — not tested here
            return false;
        }
    }

    @Test
    public void cellMatch_charAtExactCell_isTrue() {
        assertTrue(locationMatchesByCell(3, 5, 0, 0, "Downtown Cafe", 3, 5));
    }

    @Test
    public void cellMatch_charAtDifferentCell_isFalse() {
        assertFalse(locationMatchesByCell(4, 5, 0, 0, "Downtown Cafe", 3, 5));
    }

    @Test
    public void cellMatch_noCoordsFallsThrough_isFalse() {
        // locationCellX/Y == -1 → falls to name branch, which returns false in this helper
        assertFalse(locationMatchesByCell(3, 5, 0, 0, "Some Building", -1, -1));
    }

    @Test
    public void cellMatch_yourOfficeAtHome_isTrue() {
        assertTrue(locationMatchesByCell(2, 2, 2, 2, "Your Office", -1, -1));
    }

    @Test
    public void cellMatch_yourOfficeNotAtHome_isFalse() {
        assertFalse(locationMatchesByCell(3, 3, 2, 2, "Your Office", -1, -1));
    }

    // =========================================================================
    // CalendarEntry.dateTimeToMinutes — canonical shared implementation
    // =========================================================================

    @Test
    public void dateTimeToMinutes_firstDay_is1440() {
        // The calendar starts at year 2050 with 0 total days elapsed.
        // Day 1 of month 1 contributes 1 day = 1440 minutes at midnight.
        assertEquals(1440L, CalendarEntry.dateTimeToMinutes("2050-01-01 00:00"));
    }

    @Test
    public void dateTimeToMinutes_returnsPositiveValue() {
        // Confirm the shared method returns a sensible positive number for any valid date
        long v = CalendarEntry.dateTimeToMinutes("2050-06-15 08:30");
        assertTrue("Should return a large positive number for 2050-06-15", v > 0);
    }

    @Test
    public void dateTimeToMinutes_malformedInput_returnsSafeMaxValue() {
        long v = CalendarEntry.dateTimeToMinutes("not-a-date");
        assertEquals(Long.MAX_VALUE / 2, v);
    }

    @Test
    public void dateTimeToMinutes_twoConsecutiveMinutes_differ_by1() {
        long a = CalendarEntry.dateTimeToMinutes("2050-03-10 14:00");
        long b = CalendarEntry.dateTimeToMinutes("2050-03-10 14:01");
        assertEquals(1L, b - a);
    }

    @Test
    public void dateTimeToMinutes_twoConsecutiveDays_differ_by1440() {
        long a = CalendarEntry.dateTimeToMinutes("2050-04-20 12:00");
        long b = CalendarEntry.dateTimeToMinutes("2050-04-21 12:00");
        assertEquals(1440L, b - a);
    }
}
