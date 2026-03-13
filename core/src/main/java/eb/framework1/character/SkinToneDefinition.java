package eb.framework1.character;

/**
 * Represents a skin-tone definition loaded from the
 * {@code skin_tone_categories} array in {@code text/category_en.json}.
 *
 * <p>Each entry has four fields:
 * <ul>
 *   <li>{@code code}       — machine-readable identifier (e.g. {@code "porcelain_very_fair"})</li>
 *   <li>{@code name}       — human-readable label (e.g. {@code "Porcelain / Very Fair"})</li>
 *   <li>{@code rgb}        — RGB colour in HTML hex format (e.g. {@code "#FFE5D9"})</li>
 *   <li>{@code percentage} — relative spawn weight 0–100 (used for weighted-random
 *                            skin-tone assignment; percentages need not sum to 100)</li>
 * </ul>
 */
public class SkinToneDefinition {

    private String code;
    private String name;
    private String rgb;
    /** Relative spawn weight (0–100). Defaults to 10 if not specified. */
    private int percentage = 10;

    /** No-arg constructor required for JSON deserialisation. */
    public SkinToneDefinition() {}

    public SkinToneDefinition(String code, String name, String rgb) {
        this.code = code;
        this.name = name;
        this.rgb  = rgb;
    }

    public SkinToneDefinition(String code, String name, String rgb, int percentage) {
        this.code       = code;
        this.name       = name;
        this.rgb        = rgb;
        this.percentage = percentage;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRgb() { return rgb; }
    public void setRgb(String rgb) { this.rgb = rgb; }

    public int getPercentage() { return percentage; }
    public void setPercentage(int percentage) { this.percentage = percentage; }

    @Override
    public String toString() {
        return "SkinToneDefinition{code='" + code + "', name='" + name
                + "', rgb='" + rgb + "', percentage=" + percentage + "}";
    }
}
