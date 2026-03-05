package eb.framework1.character;

/**
 * The three mutually-exclusive categories that classify an {@link NpcSkill}.
 *
 * <h3>Semantics</h3>
 * <dl>
 *   <dt>{@link #WORK}</dt>
 *   <dd>The skill represents the NPC's <em>current employment</em>.  An NPC
 *       with a {@code WORK} skill must have a matching job entry in their
 *       {@link NpcSchedule} (the schedule block whose activity is
 *       {@code WORK} and whose building category matches the skill's
 *       {@link NpcSkill#getWorkBuildingCategories() work building categories}).
 *       Example: {@link NpcSkill#SHOP_CLERK}.</dd>
 *
 *   <dt>{@link #HOBBIES}</dt>
 *   <dd>The skill represents an <em>active hobby</em> that the NPC currently
 *       pursues.  An NPC with a {@code HOBBIES} skill will visit dedicated
 *       locations during their leisure schedule blocks.
 *       Example: photography, rock-climbing.</dd>
 *
 *   <dt>{@link #GENERAL}</dt>
 *   <dd>The skill is <em>inactive</em> — acquired from a former job or a past
 *       hobby that the NPC no longer actively practices.  It informs background
 *       knowledge and investigation interactions but does not drive schedule
 *       entries or location visits.
 *       Example: target shooting, basic carpentry.</dd>
 * </dl>
 *
 * <p>The {@link #code} and {@link #displayName} values mirror the
 * corresponding entries in the {@code skill_categories} array of
 * {@code text/category_en.json}.
 */
public enum SkillCategory {

    /** Current employment — drives a work schedule block. */
    WORK("work", "Work"),

    /** Active hobby — drives leisure location visits. */
    HOBBIES("hobbies", "Hobbies"),

    /** Inactive / background skill — no active schedule driver. */
    GENERAL("general", "General");

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /** Machine-readable identifier matching the JSON {@code code} field. */
    private final String code;

    /** Human-readable label matching the JSON {@code name} field. */
    private final String displayName;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    SkillCategory(String code, String displayName) {
        this.code        = code;
        this.displayName = displayName;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** Returns the machine-readable code (e.g. {@code "work"}). */
    public String getCode() { return code; }

    /** Returns the human-readable label (e.g. {@code "Work"}). */
    public String getDisplayName() { return displayName; }

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link SkillCategory} whose {@link #getCode() code} matches
     * {@code code} (case-insensitive), or {@code null} if no match is found.
     *
     * @param code the code string to look up (may be {@code null})
     * @return the matching constant, or {@code null}
     */
    public static SkillCategory fromCode(String code) {
        if (code == null) return null;
        String lower = code.toLowerCase();
        for (SkillCategory sc : values()) {
            if (sc.code.equals(lower)) return sc;
        }
        return null;
    }
}
