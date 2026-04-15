package eb.framework1.investigation;

/**
 * A fact that has been established during an investigation, tagged with its
 * {@link FactSource origin}.
 *
 * <p>Known facts are divided into three origin buckets:
 * <ul>
 *   <li><strong>CASE</strong> — provided by the client at the start of the
 *       investigation (seed facts from the case description).</li>
 *   <li><strong>POLICE</strong> — obtained from official channels: police
 *       scene reports, forensic laboratory results (DNA, toxicology, digital
 *       forensics, financial record analysis), and officer statements.</li>
 *   <li><strong>DISCOVERED</strong> — uncovered by the investigator through
 *       interviews, surveillance, research, or other field work.</li>
 * </ul>
 *
 * <p>Instances are immutable once created.
 */
public final class KnownFact {

    private final String     text;
    private final FactSource source;

    /**
     * Creates a known fact.
     *
     * @param text   human-readable fact description; must not be null or blank
     * @param source how the fact was obtained; must not be null
     */
    public KnownFact(String text, FactSource source) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Fact text must not be blank");
        }
        if (source == null) {
            throw new IllegalArgumentException("FactSource must not be null");
        }
        this.text   = text;
        this.source = source;
    }

    /** The human-readable description of this fact. */
    public String getText() { return text; }

    /** How this fact was obtained. */
    public FactSource getSource() { return source; }

    @Override
    public String toString() {
        return "KnownFact{source=" + source + ", text='" + text + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KnownFact that = (KnownFact) o;
        return source == that.source && text.equals(that.text);
    }

    @Override
    public int hashCode() {
        return 31 * text.hashCode() + source.hashCode();
    }
}
