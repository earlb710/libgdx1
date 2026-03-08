package eb.framework1.schedule;

import eb.framework1.generator.*;


/** An in-game calendar entry (appointment, case start) created when the player accepts an email. */
public class CalendarEntry {
    public final String dateTime;      // "YYYY-MM-DD HH:MM"
    public final String title;
    public final String location;
    public final int    rewardMoney;   // 0 = no money reward
    public final String rewardItemName; // null = no item reward
    /** Map cell of the appointment location; -1 if unknown. */
    public final int    locationCellX;
    public final int    locationCellY;
    /**
     * Full name of the person the player is meeting (e.g. "Alice Smith").
     * Empty string when the appointment has no specific named contact (e.g. NYPD crime scene).
     * When non-empty, the info-panel shows a "Meet [contactName]" button within 2 hours of the
     * appointment time.
     */
    public final String contactName;

    /**
     * Gender of the contact ({@code "M"} or {@code "F"}).
     * Used by {@link ClientIntroductionGenerator} to tailor the introduction message.
     * Defaults to {@code "M"} when not specified.
     */
    public final String contactGender;

    /** Convenience constructor with no reward and no contact name (backward-compat). */
    CalendarEntry(String dateTime, String title, String location) {
        this(dateTime, title, location, 0, null, -1, -1, "", "M");
    }

    public CalendarEntry(String dateTime, String title, String location,
                  int rewardMoney, String rewardItemName) {
        this(dateTime, title, location, rewardMoney, rewardItemName, -1, -1, "", "M");
    }

    public CalendarEntry(String dateTime, String title, String location,
                  int rewardMoney, String rewardItemName,
                  int locationCellX, int locationCellY) {
        this(dateTime, title, location, rewardMoney, rewardItemName, locationCellX, locationCellY, "", "M");
    }

    public CalendarEntry(String dateTime, String title, String location,
                  int rewardMoney, String rewardItemName,
                  int locationCellX, int locationCellY,
                  String contactName) {
        this(dateTime, title, location, rewardMoney, rewardItemName, locationCellX, locationCellY, contactName, "M");
    }

    public CalendarEntry(String dateTime, String title, String location,
                  int rewardMoney, String rewardItemName,
                  int locationCellX, int locationCellY,
                  String contactName, String contactGender) {
        this.dateTime       = dateTime     != null ? dateTime     : "";
        this.title          = title        != null ? title        : "";
        this.location       = location     != null ? location     : "";
        this.rewardMoney    = Math.max(0, rewardMoney);
        this.rewardItemName = rewardItemName;
        this.locationCellX  = locationCellX;
        this.locationCellY  = locationCellY;
        this.contactName    = contactName  != null ? contactName  : "";
        this.contactGender  = (contactGender != null && !contactGender.trim().isEmpty())
                              ? contactGender.trim().toUpperCase() : "M";
    }

    // -------------------------------------------------------------------------
    // Date / time utilities
    // -------------------------------------------------------------------------

    /**
     * Converts a {@code "YYYY-MM-DD HH:MM"} game date/time string to total
     * minutes using a standard 365-day calendar (no leap years).
     *
     * <p>Returns {@code Long.MAX_VALUE / 2} on malformed input so that a bad
     * date can never accidentally fall within a finite time window check.
     */
    public static long dateTimeToMinutes(String dt) {
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
}
