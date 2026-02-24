package eb.framework1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides attribute modifier effects for buildings based on keyword matching.
 * Each building name is matched against keywords to determine which character
 * attributes are affected and by how much (-3 to +3).
 *
 * <p>Keywords are matched case-insensitively against the building name.
 * When multiple keywords match, effects are combined (summed) and clamped to -3..+3.
 */
public final class BuildingEffects {

    private BuildingEffects() {
    }

    private static final Map<String, Map<CharacterAttribute, Integer>> KEYWORD_EFFECTS = new HashMap<>();

    static {
        // --- Parking ---
        keyword("Parking Lot", CharacterAttribute.PERCEPTION, 1);
        keyword("Parking Garage", CharacterAttribute.PERCEPTION, 1, CharacterAttribute.STEALTH, 1);

        // --- Retail / Commercial ---
        keyword("Convenience Store", CharacterAttribute.PERCEPTION, 1);
        keyword("Gas Station", CharacterAttribute.PERCEPTION, 1, CharacterAttribute.STAMINA, -1);
        keyword("Retail", CharacterAttribute.CHARISMA, 1, CharacterAttribute.PERCEPTION, 1);
        keyword("Pharmacy", CharacterAttribute.INTELLIGENCE, 1, CharacterAttribute.MEMORY, 1);
        keyword("Bank", CharacterAttribute.INTELLIGENCE, 1, CharacterAttribute.INTIMIDATION, 1);
        keyword("Laundromat", CharacterAttribute.STAMINA, 1);
        keyword("Supermarket", CharacterAttribute.PERCEPTION, 1, CharacterAttribute.STAMINA, 1);
        keyword("Warehouse Store", CharacterAttribute.STRENGTH, 1, CharacterAttribute.STAMINA, 1);
        keyword("Mall", CharacterAttribute.CHARISMA, 2, CharacterAttribute.PERCEPTION, 1);
        keyword("Shopping", CharacterAttribute.CHARISMA, 1, CharacterAttribute.PERCEPTION, 1);
        keyword("Strip Mall", CharacterAttribute.CHARISMA, 1);
        keyword("Car Dealership", CharacterAttribute.CHARISMA, 2, CharacterAttribute.INTIMIDATION, 1);
        keyword("Auto Repair", CharacterAttribute.STRENGTH, 1, CharacterAttribute.PERCEPTION, 1);

        // --- Food / Dining ---
        keyword("Fast Food", CharacterAttribute.STAMINA, -1, CharacterAttribute.CHARISMA, 1);
        keyword("Coffee Shop", CharacterAttribute.STAMINA, 1, CharacterAttribute.PERCEPTION, 1);
        keyword("Restaurant", CharacterAttribute.CHARISMA, 2, CharacterAttribute.EMPATHY, 1);
        keyword("Fine Dining", CharacterAttribute.CHARISMA, 3, CharacterAttribute.EMPATHY, 1);
        keyword("Casual Restaurant", CharacterAttribute.CHARISMA, 1, CharacterAttribute.EMPATHY, 1);

        // --- Fitness / Personal ---
        keyword("Hair Salon", CharacterAttribute.CHARISMA, 2);
        keyword("Fitness Center", CharacterAttribute.STRENGTH, 2, CharacterAttribute.STAMINA, 2, CharacterAttribute.AGILITY, 1);

        // --- Medical ---
        keyword("Medical Clinic", CharacterAttribute.INTELLIGENCE, 1, CharacterAttribute.EMPATHY, 2);
        keyword("Dental", CharacterAttribute.PERCEPTION, 1, CharacterAttribute.EMPATHY, 1);
        keyword("Veterinary", CharacterAttribute.EMPATHY, 2, CharacterAttribute.INTUITION, 1);
        keyword("Hospital", CharacterAttribute.INTELLIGENCE, 2, CharacterAttribute.EMPATHY, 2);
        keyword("Urgent Care", CharacterAttribute.PERCEPTION, 2, CharacterAttribute.STAMINA, 1);

        // --- Education ---
        keyword("Daycare", CharacterAttribute.EMPATHY, 2, CharacterAttribute.CHARISMA, 1);
        keyword("Elementary School", CharacterAttribute.EMPATHY, 2, CharacterAttribute.CHARISMA, 1);
        keyword("High School", CharacterAttribute.INTELLIGENCE, 1, CharacterAttribute.CHARISMA, 1);
        keyword("Community College", CharacterAttribute.INTELLIGENCE, 2, CharacterAttribute.MEMORY, 1);
        keyword("School", CharacterAttribute.INTELLIGENCE, 1, CharacterAttribute.MEMORY, 1);

        // --- Emergency / Law ---
        keyword("Fire Station", CharacterAttribute.STRENGTH, 2, CharacterAttribute.STAMINA, 2);
        keyword("Police Station", CharacterAttribute.PERCEPTION, 2, CharacterAttribute.INTIMIDATION, 2);
        keyword("Police", CharacterAttribute.PERCEPTION, 2, CharacterAttribute.INTIMIDATION, 2);

        // --- Government / Civic ---
        keyword("Post Office", CharacterAttribute.MEMORY, 1, CharacterAttribute.STAMINA, 1);
        keyword("Library", CharacterAttribute.INTELLIGENCE, 3, CharacterAttribute.MEMORY, 2);
        keyword("Community Center", CharacterAttribute.CHARISMA, 2, CharacterAttribute.EMPATHY, 2);
        keyword("City Hall", CharacterAttribute.CHARISMA, 2, CharacterAttribute.INTELLIGENCE, 1);
        keyword("Courthouse", CharacterAttribute.INTELLIGENCE, 2, CharacterAttribute.INTIMIDATION, 2);
        keyword("Senior Living", CharacterAttribute.EMPATHY, 2, CharacterAttribute.MEMORY, 1);

        // --- Residential ---
        keyword("Apartment", CharacterAttribute.STEALTH, 1, CharacterAttribute.PERCEPTION, 1);
        keyword("Luxury Apartment", CharacterAttribute.CHARISMA, 2, CharacterAttribute.STEALTH, 1);
        keyword("High-Rise", CharacterAttribute.PERCEPTION, 2, CharacterAttribute.STEALTH, 1);
        keyword("Townhouse", CharacterAttribute.STEALTH, 1, CharacterAttribute.EMPATHY, 1);
        keyword("Condominium", CharacterAttribute.PERCEPTION, 1, CharacterAttribute.CHARISMA, 1);
        keyword("Residential", CharacterAttribute.EMPATHY, 1);

        // --- Hospitality ---
        keyword("Budget Hotel", CharacterAttribute.STEALTH, 2, CharacterAttribute.PERCEPTION, 1);
        keyword("Business Hotel", CharacterAttribute.CHARISMA, 1, CharacterAttribute.INTELLIGENCE, 1);
        keyword("Luxury Hotel", CharacterAttribute.CHARISMA, 3, CharacterAttribute.EMPATHY, 1);
        keyword("Hotel", CharacterAttribute.CHARISMA, 1, CharacterAttribute.STEALTH, 1);

        // --- Office ---
        keyword("Office", CharacterAttribute.INTELLIGENCE, 1, CharacterAttribute.MEMORY, 1);
        keyword("Corporate Headquarters", CharacterAttribute.INTELLIGENCE, 2, CharacterAttribute.INTIMIDATION, 2);
        keyword("Coworking", CharacterAttribute.CHARISMA, 1, CharacterAttribute.INTELLIGENCE, 1);

        // --- Entertainment ---
        keyword("Movie Theater", CharacterAttribute.EMPATHY, 1, CharacterAttribute.PERCEPTION, 1);
        keyword("Bowling Alley", CharacterAttribute.AGILITY, 1, CharacterAttribute.CHARISMA, 1);
        keyword("Nightclub", CharacterAttribute.CHARISMA, 2, CharacterAttribute.STEALTH, -1);
        keyword("Sports Arena", CharacterAttribute.STRENGTH, 1, CharacterAttribute.STAMINA, 2, CharacterAttribute.AGILITY, 1);

        // --- Industrial ---
        keyword("Warehouse", CharacterAttribute.STRENGTH, 2, CharacterAttribute.STAMINA, 1);
        keyword("Manufacturing", CharacterAttribute.STRENGTH, 2, CharacterAttribute.STAMINA, 1);
        keyword("Data Center", CharacterAttribute.INTELLIGENCE, 3, CharacterAttribute.MEMORY, 2);
        keyword("Self Storage", CharacterAttribute.MEMORY, 1, CharacterAttribute.STEALTH, 1);
        keyword("Industrial", CharacterAttribute.STRENGTH, 1, CharacterAttribute.STAMINA, 1);

        // --- Religious ---
        keyword("Church", CharacterAttribute.EMPATHY, 2, CharacterAttribute.INTUITION, 1);
        keyword("Mosque", CharacterAttribute.EMPATHY, 2, CharacterAttribute.INTUITION, 1);
        keyword("Synagogue", CharacterAttribute.EMPATHY, 2, CharacterAttribute.INTUITION, 1);

        // --- Transit ---
        keyword("Transit Station", CharacterAttribute.AGILITY, 1, CharacterAttribute.PERCEPTION, 1);
        keyword("Bus Depot", CharacterAttribute.STAMINA, 1, CharacterAttribute.PERCEPTION, 1);

        // --- Fallback building types ---
        keyword("Park", CharacterAttribute.STAMINA, 1, CharacterAttribute.EMPATHY, 1);
        keyword("Commercial", CharacterAttribute.CHARISMA, 1);
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
     * Computes attribute modifiers for a building by matching its name
     * against known keywords. Modifiers from multiple matching keywords
     * are combined and clamped to the range -3..+3.
     *
     * <p>At most 2 distinct positive attribute enhancements are kept (the highest values).
     * Negative modifiers are not limited.
     *
     * @param buildingName The name of the building
     * @return An unmodifiable map of attribute modifiers (only non-zero values)
     */
    public static Map<CharacterAttribute, Integer> getEffects(String buildingName) {
        if (buildingName == null || buildingName.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        String nameLower = buildingName.trim().toLowerCase();
        Map<CharacterAttribute, Integer> combined = new HashMap<>();

        for (Map.Entry<String, Map<CharacterAttribute, Integer>> entry : KEYWORD_EFFECTS.entrySet()) {
            if (nameLower.contains(entry.getKey())) {
                for (Map.Entry<CharacterAttribute, Integer> effect : entry.getValue().entrySet()) {
                    combined.merge(effect.getKey(), effect.getValue(), Integer::sum);
                }
            }
        }

        // Clamp values to -3..+3 and remove zeros
        Map<CharacterAttribute, Integer> clamped = new HashMap<>();
        for (Map.Entry<CharacterAttribute, Integer> entry : combined.entrySet()) {
            int val = Math.max(-3, Math.min(3, entry.getValue()));
            if (val != 0) {
                clamped.put(entry.getKey(), val);
            }
        }

        // Separate positive and negative modifiers
        List<Map.Entry<CharacterAttribute, Integer>> positives = new ArrayList<>();
        Map<CharacterAttribute, Integer> result = new HashMap<>();
        for (Map.Entry<CharacterAttribute, Integer> entry : clamped.entrySet()) {
            if (entry.getValue() > 0) {
                positives.add(entry);
            } else {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        // Keep only the top 2 positive enhancements (by value, descending)
        positives.sort(Comparator.comparingInt(Map.Entry<CharacterAttribute, Integer>::getValue).reversed());
        int kept = 0;
        for (Map.Entry<CharacterAttribute, Integer> entry : positives) {
            if (kept < 2) {
                result.put(entry.getKey(), entry.getValue());
                kept++;
            }
        }

        return Collections.unmodifiableMap(result);
    }
}
