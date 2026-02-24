package eb.framework1;

/** An in-game calendar entry (appointment, case start) created when the player accepts an email. */
class CalendarEntry {
    final String dateTime;   // "YYYY-MM-DD HH:MM"
    final String title;
    final String location;

    CalendarEntry(String dateTime, String title, String location) {
        this.dateTime = dateTime != null ? dateTime : "";
        this.title    = title    != null ? title    : "";
        this.location = location != null ? location : "";
    }
}
