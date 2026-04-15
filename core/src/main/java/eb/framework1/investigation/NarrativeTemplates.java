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
    // Internal helpers
    // =========================================================================

    /** Picks one element at random from the provided strings. */
    private String pick(String... options) {
        return options[random.nextInt(options.length)];
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
                if (isInterview) return pick(
                    "With a stern look you make it clear that cooperation is not optional. "
                        + "Under your unblinking gaze they reluctantly answer.",
                    "You let the silence stretch until it becomes uncomfortable. "
                        + "They fill it with exactly what you needed to hear.",
                    "One measured question, delivered at precisely the wrong moment for them. "
                        + "The composure cracks and the truth spills out.");
                if (isEvidence) return pick(
                    "Your commanding presence clears the area. You examine the evidence undisturbed.",
                    "A few firm words and the bystanders step back. "
                        + "You have the space to work without interference.",
                    "Nobody questions your authority to be there — your manner doesn't invite questions.");
                if (isDocument) return pick(
                    "Your authoritative demand leaves the clerk no room to argue — "
                        + "the files appear on the counter without delay.",
                    "The administrator hesitates, then decides the easier path is compliance.",
                    "You make clear that refusal will be noted and reported. "
                        + "The files are produced immediately.");
                return pick(
                    "Your forceful demeanour gets results — they comply without further argument.",
                    "A well-timed show of authority cuts through the resistance. They yield.",
                    "Nobody wants a confrontation with someone who looks this certain. They step aside.");

            case "CHARISMA":
                if (isInterview) return pick(
                    "With a warm smile and well-chosen words you earn their trust. "
                        + "They open up more than they intended.",
                    "You match their energy perfectly and they forget this is an interrogation. "
                        + "The story comes out naturally, detail by detail.",
                    "A shared laugh, a moment of genuine connection — and then they tell you "
                        + "the one thing they'd sworn not to mention.");
                if (isDocument) return pick(
                    "A friendly rapport with the clerk gets you access to records that aren't publicly available.",
                    "You find common ground quickly and they bend the rules just enough to be useful.",
                    "The clerk pulls the restricted folder with the air of someone doing a favour for a friend.");
                if (isEvidence) return pick(
                    "You charm the officer guarding the perimeter into letting you through. "
                        + "Inside you find exactly the sample you needed.",
                    "A few well-placed compliments and the guard waves you past without checking credentials.",
                    "Your easy confidence reads as authority — nobody stops you until you've already finished.");
                if (isPhoto) return pick(
                    "A quick conversation with a bystander gets you access to a better vantage point "
                        + "for the perfect angle.",
                    "You persuade the building manager to let you in for 'just five minutes'. "
                        + "The shot you need is waiting on the third floor.",
                    "A little flattery and the neighbour invites you onto their balcony. "
                        + "It's the perfect angle.");
                return pick(
                    "Your easy manner wins cooperation and they hand over what you need.",
                    "The right words at the right moment — they agree before they've thought it through.",
                    "People respond to warmth. They give you far more than they intended.");

            case "PERCEPTION":
                if (isEvidence) return pick(
                    "A barely-visible smudge on the door frame catches your eye. "
                        + "You bag the sample — it may be the break you need.",
                    "The footprint is almost invisible against the tile. "
                        + "You notice it, document it, and leave everything else undisturbed.",
                    "Something is slightly off about the arrangement on the shelf. "
                        + "A closer look confirms someone moved these items recently.");
                if (isInterview) return pick(
                    "You catch a flicker of nervousness when they mention the alibi. "
                        + "That micro-expression tells you more than their words.",
                    "They look left when they fabricate and right when they recall. "
                        + "The pattern emerges quickly once you know what to watch for.",
                    "A tiny hesitation before they answer the second question. "
                        + "It's enough to know which thread to pull.");
                if (isDocument) return pick(
                    "Scanning the ledger you spot a date that doesn't add up. "
                        + "Someone altered this record — and you know which line.",
                    "The correction fluid is barely visible under the desk lamp. "
                        + "The altered figure is the one that matters.",
                    "A change in ink colour halfway down the page. Someone continued "
                        + "filling in this form using a different pen — at a different time.");
                if (isPhoto) return pick(
                    "Through the viewfinder you notice a reflection in the window that shows "
                        + "a detail invisible from any other angle.",
                    "You catch the subject and an unknown companion in a single frame "
                        + "— the second face is significant.",
                    "The timestamp on the background clock confirms the alibi was false. "
                        + "You zoom in and the detail is sharp enough to use.");
                return pick(
                    "A small but telling detail reveals itself to your trained eye.",
                    "While others would have passed it by, you stop, look closer, and find what matters.",
                    "Your eye for the unusual catches something that changes the entire picture.");

            case "INTELLIGENCE":
                if (isDocument) return pick(
                    "You cross-reference three separate filings and find a pattern the original "
                        + "investigator missed — the numbers don't lie.",
                    "A statistical anomaly buried in the annexe confirms what you suspected. "
                        + "The manipulation is systematic.",
                    "By mapping the transaction timeline against travel records, "
                        + "you find an overlap that shouldn't exist.");
                if (isInterview) return pick(
                    "Your probing question exposes a logical gap in their story. "
                        + "They scramble for an answer, confirming your suspicion.",
                    "You present two facts that can't both be true and watch them choose which lie to protect.",
                    "The trap is simple: you already know the answer. "
                        + "Their response confirms the deception.");
                if (isEvidence) return pick(
                    "You recognise the chemical residue from an earlier case — "
                        + "the connection clicks into place immediately.",
                    "The tool marks are consistent with a specific model used in a previous incident "
                        + "that was never publicly linked to this one.",
                    "Combining two separate pieces of physical evidence reveals a sequence "
                        + "that only makes sense one way.");
                return pick(
                    "Analytical reasoning uncovers a connection that others overlooked.",
                    "You work through the problem methodically and arrive at the only conclusion that fits all the facts.",
                    "The answer was there from the beginning — you just had to look at it from the right angle.");

            case "EMPATHY":
                if (isInterview) return pick(
                    "You sense the underlying grief behind their composure and gently steer "
                        + "the conversation. They share something they've told nobody else.",
                    "You let them speak without interruption. By the time they finish, "
                        + "they've said far more than they meant to.",
                    "Your genuine concern disarms them. They lower their guard "
                        + "and the important details emerge naturally.");
                if (isEvidence) return pick(
                    "Placing yourself in the victim's shoes, you retrace their likely path — "
                        + "and find a personal item that was overlooked.",
                    "You consider what someone in distress would do in this space "
                        + "and walk the path they would have taken. It leads you directly to the evidence.",
                    "Your instinct about the victim's emotional state points you to "
                        + "the one part of the room nobody else thought to check.");
                if (isDocument) return pick(
                    "Reading between the lines of the witness statement, you detect emotion that "
                        + "reveals they were closer to the victim than they admitted.",
                    "The word choices in the report betray a personal connection the writer "
                        + "was trying to conceal.",
                    "Small inconsistencies in the account reflect stress, not dishonesty. "
                        + "You find the truth buried underneath the self-protection.");
                return pick(
                    "Your ability to read people uncovers a hidden emotional dimension to the case.",
                    "You sense what isn't being said — and that turns out to be the crucial part.",
                    "The emotional undercurrent in the room points you somewhere specific. You follow it.");

            case "MEMORY":
                if (isDocument) return pick(
                    "You recall a similar case from years ago. The same filing error appeared — "
                        + "it's a signature trick used by a known fraudster.",
                    "The phrasing is familiar — almost identical to a contract you reviewed during "
                        + "a case three years back. The same firm was involved.",
                    "A detail in the annexe matches something from your notes on a closed case. "
                        + "The connection explains a gap that had puzzled you.");
                if (isInterview) return pick(
                    "Mid-sentence, you remember a detail from your earlier notes. "
                        + "You interrupt with a follow-up they weren't expecting and the truth slips out.",
                    "You recall exactly what they said at the first meeting — "
                        + "word for word. This version is different. You ask why.",
                    "Something they said triggers a memory from a witness statement you read three days ago. "
                        + "The contradiction is clear and you press it.");
                if (isEvidence) return pick(
                    "You've seen this type of mark before — it matches a tool catalogued in a previous investigation. "
                        + "You know exactly what to look for next.",
                    "The substance on the floor is unfamiliar to most people, "
                        + "but you handled something similar in a warehouse break-in two years ago.",
                    "Your memory of the scene from your first visit — before anything was moved — "
                        + "tells you exactly what's been tampered with.");
                if (isPhoto) return pick(
                    "Comparing the scene mentally with your earlier visit you spot something that's been "
                        + "moved — someone was here between visits.",
                    "The layout matches your mental image from Monday, except for one detail. "
                        + "That detail is significant.",
                    "You recall the position of every item from the first walk-through. "
                        + "Three things are different. One of them matters.");
                return pick(
                    "A recalled fact from a past case connects directly to the evidence at hand.",
                    "Your memory files the relevant detail precisely when you need it.",
                    "What appeared to be an unrelated case memory turns out to be exactly the key you needed.");

            case "STEALTH":
                if (isEvidence) return pick(
                    "Moving silently through the cordoned area, you retrieve a sample "
                        + "without alerting the guard on the far side.",
                    "You wait until the patrol reaches the far end of the corridor, "
                        + "then move quickly. The evidence bag is sealed before anyone turns around.",
                    "Every footfall is calculated. You're in and out in under two minutes. "
                        + "Nothing disturbed. Nothing missed.");
                if (isInterview) return pick(
                    "You overhear a whispered conversation before they realise you're listening. "
                        + "The offhand remark is exactly the piece you were missing.",
                    "You arrive early and wait in an adjacent room. "
                        + "The conversation they have before your 'arrival' is more useful than anything they'd have said to your face.",
                    "Staying quiet in the background, you hear them confirm the alibi to someone else — "
                        + "and the details don't match what they told you.");
                if (isPhoto) return pick(
                    "You position yourself unseen and capture a photograph of the subject "
                        + "meeting someone they denied knowing.",
                    "From behind a parked vehicle, you photograph a handoff that "
                        + "would have disappeared the moment either party noticed you.",
                    "An unmarked position, a long lens, and patience. The shot is clean.");
                if (isDocument) return pick(
                    "You slip into the restricted filing room unnoticed and photograph "
                        + "the sealed records before anyone checks.",
                    "The office is briefly empty. You spend ninety seconds at the filing cabinet "
                        + "and leave everything exactly as you found it.",
                    "Your approach is quiet enough that the on-duty clerk doesn't look up "
                        + "until you're already back in the corridor.");
                return pick(
                    "Moving undetected, you gather what you need without drawing attention.",
                    "They never know you were there. The evidence does all the talking.",
                    "Patience and silence accomplish what a direct approach never could.");

            default:
                return pick(
                    "Your skills prove sufficient — the task yields useful results.",
                    "The approach works. You come away with exactly what the case needed.",
                    "Execution is clean and the result is clear. Progress made.");
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
                if (isInterview) return pick(
                    "You ask them to stand aside, but they don't seem willing. "
                        + "You do notice it's the same person you saw talking to the subject earlier — "
                        + "that connection may be worth pursuing.",
                    "They meet your pressure with a calm that's almost rehearsed. "
                        + "They've been through this before — or they were warned you were coming.",
                    "The confrontation doesn't land. They disengage and leave before you can escalate. "
                        + "Their hurry, at least, tells you something.");
                if (isEvidence) return pick(
                    "Someone challenges your right to be here. Without sufficient authority "
                        + "you're forced to back down, but you catch a glimpse of something before you leave.",
                    "You bluff your way to the edge of the taped zone and no further. "
                        + "Still — what you could see from there was useful.",
                    "The challenge comes faster than expected. You withdraw, but not before "
                        + "memorising the layout. That alone is worth the visit.");
                if (isDocument) return pick(
                    "The records clerk refuses to hand over the restricted file. "
                        + "You notice they glance nervously at a particular drawer — that itself is a clue.",
                    "The supervisor intervenes before you get anywhere. "
                        + "But in their haste to stop you, they confirm the file exists.",
                    "Turned away at the counter. The form they ask you to fill out, however, "
                        + "reveals the category the document falls under — useful.");
                return pick(
                    "Your attempt to assert authority falls short. They hold their ground — "
                        + "but their reaction itself tells you something.",
                    "They don't flinch. Either they've been briefed, or they're hiding nothing. "
                        + "Worth finding out which.",
                    "The pushback is firmer than expected. Someone coached them to resist exactly this approach.");

            case "CHARISMA":
                if (isInterview) return pick(
                    "Your approach falls flat — they seem unimpressed and shut the conversation down. "
                        + "However, on your way out you overhear something that might be worth a follow-up.",
                    "They're polite but entirely closed off. Not suspicious — just unmoved. "
                        + "A different angle might work better.",
                    "The rapport doesn't form. They answer in monosyllables and check their watch. "
                        + "But one of those monosyllables contradicts something from the file.");
                if (isDocument) return pick(
                    "The clerk turns you away without the access you need. "
                        + "You'll have to find another way in, or come back with better credentials.",
                    "Charm isn't currency here — they follow procedure and nothing more. "
                        + "The request goes in writing and you're told to wait.",
                    "The supervisor is unmoved. The form they hand you to complete, however, "
                        + "reveals more than they intended about how the system is organised.");
                if (isEvidence) return pick(
                    "The officer on guard isn't swayed by your request. "
                        + "You're turned away from the perimeter, but from outside you spot a secondary entrance "
                        + "that may be worth investigating later.",
                    "Your friendly opener doesn't land. They've clearly been told not to engage. "
                        + "The instruction itself tells you someone was expecting this.",
                    "Turned back at the tape. What you can see from outside the zone, though, "
                        + "is already more than the initial report described.");
                if (isPhoto) return pick(
                    "A bystander blocks your best angle and won't budge. "
                        + "The shots you manage are mediocre, but one catches an odd detail in the background.",
                    "Security arrives before you get the frame you need. "
                        + "You leave with inferior material — but the security's speed itself is informative.",
                    "The vantage point is unavailable. The alternative position yields one usable frame "
                        + "that wasn't part of the original plan.");
                return pick(
                    "They aren't won over by your approach and give you nothing useful — "
                        + "but their reluctance itself may be telling.",
                    "The warmth isn't reciprocated. Whatever they're hiding, they've protected it well.",
                    "You don't make the connection. But their discomfort with your questions is worth noting.");

            case "PERCEPTION":
                if (isEvidence) return pick(
                    "You scan the area carefully but nothing immediately stands out. "
                        + "Whatever was here may have already been disturbed or removed.",
                    "The scene is clean — too clean for an unmanaged environment. "
                        + "Someone prepared this space before you arrived.",
                    "Nothing catches your eye on the first pass. On the second, you spot "
                        + "a gap on the shelf where something used to be.");
                if (isInterview) return pick(
                    "You miss the subtle cue in their expression. "
                        + "They answer smoothly — too smoothly — but you can't pin down what's off.",
                    "The tell is there but too brief to read. You leave uncertain whether "
                        + "they're hiding something or simply practiced at appearing composed.",
                    "Their story hangs together well enough that you can't find the join. "
                        + "That itself is suspicious — people's honest accounts usually have rough edges.");
                if (isDocument) return pick(
                    "The numbers blur together and the anomaly hides in plain sight. "
                        + "You'll need to come back with fresh eyes or a different approach.",
                    "You can feel something is wrong but can't locate it in this reading. "
                        + "The discrepancy will reveal itself with more time.",
                    "Your eye passes over the altered entry three times without stopping. "
                        + "It's subtle — and whoever made the change knew it would be.");
                if (isPhoto) return pick(
                    "You photograph the scene but miss the critical angle. "
                        + "Later review of your shots shows nothing the initial report didn't already cover.",
                    "The images are technically competent but compositionally wrong. "
                        + "The detail you needed is just outside every frame.",
                    "Back at the office, you realise the key area wasn't covered. "
                        + "You'll need to return for another pass.");
                return pick(
                    "The detail you were looking for doesn't reveal itself this time. "
                        + "It may still be there — consider returning with fresh eyes.",
                    "The environment is too complex and the detail too small for this visit. "
                        + "Returning with a more specific focus should yield better results.",
                    "You miss it this time. It happens. What matters is returning before the window closes.");

            case "INTELLIGENCE":
                if (isDocument) return pick(
                    "The volume and complexity of the paperwork overwhelms you for now. "
                        + "You'll need more context before the pattern emerges.",
                    "The filing structure is deliberately obfuscated — standard for this type of account. "
                        + "More time or a specialist's eye is required.",
                    "You work through two-thirds of the material before the shift changes and you lose access. "
                        + "What you've read doesn't yet form a complete picture.");
                if (isInterview) return pick(
                    "You ask the wrong question and they steer the conversation away. "
                        + "You leave with less than you came with.",
                    "The question you needed to ask only becomes obvious on the drive home. "
                        + "You'll have to find a reason to go back.",
                    "You follow the wrong thread and they use the digression to run out the clock. "
                        + "The conversation ends before you find your way back.");
                if (isEvidence) return pick(
                    "The evidence doesn't connect to anything you already know. "
                        + "The link is there, but without more context you can't see it yet.",
                    "The physical material is suggestive but not conclusive on its own. "
                        + "More corroborating information is needed before this makes sense.",
                    "You can tell this is important, but the significance escapes you today. "
                        + "It will make sense later — once you have the other piece.");
                return pick(
                    "The pieces don't connect yet. There's still a gap somewhere in your reasoning.",
                    "You've got most of it, but one element doesn't fit. That element may change everything.",
                    "The conclusion is close but not provable yet. Missing one key link.");

            case "EMPATHY":
                if (isInterview) return pick(
                    "You try to connect but they remain guarded throughout. "
                        + "Their defensiveness itself might be telling — why would they be so closed off?",
                    "You read the room wrong and lead with the wrong kind of empathy. "
                        + "They shut down instead of opening up.",
                    "They appear cooperative on the surface but offer nothing of substance. "
                        + "The emotional wall is impenetrable today — someone else might reach them better.");
                if (isEvidence) return pick(
                    "You walk the scene looking for anything personal or out of place, "
                        + "but the sterile environment yields nothing to your instinct this time.",
                    "There's no emotional residue in this space — it was sanitised too thoroughly "
                        + "to carry any trace of what happened here.",
                    "The scene speaks, but not loudly enough. Whatever happened here was "
                        + "methodical enough to leave very little feeling behind.");
                if (isDocument) return pick(
                    "You read through the statements but nothing strikes an emotional chord. "
                        + "The writer was careful — or perhaps genuinely uninvolved.",
                    "The language is too neutral to reveal anything about the writer's state of mind. "
                        + "Legal phrasing has been stripped out every human signal.",
                    "The tone of the document is so controlled it gives you nothing to work with. "
                        + "An official prepared this — not a witness.");
                return pick(
                    "You misread the emotional temperature in the room. "
                        + "Your approach creates distance instead of trust.",
                    "The read was wrong. Their composure isn't stoicism — it's indifference. "
                        + "They genuinely don't know what you came to ask.",
                    "The wrong register entirely. Softness where firmness was needed, "
                        + "or the reverse. Either way, the moment has passed.");

            case "MEMORY":
                if (isDocument) return pick(
                    "The relevant detail is just out of reach. You know you've seen something like this before, "
                        + "but you can't recall where. Time to review your notes.",
                    "The name rings a bell but you can't place it. If you'd brought your earlier files "
                        + "the connection would have been clear.",
                    "You leave the archive knowing you missed something — but not knowing what. "
                        + "A full re-read of your previous notes will be necessary.");
                if (isInterview) return pick(
                    "They mention something that should ring a bell, but the connection escapes you in the moment. "
                        + "Write it down — it may make sense later.",
                    "The reference is to an earlier part of this case — something you noted but can't recall precisely. "
                        + "The inconsistency will be clear once you compare.",
                    "Their phrasing is almost identical to something someone else said. Almost. "
                        + "The difference is the detail you needed.");
                if (isEvidence) return pick(
                    "You feel like you've seen this type of item before, but the case reference escapes you. "
                        + "A trip back to the case file might jog your memory.",
                    "The mark matches something, but you can't bring the detail forward. "
                        + "It's there — buried under three years of closed cases.",
                    "You leave the scene without being able to place what you found. "
                        + "The answer is in your notes somewhere. Finding it is the next step.");
                if (isPhoto) return pick(
                    "You can't recall what the scene looked like in the earlier photos, "
                        + "so you miss what changed. Re-check the originals when you get back.",
                    "The comparison you needed to make lives in a file you didn't bring. "
                        + "Without the reference point, the photographs tell you nothing new.",
                    "Something has changed — you're almost certain. "
                        + "But you can't be sure without reviewing the previous set of images.");
                return pick(
                    "The critical detail doesn't surface when you need it. "
                        + "Go back through your earlier findings before proceeding.",
                    "You can feel the memory is close but won't come. "
                        + "It will arrive at two in the morning when it's too late to act on it.",
                    "The gap in recall is frustrating but manageable. "
                        + "The answer is already in your possession — you just haven't re-read it.");

            case "STEALTH":
                if (isEvidence) return pick(
                    "Your presence is noticed earlier than expected. "
                        + "You're forced to abandon the search, but you managed to pocket one small piece of evidence.",
                    "A creak underfoot at the worst moment. You withdraw before anything escalates, "
                        + "but not before pocketing one useful item.",
                    "The light in the room was less forgiving than you expected. "
                        + "You're spotted, you exit cleanly, and you take one photograph on the way out.");
                if (isInterview) return pick(
                    "They spot you watching before you're ready. "
                        + "The element of surprise is lost, and they're now on guard.",
                    "You linger too long and they notice. The conversation that follows is stilted "
                        + "and carefully managed — but the discomfort reveals something.",
                    "Your observation post is compromised. You shift position and resume, "
                        + "but the natural conversation you were hoping for has already ended.");
                if (isPhoto) return pick(
                    "Someone sees your camera and calls you out. "
                        + "You're asked to leave, but not before you notice which area they were most anxious to protect.",
                    "The shutter sound carries further than it should. You're identified and escorted away. "
                        + "But the reaction to the camera itself was revealing.",
                    "You're made before you have the shot you need. "
                        + "What you do get shows someone looking directly at your position. "
                        + "They knew to look.");
                if (isDocument) return pick(
                    "The records clerk spots you in the restricted section. "
                        + "You're escorted out, but you glimpsed a folder label that may narrow your next search.",
                    "The door to the file room wasn't as unattended as it appeared. "
                        + "You're caught in the corridor, but you saw enough to narrow the search.",
                    "An alarm you weren't told about. You leave immediately. "
                        + "The label on the cabinet you had open was still useful.");
                return pick(
                    "You're seen when you should have remained undetected. "
                        + "The opportunity is lost for now — but they don't know exactly what you know.",
                    "Exposed too early. The approach needs rethinking before another attempt.",
                    "The cover is blown. But you got further than they'll realise, "
                        + "and what you found in those first seconds was enough.");

            default:
                return pick(
                    "Without the required skill you fall short this time. "
                        + "There may still be another way to achieve the same result.",
                    "The approach didn't work — but the attempt revealed something about the obstacle itself.",
                    "You come up short. It happens. The question now is what approach will succeed where this one didn't.");
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
                    subject + " had been secretly siphoning funds and needed " + victim + " out of the way before an audit.",
                    "A gambling debt that spiralled out of control left " + subject + " desperate for a fast payout.",
                    "The inheritance would clear every outstanding bill. " + subject + " had done the maths months ago.",
                    subject + " stood to gain a controlling share in the business once " + victim + " was no longer in the picture.",
                    "A forged will, a compliant solicitor, and " + victim + "'s removal — that was " + subject + "'s entire plan.",
                    "When the redundancy cheque ran out, " + subject + " began looking at " + victim + "'s assets in a new light.",
                    subject + " found a clause in the partnership agreement that made everything legal — as long as " + victim + " wasn't around to contest it."
                };
                break;
            case "REVENGE":
                pool = new String[]{
                    subject + " had nursed a grudge against " + victim + " for years after a devastating public humiliation.",
                    "After " + victim + " destroyed " + subject + "'s career, " + subject + " spent months planning retribution.",
                    subject + " blamed " + victim + " for the death of a loved one and vowed to settle the score.",
                    "A bitter feud over a broken promise drove " + subject + " to take drastic action against " + victim + ".",
                    "Ten years had passed, but " + subject + " never forgot what " + victim + " did at the tribunal hearing.",
                    victim + " had testified against " + subject + " and walked free while " + subject + " lost everything.",
                    "The public shaming alone would have been enough. But " + victim + " kept pressing, and " + subject + " reached a breaking point.",
                    subject + " made a promise on the day they were released. " + victim + " was the last name on the list.",
                    "Every setback in " + subject + "'s life traced back to " + victim + "'s actions. Eventually, the accounting had to come due.",
                    "Revenge, " + subject + " maintained, was a matter of principle — not emotion. That distinction had kept them patient for years."
                };
                break;
            case "JEALOUSY":
                pool = new String[]{
                    subject + " could not accept that " + victim + " had been promoted over them despite fewer qualifications.",
                    "A romantic rivalry between " + subject + " and " + victim + " escalated beyond control.",
                    subject + " envied " + victim + "'s social standing and growing influence in the community.",
                    "Watching " + victim + " succeed where " + subject + " had failed became an unbearable obsession.",
                    "The promotion, the award, the recognition — everything went to " + victim + ". " + subject + " had stopped counting.",
                    subject + " and " + victim + " had started out as equals. Somewhere along the way that had changed, and it had never stopped grating.",
                    "Everywhere " + subject + " turned, there was " + victim + "'s name — on the shortlist, in the papers, on everyone's lips.",
                    victim + " had achieved in two years what " + subject + " had chased for a decade. That alone would have been tolerable. It was the smile that wasn't.",
                    "At first it was admiration. Then it curdled into something darker as the distance between them grew.",
                    subject + " had proposed first, invested first, worked harder. " + victim + " got the credit. " + subject + " got the bill."
                };
                break;
            case "COERCION":
                pool = new String[]{
                    subject + " was being blackmailed with compromising photographs and saw no other way out.",
                    "A criminal associate threatened " + subject + "'s family unless " + victim + " was dealt with.",
                    subject + " was manipulated by a third party who stood to gain from " + victim + "'s downfall.",
                    "Under extreme pressure from mounting threats, " + subject + " reluctantly carried out someone else's plan.",
                    "The instructions arrived by courier. The consequences of non-compliance were spelled out clearly. " + subject + " had no real choice.",
                    "Someone had leverage over " + subject + " — exactly what kind, they wouldn't say. But it was enough to make them cooperate.",
                    "The people pulling " + subject + "'s strings had resources and reach. Refusing wasn't an option once the first threat arrived.",
                    subject + " was given a name, a method, and a deadline. Any questions were met with a reminder of what they stood to lose.",
                    "A debt of a different kind — the sort that can't be repaid in instalments — was what finally moved " + subject + " to act.",
                    subject + " went along with it hoping the threat would pass. It didn't, and by the time they understood, it was too late to back out."
                };
                break;
            case "POWER":
                pool = new String[]{
                    subject + " saw " + victim + " as the only obstacle to seizing control of the organisation.",
                    "With " + victim + " out of the picture, " + subject + " would inherit full authority over the estate.",
                    subject + " had long resented " + victim + "'s dominance and orchestrated a takeover.",
                    "Eliminating " + victim + " was the final move in " + subject + "'s carefully planned bid for control.",
                    "The board meeting was three weeks away. " + subject + " needed the votes, and " + victim + " held too many of them.",
                    subject + " had spent a decade cultivating alliances. " + victim + " was the one name that couldn't be won over.",
                    "Control of the foundation meant control of the funds. " + victim + " was the only signature standing in the way.",
                    subject + " had mapped out every contingency — all of them required " + victim + " to be absent for the plan to work.",
                    "Authority, legacy, the chairman's seat — it all passed to " + subject + " once " + victim + " was no longer in position to object.",
                    "The power struggle had been quiet for years. " + subject + " decided it was time to end it decisively."
                };
                break;
            case "SELF_DEFENSE":
                pool = new String[]{
                    subject + " believed " + victim + " was about to expose a secret that would ruin everything.",
                    "After receiving threatening messages from " + victim + ", " + subject + " acted out of genuine fear.",
                    subject + " claimed " + victim + " attacked first, but the evidence suggests a premeditated response.",
                    "Cornered by " + victim + "'s escalating threats, " + subject + " felt there was no alternative.",
                    "The threats had been building for months. " + subject + " maintains they simply reached a point where they had no other options.",
                    victim + " had made specific and credible threats. Whether the response was proportionate is a matter for the courts.",
                    subject + " described an atmosphere of constant intimidation — and says the confrontation was not of their making.",
                    "The night it happened, " + subject + " insists they believed their own life was at risk. The physical evidence is ambiguous.",
                    subject + " had reported " + victim + "'s behaviour three times before the incident. Nothing had been done.",
                    "A pattern of documented harassment preceded the event. " + subject + " maintains they were backed into a corner."
                };
                break;
            case "IDEOLOGY":
                pool = new String[]{
                    subject + " viewed " + victim + "'s activities as a betrayal of deeply held principles and acted accordingly.",
                    "Radicalised through online forums, " + subject + " targeted " + victim + " as a symbol of everything wrong.",
                    subject + " believed silencing " + victim + " would advance a political cause they were devoted to.",
                    "A fanatical commitment to a fringe movement drove " + subject + " to act against " + victim + ".",
                    subject + " kept journals filled with grievances. " + victim + "'s name appeared on every other page.",
                    "The manifesto found at " + subject + "'s address made clear the ideological framework behind the act.",
                    subject + " saw the operation as a principled stand — not a crime. That conviction made them more dangerous, not less.",
                    victim + " represented the system that " + subject + " had dedicated years to dismantling.",
                    "Those closest to " + subject + " described a gradual withdrawal from ordinary life and an increasing fixation on " + victim + ".",
                    subject + " had been radicalising quietly for years. " + victim + "'s public profile made them an obvious target."
                };
                break;
            case "CONCEALMENT":
                pool = new String[]{
                    subject + " had committed a prior offence that " + victim + " was about to report to the authorities.",
                    victim + " stumbled upon " + subject + "'s embezzlement scheme and had to be silenced.",
                    subject + " needed to destroy evidence of a previous fraud before " + victim + " could hand it over.",
                    "With " + victim + " threatening to reveal the truth, " + subject + " acted to protect a web of lies.",
                    "One phone call from " + victim + " and everything " + subject + " had built would have collapsed overnight.",
                    victim + " had kept records — careful, methodical records — that documented exactly what " + subject + " had done.",
                    subject + " had been living under an assumed identity. " + victim + " had recognised them. That couldn't be allowed to stand.",
                    "The scandal had been buried for six years. " + victim + " was the only one who knew where to dig.",
                    subject + " would have faced a prison sentence. The risk was not abstract. The calculation was simple.",
                    victim + " had made copies. " + subject + " knew it — and spent weeks trying to locate them before deciding on a different approach."
                };
                break;
            case "PASSION":
                pool = new String[]{
                    "An intense argument between " + subject + " and " + victim + " escalated into a violent confrontation.",
                    subject + "'s uncontrollable rage after discovering " + victim + "'s betrayal led to a fatal outburst.",
                    "Years of suppressed emotion erupted when " + subject + " confronted " + victim + " about the betrayal.",
                    "A moment of blind fury during a heated exchange drove " + subject + " to act without thinking.",
                    "Neighbours reported shouting. The argument had been building for days. What happened next was not planned.",
                    "There was no calculation involved. " + subject + " reacted in the heat of the moment and has been living with it since.",
                    subject + " describes a blackout — a gap between the argument starting and the silence that followed.",
                    "The relationship between " + subject + " and " + victim + " had been volatile for years. The last confrontation went further than either intended.",
                    victim + " said something that crossed a line " + subject + " hadn't known existed until that moment.",
                    "Witnesses say the exchange began calmly. It was only when " + victim + " made the revelation that everything changed."
                };
                break;
            case "LOYALTY":
                pool = new String[]{
                    subject + " acted to protect a family member who " + victim + " was threatening to expose.",
                    "A close friend of " + subject + " asked for help dealing with " + victim + ", and " + subject + " couldn't refuse.",
                    subject + " took the fall for an associate, believing loyalty demanded sacrifice.",
                    "To shield a loved one from " + victim + "'s harassment, " + subject + " decided to intervene permanently.",
                    "The person " + subject + " was protecting was the one person they would have done anything for. And they did.",
                    subject + " had covered for others before — small things. This time the stakes were categorically different.",
                    "Loyalty, " + subject + " says, is not a weakness. It's the one thing that separates a person from everyone else. " + victim + " had threatened what mattered most.",
                    "The request came from inside the family. " + subject + " didn't ask questions. They should have.",
                    subject + " was led to believe the situation was under control. By the time they understood what they had agreed to, it was already done.",
                    "There was no personal grudge against " + victim + ". " + subject + " acted entirely on behalf of someone else, and accepted the consequences."
                };
                break;
            default:
                pool = new String[]{
                    subject + "'s true motivation remains complex — a mix of personal grievance and opportunity.",
                    "The exact reason " + subject + " targeted " + victim + " stems from a private conflict not yet fully understood.",
                    subject + " was driven by circumstances that created a perfect storm of desperation and opportunity.",
                    "The motive is unclear, but the pattern of behaviour leading up to the incident was deliberate.",
                    subject + " has offered no explanation, and the evidence alone doesn't point to a single obvious cause."
                };
                break;
        }
        return pool[random.nextInt(pool.length)];
    }

    // =========================================================================
    // Layered motives & red-herring motives
    // =========================================================================

    /** All known motive codes used for pairing and red-herring selection. */
    private static final String[] ALL_MOTIVE_CODES = {
        "FINANCIAL_GAIN", "REVENGE", "JEALOUSY", "COERCION", "POWER",
        "SELF_DEFENSE", "IDEOLOGY", "CONCEALMENT", "PASSION", "LOYALTY"
    };

    /**
     * Logical pairings: given a primary motive, which secondary motive
     * could plausibly coexist with it?  Used for layered motives at
     * complexity&nbsp;3.
     */
    private static final String[][] SECONDARY_PAIRINGS = {
        {"FINANCIAL_GAIN", "CONCEALMENT"},
        {"REVENGE",        "PASSION"},
        {"JEALOUSY",       "REVENGE"},
        {"COERCION",       "SELF_DEFENSE"},
        {"POWER",          "FINANCIAL_GAIN"},
        {"SELF_DEFENSE",   "CONCEALMENT"},
        {"IDEOLOGY",       "POWER"},
        {"CONCEALMENT",    "FINANCIAL_GAIN"},
        {"PASSION",        "JEALOUSY"},
        {"LOYALTY",        "COERCION"}
    };

    /**
     * Selects a secondary motive code that pairs logically with the primary.
     * Used at complexity&nbsp;3 to create layered motives — a primary
     * surface-level motive plus a deeper secondary driver.
     *
     * @param primaryCode the primary motive code
     * @return a secondary motive code that pairs with the primary
     */
    public String pickSecondaryMotiveCode(String primaryCode) {
        for (String[] pair : SECONDARY_PAIRINGS) {
            if (pair[0].equals(primaryCode)) {
                return pair[1];
            }
        }
        // Fallback: pick any code that differs from primary
        String fallback;
        do {
            fallback = ALL_MOTIVE_CODES[random.nextInt(ALL_MOTIVE_CODES.length)];
        } while (fallback.equals(primaryCode));
        return fallback;
    }

    /**
     * Builds a secondary-motive narrative for layered motives (complexity&nbsp;3).
     * The secondary motive provides a deeper layer of motivation beyond the
     * primary surface-level motive.
     *
     * @param secondaryCode the secondary motive code
     * @param primaryCode   the primary motive code (for context)
     * @param subject       the perpetrator's name
     * @param victim        the victim's name
     * @return a narrative sentence linking the secondary motive to the case
     */
    public String buildSecondaryMotiveNarrative(String secondaryCode,
                                                 String primaryCode,
                                                 String subject,
                                                 String victim) {
        String secondaryBase = buildMotiveNarrative(secondaryCode, subject, victim);
        String[] connectors = {
            "Beneath the surface, a deeper motive was at work: ",
            "The primary motive was only part of the story. In addition, ",
            "Investigators would later discover a second driving force: ",
            "What appeared straightforward concealed a more complex motivation — "
        };
        return connectors[random.nextInt(connectors.length)] + secondaryBase;
    }

    /**
     * Selects a plausible red-herring motive code — one that differs from
     * the true motive but could appear convincing until contradicted.
     *
     * @param trueMotiveCode the actual motive for the case
     * @return a different motive code suitable as a red herring
     */
    public String pickRedHerringMotiveCode(String trueMotiveCode) {
        // Pick a code that differs from the truth
        String herring;
        do {
            herring = ALL_MOTIVE_CODES[random.nextInt(ALL_MOTIVE_CODES.length)];
        } while (herring.equals(trueMotiveCode));
        return herring;
    }

    /**
     * Builds a red-herring motive narrative — a plausible but ultimately
     * false explanation for the crime.  The text is designed to be
     * convincing early in the investigation but contradicted by later
     * evidence.
     *
     * @param herringCode the false motive code
     * @param subject     the perpetrator's name
     * @param victim      the victim's name
     * @return a narrative sentence for the red-herring motive
     */
    public String buildRedHerringMotiveNarrative(String herringCode,
                                                  String subject,
                                                  String victim) {
        String baseNarrative = buildMotiveNarrative(herringCode, subject, victim);
        String[] wrappers = {
            "Early evidence suggests a different motive: " + baseNarrative
                + " — but this will prove misleading.",
            "Initial interviews point to an apparent motive: " + baseNarrative
                + " However, deeper investigation reveals this is a dead end.",
            "On the surface it appeared that " + baseNarrative
                + " Further analysis contradicts this theory entirely.",
            "Circumstantial evidence initially suggested: " + baseNarrative
                + " This line of inquiry leads nowhere."
        };
        return wrappers[random.nextInt(wrappers.length)];
    }
}
