package eb.framework1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Represents a case file in the game.  Each case tracks clues and evidence
 * gathered by the player during an investigation.  Multiple cases may be open
 * at the same time.
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
    private final List<String>       clues;
    private final List<String>       evidence;
    private final List<String>       notes;
    private final List<EvidenceItem> evidenceItems;

    /**
     * Creates a new open case file with a generated id and no clues or evidence.
     *
     * @param name        short name of the case (shown in the poplist)
     * @param description longer summary of the case
     * @param dateOpened  in-game date/time when the case was opened
     */
    public CaseFile(String name, String description, String dateOpened) {
        this(UUID.randomUUID().toString(), name, description, Status.OPEN, dateOpened, null,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Full constructor — used mainly when restoring a case from a save file.
     */
    public CaseFile(String id, String name, String description, Status status,
                    String dateOpened, String dateClosed, List<String> clues,
                    List<String> evidence, List<String> notes) {
        this(id, name, description, status, dateOpened, dateClosed, clues, evidence, notes,
                new ArrayList<>());
    }

    /**
     * Full constructor including physical evidence items — used mainly when
     * restoring a case from a save file.
     */
    public CaseFile(String id, String name, String description, Status status,
                    String dateOpened, String dateClosed, List<String> clues,
                    List<String> evidence, List<String> notes,
                    List<EvidenceItem> evidenceItems) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Case name cannot be null or empty");
        }
        this.id          = id != null ? id : UUID.randomUUID().toString();
        this.name        = name;
        this.description = description != null ? description : "";
        this.status      = status != null ? status : Status.OPEN;
        this.dateOpened   = dateOpened != null ? dateOpened : "";
        this.dateClosed   = dateClosed;
        this.clues         = clues != null ? new ArrayList<>(clues) : new ArrayList<>();
        this.evidence      = evidence != null ? new ArrayList<>(evidence) : new ArrayList<>();
        this.notes         = notes != null ? new ArrayList<>(notes) : new ArrayList<>();
        this.evidenceItems = evidenceItems != null ? new ArrayList<>(evidenceItems) : new ArrayList<>();
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

    /** Returns an unmodifiable view of the evidence recorded against this case. */
    public List<String> getEvidence() {
        return Collections.unmodifiableList(evidence);
    }

    /** Returns an unmodifiable view of the player notes recorded against this case. */
    public List<String> getNotes() {
        return Collections.unmodifiableList(notes);
    }

    /**
     * Returns an unmodifiable view of the physical {@link EvidenceItem}s
     * collected for this case.
     */
    public List<EvidenceItem> getEvidenceItems() {
        return Collections.unmodifiableList(evidenceItems);
    }

    /**
     * Returns an unmodifiable list of physical evidence items that have at
     * least one analysis submitted.
     */
    public List<EvidenceItem> getSubmittedEvidenceItems() {
        return evidenceItems.stream()
                .filter(item -> !item.getSubmittedModifiers().isEmpty())
                .collect(Collectors.toList());
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

    /** Adds a piece of evidence to this case. */
    public void addEvidence(String item) {
        if (item != null && !item.trim().isEmpty()) {
            evidence.add(item);
        }
    }

    /**
     * Adds a physical {@link EvidenceItem} to this case.
     *
     * @param item the item to add; must not be {@code null}
     * @throws IllegalArgumentException if {@code item} is {@code null}
     */
    public void addEvidenceItem(EvidenceItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Evidence item must not be null");
        }
        evidenceItems.add(item);
    }

    /** Adds a player note to this case. */
    public void addNote(String note) {
        if (note != null && !note.trim().isEmpty()) {
            notes.add(note);
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
        return "CaseFile{name='" + name + "', status=" + status + ", clues=" + clues.size() + ", evidence=" + evidence.size() + ", notes=" + notes.size() + "}";
    }
}
