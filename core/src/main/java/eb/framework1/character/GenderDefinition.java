package eb.framework1.character;

/**
 * Represents a gender definition loaded from the
 * {@code gender_categories} array in {@code text/category_en.json}.
 *
 * <p>Each entry has exactly two fields:
 * <ul>
 *   <li>{@code code} — machine-readable identifier (e.g. {@code "male"})</li>
 *   <li>{@code name} — human-readable label (e.g. {@code "Male"})</li>
 * </ul>
 *
 * <p>Storing gender labels in the category file allows them to be translated
 * into different languages without changing any Java source code.
 */
public class GenderDefinition {

    private String code;
    private String name;

    /** No-arg constructor required for JSON deserialisation. */
    public GenderDefinition() {}

    public GenderDefinition(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return "GenderDefinition{code='" + code + "', name='" + name + "'}";
    }
}
