package eb.framework1.character;

/**
 * Represents a skin-tone definition loaded from the
 * {@code skin_tone_categories} array in {@code text/category_en.json}.
 *
 * <p>Each entry has three fields:
 * <ul>
 *   <li>{@code code} — machine-readable identifier (e.g. {@code "porcelain_very_fair"})</li>
 *   <li>{@code name} — human-readable label (e.g. {@code "Porcelain / Very Fair"})</li>
 *   <li>{@code rgb}  — RGB colour string (e.g. {@code "rgb(255, 229, 217)"})</li>
 * </ul>
 */
public class SkinToneDefinition {

    private String code;
    private String name;
    private String rgb;

    /** No-arg constructor required for JSON deserialisation. */
    public SkinToneDefinition() {}

    public SkinToneDefinition(String code, String name, String rgb) {
        this.code = code;
        this.name = name;
        this.rgb  = rgb;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRgb() { return rgb; }
    public void setRgb(String rgb) { this.rgb = rgb; }

    @Override
    public String toString() {
        return "SkinToneDefinition{code='" + code + "', name='" + name + "', rgb='" + rgb + "'}";
    }
}
