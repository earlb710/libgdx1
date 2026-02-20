package eb.framework1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public BuildingDefinition() {
        this.improvements = new ArrayList<>();
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
