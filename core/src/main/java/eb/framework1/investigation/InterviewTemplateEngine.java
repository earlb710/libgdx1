package eb.framework1.investigation;

import eb.framework1.RandomUtils;
import eb.framework1.generator.PersonNameGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Encapsulates the generation of pre-scripted interview content for the
 * four core NPC roles (client, subject, witness, associate).
 *
 * <p>All user-visible text is loaded from
 * {@code assets/text/interview_templates_en.json} via an
 * {@link InterviewTemplateData} instance supplied at construction time.
 * Placeholder tokens ({@code $client}, {@code $subject}, etc.) are resolved
 * by {@link TemplateResolver} at generation time so that the same template
 * data can be translated without recompiling.
 *
 * <p>If no {@link InterviewTemplateData} is provided (legacy / test usage)
 * the engine returns generic placeholder strings rather than throwing.
 *
 * <p>Extracted from {@link CaseGenerator} to reduce that class size and
 * isolate interview-specific logic.  The engine uses a shared {@link Random}
 * so that output is deterministic when seeded.
 */
public class InterviewTemplateEngine {

    private final Random               random;
    private final InterviewTemplateData templateData;

    // =========================================================================
    // Constructors
    // =========================================================================

    /**
     * Creates an engine that reads templates from {@code data}.
     *
     * @param random shared random source (same instance used by the parent
     *               generator for reproducibility); {@code null} is replaced
     *               by a default {@code new Random()}
     * @param data   pre-loaded template pool data; may be {@code null}, in
     *               which case all generated texts read as a short fallback
     */
    public InterviewTemplateEngine(Random random, InterviewTemplateData data) {
        this.random       = random != null ? random : new Random();
        this.templateData = data;
    }

    /**
     * Convenience constructor for callers that have not yet adopted the JSON
     * pipeline.  Equivalent to {@code new InterviewTemplateEngine(random, null)}.
     *
     * @param random shared random source
     */
    public InterviewTemplateEngine(Random random) {
        this(random, null);
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Builds interview scripts for all four key NPCs.
     *
     * <p>Equivalent to {@link #buildAll(CaseType, String, String, String,
     * String, String, PersonNameGenerator, int)} with {@code complexity = 1}
     * (no reliability variance).
     *
     * @param type          case category
     * @param client        client name
     * @param subject       subject/suspect name
     * @param victim        victim name (may be empty for non-Murder cases)
     * @param clientGender  {@code "M"} or {@code "F"}
     * @param subjectGender {@code "M"} or {@code "F"}
     * @param nameGen       name generator for witness/associate names
     * @return list of interview scripts, one per interviewable NPC
     */
    public List<InterviewScript> buildAll(CaseType type,
                                          String client, String subject,
                                          String victim,
                                          String clientGender,
                                          String subjectGender,
                                          PersonNameGenerator nameGen) {
        return buildAll(type, client, subject, victim,
                clientGender, subjectGender, nameGen, 1);
    }

    /**
     * Builds interview scripts for all key NPCs, with complexity-driven
     * <strong>witness reliability variance</strong>.
     *
     * <p>At <b>complexity 1</b> all witnesses are fully reliable (reliability
     * = 1.0).  At <b>complexity ≥ 2</b> some witness scripts may be generated
     * with a lower reliability score (0.5–0.9), causing a subset of their
     * responses to be randomly flagged as non-truthful.  At
     * <b>complexity 3</b> an additional <em>contradictory witness</em> is
     * appended whose accounts on key topics (whereabouts, last contact,
     * observation) conflict with the primary witness — the player must
     * determine which account is accurate.
     *
     * @param type          case category
     * @param client        client name
     * @param subject       subject/suspect name
     * @param victim        victim name (may be empty for non-Murder cases)
     * @param clientGender  {@code "M"} or {@code "F"}
     * @param subjectGender {@code "M"} or {@code "F"}
     * @param nameGen       name generator for witness/associate names
     * @param complexity    1, 2, or 3
     * @return list of interview scripts, one per interviewable NPC
     */
    public List<InterviewScript> buildAll(CaseType type,
                                          String client, String subject,
                                          String victim,
                                          String clientGender,
                                          String subjectGender,
                                          PersonNameGenerator nameGen,
                                          int complexity) {
        List<InterviewScript> scripts = new ArrayList<>();
        boolean isMurder = type == CaseType.MURDER;
        String targetPerson = isMurder ? victim : subject;

        // --- Client script (always reliable) ---
        InterviewScript clientScript = new InterviewScript("npc-client", client, "Client");
        buildClientInterview(clientScript, type, client, subject, victim, clientGender);
        scripts.add(clientScript);

        // --- Subject (suspect) script (always uses its own deception logic) ---
        InterviewScript subjectScript = new InterviewScript("npc-subject", subject,
                isMurder ? "Subject (Suspect)" : "Subject");
        buildSubjectInterview(subjectScript, type, client, subject, victim, subjectGender);
        scripts.add(subjectScript);

        // --- Key Witness script ---
        // At complexity ≥ 2, there is a chance the witness is unreliable
        float witnessReliability = computeWitnessReliability(complexity);
        String witnessGender = RandomUtils.randomGender(random);
        String witnessName = nameGen.generateFull(witnessGender);
        InterviewScript witnessScript = new InterviewScript("npc-witness", witnessName,
                "Key Witness", witnessReliability);
        buildWitnessInterview(witnessScript, type, client, subject, victim,
                targetPerson, witnessReliability);
        scripts.add(witnessScript);

        // --- Associate script (victim's or subject's associate) ---
        String assocGender = RandomUtils.randomGender(random);
        String associateName = nameGen.generateFull(assocGender);
        String assocRole = isMurder ? "Victim's Associate" : "Subject's Associate";
        InterviewScript associateScript = new InterviewScript("npc-associate", associateName,
                assocRole);
        buildAssociateInterview(associateScript, type, client, subject, victim,
                targetPerson, associateName);
        scripts.add(associateScript);

        // --- Contradictory witness (complexity 3 only) ---
        if (complexity >= 3) {
            String contraGender = RandomUtils.randomGender(random);
            String contraName = nameGen.generateFull(contraGender);
            // Contradictory witness has low reliability (0.5–0.7)
            float contraReliability = 0.5f + random.nextFloat() * 0.2f;
            InterviewScript contraScript = new InterviewScript("npc-contra-witness",
                    contraName, "Contradictory Witness", contraReliability);
            buildContradictoryWitnessInterview(contraScript, type, client, subject,
                    victim, targetPerson, witnessScript);
            scripts.add(contraScript);
        }

        return scripts;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Picks one element at random from a pool list. */
    private String pick(List<String> pool) {
        if (pool == null || pool.isEmpty()) return "[text unavailable]";
        return pool.get(random.nextInt(pool.size()));
    }

    /**
     * Picks a random template from the named JSON pool and resolves all
     * {@code $placeholder} tokens using {@code vars}.
     */
    private String pickAndResolve(String poolKey, Map<String, String> vars) {
        if (templateData == null) return "[text unavailable]";
        String template = pick(templateData.getPool(poolKey));
        return TemplateResolver.resolve(template, vars, templateData, random);
    }

    /** Resolves a question string from the JSON, substituting placeholders. */
    private String resolveQuestion(String questionKey, Map<String, String> vars) {
        if (templateData == null) return "";
        return templateData.getQuestion(questionKey, vars, templateData, random);
    }

    private static String pronoun(String gender) {
        return "F".equals(gender) ? "she" : "he";
    }

    private static String pronounCap(String gender) {
        return "F".equals(gender) ? "She" : "He";
    }

    /** Builds a vars map pre-populated with the standard named placeholders. */
    private static Map<String, String> vars(String client, String subject, String victim,
                                             String targetPerson,
                                             String pronoun, String pronounCap) {
        Map<String, String> m = new HashMap<String, String>();
        m.put("client",       client       != null ? client       : "");
        m.put("subject",      subject      != null ? subject      : "");
        m.put("victim",       victim       != null ? victim       : "");
        m.put("targetPerson", targetPerson != null ? targetPerson : "");
        m.put("pronoun",      pronoun      != null ? pronoun      : "");
        m.put("pronounCap",   pronounCap   != null ? pronounCap   : "");
        return m;
    }

    // =========================================================================
    // Client interview
    // =========================================================================

    private void buildClientInterview(InterviewScript script, CaseType type,
                                      String client, String subject, String victim,
                                      String clientGender) {
        String pronoun    = pronoun(clientGender);
        String pronounCap = pronounCap(clientGender);
        boolean isMurder  = type == CaseType.MURDER;
        String targetPerson = isMurder ? victim : subject;
        Map<String, String> vars = vars(client, subject, victim, targetPerson, pronoun, pronounCap);

        // ALIBI
        script.addResponse(new InterviewResponse(InterviewTopic.ALIBI,
                resolveQuestion("alibi", vars),
                pickAndResolve("client.alibi", vars), true, ""));

        // OPINION of subject — Empathy-gated
        String opinionKey, opinionAltKey;
        switch (type) {
            case MURDER:
                opinionKey    = "client.opinion.murder";
                opinionAltKey = "client.opinion.murder.alt";
                break;
            case INFIDELITY:
                opinionKey    = "client.opinion.infidelity";
                opinionAltKey = "client.opinion.infidelity.alt";
                break;
            default:
                opinionKey    = "client.opinion.default";
                opinionAltKey = "client.opinion.default.alt";
                break;
        }
        script.addResponse(new InterviewResponse(InterviewTopic.OPINION,
                resolveQuestion("client.opinion.subject", vars),
                pickAndResolve(opinionKey, vars), true, subject,
                "Empathy", 5, pickAndResolve(opinionAltKey, vars)));

        // LAST CONTACT with target
        script.addResponse(new InterviewResponse(InterviewTopic.LAST_CONTACT,
                resolveQuestion("client.lastContact", vars),
                pickAndResolve("client.lastContact", vars), true, targetPerson));

        // OBSERVATION — Perception-gated
        script.addResponse(new InterviewResponse(InterviewTopic.OBSERVATION,
                resolveQuestion("client.observation", vars),
                pickAndResolve("client.observation", vars), true, "",
                "Perception", 5, pickAndResolve("client.observation.alt", vars)));

        // MOTIVE — Intuition-gated (murder only)
        if (isMurder) {
            script.addResponse(new InterviewResponse(InterviewTopic.MOTIVE,
                    resolveQuestion("client.motive", vars),
                    pickAndResolve("client.motive", vars), true, victim,
                    "Intuition", 5, pickAndResolve("client.motive.alt", vars)));
        }

        // RELATIONSHIP with the target person
        script.addResponse(new InterviewResponse(InterviewTopic.RELATIONSHIP,
                resolveQuestion("client.relationship", vars),
                pickAndResolve("client.relationship", vars), true, targetPerson));

        // CONTACT_INFO — Charisma-gated
        script.addResponse(new InterviewResponse(InterviewTopic.CONTACT_INFO,
                resolveQuestion("client.contactInfo", vars),
                pickAndResolve("client.contactInfo", vars), true, subject,
                "Charisma", 4, pickAndResolve("client.contactInfo.alt", vars)));

        // PERSONALITY — Empathy-gated
        script.addResponse(new InterviewResponse(InterviewTopic.PERSONALITY,
                resolveQuestion("client.personality", vars),
                pickAndResolve("client.personality", vars), true, subject,
                "Empathy", 4, pickAndResolve("client.personality.alt", vars)));
    }

    // =========================================================================
    // Subject interview
    // =========================================================================

    private void buildSubjectInterview(InterviewScript script, CaseType type,
                                       String client, String subject, String victim,
                                       String subjectGender) {
        String pronoun    = pronoun(subjectGender);
        String pronounCap = pronounCap(subjectGender);
        boolean isMurder  = type == CaseType.MURDER;
        String targetPerson = isMurder ? victim : client;
        Map<String, String> vars = vars(client, subject, victim, targetPerson, pronoun, pronounCap);

        // ALIBI — subject may lie (30 % truthful)
        boolean truthfulAlibi = random.nextInt(10) < 3;
        String alibiKey = truthfulAlibi ? "subject.alibi.truthful" : "subject.alibi.false";
        script.addResponse(new InterviewResponse(InterviewTopic.ALIBI,
                resolveQuestion("alibi", vars),
                pickAndResolve(alibiKey, vars), truthfulAlibi, ""));

        // OPINION of client
        script.addResponse(new InterviewResponse(InterviewTopic.OPINION,
                resolveQuestion("subject.opinionOfClient", vars),
                pickAndResolve("subject.opinionOfClient", vars), random.nextBoolean(), client));

        // OPINION of victim (murder only, 40 % truthful)
        if (isMurder) {
            boolean truthfulOpinion = random.nextInt(10) < 4;
            String victimOpKey = truthfulOpinion
                    ? "subject.opinionOfVictim.truthful"
                    : "subject.opinionOfVictim.false";
            script.addResponse(new InterviewResponse(InterviewTopic.OPINION,
                    resolveQuestion("subject.opinionOfVictim", vars),
                    pickAndResolve(victimOpKey, vars), truthfulOpinion, victim));
        }

        // WHEREABOUTS
        script.addResponse(new InterviewResponse(InterviewTopic.WHEREABOUTS,
                resolveQuestion("subject.whereabouts", vars),
                pickAndResolve("subject.whereabouts", vars), random.nextBoolean(), targetPerson));

        // OBSERVATION — Intimidation-gated (reveal shown when gate is met)
        script.addResponse(new InterviewResponse(InterviewTopic.OBSERVATION,
                resolveQuestion("subject.observation", vars),
                pickAndResolve("subject.observation.reveal", vars), random.nextBoolean(), "",
                "Intimidation", 6, pickAndResolve("subject.observation.default", vars)));

        // LAST CONTACT (40 % truthful)
        boolean truthfulContact = random.nextInt(10) < 4;
        String contactKey = truthfulContact
                ? "subject.lastContact.truthful"
                : "subject.lastContact.false";
        script.addResponse(new InterviewResponse(InterviewTopic.LAST_CONTACT,
                resolveQuestion("subject.lastContact", vars),
                pickAndResolve(contactKey, vars), truthfulContact, targetPerson));

        // RELATIONSHIP
        script.addResponse(new InterviewResponse(InterviewTopic.RELATIONSHIP,
                resolveQuestion("subject.relationship", vars),
                pickAndResolve("subject.relationship", vars), random.nextBoolean(), targetPerson));

        // MOTIVE — Intimidation-gated (murder only)
        if (isMurder) {
            script.addResponse(new InterviewResponse(InterviewTopic.MOTIVE,
                    resolveQuestion("subject.motive", vars),
                    pickAndResolve("subject.motive.reveal", vars), random.nextBoolean(), victim,
                    "Intimidation", 7, pickAndResolve("subject.motive.deflect", vars)));
        }

        // CONTACT_INFO — subject refuses
        script.addResponse(new InterviewResponse(InterviewTopic.CONTACT_INFO,
                resolveQuestion("subject.contactInfo", vars),
                pickAndResolve("subject.contactRefusal", vars), random.nextBoolean(), client));

        // PERSONALITY — Intimidation-gated
        script.addResponse(new InterviewResponse(InterviewTopic.PERSONALITY,
                resolveQuestion("subject.personality", vars),
                pickAndResolve("subject.personality", vars), random.nextBoolean(), client,
                "Intimidation", 5, pickAndResolve("subject.personality.alt", vars)));
    }

    // =========================================================================
    // Witness interview
    // =========================================================================

    private void buildWitnessInterview(InterviewScript script, CaseType type,
                                       String client, String subject, String victim,
                                       String targetPerson, float reliability) {
        boolean isMurder = type == CaseType.MURDER;
        Map<String, String> vars = vars(client, subject, victim, targetPerson, "", "");

        // ALIBI
        script.addResponse(new InterviewResponse(InterviewTopic.ALIBI,
                resolveQuestion("alibi", vars),
                pickAndResolve("witness.alibi", vars),
                reliableTruth(reliability), ""));

        // WHEREABOUTS of subject
        script.addResponse(new InterviewResponse(InterviewTopic.WHEREABOUTS,
                resolveQuestion("witness.subjectWhereabouts", vars),
                pickAndResolve("witness.subjectWhereabouts", vars),
                reliableTruth(reliability), subject));

        // OPINION of subject — Empathy-gated
        script.addResponse(new InterviewResponse(InterviewTopic.OPINION,
                resolveQuestion("witness.opinion.subject", vars),
                pickAndResolve("witness.opinion.subject", vars),
                reliableTruth(reliability), subject,
                "Empathy", 6, pickAndResolve("witness.opinion.subject.alt", vars)));

        // OPINION of victim (murder only)
        if (isMurder) {
            script.addResponse(new InterviewResponse(InterviewTopic.OPINION,
                    resolveQuestion("witness.opinion.victim", vars),
                    pickAndResolve("witness.opinion.victim", vars),
                    reliableTruth(reliability), victim));
        }

        // OBSERVATION — Perception-gated
        script.addResponse(new InterviewResponse(InterviewTopic.OBSERVATION,
                resolveQuestion("witness.observation", vars),
                pickAndResolve("witness.observation", vars),
                reliableTruth(reliability), "",
                "Perception", 6, pickAndResolve("witness.observation.alt", vars)));

        // LAST CONTACT
        script.addResponse(new InterviewResponse(InterviewTopic.LAST_CONTACT,
                resolveQuestion("witness.lastContact", vars),
                pickAndResolve("witness.lastContact", vars),
                reliableTruth(reliability), targetPerson));

        // RELATIONSHIP
        script.addResponse(new InterviewResponse(InterviewTopic.RELATIONSHIP,
                resolveQuestion("witness.relationship", vars),
                pickAndResolve("witness.relationship", vars),
                reliableTruth(reliability), targetPerson));

        // MOTIVE — Intuition-gated (murder only)
        if (isMurder) {
            script.addResponse(new InterviewResponse(InterviewTopic.MOTIVE,
                    resolveQuestion("witness.motive", vars),
                    pickAndResolve("witness.motive", vars),
                    reliableTruth(reliability), victim,
                    "Intuition", 5, pickAndResolve("witness.motive.alt", vars)));
        }

        // CONTACT_INFO — Charisma-gated
        script.addResponse(new InterviewResponse(InterviewTopic.CONTACT_INFO,
                resolveQuestion("witness.contactInfo", vars),
                pickAndResolve("witness.contactInfo", vars),
                reliableTruth(reliability), subject,
                "Charisma", 5, pickAndResolve("witness.contactInfo.alt", vars)));

        // PERSONALITY — Empathy-gated (victim for murder, subject otherwise)
        String targetForTraits = isMurder ? victim : subject;
        Map<String, String> persVars = vars(client, subject, victim, targetForTraits, "", "");
        script.addResponse(new InterviewResponse(InterviewTopic.PERSONALITY,
                resolveQuestion("witness.personality", persVars),
                pickAndResolve("witness.personality", persVars),
                reliableTruth(reliability), targetForTraits,
                "Empathy", 5, pickAndResolve("witness.personality.alt", persVars)));
    }

    // =========================================================================
    // Associate interview
    // =========================================================================

    private void buildAssociateInterview(InterviewScript script, CaseType type,
                                         String client, String subject, String victim,
                                         String targetPerson, String associateName) {
        boolean isMurder = type == CaseType.MURDER;
        Map<String, String> vars = vars(client, subject, victim, targetPerson, "", "");
        vars.put("associateName", associateName != null ? associateName : "");

        // ALIBI
        script.addResponse(new InterviewResponse(InterviewTopic.ALIBI,
                resolveQuestion("alibi", vars),
                pickAndResolve("associate.alibi", vars), true, ""));

        // RELATIONSHIP with target
        String relKey = isMurder ? "associate.relationship.murder"
                                 : "associate.relationship.other";
        script.addResponse(new InterviewResponse(InterviewTopic.RELATIONSHIP,
                resolveQuestion("associate.relationship", vars),
                pickAndResolve(relKey, vars), true, targetPerson));

        // OPINION of subject — Empathy-gated
        script.addResponse(new InterviewResponse(InterviewTopic.OPINION,
                resolveQuestion("associate.opinion.subject", vars),
                pickAndResolve("associate.opinion.subject", vars), true, subject,
                "Empathy", 6, pickAndResolve("associate.opinion.subject.alt", vars)));

        // WHEREABOUTS
        script.addResponse(new InterviewResponse(InterviewTopic.WHEREABOUTS,
                resolveQuestion("associate.whereabouts", vars),
                pickAndResolve("associate.whereabouts", vars), random.nextBoolean(), subject));

        // OBSERVATION — Perception-gated
        script.addResponse(new InterviewResponse(InterviewTopic.OBSERVATION,
                resolveQuestion("associate.observation", vars),
                pickAndResolve("associate.observation", vars), true, "",
                "Perception", 5, pickAndResolve("associate.observation.alt", vars)));

        // LAST CONTACT
        script.addResponse(new InterviewResponse(InterviewTopic.LAST_CONTACT,
                resolveQuestion("associate.lastContact", vars),
                pickAndResolve("associate.lastContact", vars), true, targetPerson));

        // MOTIVE — Intuition-gated (murder only)
        if (isMurder) {
            script.addResponse(new InterviewResponse(InterviewTopic.MOTIVE,
                    resolveQuestion("associate.motive", vars),
                    pickAndResolve("associate.motive", vars), true, victim,
                    "Intuition", 6, pickAndResolve("associate.motive.alt", vars)));
        }

        // CONTACT_INFO — Charisma-gated
        script.addResponse(new InterviewResponse(InterviewTopic.CONTACT_INFO,
                resolveQuestion("associate.contactInfo", vars),
                pickAndResolve("associate.contactInfo", vars), true, subject,
                "Charisma", 4, pickAndResolve("associate.contactInfo.alt", vars)));

        // PERSONALITY — Empathy-gated
        script.addResponse(new InterviewResponse(InterviewTopic.PERSONALITY,
                resolveQuestion("associate.personality", vars),
                pickAndResolve("associate.personality", vars), true, targetPerson,
                "Empathy", 4, pickAndResolve("associate.personality.alt", vars)));
    }

    // =========================================================================
    // Dynamic cross-NPC text helpers (used by the admin panel)
    // =========================================================================

    /**
     * Generates opinion text for one NPC's view of another, used when the
     * admin panel produces cross-NPC opinion rows.
     *
     * @param npcRole   role of the NPC giving the opinion
     * @param otherName name of the NPC being discussed
     * @param otherRole role of the NPC being discussed
     * @param subject   case subject name
     * @param victim    case victim name (may be empty for non-Murder cases)
     * @param isMurder  {@code true} for Murder case types
     * @return opinion sentence drawn from the appropriate text pool
     */
    public String buildOpinionText(String npcRole, String otherName, String otherRole,
                                   String subject, String victim, boolean isMurder) {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("otherName", otherName != null ? otherName : "");
        vars.put("subject",   subject   != null ? subject   : "");
        vars.put("victim",    victim    != null ? victim    : "");

        if (npcRole.startsWith("Subject") || npcRole.startsWith("Suspect")) {
            if (otherRole.startsWith("Client")) {
                return pickAndResolve("dynamic.opinion.subjectAboutClient", vars);
            }
            return pickAndResolve("dynamic.opinion.subjectAboutOther", vars);
        }
        if (otherName != null && otherName.equals(subject)) {
            return pickAndResolve("dynamic.opinion.aboutSubject", vars);
        }
        if (isMurder && otherName != null && otherName.equals(victim)) {
            return pickAndResolve("dynamic.opinion.aboutVictim", vars);
        }
        return pickAndResolve("dynamic.opinion.generic", vars);
    }

    /**
     * Returns a generic, non-committal opinion text shown when the player's
     * Empathy attribute does not meet the gate requirement.
     *
     * @param otherName name of the NPC being asked about
     * @return evasive answer sentence
     */
    public String buildOpinionAltText(String otherName) {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("otherName", otherName != null ? otherName : "");
        return pickAndResolve("dynamic.opinionAlt", vars);
    }

    /**
     * Generates a contact-info answer that includes the other NPC's phone
     * number and usual location.  Shown when the Charisma gate is met.
     *
     * @param otherName name of the NPC whose contact details are revealed
     * @param phone     phone number string from the NPC table
     * @param location  usual location string from the NPC table
     * @return answer sentence containing the phone and location details
     */
    public String buildContactInfoText(String otherName, String phone, String location) {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("otherName", otherName != null ? otherName : "");
        vars.put("phone",     phone     != null ? phone     : "");
        vars.put("location",  location  != null ? location  : "");
        return pickAndResolve("dynamic.contactInfo", vars);
    }

    /**
     * Returns a contact-info refusal text shown when the Charisma gate is
     * not met.
     *
     * @param otherName name of the NPC whose contact details were requested
     * @return refusal sentence
     */
    public String buildContactInfoAltText(String otherName) {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("otherName", otherName != null ? otherName : "");
        return pickAndResolve("dynamic.contactInfoAlt", vars);
    }

    // =========================================================================
    // Witness reliability helpers
    // =========================================================================

    /**
     * Computes the witness reliability score based on case complexity.
     *
     * <ul>
     *   <li><b>Complexity 1</b> — always 1.0 (fully reliable).</li>
     *   <li><b>Complexity 2</b> — 60 % chance of 1.0; otherwise 0.7–0.9.</li>
     *   <li><b>Complexity 3</b> — 40 % chance of 1.0; otherwise 0.5–0.8.</li>
     * </ul>
     *
     * @param complexity 1–3
     * @return reliability score between 0.5 and 1.0
     */
    float computeWitnessReliability(int complexity) {
        if (complexity <= 1) return InterviewScript.DEFAULT_RELIABILITY;
        if (complexity == 2) {
            // 60 % chance of fully reliable witness
            if (random.nextInt(10) < 6) return InterviewScript.DEFAULT_RELIABILITY;
            // 0.7 – 0.9
            return 0.7f + random.nextFloat() * 0.2f;
        }
        // complexity >= 3
        // 40 % chance of fully reliable witness
        if (random.nextInt(10) < 4) return InterviewScript.DEFAULT_RELIABILITY;
        // 0.5 – 0.8
        return 0.5f + random.nextFloat() * 0.3f;
    }

    /**
     * Returns {@code true} with probability equal to {@code reliability},
     * and {@code false} otherwise.  Used to decide whether a witness's
     * individual response is truthful or not.
     *
     * @param reliability the witness's reliability score (0.5–1.0)
     * @return {@code true} if this particular response should be truthful
     */
    private boolean reliableTruth(float reliability) {
        if (reliability >= InterviewScript.DEFAULT_RELIABILITY) return true;
        return random.nextFloat() < reliability;
    }

    // =========================================================================
    // Contradictory witness
    // =========================================================================

    /**
     * Builds a contradictory witness script that deliberately conflicts with
     * the primary witness on key topics: WHEREABOUTS, LAST_CONTACT, and
     * OBSERVATION.  On other topics the contradictory witness gives their own
     * independent (unreliable) answers.
     *
     * <p>The player must compare both witnesses' accounts and determine which
     * is accurate.
     *
     * @param script         the empty script to populate
     * @param type           case type
     * @param client         client name
     * @param subject        subject name
     * @param victim         victim name (may be empty)
     * @param targetPerson   primary target for questions
     * @param primaryWitness the primary witness's completed script; used to
     *                       generate contradicting responses
     */
    private void buildContradictoryWitnessInterview(InterviewScript script,
                                                     CaseType type,
                                                     String client,
                                                     String subject,
                                                     String victim,
                                                     String targetPerson,
                                                     InterviewScript primaryWitness) {
        boolean isMurder = type == CaseType.MURDER;
        Map<String, String> vars = vars(client, subject, victim, targetPerson, "", "");

        // ALIBI — own claim, unreliable
        script.addResponse(new InterviewResponse(InterviewTopic.ALIBI,
                resolveQuestion("alibi", vars),
                pickAndResolve("witness.alibi", vars), false, ""));

        // WHEREABOUTS — contradicts primary witness
        String whereaboutsText = buildContradiction(
                primaryWitness, InterviewTopic.WHEREABOUTS,
                "witness.subjectWhereabouts", vars, subject);
        script.addResponse(new InterviewResponse(InterviewTopic.WHEREABOUTS,
                resolveQuestion("witness.subjectWhereabouts", vars),
                whereaboutsText, false, subject));

        // OBSERVATION — contradicts primary witness
        String observationText = buildContradiction(
                primaryWitness, InterviewTopic.OBSERVATION,
                "witness.observation", vars, "");
        script.addResponse(new InterviewResponse(InterviewTopic.OBSERVATION,
                resolveQuestion("witness.observation", vars),
                observationText, false, ""));

        // LAST CONTACT — contradicts primary witness
        String lastContactText = buildContradiction(
                primaryWitness, InterviewTopic.LAST_CONTACT,
                "witness.lastContact", vars, targetPerson);
        script.addResponse(new InterviewResponse(InterviewTopic.LAST_CONTACT,
                resolveQuestion("witness.lastContact", vars),
                lastContactText, false, targetPerson));

        // RELATIONSHIP — own account (unreliable)
        script.addResponse(new InterviewResponse(InterviewTopic.RELATIONSHIP,
                resolveQuestion("witness.relationship", vars),
                pickAndResolve("witness.relationship", vars), false, targetPerson));

        // OPINION of subject
        script.addResponse(new InterviewResponse(InterviewTopic.OPINION,
                resolveQuestion("witness.opinion.subject", vars),
                pickAndResolve("witness.opinion.subject", vars), false, subject));

        // OPINION of victim (murder only)
        if (isMurder) {
            script.addResponse(new InterviewResponse(InterviewTopic.OPINION,
                    resolveQuestion("witness.opinion.victim", vars),
                    pickAndResolve("witness.opinion.victim", vars), false, victim));
        }

        // MOTIVE (murder only)
        if (isMurder) {
            script.addResponse(new InterviewResponse(InterviewTopic.MOTIVE,
                    resolveQuestion("witness.motive", vars),
                    pickAndResolve("witness.motive", vars), false, victim));
        }
    }

    /**
     * Builds a contradicting answer text for a specific topic.  If the
     * primary witness has a response on that topic, a fresh pick from the
     * same pool is drawn (re-rolling until it differs from the primary's
     * answer, up to 5 attempts).  If the primary has no such response, a
     * simple pick is returned.
     */
    private String buildContradiction(InterviewScript primaryWitness,
                                      InterviewTopic topic,
                                      String poolKey,
                                      Map<String, String> vars,
                                      String aboutNpc) {
        List<InterviewResponse> primaryResponses = primaryWitness.getResponsesByTopic(topic);
        String primaryAnswer = "";
        for (InterviewResponse r : primaryResponses) {
            if (aboutNpc.isEmpty() || aboutNpc.equals(r.getAboutNpcName())) {
                primaryAnswer = r.getAnswer();
                break;
            }
        }

        // Try up to 5 times to get a different answer from the same pool
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = pickAndResolve(poolKey, vars);
            if (!candidate.equals(primaryAnswer) || primaryAnswer.isEmpty()) {
                return candidate;
            }
        }
        // Fallback: just use whatever we drew
        return pickAndResolve(poolKey, vars);
    }
}
