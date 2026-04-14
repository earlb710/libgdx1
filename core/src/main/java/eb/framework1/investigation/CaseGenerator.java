package eb.framework1.investigation;

import eb.framework1.RandomUtils;
import eb.framework1.character.*;
import eb.framework1.generator.*;
import eb.framework1.phone.*;
import eb.framework1.popup.*;


import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
 *   <li>A hidden <em>complexity</em> value (1–3) controlling how many plot
 *       twists the case contains</li>
 *   <li>A four-level {@link CaseStoryNode} <em>story tree</em>:
 *       <ol>
 *         <li>{@link CaseStoryNode.NodeType#PLOT_TWIST} — one node per
 *             complexity level (initial phase + each twist)</li>
 *         <li>{@link CaseStoryNode.NodeType#MAJOR_PROGRESS} — two milestones
 *             per phase</li>
 *         <li>{@link CaseStoryNode.NodeType#MINOR_PROGRESS} — two sub-tasks
 *             per milestone</li>
 *         <li>{@link CaseStoryNode.NodeType#ACTION} (leaf) — two player
 *             actions per sub-task</li>
 *       </ol>
 *       Each sub-branch must be fully completed before the next one becomes
 *       available (see {@link CaseStoryNode#isChildAvailable(int)}).
 *   </li>
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
    private final InterviewTemplateEngine interviewEngine;
    private final CaseTemplateData    caseTemplateData;

    /**
     * Creates a generator with the given name source and a default {@link Random}.
     *
     * @param nameGen name generator used to produce client and subject names;
     *                must not be {@code null}
     */
    public CaseGenerator(PersonNameGenerator nameGen) {
        this(nameGen, new Random(), null, null);
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
        this(nameGen, random, null, null);
    }

    /**
     * Creates a generator with an explicit {@link Random} instance and
     * pre-loaded interview template data.
     *
     * @param nameGen      name generator; must not be {@code null}
     * @param random       random-number source; {@code null} is replaced by
     *                     a default {@code new Random()}
     * @param templateData interview text pools loaded from JSON; {@code null}
     *                     causes the engine to emit placeholder strings
     */
    public CaseGenerator(PersonNameGenerator nameGen, Random random,
                         InterviewTemplateData templateData) {
        this(nameGen, random, templateData, null);
    }

    /**
     * Creates a generator with all optional data sources.
     *
     * @param nameGen          name generator; must not be {@code null}
     * @param random           random-number source; {@code null} → default
     * @param templateData     interview text pools; {@code null} → placeholders
     * @param caseTemplateData case description/objective pools loaded from
     *                         {@code case_templates_en.json}; {@code null} →
     *                         built-in hardcoded templates
     */
    public CaseGenerator(PersonNameGenerator nameGen, Random random,
                         InterviewTemplateData templateData,
                         CaseTemplateData caseTemplateData) {
        if (nameGen == null) throw new IllegalArgumentException("nameGen must not be null");
        this.nameGen = nameGen;
        this.random  = random != null ? random : new Random();
        this.interviewEngine = new InterviewTemplateEngine(this.random, templateData);
        this.caseTemplateData = caseTemplateData;
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

        // For Murder cases, generate a distinct victim name; otherwise empty.
        String victimName = "";
        if (type == CaseType.MURDER) {
            String victimGender = randomGender();
            victimName = nameGen.generateFull(victimGender);
        }

        // --- Generate hidden personality traits for the key NPCs ---
        Map<String, Map<PersonalityTrait, Integer>> allTraits = new java.util.HashMap<>();
        allTraits.put(clientName, generateTraitMap());
        allTraits.put(subjectName, generateTraitMap());
        if (!victimName.isEmpty()) {
            allTraits.put(victimName, generateTraitMap());
        }

        // Determine complexity before description generation so templates can vary
        int complexity = 1 + random.nextInt(3);  // 1, 2, or 3

        // Build the description with trait-informed details about the subject
        Map<PersonalityTrait, Integer> subjectTraits = allTraits.get(subjectName);
        String description = capitalizeSentences(
                buildDescription(type, clientName, subjectName, victimName,
                        clientGender, subjectGender, random, complexity,
                        caseTemplateData)
                + " " + buildTraitColour(subjectName, subjectTraits, subjectGender));
        String objective   = buildObjective(type, clientName, subjectName, victimName,
                random, complexity, caseTemplateData);

        CaseFile cf = new CaseFile(type.getDisplayName() + ": " + subjectName,
                description, dateOpened != null ? dateOpened : "");
        cf.setCaseType(type);
        cf.setClientName(clientName);
        cf.setSubjectName(subjectName);
        cf.setVictimName(victimName);
        cf.setObjective(objective);
        cf.setComplexity(complexity);
        cf.setStoryRoot(buildStoryTree(type, cf.getComplexity(), subjectName));
        cf.setMeetingDialogue(buildMeetingDialogue(type, subjectName, objective, description,
                cf.getStoryRoot()));

        // Store personality traits on the case file so they feed into interviews
        for (Map.Entry<String, Map<PersonalityTrait, Integer>> e : allTraits.entrySet()) {
            cf.setNpcTraits(e.getKey(), e.getValue());
        }

        // Leads — standard + trait-driven + red herrings
        List<CaseLead> leads = buildLeads(type, subjectName, victimName, cf.getComplexity());
        addTraitDrivenLeads(leads, subjectName, subjectTraits);
        for (CaseLead lead : leads) {
            cf.addLead(lead);
        }

        cf.setInterviewScripts(
                buildInterviewScripts(type, clientName, subjectName, victimName,
                        clientGender, subjectGender));

        return cf;
    }

    // =========================================================================
    // Meeting dialogue generation
    // =========================================================================

    /**
     * Builds the pre-generated meeting dialogue for the client appointment.
     *
     * <p>The returned list always contains the four standard questions (objective,
     * description, timeline, contacts) with case-specific answers, followed by
     * <em>one entry per {@link CaseStoryNode.NodeType#PLOT_TWIST} phase</em> in
     * the story tree so that the player can ask about every major branch of the
     * investigation.
     *
     * @param type      case category
     * @param subject   name of the person being investigated
     * @param objective one-sentence summary of the player's goal
     * @param desc      narrative description of the problem
     * @param storyRoot root of the story tree (may be {@code null})
     * @return ordered list of question/answer pairs ready for use in {@link MeetPopup}
     */
    List<MeetingQA> buildMeetingDialogue(CaseType type, String subject,
                                         String objective, String desc,
                                         CaseStoryNode storyRoot) {
        List<MeetingQA> dialogue = new ArrayList<>();

        // --- Q1: What exactly do you need me to do? ---
        dialogue.add(new MeetingQA(
                "What exactly do you need me to do?",
                objective != null && !objective.isEmpty() ? objective
                        : "I need you to investigate " + subject + " and report what you find."));

        // --- Q2: Tell me more about the subject ---
        dialogue.add(new MeetingQA(
                "Tell me more about the subject.",
                desc != null && !desc.isEmpty() ? desc
                        : "The subject is " + subject + ". I don't have much else to share right now."));

        // --- Q3: How long has this been going on? ---
        dialogue.add(new MeetingQA(
                "How long has this been going on?",
                buildTimelineAnswer(type, subject)));

        // --- Q4: Is there anyone else who knows about this? ---
        dialogue.add(new MeetingQA(
                "Is there anyone else who knows about this?",
                buildContactsAnswer(type, subject)));

        // --- One entry per PLOT_TWIST phase (covers all story-tree branches) ---
        if (storyRoot != null) {
            for (CaseStoryNode phase : storyRoot.getChildren()) {
                if (phase.getNodeType() == CaseStoryNode.NodeType.PLOT_TWIST) {
                    String phaseTitle = phase.getTitle();
                    String phaseDesc  = phase.getDescription();
                    String question   = "What can you tell me about \"" + phaseTitle + "\"?";
                    String answer     = phaseDesc != null && !phaseDesc.isEmpty()
                            ? phaseDesc : "That phase of the investigation will become clearer as you dig in.";
                    dialogue.add(new MeetingQA(question, answer));
                }
            }
        }

        return dialogue;
    }

    /** Case-specific answer for the "How long has this been going on?" question. */
    private String buildTimelineAnswer(CaseType type, String subject) {
        switch (type) {
            case MISSING_PERSON:
                return subject + " was last seen three days ago. Every hour matters at this point.";
            case INFIDELITY:
                return "The behaviour I've noticed has been going on for at least two months, "
                        + "though I only started keeping records in the last few weeks.";
            case THEFT:
                return "The incident happened five days ago. I reported it to the police the same day.";
            case FRAUD:
                return "I started noticing irregularities about six months ago, "
                        + "but the discrepancies may go back further than that.";
            case BLACKMAIL:
                return "The first message arrived three weeks ago. They've been escalating since then.";
            case MURDER:
                return "The death was ruled accidental two months ago. I've been fighting the official "
                        + "account ever since.";
            case STALKING:
                return "I first noticed " + subject + " following me about six weeks ago. "
                        + "It's become more aggressive in the last fortnight.";
            case CORPORATE_ESPIONAGE:
                return "Our competitors started undercutting us suspiciously well about four months ago. "
                        + "That's when I started watching " + subject + " more closely.";
            default:
                return "It's been going on for a while — long enough that I knew I needed help.";
        }
    }

    /** Case-specific answer for the "Is there anyone else who knows about this?" question. */
    private String buildContactsAnswer(CaseType type, String subject) {
        switch (type) {
            case MISSING_PERSON:
                return subject + "'s closest friend knows I'm looking into it. "
                        + "The family asked me to keep things quiet until we know more.";
            case INFIDELITY:
                return "No one else knows. I haven't told friends or family — "
                        + "I didn't want to start rumours before I had proof.";
            case THEFT:
                return "My insurance company knows, and so does the officer who took my report. "
                        + "Beyond that, I've kept it to myself.";
            case FRAUD:
                return "My accountant noticed the discrepancies first and flagged it to me. "
                        + "I've told no one else — especially not anyone at the firm.";
            case BLACKMAIL:
                return "No one. That's why I came to you privately. "
                        + "If this gets out it could ruin me.";
            case MURDER:
                return "The victim's family suspects something is wrong. "
                        + "One of the detectives on the original case may be open to revisiting it.";
            case STALKING:
                return "My neighbour has seen " + subject + " outside my building. "
                        + "She'd be willing to make a statement if it comes to that.";
            case CORPORATE_ESPIONAGE:
                return "My legal team is aware there may be a leak. "
                        + "I've kept the specific suspicion about " + subject
                        + " restricted to myself and one trusted colleague.";
            default:
                return "Very few people. I prefer to keep this contained for now.";
        }
    }

    // -------------------------------------------------------------------------
    // Description templates
    // -------------------------------------------------------------------------

    /**
     * Generates a narrative case description for the given type and parties.
     *
     * <p>This method is also used by the admin tool to preview generated text
     * without constructing a full {@link CaseFile}.
     *
     * @param type          the case type
     * @param client        client (hiring party) name
     * @param subject       subject (investigated party) name
     * @param victim        victim name (used only for {@link CaseType#MURDER};
     *                      may be empty for other types)
     * @param clientGender  {@code "M"} or {@code "F"}
     * @param subjectGender {@code "M"} or {@code "F"}
     * @param rng           random source used to select among template variants
     * @return a multi-sentence narrative description
     */
    public static String buildDescription(CaseType type, String client, String subject,
                                    String victim, String clientGender,
                                    String subjectGender, Random rng) {
        if (rng == null) rng = new Random();
        String pronoun = "F".equals(subjectGender) ? "She" : "He";
        String pron    = pronoun.toLowerCase();
        switch (type) {
            case MISSING_PERSON:
                return RandomUtils.pick(rng,
                    client + " came in looking for answers. " + subject
                        + " vanished three days ago without a word."
                        + " No note, no call, nothing. " + pronoun
                        + " left behind a half-eaten meal and an unlocked door."
                        + " The police say it's too early to file a report.",
                    subject + " hasn't been seen since last Tuesday. " + client
                        + " is frantic — " + pron + " wouldn't just disappear."
                        + " A neighbour spotted someone fitting " + subject + "'s description"
                        + " getting into an unfamiliar car that evening.",
                    client + " last spoke to " + subject + " four days ago."
                        + " The call cut off mid-sentence and went straight to voicemail after that."
                        + " " + pronoun + " missed work, missed the weekend visit, missed everything."
                        + " Something is very wrong.");
            case INFIDELITY:
                return RandomUtils.pick(rng,
                    client + " suspects their partner, " + subject
                        + ", has been seeing someone else."
                        + " Late nights at the office, unexplained receipts,"
                        + " a phone that's always face-down."
                        + " They need the truth — one way or the other.",
                    subject + " has been distant for weeks. " + client
                        + " found a receipt for a restaurant " + pron + " doesn't remember."
                        + " The explanation didn't add up, and " + client
                        + " stopped asking questions. Now they want answers.",
                    "The relationship between " + client + " and " + subject
                        + " looked fine from the outside."
                        + " But text messages started arriving at odd hours,"
                        + " and a colleague mentioned seeing " + subject + " at a hotel across town."
                        + " " + client + " needs confirmation before they can act.");
            case THEFT:
                return RandomUtils.pick(rng,
                    client + " reported that " + subject
                        + " was seen near the premises on the night of the theft."
                        + " Valuables are missing. Insurance won't pay without a name.",
                    "Items went missing from " + client + "'s property over a two-week period."
                        + " A neighbour's security camera caught " + subject
                        + " on the street that night. Coincidence, or something more?",
                    client + " has been robbed before, but this feels different."
                        + " The thief knew exactly which room to check and which lock to bypass."
                        + " " + subject + "'s name keeps surfacing — " + pron
                        + " had access, means, and, it seems, motive.");
            case FRAUD:
                return RandomUtils.pick(rng,
                    client + " noticed irregularities in the accounts linked to "
                        + subject + "."
                        + " Money moved in small amounts, always just under the threshold."
                        + " Someone knows what they're doing.",
                    "The discrepancies in " + client + "'s books go back eight months."
                        + " A forensic accountant flagged a pattern of transfers to accounts connected to "
                        + subject + "."
                        + " Whoever did this was patient and methodical.",
                    subject + " joined the company six months before the losses began."
                        + " " + pronoun + " was charming, punctual, and well-liked."
                        + " " + client + " trusted " + pron + " completely."
                        + " That may have been the first mistake.");
            case BLACKMAIL:
                return RandomUtils.pick(rng,
                    client + " has been receiving anonymous messages."
                        + " The sender claims to have compromising material."
                        + " A name has come up: " + subject + "."
                        + " Could be a coincidence. Probably isn't.",
                    "Three envelopes. Three payments. No name."
                        + " Then " + client + " made a mistake and missed a deadline."
                        + " The next envelope contained a photograph."
                        + " " + subject + "'s address was stamped on the reverse.",
                    client + " calls it extortion. The letters call it 'fair compensation'."
                        + " The amounts are increasing. The threats are becoming specific."
                        + " " + subject + " is the only person who could have known what they know.");
            case MURDER:
                return RandomUtils.pick(rng,
                    "The client, " + client + ", doesn't believe the official story."
                        + " The coroner called it an accident, but the family of "
                        + "the victim, " + victim + ", called it murder."
                        + " " + subject + " was the last person seen with the victim.",
                    victim + " was found at the bottom of a staircase."
                        + " The inquest ruled accidental death."
                        + " " + client + " was at the funeral and noticed " + subject
                        + " leave before the eulogy finished."
                        + " No explanation. No condolences. Just gone.",
                    "A reopened case. A nervous client. A closed verdict that closed too fast."
                        + " " + client + " hired a solicitor who hit a wall."
                        + " Now they're here."
                        + " " + subject + " was questioned once and released. " + pronoun
                        + " had an alibi. " + client + " doesn't believe it.");
            case STALKING:
                return RandomUtils.pick(rng,
                    client + " has been followed, photographed, and harassed for weeks."
                        + " " + subject + " was identified by a neighbour."
                        + " " + client + " is scared. They need this stopped.",
                    "It started with notes slipped under the door. Then flowers."
                        + " Then a figure outside the window at 2 a.m."
                        + " The police took a report. Nothing happened."
                        + " " + subject + "'s name came up during their enquiries.",
                    client + " changed their phone number twice."
                        + " Changed their route to work. Changed their locks."
                        + " None of it worked. " + subject + " always found a way."
                        + " It's time to find out how.");
            case CORPORATE_ESPIONAGE:
                return RandomUtils.pick(rng,
                    client + "'s company has been bleeding trade secrets."
                        + " Competitor bids match their proposals almost word for word."
                        + " Internal suspicion has settled on " + subject + ".",
                    "Three product launches. Three leaks. Three contracts lost to a rival"
                        + " who shouldn't have known the details."
                        + " Someone inside " + client + "'s organisation is talking to that rival."
                        + " The trail leads to " + subject + ".",
                    "The legal team calls it a coincidence. The board calls it a pattern."
                        + " " + client + " calls it sabotage."
                        + " " + subject + " has had access to every document that was leaked."
                        + " The numbers don't lie.");
            default:
                return client + " needs information about " + subject + ".";
        }
    }

    /**
     * Backwards-compatible overload — equivalent to calling the overload with
     * a freshly created {@link Random}.
     */
    public static String buildDescription(CaseType type, String client, String subject,
                                    String victim, String clientGender,
                                    String subjectGender) {
        return buildDescription(type, client, subject, victim, clientGender, subjectGender,
                new Random());
    }

    /**
     * Generates a narrative case description using JSON template pools when
     * available, falling back to the built-in hardcoded templates when the
     * template data is {@code null} or lacks an entry for the requested type.
     *
     * <p>Templates may contain {@code $client}, {@code $subject},
     * {@code $victim}, {@code $pronounCap}, and {@code $pron} placeholders
     * which are resolved before returning.
     *
     * @param type             the case type
     * @param client           client name
     * @param subject          subject name
     * @param victim           victim name
     * @param clientGender     {@code "M"} or {@code "F"}
     * @param subjectGender    {@code "M"} or {@code "F"}
     * @param rng              random source
     * @param complexity       1, 2, or 3
     * @param caseTemplateData JSON template pools; may be {@code null}
     * @return a resolved multi-sentence narrative description
     */
    public static String buildDescription(CaseType type, String client, String subject,
                                    String victim, String clientGender,
                                    String subjectGender, Random rng,
                                    int complexity,
                                    CaseTemplateData caseTemplateData) {
        if (rng == null) rng = new Random();
        if (caseTemplateData != null) {
            String template = caseTemplateData.pickDescription(type.name(), complexity, rng);
            if (template != null) {
                return resolveCasePlaceholders(template, client, subject, victim,
                        clientGender, subjectGender);
            }
        }
        // Fallback to built-in hardcoded templates
        return buildDescription(type, client, subject, victim, clientGender, subjectGender, rng);
    }

    // -------------------------------------------------------------------------
    // Objective templates
    // -------------------------------------------------------------------------

    /**
     * Generates the case objective for the given type and parties.
     *
     * <p>This method is also used by the admin tool to preview generated text
     * without constructing a full {@link CaseFile}.
     *
     * @param type    the case type
     * @param client  client name
     * @param subject subject name
     * @param victim  victim name (used only for {@link CaseType#MURDER})
     * @param rng     random source used to select among template variants
     * @return the objective sentence(s)
     */
    public static String buildObjective(CaseType type, String client, String subject,
                                        String victim, Random rng) {
        if (rng == null) rng = new Random();
        switch (type) {
            case MISSING_PERSON:
                return RandomUtils.pick(rng,
                    "Locate " + subject + " and determine whether they left voluntarily"
                        + " or were taken against their will.",
                    "Find out where " + subject + " went after the last known sighting"
                        + " and establish whether foul play was involved.",
                    "Trace " + subject + "'s movements in the 48 hours before they disappeared"
                        + " and determine whether they are in danger.");
            case INFIDELITY:
                return RandomUtils.pick(rng,
                    "Confirm or disprove that " + subject
                        + " is conducting an undisclosed relationship,"
                        + " and if so, identify the other party.",
                    "Establish with documented evidence whether " + subject
                        + " has been meeting someone secretly, and determine who that person is.",
                    "Photograph or otherwise confirm any undisclosed contact between " + subject
                        + " and a third party, including times, locations, and identity.");
            case THEFT:
                return RandomUtils.pick(rng,
                    "Establish whether " + subject + " committed the theft and,"
                        + " where possible, locate the stolen property.",
                    "Gather sufficient evidence to confirm or rule out " + subject
                        + " as the perpetrator, and identify the current location of the stolen items.",
                    "Document " + subject + "'s movements on the night of the theft and"
                        + " establish whether they had access to the stolen property.");
            case FRAUD:
                return RandomUtils.pick(rng,
                    "Gather documented evidence of financial misconduct by "
                        + subject + " sufficient to support a formal complaint.",
                    "Build a paper trail linking " + subject + " to the irregular transactions"
                        + " and identify any co-conspirators or beneficiaries.",
                    "Confirm that the financial irregularities originate with " + subject
                        + " and obtain records that demonstrate the scale and duration of the scheme.");
            case BLACKMAIL:
                return RandomUtils.pick(rng,
                    "Identify whether " + subject + " is the source of the blackmail"
                        + " and obtain evidence that can be handed to the authorities.",
                    "Confirm " + subject + "'s identity as the blackmailer and locate the"
                        + " compromising material being used as leverage.",
                    "Establish proof of " + subject + "'s involvement in the blackmail scheme"
                        + " and uncover how they obtained the information.");
            case MURDER:
                return RandomUtils.pick(rng,
                    "Uncover what " + subject + " knows about the death of "
                        + victim + " and find evidence that contradicts the official findings.",
                    "Establish whether " + subject + " had means, motive, and opportunity"
                        + " to kill " + victim + ", and gather evidence to reopen the inquest.",
                    "Prove that " + victim + "'s death was not accidental and identify"
                        + " " + subject + "'s role in what took place.");
            case STALKING:
                return RandomUtils.pick(rng,
                    "Document " + subject + "'s surveillance activities against "
                        + client + " and gather evidence for a restraining order or police report.",
                    "Establish a pattern of harassing behaviour by " + subject
                        + " and compile a dossier suitable for legal action.",
                    "Confirm " + subject + "'s identity as the stalker and produce dated evidence"
                        + " of repeated unwanted contact with " + client + ".");
            case CORPORATE_ESPIONAGE:
                return RandomUtils.pick(rng,
                    "Prove that " + subject + " has been passing confidential information"
                        + " to a competitor and identify who they are working with.",
                    "Establish the method by which " + subject + " has been extracting"
                        + " trade secrets and identify the recipient organisation.",
                    "Document at least one specific instance of information transfer by "
                        + subject + " and link it to the resulting competitive advantage.");
            default:
                return "Investigate " + subject + " and report the findings.";
        }
    }

    /**
     * Backwards-compatible overload — equivalent to calling the overload with
     * a freshly created {@link Random}.
     */
    public static String buildObjective(CaseType type, String client, String subject,
                                        String victim) {
        return buildObjective(type, client, subject, victim, new Random());
    }

    /**
     * Generates the case objective using JSON template pools when available,
     * falling back to the built-in hardcoded templates otherwise.
     *
     * @param type             the case type
     * @param client           client name
     * @param subject          subject name
     * @param victim           victim name
     * @param rng              random source
     * @param complexity       1, 2, or 3
     * @param caseTemplateData JSON template pools; may be {@code null}
     * @return resolved objective sentence(s)
     */
    public static String buildObjective(CaseType type, String client, String subject,
                                        String victim, Random rng,
                                        int complexity,
                                        CaseTemplateData caseTemplateData) {
        if (rng == null) rng = new Random();
        if (caseTemplateData != null) {
            String template = caseTemplateData.pickObjective(type.name(), complexity, rng);
            if (template != null) {
                return resolveCasePlaceholders(template, client, subject, victim, null, null);
            }
        }
        return buildObjective(type, client, subject, victim, rng);
    }

    // -------------------------------------------------------------------------
    // Placeholder resolution for case templates
    // -------------------------------------------------------------------------

    /**
     * Resolves {@code $client}, {@code $subject}, {@code $victim},
     * {@code $pronounCap}, and {@code $pron} placeholders in a template
     * string.  Gender-based tokens are derived from {@code subjectGender}.
     */
    static String resolveCasePlaceholders(String template, String client, String subject,
                                          String victim, String clientGender,
                                          String subjectGender) {
        String result = template;
        result = result.replace("$client",  client  != null ? client  : "the client");
        result = result.replace("$subject", subject != null ? subject : "the subject");
        result = result.replace("$victim",  victim  != null ? victim  : "the victim");
        if (subjectGender != null) {
            String cap = "F".equals(subjectGender) ? "She" : "He";
            String low = cap.toLowerCase();
            result = result.replace("$pronounCap", cap);
            result = result.replace("$pron", low);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Lead generation (delegates to LeadDataRepository)
    // -------------------------------------------------------------------------

    private List<CaseLead> buildLeads(CaseType type, String subject,
                                      String victim, int complexity) {
        List<CaseLead> leads = LeadDataRepository.buildLeads(type, subject, victim);
        // Red herring leads: complexity 1 = 0 red herrings, 2 = 1, 3 = 2
        int redHerringCount = complexity - 1;
        if (redHerringCount > 0) {
            addRedHerringLeads(leads, subject, redHerringCount);
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
        return RandomUtils.randomGender(random);
    }

    /**
     * Ensures the first character of the text is upper-case.
     * Returns the text unchanged if it is {@code null}, empty, or already
     * starts with a capital letter.
     */
    public static String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        char first = text.charAt(0);
        if (Character.isUpperCase(first)) return text;
        return Character.toUpperCase(first) + text.substring(1);
    }

    /**
     * Capitalises the first letter of every sentence in the text.
     * A sentence boundary is defined as a period followed by a space
     * and then a lowercase letter.
     */
    public static String capitalizeSentences(String text) {
        if (text == null || text.isEmpty()) return text;
        StringBuilder sb = new StringBuilder(text.length());
        sb.append(Character.toUpperCase(text.charAt(0)));
        for (int i = 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (i >= 2 && text.charAt(i - 1) == ' ' && text.charAt(i - 2) == '.') {
                sb.append(Character.toUpperCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // =========================================================================
    // Personality-trait helpers
    // =========================================================================

    /**
     * Generates a random set of 3–5 personality traits (non-zero, −3 to +3)
     * as an {@link EnumMap}.  Mirrors the algorithm in
     * {@link eb.framework1.character.CharacterGenerator#assignRandomPersonalityTraits}.
     */
    private Map<PersonalityTrait, Integer> generateTraitMap() {
        PersonalityTrait[] all = PersonalityTrait.values();
        int count = 3 + random.nextInt(3); // 3–5
        PersonalityTrait[] shuffled = all.clone();
        for (int i = 0; i < count && i < shuffled.length; i++) {
            int j = i + random.nextInt(shuffled.length - i);
            PersonalityTrait tmp = shuffled[i];
            shuffled[i] = shuffled[j];
            shuffled[j] = tmp;
        }
        Map<PersonalityTrait, Integer> map = new EnumMap<>(PersonalityTrait.class);
        for (int i = 0; i < count && i < shuffled.length; i++) {
            int value = 1 + random.nextInt(3);
            if (random.nextBoolean()) value = -value;
            map.put(shuffled[i], value);
        }
        return map;
    }

    /**
     * Returns a one-to-two-sentence "colour" passage about the subject based
     * on their personality traits.  This text is appended to the case
     * description to give the player indirect clues about motive and behaviour.
     *
     * <p>If the subject has no notable traits the method returns an empty string.
     */
    private String buildTraitColour(String subject, Map<PersonalityTrait, Integer> traits,
                                    String gender) {
        if (traits == null || traits.isEmpty()) return "";

        String pronoun = "F".equals(gender) ? "she" : "he";
        String pronounCap = capitalize(pronoun);

        // Collect strong positive and strong negative traits
        List<String> likes  = new ArrayList<>();
        List<String> dislikes = new ArrayList<>();
        for (Map.Entry<PersonalityTrait, Integer> e : traits.entrySet()) {
            PersonalityTrait t = e.getKey();
            int v = e.getValue();
            if (v >= 2)  likes.add(t.getDisplayName().toLowerCase());
            if (v <= -2) dislikes.add(t.getDisplayName().toLowerCase());
        }

        StringBuilder sb = new StringBuilder();
        if (!likes.isEmpty()) {
            sb.append(pronounCap).append(" is known to be passionate about ")
              .append(joinWithAnd(likes)).append(".");
        }
        if (!dislikes.isEmpty()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append("Neighbours say ").append(pronoun).append(" has a strong aversion to ")
              .append(joinWithAnd(dislikes)).append(".");
        }
        return sb.toString();
    }

    /**
     * Adds 1–2 leads derived from the subject's personality traits.
     * These leads hint at locations or behaviours the player should investigate
     * but don't reveal the trait directly — the player still needs interviews.
     */
    private void addTraitDrivenLeads(List<CaseLead> leads,
                                     String subject,
                                     Map<PersonalityTrait, Integer> traits) {
        if (traits == null || traits.isEmpty()) return;

        int nextIndex = leads.size() + 1;
        int added = 0;
        int maxLeads = 1 + random.nextInt(2); // 1–2

        for (Map.Entry<PersonalityTrait, Integer> e : traits.entrySet()) {
            if (added >= maxLeads) break;
            PersonalityTrait trait = e.getKey();
            int value = e.getValue();
            if (Math.abs(value) < 2) continue; // only notable traits

            String location = traitLocation(trait);
            String behaviour = traitBehaviour(trait, value, subject);
            if (location == null || behaviour == null) continue;

            String desc = behaviour + " Visiting " + location
                    + " may provide further insight into " + subject + "'s activities.";
            String hint = "Someone matching " + subject + "'s description has been seen near "
                    + location + " on several occasions.";

            leads.add(lead(nextIndex++, desc, hint, DiscoveryMethod.SURVEILLANCE));
            added++;
        }
    }

    /** Returns a plausible location associated with the given trait. */
    private String traitLocation(PersonalityTrait trait) {
        switch (trait) {
            case SPORTS:      return "the gym on Main Street";
            case HIKING:      return "the trailhead near the reservoir";
            case COOKING:     return "the farmers' market on weekends";
            case READING:     return "the public library downtown";
            case MUSIC:       return "the live-music venue on 5th Avenue";
            case ART:         return "the gallery district";
            case GAMBLING:    return "the back-room card game off Vine Street";
            case TRAVEL:      return "the bus station";
            case FLIRTING:    return "the bar on George Street";
            case SOCIALIZING: return "the community centre";
            case GOSSIP:      return "the café on the corner of High Street";
            case SOLITUDE:    return "an isolated cabin outside town";
            case RISK_TAKING: return "the underground fight club";
            case AUTHORITY:   return "the magistrate's office";
            case WEALTH:      return "the private club on Regent Row";
            case ANIMALS:     return "the local animal shelter";
            default:          return null;
        }
    }

    /** Returns a behaviour snippet for a trait-driven lead. */
    private static String traitBehaviour(PersonalityTrait trait, int value,
                                          String subject) {
        boolean positive = value > 0;
        switch (trait) {
            case SPORTS:
                return positive
                        ? subject + " has been spotted exercising regularly at odd hours."
                        : subject + " actively avoids athletic venues, which narrows down possible meeting spots.";
            case HIKING:
                return positive
                        ? subject + " was seen carrying outdoor gear on multiple occasions."
                        : subject + " has never been seen near any trail or park.";
            case GAMBLING:
                return positive
                        ? "Financial irregularities suggest " + subject + " may have a gambling habit."
                        : subject + " is known to avoid games of chance entirely.";
            case FLIRTING:
                return positive
                        ? subject + " has a reputation for flirtatious behaviour in local bars."
                        : subject + " tends to keep social interactions strictly professional.";
            case WEALTH:
                return positive
                        ? subject + " has expensive tastes that don't match their declared income."
                        : subject + " lives modestly despite apparent means.";
            case RISK_TAKING:
                return positive
                        ? subject + " has a history of reckless decisions and high-stakes behaviour."
                        : subject + " is methodical and risk-averse, suggesting careful planning.";
            case SOLITUDE:
                return positive
                        ? subject + " spends long periods alone and avoids social gatherings."
                        : subject + " is rarely seen without company.";
            default:
                return subject + " appears to have strong feelings about "
                        + trait.getDisplayName().toLowerCase() + ".";
        }
    }

    /** Joins a list of strings with commas and "and" before the last element. */
    private static String joinWithAnd(List<String> items) {
        if (items.size() == 1) return items.get(0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0 && i == items.size() - 1) sb.append(" and ");
            else if (i > 0) sb.append(", ");
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    /** Pool of generic red-herring leads that can apply to any case type. */
    private static final String[][] RED_HERRING_POOL = {
            // { description, hint, discoveryMethod name }
            {"A former associate of {s} was seen in the area around the time of the incident,"
                    + " but further investigation reveals they were there for unrelated reasons.",
             "A known associate of the subject was reportedly nearby.",
             "INTERVIEW"},

            {"Financial records show {s} made a large cash withdrawal recently,"
                    + " but it turns out to be an unrelated personal expense.",
             "Unusual financial activity by the subject warrants closer inspection.",
             "DOCUMENTS"},

            {"A witness claims to have seen {s} acting suspiciously near the scene,"
                    + " but their account does not hold up under scrutiny.",
             "An eyewitness may have information about the subject's movements.",
             "INTERVIEW"},

            {"Anonymous tip suggests {s} has connections to organised crime,"
                    + " but the source proves unreliable and the link is fabricated.",
             "An anonymous source has provided information linking the subject to criminal activity.",
             "BACKGROUND_CHECK"},

            {"A surveillance camera captured someone resembling {s} at a key location,"
                    + " but enhanced footage confirms it was a different person.",
             "CCTV footage from the area may show the subject at a relevant time.",
             "SURVEILLANCE"},

            {"Phone records reveal {s} called an unlisted number repeatedly,"
                    + " but the number belongs to an innocent party with no connection to the case.",
             "The subject's phone records contain calls to an unidentified number.",
             "DOCUMENTS"},

            {"Forensic traces found at a secondary location initially point to {s},"
                    + " but lab analysis rules them out as a match.",
             "Physical evidence recovered from a second site may tie the subject to the case.",
             "FORENSICS"},

            {"A neighbour reports hearing a heated argument involving {s} the night before,"
                    + " but the argument was about an entirely different matter.",
             "Neighbours may have overheard something relevant.",
             "INTERVIEW"},
    };

    /**
     * Appends the specified number of red-herring leads to the given list.
     * Each red herring looks plausible but ultimately leads nowhere.
     *
     * @param leads   mutable list to append to
     * @param subject the subject name (substituted for {@code {s}})
     * @param count   how many red herrings to add (should be &gt; 0)
     */
    private void addRedHerringLeads(List<CaseLead> leads, String subject, int count) {
        // Start lead numbering after existing leads
        int nextIndex = leads.size() + 1;

        // Build a shuffled index array so we don't repeat
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < RED_HERRING_POOL.length; i++) indices.add(i);
        for (int i = indices.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = indices.get(i);
            indices.set(i, indices.get(j));
            indices.set(j, tmp);
        }

        int added = 0;
        for (int idx : indices) {
            if (added >= count) break;
            String[] entry = RED_HERRING_POOL[idx];
            String desc = entry[0].replace("{s}", subject);
            String hint = entry[1].replace("{s}", subject);
            DiscoveryMethod method = DiscoveryMethod.valueOf(entry[2]);
            leads.add(lead(nextIndex++, desc, hint, method));
            added++;
        }
    }

    // =========================================================================
    // Interview script generation (delegates to InterviewTemplateEngine)
    // =========================================================================

    /**
     * Builds pre-generated interview scripts for the key NPCs in the case.
     * Delegates to {@link InterviewTemplateEngine}.
     */
    List<InterviewScript> buildInterviewScripts(CaseType type,
                                                String client, String subject,
                                                String victim,
                                                String clientGender,
                                                String subjectGender) {
        return interviewEngine.buildAll(type, client, subject, victim,
                clientGender, subjectGender, nameGen);
    }

    // =========================================================================
    // Story tree generation
    // =========================================================================

    /**
     * Builds the four-level story-progression tree for the given case.
     *
     * <p>Structure per generated case:
     * <pre>
     *   ROOT
     *   └─ PLOT_TWIST  (one per complexity level)
     *        └─ MAJOR_PROGRESS  (×2 per phase)
     *             └─ MINOR_PROGRESS  (×2 per major)
     *                  └─ ACTION  (×2 per minor — leaf nodes)
     * </pre>
     *
     * @param type       case category, used to select narrative templates
     * @param complexity number of phases (1–3)
     * @param subject    subject name, substituted into node text
     * @return the ROOT node containing all phase sub-trees
     */
    private CaseStoryNode buildStoryTree(CaseType type, int complexity, String subject) {
        CaseStoryNode root = sn("story", type.getDisplayName() + " — Case Story",
                "Story-progression tree for this investigation.",
                CaseStoryNode.NodeType.ROOT);
        String[][] data = STORY_DATA[type.ordinal()];
        for (int p = 0; p < complexity && p < data.length; p++) {
            root.addChild(buildPhase(data[p], subject, "p" + p));
        }
        return root;
    }

    /**
     * Constructs a single PLOT_TWIST phase node from a 24-element string
     * array.  This is a pure utility method with no dependency on instance
     * state; it can safely be called in any order.
     *
     * <p>The array layout is:
     * <pre>
     * [0]  phase title         [1]  phase description
     * [2]  major-0 title
     * [3]  minor-0-0 title
     * [4]  action-0-0-0 title  [5]  action-0-0-0 description
     * [6]  action-0-0-1 title  [7]  action-0-0-1 description
     * [8]  minor-0-1 title
     * [9]  action-0-1-0 title  [10] action-0-1-0 description
     * [11] action-0-1-1 title  [12] action-0-1-1 description
     * [13] major-1 title
     * [14] minor-1-0 title
     * [15] action-1-0-0 title  [16] action-1-0-0 description
     * [17] action-1-0-1 title  [18] action-1-0-1 description
     * [19] minor-1-1 title
     * [20] action-1-1-0 title  [21] action-1-1-0 description
     * [22] action-1-1-1 title  [23] action-1-1-1 description
     * </pre>
     * All entries support the {@code {s}} placeholder which is replaced with
     * the subject name.
     */
    private static CaseStoryNode buildPhase(String[] raw, String subject, String prefix) {
        String[] d = new String[raw.length];
        for (int i = 0; i < raw.length; i++) d[i] = raw[i].replace("{s}", subject);

        CaseStoryNode phase = sn(prefix,       d[0],  d[1],  CaseStoryNode.NodeType.PLOT_TWIST);

        CaseStoryNode m0    = sn(prefix+"-m0", d[2],  "",    CaseStoryNode.NodeType.MAJOR_PROGRESS);
        CaseStoryNode m0n0  = sn(prefix+"-m0n0", d[3], "",   CaseStoryNode.NodeType.MINOR_PROGRESS);
        m0n0.addChild(sn(prefix+"-m0n0a0", d[4],  d[5],  CaseStoryNode.NodeType.ACTION));
        m0n0.addChild(sn(prefix+"-m0n0a1", d[6],  d[7],  CaseStoryNode.NodeType.ACTION));
        CaseStoryNode m0n1  = sn(prefix+"-m0n1", d[8], "",   CaseStoryNode.NodeType.MINOR_PROGRESS);
        m0n1.addChild(sn(prefix+"-m0n1a0", d[9],  d[10], CaseStoryNode.NodeType.ACTION));
        m0n1.addChild(sn(prefix+"-m0n1a1", d[11], d[12], CaseStoryNode.NodeType.ACTION));
        m0.addChild(m0n0); m0.addChild(m0n1);

        CaseStoryNode m1    = sn(prefix+"-m1", d[13], "",    CaseStoryNode.NodeType.MAJOR_PROGRESS);
        CaseStoryNode m1n0  = sn(prefix+"-m1n0", d[14], "",  CaseStoryNode.NodeType.MINOR_PROGRESS);
        m1n0.addChild(sn(prefix+"-m1n0a0", d[15], d[16], CaseStoryNode.NodeType.ACTION));
        m1n0.addChild(sn(prefix+"-m1n0a1", d[17], d[18], CaseStoryNode.NodeType.ACTION));
        CaseStoryNode m1n1  = sn(prefix+"-m1n1", d[19], "",  CaseStoryNode.NodeType.MINOR_PROGRESS);
        m1n1.addChild(sn(prefix+"-m1n1a0", d[20], d[21], CaseStoryNode.NodeType.ACTION));
        m1n1.addChild(sn(prefix+"-m1n1a1", d[22], d[23], CaseStoryNode.NodeType.ACTION));
        m1.addChild(m1n0); m1.addChild(m1n1);

        phase.addChild(m0); phase.addChild(m1);
        return phase;
    }

    private static CaseStoryNode sn(String id, String title, String desc,
                                    CaseStoryNode.NodeType type) {
        return new CaseStoryNode(id, title, desc, type);
    }

    // =========================================================================
    // Story template data   STORY_DATA[caseTypeOrdinal][phaseIndex][stringIndex]
    //
    // Each phase is a String[24] — see buildPhase() for the index layout.
    // Use {s} as a placeholder for the subject's name.
    //
    // CaseType ordinals:
    //   0 MISSING_PERSON  1 INFIDELITY  2 THEFT          3 FRAUD
    //   4 BLACKMAIL       5 MURDER      6 STALKING        7 CORPORATE_ESPIONAGE
    // =========================================================================
    private static final String[][][] STORY_DATA = {

        // ---- 0: MISSING_PERSON -----------------------------------------------
        {
            // Phase 0 — Initial Investigation
            {
                "Initial Investigation",
                "Locate {s} and establish what happened to them.",
                // major 0
                "Establish the Facts",
                // minor 0-0
                "Client Briefing",
                "Interview the client",
                "Take a detailed statement from the client about the disappearance of {s}.",
                "Review personal effects",
                "Examine items left behind by {s} for clues about their state of mind.",
                // minor 0-1
                "Scene Assessment",
                "Visit the last known location",
                "Document the scene where {s} was last seen.",
                "Photograph the scene",
                "Record any physical evidence at the location before it is disturbed.",
                // major 1
                "Trace Movements",
                // minor 1-0
                "Digital Trail",
                "Obtain phone records",
                "Retrieve call and message logs from {s}'s carrier.",
                "Check online accounts",
                "Review {s}'s recent activity on social media and email.",
                // minor 1-1
                "Physical Trail",
                "Review local CCTV",
                "Obtain and examine surveillance footage from the area around {s}'s last location.",
                "Canvass for witnesses",
                "Speak with residents and businesses near {s}'s last sighting."
            },
            // Phase 1 — Plot twist: Body Found → Murder Investigation
            {
                "Body Found — Murder Investigation",
                "A body matching {s}'s description has been found. The investigation pivots from missing person to homicide.",
                // major 0
                "Secure the Crime Scene",
                // minor 0-0
                "Scene Documentation",
                "Attend the discovery site",
                "Examine and document the location where {s}'s body was found.",
                "Request a forensic sweep",
                "Arrange forensic technicians to process the scene for evidence.",
                // minor 0-1
                "Post-Mortem Coordination",
                "Attend the autopsy",
                "Observe the post-mortem examination to gain first-hand forensic insight.",
                "Obtain the autopsy report",
                "Collect the official medical findings on cause and time of death.",
                // major 1
                "Identify a Suspect",
                // minor 1-0
                "Suspect Identification",
                "Re-interview the client",
                "Return to the client with the new information and note any changes in their account.",
                "Cross-reference associates",
                "Map known relationships between {s} and potential suspects.",
                // minor 1-1
                "Build the Murder Case",
                "Compile a timeline",
                "Assemble a chronological account of {s}'s final hours.",
                "Confront the primary suspect",
                "Approach the primary suspect with the evidence gathered so far."
            },
            // Phase 2 — Plot twist 2: Cover-Up Exposed
            {
                "Cover-Up Exposed",
                "Evidence reveals a deliberate effort to conceal the circumstances of {s}'s death.",
                // major 0
                "Expose the Conspiracy",
                // minor 0-0
                "Follow the Money",
                "Audit financial records",
                "Trace unusual payments or transactions linked to the case.",
                "Identify the beneficiary",
                "Determine who stands to gain from keeping the truth hidden.",
                // minor 0-1
                "Locate Silenced Witnesses",
                "Find suppressed witnesses",
                "Track down individuals who were warned or threatened into silence.",
                "Secure testimony",
                "Obtain a protected statement from a witness prepared to speak out.",
                // major 1
                "Deliver Justice",
                // minor 1-0
                "Compile Final Evidence",
                "Organise the case file",
                "Assemble all evidence into a coherent, presentable package.",
                "Verify chain of custody",
                "Confirm that all physical evidence was handled correctly and is admissible.",
                // minor 1-1
                "Resolution",
                "Submit to authorities",
                "Hand the complete case file to the relevant law-enforcement authority.",
                "Report to the client",
                "Deliver a full account of the findings and outcome to the client."
            }
        },

        // ---- 1: INFIDELITY ---------------------------------------------------
        {
            // Phase 0 — Verify the Suspicion
            {
                "Verify the Suspicion",
                "Determine whether {s} is involved in an undisclosed relationship.",
                // major 0
                "Establish a Pattern",
                // minor 0-0
                "Initial Surveillance",
                "Map {s}'s schedule",
                "Record {s}'s regular movements and note any unexplained absences.",
                "Identify unusual venues",
                "Note any locations {s} visits outside their stated routine.",
                // minor 0-1
                "Document the Behaviour",
                "Photograph meetings",
                "Capture photographic evidence of {s} meeting with the other party.",
                "Record dates and times",
                "Log a detailed timeline of all observed meetings and interactions.",
                // major 1
                "Corroborate the Evidence",
                // minor 1-0
                "Financial Records",
                "Trace hotel receipts",
                "Identify accommodation charges that cannot be explained legitimately.",
                "Check card transactions",
                "Review unexplained card transactions linked to {s}'s accounts.",
                // minor 1-1
                "Witness Corroboration",
                "Interview a mutual contact",
                "Speak with someone who knows both {s} and the other party.",
                "Confirm the second party's identity",
                "Establish a full identity for the person {s} is meeting."
            },
            // Phase 1 — Plot twist: Second Relationship Revealed
            {
                "Second Relationship Revealed",
                "Evidence confirms {s} has been maintaining a second relationship. The client must be informed and further proof gathered.",
                // major 0
                "Deepen the Investigation",
                // minor 0-0
                "Gather Hard Evidence",
                "Obtain accommodation records",
                "Secure documentary evidence of shared stays between {s} and the other party.",
                "Recover deleted messages",
                "Work with a digital-forensics contact to retrieve deleted communications.",
                // minor 0-1
                "Establish Frequency",
                "Extend surveillance",
                "Continue monitoring to establish how long the relationship has been ongoing.",
                "Document habitual patterns",
                "Record repeated locations, times, and behaviours forming an undeniable pattern.",
                // major 1
                "Compile the Report",
                // minor 1-0
                "Client Debriefing",
                "Conduct an interim briefing",
                "Share findings with the client and prepare them for the full report.",
                "Advise on next steps",
                "Discuss the client's options in light of the evidence gathered.",
                // minor 1-1
                "Prepare the Evidence Pack",
                "Assemble the evidence file",
                "Compile all photographs, documents, and records into a single file.",
                "Finalise the report",
                "Prepare the formal written report for delivery to the client."
            },
            // Phase 2 — Plot twist 2: Financial Fraud Discovered
            {
                "Financial Fraud Uncovered",
                "The investigation reveals {s} has also been concealing financial misconduct, compounding the betrayal.",
                // major 0
                "Follow the Financial Trail",
                // minor 0-0
                "Hidden Accounts",
                "Identify undisclosed accounts",
                "Locate bank or investment accounts that {s} has not disclosed.",
                "Trace asset transfers",
                "Map any movement of shared assets into accounts controlled solely by {s}.",
                // minor 0-1
                "Document the Deception",
                "Gather financial statements",
                "Collect statements covering the period of suspected fraud.",
                "Cross-reference with income",
                "Compare {s}'s financial activity against their known income sources.",
                // major 1
                "Legal and Client Resolution",
                // minor 1-0
                "Engage Legal Support",
                "Brief a financial solicitor",
                "Provide findings to a legal professional specialising in financial disputes.",
                "Advise on asset protection",
                "Help the client understand immediate steps to protect their financial position.",
                // minor 1-1
                "Close the Case",
                "Submit the financial report",
                "Deliver the full financial findings alongside the original infidelity report.",
                "Final client meeting",
                "Meet the client to present all findings and confirm the case is complete."
            }
        },

        // ---- 2: THEFT --------------------------------------------------------
        {
            // Phase 0 — Initial Investigation
            {
                "Initial Investigation",
                "Establish whether {s} is responsible for the theft and locate the stolen property.",
                // major 0
                "Gather Primary Evidence",
                // minor 0-0
                "Crime Scene Examination",
                "Inspect the entry point",
                "Examine how access was gained and look for trace evidence.",
                "Document missing items",
                "Compile a precise inventory of what was taken, with descriptions and values.",
                // minor 0-1
                "Witness Accounts",
                "Interview staff or occupants",
                "Speak with anyone present at the premises around the time of the theft.",
                "Check for CCTV",
                "Identify and obtain footage from cameras covering the premises.",
                // major 1
                "Build a Case Against the Suspect",
                // minor 1-0
                "Background Check",
                "Research {s}'s record",
                "Pull prior offence history and known associates for {s}.",
                "Trace {s}'s movements",
                "Establish where {s} was on the night of the theft.",
                // minor 1-1
                "Forensic Evidence",
                "Submit prints for matching",
                "Have any lifted fingerprints matched against {s}'s prints on file.",
                "Secure forensic report",
                "Obtain the laboratory's written report on all physical evidence examined."
            },
            // Phase 1 — Plot twist: Organised Ring Uncovered
            {
                "Organised Ring Uncovered",
                "Evidence indicates {s} is not acting alone — a wider theft ring is involved.",
                // major 0
                "Map the Network",
                // minor 0-0
                "Identify Associates",
                "Surveil {s}'s contacts",
                "Monitor known associates of {s} to identify the broader group.",
                "Trace the stolen goods",
                "Follow the fencing chain to identify others profiting from the theft.",
                // minor 0-1
                "Link to Prior Offences",
                "Review unsolved cases",
                "Cross-reference the method with unsolved thefts in the area.",
                "Identify a pattern",
                "Document recurring methods, locations, and timing suggesting organised activity.",
                // major 1
                "Build the Full Case",
                // minor 1-0
                "Accumulate Evidence",
                "Photograph the ring in operation",
                "Use surveillance to capture network members in the act.",
                "Recover stolen property",
                "Locate and document items from the original theft now in others' possession.",
                // minor 1-1
                "Compile for Prosecution",
                "Organise network evidence",
                "Assemble all evidence relating to all members of the ring.",
                "Submit to police",
                "Hand the complete file to police for potential prosecution."
            },
            // Phase 2 — Plot twist 2: Inside Job Revealed
            {
                "Inside Job Revealed",
                "The investigation reveals the theft was facilitated by someone with insider access.",
                // major 0
                "Identify the Insider",
                // minor 0-0
                "Audit Access Records",
                "Review key and access logs",
                "Determine who had legitimate access to the premises at the relevant time.",
                "Check staff communications",
                "Look for contact between an insider and {s} in the period before the theft.",
                // minor 0-1
                "Establish the Insider's Role",
                "Document the insider's involvement",
                "Establish exactly how the insider assisted {s} in committing the theft.",
                "Secure witness testimony",
                "Obtain a statement from a colleague who observed suspicious behaviour.",
                // major 1
                "Resolve the Case",
                // minor 1-0
                "Confront the Insider",
                "Present the evidence",
                "Confront the insider with the full evidence of their involvement.",
                "Obtain a statement",
                "Secure an admission or formal statement for use in proceedings.",
                // minor 1-1
                "Final Report",
                "Compile the full report",
                "Assemble all evidence, statements, and findings into a final report.",
                "Report to the client",
                "Present findings to the client and advise on civil or criminal options."
            }
        },

        // ---- 3: FRAUD --------------------------------------------------------
        {
            // Phase 0 — Audit the Irregularities
            {
                "Audit the Irregularities",
                "Gather evidence of the financial misconduct attributed to {s}.",
                // major 0
                "Financial Records Review",
                // minor 0-0
                "Obtain Account Statements",
                "Request financial records",
                "Obtain full account statements covering the suspected fraud period.",
                "Identify irregular transactions",
                "Flag all transactions by {s} that appear inconsistent with legitimate activity.",
                // minor 0-1
                "Corporate Structure Investigation",
                "Search for shell companies",
                "Look for dormant or nominee companies connected to {s}.",
                "Trace money flows",
                "Map the path of funds from the victim's accounts to {s}'s control.",
                // major 1
                "Interview Key Contacts",
                // minor 1-0
                "Insiders and Colleagues",
                "Approach the accountant",
                "Speak with accounting staff who may be aware of the manipulation.",
                "Interview business partners",
                "Determine what {s}'s associates know about the suspect transactions.",
                // minor 1-1
                "Digital Evidence",
                "Retrieve electronic records",
                "Recover email correspondence and documents relating to the fraud.",
                "Obtain system access logs",
                "Document {s}'s access to financial systems during the fraud period."
            },
            // Phase 1 — Plot twist: Money Laundering Discovered
            {
                "Money Laundering Discovered",
                "The fraud extends to laundering illicit funds through a network controlled by {s}.",
                // major 0
                "Map the Laundering Network",
                // minor 0-0
                "Trace Offshore Accounts",
                "Identify foreign accounts",
                "Locate offshore accounts used by {s} to receive and move laundered funds.",
                "Map transaction routes",
                "Chart the full movement of funds from source through laundering layers.",
                // minor 0-1
                "Identify Collaborators",
                "Research nominee directors",
                "Identify individuals used as fronts for {s}'s corporate network.",
                "Interview financial contacts",
                "Speak with professionals who facilitated or observed the scheme.",
                // major 1
                "Compile the Prosecution File",
                // minor 1-0
                "Prepare the Evidence",
                "Assemble transaction records",
                "Collate all documentary evidence of the laundering scheme.",
                "Prepare a financial narrative",
                "Write a clear account of how the scheme operated for legal proceedings.",
                // minor 1-1
                "Engage Authorities",
                "Brief financial investigators",
                "Work with specialist financial-crime investigators to hand over the case.",
                "Support a regulatory complaint",
                "Assist the client in filing a formal complaint with the relevant regulator."
            },
            // Phase 2 — Plot twist 2: Wider Conspiracy Exposed
            {
                "Wider Conspiracy Exposed",
                "The fraud is part of a larger scheme involving multiple parties coordinated by {s}.",
                // major 0
                "Uncover the Full Scheme",
                // minor 0-0
                "Map All Co-conspirators",
                "Identify all actors",
                "Compile a list of everyone knowingly involved in the scheme with {s}.",
                "Document each role",
                "Establish what each participant did and how they benefited.",
                // minor 0-1
                "Trace the Victims",
                "Identify all affected parties",
                "Locate every person or entity defrauded by the conspiracy.",
                "Quantify total losses",
                "Calculate the total financial damage caused by the full scheme.",
                // major 1
                "Deliver Justice",
                // minor 1-0
                "Prepare for Prosecution",
                "Compile prosecution brief",
                "Assemble a comprehensive brief suitable for criminal proceedings.",
                "Liaise with authorities",
                "Coordinate with police and prosecutors to initiate formal action.",
                // minor 1-1
                "Final Report",
                "Report to the client",
                "Deliver the full findings and explain the likely legal outcomes.",
                "Close the investigation",
                "Formally close the case and archive all evidence securely."
            }
        },

        // ---- 4: BLACKMAIL ----------------------------------------------------
        {
            // Phase 0 — Trace the Blackmailer
            {
                "Trace the Blackmailer",
                "Identify the source of the blackmail messages and gather admissible evidence.",
                // major 0
                "Analyse the Messages",
                // minor 0-0
                "Message Forensics",
                "Examine the source device",
                "Determine what device or network was used to send the blackmail messages.",
                "Analyse the writing style",
                "Compare the language of the messages with {s}'s known correspondence.",
                // minor 0-1
                "Surveillance",
                "Observe {s}'s behaviour",
                "Monitor {s} for activity consistent with maintaining a blackmail campaign.",
                "Document contact attempts",
                "Record any further attempts by {s} to contact or intimidate the client.",
                // major 1
                "Gather Physical Evidence",
                // minor 1-0
                "Search {s}'s Environment",
                "Identify the source material",
                "Locate the compromising material {s} claims to possess.",
                "Photograph storage media",
                "Document any physical storage devices found during the investigation.",
                // minor 1-1
                "Digital Corroboration",
                "Trace IP addresses",
                "Work with a digital specialist to trace the origin of electronic messages.",
                "Recover deleted files",
                "Retrieve deleted communications from devices associated with {s}."
            },
            // Phase 1 — Plot twist: Extortion Ring Identified
            {
                "Extortion Ring Identified",
                "{s} is part of a wider extortion operation targeting multiple victims.",
                // major 0
                "Map the Ring",
                // minor 0-0
                "Identify Other Victims",
                "Contact potential victims",
                "Discreetly approach others who may have received similar messages.",
                "Compile a victim list",
                "Assemble a full list of known targets of the extortion ring.",
                // minor 0-1
                "Identify Ring Members",
                "Surveil known associates",
                "Monitor individuals associated with {s} for involvement in the ring.",
                "Establish the hierarchy",
                "Determine who leads the ring and what role {s} plays within it.",
                // major 1
                "Secure Evidence for Prosecution",
                // minor 1-0
                "Collect Cross-Case Evidence",
                "Gather statements from victims",
                "Obtain signed statements from multiple victims of the ring.",
                "Document the financial flows",
                "Trace payments made by victims to identify collection accounts.",
                // minor 1-1
                "Close the Operation",
                "Prepare the prosecution file",
                "Compile all evidence across all victims into a single prosecution brief.",
                "Brief law enforcement",
                "Hand the full file to the appropriate authorities for criminal action."
            },
            // Phase 2 — Plot twist 2: Political Dimension Uncovered
            {
                "Political Dimension Uncovered",
                "The blackmail material involves a public figure, elevating the stakes and danger significantly.",
                // major 0
                "Protect the Client",
                // minor 0-0
                "Threat Assessment",
                "Evaluate immediate risks",
                "Determine the level of personal danger to the client given the political dimension.",
                "Recommend protective measures",
                "Advise the client on security measures to implement immediately.",
                // minor 0-1
                "Identify the Public Figure",
                "Research the political connection",
                "Establish the identity and position of the public figure implicated.",
                "Assess their involvement",
                "Determine whether the figure is a victim, participant, or orchestrator.",
                // major 1
                "Resolve Safely",
                // minor 1-0
                "Coordinate with Authorities",
                "Brief a trusted authority contact",
                "Carefully share key findings with a vetted law-enforcement contact.",
                "Secure the client's testimony",
                "Prepare the client's account in a form suitable for protected disclosure.",
                // minor 1-1
                "Final Resolution",
                "Neutralise the threat",
                "Work with authorities to ensure the blackmail material is seized.",
                "Report to the client",
                "Brief the client on the outcome and confirm the threat has been eliminated."
            }
        },

        // ---- 5: MURDER -------------------------------------------------------
        {
            // Phase 0 — Reinvestigate the Death
            {
                "Reinvestigate the Death",
                "Examine the official account and gather evidence that contradicts it.",
                // major 0
                "Review Official Findings",
                // minor 0-0
                "Obtain the Case File",
                "Request the official records",
                "Obtain the police report, coroner's findings, and any on-file witness statements.",
                "Identify gaps in the record",
                "Note inconsistencies, missing evidence, or unexplored leads in the official file.",
                // minor 0-1
                "Forensic Reanalysis",
                "Re-examine the scene",
                "Return to the scene and look for evidence overlooked in the original investigation.",
                "Submit evidence for retesting",
                "Have key physical items retested by an independent forensic laboratory.",
                // major 1
                "Challenge the Official Account",
                // minor 1-0
                "Witness Re-interviews",
                "Re-interview key witnesses",
                "Speak again with witnesses who gave police statements, looking for changes.",
                "Identify new witnesses",
                "Locate individuals not previously interviewed who may have relevant information.",
                // minor 1-1
                "Alibi Investigation",
                "Verify {s}'s alibi",
                "Test every aspect of the alibi {s} provided to police.",
                "Identify alibi weaknesses",
                "Document specific points where {s}'s account cannot be substantiated."
            },
            // Phase 1 — Plot twist: Murder Weapon Located
            {
                "Murder Weapon Located",
                "The weapon used in the killing has been found, opening new forensic avenues.",
                // major 0
                "Process the Weapon",
                // minor 0-0
                "Forensic Testing",
                "Submit weapon for fingerprints",
                "Have the weapon tested for usable fingerprints or DNA.",
                "Ballistic or trace analysis",
                "Conduct ballistic or trace-evidence analysis on the weapon where applicable.",
                // minor 0-1
                "Chain of Custody",
                "Document recovery conditions",
                "Record exactly how, where, and by whom the weapon was found.",
                "Establish handling history",
                "Trace everyone who has touched or possessed the weapon since the killing.",
                // major 1
                "Link to Suspect",
                // minor 1-0
                "Connect {s} to the Weapon",
                "Cross-reference forensic results",
                "Compare weapon forensics against {s}'s known profile.",
                "Trace the weapon's origin",
                "Establish how the weapon was obtained and by whom.",
                // minor 1-1
                "Strengthen the Case",
                "Compile forensic report",
                "Assemble the full forensic narrative linking {s} to the weapon.",
                "Prepare for confrontation",
                "Brief legal counsel on the forensic findings before confronting {s}."
            },
            // Phase 2 — Plot twist 2: Conspiracy to Pervert Justice
            {
                "Conspiracy to Pervert Justice",
                "Evidence emerges that officials were complicit in covering up {s}'s involvement.",
                // major 0
                "Expose the Cover-Up",
                // minor 0-0
                "Identify the Officials",
                "Trace the interference",
                "Identify who in authority altered, withheld, or destroyed evidence.",
                "Document the chain of command",
                "Establish who ordered the cover-up and who carried it out.",
                // minor 0-1
                "Secure Uncorrupted Evidence",
                "Locate uncompromised records",
                "Find evidence that was not tampered with and preserves the original facts.",
                "Identify honest insiders",
                "Find officials willing to speak truthfully about what occurred.",
                // major 1
                "Achieve Justice",
                // minor 1-0
                "Build the Corruption Case",
                "Compile evidence of obstruction",
                "Assemble proof that justice was deliberately perverted.",
                "Brief independent prosecutors",
                "Hand the full file to prosecutors outside the affected jurisdiction.",
                // minor 1-1
                "Final Resolution",
                "Protect the witnesses",
                "Coordinate protective arrangements for any witnesses at risk.",
                "Report to the client",
                "Deliver the complete account of the case and its resolution to the client."
            }
        },

        // ---- 6: STALKING -----------------------------------------------------
        {
            // Phase 0 — Identify and Document the Stalker
            {
                "Identify and Document the Stalker",
                "Confirm {s}'s identity as the stalker and gather evidence for legal proceedings.",
                // major 0
                "Establish the Pattern",
                // minor 0-0
                "Document Incidents",
                "Compile an incident log",
                "Record every known incident of stalking with dates, locations, and descriptions.",
                "Photograph the subject",
                "Obtain clear photographs of {s} suitable for identification purposes.",
                // minor 0-1
                "Gather Evidence",
                "Collect digital evidence",
                "Retrieve messages, posts, or emails sent by {s} to the client.",
                "Identify surveillance devices",
                "Check the client's home and vehicle for tracking devices placed by {s}.",
                // major 1
                "Background Investigation",
                // minor 1-0
                "Research {s}'s History",
                "Check for prior complaints",
                "Search for previous stalking or harassment complaints made against {s}.",
                "Map known locations",
                "Identify addresses, workplaces, and locations regularly visited by {s}.",
                // minor 1-1
                "Profile the Behaviour",
                "Document the obsession",
                "Gather evidence of the nature and duration of {s}'s fixation on the client.",
                "Assess escalation risk",
                "Evaluate whether {s}'s behaviour is escalating and advise the client accordingly."
            },
            // Phase 1 — Plot twist: Cyber Harassment Uncovered
            {
                "Cyber Harassment Uncovered",
                "{s}'s stalking has extended into online harassment through fake accounts and digital surveillance.",
                // major 0
                "Expose the Online Activity",
                // minor 0-0
                "Fake Account Identification",
                "Identify fake profiles",
                "Locate all fake social media and online accounts operated by {s}.",
                "Document the harassment",
                "Screenshot and preserve all online harassment directed at the client.",
                // minor 0-1
                "Technical Investigation",
                "Trace IP origins",
                "Work with a digital specialist to trace the origin of {s}'s fake accounts.",
                "Retrieve deleted messages",
                "Recover deleted or hidden messages sent between {s} and the client.",
                // major 1
                "Secure Legal Protection",
                // minor 1-0
                "Prepare for a Restraining Order",
                "Compile the application",
                "Assemble all evidence for an emergency restraining order application.",
                "Submit to the court",
                "File the restraining order application with full supporting evidence.",
                // minor 1-1
                "Ongoing Monitoring",
                "Establish monitoring protocols",
                "Set up procedures to detect if {s} creates new fake accounts or contacts.",
                "Brief the client on risks",
                "Advise the client on how to protect themselves during ongoing proceedings."
            },
            // Phase 2 — Plot twist 2: Threat Escalates to Physical Danger
            {
                "Threat Escalates to Physical Danger",
                "{s}'s behaviour has escalated beyond harassment to direct physical threat.",
                // major 0
                "Protect the Client",
                // minor 0-0
                "Threat Assessment",
                "Conduct a formal threat assessment",
                "Carry out a professional assessment of the physical danger to the client.",
                "Brief personal security",
                "Engage personal security for the client for the immediate period.",
                // minor 0-1
                "Evidence of the Threat",
                "Document the escalation",
                "Record all evidence of {s}'s escalating threatening behaviour.",
                "Obtain corroborating witnesses",
                "Identify third parties who have directly observed {s}'s threatening actions.",
                // major 1
                "Law Enforcement Engagement",
                // minor 1-0
                "Emergency Police Briefing",
                "Brief the detective unit",
                "Provide a full briefing to police including all evidence of the escalation.",
                "Support arrest proceedings",
                "Provide evidence to support arrest and detention of {s}.",
                // minor 1-1
                "Long-term Safety Plan",
                "Develop a safety plan",
                "Work with the client to create a comprehensive long-term safety plan.",
                "Final report to client",
                "Deliver the full case report and safety recommendations to the client."
            }
        },

        // ---- 7: CORPORATE_ESPIONAGE ------------------------------------------
        {
            // Phase 0 — Identify the Leak
            {
                "Identify the Leak",
                "Determine whether {s} is responsible for leaking confidential information to a competitor.",
                // major 0
                "Internal Investigation",
                // minor 0-0
                "Access Log Review",
                "Audit system access logs",
                "Review all access logs to confidential files and systems for anomalies.",
                "Identify unusual access",
                "Flag instances where {s} accessed sensitive information outside normal patterns.",
                // minor 0-1
                "Behavioural Surveillance",
                "Observe {s} in the workplace",
                "Monitor {s}'s office behaviour for signs of information-gathering.",
                "Document suspicious meetings",
                "Record any off-site meetings {s} holds with individuals from competitor firms.",
                // major 1
                "Build the Evidence Base",
                // minor 1-0
                "Technical Forensics",
                "Examine {s}'s devices",
                "With authorisation, examine {s}'s work devices for data-exfiltration evidence.",
                "Review file transfers",
                "Identify any large or unusual file transfers or external email activity.",
                // minor 1-1
                "External Confirmation",
                "Surveil competitor premises",
                "Observe the competitor's facility for signs that leaked information is being used.",
                "Identify the recipient",
                "Establish the identity of the person at the competitor receiving {s}'s information."
            },
            // Phase 1 — Plot twist: Foreign Intelligence Suspected
            {
                "Foreign Intelligence Involvement Suspected",
                "Evidence suggests the espionage extends beyond a commercial competitor to a foreign intelligence operation.",
                // major 0
                "Assess the Scope",
                // minor 0-0
                "Determine What Was Leaked",
                "Review the leaked data",
                "Identify the exact confidential information {s} has passed on.",
                "Assess strategic value",
                "Evaluate the national-security implications of the leaked data.",
                // minor 0-1
                "Identify the Foreign Connection",
                "Trace the intelligence chain",
                "Follow the information trail beyond the competitor to the foreign actor.",
                "Identify the handler",
                "Establish who has been directing {s}'s espionage activities.",
                // major 1
                "Notify Authorities",
                // minor 1-0
                "Security Services Briefing",
                "Brief national security contacts",
                "Provide a secure briefing to the appropriate security-service contacts.",
                "Support counter-intelligence",
                "Cooperate with counter-intelligence efforts to contain the breach.",
                // minor 1-1
                "Protect the Client",
                "Implement information controls",
                "Work with the client to immediately restrict access to sensitive information.",
                "Final intelligence report",
                "Deliver a classified summary of findings to the client and the authorities."
            },
            // Phase 2 — Plot twist 2: Double Agent Discovered
            {
                "Double Agent Discovered",
                "A second actor within the organisation is revealed to be feeding information to {s}.",
                // major 0
                "Identify the Second Leak",
                // minor 0-0
                "Internal Audit",
                "Run a deeper access audit",
                "Perform a comprehensive audit of all staff access to the affected systems.",
                "Identify secondary anomalies",
                "Find access patterns that indicate a second individual passing information.",
                // minor 0-1
                "Confront the Double Agent",
                "Surveil the secondary actor",
                "Covertly observe the identified double agent and document their activity.",
                "Gather direct evidence",
                "Obtain unambiguous evidence of the double agent's role in the leak.",
                // major 1
                "Final Resolution",
                // minor 1-0
                "Prepare the Full Brief",
                "Compile the complete case",
                "Assemble all evidence against both {s} and the double agent.",
                "Deliver to client leadership",
                "Present the full brief to the client's senior leadership and legal team.",
                // minor 1-1
                "Close the Investigation",
                "Oversee the disciplinary process",
                "Support the client through the disciplinary and/or criminal proceedings.",
                "Final report",
                "Deliver the final case report and formally close the investigation."
            }
        }
    };
}
