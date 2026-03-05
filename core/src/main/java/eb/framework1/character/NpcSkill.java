package eb.framework1.character;

/**
 * Represents a learned skill or occupation type for a non-player character.
 *
 * <p>Each skill defines:
 * <ul>
 *   <li>A human-readable display name.</li>
 *   <li>One or more building-category IDs where a character with this skill
 *       typically works.  These match the {@code category} field in
 *       {@code buildings_en.json}.</li>
 *   <li>Typical work-start and work-end hours (24-hour clock).</li>
 * </ul>
 *
 * <p>Skills are used by {@link NpcGenerator} to assign a realistic work
 * location on the city map and to build a plausible daily schedule.
 */
public enum NpcSkill {

    SHOP_CLERK("Shop Clerk",
            new String[]{"commercial", "retail"},
            8, 18,
            "Serves customers and manages merchandise in a retail or commercial establishment."),

    OFFICE_WORKER("Office Worker",
            new String[]{"office"},
            9, 17,
            "Handles administrative or professional tasks in an office environment."),

    MEDICAL_PROFESSIONAL("Medical Professional",
            new String[]{"medical"},
            7, 19,
            "Provides healthcare services at a clinic, hospital, or similar facility."),

    EDUCATOR("Educator",
            new String[]{"education"},
            7, 15,
            "Teaches or instructs students at a school, college, or educational centre."),

    LAW_ENFORCEMENT("Law Enforcement",
            new String[]{"public_services"},
            7, 19,
            "Enforces laws and maintains public safety at a police station or government facility."),

    HOSPITALITY_WORKER("Hospitality Worker",
            new String[]{"hospitality"},
            6, 22,
            "Works in a hotel, restaurant, or other service-industry establishment."),

    ENTERTAINER("Entertainer",
            new String[]{"entertainment"},
            12, 23,
            "Performs or hosts events at a theatre, arena, or entertainment venue."),

    LABORER("Laborer",
            new String[]{"industrial"},
            6, 14,
            "Performs manual work at a warehouse, factory, or industrial site."),

    GOVERNMENT_WORKER("Government Worker",
            new String[]{"government", "public_services"},
            8, 17,
            "Works in a government building such as city hall or a courthouse."),

    RESEARCHER("Researcher",
            new String[]{"education", "office"},
            9, 18,
            "Conducts research at a university, laboratory, or corporate office."),

    FREELANCER("Freelancer",
            new String[]{"office", "commercial"},
            8, 18,
            "Works independently, often from coworking spaces or coffee shops."),

    HOMEMAKER("Homemaker",
            new String[]{"residential", "commercial"},
            9, 17,
            "Manages household duties, primarily spending time at home or nearby shops.");

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final String   displayName;
    private final String[] workBuildingCategories;
    private final int      workStartHour;
    private final int      workEndHour;
    private final String   description;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    NpcSkill(String displayName, String[] workBuildingCategories,
             int workStartHour, int workEndHour, String description) {
        this.displayName            = displayName;
        this.workBuildingCategories = workBuildingCategories;
        this.workStartHour          = workStartHour;
        this.workEndHour            = workEndHour;
        this.description            = description;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** Human-readable skill name (e.g. {@code "Shop Clerk"}). */
    public String getDisplayName() { return displayName; }

    /**
     * Building-category IDs where a character with this skill typically works.
     * These match the {@code category} field in {@code buildings_en.json} and
     * are used by {@link NpcGenerator} to locate a suitable workplace on the
     * city map.
     *
     * @return a defensive copy of the non-null, non-empty category array
     */
    public String[] getWorkBuildingCategories() { return workBuildingCategories.clone(); }

    /** Hour of day (0–23) when a character with this skill typically starts work. */
    public int getWorkStartHour() { return workStartHour; }

    /** Hour of day (0–23) when a character with this skill typically finishes work. */
    public int getWorkEndHour() { return workEndHour; }

    /** One-sentence description of what a character with this skill does. */
    public String getDescription() { return description; }

    /**
     * Returns {@code true} if any of this skill's work building categories
     * matches {@code categoryId}.
     *
     * @param categoryId a building category ID such as {@code "commercial"}
     * @return {@code true} if {@code categoryId} is one of the skill's work categories
     */
    public boolean worksInCategory(String categoryId) {
        if (categoryId == null) return false;
        for (String cat : workBuildingCategories) {
            if (cat.equals(categoryId)) return true;
        }
        return false;
    }
}
