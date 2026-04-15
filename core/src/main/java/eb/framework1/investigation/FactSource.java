package eb.framework1.investigation;

/**
 * Classifies how a known fact was obtained.
 *
 * <ul>
 *   <li>{@link #CASE} — information provided by the client when the case was
 *       opened (seed facts, case description, client relationships).</li>
 *   <li>{@link #POLICE} — information from official police investigation,
 *       including scene reports, forensic analysis results (DNA, toxicology,
 *       digital forensics, financial records), and witness statements
 *       gathered by officers.</li>
 *   <li>{@link #DISCOVERED} — facts uncovered through the investigator's own
 *       work during the case (interviews, surveillance, document analysis,
 *       etc.).</li>
 * </ul>
 */
public enum FactSource {

    /** Client-provided information from the case briefing. */
    CASE,

    /** Official police/forensic investigation results. */
    POLICE,

    /** Facts uncovered by the investigator during the case. */
    DISCOVERED
}
