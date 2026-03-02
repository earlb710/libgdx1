package eb.gmodel1.city;

import eb.gmodel1.character.*;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides attribute modifier effects for buildings based on keyword matching.
 * Each building name is matched against keywords to determine which character
 * attributes are affected and by how much (-2 to +3).
 *
 * <p>Only a small number of landmark/iconic building types carry attribute effects
 * (effects are intentionally scarce). Keywords are matched case-insensitively
 * against the building name. When multiple keywords match, effects are combined
 * (summed) and clamped to -2..+3.
 */
public final class BuildingEffects {

    private BuildingEffects() {
    }

    private static final Map<String, Map<CharacterAttribute, Integer>> KEYWORD_EFFECTS = new HashMap<>();

    static {
        // Only landmark / iconic building types carry attribute effects (scarce).

        // --- Emergency / Law ---
        keyword("Police Station", CharacterAttribute.PERCEPTION, 3, CharacterAttribute.INTIMIDATION, 2);
        keyword("Fire Station",   CharacterAttribute.STRENGTH, 2, CharacterAttribute.STAMINA, 2);

        // --- Government / Civic ---
        keyword("Library",           CharacterAttribute.INTELLIGENCE, 3, CharacterAttribute.MEMORY, 2);
        keyword("Courthouse",        CharacterAttribute.INTELLIGENCE, 2, CharacterAttribute.INTIMIDATION, 2);
        keyword("Community College", CharacterAttribute.INTELLIGENCE, 2);

        // --- Medical ---
        keyword("Hospital", CharacterAttribute.EMPATHY, 3, CharacterAttribute.INTELLIGENCE, 2);

        // --- Fitness ---
        keyword("Fitness Center", CharacterAttribute.STRENGTH, 3);

        // --- Industrial / Tech ---
        keyword("Data Center", CharacterAttribute.INTELLIGENCE, 3, CharacterAttribute.MEMORY, 2);

        // --- Food / Dining ---
        keyword("Fine Dining", CharacterAttribute.CHARISMA, 3);
        keyword("Fast Food",   CharacterAttribute.STAMINA, -2);

        // --- Entertainment ---
        keyword("Sports Arena", CharacterAttribute.STRENGTH, 2, CharacterAttribute.AGILITY, 1);
        keyword("Nightclub",    CharacterAttribute.CHARISMA, 2, CharacterAttribute.STEALTH, -2);

        // --- Religious ---
        keyword("Church",    CharacterAttribute.EMPATHY, 2);
        keyword("Mosque",    CharacterAttribute.EMPATHY, 2);
        keyword("Synagogue", CharacterAttribute.EMPATHY, 2);
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
     * are combined and clamped to the range -2..+3.
     *
     * <p>At most 2 distinct positive attribute enhancements are kept (the highest values).
     * Negative modifiers are not limited beyond the -2 floor.
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

        // Clamp values to -2..+3 and remove zeros
        Map<CharacterAttribute, Integer> clamped = new HashMap<>();
        for (Map.Entry<CharacterAttribute, Integer> entry : combined.entrySet()) {
            int val = Math.max(-2, Math.min(3, entry.getValue()));
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
