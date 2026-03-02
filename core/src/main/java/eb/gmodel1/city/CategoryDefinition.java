package eb.gmodel1.city;

/**
 * Represents a category definition loaded from text/category_en.json.
 * Defines building categories with display properties.
 */
public class CategoryDefinition {
    private String id;
    private String name;
    private String description;
    private String color;

    public CategoryDefinition() {
    }

    public CategoryDefinition(String id, String name, String description, String color) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    /**
     * Parses the hex color string and returns RGB components.
     * @return int array with [red, green, blue] values (0-255)
     */
    public int[] getColorRGB() {
        if (color == null || color.isEmpty()) {
            return new int[]{128, 128, 128}; // Default gray
        }
        String hex = color.startsWith("#") ? color.substring(1) : color;
        if (hex.length() != 6) {
            return new int[]{128, 128, 128}; // Default gray for invalid length
        }
        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return new int[]{r, g, b};
        } catch (Exception e) {
            return new int[]{128, 128, 128}; // Default gray on parse error
        }
    }

    /**
     * Returns the color as normalized floats for libGDX (0.0-1.0).
     * @return float array with [red, green, blue] values (0.0-1.0)
     */
    public float[] getColorFloats() {
        int[] rgb = getColorRGB();
        return new float[]{rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f};
    }

    @Override
    public String toString() {
        return "CategoryDefinition{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", color='" + color + '\'' +
                '}';
    }
}
