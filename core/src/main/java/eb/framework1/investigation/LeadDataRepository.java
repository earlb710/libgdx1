package eb.framework1.investigation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Data-driven repository of lead templates, keyed by {@link CaseType}.
 *
 * <p>Each template is a triple of {@code {description, hint, discoveryMethod}}
 * where {@code {s}} is replaced with the subject name and {@code {v}} with
 * the victim name at generation time.
 *
 * <p>This class replaces the eight parallel {@code buildXxxLeads()} methods
 * that previously existed in {@link CaseGenerator}, eliminating structural
 * duplication while preserving all narrative content.
 */
public final class LeadDataRepository {

    /** A single lead template before name substitution. */
    private static final class Template {
        final String description;
        final String hint;
        final DiscoveryMethod method;

        Template(String description, String hint, DiscoveryMethod method) {
            this.description = description;
            this.hint = hint;
            this.method = method;
        }
    }

    private static final Map<CaseType, Template[]> TEMPLATES = buildTemplates();

    private LeadDataRepository() { /* utility class */ }

    /**
     * Builds leads for the given case type, substituting the subject and
     * victim names into the templates.
     *
     * @param type    case type
     * @param subject the subject name (substituted for {@code {s}})
     * @param victim  the victim name (substituted for {@code {v}}); may be empty
     * @return mutable list of leads
     */
    public static List<CaseLead> buildLeads(CaseType type, String subject, String victim) {
        Template[] templates = TEMPLATES.get(type);
        if (templates == null) {
            List<CaseLead> fallback = new ArrayList<>();
            fallback.add(new CaseLead("lead-1",
                    "Information about " + subject + " has not yet been gathered.",
                    "Begin with a background check on the subject.",
                    DiscoveryMethod.BACKGROUND_CHECK));
            return fallback;
        }

        List<CaseLead> leads = new ArrayList<>();
        int i = 1;
        for (Template t : templates) {
            String desc = t.description.replace("{s}", subject).replace("{v}", victim);
            String hint = t.hint.replace("{s}", subject).replace("{v}", victim);
            leads.add(new CaseLead("lead-" + i, desc, hint, t.method));
            i++;
        }
        return leads;
    }

    // =========================================================================
    // Template data — one entry per case type
    // =========================================================================

    private static Map<CaseType, Template[]> buildTemplates() {
        Map<CaseType, Template[]> m = new EnumMap<>(CaseType.class);

        m.put(CaseType.MISSING_PERSON, new Template[]{
            new Template(
                "{s} withdrew a large sum of cash two days before disappearing,"
                    + " suggesting a planned departure — or someone forcing their hand.",
                "Recent unusual financial activity may explain the disappearance.",
                DiscoveryMethod.DOCUMENTS),
            new Template(
                "A neighbour saw {s} arguing with an unknown individual"
                    + " the evening before they vanished.",
                "Someone in the area may have witnessed something important.",
                DiscoveryMethod.INTERVIEW),
            new Template(
                "{s}'s phone was last active two blocks from their home,"
                    + " near a parking structure with no CCTV coverage.",
                "The subject's last known location has not been fully examined.",
                DiscoveryMethod.PHYSICAL_SEARCH)
        });

        m.put(CaseType.INFIDELITY, new Template[]{
            new Template(
                "{s} meets the other party at a downtown bar every Tuesday"
                    + " between 19:00 and 22:00.",
                "The subject appears to have a regular undocumented appointment.",
                DiscoveryMethod.SURVEILLANCE),
            new Template(
                "Hotel receipts under a false name match {s}"
                    + "'s handwriting and a credit card linked to a shell account.",
                "Financial records may corroborate the client's suspicions.",
                DiscoveryMethod.DOCUMENTS),
            new Template(
                "A former colleague of {s}"
                    + " was seen leaving the same address on two separate occasions.",
                "Speaking to people from the subject's past may be revealing.",
                DiscoveryMethod.INTERVIEW)
        });

        m.put(CaseType.THEFT, new Template[]{
            new Template(
                "Fingerprints lifted from the point of entry match {s}"
                    + "'s on file from a prior caution.",
                "The entry point may still hold physical evidence.",
                DiscoveryMethod.FORENSICS),
            new Template(
                "{s} sold items matching the stolen property's description"
                    + " to a second-hand dealer two days after the incident.",
                "The stolen goods may have already changed hands.",
                DiscoveryMethod.BACKGROUND_CHECK),
            new Template(
                "A security camera at a nearby business captured a figure matching"
                    + " {s}'s build leaving the area at 02:30.",
                "Footage from the night of the theft may not have been reviewed.",
                DiscoveryMethod.PHYSICAL_SEARCH)
        });

        m.put(CaseType.FRAUD, new Template[]{
            new Template(
                "{s} set up a dormant company eighteen months ago."
                    + " Payments from the victim's accounts flow into it via two intermediaries.",
                "Corporate records may reveal undisclosed financial connections.",
                DiscoveryMethod.DOCUMENTS),
            new Template(
                "An accountant at {s}'s firm suspects the manipulation"
                    + " but was told to stay quiet.",
                "An insider at the subject's workplace may be willing to talk.",
                DiscoveryMethod.INTERVIEW),
            new Template(
                "{s} has transferred substantial sums abroad in the past"
                    + " six months, well above their declared income.",
                "A background check on the subject's known associates and assets"
                    + " may expose the full picture.",
                DiscoveryMethod.BACKGROUND_CHECK)
        });

        m.put(CaseType.BLACKMAIL, new Template[]{
            new Template(
                "The blackmail messages were sent from a disposable device"
                    + " registered to a false address, but the writing style"
                    + " is consistent with {s}'s known correspondence.",
                "The communication method used carries traces of the sender's habits.",
                DiscoveryMethod.DOCUMENTS),
            new Template(
                "{s} was present at the event the client is being blackmailed about"
                    + " and was seen photographing guests.",
                "Witness accounts from that event could place {s}"
                    + " in a position to gather compromising material.",
                DiscoveryMethod.INTERVIEW),
            new Template(
                "A hidden drive in {s}'s office contains copies of the"
                    + " material referenced in the blackmail demands.",
                "A search of the subject's private space may yield physical evidence.",
                DiscoveryMethod.PHYSICAL_SEARCH)
        });

        m.put(CaseType.MURDER, new Template[]{
            new Template(
                "Traces of a sedative not consistent with {v}'s prescription"
                    + " were found on a glass recovered near the scene.",
                "Physical objects at the scene may yield forensic evidence"
                    + " overlooked in the original investigation.",
                DiscoveryMethod.FORENSICS),
            new Template(
                "{s} altered their alibi between the first and second police"
                    + " interviews; a witness can confirm the discrepancy.",
                "Inconsistencies in the official account may surface"
                    + " when speaking to those who were there.",
                DiscoveryMethod.INTERVIEW),
            new Template(
                "{s} was observed watching {v}'s residence"
                    + " on three evenings in the week before the death.",
                "Systematic observation of the subject's behaviour patterns"
                    + " may reveal prior knowledge of the victim's movements.",
                DiscoveryMethod.SURVEILLANCE),
            new Template(
                "{v} sent a message naming {s}"
                    + " hours before their death; the message was deleted remotely.",
                "Reviewing digital records and correspondence"
                    + " from the days surrounding the incident may be critical.",
                DiscoveryMethod.DOCUMENTS),
            new Template(
                "Independent forensic analysis has narrowed the time of death to a"
                    + " precise window that contradicts {s}'s alibi.",
                "The exact time of death is uncertain — commissioning an independent"
                    + " forensic review could narrow the window and expose alibi gaps.",
                DiscoveryMethod.FORENSICS)
        });

        m.put(CaseType.STALKING, new Template[]{
            new Template(
                "{s} has photographed the client's home and daily routine"
                    + " over a six-week period; a folder of images was found"
                    + " discarded near their residence.",
                "The subject's personal space may contain evidence of obsessive tracking.",
                DiscoveryMethod.PHYSICAL_SEARCH),
            new Template(
                "{s} created multiple fake online accounts to monitor"
                    + " and contact the client.",
                "Tracing digital footprints and correspondence trails"
                    + " may identify the source of online harassment.",
                DiscoveryMethod.DOCUMENTS),
            new Template(
                "A former partner of {s}"
                    + " filed a similar complaint two years ago in another city.",
                "A background investigation into the subject's history"
                    + " may reveal a pattern of behaviour.",
                DiscoveryMethod.BACKGROUND_CHECK)
        });

        m.put(CaseType.CORPORATE_ESPIONAGE, new Template[]{
            new Template(
                "{s} accessed confidential project files outside normal"
                    + " working hours on at least twelve occasions in the past quarter.",
                "Access logs and internal records may show irregular system activity.",
                DiscoveryMethod.DOCUMENTS),
            new Template(
                "A colleague noticed {s}"
                    + " photographing a whiteboard during a closed strategy session.",
                "Other staff members may have witnessed suspicious behaviour.",
                DiscoveryMethod.INTERVIEW),
            new Template(
                "{s} meets a contact from the rival firm monthly"
                    + " at a location away from both offices.",
                "Regular off-site meetings between the subject and"
                    + " a competitor employee would confirm the leak.",
                DiscoveryMethod.SURVEILLANCE)
        });

        return m;
    }
}
