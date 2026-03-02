package eb.gmodel1.city;

import eb.gmodel1.character.*;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a building definition loaded from buildings.json.
 * This defines the template/type of a building, not an individual building instance.
 */
public class BuildingDefinition {
    private String id;
    private String name;
    private String category;
    private int minFloors;
    private int maxFloors;
    private int unitsPerFloor;
    private int capacity;
    private double percentage;
    private String description;
    private List<String> improvements;
    /** Attribute modifiers from buildings.json (attribute name → modifier value). */
    private Map<String, Integer> attributeModifiers;

    public BuildingDefinition() {
        this.improvements = new ArrayList<>();
        this.attributeModifiers = new HashMap<>();
    }

    public BuildingDefinition(String id, String name, String category, int minFloors, int maxFloors,
                              int unitsPerFloor, int capacity, double percentage, String description,
                              List<String> improvements) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.minFloors = minFloors;
        this.maxFloors = maxFloors;
        this.unitsPerFloor = unitsPerFloor;
        this.capacity = capacity;
        this.percentage = percentage;
        this.description = description;
        this.improvements = improvements != null ? new ArrayList<>(improvements) : new ArrayList<>();
        this.attributeModifiers = new HashMap<>();
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getMinFloors() {
        return minFloors;
    }

    public void setMinFloors(int minFloors) {
        this.minFloors = minFloors;
    }

    public int getMaxFloors() {
        return maxFloors;
    }

    public void setMaxFloors(int maxFloors) {
        this.maxFloors = maxFloors;
    }

    public int getUnitsPerFloor() {
        return unitsPerFloor;
    }

    public void setUnitsPerFloor(int unitsPerFloor) {
        this.unitsPerFloor = unitsPerFloor;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getImprovements() {
        return Collections.unmodifiableList(improvements);
    }

    public void setImprovements(List<String> improvements) {
        this.improvements = improvements != null ? new ArrayList<>(improvements) : new ArrayList<>();
    }

    /**
     * Returns the raw attribute modifiers defined in buildings.json for this building type.
     * Keys are {@link CharacterAttribute} enum names; values are modifier amounts.
     * Returns an empty map when no modifiers are defined for this building type.
     */
    public Map<String, Integer> getAttributeModifiers() {
        return Collections.unmodifiableMap(attributeModifiers);
    }

    public void setAttributeModifiers(Map<String, Integer> mods) {
        this.attributeModifiers = mods != null ? new HashMap<>(mods) : new HashMap<>();
    }

    /**
     * Returns the path to the building's icon file within the assets/icons/ directory.
     * The icon filename is derived from the building name by converting to lowercase
     * and replacing non-alphanumeric characters with underscores.
     *
     * @return The icon file path relative to the assets directory (e.g. "icons/police_station.png")
     */
    public String getIconPath() {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        return "icons/" + slug + ".png";
    }

    @Override
    public String toString() {
        return "BuildingDefinition{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", floors=" + minFloors + "-" + maxFloors +
                ", unitsPerFloor=" + unitsPerFloor +
                ", percentage=" + percentage +
                '}';
    }
}
