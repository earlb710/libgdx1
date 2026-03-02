package eb.gmodel1.schedule;

import eb.gmodel1.generator.*;


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
}
