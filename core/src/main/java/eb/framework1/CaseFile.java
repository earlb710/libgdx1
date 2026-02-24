package eb.framework1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Represents a case file in the game.  Each case tracks clues gathered by the
 * player during an investigation.  Multiple cases may be open at the same time.
 */
public class CaseFile {

    /** Possible statuses for a case. */
    public enum Status {
        OPEN, CLOSED, COLD
    }

    private final String id;
    private String name;
    private String description;
    private Status status;
    private String dateOpened;
    private String dateClosed;
    private final List<String> clues;

    /**
     * Creates a new open case file with a generated id and no clues.
     *
     * @param name        short name of the case (shown in the poplist)
     * @param description longer summary of the case
     * @param dateOpened  in-game date/time when the case was opened
     */
    public CaseFile(String name, String description, String dateOpened) {
        this(UUID.randomUUID().toString(), name, description, Status.OPEN, dateOpened, null, new ArrayList<>());
    }

    /**
     * Full constructor — used mainly when restoring a case from a save file.
     */
    public CaseFile(String id, String name, String description, Status status,
                    String dateOpened, String dateClosed, List<String> clues) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Case name cannot be null or empty");
        }
        this.id          = id != null ? id : UUID.randomUUID().toString();
        this.name        = name;
        this.description = description != null ? description : "";
        this.status      = status != null ? status : Status.OPEN;
        this.dateOpened   = dateOpened != null ? dateOpened : "";
        this.dateClosed   = dateClosed;
        this.clues       = clues != null ? new ArrayList<>(clues) : new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public String getId()          { return id; }
    public String getName()        { return name; }
    public String getDescription() { return description; }
    public Status getStatus()      { return status; }
    public String getDateOpened()  { return dateOpened; }
    public String getDateClosed()  { return dateClosed; }

    public void setName(String name)               { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setDateOpened(String dateOpened)    { this.dateOpened = dateOpened; }

    /** Returns an unmodifiable view of the clues recorded against this case. */
    public List<String> getClues() {
        return Collections.unmodifiableList(clues);
    }

    // -------------------------------------------------------------------------
    // Mutation helpers
    // -------------------------------------------------------------------------

    /** Adds a clue to this case. */
    public void addClue(String clue) {
        if (clue != null && !clue.trim().isEmpty()) {
            clues.add(clue);
        }
    }

    /** Closes the case and records the closing date. */
    public void close(String dateClosed) {
        this.status = Status.CLOSED;
        this.dateClosed = dateClosed;
    }

    /** Marks the case as cold (inactive but not solved). */
    public void markCold() {
        this.status = Status.COLD;
    }

    /** Re-opens a closed or cold case. */
    public void reopen() {
        this.status = Status.OPEN;
        this.dateClosed = null;
    }

    /** Returns true when the case status is {@link Status#OPEN}. */
    public boolean isOpen() {
        return status == Status.OPEN;
    }

    @Override
    public String toString() {
        return "CaseFile{name='" + name + "', status=" + status + ", clues=" + clues.size() + "}";
    }
}
