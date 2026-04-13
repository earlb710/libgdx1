package eb.framework1.investigation;

/**
 * Represents a named in-game location where an NPC can be met during an
 * investigation.
 *
 * <p>Each NPC has a <em>default location</em> — the place where they can
 * usually be found — and may agree to meet the player at a <em>meeting
 * location</em> when contacted by phone.  The player must first discover
 * the NPC's phone number (through interviews, leads, or investigation) before
 * they can arrange an appointment.
 *
 * <p>Location codes are drawn from a fixed pool of recognisable urban and
 * suburban places: cafés, offices, parks, bars, libraries, etc.  Each code
 * has a human-readable display name.
 *
 * @see CaseFile
 * @see CaseGenerator
 */
public final class NpcLocation {

    /** Predefined location codes. */
    public enum LocationCode {
        CAFE("Café"),
        BAR("Bar"),
        OFFICE("Office"),
        PARK("Public Park"),
        LIBRARY("Library"),
        RESTAURANT("Restaurant"),
        HOME("Their Home"),
        GYM("Gym"),
        WAREHOUSE("Warehouse District"),
        CHURCH("Church"),
        HOSPITAL("Hospital"),
        POLICE_STATION("Police Station"),
        DINER("Diner"),
        HOTEL_LOBBY("Hotel Lobby"),
        PARKING_GARAGE("Parking Garage"),
        BUS_STATION("Bus Station"),
        MARKET("Street Market"),
        COURTHOUSE("Courthouse");

        private final String displayName;

        LocationCode(String displayName) {
            this.displayName = displayName;
        }

        /** Returns the human-readable name shown in the UI. */
        public String getDisplayName() { return displayName; }
    }

    private final LocationCode code;
    private final String description;

    /**
     * Creates a location with a code and optional flavour description.
     *
     * @param code        the location code; must not be {@code null}
     * @param description a short flavour line, e.g. "corner table by the window";
     *                    may be empty but not {@code null}
     */
    public NpcLocation(LocationCode code, String description) {
        if (code == null) throw new IllegalArgumentException("LocationCode must not be null");
        this.code = code;
        this.description = description != null ? description : "";
    }

    /** Returns the location code. */
    public LocationCode getCode() { return code; }

    /** Returns the display name of this location. */
    public String getDisplayName() { return code.getDisplayName(); }

    /** Returns the optional flavour description. */
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return description.isEmpty()
                ? code.getDisplayName()
                : code.getDisplayName() + " — " + description;
    }
}
