package eb.framework1.character;

/**
 * Represents a skill-category definition loaded from the
 * {@code skill_categories} array in {@code text/category_en.json}.
 *
 * <p>Each entry has exactly two fields:
 * <ul>
 *   <li>{@code code} — machine-readable identifier (e.g. {@code "work"})</li>
 *   <li>{@code name} — human-readable label (e.g. {@code "Work"})</li>
 * </ul>
 *
 * <p>The three built-in codes correspond to the {@link SkillCategory} enum
 * constants {@link SkillCategory#WORK}, {@link SkillCategory#HOBBIES}, and
 * {@link SkillCategory#GENERAL}.
 */
public class SkillCategoryDefinition {

    private String code;
    private String name;

    /** No-arg constructor required for JSON deserialisation. */
    public SkillCategoryDefinition() {}

    public SkillCategoryDefinition(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return "SkillCategoryDefinition{code='" + code + "', name='" + name + "'}";
    }
}
