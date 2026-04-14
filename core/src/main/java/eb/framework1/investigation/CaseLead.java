package eb.framework1.investigation;

/**
 * A single hidden piece of information that the player must uncover during an
 * investigation.
 *
 * <p>Each lead has a secret {@link #getDescription() description} (what is
 * actually hidden), a vague {@link #getHint() hint} that the player can read
 * before they have solved it, and a {@link DiscoveryMethod} indicating which
 * in-game activity will reveal it.
 *
 * <p>Once the player completes the necessary activity the lead should be
 * marked as discovered via {@link #discover()}.  Discovered leads can be read
 * in full.
 *
 * <h3>Example</h3>
 * <pre>
 *   CaseLead lead = new CaseLead(
 *       "lead-1",
 *       "The subject met a contact at the Blue Moon bar every Thursday night",
 *       "The subject has a regular out-of-office meeting",
 *       DiscoveryMethod.SURVEILLANCE);
 *
 *   // Player performs surveillance…
 *   lead.discover();
 *   System.out.println(lead.getDescription()); // full detail now readable
 * </pre>
 */
public class CaseLead {

    private final String          id;
    private final String          description;   // full secret (hidden until discovered)
    private final String          hint;          // vague hint always visible to player
    private final DiscoveryMethod discoveryMethod;
    private boolean               discovered;
    /**
     * The in-game day by which this lead must be discovered, or {@code 0} if
     * the lead never expires.  A non-zero value means the lead becomes stale
     * and unrecoverable after this day — creating time pressure for the player.
     */
    private int                   expirationDay;

    /**
     * Creates a new, undiscovered lead.
     *
     * @param id              unique identifier within its {@link CaseFile}
     * @param description     the hidden fact (revealed to the player on discovery)
     * @param hint            a vague hint the player can read before discovery;
     *                        must not be {@code null} or blank
     * @param discoveryMethod how the player can uncover this lead
     * @throws IllegalArgumentException if any required argument is {@code null}
     *         or blank
     */
    public CaseLead(String id, String description, String hint,
                    DiscoveryMethod discoveryMethod) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Lead id must not be blank");
        }
        if (hint == null || hint.trim().isEmpty()) {
            throw new IllegalArgumentException("Lead hint must not be blank");
        }
        if (discoveryMethod == null) {
            throw new IllegalArgumentException("DiscoveryMethod must not be null");
        }
        this.id              = id.trim();
        this.description     = description != null ? description : "";
        this.hint            = hint.trim();
        this.discoveryMethod = discoveryMethod;
        this.discovered      = false;
        this.expirationDay   = 0;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** Unique identifier for this lead within its case. */
    public String getId() { return id; }

    /**
     * Returns the full hidden fact.
     *
     * <p>This is the secret the player is trying to uncover.  In the UI it
     * should only be shown in full once {@link #isDiscovered()} returns
     * {@code true}.
     */
    public String getDescription() { return description; }

    /**
     * Returns a vague, always-visible hint about what kind of information is
     * hidden here.  Safe to show to the player at any time.
     */
    public String getHint() { return hint; }

    /** The in-game activity the player must perform to reveal this lead. */
    public DiscoveryMethod getDiscoveryMethod() { return discoveryMethod; }

    /** Returns {@code true} once this lead has been uncovered by the player. */
    public boolean isDiscovered() { return discovered; }

    /**
     * Returns the in-game day by which this lead must be discovered, or
     * {@code 0} if the lead never expires.
     */
    public int getExpirationDay() { return expirationDay; }

    /**
     * Sets the in-game day by which this lead must be discovered.  Pass
     * {@code 0} (the default) to indicate the lead never expires.
     *
     * @param day expiration day (0 = no expiration, &gt; 0 = expires after
     *            this day)
     */
    public void setExpirationDay(int day) {
        this.expirationDay = Math.max(0, day);
    }

    /**
     * Returns {@code true} if this lead has expired — i.e. it has an
     * expiration day, the current in-game day is past that day, and the lead
     * was never discovered.
     *
     * @param currentDay the current in-game day
     */
    public boolean isExpired(int currentDay) {
        return expirationDay > 0 && !discovered && currentDay > expirationDay;
    }

    // -------------------------------------------------------------------------
    // State mutation
    // -------------------------------------------------------------------------

    /**
     * Marks this lead as discovered.  Calling this more than once is a no-op.
     */
    public void discover() {
        this.discovered = true;
    }

    @Override
    public String toString() {
        return "CaseLead{id='" + id + "', method=" + discoveryMethod
                + ", discovered=" + discovered
                + (expirationDay > 0 ? ", expires=" + expirationDay : "")
                + '}';
    }
}
