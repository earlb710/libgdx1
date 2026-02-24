package eb.framework1;

/** An in-game calendar entry (appointment, case start) created when the player accepts an email. */
class CalendarEntry {
    final String dateTime;      // "YYYY-MM-DD HH:MM"
    final String title;
    final String location;
    final int    rewardMoney;   // 0 = no money reward
    final String rewardItemName; // null = no item reward
    /** Map cell of the appointment location; -1 if unknown. */
    final int    locationCellX;
    final int    locationCellY;

    /** Convenience constructor with no reward (backward-compat). */
    CalendarEntry(String dateTime, String title, String location) {
        this(dateTime, title, location, 0, null, -1, -1);
    }

    CalendarEntry(String dateTime, String title, String location,
                  int rewardMoney, String rewardItemName) {
        this(dateTime, title, location, rewardMoney, rewardItemName, -1, -1);
    }

    CalendarEntry(String dateTime, String title, String location,
                  int rewardMoney, String rewardItemName,
                  int locationCellX, int locationCellY) {
        this.dateTime       = dateTime != null ? dateTime : "";
        this.title          = title    != null ? title    : "";
        this.location       = location != null ? location : "";
        this.rewardMoney    = Math.max(0, rewardMoney);
        this.rewardItemName = rewardItemName;
        this.locationCellX  = locationCellX;
        this.locationCellY  = locationCellY;
    }
}
