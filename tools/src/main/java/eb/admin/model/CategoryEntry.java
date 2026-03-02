package eb.admin.model;

/**
 * Represents a single entry in any category list within category_en.json.
 * The {@code color} field is only used by building categories.
 */
public class CategoryEntry {
    private String code;
    private String description;
    private String color;

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
}
