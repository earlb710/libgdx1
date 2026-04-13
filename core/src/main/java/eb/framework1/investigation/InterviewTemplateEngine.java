package eb.framework1.investigation;

import eb.framework1.RandomUtils;
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

    // Word pools for personality-trait interview responses
    private static final String[] HOBBY_WORDS = {
        "sports", "hiking", "cooking", "reading", "music",
        "art", "gambling", "travel", "photography", "gardening",
        "chess", "cycling", "fishing", "theatre", "DIY projects"
    };
    private static final String[] SOCIAL_WORDS = {
        "enjoys socializing with everyone", "prefers being alone",
        "loves to gossip", "tends to flirt with people",
        "likes taking risks", "is very respectful of authority",
        "is obsessed with money and status", "has a soft spot for animals",
        "keeps a small, tight circle of friends", "is overly competitive",
        "never misses a social event", "rarely opens up to anyone new",
        "is generous to a fault", "keeps grudges for years"
    };
    private static final String[] LIKE_DISLIKE_WORDS = {
        "loves", "can't stand", "really enjoys", "has no interest in",
        "is passionate about", "absolutely hates", "is quietly obsessed with",
        "tolerates at best", "openly avoids", "gets animated talking about"
    };
    private static final String[] LOCATION_CLUE_WORDS = {
        "the park nearby", "the gym on Main Street",
        "that café by the station", "the library downtown",
        "the bar on 5th Avenue", "the snooker hall behind the market",
        "the waterfront promenade", "a community centre across the river",
        "the back room of an accountant's office", "a rooftop bar in the centre"
    };

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
        String witnessGender = RandomUtils.randomGender(random);
        String witnessName = nameGen.generateFull(witnessGender);
        InterviewScript witnessScript = new InterviewScript("npc-witness", witnessName,
                "Key Witness");
        buildWitnessInterview(witnessScript, type, client, subject, victim, targetPerson);
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

    private static String capitalize(String text) {
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
            "I was visiting family out of town. I have the train tickets to prove it.",
            "I was at a community meeting. At least a dozen people saw me there.",
            "I was at the hospital with my mother. The nurses can confirm.",
            "I was on a call — a long one. The phone records will show it.",
            "I was travelling back from the airport. I can pull up the flight details."
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
                    subject + " and " + victim + " used to be close, but something changed. " + pronounCap + " became possessive.",
                    "The relationship between " + subject + " and " + victim + " had turned toxic months before this happened.",
                    subject + " made comments. Little digs at " + victim + ". I noticed, but I didn't say anything.",
                    "I saw " + subject + " watching " + victim + " once — the kind of look you don't forget.",
                    subject + " was obsessed with " + victim + "'s life. What " + pronoun + " had, who " + pronoun + " knew."
                };
                opinionAltPool = new String[]{
                    "I don't really know what to say about " + subject + ". We weren't that close.",
                    subject + "? I suppose we didn't get along, but I can't put my finger on why.",
                    "I'd rather not say too much about " + subject + ". It's complicated.",
                    "I can't really comment on " + subject + ". We moved in different circles.",
                    subject + " and I have history, but I'd rather leave it at that.",
                    "I don't know enough about " + subject + " to say anything useful."
                };
                break;
            case INFIDELITY:
                opinionPool = new String[]{
                    subject + " has been distant and secretive lately. I know something is wrong.",
                    "I used to trust " + subject + " completely. Now I'm not sure I know " + pronoun + " at all.",
                    subject + " gets defensive whenever I ask about " + pronoun + " schedule.",
                    "People have told me " + subject + " has been seen with someone else. I need to know the truth.",
                    "There's a coldness between us now. " + pronounCap + " won't look me in the eye the way " + pronoun + " used to.",
                    subject + " keeps the phone on silent. At the dinner table. I've noticed.",
                    "The small lies started first. Then I stopped believing the big ones.",
                    subject + " is putting on a performance. I can feel it — I just need the proof."
                };
                opinionAltPool = new String[]{
                    "Things have been different between us lately. That's all I'll say.",
                    "I don't know what's going on with " + subject + ". Something feels off.",
                    subject + " has been busy. I'm sure there's a reason.",
                    "I'm not sure I should be talking about this. It's personal.",
                    "Let's just say things haven't been great lately.",
                    subject + " has their reasons, I suppose. I just don't know what they are."
                };
                break;
            default:
                opinionPool = new String[]{
                    "I've known " + subject + " for years. " + pronounCap + "'s not who " + pronoun + " pretends to be.",
                    subject + " has always been envious of what others have.",
                    "There's something off about " + subject + ". " + pronounCap + " acts helpful but I don't believe it.",
                    subject + " was always competitive — jealous of anyone who got ahead.",
                    subject + " plays the long game. Patient, quiet, always watching.",
                    "I've caught " + subject + " in small lies before. The big ones are harder to spot.",
                    subject + " is charming when it suits " + pronoun + ". Less so otherwise.",
                    "I wouldn't turn my back on " + subject + ". That's my honest assessment."
                };
                opinionAltPool = new String[]{
                    "I know " + subject + ", but I'm not sure what to tell you.",
                    subject + " and I aren't exactly close. I can't really say much.",
                    "I don't have a strong opinion about " + subject + ", honestly.",
                    "I wouldn't want to say anything I couldn't support.",
                    subject + " is... complicated. I'll leave it there.",
                    "I've only seen " + subject + " in certain situations. Hard to generalise."
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
            "It's been over a week since I last spoke to " + targetPerson + " properly.",
            "We had coffee together three days before. I had no idea it would be the last time.",
            "I texted " + targetPerson + " the day before. No reply — which I should have taken more seriously.",
            "I saw " + targetPerson + " at work last week. Everything seemed fine on the surface.",
            "We spoke briefly on the phone. A few hours before it happened."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.LAST_CONTACT,
                "When did you last see " + targetPerson + "?",
                pick(lastContactPool), true, targetPerson));

        // OBSERVATION — attribute-gated (Perception reveals more detail)
        String[] observPool = {
            "Now that you mention it, I did notice " + subject + " acting strangely a few days before.",
            "I saw an unfamiliar car parked near " + targetPerson + "'s place more than once.",
            "There were raised voices coming from " + targetPerson + "'s flat the night before.",
            "I noticed " + subject + " was unusually nervous the last time we spoke.",
            subject + " was watching the building from across the street. I saw " + pronoun + " twice.",
            "There was a conversation between " + subject + " and someone I didn't recognise. Hushed. Tense.",
            targetPerson + " told me they'd been getting calls from an unknown number. I didn't think much of it then.",
            "I found notes that suggested " + targetPerson + " was meeting someone they didn't want me to know about."
        };
        String[] observAltPool = {
            "I'm not sure. I don't think I noticed anything out of the ordinary.",
            "Nothing comes to mind. Sorry, I wish I could be more helpful.",
            "I wasn't really paying attention to anything in particular.",
            "Everything seemed normal at the time. I didn't think to look.",
            "If I had noticed something, I'd have said something sooner.",
            "Nothing jumped out at me. But then, I wasn't looking."
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
                subject + " always wanted what " + victim + " had — the status, the connections, everything.",
                "There were arguments. Escalating arguments. I warned " + victim + " to be careful.",
                subject + " had made threats before — indirect ones, but clear enough if you knew them.",
                victim + " was planning to cut " + subject + " out completely. " + subject + " must have known.",
                "It's always about control with " + subject + ". " + victim + " was slipping beyond " + pronoun + "r reach."
            };
            String[] motiveAltPool = {
                "I don't know why anyone would do this. It's terrible.",
                "I can't think of anyone specific. I'm sorry.",
                "I wish I knew. I've been asking myself the same question.",
                "There are things I'm not sure I should be saying. Not yet.",
                "I don't want to point fingers without more to go on.",
                "The whole thing is beyond me. I keep trying to make sense of it."
            };
            script.addResponse(new InterviewResponse(InterviewTopic.MOTIVE,
                    "Can you think of anyone who would want to harm " + victim + "?",
                    pick(motivePool), true, victim,
                    "Intuition", 5, pick(motiveAltPool)));
        }

        // RELATIONSHIP with the target person
        String[] clientRelPool = {
            "I've known " + targetPerson + " for years. That's why this hurts so much.",
            targetPerson + " and I are — were — close.",
            "We're family. Or as close as family gets.",
            "I've known " + targetPerson + " most of my life.",
            "We met through work, years ago. It became something more than that.",
            targetPerson + " is someone I trusted completely. That's the short version.",
            "We go back a long way. It's hard to explain to someone who doesn't know us.",
            "I thought I knew " + targetPerson + " better than anyone. That's what makes this so difficult."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.RELATIONSHIP,
                "How do you know " + targetPerson + "?",
                pick(clientRelPool), true, targetPerson));

        // CONTACT_INFO — Charisma-gated
        String[] contactPool = {
            "I have " + subject + "'s number. Let me find it for you — you can call and arrange to meet.",
            subject + "'s number? Yes, I have it. You should be able to reach " + subject + " directly.",
            "Here, take " + subject + "'s number. Good luck getting a straight answer out of " + subject + ".",
            "I can give you " + subject + "'s details. " + capitalize(pronoun) + " usually hangs around the usual places.",
            "I'll give you " + subject + "'s number. Whether " + pronoun + " picks up is another matter.",
            subject + " isn't easy to reach, but I have a number that used to work.",
            "I have " + subject + "'s details here. You didn't get them from me.",
            subject + "'s number is in my contacts. I'll read it out to you."
        };
        String[] contactAltPool = {
            "I'm not sure I should give that out. Can you come back when I've had time to think?",
            "I'd rather not share anyone's number without their permission.",
            "I don't have it on me right now. Sorry.",
            "That's not something I'm comfortable doing today.",
            "I'd need to speak to " + subject + " first. Give me a day.",
            "I have it, but I'd rather you found another way. It's complicated."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.CONTACT_INFO,
                "Do you have a way to reach " + subject + "?",
                pick(contactPool), true, subject,
                "Charisma", 4, pick(contactAltPool)));

        // PERSONALITY — reveal subject's hidden traits (Empathy-gated)
        String[] personalityPool = {
            "Well, I know " + subject + " is really into " + pick(HOBBY_WORDS) + ". Always talking about it.",
            subject + " can't stand " + pick(HOBBY_WORDS) + ", I can tell you that much. But " + pronoun + " absolutely loves " + pick(HOBBY_WORDS) + ".",
            "If you want to find " + subject + ", try " + pick(LOCATION_CLUE_WORDS) + ". " + capitalize(pronoun) + " spends a lot of time there.",
            subject + " is the type who " + pick(SOCIAL_WORDS) + ". That much I know for certain.",
            "I know " + subject + " has a thing for " + pick(HOBBY_WORDS) + ". Wouldn't stop talking about it.",
            "You'd find " + subject + " at " + pick(LOCATION_CLUE_WORDS) + " most evenings. Creature of habit.",
            subject + " " + pick(LIKE_DISLIKE_WORDS) + " " + pick(HOBBY_WORDS) + ". It was all " + pronoun + " talked about.",
            "If you're looking for insight — " + subject + " " + pick(SOCIAL_WORDS) + ". That defines " + pronoun + " as much as anything."
        };
        String[] personalityAltPool = {
            "I don't really know " + subject + " well enough to say what " + pronoun + " likes.",
            "We weren't close enough for me to know " + subject + "'s hobbies.",
            "I can't say I paid much attention to " + subject + "'s interests.",
            "We never really talked about that kind of thing.",
            "I knew " + subject + " professionally. The personal side — I couldn't say.",
            "I'm not the best person to ask about " + subject + "'s private life."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.PERSONALITY,
                "What are " + subject + "'s interests or hobbies?",
                pick(personalityPool), true, subject,
                "Empathy", 4, pick(personalityAltPool)));
    }

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
            "I was out for a walk that evening. No one saw me, as far as I know.",
            "I was at my sister's place. She'll tell you the same if you ask her.",
            "I was home alone. I ordered a takeaway — the delivery record will show the time.",
            "I was at the gym. The entry log will have my key fob scan."
        };
        String[] falseAlibiPool = {
            "I was with a friend all night. We were playing cards until past midnight.",
            "I was working late at the office. Check the security cameras if you want.",
            "I was at a restaurant across town. I'm sure they'll remember me.",
            "I was at a private function. I can get you names if you need them.",
            "I drove out to see someone. Long drive. I wasn't anywhere near the scene.",
            "I was on the phone most of that evening. International call — the records will show it."
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
            "I have nothing against " + client + ". I don't know why " + pronoun + "'s dragging me into this.",
            client + " is fishing for someone to blame. I happen to be convenient.",
            "You can ask " + client + " whatever you like, but take everything " + pronoun + " says with a pinch of salt.",
            "I know what " + client + " thinks of me. " + pronounCap + " made that clear a long time ago.",
            client + " and I have history. Old grievances. This isn't really about what happened."
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
                victim + " crossed me, yes. But that doesn't make me a killer.",
                "We had a falling out months ago. I'll be honest about that. But what happened — that wasn't me.",
                "I'm not going to pretend " + victim + " and I got along. We didn't. But there's a line.",
                "Things were strained between us. I've said that from the beginning."
            };
            String[] falseVictimPool = {
                victim + " and I were perfectly fine. There was no bad blood between us.",
                "I liked " + victim + ". We got along well. This whole thing is a misunderstanding.",
                victim + "? We were on good terms. Ask anyone — they'll tell you the same.",
                "We had no issues. None. Whoever told you otherwise is mistaken.",
                "I don't know where this idea of conflict comes from. " + victim + " and I were civil.",
                victim + " was a decent person. I had no reason to do anything to " + victim + "."
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
            "Last I heard, " + targetPerson + " was going to meet someone. I don't know who.",
            "You'd have to ask someone closer to " + targetPerson + " than I am.",
            targetPerson + " moves around a lot. I couldn't tell you where " + targetPerson + " was on any given night.",
            "I'm not " + targetPerson + "'s keeper. I don't know and didn't ask.",
            "I heard " + targetPerson + " mention something about going out that evening. That's all I know."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.WHEREABOUTS,
                "Do you know where " + targetPerson + " was at the time?",
                pick(whereaboutsPool), random.nextBoolean(), targetPerson));

        // OBSERVATION — Intimidation-gated
        String[] observPool = {
            "I didn't notice anything. I've been keeping to myself lately.",
            "Nothing unusual. Everything seemed normal to me.",
            "I try to mind my own business. I suggest you do the same.",
            "Look, I don't watch people. I had my own things going on.",
            "I wasn't around much that week. Can't help you.",
            "I keep my head down. It's not my business what others are doing.",
            "You're asking the wrong person. I don't pay attention to that kind of thing.",
            "I had nothing to do with any of it. That's all I'm saying."
        };
        String[] observRevealPool = {
            "Fine. I did see something that night. There was someone else hanging around the area, but I don't know who.",
            "Alright, alright. I noticed things were off. " + targetPerson + " had been acting scared for days.",
            "Okay, look — there was a meeting. I overheard part of it. Voices were raised.",
            "I'll tell you this much — " + targetPerson + " was expecting trouble. They told me so.",
            "Since you're pressing — I saw a car outside that I didn't recognise. It was there two nights running.",
            "Look — I didn't want to say this, but " + targetPerson + " called me the day before. Said something felt wrong.",
            "Fine. There was an argument. Not between me and " + targetPerson + " — someone else. I heard it.",
            "Alright. I was nearby. I saw something. But I can't be sure it's relevant."
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
            "I ran into " + targetPerson + " at a café a couple of days before.",
            "I was with " + targetPerson + " the evening before. We had a short conversation.",
            "We crossed paths at a mutual friend's place two days before.",
            "I spoke to " + targetPerson + " briefly, same day — I remember because it stood out."
        };
        String[] falseContactPool = {
            "I haven't seen " + targetPerson + " in weeks. We don't run in the same circles.",
            "I can't remember the last time I saw " + targetPerson + ". It's been a long time.",
            "We haven't been in touch. Not for months.",
            "I don't even have " + targetPerson + "'s number anymore. We drifted apart.",
            "Ask someone else — I'm the last person " + targetPerson + " would see.",
            "I haven't spoken to " + targetPerson + " since the last time we fell out. That was months ago."
        };
        String[] contactPool = truthfulContact ? truthfulContactPool : falseContactPool;
        script.addResponse(new InterviewResponse(InterviewTopic.LAST_CONTACT,
                "When did you last see " + targetPerson + "?",
                pick(contactPool), truthfulContact, targetPerson));

        // RELATIONSHIP with the target person — subject is evasive
        String[] subjectRelPool = {
            "We were acquaintances. Nothing more.",
            "I knew " + targetPerson + " through work. Strictly professional.",
            "We moved in the same circles. That's all.",
            "I barely know " + targetPerson + ". We crossed paths occasionally.",
            targetPerson + " and I have some history. I'd rather not go into it.",
            "We had dealings a while back. That's the extent of it.",
            "We knew each other. Not well. Not anymore.",
            "It was a professional relationship. It ended. Nothing complicated."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.RELATIONSHIP,
                "How do you know " + targetPerson + "?",
                pick(subjectRelPool), random.nextBoolean(), targetPerson));

        // MOTIVE — Intimidation-gated
        if (isMurder) {
            String[] motiveDeflectPool = {
                "Why are you asking me? You should be looking at " + client + ".",
                "I don't know who would do this. But I know it wasn't me.",
                "Plenty of people had issues with " + victim + ". I'm not the only one.",
                "Have you checked " + victim + "'s financial records? There were debts. People were owed money.",
                "Look elsewhere. There are people with far more reason than me.",
                client + " came to you because it's easier to blame someone than face the truth.",
                "I've answered your questions. I'm not going to speculate about who else had issues.",
                "This is a waste of both our time. I'm not the one you're looking for."
            };
            String[] motiveRevealPool = {
                "Fine. " + victim + " and I had problems. But there are others who had it worse with " + victim + ".",
                "You want the truth? " + victim + " made enemies. I was one of them, but I'm not the only one.",
                "Look, I'll admit it — " + victim + " wronged me. But I didn't do this. Check " + client + "'s story.",
                "Alright. Yes, I was angry with " + victim + ". But killing? That's not me. Someone else was circling.",
                "Fine — yes, we had a serious falling out. I'm not pretending otherwise. But I'm not a murderer.",
                victim + " owed me. Significantly. And I was angry. But anger isn't motive for this.",
                "I had reasons to be bitter. I'll admit that. But so did three other people. Look harder.",
                "I won't lie — there was bad blood. But you should know there were others circling too."
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
            "I don't hand out other people's details. Ask someone who cares.",
            "That's not something I'm going to help you with.",
            "You want me to help you build a case against someone? No.",
            "I've said what I'm going to say. I'm not handing over contact details on top of it."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.CONTACT_INFO,
                "Do you have a way to reach " + client + "?",
                pick(contactRefusalPool), random.nextBoolean(), client));

        // PERSONALITY — subject reluctantly reveals own/client's traits
        // Intimidation-gated: the subject opens up under pressure
        String[] persPool = {
            "Fine, you want to know about me? I " + pick(LIKE_DISLIKE_WORDS) + " " + pick(HOBBY_WORDS) + ". Happy now?",
            client + "? " + capitalize(client) + " is the kind of person who " + pick(SOCIAL_WORDS) + ". Ask anyone.",
            "Look, " + client + " " + pick(LIKE_DISLIKE_WORDS) + " " + pick(HOBBY_WORDS) + " more than most. That's no secret.",
            "I don't see how my interests matter. But fine — I " + pick(LIKE_DISLIKE_WORDS) + " " + pick(HOBBY_WORDS) + ".",
            "You want gossip? " + client + " always " + pick(SOCIAL_WORDS) + ". That tells you something.",
            "If you're asking about me — I spend most of my time at " + pick(LOCATION_CLUE_WORDS) + ". For what it's worth.",
            "I'll tell you one thing: I " + pick(LIKE_DISLIKE_WORDS) + " " + pick(HOBBY_WORDS) + ". Always have. It's got nothing to do with this.",
            "You want character references? " + client + " " + pick(SOCIAL_WORDS) + ". Go ask them what that means."
        };
        String[] persAltPool = {
            "My personal life is none of your business.",
            "I'm not here to chat about hobbies. Ask me something relevant.",
            "Why would I tell you what I like or don't like?",
            "That's got nothing to do with anything. Next question.",
            "You're wasting my time with that. I'm not answering it.",
            "I came here to answer questions about the case. That isn't one of them."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.PERSONALITY,
                "Tell me about your interests — or " + client + "'s.",
                pick(persPool), random.nextBoolean(), client,
                "Intimidation", 5, pick(persAltPool)));
    }

    private void buildWitnessInterview(InterviewScript script, CaseType type,
                                       String client, String subject, String victim,
                                       String targetPerson) {
        boolean isMurder = type == CaseType.MURDER;

        // ALIBI
        String[] alibiPool = {
            "I was in the neighbourhood that evening, on my way home from work.",
            "I was visiting a friend nearby. I passed through the area around the time it happened.",
            "I was at the local shop. I remember because it was close to closing time.",
            "I was walking the dog. That's how I ended up seeing what I saw.",
            "I'd just come back from a late errand. The area was quiet but I was definitely there.",
            "I live two streets over. I walk through there every evening.",
            "I was on my way to meet someone. I cut through that street as I always do.",
            "I was out late that night — unusual for me. That's exactly why I remember it."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.ALIBI,
                "Where were you at the time of the incident?",
                pick(alibiPool), true, ""));

        // WHEREABOUTS of subject
        String[] subjectWhereaboutsPool = {
            "I saw " + subject + " near " + targetPerson + "'s place that evening. " + subject + " looked agitated.",
            "I'm pretty sure " + subject + " was in the area. I recognised the car.",
            "I didn't see " + subject + " personally, but a neighbour mentioned spotting someone matching the description.",
            subject + " was definitely around. I saw someone who looked just like them leaving in a hurry.",
            "I noticed " + subject + " on the street earlier that afternoon. They seemed like they were waiting for something.",
            "There was a figure outside " + targetPerson + "'s building. I'm fairly certain it was " + subject + ".",
            "I passed " + subject + " heading in the direction of " + targetPerson + "'s place. About an hour before.",
            "Someone was watching the building that night. I can't be absolutely certain, but it looked like " + subject + "."
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
            "I've seen " + subject + " around but I couldn't tell you much about their character.",
            "I wouldn't want to pass judgement. I only know " + subject + " by sight.",
            "We've never spoken. I can only go on what I've observed from a distance.",
            "I try not to judge people I don't really know. I'll leave it at that."
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
                victim + " was private. Kept to themselves. I don't know much about their personal life.",
                victim + " was generous — always willing to help out a neighbour. A real loss.",
                "There was a quiet dignity to " + victim + ". Whatever happened, they didn't deserve it.",
                "I'd see " + victim + " most mornings. We'd wave. That's all — but enough to know they were a decent person.",
                victim + " had a few enemies. I don't know the details, but I heard things. Nothing that would explain this."
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
            "Things had been tense in the neighbourhood. There were arguments in the days before.",
            "A van that had been sitting on the street all week suddenly disappeared that night.",
            "I saw a light flicker off in " + targetPerson + "'s flat during a time when nobody should have been home.",
            "There was shouting — brief but sharp. It cut off fast. That stayed with me."
        };
        String[] observAltPool = {
            "I might have heard something, but I can't be sure.",
            "Nothing stands out in particular. It was a normal evening.",
            "I was busy with my own things. I didn't pay much attention.",
            "I heard a few things in passing but nothing that seemed significant at the time.",
            "I can't say for certain. The area is noisy at night.",
            "I wish I'd paid more attention. I just didn't think anything of it."
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
            "I can't remember exactly — maybe three or four days before.",
            "I waved at " + targetPerson + " on Monday morning. Same as always.",
            "We exchanged a few words outside the building two days before. Nothing unusual.",
            "I bumped into " + targetPerson + " at the corner shop. They seemed distracted, but I didn't think much of it.",
            "About a week before. We shared the lift — brief conversation, nothing memorable."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.LAST_CONTACT,
                "When did you last see " + targetPerson + "?",
                pick(contactPool), true, targetPerson));

        // RELATIONSHIP with the target person — casual/neighbourhood acquaintance
        String[] witnessRelPool = {
            "I know " + targetPerson + " from the neighbourhood.",
            "We're acquaintances. I see " + targetPerson + " around.",
            "We've spoken a few times. Nothing deep.",
            "I know " + targetPerson + " by sight. We've exchanged pleasantries.",
            "We've been neighbours for years. Never particularly close, but familiar.",
            "I see " + targetPerson + " at the local shops. We nod. That's about the extent of it.",
            "We're on speaking terms. Have been for a few years. Nothing more than that.",
            "I know " + targetPerson + " from the building. Polite relationship, no more."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.RELATIONSHIP,
                "How do you know " + targetPerson + "?",
                pick(witnessRelPool), true, targetPerson));

        // MOTIVE — Intuition-gated
        if (isMurder) {
            String[] motivePool = {
                "I always thought " + subject + " was jealous of " + victim + ". " + subject + " couldn't hide it.",
                "There was bad blood between " + subject + " and " + victim + ". Everyone could see it.",
                subject + " once said something that stuck with me — about " + victim + " getting what was coming.",
                "I don't want to point fingers, but " + subject + " had more reason than anyone.",
                "I saw " + subject + " and " + victim + " arguing in the street once. It looked serious.",
                subject + " was fixated on " + victim + " in a way that went beyond normal conflict.",
                "I heard through mutual contacts that " + subject + " had threatened " + victim + " before.",
                "The way " + subject + " used to watch " + victim + " — it wasn't normal. It was something else."
            };
            String[] motiveAltPool = {
                "I can't think of anyone specific. It's a mystery to me.",
                "I don't like to speculate. I really don't know.",
                "I wouldn't want to accuse anyone. I'm just a witness.",
                "I've been asking myself the same thing. Nothing clear comes to mind.",
                "It could be anyone. I don't know enough about " + victim + "'s private life.",
                "Speculation feels wrong. I'd rather stick to what I actually saw."
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
            "I'm not sure I have " + subject + "'s number, but they're usually around the area.",
            "Actually, yes — I saved it after a noise dispute a few months back. Here.",
            "Let me check my phone. I think they're in here from the building committee.",
            "I do, as it happens. We were both on the residents' group for a while."
        };
        String[] contactRefusePool = {
            "I don't really know " + subject + " well enough to share their details.",
            "Sorry, I don't feel comfortable giving out contact information.",
            "I'd rather you found them yourself. I don't want to get involved.",
            "I'm not sure it's my place to hand out someone else's number.",
            "That feels like overstepping. I'm here to say what I saw — no more.",
            "I wouldn't know how to reach them anyway. We're not on those terms."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.CONTACT_INFO,
                "Do you have a way to reach " + subject + "?",
                pick(contactRevealPool), true, subject,
                "Charisma", 5, pick(contactRefusePool)));

        // PERSONALITY — witness reveals subject's or victim's traits (Empathy-gated)
        String targetForTraits = isMurder ? victim : subject;
        String[] witPersPool = {
            targetForTraits + " always had a thing for " + pick(HOBBY_WORDS) + ". I'd see them doing it all the time.",
            "I noticed " + targetForTraits + " " + pick(LIKE_DISLIKE_WORDS) + " " + pick(HOBBY_WORDS) + ". It was pretty obvious.",
            "From what I saw, " + targetForTraits + " " + pick(SOCIAL_WORDS) + ". That was just their nature.",
            targetForTraits + " was the kind who " + pick(SOCIAL_WORDS) + ". Anyone in the neighbourhood could tell you that.",
            "If there's one thing I know about " + targetForTraits + ", it's that they " + pick(LIKE_DISLIKE_WORDS) + " " + pick(HOBBY_WORDS) + ".",
            "I remember seeing " + targetForTraits + " at " + pick(LOCATION_CLUE_WORDS) + " more than once. A creature of habit.",
            "The way " + targetForTraits + " talked about " + pick(HOBBY_WORDS) + " — you could tell it was important to them.",
            targetForTraits + " " + pick(SOCIAL_WORDS) + ". That much was obvious to anyone paying attention."
        };
        String[] witPersAltPool = {
            "I didn't know " + targetForTraits + " well enough to say what they liked.",
            "I only saw " + targetForTraits + " in passing. I couldn't tell you their hobbies.",
            "I didn't pay much attention to " + targetForTraits + "'s personal life.",
            "We weren't close enough for that kind of conversation.",
            "I can only speak to what I observed, and that wasn't something I paid attention to.",
            targetForTraits + " kept to themselves. I couldn't tell you much about their interests."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.PERSONALITY,
                "What can you tell me about " + targetForTraits + "'s interests?",
                pick(witPersPool), true, targetForTraits,
                "Empathy", 5, pick(witPersAltPool)));
    }

    private void buildAssociateInterview(InterviewScript script, CaseType type,
                                         String client, String subject, String victim,
                                         String targetPerson, String associateName) {
        boolean isMurder = type == CaseType.MURDER;

        // ALIBI
        String[] alibiPool = {
            "I was at home that night. I went to bed early — I had an early start the next day.",
            "I was out with colleagues after work. We were at a restaurant until about 10 PM.",
            "I was at the gym until around 8 PM, then went straight home.",
            "I was travelling back from a meeting. The train was delayed — I have the ticket.",
            "I was finishing a project. Stayed late at the office — the building logs will confirm.",
            "I was at a friend's place for dinner. Four of us, all evening.",
            "I was on a long drive back from a site visit. Petrol station receipt will give you the time.",
            "I was at home dealing with a family matter. Not something I want to discuss, but I can provide a contact."
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
                "We weren't best friends, but I respected " + victim + ". A good person.",
                victim + " was someone I trusted entirely. Losing them this way — it doesn't make sense.",
                "We met through work and became genuinely close over the years.",
                "I'd known " + victim + " for the better part of a decade. A significant loss.",
                victim + " and I had our share of disagreements, but we always found a way through. This is a terrible end."
            };
        } else {
            relationshipPool = new String[]{
                "I've known " + subject + " professionally for several years.",
                subject + " and I are acquainted through mutual contacts.",
                "We used to work together. I know " + subject + " fairly well.",
                "I wouldn't say we're close, but I know " + subject + " well enough.",
                subject + " and I have a history — mostly professional, some personal.",
                "We were on the same team for a while. I know " + subject + " reasonably well.",
                "We move in the same industry circles. I've known " + subject + " for years.",
                "We're associates. The relationship has had its ups and downs."
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
            subject + " seemed like anyone else. Nothing stood out to me.",
            "We're professional acquaintances. I've never seen anything that concerned me.",
            "I wouldn't want to characterise someone based on limited interaction.",
            subject + " always seemed normal to me. That's all I can really say."
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
            "Someone mentioned seeing " + subject + " in the area, but I can't say for certain.",
            "I have no idea. " + subject + " doesn't report their movements to me.",
            "We didn't speak that day. I can't account for where " + subject + " was.",
            subject + " mentioned something about a meeting that evening. That's all I know.",
            "The last time I saw " + subject + " was before any of this happened. I couldn't say where they were that night."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.WHEREABOUTS,
                "Do you know where " + subject + " was at the time?",
                pick(whereaboutsPool), random.nextBoolean(), subject));

        // OBSERVATION — Perception-gated
        String[] observPool = {
            "In the weeks before, " + subject + " was acting differently. More secretive, more on edge.",
            targetPerson + " mentioned feeling watched or followed. I didn't take it seriously at the time.",
            "There was a lot of tension between " + subject + " and " + targetPerson + " recently.",
            "I noticed " + subject + " drinking more than usual. Something was clearly bothering them.",
            subject + " was taking calls at odd hours. Stepping out of rooms. I noticed, but I didn't ask.",
            targetPerson + " had been quieter than usual. Distant. I should have pushed harder to find out why.",
            "There was an incident a few weeks back — a heated exchange that I was present for. Things were clearly not fine.",
            subject + " asked me some unusual questions recently. About the case, about who knew what. It seemed odd at the time."
        };
        String[] observAltPool = {
            "I can't say I noticed anything unusual. Everything seemed normal.",
            "I wasn't around much in the weeks before, so I can't really say.",
            "Nothing specific comes to mind. I'm sorry.",
            "I wish I'd paid closer attention. I just didn't see it.",
            "Everything seemed routine to me. In hindsight, I may have missed something.",
            "I've been trying to think back. Nothing stands out clearly."
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
            "I can't remember exactly when I last saw " + targetPerson + ". Maybe four or five days before.",
            "We exchanged messages the morning of. Nothing out of the ordinary.",
            "I called " + targetPerson + " two days earlier. A short call. " + capitalize(targetPerson) + " seemed normal.",
            "We were in a meeting together earlier that week. Unremarkable — until all this.",
            "I last saw " + targetPerson + " about three days before. We parted on good terms."
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
                "There was a rivalry between them. " + subject + " couldn't stand that " + victim + " was doing better.",
                subject + " had made no secret of the resentment. " + victim + " knew about it. We all did.",
                "The falling out between " + subject + " and " + victim + " was more serious than either let on publicly.",
                "I saw them argue in private once. It was not the kind of argument you forget."
            };
            String[] motiveAltPool = {
                "I honestly don't know. This is all such a shock.",
                "I can't imagine why anyone would do something like this.",
                "I'd rather not speculate. I don't want to say the wrong thing.",
                "I've been trying to make sense of it. I can't.",
                "There was no one thing I could point to. Looking back — I still can't.",
                "I don't want to point fingers without something more solid to go on."
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
            "I've got " + subject + "'s number. They might not pick up, but it's worth a try.",
            "I have " + subject + " saved from a project we did together. Here.",
            subject + " and I share a contact group. I can pull the number from there.",
            "Yes — we've stayed in touch. I can pass on " + subject + "'s number.",
            "I have it. We met through work and kept each other's details. Here."
        };
        String[] contactInfoAltPool = {
            "I'd rather not share " + subject + "'s details without asking first.",
            "I don't think " + subject + " would appreciate me giving out their number.",
            "You'll have to find them through other channels. I can't help with that.",
            "I'm not comfortable sharing someone's contact details without their knowledge.",
            "That feels like a step too far. I'll leave it to " + subject + " to decide what to share.",
            "I have the number, but I won't give it without speaking to " + subject + " first."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.CONTACT_INFO,
                "Do you have a way to reach " + subject + "?",
                pick(contactInfoPool), true, subject,
                "Charisma", 4, pick(contactInfoAltPool)));

        // PERSONALITY — associate reveals target's traits (Empathy-gated)
        String[] assocPersPool = {
            "Oh, " + targetPerson + " definitely " + pick(LIKE_DISLIKE_WORDS) + " " + pick(HOBBY_WORDS) + ". We talked about it often.",
            targetPerson + " is really into " + pick(HOBBY_WORDS) + ". It's one of those things everyone knew about them.",
            "Between you and me, " + targetPerson + " " + pick(SOCIAL_WORDS) + ". I saw it firsthand.",
            "I spent enough time with " + targetPerson + " to know they " + pick(LIKE_DISLIKE_WORDS) + " " + pick(HOBBY_WORDS) + ".",
            targetPerson + " had strong feelings about " + pick(HOBBY_WORDS) + " — " + pick(LIKE_DISLIKE_WORDS) + " it, actually.",
            "You'd often find " + targetPerson + " at " + pick(LOCATION_CLUE_WORDS) + " after hours. A real regular.",
            "The way " + targetPerson + " talked about " + pick(HOBBY_WORDS) + " — it mattered to them more than most things.",
            targetPerson + " " + pick(SOCIAL_WORDS) + ". Anyone close to them would tell you the same."
        };
        String[] assocPersAltPool = {
            "We didn't really talk about personal interests. Our relationship was professional.",
            "I'm not sure I could tell you what " + targetPerson + " liked. We didn't talk about that.",
            "I'd rather not speculate about " + targetPerson + "'s personal life.",
            "That side of their life wasn't something we discussed.",
            "I knew " + targetPerson + " in a specific context. Beyond that — I couldn't say.",
            "Our conversations stayed professional. I don't know enough about their interests to comment."
        };
        script.addResponse(new InterviewResponse(InterviewTopic.PERSONALITY,
                "What do you know about " + targetPerson + "'s hobbies or interests?",
                pick(assocPersPool), true, targetPerson,
                "Empathy", 4, pick(assocPersAltPool)));
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
        // Subject/Suspect opinions about the client are deflective
        if (npcRole.startsWith("Subject") || npcRole.startsWith("Suspect")) {
            if (otherRole.startsWith("Client")) {
                String[] pool = {
                    otherName + " has always had it in for me. Don't believe everything you hear.",
                    "I barely know " + otherName + ". We've spoken maybe twice.",
                    otherName + " is paranoid. Sees conspiracies everywhere."
                };
                return pick(pool);
            }
            String[] pool = {
                "I don't really know " + otherName + " well enough to comment.",
                otherName + "? We get along fine. No issues between us.",
                "I've nothing to say about " + otherName + "."
            };
            return pick(pool);
        }

        // Opinions about the subject — emphasise character flaws
        if (otherName.equals(subject)) {
            String[] pool = {
                otherName + " has always been the jealous type. Envious of anyone who had more.",
                "I've heard " + otherName + " has a short temper. People are wary of confrontation.",
                otherName + " seemed charming on the surface, but there was something calculating underneath.",
                otherName + " was possessive. Couldn't stand others having what they wanted.",
                otherName + " was competitive to the point of obsession. Always comparing.",
                otherName + " was resentful — especially about money. Always felt shortchanged.",
                "I wouldn't call " + otherName + " violent, but there was a bitterness. A deep grudge.",
                otherName + " was manipulative. Tells people what they want to hear.",
                "I've seen " + otherName + " fly into a rage over small things."
            };
            return pick(pool);
        }

        // Opinions about the victim in a murder case
        if (isMurder && otherName.equals(victim)) {
            String[] pool = {
                otherName + " was well-liked by most people. I can't imagine who would do this.",
                otherName + " had a way of rubbing some people the wrong way, but nothing serious.",
                "Everyone knew " + otherName + ". A decent person. This has shaken everyone.",
                otherName + " was private. Kept to themselves. I don't know much about their personal life."
            };
            return pick(pool);
        }

        // Generic opinions about other NPCs
        String[] pool = {
            otherName + " seems reliable enough. We've had no problems.",
            "I don't know " + otherName + " very well, to be honest.",
            otherName + " is a decent person as far as I can tell.",
            "I've heard mixed things about " + otherName + ", but nothing specific.",
            otherName + " keeps to themselves mostly. Hard to read."
        };
        return pick(pool);
    }

    /**
     * Returns a generic, non-committal opinion text shown when the player's
     * Empathy attribute does not meet the gate requirement.
     *
     * @param otherName name of the NPC being asked about
     * @return evasive answer sentence
     */
    public String buildOpinionAltText(String otherName) {
        String[] pool = {
            "I don't really know " + otherName + " well enough to say.",
            otherName + "? I couldn't tell you much. We weren't close.",
            "I don't have strong feelings about " + otherName + " either way.",
            "I'd rather not comment on " + otherName + ". I barely know them."
        };
        return pick(pool);
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
        String[] pool = {
            "Sure, I have " + otherName + "'s number. It's " + phone
                    + ". You can usually find them at the " + location + ".",
            "Yes — " + otherName + " can be reached at " + phone
                    + ". They spend most of their time at the " + location + ".",
            otherName + "? Their number is " + phone
                    + ". Last I heard they hang around the " + location + " most days.",
            "I've got " + otherName + "'s number right here: " + phone
                    + ". If you want to meet in person, try the " + location + ".",
            "Here — " + phone + ". That's " + otherName + "'s direct number."
                    + " They're usually at the " + location + " in the evenings."
        };
        return pick(pool);
    }

    /**
     * Returns a contact-info refusal text shown when the Charisma gate is
     * not met.
     *
     * @param otherName name of the NPC whose contact details were requested
     * @return refusal sentence
     */
    public String buildContactInfoAltText(String otherName) {
        String[] pool = {
            "I'm not comfortable sharing " + otherName + "'s details with a stranger.",
            "I don't think " + otherName + " would want me giving out their number.",
            "You'd have to ask someone else. I don't give out people's information.",
            "I might have their number somewhere, but I'm not sharing it with you.",
            "Sorry, I don't feel right handing out " + otherName + "'s contact details."
        };
        return pick(pool);
    }
}
