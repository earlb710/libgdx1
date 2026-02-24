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
}
