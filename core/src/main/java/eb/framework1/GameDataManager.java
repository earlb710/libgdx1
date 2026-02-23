package eb.framework1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages game data loaded from JSON files including building definitions and categories.
 * Loads data on startup and provides access to the definitions.
 */
public class GameDataManager {
    private static final String BUILDINGS_FILE  = "buildings.json";
    private static final String CATEGORIES_FILE = "categories.json";
    private static final String TEXT_FILE       = "text/en.json";

    private final List<BuildingDefinition> buildings;
    private final Map<String, BuildingDefinition> buildingsById;
    private final Map<String, List<BuildingDefinition>> buildingsByCategory;
    private final List<CategoryDefinition> categories;
    private final Map<String, CategoryDefinition> categoriesById;

    private String buildingsVersion;
    private String categoriesVersion;
    private NovelTextEngine novelTextEngine;

    public GameDataManager() {
        this.buildings = new ArrayList<>();
        this.buildingsById = new HashMap<>();
        this.buildingsByCategory = new HashMap<>();
        this.categories = new ArrayList<>();
        this.categoriesById = new HashMap<>();

        loadBuildings();
        loadCategories();
        loadNovelTextEngine();

        Gdx.app.log("GameDataManager", "Loaded " + buildings.size() + " buildings and " + categories.size() + " categories");
    }
    
    /**
     * Loads the novel text engine from {@code text/en.json}.
     */
    private void loadNovelTextEngine() {
        try {
            FileHandle file = Gdx.files.internal(TEXT_FILE);
            if (!file.exists()) {
                Gdx.app.error("GameDataManager", "Text file not found: " + TEXT_FILE);
                novelTextEngine = new NovelTextEngine(null, null);
                return;
            }
            String json = file.readString("UTF-8");
            novelTextEngine = NovelTextEngine.fromJsonString(json);
            Gdx.app.log("GameDataManager", "Loaded novel text engine from " + TEXT_FILE);
        } catch (Exception e) {
            Gdx.app.error("GameDataManager", "Error loading novel text engine: " + e.getMessage(), e);
            novelTextEngine = new NovelTextEngine(null, null);
        }
    }

    /**
     * Loads building definitions from buildings.json
     */
    private void loadBuildings() {
        try {
            FileHandle file = Gdx.files.internal(BUILDINGS_FILE);
            if (!file.exists()) {
                Gdx.app.error("GameDataManager", "Buildings file not found: " + BUILDINGS_FILE);
                return;
            }
            
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(file);
            
            buildingsVersion = root.getString("version", "unknown");
            
            JsonValue buildingsArray = root.get("buildings");
            if (buildingsArray != null) {
                for (JsonValue buildingJson = buildingsArray.child; buildingJson != null; buildingJson = buildingJson.next) {
                    BuildingDefinition building = parseBuildingDefinition(buildingJson);
                    buildings.add(building);
                    buildingsById.put(building.getId(), building);
                    
                    // Cache by category for fast lookup
                    String category = building.getCategory();
                    if (!buildingsByCategory.containsKey(category)) {
                        buildingsByCategory.put(category, new ArrayList<>());
                    }
                    buildingsByCategory.get(category).add(building);
                }
            }
            
            Gdx.app.log("GameDataManager", "Loaded buildings.json v" + buildingsVersion + " with " + buildings.size() + " buildings");
        } catch (Exception e) {
            Gdx.app.error("GameDataManager", "Error loading buildings: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parses a single building definition from JSON.
     */
    private BuildingDefinition parseBuildingDefinition(JsonValue json) {
        BuildingDefinition building = new BuildingDefinition();
        building.setId(json.getString("id"));
        building.setName(json.getString("name"));
        building.setCategory(json.getString("category"));
        building.setMinFloors(json.getInt("minFloors"));
        building.setMaxFloors(json.getInt("maxFloors"));
        building.setUnitsPerFloor(json.getInt("unitsPerFloor"));
        building.setCapacity(json.getInt("capacity"));
        building.setPercentage(json.getDouble("percentage"));
        building.setDescription(json.getString("description"));
        
        List<String> improvements = new ArrayList<>();
        JsonValue improvementsArray = json.get("improvements");
        if (improvementsArray != null) {
            for (JsonValue imp = improvementsArray.child; imp != null; imp = imp.next) {
                improvements.add(imp.asString());
            }
        }
        building.setImprovements(improvements);
        
        return building;
    }
    
    /**
     * Loads category definitions from categories.json
     */
    private void loadCategories() {
        try {
            FileHandle file = Gdx.files.internal(CATEGORIES_FILE);
            if (!file.exists()) {
                Gdx.app.error("GameDataManager", "Categories file not found: " + CATEGORIES_FILE);
                return;
            }
            
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(file);
            
            categoriesVersion = root.getString("version", "unknown");
            
            JsonValue categoriesArray = root.get("categories");
            if (categoriesArray != null) {
                for (JsonValue categoryJson = categoriesArray.child; categoryJson != null; categoryJson = categoryJson.next) {
                    CategoryDefinition category = parseCategoryDefinition(categoryJson);
                    categories.add(category);
                    categoriesById.put(category.getId(), category);
                }
            }
            
            Gdx.app.log("GameDataManager", "Loaded categories.json v" + categoriesVersion + " with " + categories.size() + " categories");
        } catch (Exception e) {
            Gdx.app.error("GameDataManager", "Error loading categories: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parses a single category definition from JSON.
     */
    private CategoryDefinition parseCategoryDefinition(JsonValue json) {
        CategoryDefinition category = new CategoryDefinition();
        category.setId(json.getString("id"));
        category.setName(json.getString("name"));
        category.setDescription(json.getString("description"));
        category.setColor(json.getString("color"));
        return category;
    }
    
    // ===== Accessors =====
    
    /**
     * Returns all building definitions.
     */
    public List<BuildingDefinition> getBuildings() {
        return Collections.unmodifiableList(buildings);
    }
    
    /**
     * Returns a building definition by its ID.
     */
    public BuildingDefinition getBuildingById(String id) {
        return buildingsById.get(id);
    }
    
    /**
     * Returns all buildings in a specific category.
     */
    public List<BuildingDefinition> getBuildingsByCategory(String categoryId) {
        List<BuildingDefinition> result = buildingsByCategory.get(categoryId);
        if (result == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(result);
    }
    
    /**
     * Returns all category definitions.
     */
    public List<CategoryDefinition> getCategories() {
        return Collections.unmodifiableList(categories);
    }
    
    /**
     * Returns a category definition by its ID.
     */
    public CategoryDefinition getCategoryById(String id) {
        return categoriesById.get(id);
    }
    
    /**
     * Returns the category for a building.
     */
    public CategoryDefinition getCategoryForBuilding(BuildingDefinition building) {
        if (building == null) return null;
        return categoriesById.get(building.getCategory());
    }
    
    /**
     * Returns the version of the loaded buildings.json file.
     */
    public String getBuildingsVersion() {
        return buildingsVersion;
    }
    
    /**
     * Returns the version of the loaded categories.json file.
     */
    public String getCategoriesVersion() {
        return categoriesVersion;
    }

    /**
     * Returns the {@link NovelTextEngine} loaded from {@code text/en.json}.
     * Never {@code null}; returns an empty engine if the file could not be loaded.
     */
    public NovelTextEngine getNovelTextEngine() {
        return novelTextEngine;
    }
}
