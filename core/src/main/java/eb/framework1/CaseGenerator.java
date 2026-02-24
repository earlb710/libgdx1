package eb.framework1;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Procedurally generates fully-populated {@link CaseFile} objects that
 * simulate a client meeting and case briefing.
 *
 * <p>Each generated case includes:
 * <ul>
 *   <li>A randomly chosen (or caller-specified) {@link CaseType}</li>
 *   <li>A client name — the person who hired the detective</li>
 *   <li>A subject name — the person or entity under investigation</li>
 *   <li>A narrative description of the problem as told by the client</li>
 *   <li>A clear objective stating what must be proven or found</li>
 *   <li>A list of {@link CaseLead}s — hidden facts and the in-game
 *       activity required to uncover each one</li>
 * </ul>
 *
 * <p>This class has <strong>no libGDX dependency</strong> and can be
 * constructed and tested with plain JUnit.  Data is injected via the
 * constructor; a {@link Random} instance is accepted so that tests can
 * produce deterministic output.
 *
 * <h3>Example</h3>
 * <pre>
 *   CaseGenerator gen = new CaseGenerator(nameGenerator, new Random(42));
 *   CaseFile case1 = gen.generate("2050-03-15 09:00");
 *   // or generate a specific type:
 *   CaseFile case2 = gen.generate(CaseType.MISSING_PERSON, "2050-03-16 10:00");
 * </pre>
 */
public class CaseGenerator {

    private final PersonNameGenerator nameGen;
    private final Random              random;

    /**
     * Creates a generator with the given name source and a default {@link Random}.
     *
     * @param nameGen name generator used to produce client and subject names;
     *                must not be {@code null}
     */
    public CaseGenerator(PersonNameGenerator nameGen) {
        this(nameGen, new Random());
    }

    /**
     * Creates a generator with an explicit {@link Random} instance.
     * Pass a seeded {@code Random} to obtain reproducible output in tests.
     *
     * @param nameGen name generator; must not be {@code null}
     * @param random  random-number source; {@code null} is replaced by a
     *                default {@code new Random()}
     */
    public CaseGenerator(PersonNameGenerator nameGen, Random random) {
        if (nameGen == null) throw new IllegalArgumentException("nameGen must not be null");
        this.nameGen = nameGen;
        this.random  = random != null ? random : new Random();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Generates a {@link CaseFile} for a randomly chosen {@link CaseType}.
     *
     * @param dateOpened the in-game date/time the case is opened (e.g. {@code "2050-03-15 09:00"})
     * @return a fully populated, open {@link CaseFile}
     */
    public CaseFile generate(String dateOpened) {
        CaseType[] types = CaseType.values();
        return generate(types[random.nextInt(types.length)], dateOpened);
    }

    /**
     * Generates a {@link CaseFile} for the specified {@link CaseType}.
     *
     * @param type       the category of case to generate; must not be {@code null}
     * @param dateOpened in-game date/time the case is opened
     * @return a fully populated, open {@link CaseFile}
     * @throws IllegalArgumentException if {@code type} is {@code null}
     */
    public CaseFile generate(CaseType type, String dateOpened) {
        if (type == null) throw new IllegalArgumentException("CaseType must not be null");

        String clientGender  = randomGender();
        String subjectGender = randomGender();

        String clientName  = nameGen.generateFull(clientGender);
        String subjectName = nameGen.generateFull(subjectGender);

        String caseName   = type.getDisplayName() + ": " + subjectName;
        String description = buildDescription(type, clientName, subjectName, clientGender, subjectGender);
        String objective   = buildObjective(type, subjectName);

        CaseFile cf = new CaseFile(caseName, description, dateOpened != null ? dateOpened : "");
        cf.setCaseType(type);
        cf.setClientName(clientName);
        cf.setSubjectName(subjectName);
        cf.setObjective(objective);

        for (CaseLead lead : buildLeads(type, subjectName)) {
            cf.addLead(lead);
        }
        return cf;
    }

    // -------------------------------------------------------------------------
    // Description templates
    // -------------------------------------------------------------------------

    private String buildDescription(CaseType type, String client, String subject,
                                    String clientGender, String subjectGender) {
        String pronoun = "F".equals(subjectGender) ? "She" : "He";
        switch (type) {
            case MISSING_PERSON:
                return client + " came in looking for answers. " + subject
                        + " vanished three days ago without a word."
                        + " No note, no call, nothing. " + pronoun
                        + " left behind a half-eaten meal and an unlocked door."
                        + " The police say it's too early to file a report.";
            case INFIDELITY:
                return client + " suspects their partner, " + subject
                        + ", has been seeing someone else."
                        + " Late nights at the office, unexplained receipts,"
                        + " a phone that's always face-down."
                        + " They need the truth — one way or the other.";
            case THEFT:
                return client + " reported that " + subject
                        + " was seen near the premises on the night of the theft."
                        + " Valuables are missing. Insurance won't pay without a name.";
            case FRAUD:
                return client + " noticed irregularities in the accounts linked to "
                        + subject + "."
                        + " Money moved in small amounts, always just under the threshold."
                        + " Someone knows what they're doing.";
            case BLACKMAIL:
                return client + " has been receiving anonymous messages."
                        + " The sender claims to have compromising material."
                        + " A name has come up: " + subject + "."
                        + " Could be a coincidence. Probably isn't.";
            case MURDER:
                return client + " doesn't believe the official story."
                        + " The coroner called it an accident. The family called it murder."
                        + " " + subject + " was the last person seen with the deceased.";
            case STALKING:
                return client + " has been followed, photographed, and harassed for weeks."
                        + " " + subject + " was identified by a neighbour."
                        + " The client is scared. They need this stopped.";
            case CORPORATE_ESPIONAGE:
                return client + "'s company has been bleeding trade secrets."
                        + " Competitor bids match their proposals almost word for word."
                        + " Internal suspicion has settled on " + subject + ".";
            default:
                return client + " needs information about " + subject + ".";
        }
    }

    // -------------------------------------------------------------------------
    // Objective templates
    // -------------------------------------------------------------------------

    private String buildObjective(CaseType type, String subject) {
        switch (type) {
            case MISSING_PERSON:
                return "Locate " + subject + " and determine whether they left voluntarily"
                        + " or were taken against their will.";
            case INFIDELITY:
                return "Confirm or disprove that " + subject
                        + " is conducting an undisclosed relationship,"
                        + " and if so, identify the other party.";
            case THEFT:
                return "Establish whether " + subject + " committed the theft and,"
                        + " where possible, locate the stolen property.";
            case FRAUD:
                return "Gather documented evidence of financial misconduct by "
                        + subject + " sufficient to support a formal complaint.";
            case BLACKMAIL:
                return "Identify whether " + subject + " is the source of the blackmail"
                        + " and obtain evidence that can be handed to the authorities.";
            case MURDER:
                return "Uncover what " + subject + " knows about the victim's death"
                        + " and find evidence that contradicts the official findings.";
            case STALKING:
                return "Document " + subject + "'s surveillance activities"
                        + " and gather evidence for a restraining order or police report.";
            case CORPORATE_ESPIONAGE:
                return "Prove that " + subject + " has been passing confidential information"
                        + " to a competitor and identify who they are working with.";
            default:
                return "Investigate " + subject + " and report the findings.";
        }
    }

    // -------------------------------------------------------------------------
    // Lead templates (hidden information + how to discover each)
    // -------------------------------------------------------------------------

    private List<CaseLead> buildLeads(CaseType type, String subject) {
        List<CaseLead> leads = new ArrayList<>();
        int leadIndex = 1;
        switch (type) {
            case MISSING_PERSON:
                leads.add(lead(leadIndex++,
                        subject + " withdrew a large sum of cash two days before disappearing,"
                                + " suggesting a planned departure — or someone forcing their hand.",
                        "Recent unusual financial activity may explain the disappearance.",
                        DiscoveryMethod.DOCUMENTS));
                leads.add(lead(leadIndex++,
                        "A neighbour saw " + subject + " arguing with an unknown individual"
                                + " the evening before they vanished.",
                        "Someone in the area may have witnessed something important.",
                        DiscoveryMethod.INTERVIEW));
                leads.add(lead(leadIndex++,
                        subject + "'s phone was last active two blocks from their home,"
                                + " near a parking structure with no CCTV coverage.",
                        "The subject's last known location has not been fully examined.",
                        DiscoveryMethod.PHYSICAL_SEARCH));
                break;

            case INFIDELITY:
                leads.add(lead(leadIndex++,
                        subject + " meets the other party at a downtown bar every Tuesday"
                                + " between 19:00 and 22:00.",
                        "The subject appears to have a regular undocumented appointment.",
                        DiscoveryMethod.SURVEILLANCE));
                leads.add(lead(leadIndex++,
                        "Hotel receipts under a false name match " + subject
                                + "'s handwriting and a credit card linked to a shell account.",
                        "Financial records may corroborate the client's suspicions.",
                        DiscoveryMethod.DOCUMENTS));
                leads.add(lead(leadIndex++,
                        "A former colleague of " + subject
                                + " was seen leaving the same address on two separate occasions.",
                        "Speaking to people from the subject's past may be revealing.",
                        DiscoveryMethod.INTERVIEW));
                break;

            case THEFT:
                leads.add(lead(leadIndex++,
                        "Fingerprints lifted from the point of entry match " + subject
                                + "'s on file from a prior caution.",
                        "The entry point may still hold physical evidence.",
                        DiscoveryMethod.FORENSICS));
                leads.add(lead(leadIndex++,
                        subject + " sold items matching the stolen property's description"
                                + " to a second-hand dealer two days after the incident.",
                        "The stolen goods may have already changed hands.",
                        DiscoveryMethod.BACKGROUND_CHECK));
                leads.add(lead(leadIndex++,
                        "A security camera at a nearby business captured a figure matching"
                                + " " + subject + "'s build leaving the area at 02:30.",
                        "Footage from the night of the theft may not have been reviewed.",
                        DiscoveryMethod.PHYSICAL_SEARCH));
                break;

            case FRAUD:
                leads.add(lead(leadIndex++,
                        subject + " set up a dormant company eighteen months ago."
                                + " Payments from the victim's accounts flow into it via two intermediaries.",
                        "Corporate records may reveal undisclosed financial connections.",
                        DiscoveryMethod.DOCUMENTS));
                leads.add(lead(leadIndex++,
                        "An accountant at " + subject + "'s firm suspects the manipulation"
                                + " but was told to stay quiet.",
                        "An insider at the subject's workplace may be willing to talk.",
                        DiscoveryMethod.INTERVIEW));
                leads.add(lead(leadIndex++,
                        subject + " has transferred substantial sums abroad in the past"
                                + " six months, well above their declared income.",
                        "A background check on the subject's known associates and assets"
                                + " may expose the full picture.",
                        DiscoveryMethod.BACKGROUND_CHECK));
                break;

            case BLACKMAIL:
                leads.add(lead(leadIndex++,
                        "The blackmail messages were sent from a disposable device"
                                + " registered to a false address, but the writing style"
                                + " is consistent with " + subject + "'s known correspondence.",
                        "The communication method used carries traces of the sender's habits.",
                        DiscoveryMethod.DOCUMENTS));
                leads.add(lead(leadIndex++,
                        subject + " was present at the event the client is being blackmailed about"
                                + " and was seen photographing guests.",
                        "Witness accounts from that event could place " + subject
                                + " in a position to gather compromising material.",
                        DiscoveryMethod.INTERVIEW));
                leads.add(lead(leadIndex++,
                        "A hidden drive in " + subject + "'s office contains copies of the"
                                + " material referenced in the blackmail demands.",
                        "A search of the subject's private space may yield physical evidence.",
                        DiscoveryMethod.PHYSICAL_SEARCH));
                break;

            case MURDER:
                leads.add(lead(leadIndex++,
                        "Traces of a sedative not consistent with the victim's prescription"
                                + " were found on a glass recovered near the scene.",
                        "Physical objects at the scene may yield forensic evidence"
                                + " overlooked in the original investigation.",
                        DiscoveryMethod.FORENSICS));
                leads.add(lead(leadIndex++,
                        subject + " altered their alibi between the first and second police"
                                + " interviews; a witness can confirm the discrepancy.",
                        "Inconsistencies in the official account may surface"
                                + " when speaking to those who were there.",
                        DiscoveryMethod.INTERVIEW));
                leads.add(lead(leadIndex++,
                        subject + " was observed watching the victim's residence"
                                + " on three evenings in the week before the death.",
                        "Systematic observation of the subject's behaviour patterns"
                                + " may reveal prior knowledge of the victim's movements.",
                        DiscoveryMethod.SURVEILLANCE));
                leads.add(lead(leadIndex,
                        "The victim sent a message naming " + subject
                                + " hours before their death; the message was deleted remotely.",
                        "Reviewing digital records and correspondence"
                                + " from the days surrounding the incident may be critical.",
                        DiscoveryMethod.DOCUMENTS));
                break;

            case STALKING:
                leads.add(lead(leadIndex++,
                        subject + " has photographed the client's home and daily routine"
                                + " over a six-week period; a folder of images was found"
                                + " discarded near their residence.",
                        "The subject's personal space may contain evidence of obsessive tracking.",
                        DiscoveryMethod.PHYSICAL_SEARCH));
                leads.add(lead(leadIndex++,
                        subject + " created multiple fake online accounts to monitor"
                                + " and contact the client.",
                        "Tracing digital footprints and correspondence trails"
                                + " may identify the source of online harassment.",
                        DiscoveryMethod.DOCUMENTS));
                leads.add(lead(leadIndex++,
                        "A former partner of " + subject
                                + " filed a similar complaint two years ago in another city.",
                        "A background investigation into the subject's history"
                                + " may reveal a pattern of behaviour.",
                        DiscoveryMethod.BACKGROUND_CHECK));
                break;

            case CORPORATE_ESPIONAGE:
                leads.add(lead(leadIndex++,
                        subject + " accessed confidential project files outside normal"
                                + " working hours on at least twelve occasions in the past quarter.",
                        "Access logs and internal records may show irregular system activity.",
                        DiscoveryMethod.DOCUMENTS));
                leads.add(lead(leadIndex++,
                        "A colleague noticed " + subject
                                + " photographing a whiteboard during a closed strategy session.",
                        "Other staff members may have witnessed suspicious behaviour.",
                        DiscoveryMethod.INTERVIEW));
                leads.add(lead(leadIndex++,
                        subject + " meets a contact from the rival firm monthly"
                                + " at a location away from both offices.",
                        "Regular off-site meetings between the subject and"
                                + " a competitor employee would confirm the leak.",
                        DiscoveryMethod.SURVEILLANCE));
                break;

            default:
                leads.add(lead(leadIndex,
                        "Information about " + subject + " has not yet been gathered.",
                        "Begin with a background check on the subject.",
                        DiscoveryMethod.BACKGROUND_CHECK));
                break;
        }
        return leads;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static CaseLead lead(int index, String description, String hint,
                                 DiscoveryMethod method) {
        return new CaseLead("lead-" + index, description, hint, method);
    }

    /** Returns {@code "M"} or {@code "F"} at random. */
    private String randomGender() {
        return random.nextBoolean() ? "M" : "F";
    }
}
