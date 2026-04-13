package eb.framework1.investigation;

import java.util.Random;

/**
 * Shared narrative-text templates used by both the core case generator and the
 * admin tool.
 *
 * <p>This class holds no state except an injected {@link Random} for picking
 * among template variants.  All templates are pure functions of their
 * parameters — no side-effects, no UI dependency.
 *
 * <h3>Template categories</h3>
 * <ul>
 *   <li><b>Attribute success/failure narratives</b> — flavour text for
 *       story-tree action results, driven by the player attribute used and
 *       the {@link ActionType} of the action.</li>
 *   <li><b>Motive narratives</b> — case-specific explanations of the
 *       perpetrator's motive, keyed by a motive code.</li>
 * </ul>
 */
public class NarrativeTemplates {

    private final Random random;

    /** Creates an instance using the default {@link Random}. */
    public NarrativeTemplates() {
        this(new Random());
    }

    /**
     * Creates an instance with an explicit {@link Random} for reproducible output.
     *
     * @param random random-number source; must not be {@code null}
     */
    public NarrativeTemplates(Random random) {
        this.random = random != null ? random : new Random();
    }

    // =========================================================================
    // Attribute success narrative
    // =========================================================================

    /**
     * Returns a flavour-text sentence describing how the player's attribute
     * helped them succeed at the given action.
     *
     * @param attr        one of the seven player attributes
     *                    ({@code "INTIMIDATION"}, {@code "CHARISMA"}, etc.)
     * @param actionTitle the full action title from the story tree
     * @return a single-paragraph narrative string
     */
    public String buildAttributeSuccessNarrative(String attr, String actionTitle) {
        ActionType at = ActionType.classify(actionTitle);
        boolean isInterview = at == ActionType.INTERVIEW;
        boolean isEvidence  = at == ActionType.EVIDENCE;
        boolean isDocument  = at == ActionType.DOCUMENT;
        boolean isPhoto     = at == ActionType.PHOTOGRAPH;

        switch (attr) {
            case "INTIMIDATION":
                if (isInterview)
                    return "With a stern look you make it clear that cooperation is not optional. "
                         + "Under your unblinking gaze they reluctantly answer.";
                if (isEvidence)
                    return "Your commanding presence clears the area. You examine the evidence undisturbed.";
                if (isDocument)
                    return "Your authoritative demand leaves the clerk no room to argue — "
                         + "the files appear on the counter without delay.";
                return "Your forceful demeanour gets results — they comply without further argument.";

            case "CHARISMA":
                if (isInterview)
                    return "With a warm smile and well-chosen words you earn their trust. "
                         + "They open up more than they intended.";
                if (isDocument)
                    return "A friendly rapport with the clerk gets you access to records that aren't publicly available.";
                if (isEvidence)
                    return "You charm the officer guarding the perimeter into letting you through. "
                         + "Inside you find exactly the sample you needed.";
                if (isPhoto)
                    return "A quick conversation with a bystander gets you access to a better vantage point "
                         + "for the perfect angle.";
                return "Your easy manner wins cooperation and they hand over what you need.";

            case "PERCEPTION":
                if (isEvidence)
                    return "A barely-visible smudge on the door frame catches your eye. "
                         + "You bag the sample — it may be the break you need.";
                if (isInterview)
                    return "You catch a flicker of nervousness when they mention the alibi. "
                         + "That micro-expression tells you more than their words.";
                if (isDocument)
                    return "Scanning the ledger you spot a date that doesn't add up. "
                         + "Someone altered this record — and you know which line.";
                if (isPhoto)
                    return "Through the viewfinder you notice a reflection in the window that shows "
                         + "a detail invisible from any other angle.";
                return "A small but telling detail reveals itself to your trained eye.";

            case "INTELLIGENCE":
                if (isDocument)
                    return "You cross-reference three separate filings and find a pattern the original "
                         + "investigator missed — the numbers don't lie.";
                if (isInterview)
                    return "Your probing question exposes a logical gap in their story. "
                         + "They scramble for an answer, confirming your suspicion.";
                if (isEvidence)
                    return "You recognise the chemical residue from an earlier case — "
                         + "the connection clicks into place immediately.";
                return "Analytical reasoning uncovers a connection that others overlooked.";

            case "EMPATHY":
                if (isInterview)
                    return "You sense the underlying grief behind their composure and gently steer "
                         + "the conversation. They share something they've told nobody else.";
                if (isEvidence)
                    return "Placing yourself in the victim's shoes, you retrace their likely path — "
                         + "and find a personal item that was overlooked.";
                if (isDocument)
                    return "Reading between the lines of the witness statement, you detect emotion that "
                         + "reveals they were closer to the victim than they admitted.";
                return "Your ability to read people uncovers a hidden emotional dimension to the case.";

            case "MEMORY":
                if (isDocument)
                    return "You recall a similar case from years ago. The same filing error appeared — "
                         + "it's a signature trick used by a known fraudster.";
                if (isInterview)
                    return "Mid-sentence, you remember a detail from your earlier notes. "
                         + "You interrupt with a follow-up they weren't expecting and the truth slips out.";
                if (isEvidence)
                    return "You've seen this type of mark before — it matches a tool catalogued in a previous investigation. "
                         + "You know exactly what to look for next.";
                if (isPhoto)
                    return "Comparing the scene mentally with your earlier visit you spot something that's been "
                         + "moved — someone was here between visits.";
                return "A recalled fact from a past case connects directly to the evidence at hand.";

            case "STEALTH":
                if (isEvidence)
                    return "Moving silently through the cordoned area, you retrieve a sample "
                         + "without alerting the guard on the far side.";
                if (isInterview)
                    return "You overhear a whispered conversation before they realise you're listening. "
                         + "The offhand remark is exactly the piece you were missing.";
                if (isPhoto)
                    return "You position yourself unseen and capture a photograph of the subject "
                         + "meeting someone they denied knowing.";
                if (isDocument)
                    return "You slip into the restricted filing room unnoticed and photograph "
                         + "the sealed records before anyone checks.";
                return "Moving undetected, you gather what you need without drawing attention.";

            default:
                return "Your skills prove sufficient — the task yields useful results.";
        }
    }

    // =========================================================================
    // Attribute failure narrative
    // =========================================================================

    /**
     * Returns a flavour-text sentence describing how the player's attribute
     * was insufficient for the given action but they still gained a small clue.
     *
     * @param attr        one of the seven player attributes
     * @param actionTitle the full action title from the story tree
     * @return a single-paragraph narrative string
     */
    public String buildAttributeFailureNarrative(String attr, String actionTitle) {
        ActionType at = ActionType.classify(actionTitle);
        boolean isInterview = at == ActionType.INTERVIEW;
        boolean isEvidence  = at == ActionType.EVIDENCE;
        boolean isDocument  = at == ActionType.DOCUMENT;
        boolean isPhoto     = at == ActionType.PHOTOGRAPH;

        switch (attr) {
            case "INTIMIDATION":
                if (isInterview)
                    return "You ask them to stand aside, but they don't seem willing. "
                         + "You do notice it's the same person you saw talking to the subject earlier — "
                         + "that connection may be worth pursuing.";
                if (isEvidence)
                    return "Someone challenges your right to be here. Without sufficient authority "
                         + "you're forced to back down, but you catch a glimpse of something before you leave.";
                if (isDocument)
                    return "The records clerk refuses to hand over the restricted file. "
                         + "You notice they glance nervously at a particular drawer — that itself is a clue.";
                return "Your attempt to assert authority falls short. They hold their ground — "
                     + "but their reaction itself tells you something.";

            case "CHARISMA":
                if (isInterview)
                    return "Your approach falls flat — they seem unimpressed and shut the conversation down. "
                         + "However, on your way out you overhear something that might be worth a follow-up.";
                if (isDocument)
                    return "The clerk turns you away without the access you need. "
                         + "You'll have to find another way in, or come back with better credentials.";
                if (isEvidence)
                    return "The officer on guard isn't swayed by your request. "
                         + "You're turned away from the perimeter, but from outside you spot a secondary entrance "
                         + "that may be worth investigating later.";
                if (isPhoto)
                    return "A bystander blocks your best angle and won't budge. "
                         + "The shots you manage are mediocre, but one catches an odd detail in the background.";
                return "They aren't won over by your approach and give you nothing useful — "
                     + "but their reluctance itself may be telling.";

            case "PERCEPTION":
                if (isEvidence)
                    return "You scan the area carefully but nothing immediately stands out. "
                         + "Whatever was here may have already been disturbed or removed.";
                if (isInterview)
                    return "You miss the subtle cue in their expression. "
                         + "They answer smoothly — too smoothly — but you can't pin down what's off.";
                if (isDocument)
                    return "The numbers blur together and the anomaly hides in plain sight. "
                         + "You'll need to come back with fresh eyes or a different approach.";
                if (isPhoto)
                    return "You photograph the scene but miss the critical angle. "
                         + "Later review of your shots shows nothing the initial report didn't already cover.";
                return "The detail you were looking for doesn't reveal itself this time. "
                     + "It may still be there — consider returning with fresh eyes.";

            case "INTELLIGENCE":
                if (isDocument)
                    return "The volume and complexity of the paperwork overwhelms you for now. "
                         + "You'll need more context before the pattern emerges.";
                if (isInterview)
                    return "You ask the wrong question and they steer the conversation away. "
                         + "You leave with less than you came with.";
                if (isEvidence)
                    return "The evidence doesn't connect to anything you already know. "
                         + "The link is there, but without more context you can't see it yet.";
                return "The pieces don't connect yet. There's still a gap somewhere in your reasoning.";

            case "EMPATHY":
                if (isInterview)
                    return "You try to connect but they remain guarded throughout. "
                         + "Their defensiveness itself might be telling — why would they be so closed off?";
                if (isEvidence)
                    return "You walk the scene looking for anything personal or out of place, "
                         + "but the sterile environment yields nothing to your instinct this time.";
                if (isDocument)
                    return "You read through the statements but nothing strikes an emotional chord. "
                         + "The writer was careful — or perhaps genuinely uninvolved.";
                return "You misread the emotional temperature in the room. "
                     + "Your approach creates distance instead of trust.";

            case "MEMORY":
                if (isDocument)
                    return "The relevant detail is just out of reach. You know you've seen something like this before, "
                         + "but you can't recall where. Time to review your notes.";
                if (isInterview)
                    return "They mention something that should ring a bell, but the connection escapes you in the moment. "
                         + "Write it down — it may make sense later.";
                if (isEvidence)
                    return "You feel like you've seen this type of item before, but the case reference escapes you. "
                         + "A trip back to the case file might jog your memory.";
                if (isPhoto)
                    return "You can't recall what the scene looked like in the earlier photos, "
                         + "so you miss what changed. Re-check the originals when you get back.";
                return "The critical detail doesn't surface when you need it. "
                     + "Go back through your earlier findings before proceeding.";

            case "STEALTH":
                if (isEvidence)
                    return "Your presence is noticed earlier than expected. "
                         + "You're forced to abandon the search, but you managed to pocket one small piece of evidence.";
                if (isInterview)
                    return "They spot you watching before you're ready. "
                         + "The element of surprise is lost, and they're now on guard.";
                if (isPhoto)
                    return "Someone sees your camera and calls you out. "
                         + "You're asked to leave, but not before you notice which area they were most anxious to protect.";
                if (isDocument)
                    return "The records clerk spots you in the restricted section. "
                         + "You're escorted out, but you glimpsed a folder label that may narrow your next search.";
                return "You're seen when you should have remained undetected. "
                     + "The opportunity is lost for now — but they don't know exactly what you know.";

            default:
                return "Without the required skill you fall short this time. "
                     + "There may still be another way to achieve the same result.";
        }
    }

    // =========================================================================
    // Motive narrative
    // =========================================================================

    /**
     * Builds a case-specific motive narrative for the given motive code.
     * Each code has multiple templates using the subject/victim names, ensuring
     * that every case gets a unique, tailored motivation story.
     *
     * @param motiveCode one of the predefined motive codes
     *                   (e.g.&nbsp;{@code "FINANCIAL_GAIN"}, {@code "REVENGE"})
     * @param subject    the perpetrator's name
     * @param victim     the victim's name
     * @return a narrative sentence describing the motive
     */
    public String buildMotiveNarrative(String motiveCode, String subject, String victim) {
        String[] pool;
        switch (motiveCode) {
            case "FINANCIAL_GAIN":
                pool = new String[]{
                    "Having fallen on hard times, " + subject + " decided this would be a quick solution to all financial problems.",
                    subject + " discovered a lucrative insurance policy on " + victim + " and devised a plan to collect.",
                    "Mounting debts and a failing business drove " + subject + " to target " + victim + "'s estate.",
                    subject + " had been secretly siphoning funds and needed " + victim + " out of the way before an audit."};
                break;
            case "REVENGE":
                pool = new String[]{
                    subject + " had nursed a grudge against " + victim + " for years after a devastating public humiliation.",
                    "After " + victim + " destroyed " + subject + "'s career, " + subject + " spent months planning retribution.",
                    subject + " blamed " + victim + " for the death of a loved one and vowed to settle the score.",
                    "A bitter feud over a broken promise drove " + subject + " to take drastic action against " + victim + "."};
                break;
            case "JEALOUSY":
                pool = new String[]{
                    subject + " could not accept that " + victim + " had been promoted over them despite fewer qualifications.",
                    "A romantic rivalry between " + subject + " and " + victim + " escalated beyond control.",
                    subject + " envied " + victim + "'s social standing and growing influence in the community.",
                    "Watching " + victim + " succeed where " + subject + " had failed became an unbearable obsession."};
                break;
            case "COERCION":
                pool = new String[]{
                    subject + " was being blackmailed with compromising photographs and saw no other way out.",
                    "A criminal associate threatened " + subject + "'s family unless " + victim + " was dealt with.",
                    subject + " was manipulated by a third party who stood to gain from " + victim + "'s downfall.",
                    "Under extreme pressure from mounting threats, " + subject + " reluctantly carried out someone else's plan."};
                break;
            case "POWER":
                pool = new String[]{
                    subject + " saw " + victim + " as the only obstacle to seizing control of the organisation.",
                    "With " + victim + " out of the picture, " + subject + " would inherit full authority over the estate.",
                    subject + " had long resented " + victim + "'s dominance and orchestrated a takeover.",
                    "Eliminating " + victim + " was the final move in " + subject + "'s carefully planned bid for control."};
                break;
            case "SELF_DEFENSE":
                pool = new String[]{
                    subject + " believed " + victim + " was about to expose a secret that would ruin everything.",
                    "After receiving threatening messages from " + victim + ", " + subject + " acted out of genuine fear.",
                    subject + " claimed " + victim + " attacked first, but the evidence suggests a premeditated response.",
                    "Cornered by " + victim + "'s escalating threats, " + subject + " felt there was no alternative."};
                break;
            case "IDEOLOGY":
                pool = new String[]{
                    subject + " viewed " + victim + "'s activities as a betrayal of deeply held principles and acted accordingly.",
                    "Radicalised through online forums, " + subject + " targeted " + victim + " as a symbol of everything wrong.",
                    subject + " believed silencing " + victim + " would advance a political cause they were devoted to.",
                    "A fanatical commitment to a fringe movement drove " + subject + " to act against " + victim + "."};
                break;
            case "CONCEALMENT":
                pool = new String[]{
                    subject + " had committed a prior offence that " + victim + " was about to report to the authorities.",
                    victim + " stumbled upon " + subject + "'s embezzlement scheme and had to be silenced.",
                    subject + " needed to destroy evidence of a previous fraud before " + victim + " could hand it over.",
                    "With " + victim + " threatening to reveal the truth, " + subject + " acted to protect a web of lies."};
                break;
            case "PASSION":
                pool = new String[]{
                    "An intense argument between " + subject + " and " + victim + " escalated into a violent confrontation.",
                    subject + "'s uncontrollable rage after discovering " + victim + "'s betrayal led to a fatal outburst.",
                    "Years of suppressed emotion erupted when " + subject + " confronted " + victim + " about the affair.",
                    "A moment of blind fury during a heated exchange drove " + subject + " to act without thinking."};
                break;
            case "LOYALTY":
                pool = new String[]{
                    subject + " acted to protect a family member who " + victim + " was threatening to expose.",
                    "A close friend of " + subject + " asked for help dealing with " + victim + ", and " + subject + " couldn't refuse.",
                    subject + " took the fall for an associate, believing loyalty demanded sacrifice.",
                    "To shield a loved one from " + victim + "'s harassment, " + subject + " decided to intervene permanently."};
                break;
            default:
                pool = new String[]{
                    subject + "'s true motivation remains complex — a mix of personal grievance and opportunity.",
                    "The exact reason " + subject + " targeted " + victim + " stems from a private conflict not yet fully understood.",
                    subject + " was driven by circumstances that created a perfect storm of desperation and opportunity."};
                break;
        }
        return pool[random.nextInt(pool.length)];
    }
}
