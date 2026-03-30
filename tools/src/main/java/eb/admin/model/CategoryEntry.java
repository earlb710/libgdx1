package eb.admin.model;

/**
 * Represents a single entry in any category list within category_en.json.
 * The {@code color} field is used by building categories and improvement categories.
 * The {@code actions} field is only used by improvement categories and holds a
 * comma-separated list of action button labels (e.g. "rest, read, practice").
 * The {@code name} field is used by skill categories in place of {@code description}.
 * The {@code percentage} field is used by skin tone categories as a spawn weight (0–100).
 */
public class CategoryEntry {
    private String code;
    private String description;
    private String color;
    private String actions;
    private String name;
    private String rgb;
    /** Relative spawn weight for skin tone categories. Defaults to 10 if absent. */
    private int percentage = 10;

    public CategoryEntry() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getActions() {
        return actions;
    }

    public void setActions(String actions) {
        this.actions = actions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRgb() {
        return rgb;
    }

    public void setRgb(String rgb) {
        this.rgb = rgb;
    }

    public int getPercentage() {
        return percentage;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }
}
