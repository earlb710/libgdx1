package eb.framework1.investigation;

import eb.framework1.generator.PersonNameGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Encapsulates the generation of pre-scripted interview content for the
 * four core NPC roles (client, subject, witness, associate).
 *
 * <p>Extracted from {@link CaseGenerator} to reduce that class's size and
 * isolate interview-specific logic.  The engine uses a shared {@link Random}
 * so that output is deterministic when seeded.
 *
 * <p>Each role-specific builder produces responses across the standard
 * {@link InterviewTopic} categories (alibi, opinion, whereabouts, etc.),
 * with role-appropriate attribute gates and truthfulness settings.
 */
public class InterviewTemplateEngine {

    private final Random random;

    /**
     * @param random shared random source (same instance used by the
     *               parent generator for reproducibility)
     */
    public InterviewTemplateEngine(Random random) {
        this.random = random != null ? random : new Random();
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Builds interview scripts for all four key NPCs.
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
        List<InterviewScript> scripts = new ArrayList<>();
        boolean isMurder = type == CaseType.MURDER;
        String targetPerson = isMurder ? victim : subject;

        // --- Client script ---
        InterviewScript clientScript = new InterviewScript("npc-client", client, "Client");
        buildClientInterview(clientScript, type, client, subject, victim, clientGender);
        scripts.add(clientScript);

        // --- Subject (suspect) script ---
        InterviewScript subjectScript = new InterviewScript("npc-subject", subject,
                isMurder ? "Subject (Suspect)" : "Subject");
        buildSubjectInterview(subjectScript, type, client, subject, victim, subjectGender);
        scripts.add(subjectScript);

        // --- Key Witness script ---
        String witnessGender = random.nextBoolean() ? "M" : "F";
        String witnessName = nameGen.generateFull(witnessGender);
        InterviewScript witnessScript = new InterviewScript("npc-witness", witnessName,
                "Key Witness");
        buildWitnessInterview(witnessScript, type, client, subject, victim, targetPerson);
        scripts.add(witnessScript);

        // --- Associate script (victim's or subject's associate) ---
        String assocGender = random.nextBoolean() ? "M" : "F";
        String associateName = nameGen.generateFull(assocGender);
        String assocRole = isMurder ? "Victim's Associate" : "Subject's Associate";
        InterviewScript associateScript = new InterviewScript("npc-associate", associateName,
                assocRole);
        buildAssociateInterview(associateScript, type, client, subject, victim,
                targetPerson, associateName);
        scripts.add(associateScript);

        return scripts;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Picks one element at random from the pool. */
    private String pick(String[] pool) {
        return pool[random.nextInt(pool.length)];
    }

    private static String pronoun(String gender) {
        return "F".equals(gender) ? "she" : "he";
    }

    private static String pronounCap(String gender) {
        return capitalize(pronoun(gender));
    }

    static String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        char first = text.charAt(0);
        if (Character.isUpperCase(first)) return text;
        return Character.toUpperCase(first) + text.substring(1);
    }

    // =========================================================================
    // Client interview
    // =========================================================================

    private void buildClientInterview(InterviewScript script, CaseType type,
                                      String client, String subject, String victim,
                                      String clientGender) {
        String pronoun   = pronoun(clientGender);
        String pronounCap = pronounCap(clientGender);
        boolean isMurder = type == CaseType.MURDER;
        String targetPerson = isMurder ? victim : subject;

        // ALIBI
        String[] alibiPool = {
            "I was at home all evening. I can show you my phone records if you need them.",
            "I was having dinner with friends that night. They'll vouch for me.",
            "I was at work late — the security desk will have me on the sign-out log.",
            "I was visiting family out of town. I have the train tickets to prove it."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.ALIBI,
                "Where were you at the time of the incident?",
                pick(alibiPool), true, ""));

        // OPINION of subject — attribute-gated (Empathy reveals deeper insight)
        String[] opinionPool;
        String[] opinionAltPool;
        switch (type) {
            case MURDER:
                opinionPool = new String[]{
                    "I never trusted " + subject + ". " + pronounCap + " was always jealous of " + victim + "'s success.",
                    subject + " had a temper. Everyone knew " + pronoun + " resented " + victim + ".",
                    "Honestly, " + subject + " scared me. There was something cold about " + pronoun + ".",
                    subject + " and " + victim + " used to be close, but something changed. " + pronounCap + " became possessive."
                };
                opinionAltPool = new String[]{
                    "I don't really know what to say about " + subject + ". We weren't that close.",
                    subject + "? I suppose we didn't get along, but I can't put my finger on why.",
                    "I'd rather not say too much about " + subject + ". It's complicated."
                };
                break;
            case INFIDELITY:
                opinionPool = new String[]{
                    subject + " has been distant and secretive lately. I know something is wrong.",
                    "I used to trust " + subject + " completely. Now I'm not sure I know " + pronoun + " at all.",
                    subject + " gets defensive whenever I ask about " + pronoun + " schedule.",
                    "People have told me " + subject + " has been seen with someone else. I need to know the truth."
                };
                opinionAltPool = new String[]{
                    "Things have been different between us lately. That's all I'll say.",
                    "I don't know what's going on with " + subject + ". Something feels off.",
                    subject + " has been busy. I'm sure there's a reason."
                };
                break;
            default:
                opinionPool = new String[]{
                    "I've known " + subject + " for years. " + pronounCap + "'s not who " + pronoun + " pretends to be.",
                    subject + " has always been envious of what others have.",
                    "There's something off about " + subject + ". " + pronounCap + " acts helpful but I don't believe it.",
                    subject + " was always competitive — jealous of anyone who got ahead."
                };
                opinionAltPool = new String[]{
                    "I know " + subject + ", but I'm not sure what to tell you.",
                    subject + " and I aren't exactly close. I can't really say much.",
                    "I don't have a strong opinion about " + subject + ", honestly."
                };
                break;
        }
        script.addResponse(new InterviewResponse(InterviewTopic.OPINION,
                "What do you think of " + subject + "?",
                pick(opinionPool), true, subject,
                "Empathy", 5, pick(opinionAltPool)));

        // LAST CONTACT with target
        String[] lastContactPool = {
            "I last saw " + targetPerson + " about two days before everything happened.",
            "We spoke on the phone three days ago. " + capitalize(targetPerson) + " seemed normal.",
            "I saw " + targetPerson + " the morning before the incident. Nothing seemed wrong.",
            "It's been over a week since I last spoke to " + targetPerson + " properly."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.LAST_CONTACT,
                "When did you last see " + targetPerson + "?",
                pick(lastContactPool), true, targetPerson));

        // OBSERVATION — attribute-gated (Perception reveals more detail)
        String[] observPool = {
            "Now that you mention it, I did notice " + subject + " acting strangely a few days before.",
            "I saw an unfamiliar car parked near " + targetPerson + "'s place more than once.",
            "There were raised voices coming from " + targetPerson + "'s flat the night before.",
            "I noticed " + subject + " was unusually nervous the last time we spoke."
        };
        String[] observAltPool = {
            "I'm not sure. I don't think I noticed anything out of the ordinary.",
            "Nothing comes to mind. Sorry, I wish I could be more helpful.",
            "I wasn't really paying attention to anything in particular."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.OBSERVATION,
                "Did you notice anything unusual around the time of the incident?",
                pick(observPool), true, "",
                "Perception", 5, pick(observAltPool)));

        // MOTIVE — attribute-gated (Intuition reveals motive insight)
        if (isMurder) {
            String[] motivePool = {
                subject + " was jealous of " + victim + "'s relationship with others. It ate " + pronoun + " up inside.",
                "I heard " + subject + " and " + victim + " had a falling out over money. " + subject + " felt cheated.",
                victim + " was about to expose something about " + subject + ". I think " + pronoun + " panicked.",
                subject + " always wanted what " + victim + " had — the status, the connections, everything."
            };
            String[] motiveAltPool = {
                "I don't know why anyone would do this. It's terrible.",
                "I can't think of anyone specific. I'm sorry.",
                "I wish I knew. I've been asking myself the same question."
            };
            script.addResponse(new InterviewResponse(InterviewTopic.MOTIVE,
                    "Can you think of anyone who would want to harm " + victim + "?",
                    pick(motivePool), true, victim,
                    "Intuition", 5, pick(motiveAltPool)));
        }

        // CONTACT_INFO — Charisma-gated
        String[] contactPool = {
            "I have " + subject + "'s number. Let me find it for you — you can call and arrange to meet.",
            subject + "'s number? Yes, I have it. You should be able to reach " + subject + " directly.",
            "Here, take " + subject + "'s number. Good luck getting a straight answer out of " + subject + ".",
            "I can give you " + subject + "'s details. " + capitalize(pronoun) + " usually hangs around the usual places."
        };
        String[] contactAltPool = {
            "I'm not sure I should give that out. Can you come back when I've had time to think?",
            "I'd rather not share anyone's number without their permission.",
            "I don't have it on me right now. Sorry."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.CONTACT_INFO,
                "Do you have a way to reach " + subject + "?",
                pick(contactPool), true, subject,
                "Charisma", 4, pick(contactAltPool)));
    }

    // =========================================================================
    // Subject interview
    // =========================================================================

    private void buildSubjectInterview(InterviewScript script, CaseType type,
                                       String client, String subject, String victim,
                                       String subjectGender) {
        String pronoun   = pronoun(subjectGender);
        String pronounCap = pronounCap(subjectGender);
        boolean isMurder = type == CaseType.MURDER;
        String targetPerson = isMurder ? victim : client;

        // ALIBI — subject may lie
        boolean truthfulAlibi = random.nextInt(10) < 3; // 30% chance truthful
        String[] truthfulAlibiPool = {
            "I was at a bar downtown. The bartender knows me — you can check.",
            "I was at home watching television. I know it's not much of an alibi.",
            "I was out for a walk that evening. No one saw me, as far as I know."
        };
        String[] falseAlibiPool = {
            "I was with a friend all night. We were playing cards until past midnight.",
            "I was working late at the office. Check the security cameras if you want.",
            "I was at a restaurant across town. I'm sure they'll remember me."
        };
        String[] alibiPool = truthfulAlibi ? truthfulAlibiPool : falseAlibiPool;
        script.addResponse(new InterviewResponse(InterviewTopic.ALIBI,
                "Where were you at the time of the incident?",
                pick(alibiPool), truthfulAlibi, ""));

        // OPINION of client
        String[] opinionOfClientPool = {
            client + " has always had it in for me. Don't believe everything " + pronoun + " tells you.",
            "I barely know " + client + ". We've spoken maybe twice.",
            client + " is paranoid. " + pronounCap + " sees conspiracies everywhere.",
            "I have nothing against " + client + ". I don't know why " + pronoun + "'s dragging me into this."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.OPINION,
                "What do you think of " + client + "?",
                pick(opinionOfClientPool), random.nextBoolean(), client));

        // OPINION of victim (for murder)
        if (isMurder) {
            boolean truthfulOpinion = random.nextInt(10) < 4; // 40% truthful
            String[] truthfulVictimPool = {
                victim + " and I had our differences, I won't deny that.",
                "I'll admit I was angry with " + victim + " about some things. But I didn't do this.",
                victim + " crossed me, yes. But that doesn't make me a killer."
            };
            String[] falseVictimPool = {
                victim + " and I were perfectly fine. There was no bad blood between us.",
                "I liked " + victim + ". We got along well. This whole thing is a misunderstanding.",
                victim + "? We were on good terms. Ask anyone — they'll tell you the same."
            };
            String[] pool = truthfulOpinion ? truthfulVictimPool : falseVictimPool;
            script.addResponse(new InterviewResponse(InterviewTopic.OPINION,
                    "What was your relationship with " + victim + "?",
                    pick(pool), truthfulOpinion, victim));
        }

        // WHEREABOUTS
        String[] whereaboutsPool = {
            "I have no idea where " + targetPerson + " was. Why would I?",
            "How should I know? I wasn't keeping tabs on " + targetPerson + ".",
            "I think " + targetPerson + " was at home, but don't quote me on that.",
            "Last I heard, " + targetPerson + " was going to meet someone. I don't know who."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.WHEREABOUTS,
                "Do you know where " + targetPerson + " was at the time?",
                pick(whereaboutsPool), random.nextBoolean(), targetPerson));

        // OBSERVATION — Intimidation-gated
        String[] observPool = {
            "I didn't notice anything. I've been keeping to myself lately.",
            "Nothing unusual. Everything seemed normal to me.",
            "I try to mind my own business. I suggest you do the same.",
            "Look, I don't watch people. I had my own things going on."
        };
        String[] observRevealPool = {
            "Fine. I did see something that night. There was someone else hanging around the area, but I don't know who.",
            "Alright, alright. I noticed things were off. " + targetPerson + " had been acting scared for days.",
            "Okay, look — there was a meeting. I overheard part of it. Voices were raised.",
            "I'll tell you this much — " + targetPerson + " was expecting trouble. They told me so."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.OBSERVATION,
                "Did you notice anything unusual around the time of the incident?",
                pick(observRevealPool), random.nextBoolean(), "",
                "Intimidation", 6, pick(observPool)));

        // LAST CONTACT
        boolean truthfulContact = random.nextInt(10) < 4; // 40% truthful
        String[] truthfulContactPool = {
            "I saw " + targetPerson + " that same day, earlier in the afternoon.",
            "We spoke on the phone that morning. It was brief — nothing important.",
            "I ran into " + targetPerson + " at a café a couple of days before."
        };
        String[] falseContactPool = {
            "I haven't seen " + targetPerson + " in weeks. We don't run in the same circles.",
            "I can't remember the last time I saw " + targetPerson + ". It's been a long time.",
            "We haven't been in touch. Not for months."
        };
        String[] contactPool = truthfulContact ? truthfulContactPool : falseContactPool;
        script.addResponse(new InterviewResponse(InterviewTopic.LAST_CONTACT,
                "When did you last see " + targetPerson + "?",
                pick(contactPool), truthfulContact, targetPerson));

        // MOTIVE — Intimidation-gated
        if (isMurder) {
            String[] motiveDeflectPool = {
                "Why are you asking me? You should be looking at " + client + ".",
                "I don't know who would do this. But I know it wasn't me.",
                "Plenty of people had issues with " + victim + ". I'm not the only one.",
                "Have you checked " + victim + "'s financial records? There were debts. People were owed money."
            };
            String[] motiveRevealPool = {
                "Fine. " + victim + " and I had problems. But there are others who had it worse with " + victim + ".",
                "You want the truth? " + victim + " made enemies. I was one of them, but I'm not the only one.",
                "Look, I'll admit it — " + victim + " wronged me. But I didn't do this. Check " + client + "'s story.",
                "Alright. Yes, I was angry with " + victim + ". But killing? That's not me. Someone else was circling."
            };
            script.addResponse(new InterviewResponse(InterviewTopic.MOTIVE,
                    "Can you think of anyone who would want to harm " + victim + "?",
                    pick(motiveRevealPool), random.nextBoolean(), victim,
                    "Intimidation", 7, pick(motiveDeflectPool)));
        }

        // CONTACT_INFO — Subject refuses
        String[] contactRefusalPool = {
            "Why would I help you track down anyone else? Leave me alone.",
            "I'm not giving you anyone's number. You can find them yourself.",
            "I don't hand out other people's details. Ask someone who cares."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.CONTACT_INFO,
                "Do you have a way to reach " + client + "?",
                pick(contactRefusalPool), random.nextBoolean(), client));
    }

    // =========================================================================
    // Witness interview
    // =========================================================================

    private void buildWitnessInterview(InterviewScript script, CaseType type,
                                       String client, String subject, String victim,
                                       String targetPerson) {
        boolean isMurder = type == CaseType.MURDER;

        // ALIBI
        String[] alibiPool = {
            "I was in the neighbourhood that evening, on my way home from work.",
            "I was visiting a friend nearby. I passed through the area around the time it happened.",
            "I was at the local shop. I remember because it was close to closing time.",
            "I was walking the dog. That's how I ended up seeing what I saw."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.ALIBI,
                "Where were you at the time of the incident?",
                pick(alibiPool), true, ""));

        // WHEREABOUTS of subject
        String[] subjectWhereaboutsPool = {
            "I saw " + subject + " near " + targetPerson + "'s place that evening. " + subject + " looked agitated.",
            "I'm pretty sure " + subject + " was in the area. I recognised the car.",
            "I didn't see " + subject + " personally, but a neighbour mentioned spotting someone matching the description.",
            subject + " was definitely around. I saw someone who looked just like them leaving in a hurry."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.WHEREABOUTS,
                "Did you see " + subject + " near the scene?",
                pick(subjectWhereaboutsPool), true, subject));

        // OPINION of subject — Empathy-gated
        String[] opinionPool = {
            subject + " has always been the jealous type. Envious of anyone who had more.",
            "I've heard " + subject + " has a short temper. People around here are wary of confrontation.",
            subject + " seemed charming on the surface, but there was something calculating underneath.",
            "People say " + subject + " was possessive. Couldn't stand others having what they wanted.",
            subject + " was competitive to the point of obsession. Always comparing, always resentful.",
            "I wouldn't call " + subject + " violent, but there was a bitterness. A deep grudge."
        };
        String[] opinionAltPool = {
            "I don't know " + subject + " well enough to say.",
            subject + "? Seemed normal enough to me. I can't really comment.",
            "I've seen " + subject + " around but I couldn't tell you much about their character."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.OPINION,
                "What can you tell me about " + subject + "'s character?",
                pick(opinionPool), true, subject,
                "Empathy", 6, pick(opinionAltPool)));

        // OPINION of victim/target
        if (isMurder) {
            String[] victimOpinionPool = {
                victim + " was well-liked by most people. I can't imagine who would do this.",
                victim + " had a way of rubbing some people the wrong way, but nothing that would justify this.",
                "Everyone around here knew " + victim + ". A decent person. This has shaken the whole street.",
                victim + " was private. Kept to themselves. I don't know much about their personal life."
            };
            script.addResponse(new InterviewResponse(InterviewTopic.OPINION,
                    "What was " + victim + " like?",
                    pick(victimOpinionPool), true, victim));
        }

        // OBSERVATION — Perception-gated
        String[] observPool = {
            "I heard raised voices from the direction of " + targetPerson + "'s place that night.",
            "There was a car I didn't recognise parked outside for hours. It left in a hurry.",
            "I noticed the lights were on unusually late at " + targetPerson + "'s. That's not normal.",
            "I saw someone running from the area around 11 PM. I couldn't make out who it was.",
            "Things had been tense in the neighbourhood. There were arguments in the days before."
        };
        String[] observAltPool = {
            "I might have heard something, but I can't be sure.",
            "Nothing stands out in particular. It was a normal evening.",
            "I was busy with my own things. I didn't pay much attention."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.OBSERVATION,
                "Did you notice anything unusual around the time of the incident?",
                pick(observPool), true, "",
                "Perception", 6, pick(observAltPool)));

        // LAST CONTACT
        String[] contactPool = {
            "I said hello to " + targetPerson + " the day before. Seemed fine.",
            "I saw " + targetPerson + " a few days ago at the shops. We didn't really talk.",
            "We're not close, but I saw " + targetPerson + " that week. Nothing seemed off.",
            "I can't remember exactly — maybe three or four days before."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.LAST_CONTACT,
                "When did you last see " + targetPerson + "?",
                pick(contactPool), true, targetPerson));

        // MOTIVE — Intuition-gated
        if (isMurder) {
            String[] motivePool = {
                "I always thought " + subject + " was jealous of " + victim + ". " + subject + " couldn't hide it.",
                "There was bad blood between " + subject + " and " + victim + ". Everyone could see it.",
                subject + " once said something that stuck with me — about " + victim + " getting what was coming.",
                "I don't want to point fingers, but " + subject + " had more reason than anyone."
            };
            String[] motiveAltPool = {
                "I can't think of anyone specific. It's a mystery to me.",
                "I don't like to speculate. I really don't know.",
                "I wouldn't want to accuse anyone. I'm just a witness."
            };
            script.addResponse(new InterviewResponse(InterviewTopic.MOTIVE,
                    "Do you know of anyone who might have wanted to harm " + victim + "?",
                    pick(motivePool), true, victim,
                    "Intuition", 5, pick(motiveAltPool)));
        }

        // CONTACT_INFO — Charisma-gated
        String[] contactRevealPool = {
            "I think I have " + subject + "'s number somewhere. Let me check — yes, here it is.",
            subject + "? I've seen them around. I might have their number from the neighbourhood group.",
            "I'm not sure I have " + subject + "'s number, but they're usually around the area."
        };
        String[] contactRefusePool = {
            "I don't really know " + subject + " well enough to share their details.",
            "Sorry, I don't feel comfortable giving out contact information.",
            "I'd rather you found them yourself. I don't want to get involved."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.CONTACT_INFO,
                "Do you have a way to reach " + subject + "?",
                pick(contactRevealPool), true, subject,
                "Charisma", 5, pick(contactRefusePool)));
    }

    // =========================================================================
    // Associate interview
    // =========================================================================

    private void buildAssociateInterview(InterviewScript script, CaseType type,
                                         String client, String subject, String victim,
                                         String targetPerson, String associateName) {
        boolean isMurder = type == CaseType.MURDER;

        // ALIBI
        String[] alibiPool = {
            "I was at home that night. I went to bed early — I had an early start the next day.",
            "I was out with colleagues after work. We were at a restaurant until about 10 PM.",
            "I was at the gym until around 8 PM, then went straight home.",
            "I was travelling back from a meeting. The train was delayed — I have the ticket."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.ALIBI,
                "Where were you at the time of the incident?",
                pick(alibiPool), true, ""));

        // RELATIONSHIP with target
        String[] relationshipPool;
        if (isMurder) {
            relationshipPool = new String[]{
                victim + " and I were close. We'd known each other for years. This is devastating.",
                "I worked with " + victim + " for a long time. We were colleagues and friends.",
                victim + " was like family to me. I can't believe this has happened.",
                "We weren't best friends, but I respected " + victim + ". A good person."
            };
        } else {
            relationshipPool = new String[]{
                "I've known " + subject + " professionally for several years.",
                subject + " and I are acquainted through mutual contacts.",
                "We used to work together. I know " + subject + " fairly well.",
                "I wouldn't say we're close, but I know " + subject + " well enough."
            };
        }
        script.addResponse(new InterviewResponse(InterviewTopic.RELATIONSHIP,
                "How do you know " + targetPerson + "?",
                pick(relationshipPool), true, targetPerson));

        // OPINION of subject — Empathy-gated
        String[] opinionPool = {
            subject + " was always envious. The kind of person who measures themselves against everyone else.",
            "I noticed " + subject + " becoming more controlling and possessive over time. It worried me.",
            subject + " has a charming side, but underneath there's a deep insecurity. Jealousy drives a lot of what " + subject + " does.",
            "Honestly? " + subject + " is manipulative. " + subject + " tells people what they want to hear.",
            subject + " was resentful — especially about money. Always felt shortchanged.",
            "I've seen " + subject + " fly into a rage over small things. There's a violent streak there."
        };
        String[] opinionAltPool = {
            subject + "? We got along fine. I can't say I noticed anything unusual.",
            "I don't know " + subject + " well enough to comment on their personality.",
            subject + " seemed like anyone else. Nothing stood out to me."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.OPINION,
                "What's your impression of " + subject + "?",
                pick(opinionPool), true, subject,
                "Empathy", 6, pick(opinionAltPool)));

        // WHEREABOUTS
        String[] whereaboutsPool = {
            "I believe " + subject + " was supposed to be somewhere else that night, but I can't confirm.",
            subject + " told me " + subject + " was going to be at home. Whether that's true, I don't know.",
            "I haven't spoken to " + subject + " about that night. I'd rather not speculate.",
            "Someone mentioned seeing " + subject + " in the area, but I can't say for certain."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.WHEREABOUTS,
                "Do you know where " + subject + " was at the time?",
                pick(whereaboutsPool), random.nextBoolean(), subject));

        // OBSERVATION — Perception-gated
        String[] observPool = {
            "In the weeks before, " + subject + " was acting differently. More secretive, more on edge.",
            targetPerson + " mentioned feeling watched or followed. I didn't take it seriously at the time.",
            "There was a lot of tension between " + subject + " and " + targetPerson + " recently.",
            "I noticed " + subject + " drinking more than usual. Something was clearly bothering them."
        };
        String[] observAltPool = {
            "I can't say I noticed anything unusual. Everything seemed normal.",
            "I wasn't around much in the weeks before, so I can't really say.",
            "Nothing specific comes to mind. I'm sorry."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.OBSERVATION,
                "Did you notice anything unusual in the weeks leading up to the incident?",
                pick(observPool), true, "",
                "Perception", 5, pick(observAltPool)));

        // LAST CONTACT
        String[] contactPool = {
            "I spoke to " + targetPerson + " two days before. We had a normal conversation.",
            "I saw " + targetPerson + " at work a few days before. Everything seemed routine.",
            "We had lunch together the week before. " + capitalize(targetPerson) + " seemed a bit distracted but fine.",
            "I can't remember exactly when I last saw " + targetPerson + ". Maybe four or five days before."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.LAST_CONTACT,
                "When did you last see " + targetPerson + "?",
                pick(contactPool), true, targetPerson));

        // MOTIVE — Intuition-gated
        if (isMurder) {
            String[] motivePool = {
                subject + " and " + victim + " had a bitter dispute about money. " + subject + " never got over it.",
                "I know " + subject + " was jealous of " + victim + "'s position. " + subject + " felt passed over and humiliated.",
                victim + " knew something about " + subject + " — something " + subject + " wanted kept quiet.",
                subject + " was possessive about " + victim + ". When things soured, it turned ugly.",
                "There was a rivalry between them. " + subject + " couldn't stand that " + victim + " was doing better."
            };
            String[] motiveAltPool = {
                "I honestly don't know. This is all such a shock.",
                "I can't imagine why anyone would do something like this.",
                "I'd rather not speculate. I don't want to say the wrong thing."
            };
            script.addResponse(new InterviewResponse(InterviewTopic.MOTIVE,
                    "Why do you think someone might have wanted to harm " + victim + "?",
                    pick(motivePool), true, victim,
                    "Intuition", 6, pick(motiveAltPool)));
        }

        // CONTACT_INFO — Charisma-gated
        String[] contactInfoPool = {
            "Yes, I have " + subject + "'s number. We used to work together. Here — take it.",
            subject + "? Sure, I can give you their number. We've been in touch recently.",
            "I can give you " + subject + "'s details. Just tell them I sent you.",
            "I've got " + subject + "'s number. They might not pick up, but it's worth a try."
        };
        String[] contactInfoAltPool = {
            "I'd rather not share " + subject + "'s details without asking first.",
            "I don't think " + subject + " would appreciate me giving out their number.",
            "You'll have to find them through other channels. I can't help with that."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.CONTACT_INFO,
                "Do you have a way to reach " + subject + "?",
                pick(contactInfoPool), true, subject,
                "Charisma", 4, pick(contactInfoAltPool)));
    }
}
