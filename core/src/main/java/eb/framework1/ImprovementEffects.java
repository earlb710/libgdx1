package eb.framework1;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides attribute modifier effects for improvements based on keyword matching.
 * Each improvement name is matched against keywords to determine which character
 * attributes are affected and by how much (-1 to +2).
 *
 * <p>Keywords are matched case-insensitively against the improvement name.
 * When multiple keywords match, effects are combined (summed) and clamped to -1..+2.
 */
public final class ImprovementEffects {

    private ImprovementEffects() {
    }

    private static final Map<String, Map<CharacterAttribute, Integer>> KEYWORD_EFFECTS = new HashMap<>();

    // Quality ratings: keyword (lower-case) → cost/luxury score 1-10.
    // getQuality() returns the maximum score among all matching keywords; default = 3.
    private static final Map<String, Integer> QUALITY_KEYWORDS = new HashMap<>();

    static {
        // --- Security / Surveillance ---
        keyword("Security", CharacterAttribute.PERCEPTION, 2, CharacterAttribute.INTIMIDATION, 1);
        keyword("Camera", CharacterAttribute.PERCEPTION, 1);
        keyword("Surveillance", CharacterAttribute.PERCEPTION, 2);
        keyword("Bullet-Resistant", CharacterAttribute.INTIMIDATION, 2, CharacterAttribute.STAMINA, 1);
        keyword("Armory", CharacterAttribute.STRENGTH, 2, CharacterAttribute.INTIMIDATION, 2);
        keyword("Checkpoint", CharacterAttribute.PERCEPTION, 1, CharacterAttribute.INTIMIDATION, 1);
        keyword("Evidence", CharacterAttribute.INTELLIGENCE, 2, CharacterAttribute.PERCEPTION, 1);
        keyword("Interrogation", CharacterAttribute.INTIMIDATION, 3, CharacterAttribute.EMPATHY, 1);
        keyword("Holding Cell", CharacterAttribute.INTIMIDATION, 2);
        keyword("Detective", CharacterAttribute.INTELLIGENCE, 2, CharacterAttribute.PERCEPTION, 2);
        keyword("Patrol", CharacterAttribute.STAMINA, 1, CharacterAttribute.PERCEPTION, 1);

        // --- Fitness / Sports / Physical ---
        keyword("Fitness", CharacterAttribute.STRENGTH, 2, CharacterAttribute.STAMINA, 2, CharacterAttribute.AGILITY, 1);
        keyword("Weight Room", CharacterAttribute.STRENGTH, 3, CharacterAttribute.STAMINA, 1);
        keyword("Gym", CharacterAttribute.STRENGTH, 2, CharacterAttribute.STAMINA, 2);
        keyword("Swimming", CharacterAttribute.STAMINA, 2, CharacterAttribute.AGILITY, 1);
        keyword("Pool", CharacterAttribute.STAMINA, 1, CharacterAttribute.AGILITY, 1);
        keyword("Tennis", CharacterAttribute.AGILITY, 2, CharacterAttribute.STAMINA, 1);
        keyword("Basketball", CharacterAttribute.AGILITY, 2, CharacterAttribute.STAMINA, 1);
        keyword("Athletic", CharacterAttribute.AGILITY, 2, CharacterAttribute.STAMINA, 2);
        keyword("Climbing Wall", CharacterAttribute.STRENGTH, 2, CharacterAttribute.AGILITY, 2);
        keyword("Yoga", CharacterAttribute.AGILITY, 2, CharacterAttribute.EMPATHY, 1);
        keyword("Spin Room", CharacterAttribute.STAMINA, 3);
        keyword("Cardio", CharacterAttribute.STAMINA, 2);
        keyword("Track", CharacterAttribute.STAMINA, 2, CharacterAttribute.AGILITY, 1);
        keyword("Golf", CharacterAttribute.PERCEPTION, 1, CharacterAttribute.CHARISMA, 1);
        keyword("Bowling", CharacterAttribute.PERCEPTION, 1);
        keyword("Sport", CharacterAttribute.STAMINA, 1, CharacterAttribute.AGILITY, 1);

        // --- Medical / Health ---
        keyword("Medical", CharacterAttribute.INTELLIGENCE, 1, CharacterAttribute.EMPATHY, 1);
        keyword("Pharmacy", CharacterAttribute.INTELLIGENCE, 1, CharacterAttribute.MEMORY, 1);
        keyword("Health", CharacterAttribute.STAMINA, 1, CharacterAttribute.EMPATHY, 1);
        keyword("Hospital", CharacterAttribute.EMPATHY, 2, CharacterAttribute.INTELLIGENCE, 1);
        keyword("Surgery", CharacterAttribute.INTELLIGENCE, 2, CharacterAttribute.PERCEPTION, 1);
        keyword("ICU", CharacterAttribute.PERCEPTION, 2, CharacterAttribute.STAMINA, 1);
        keyword("Triage", CharacterAttribute.PERCEPTION, 2, CharacterAttribute.INTUITION, 1);
        keyword("Trauma", CharacterAttribute.STAMINA, 2, CharacterAttribute.PERCEPTION, 1);
        keyword("Cardiac", CharacterAttribute.INTELLIGENCE, 1, CharacterAttribute.PERCEPTION, 1);
        keyword("Nurse", CharacterAttribute.EMPATHY, 2);
        keyword("Patient", CharacterAttribute.EMPATHY, 1);
        keyword("Dental", CharacterAttribute.PERCEPTION, 1);
        keyword("X-Ray", CharacterAttribute.PERCEPTION, 2, CharacterAttribute.INTELLIGENCE, 1);
        keyword("Imaging", CharacterAttribute.PERCEPTION, 2);
        keyword("Vaccine", CharacterAttribute.STAMINA, 1);
        keyword("Therapy", CharacterAttribute.EMPATHY, 2, CharacterAttribute.INTUITION, 1);
        keyword("Wellness", CharacterAttribute.STAMINA, 1, CharacterAttribute.EMPATHY, 1);
        keyword("Spa", CharacterAttribute.CHARISMA, 1, CharacterAttribute.STAMINA, 1);
        keyword("Sauna", CharacterAttribute.STAMINA, 1);
        keyword("Massage", CharacterAttribute.STAMINA, 1);

        // --- Library / Education / Research ---
        keyword("Library", CharacterAttribute.INTELLIGENCE, 2, CharacterAttribute.MEMORY, 2);
        keyword("Book", CharacterAttribute.INTELLIGENCE, 1, CharacterAttribute.MEMORY, 1);
        keyword("Study", CharacterAttribute.INTELLIGENCE, 2, CharacterAttribute.MEMORY, 1);
        keyword("Lab", CharacterAttribute.INTELLIGENCE, 2, CharacterAttribute.PERCEPTION, 1);
        keyword("Research", CharacterAttribute.INTELLIGENCE, 3, CharacterAttribute.MEMORY, 1);
        keyword("Science", CharacterAttribute.INTELLIGENCE, 2, CharacterAttribute.PERCEPTION, 1);
        keyword("Computer", CharacterAttribute.INTELLIGENCE, 2, CharacterAttribute.MEMORY, 1);
        keyword("Archive", CharacterAttribute.MEMORY, 3, CharacterAttribute.INTELLIGENCE, 1);
        keyword("Reference", CharacterAttribute.MEMORY, 2, CharacterAttribute.INTELLIGENCE, 1);
        keyword("Classroom", CharacterAttribute.INTELLIGENCE, 1, CharacterAttribute.MEMORY, 1);
        keyword("Lecture", CharacterAttribute.INTELLIGENCE, 1, CharacterAttribute.MEMORY, 1);
        keyword("Education", CharacterAttribute.INTELLIGENCE, 2, CharacterAttribute.MEMORY, 1);
        keyword("Teaching", CharacterAttribute.INTELLIGENCE, 1, CharacterAttribute.CHARISMA, 1);
        keyword("Training", CharacterAttribute.INTELLIGENCE, 1, CharacterAttribute.STAMINA, 1);
        keyword("Innovation", CharacterAttribute.INTELLIGENCE, 2, CharacterAttribute.INTUITION, 1);

        // --- Technology / IT ---
        keyword("Server", CharacterAttribute.INTELLIGENCE, 1, CharacterAttribute.MEMORY, 1);
        keyword("Data Center", CharacterAttribute.INTELLIGENCE, 2, CharacterAttribute.MEMORY, 2);
        keyword("WiFi", CharacterAttribute.INTELLIGENCE, 1);
        keyword("Internet", CharacterAttribute.INTELLIGENCE, 1, CharacterAttribute.MEMORY, 1);
        keyword("Network", CharacterAttribute.INTELLIGENCE, 1);
        keyword("Digital", CharacterAttribute.INTELLIGENCE, 1);

        // --- Social / Dining / Entertainment ---
        keyword("Restaurant", CharacterAttribute.CHARISMA, 1, CharacterAttribute.EMPATHY, 1);
        keyword("Bar", CharacterAttribute.CHARISMA, 2, CharacterAttribute.EMPATHY, 1);
        keyword("Lounge", CharacterAttribute.CHARISMA, 1, CharacterAttribute.EMPATHY, 1);
        keyword("Dining", CharacterAttribute.CHARISMA, 1);
        keyword("Cafe", CharacterAttribute.CHARISMA, 1, CharacterAttribute.EMPATHY, 1);
        keyword("Coffee", CharacterAttribute.STAMINA, 1, CharacterAttribute.PERCEPTION, 1);
        keyword("Kitchen", CharacterAttribute.PERCEPTION, 1);
        keyword("Banquet", CharacterAttribute.CHARISMA, 2);
        keyword("Party", CharacterAttribute.CHARISMA, 2, CharacterAttribute.STAMINA, -1);
        keyword("Dance", CharacterAttribute.AGILITY, 1, CharacterAttribute.CHARISMA, 1);
        keyword("DJ", CharacterAttribute.CHARISMA, 1);
        keyword("Club", CharacterAttribute.CHARISMA, 1, CharacterAttribute.STEALTH, -1);
        keyword("Game Room", CharacterAttribute.INTELLIGENCE, 1, CharacterAttribute.INTUITION, 1);
        keyword("Arcade", CharacterAttribute.AGILITY, 1, CharacterAttribute.PERCEPTION, 1);
        keyword("Theater", CharacterAttribute.EMPATHY, 1, CharacterAttribute.PERCEPTION, 1);
        keyword("Cinema", CharacterAttribute.EMPATHY, 1, CharacterAttribute.PERCEPTION, 1);
        keyword("Movie", CharacterAttribute.EMPATHY, 1);
        keyword("Music", CharacterAttribute.EMPATHY, 1, CharacterAttribute.INTUITION, 1);
        keyword("Entertainment", CharacterAttribute.CHARISMA, 1);
        keyword("Wine", CharacterAttribute.CHARISMA, 1, CharacterAttribute.PERCEPTION, 1);
        keyword("Cigar", CharacterAttribute.CHARISMA, 1, CharacterAttribute.STAMINA, -1);
        keyword("Karaoke", CharacterAttribute.CHARISMA, 2, CharacterAttribute.STEALTH, -1);

        // --- Creative / Art ---
        keyword("Art", CharacterAttribute.INTUITION, 2, CharacterAttribute.EMPATHY, 1);
        keyword("Gallery", CharacterAttribute.PERCEPTION, 1, CharacterAttribute.INTUITION, 1);
        keyword("Studio", CharacterAttribute.INTUITION, 1);
        keyword("Photography", CharacterAttribute.PERCEPTION, 2, CharacterAttribute.INTUITION, 1);
        keyword("Craft", CharacterAttribute.PERCEPTION, 1, CharacterAttribute.INTUITION, 1);
        keyword("Woodshop", CharacterAttribute.STRENGTH, 1, CharacterAttribute.PERCEPTION, 1);
        keyword("Maker", CharacterAttribute.INTELLIGENCE, 1, CharacterAttribute.INTUITION, 1);

        // --- Stealth / Covert / Underground ---
        keyword("Underground", CharacterAttribute.STEALTH, 2);
        keyword("Basement", CharacterAttribute.STEALTH, 1);
        keyword("Vault", CharacterAttribute.STEALTH, 1, CharacterAttribute.PERCEPTION, 1);
        keyword("Hidden", CharacterAttribute.STEALTH, 2, CharacterAttribute.PERCEPTION, 1);
        keyword("Undercover", CharacterAttribute.STEALTH, 3, CharacterAttribute.CHARISMA, 1);
        keyword("Cellar", CharacterAttribute.STEALTH, 1);
        keyword("Attic", CharacterAttribute.STEALTH, 1);
        keyword("Tunnel", CharacterAttribute.STEALTH, 2, CharacterAttribute.AGILITY, 1);
        keyword("Private", CharacterAttribute.STEALTH, 1);
        keyword("Quiet", CharacterAttribute.STEALTH, 1, CharacterAttribute.PERCEPTION, 1);

        // --- Administrative / Office ---
        keyword("Office", CharacterAttribute.INTELLIGENCE, 1);
        keyword("Admin", CharacterAttribute.INTELLIGENCE, 1, CharacterAttribute.MEMORY, 1);
        keyword("Reception", CharacterAttribute.CHARISMA, 1, CharacterAttribute.EMPATHY, 1);
        keyword("Conference", CharacterAttribute.CHARISMA, 1, CharacterAttribute.INTELLIGENCE, 1);
        keyword("Meeting", CharacterAttribute.CHARISMA, 1);
        keyword("Board Room", CharacterAttribute.CHARISMA, 2, CharacterAttribute.INTIMIDATION, 1);
        keyword("Records", CharacterAttribute.MEMORY, 2, CharacterAttribute.PERCEPTION, 1);
        keyword("Document", CharacterAttribute.MEMORY, 1, CharacterAttribute.INTELLIGENCE, 1);
        keyword("Mail", CharacterAttribute.MEMORY, 1);
        keyword("Human Resources", CharacterAttribute.EMPATHY, 2, CharacterAttribute.CHARISMA, 1);

        // --- Garden / Nature / Outdoor ---
        keyword("Garden", CharacterAttribute.EMPATHY, 1, CharacterAttribute.STAMINA, 1);
        keyword("Park", CharacterAttribute.STAMINA, 1, CharacterAttribute.EMPATHY, 1);
        keyword("Playground", CharacterAttribute.AGILITY, 1, CharacterAttribute.EMPATHY, 1);
        keyword("Courtyard", CharacterAttribute.PERCEPTION, 1, CharacterAttribute.EMPATHY, 1);
        keyword("Terrace", CharacterAttribute.PERCEPTION, 1);
        keyword("Rooftop", CharacterAttribute.PERCEPTION, 2);
        keyword("Patio", CharacterAttribute.CHARISMA, 1);
        keyword("Outdoor", CharacterAttribute.STAMINA, 1, CharacterAttribute.PERCEPTION, 1);
        keyword("Green", CharacterAttribute.EMPATHY, 1);
        keyword("Landscape", CharacterAttribute.EMPATHY, 1, CharacterAttribute.PERCEPTION, 1);

        // --- Vehicle / Transport ---
        keyword("Parking", CharacterAttribute.PERCEPTION, 1);
        keyword("Car", CharacterAttribute.AGILITY, 1);
        keyword("Shuttle", CharacterAttribute.AGILITY, 1);
        keyword("Transit", CharacterAttribute.AGILITY, 1);
        keyword("Bicycle", CharacterAttribute.STAMINA, 1, CharacterAttribute.AGILITY, 1);
        keyword("Bike", CharacterAttribute.STAMINA, 1, CharacterAttribute.AGILITY, 1);
        keyword("Helipad", CharacterAttribute.AGILITY, 2, CharacterAttribute.PERCEPTION, 1);
        keyword("Fuel", CharacterAttribute.STAMINA, -1);

        // --- Storage / Industrial ---
        keyword("Storage", CharacterAttribute.MEMORY, 1);
        keyword("Warehouse", CharacterAttribute.STRENGTH, 1, CharacterAttribute.MEMORY, 1);
        keyword("Loading", CharacterAttribute.STRENGTH, 2);
        keyword("Forklift", CharacterAttribute.STRENGTH, 1, CharacterAttribute.AGILITY, 1);
        keyword("Assembly", CharacterAttribute.PERCEPTION, 1, CharacterAttribute.STAMINA, 1);
        keyword("Conveyor", CharacterAttribute.PERCEPTION, 1);
        keyword("Welding", CharacterAttribute.STRENGTH, 1, CharacterAttribute.PERCEPTION, 1);
        keyword("Maintenance", CharacterAttribute.STRENGTH, 1, CharacterAttribute.INTELLIGENCE, 1);

        // --- Legal / Government ---
        keyword("Courtroom", CharacterAttribute.CHARISMA, 2, CharacterAttribute.INTIMIDATION, 1);
        keyword("Judge", CharacterAttribute.INTIMIDATION, 2, CharacterAttribute.INTELLIGENCE, 1);
        keyword("Attorney", CharacterAttribute.INTELLIGENCE, 2, CharacterAttribute.CHARISMA, 1);
        keyword("Counsel", CharacterAttribute.EMPATHY, 2, CharacterAttribute.INTELLIGENCE, 1);
        keyword("Probation", CharacterAttribute.INTIMIDATION, 1, CharacterAttribute.EMPATHY, 1);
        keyword("Dispatch", CharacterAttribute.PERCEPTION, 2, CharacterAttribute.MEMORY, 1);

        // --- Religious ---
        keyword("Chapel", CharacterAttribute.EMPATHY, 2, CharacterAttribute.INTUITION, 1);
        keyword("Sanctuary", CharacterAttribute.EMPATHY, 2, CharacterAttribute.STEALTH, 1);
        keyword("Prayer", CharacterAttribute.INTUITION, 2, CharacterAttribute.EMPATHY, 1);
        keyword("Fellowship", CharacterAttribute.CHARISMA, 1, CharacterAttribute.EMPATHY, 2);

        // --- Luxury / VIP ---
        keyword("VIP", CharacterAttribute.CHARISMA, 2, CharacterAttribute.INTIMIDATION, 1);
        keyword("Luxury", CharacterAttribute.CHARISMA, 2);
        keyword("Executive", CharacterAttribute.CHARISMA, 2, CharacterAttribute.INTIMIDATION, 1);
        keyword("Presidential", CharacterAttribute.CHARISMA, 3, CharacterAttribute.INTIMIDATION, 2);
        keyword("Concierge", CharacterAttribute.CHARISMA, 2, CharacterAttribute.EMPATHY, 1);
        keyword("Valet", CharacterAttribute.CHARISMA, 1);
        keyword("Doorman", CharacterAttribute.CHARISMA, 1, CharacterAttribute.PERCEPTION, 1);
        keyword("Butler", CharacterAttribute.CHARISMA, 1, CharacterAttribute.STEALTH, 1);

        // --- Emergency / Safety ---
        keyword("Emergency", CharacterAttribute.PERCEPTION, 2, CharacterAttribute.STAMINA, 1);
        keyword("Fire", CharacterAttribute.STAMINA, 1, CharacterAttribute.PERCEPTION, 1);
        keyword("First Aid", CharacterAttribute.EMPATHY, 1, CharacterAttribute.INTELLIGENCE, 1);
        keyword("Decontamination", CharacterAttribute.INTELLIGENCE, 1, CharacterAttribute.PERCEPTION, 1);
        keyword("Hazmat", CharacterAttribute.INTELLIGENCE, 1, CharacterAttribute.PERCEPTION, 1);

        // --- Childcare ---
        keyword("Daycare", CharacterAttribute.EMPATHY, 2, CharacterAttribute.CHARISMA, 1);
        keyword("Day Care", CharacterAttribute.EMPATHY, 2, CharacterAttribute.CHARISMA, 1);
        keyword("Nursery", CharacterAttribute.EMPATHY, 2);
        keyword("Children", CharacterAttribute.EMPATHY, 1, CharacterAttribute.CHARISMA, 1);
        keyword("Kids", CharacterAttribute.EMPATHY, 1, CharacterAttribute.CHARISMA, 1);
        keyword("Youth", CharacterAttribute.EMPATHY, 1, CharacterAttribute.CHARISMA, 1);
        keyword("Teen", CharacterAttribute.EMPATHY, 1);
        keyword("Infant", CharacterAttribute.EMPATHY, 2);

        // --- Negative effects for specific categories ---
        keyword("Noise", CharacterAttribute.PERCEPTION, -1, CharacterAttribute.STEALTH, -2);
        keyword("Jumbotron", CharacterAttribute.STEALTH, -2, CharacterAttribute.PERCEPTION, -1);
        keyword("Light Show", CharacterAttribute.STEALTH, -2, CharacterAttribute.PERCEPTION, 1);
        keyword("Sound System", CharacterAttribute.STEALTH, -1, CharacterAttribute.PERCEPTION, -1);
        keyword("Lottery", CharacterAttribute.INTELLIGENCE, -1, CharacterAttribute.INTUITION, 1);

        // ---- Quality ratings (1-10, higher = more expensive/luxurious) ----
        // 10 — ultra-luxury, one-of-a-kind
        quality("presidential", 10);
        quality("helipad",      10);
        quality("yacht",        10);
        // 9 — private/bespoke
        quality("butler",           9);
        quality("private cinema",   9);
        quality("infinity pool",    9);
        quality("private beach",    9);
        quality("private elevator", 9);
        quality("sky lounge",       9);
        // 8 — executive/luxury tier
        quality("luxury",       8);
        quality("executive",    8);
        quality("vip",          8);
        quality("penthouse",    8);
        quality("ballroom",     8);
        quality("golf",         8);
        quality("fine dining",  8);
        quality("private dining", 8);
        quality("wine cellar",  8);
        quality("doorman",      8);
        // 7 — premium amenities
        quality("spa",          7);
        quality("concierge",    7);
        quality("valet",        7);
        quality("rooftop",      7);
        quality("rooftop bar",  7);
        quality("rooftop lounge", 7);
        quality("cigar",        7);
        quality("sommelier",    7);
        quality("personal shopping", 7);
        quality("butler service", 7);
        // 6 — high-quality standard
        quality("restaurant",   6);
        quality("swimming pool", 6);
        quality("pool",         6);
        quality("gym",          6);
        quality("fitness",      6);
        quality("bar",          6);
        quality("lounge",       6);
        quality("theater",      6);
        quality("data center",  6);
        quality("conference",   6);
        quality("surgery",      6);
        quality("science lab",  6);
        quality("weight room",  6);
        quality("auditorium",   6);
        // 5 — good mid-range amenities
        quality("library",      5);
        quality("café",         5);
        quality("cafe",         5);
        quality("coffee",       5);
        quality("office",       5);
        quality("reception",    5);
        quality("lab",          5);
        quality("sauna",        5);
        quality("tennis",       5);
        quality("yoga",         5);
        quality("arcade",       5);
        quality("dance",        5);
        quality("chapel",       5);
        quality("courtroom",    5);
        quality("pharmacy",     5);
        // 4 — standard/functional
        quality("cafeteria",    4);
        quality("kitchen",      4);
        quality("break room",   4);
        quality("meeting",      4);
        quality("garden",       4);
        quality("courtyard",    4);
        quality("playground",   4);
        quality("wifi",         4);
        quality("atm",          4);
        quality("laundry",      4);
        quality("locker room",  4);
        quality("music room",   4);
        quality("computer",     4);
        // 3 — basic utility
        quality("parking",      3);
        quality("storage",      3);
        quality("stairwell",    3);
        quality("elevator",     3);
        quality("mailbox",      3);
        quality("recycling",    3);
        quality("bike",         3);
        quality("bicycle",      3);
        quality("intercom",     3);
        quality("vending",      3);
        quality("restroom",     3);
        // 2 — infrastructure/safety
        quality("loading",      2);
        quality("trash",        2);
        quality("signage",      2);
        quality("fire lane",    2);
        quality("drainage",     2);
        quality("generator",    2);
        quality("ventilation",  2);
        // 1 — absolute basics (markings, bumps, stripes)
        quality("striping",     1);
        quality("speed bump",   1);
        quality("handicap",     1);
        quality("fire escape",  1);
    }

    /**
     * Helper to register keyword effects with 1 attribute modifier.
     */
    private static void keyword(String kw, CharacterAttribute a1, int v1) {
        Map<CharacterAttribute, Integer> effects = new HashMap<>();
        effects.put(a1, v1);
        KEYWORD_EFFECTS.put(kw.toLowerCase(), Collections.unmodifiableMap(effects));
    }

    /**
     * Helper to register keyword effects with 2 attribute modifiers.
     */
    private static void keyword(String kw, CharacterAttribute a1, int v1,
                                CharacterAttribute a2, int v2) {
        Map<CharacterAttribute, Integer> effects = new HashMap<>();
        effects.put(a1, v1);
        effects.put(a2, v2);
        KEYWORD_EFFECTS.put(kw.toLowerCase(), Collections.unmodifiableMap(effects));
    }

    /**
     * Helper to register keyword effects with 3 attribute modifiers.
     */
    private static void keyword(String kw, CharacterAttribute a1, int v1,
                                CharacterAttribute a2, int v2,
                                CharacterAttribute a3, int v3) {
        Map<CharacterAttribute, Integer> effects = new HashMap<>();
        effects.put(a1, v1);
        effects.put(a2, v2);
        effects.put(a3, v3);
        KEYWORD_EFFECTS.put(kw.toLowerCase(), Collections.unmodifiableMap(effects));
    }

    /**
     * Computes attribute modifiers for an improvement by matching its name
     * against known keywords. Modifiers from multiple matching keywords
     * are combined and clamped to the range -1..+2.
     *
     * @param improvementName The name of the improvement
     * @return An unmodifiable map of attribute modifiers (only non-zero values)
     */
    public static Map<CharacterAttribute, Integer> getEffects(String improvementName) {
        if (improvementName == null || improvementName.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        String nameLower = improvementName.toLowerCase();
        Map<CharacterAttribute, Integer> combined = new HashMap<>();

        for (Map.Entry<String, Map<CharacterAttribute, Integer>> entry : KEYWORD_EFFECTS.entrySet()) {
            if (nameLower.contains(entry.getKey())) {
                for (Map.Entry<CharacterAttribute, Integer> effect : entry.getValue().entrySet()) {
                    combined.merge(effect.getKey(), effect.getValue(), Integer::sum);
                }
            }
        }

        // Clamp values to -1..+2 and remove zeros
        Map<CharacterAttribute, Integer> result = new HashMap<>();
        for (Map.Entry<CharacterAttribute, Integer> entry : combined.entrySet()) {
            int clamped = Math.max(-1, Math.min(2, entry.getValue()));
            if (clamped != 0) {
                result.put(entry.getKey(), clamped);
            }
        }

        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns a quality rating (1–10) for an improvement based on keyword matching.
     * The rating reflects how expensive or luxurious the improvement is:
     * 1 = basic infrastructure, 10 = ultra-luxury.
     * Returns 3 (basic utility) when no quality keyword matches.
     *
     * @param improvementName The name of the improvement
     * @return quality score in the range [1, 10]
     */
    public static int getQuality(String improvementName) {
        if (improvementName == null || improvementName.trim().isEmpty()) return 3;
        String nameLower = improvementName.toLowerCase();
        int best = 3; // default: basic utility
        for (Map.Entry<String, Integer> entry : QUALITY_KEYWORDS.entrySet()) {
            if (nameLower.contains(entry.getKey())) {
                if (entry.getValue() > best) best = entry.getValue();
            }
        }
        return best;
    }

    /** Registers a quality keyword. */
    private static void quality(String kw, int score) {
        QUALITY_KEYWORDS.put(kw.toLowerCase(), score);
    }
}
